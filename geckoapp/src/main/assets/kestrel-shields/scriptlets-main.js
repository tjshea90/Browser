'use strict';
// Kestrel's scriptlets (AW1b): small functions that run INSIDE the page, before its own scripts, to defuse what the filter lists call anti-adblock walls, ad injectors, pop-up openers and countdown
// timers. Our OWN code, written from the behaviour the public uBlock Origin wiki documents (names, arguments, effect); nothing is copied from it. Runs in world MAIN (registered by background.js like the
// pop-up guard) in every frame at document_start and does nothing until the isolated content script (content.js) sends the calls that the lists have for THIS site:
//   content.js  --  document event __kst_sl_v1, detail = JSON  [["name","arg","arg"],...]  -->  this file runs each call once (every call in try/catch: a broken call never reaches the page)
//   this file  --  document event __kst_sl_ready_v1 (so content.js can send again if it spoke first)  and  __kst_sl_n_v1 "<calls run>:<calls that threw>"  -->  content.js
// The names are the canonical ones of core/filter/ScriptletRules.kt (aliases are folded there). Tested in headless Chromium by web-tests/scriptlets.test.js (plain DOM and JS: the same in Gecko).
(function () {
  var EVENT = '__kst_sl_v1', READY = '__kst_sl_ready_v1', REPORT = '__kst_sl_n_v1';
  try {
    if (typeof browser !== 'undefined' && browser && browser.runtime) return; // RX3-G1: declared in the manifest as well; a copy that ever ran in the isolated world (extension API present) must do nothing there
    if (window.__kstSL) return;
    Object.defineProperty(window, '__kstSL', { value: 1 });
  } catch (e) { return; }

  // ---- the natives this file needs, taken now: no page script has run yet
  var O = Object, defProp = O.defineProperty, getDesc = O.getOwnPropertyDescriptor, keys = O.keys, hasOwn = O.prototype.hasOwnProperty;
  var nParse = JSON.parse, nStringify = JSON.stringify;
  var nToString = Function.prototype.toString;
  var nRegExp = RegExp, nDate = Date;
  var nFetch = window.fetch, nResponse = window.Response;
  var nAEL = (window.EventTarget && EventTarget.prototype.addEventListener) || null;
  var XHR = window.XMLHttpRequest;
  var xhrOpen = XHR && XHR.prototype.open, xhrSend = XHR && XHR.prototype.send;
  var nSetTimeout = window.setTimeout;
  var started = nDate.now();
  var noop = function () {};

  function rnd() { return Math.random().toString(36).slice(2, 10); }
  function fnText(f) { try { return nToString.call(f); } catch (e) { return String(f); } }
  function text(v) { try { return typeof v === 'function' ? fnText(v) : String(v); } catch (e) { return ''; } }
  function isObj(v) { return v !== null && (typeof v === 'object' || typeof v === 'function'); }

  // ---- needles: a plain text (found anywhere), a /regex/flags, `!` in front = "does NOT match", empty = anything
  function pattern(s, flags) {
    s = (s === undefined || s === null) ? '' : String(s);
    var neg = false;
    if (s.length > 1 && s.charAt(0) === '!') { neg = true; s = s.slice(1); }
    if (s === '') return { all: true, neg: neg };
    var m = /^\/(.+)\/([gimsuy]*)$/.exec(s), re;
    try {
      if (m) re = new nRegExp(m[1], (m[2] || '').replace(/[gy]/g, ''));
      else re = new nRegExp(s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), (flags || '').replace(/[gy]/g, ''));
    } catch (e) { return { never: true, neg: false }; }
    return { re: re, neg: neg };
  }
  function hit(p, str) {
    if (p.never) return false;
    var r = p.all ? true : p.re.test(str);
    return p.neg ? !r : r;
  }
  // like pattern() but the regular expression keeps its g flag (replace-all): for replace-node-text
  function replacer(s) {
    s = (s === undefined || s === null) ? '' : String(s);
    var m = /^\/(.+)\/([gimsuy]*)$/.exec(s);
    try {
      if (m) { var f = (m[2] || '').replace(/[gy]/g, ''); return new nRegExp(m[1], f.indexOf('g') < 0 ? f + 'g' : f); }
      return new nRegExp(s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'g');
    } catch (e) { return null; }
  }
  // arguments after the fixed ones come as pairs `name, value` (includes, excludes, condition, sedCount, stay, propsToMatch, stack ...)
  function pairs(args, from) {
    var o = {};
    for (var i = from; i < args.length; i += 2) o[String(args[i])] = i + 1 < args.length ? String(args[i + 1]) : '';
    return o;
  }

  // ---- following `a.b.c` where parts of it do not exist yet: the missing place gets an accessor that remembers what the page puts there and goes on once an object arrives
  function trapChain(owner, parts, i, fin, depth) {
    if (depth > 24 || !isObj(owner)) return;
    var prop = parts[i];
    if (i === parts.length - 1) { fin(owner, prop); return; }
    var d = getDesc(owner, prop), cur;
    if (d && 'value' in d) cur = d.value;
    else { try { cur = owner[prop]; } catch (e) { cur = undefined; } }
    if (isObj(cur)) { trapChain(cur, parts, i + 1, fin, depth + 1); return; }
    if (d && d.configurable === false) return;
    var stored = cur;
    defProp(owner, prop, {
      configurable: true, enumerable: d ? d.enumerable : true,
      get: function () { return stored; },
      set: function (v) { stored = v; if (isObj(v)) { try { trapChain(v, parts, i + 1, fin, depth + 1); } catch (e) { /* ignore */ } } }
    });
  }
  function chainOf(s) {
    var p = String(s || '').split('.');
    if (p.length > 1 && p[0] === 'window') p.shift();
    for (var i = 0; i < p.length; i++) if (!p[i]) return null;
    return p.length ? p : null;
  }
  function current(owner, prop) { // what is there now (so a trap can keep returning it)
    var d = getDesc(owner, prop);
    if (d && 'value' in d) return d.value;
    try { return owner[prop]; } catch (e) { return undefined; }
  }
  function trap(chainText, make) { // make(owner, prop) -> a property descriptor, or null to leave the place alone
    var parts = chainOf(chainText);
    if (!parts) return;
    trapChain(window, parts, 0, function (owner, prop) {
      var d = getDesc(owner, prop);
      if (d && d.configurable === false) return;
      var nd = make(owner, prop, current(owner, prop));
      if (!nd) return;
      nd.configurable = true;
      nd.enumerable = d ? d.enumerable : true;
      defProp(owner, prop, nd);
    }, 0);
  }

  // ---- set-constant: the property always reads as the given value
  function valueOfToken(t) {
    t = String(t === undefined ? '' : t).trim();
    switch (t) {
      case 'undefined': return { v: undefined };
      case 'false': return { v: false };
      case 'true': return { v: true };
      case 'null': return { v: null };
      case 'noopFunc': return { v: function () {} };
      case 'trueFunc': return { v: function () { return true; } };
      case 'falseFunc': return { v: function () { return false; } };
      case 'throwFunc': return { v: function () { throw new Error(rnd()); } };
      case 'noopPromiseResolve': return { v: function () { return Promise.resolve({}); } };
      case 'noopPromiseReject': return { v: function () { return Promise.reject(new Error(rnd())); } };
      case 'emptyStr': case '': return { v: '' };
      case 'emptyArr': return { v: [] };
      case 'emptyObj': return { v: {} };
      case 'yes': return { v: 'yes' };
      case 'no': return { v: 'no' };
    }
    if (/^-?\d{1,5}$/.test(t)) return { v: parseInt(t, 10) };
    return null;
  }
  function setConstant(chain, value) {
    var c = valueOfToken(value);
    if (!c) return;
    trap(chain, function () { return { get: function () { return c.v; }, set: function () {} }; });
  }

  // ---- aopr / aopw / acs / aost: abort (throw) when the page reads / writes the property, when the running script holds the needle, when the stack holds the needle
  function abortRead(chain) {
    trap(chain, function () { return { get: function () { throw new ReferenceError(rnd()); }, set: function () {} }; });
  }
  function abortWrite(chain) {
    trap(chain, function (o, p, cur) { return { get: function () { return cur; }, set: function () { throw new ReferenceError(rnd()); } }; });
  }
  function abortScript(chain, needle) {
    var np = pattern(needle);
    var own = document.currentScript;
    trap(chain, function (o, p, cur) {
      return {
        get: function () {
          var s = document.currentScript;
          if (s && s !== own) {
            var body = s.src ? s.src : (s.textContent || '');
            if (hit(np, body)) throw new ReferenceError(rnd());
          }
          return cur;
        },
        set: function (v) { cur = v; }
      };
    });
  }
  function abortStack(chain, needle) {
    var np = pattern(needle);
    trap(chain, function (o, p, cur) {
      return {
        get: function () { var st = ''; try { st = new Error().stack || ''; } catch (e) { /* ignore */ } if (hit(np, st)) throw new ReferenceError(rnd()); return cur; },
        set: function (v) { cur = v; }
      };
    });
  }

  // ---- addEventListener-defuser: listeners of a type whose code holds the needle are never added
  var aeldRules = [], aeldWrapped = false;
  function aeld(type, handler) {
    aeldRules.push({ t: pattern(type), h: pattern(handler) });
    if (aeldWrapped || !nAEL) return;
    aeldWrapped = true;
    EventTarget.prototype.addEventListener = function (t, l) {
      try {
        var ts = String(t), ht = null;
        for (var i = 0; i < aeldRules.length; i++) {
          var r = aeldRules[i];
          if (!hit(r.t, ts)) continue;
          if (ht === null) ht = (typeof l === 'function') ? fnText(l) : ((l && typeof l.handleEvent === 'function') ? fnText(l.handleEvent) : String(l));
          if (hit(r.h, ht)) return undefined;
        }
      } catch (e) { /* ignore */ }
      return nAEL.apply(this, arguments);
    };
  }

  // ---- timers: no-setTimeout-if / no-setInterval-if turn a matching callback into nothing; adjust-setTimeout / adjust-setInterval run it sooner (countdowns)
  var timerRules = { setTimeout: [], setInterval: [], requestAnimationFrame: [] }, timerWrapped = {};
  function delayOk(spec, d) {
    if (spec === undefined || spec === null || spec === '' || spec === '*') return true;
    var s = String(spec), neg = s.charAt(0) === '!';
    if (neg) s = s.slice(1);
    var n = parseFloat(s);
    if (n !== n) return true;
    var same = (+d || 0) === n;
    return neg ? !same : same;
  }
  function wrapTimer(name) {
    if (timerWrapped[name]) return;
    timerWrapped[name] = true;
    var orig = window[name];
    if (typeof orig !== 'function') return;
    window[name] = function (cb, delay) {
      try {
        var rules = timerRules[name];
        if (rules.length) {
          var body = text(cb);
          for (var i = 0; i < rules.length; i++) {
            var r = rules[i];
            if (!hit(r.needle, body) || !delayOk(r.delay, delay)) continue;
            if (r.kind === 'no') arguments[0] = noop; else arguments[1] = (+delay || 0) * r.boost;
            break;
          }
        }
      } catch (e) { /* ignore */ }
      return orig.apply(this, arguments);
    };
  }
  function noTimer(name, needle, delay) { timerRules[name].push({ kind: 'no', needle: pattern(needle), delay: delay }); wrapTimer(name); }
  function nanoTimer(name, needle, delay, boost) {
    var b = parseFloat(boost); if (!(b === b)) b = 0.05; if (b < 0.02) b = 0.02; if (b > 50) b = 50;
    timerRules[name].push({ kind: 'nano', needle: pattern(needle), delay: (delay === undefined || delay === '') ? '1000' : delay, boost: b });
    wrapTimer(name);
  }

  // ---- no-window-open-if: window.open called by script gets nothing (or a harmless stand-in window: a script that checks the result must not fall back to redirecting the page)
  var woifRules = [], woifWrapped = false;
  function standIn() {
    var w = {
      closed: false, close: noop, focus: noop, blur: noop, postMessage: noop, open: noop, addEventListener: noop, removeEventListener: noop,
      location: { href: 'about:blank', assign: noop, replace: noop, reload: noop },
      document: { write: noop, writeln: noop, close: noop, open: noop, title: '', body: {}, documentElement: {} }
    };
    w.top = w.parent = w.self = w.window = w;
    return w;
  }
  function noWindowOpen(needle, delay, decoy) {
    woifRules.push({ p: pattern(needle), delay: parseFloat(delay) || 0, decoy: decoy });
    if (woifWrapped || typeof window.open !== 'function') return;
    woifWrapped = true;
    var orig = window.open;
    window.open = function () {
      try {
        var hay = Array.prototype.join.call(arguments, ' ');
        for (var i = 0; i < woifRules.length; i++) {
          var r = woifRules[i];
          if (r.delay > 0 && (nDate.now() - started) / 1000 > r.delay) continue;
          if (hit(r.p, hay)) return r.decoy === 'null' ? null : standIn();
        }
      } catch (e) { /* ignore */ }
      return orig.apply(this, arguments);
    };
  }

  // ---- eval
  var evalRules = [], evalWrapped = false;
  function noEval(needle) {
    evalRules.push(pattern(needle));
    if (evalWrapped || typeof window.eval !== 'function') return;
    evalWrapped = true;
    var orig = window.eval;
    window.eval = function (s) {
      try { if (typeof s === 'string') for (var i = 0; i < evalRules.length; i++) if (hit(evalRules[i], s)) return undefined; } catch (e) { /* ignore */ }
      return orig.apply(this, arguments);
    };
  }

  // ---- requests: needles are `prop:value` pairs split on spaces; a bare word is the address
  var PROPS = { url: 1, method: 1, body: 1, mode: 1, credentials: 1, cache: 1, redirect: 1, referrer: 1, referrerpolicy: 1, integrity: 1, keepalive: 1, priority: 1 };
  function parseNeedles(s) {
    var out = [], toks = String(s || '').trim().split(/\s+/);
    for (var i = 0; i < toks.length; i++) {
      var t = toks[i]; if (!t) continue;
      var c = t.indexOf(':'), k = c > 0 ? t.slice(0, c).toLowerCase() : '';
      if (k && PROPS[k]) out.push({ k: k, p: pattern(t.slice(c + 1)) });
      else out.push({ k: 'url', p: pattern(t) });
    }
    return out;
  }
  function allHit(needles, values) {
    for (var i = 0; i < needles.length; i++) {
      var v = values[needles[i].k];
      if (v === undefined) return false;
      if (!hit(needles[i].p, String(v))) return false;
    }
    return true;
  }
  function absUrl(u) { try { return new URL(String(u), location.href).href; } catch (e) { return String(u); } }
  var fetchRules = [], fetchWrapped = false;
  function fetchValues(input, init) {
    var o = isObj(input) && typeof input !== 'function' ? input : {};
    init = isObj(init) ? init : {};
    var v = { url: absUrl(typeof input === 'string' ? input : (o.url !== undefined ? o.url : input)) };
    v.method = String(init.method || o.method || 'GET').toUpperCase();
    var names = ['body', 'mode', 'credentials', 'cache', 'redirect', 'referrer', 'integrity', 'keepalive', 'priority'];
    for (var i = 0; i < names.length; i++) { var x = init[names[i]] !== undefined ? init[names[i]] : o[names[i]]; if (x !== undefined) v[names[i]] = x; }
    if (init.referrerPolicy !== undefined || o.referrerPolicy !== undefined) v.referrerpolicy = init.referrerPolicy !== undefined ? init.referrerPolicy : o.referrerPolicy;
    return v;
  }
  function emptyResponse(url) {
    var r = new nResponse('{}', { status: 200, statusText: 'OK' });
    try { defProp(r, 'url', { value: url }); } catch (e) { /* ignore */ }
    return r;
  }
  function noFetch(needles) {
    fetchRules.push({ n: parseNeedles(needles), prune: null });
    wrapFetch();
  }
  function wrapFetch() {
    if (fetchWrapped || typeof nFetch !== 'function') return;
    fetchWrapped = true;
    window.fetch = function (input, init) {
      var vals = null, edits = null;
      try {
        vals = fetchValues(input, init);
        for (var i = 0; i < fetchRules.length; i++) {
          var r = fetchRules[i];
          if (!allHit(r.n, vals)) continue;
          if (!r.prune && !r.re && !r.edit) return Promise.resolve(emptyResponse(vals.url));
          (edits = edits || []).push(r);
        }
      } catch (e) { /* ignore */ }
      var p = nFetch.apply(this, arguments);
      if (!edits) return p;
      return p.then(function (resp) {
        try {
          return resp.clone().text().then(function (t) {
            var out = editText(t, edits);
            if (out === null) return resp;
            var nr = new nResponse(out, { status: resp.status, statusText: resp.statusText, headers: resp.headers });
            try { defProp(nr, 'url', { value: resp.url }); } catch (e) { /* ignore */ }
            return nr;
          }, function () { return resp; });
        } catch (e) { return resp; }
      });
    };
  }
  // the text of an answer through every rule that matched its request, in turn: a prune rule cuts JSON properties, a replace rule rewrites text. null = nothing changed.
  function editText(t, rules) {
    var cur = t, changed = false;
    for (var i = 0; i < rules.length; i++) {
      var r = rules[i];
      try {
        if (r.re) { var nt = cur.replace(r.re, r.rep); if (nt !== cur) { cur = nt; changed = true; } }
        else if (r.prune) { var obj = nParse(cur); if (pruneObject(obj, r.prune.paths, r.prune.needles)) { cur = nStringify(obj); changed = true; } }
        else if (r.edit) { var eo = nParse(cur); if (jqRun(eo, r.edit)) { cur = nStringify(eo); changed = true; } }
      } catch (e) { /* this rule does not suit this text: the next one may */ }
    }
    return changed ? cur : null;
  }
  var xhrRules = [], xhrWrapped = false;
  function noXhr(needles) {
    xhrRules.push({ n: parseNeedles(needles), prune: null });
    wrapXhr();
  }
  function wrapXhr() {
    if (xhrWrapped || !XHR || !xhrOpen || !xhrSend) return;
    xhrWrapped = true;
    XHR.prototype.open = function (method, url) {
      try { this.__kst = { method: String(method || 'GET').toUpperCase(), url: absUrl(url) }; } catch (e) { /* ignore */ }
      return xhrOpen.apply(this, arguments);
    };
    XHR.prototype.send = function (body) {
      try {
        var info = this.__kst;
        if (info) {
          var vals = { url: info.url, method: info.method };
          if (body !== undefined && body !== null && typeof body === 'string') vals.body = body;
          var edits = null;
          for (var i = 0; i < xhrRules.length; i++) {
            var r = xhrRules[i];
            if (!allHit(r.n, vals)) continue;
            if (!r.prune && !r.re && !r.edit) { fakeXhr(this, info.url); return undefined; }
            (edits = edits || []).push(r);
          }
          if (edits) editedXhr(this, edits);
        }
      } catch (e) { /* ignore */ }
      return xhrSend.apply(this, arguments);
    };
  }
  function fakeXhr(x, url) {
    var rt = x.responseType || '';
    var resp = rt === 'json' ? {} : (rt === '' || rt === 'text' ? '{}' : null);
    function def(n, v) { try { defProp(x, n, { value: v, configurable: true }); } catch (e) { /* ignore */ } }
    nSetTimeout(function () {
      def('readyState', 4); def('status', 200); def('statusText', 'OK'); def('responseURL', url); def('response', resp);
      if (rt === '' || rt === 'text') def('responseText', '{}');
      var names = ['readystatechange', 'load', 'loadend'];
      for (var i = 0; i < names.length; i++) { try { x.dispatchEvent(new Event(names[i])); } catch (e) { /* ignore */ } }
    }, 1);
  }
  function editedXhr(x, rules) { // the answer is edited when it is read, whatever the order of the page's own handlers
    var cache = null, done = false;
    function build() {
      if (done) return;
      done = true;
      try {
        var rt = x.responseType || '', raw = rt === 'json' ? x.response : (rt === '' || rt === 'text' ? x.responseText : null);
        if (raw === null || raw === undefined) return;
        var out = editText(typeof raw === 'string' ? raw : nStringify(raw), rules);
        if (out === null) return;
        cache = { text: out, obj: null };
        if (rt === 'json') { try { cache.obj = nParse(out); } catch (e) { cache = null; } } // an edit that breaks the JSON leaves the answer as it was
      } catch (e) { /* ignore */ }
    }
    var protoDesc = (function () { var p = XHR.prototype; return { r: getDesc(p, 'response'), t: getDesc(p, 'responseText') }; })();
    try {
      defProp(x, 'response', { configurable: true, get: function () { if (x.readyState === 4) build(); var v = protoDesc.r.get.call(x); return (cache && x.responseType === 'json') ? cache.obj : (cache && (x.responseType === '' || x.responseType === 'text') ? cache.text : v); } });
      defProp(x, 'responseText', { configurable: true, get: function () { if (x.readyState === 4) build(); var v = protoDesc.t.get.call(x); return cache ? cache.text : v; } });
    } catch (e) { /* ignore */ }
  }

  // ---- json-prune: properties named by paths are removed from parsed JSON (when the needle paths are all there)
  function walk(node, toks, i, del) {
    if (!isObj(node)) return false;
    var t = toks[i], last = i === toks.length - 1, found = false, ks, k;
    if (t === '*' || t === '[-]' || t === '[]') {
      if (t !== '*' && !Array.isArray(node)) return false;
      try { ks = keys(node); } catch (e) { return false; }
      for (k = 0; k < ks.length; k++) {
        if (last) { found = true; if (del) { try { delete node[ks[k]]; } catch (e) { /* ignore */ } } }
        else if (walk(node[ks[k]], toks, i + 1, del)) found = true;
      }
      return found;
    }
    if (!hasOwn.call(node, t)) return false;
    if (last) { if (del) { try { delete node[t]; } catch (e) { /* ignore */ } } return true; }
    return walk(node[t], toks, i + 1, del);
  }
  function pathsOf(s) {
    var out = [], parts = String(s || '').trim().split(/\s+/);
    for (var i = 0; i < parts.length; i++) if (parts[i]) out.push(parts[i].split('.'));
    return out;
  }
  function pruneObject(obj, paths, needles) {
    if (!isObj(obj)) return false;
    var i;
    for (i = 0; i < needles.length; i++) if (!walk(obj, needles[i], 0, false)) return false;
    var any = false;
    for (i = 0; i < paths.length; i++) if (walk(obj, paths[i], 0, true)) any = true;
    return any;
  }
  var pruneRules = [], parseWrapped = false;
  // one JSON.parse wrapper for every rule that works on parsed JSON: prune rules (RX: json-prune) and edit rules (RX3-Y1: json-edit)
  function ensureParseHook() {
    if (parseWrapped) return;
    parseWrapped = true;
    JSON.parse = function (src) {
      var r = nParse.apply(this, arguments);
      try {
        var st = null;
        for (var i = 0; i < pruneRules.length; i++) {
          var p = pruneRules[i];
          if (p.stack) { if (st === null) { try { st = new Error().stack || ''; } catch (e) { st = ''; } } if (!hit(p.stack, st)) continue; }
          if (p.edit) {
            if (typeof src === 'string') { if (p.first && src.indexOf(p.first) < 0) continue; if (p.matches && !hit(p.matches, src)) continue; } // cheap checks on the text first: most JSON on a page is none of our business
            else if (p.matches) continue;
            jqRun(r, p.edit);
          } else pruneObject(r, p.paths, p.needles);
        }
      } catch (e) { /* ignore */ }
      return r;
    };
  }
  function jsonPrune(prune, needle) {
    var ex = pairs(arguments, 2);
    pruneRules.push({ paths: pathsOf(prune), needles: pathsOf(needle), stack: ex.stack ? pattern(ex.stack) : null });
    ensureParseHook();
  }
  function jsonPruneFetch(prune, needle) {
    var ex = pairs(arguments, 2);
    var n = ex.propsToMatch ? parseNeedles(ex.propsToMatch) : [];
    fetchRules.push({ n: n, prune: { paths: pathsOf(prune), needles: pathsOf(needle) } });
    wrapFetch();
  }
  function jsonPruneXhr(prune, needle) {
    var ex = pairs(arguments, 2);
    var n = ex.propsToMatch ? parseNeedles(ex.propsToMatch) : [];
    xhrRules.push({ n: n, prune: { paths: pathsOf(prune), needles: pathsOf(needle) } });
    wrapXhr();
  }

  // ---- the JSON edit language (RX3-Y1): `json-edit`, `json-edit-fetch-response`, `json-edit-xhr-response` and their `trusted-` forms apply a QUERY to parsed JSON.
  // Written from the behaviour the public uBlock Origin wiki documents and from the shapes of the real list lines (88 of them in the lists the app downloads); nothing is copied from it.
  //   steps    ..name  a key at any depth below     .name  a child     .*  every child     ./regex/  children whose key matches     [?test]  keep only the nodes that pass the test
  //   test     .a.b  has that property     .a.b=="x"  equals (text, number, true, false, null)     *=  contains     ^=  starts with     $=  ends with     =/re/flags  matches
  //            (the path of a test may use ..name and .*: any value found counts)
  //   action   none: delete the node (an array item is cut out)     =<json>  set it     +=<object>  merge into it     =repl({"regex":"..","replacement":".."})  regex-replace in a text
  //   a leading `$` and `v2:` are accepted. Only the trusted forms may set, merge or replace; the others only delete. Work is bounded (nodes visited, depth); nothing here throws into the page.
  var JQ_MAXN = 120000, JQ_DEPTH = 60;
  function jqIsKeyEnd(s, i) {
    var c = s.charAt(i);
    return c === '' || c === '.' || c === '[' || c === '=' || ((c === '+' || c === '*' || c === '^' || c === '$') && s.charAt(i + 1) === '=');
  }
  // one key at s[i]: `*` (every child), `/regex/flags`, or a plain name -> { any } | { re } | { k }, and where it ends
  function jqKey(s, i) {
    var c = s.charAt(i);
    if (c === '*') return { any: true, end: i + 1 };
    if (c === '/') {
      var j = i + 1;
      while (j < s.length) { if (s.charAt(j) === '\\') { j += 2; continue; } if (s.charAt(j) === '/') break; j++; }
      if (j >= s.length) return null;
      var f = j + 1, fl = '';
      while (f < s.length && /[a-z]/.test(s.charAt(f))) { fl += s.charAt(f); f++; }
      var re; try { re = new nRegExp(s.slice(i + 1, j), fl.replace(/[gy]/g, '')); } catch (e) { return null; }
      return { re: re, end: f };
    }
    var k = i;
    while (k < s.length && !jqIsKeyEnd(s, k)) k++;
    return k === i ? null : { k: s.slice(i, k), end: k };
  }
  function jqMatchBracket(s, i) { // the `]` that closes the `[` at s[i], skipping quoted text and a /regex/ right after `=`
    var depth = 0, q = '';
    for (var j = i; j < s.length; j++) {
      var c = s.charAt(j);
      if (q) { if (c === '\\') { j++; continue; } if (c === q) q = ''; continue; }
      if (c === '"' || c === "'") { q = c; continue; }
      if (c === '/' && s.charAt(j - 1) === '=') {
        var k = j + 1;
        while (k < s.length) { if (s.charAt(k) === '\\') { k += 2; continue; } if (s.charAt(k) === '/') break; k++; }
        if (k >= s.length) return -1;
        j = k; continue;
      }
      if (c === '[') depth++;
      else if (c === ']') { depth--; if (depth === 0) return j; }
    }
    return -1;
  }
  function jqValue(t) { // what stands after an operator: "text", 'text', /re/flags, true, false, null, a number, or a bare word
    t = t.trim();
    var c = t.charAt(0), n = t.length;
    if ((c === '"' || c === "'") && n >= 2 && t.charAt(n - 1) === c) {
      if (c === '"') { try { return { v: nParse(t) }; } catch (e) { return { v: t.slice(1, n - 1) }; } }
      return { v: t.slice(1, n - 1).replace(/\\'/g, "'") };
    }
    var m = /^\/(.+)\/([a-z]*)$/.exec(t);
    if (m) { try { return { re: new nRegExp(m[1], m[2].replace(/[gy]/g, '')) }; } catch (e) { return null; } }
    if (t === 'true') return { v: true };
    if (t === 'false') return { v: false };
    if (t === 'null') return { v: null };
    if (/^-?\d+(\.\d+)?$/.test(t)) return { v: parseFloat(t) };
    return t === '' ? null : { v: t };
  }
  function jqPredicate(t) { // the text between `[?` and `]`
    var i = 0, steps = [];
    while (i < t.length && t.charAt(i) === '.') {
      var deep = t.charAt(i + 1) === '.';
      i += deep ? 2 : 1;
      var r = jqKey(t, i);
      if (!r) return null;
      steps.push({ t: deep ? 'deep' : 'child', k: r.k, re: r.re, any: r.any });
      i = r.end;
    }
    if (!steps.length) return null;
    if (i >= t.length) return { path: steps, op: '' };
    var two = t.substr(i, 2), op;
    if (two === '==' || two === '*=' || two === '^=' || two === '$=') { op = two; i += 2; }
    else if (t.charAt(i) === '=') { op = '='; i += 1; }
    else return null;
    var val = jqValue(t.slice(i));
    return val ? { path: steps, op: op, val: val } : null;
  }
  function jqJson(t) { try { return { v: nParse(String(t).trim()) }; } catch (e) { return null; } }
  function jqAction(t) { // t starts at `=` or `+=`
    var m;
    if (t.indexOf('+=') === 0) { var o = jqJson(t.slice(2)); return (o && isObj(o.v) && !Array.isArray(o.v)) ? { k: 'merge', v: o.v } : null; }
    var rest = t.slice(1).trim();
    if ((m = /^repl\(([\s\S]*)\)$/.exec(rest))) {
      var spec = jqJson(m[1]);
      if (!spec || !isObj(spec.v) || typeof spec.v.regex !== 'string') return null;
      var rm = /^\/(.+)\/([a-z]*)$/.exec(spec.v.regex), re;
      try { re = rm ? new nRegExp(rm[1], rm[2]) : new nRegExp(spec.v.regex); } catch (e) { return null; }
      return { k: 'repl', re: re, rep: String(spec.v.replacement === undefined ? '' : spec.v.replacement) };
    }
    var lit = jqJson(rest);
    return lit ? { k: 'set', v: lit.v } : null;
  }
  function jqCompile(q, trusted) { // the query as steps and an action, or null when it is not understood (or changes values without a trusted list)
    try {
      var s = String(q), i = 0, n = s.length, steps = [], act = null;
      if (s.indexOf('v2:') === 0) i = 3;
      if (s.charAt(i) === '$') i++;
      while (i < n) {
        var c = s.charAt(i);
        if (c === '.') {
          var deep = s.charAt(i + 1) === '.';
          i += deep ? 2 : 1;
          var r = jqKey(s, i);
          if (!r) return null;
          steps.push({ t: deep ? 'deep' : 'child', k: r.k, re: r.re, any: r.any });
          i = r.end;
        } else if (c === '[') {
          var j = jqMatchBracket(s, i);
          if (j < 0 || s.charAt(i + 1) !== '?') return null;
          var pr = jqPredicate(s.slice(i + 2, j));
          if (!pr) return null;
          steps.push({ t: 'filter', p: pr });
          i = j + 1;
        } else if (c === '=' || (c === '+' && s.charAt(i + 1) === '=')) {
          act = jqAction(s.slice(i));
          if (!act) return null;
          i = n;
        } else return null;
      }
      if (!steps.length && !act) return null;
      if (act && !trusted) return null;
      return { steps: steps, act: act };
    } catch (e) { return null; }
  }
  function jqKeyOk(st, key) { return st.any ? true : (st.re ? st.re.test(key) : key === st.k); }
  function jqChildren(rec, st, out, ctx) {
    var v = rec.v;
    if (!isObj(v) || typeof v === 'function') return;
    var ks; try { ks = keys(v); } catch (e) { return; }
    for (var i = 0; i < ks.length; i++) { if (--ctx.n < 0) return; if (jqKeyOk(st, ks[i])) out.push({ p: v, k: ks[i], v: v[ks[i]] }); }
  }
  function jqDeep(v, st, out, ctx, depth) {
    if (!isObj(v) || typeof v === 'function' || depth > JQ_DEPTH || ctx.n < 0) return;
    var ks; try { ks = keys(v); } catch (e) { return; }
    for (var i = 0; i < ks.length; i++) {
      if (--ctx.n < 0) return;
      var key = ks[i], child = v[key];
      if (jqKeyOk(st, key)) out.push({ p: v, k: key, v: child });
      jqDeep(child, st, out, ctx, depth + 1);
    }
  }
  function jqStep(recs, st, ctx) {
    var out = [];
    for (var i = 0; i < recs.length && ctx.n >= 0; i++) {
      var r = recs[i];
      if (st.t === 'child') jqChildren(r, st, out, ctx);
      else if (st.t === 'deep') jqDeep(r.v, st, out, ctx, 0);
      else if (jqTest(r.v, st.p, ctx)) out.push(r);
    }
    return out;
  }
  function jqCompare(v, pr) {
    var a = pr.val;
    if (a.re) return a.re.test(typeof v === 'string' ? v : String(v));
    if (pr.op === '==' || pr.op === '=') return v === a.v;
    var s = typeof v === 'string' ? v : ((v === null || v === undefined || typeof v === 'object') ? '' : String(v)), t = String(a.v);
    if (pr.op === '*=') return s.indexOf(t) >= 0;
    if (pr.op === '^=') return s.indexOf(t) === 0;
    return t.length <= s.length && s.lastIndexOf(t) === s.length - t.length; // $=
  }
  function jqTest(node, pr, ctx) {
    var recs = [{ p: null, k: null, v: node }];
    for (var i = 0; i < pr.path.length && recs.length; i++) recs = jqStep(recs, pr.path[i], ctx);
    if (!recs.length) return false;
    if (!pr.op) return true;
    for (var j = 0; j < recs.length; j++) if (jqCompare(recs[j].v, pr)) return true;
    return false;
  }
  function jqClone(v) { try { return isObj(v) ? nParse(nStringify(v)) : v; } catch (e) { return v; } }
  function jqRun(root, q) { // true when something changed
    if (!isObj(root) || !q) return false;
    var ctx = { n: JQ_MAXN }, recs = [{ p: null, k: null, v: root }], i, r, changed = false;
    for (i = 0; i < q.steps.length && recs.length; i++) recs = jqStep(recs, q.steps[i], ctx);
    var act = q.act;
    for (i = recs.length - 1; i >= 0; i--) { // from the last match to the first: cutting array items out never shifts the ones still to come
      r = recs[i];
      try {
        if (!act) {
          if (r.p === null) continue;
          if (Array.isArray(r.p)) { var at = +r.k; if (r.p[at] === r.v) { r.p.splice(at, 1); changed = true; } }
          else if (hasOwn.call(r.p, r.k)) { delete r.p[r.k]; changed = true; }
        } else if (act.k === 'set') {
          if (r.p !== null && r.p[r.k] !== act.v) { r.p[r.k] = jqClone(act.v); changed = true; }
        } else if (act.k === 'merge') {
          if (isObj(r.v) && !Array.isArray(r.v)) { var mk = keys(act.v); for (var m = 0; m < mk.length; m++) r.v[mk[m]] = jqClone(act.v[mk[m]]); changed = true; }
        } else if (act.k === 'repl') {
          if (r.p !== null && typeof r.v === 'string') { var nv = r.v.replace(act.re, act.rep); if (nv !== r.v) { r.p[r.k] = nv; changed = true; } }
        }
      } catch (e) { /* a frozen or odd node: the next one may be fine */ }
    }
    return changed;
  }
  // (query, [propsToMatch | name, value ...]): the second argument on its own is the request needle (an empty one is skipped); `propsToMatch`, `matches` and `stack` come as name, value pairs
  function editArgs(a) {
    var o = { q: String(a[0] === undefined ? '' : a[0]), props: '', matches: '' }, i = 1;
    if (a.length > 1) { var x = String(a[1]); if (x !== 'propsToMatch' && x !== 'matches' && x !== 'stack') { o.props = x; i = 2; } }
    for (; i < a.length; i += 2) {
      var nm = String(a[i]), val = i + 1 < a.length ? String(a[i + 1]) : '';
      if (nm === 'propsToMatch') o.props = val; else if (nm === 'matches') o.matches = val;
    }
    return o;
  }
  function firstKey(q) { for (var i = 0; i < q.steps.length; i++) { var st = q.steps[i]; if ((st.t === 'child' || st.t === 'deep') && st.k) return st.k; } return ''; }
  function jsonEdit(trusted) { // JSON.parse results; `matches` is a needle for the TEXT that was parsed
    return function () {
      var o = editArgs(arguments), q = jqCompile(o.q, trusted);
      if (!q) return;
      pruneRules.push({ edit: q, matches: o.matches ? pattern(o.matches) : null, first: firstKey(q), stack: null });
      ensureParseHook();
    };
  }
  function jsonEditFetch(trusted) { // the answer to a matching fetch; a rule without a request needle would parse every answer of the page, so it is not accepted
    return function () {
      var o = editArgs(arguments), q = jqCompile(o.q, trusted);
      if (!q || !o.props) return;
      fetchRules.push({ n: parseNeedles(o.props), edit: q });
      wrapFetch();
    };
  }
  function jsonEditXhr(trusted) {
    return function () {
      var o = editArgs(arguments), q = jqCompile(o.q, trusted);
      if (!q || !o.props) return;
      xhrRules.push({ n: parseNeedles(o.props), edit: q });
      wrapXhr();
    };
  }
  // trusted-edit-inbound-object (path, index, query): what the page hands to a function (JSON.stringify: the body of a request it builds) is edited just before the function runs
  function editInbound(path, idx, query) {
    var parts = chainOf(path), q = jqCompile(query, true), at = parseInt(idx, 10) || 0;
    if (!parts || !q || at < 0 || at > 9) return;
    var owner = window, i;
    for (i = 0; i < parts.length - 1; i++) { try { owner = owner[parts[i]]; } catch (e) { return; } if (!isObj(owner)) return; }
    var prop = parts[parts.length - 1], orig;
    try { orig = owner[prop]; } catch (e) { return; }
    if (typeof orig !== 'function') return;
    var wrapped = function () {
      try { var a = arguments[at]; if (isObj(a)) jqRun(a, q); } catch (e) { /* never break the call */ }
      return orig.apply(this, arguments);
    };
    try { owner[prop] = wrapped; } catch (e) { /* ignore */ }
  }
  // trusted-prevent-dom-bypass (method, prop): a frame the page inserts with the method cannot hand the page an untouched `fetch` / `Request` (a fresh frame has its own, which would skip every fix above)
  function preventDomBypass(path, prop) {
    var parts = chainOf(path);
    if (!parts || !prop) return;
    var owner = window, i;
    for (i = 0; i < parts.length - 1; i++) { try { owner = owner[parts[i]]; } catch (e) { return; } if (!isObj(owner)) return; }
    var name = parts[parts.length - 1], orig;
    try { orig = owner[name]; } catch (e) { return; }
    if (typeof orig !== 'function') return;
    function fix(node) {
      try {
        var list = (node && node.tagName === 'IFRAME') ? [node] : (node && node.querySelectorAll ? node.querySelectorAll('iframe') : []);
        for (var j = 0; j < list.length && j < 8; j++) { var w = list[j].contentWindow; if (w && typeof window[prop] === 'function') w[prop] = window[prop]; }
      } catch (e) { /* a frame of another origin: its own fetch is not ours to give away */ }
    }
    try { owner[name] = function (node) { var r = orig.apply(this, arguments); fix(node); return r; }; } catch (e) { /* ignore */ }
  }

  // ---- DOM work: remove-attr / remove-class / remove-node-text / replace-node-text
  function whenReady(fn) {
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', fn, { once: true }); else fn();
  }
  function watch(run, stay) { // once the page is parsed and again at load; with `stay` also whenever it changes (at most every 150 ms)
    var pending = false;
    function soon() { if (pending) return; pending = true; nSetTimeout(function () { pending = false; try { run(); } catch (e) { /* ignore */ } }, 150); }
    whenReady(function () { try { run(); } catch (e) { /* ignore */ } });
    window.addEventListener('load', function () { try { run(); } catch (e) { /* ignore */ } }, { once: true });
    if (stay && window.MutationObserver) whenReady(function () { try { new MutationObserver(soon).observe(document, { childList: true, subtree: true, attributes: true }); } catch (e) { /* ignore */ } });
  }
  function removeAttr(attrs, selector, behaviour) {
    var names = String(attrs).split('|').filter(Boolean);
    if (!names.length) return;
    var sel = selector || names.map(function (a) { return '[' + a + ']'; }).join(',');
    watch(function () {
      var list = document.querySelectorAll(sel);
      for (var i = 0; i < list.length && i < 2000; i++) for (var j = 0; j < names.length; j++) list[i].removeAttribute(names[j]);
    }, String(behaviour || '').indexOf('stay') >= 0);
  }
  function removeClass(classes, selector, behaviour) {
    var names = String(classes).split('|').filter(Boolean);
    if (!names.length) return;
    var sel = selector || names.map(function (c) { return '.' + c; }).join(',');
    watch(function () {
      var list = document.querySelectorAll(sel);
      for (var i = 0; i < list.length && i < 2000; i++) for (var j = 0; j < names.length; j++) list[i].classList.remove(names[j]);
    }, String(behaviour || '').indexOf('stay') >= 0);
  }

  // Text of nodes: parser-made scripts have usually run before anything can change them (Gecko's `beforescriptexecute` is used when it exists); scripts and styles that the page adds are changed BEFORE
  // they are inserted (hooks on the insertion methods), text that is already in the page and what is added later is handled by an observer.
  var nodeRules = [], nodeHooked = false;
  function nodeEdit(spec) {
    nodeRules.push(spec);
    if (nodeHooked) return;
    nodeHooked = true;
    function procNode(n) {
      if (!n || !n.nodeName) return;
      var name = n.nodeName;
      for (var i = 0; i < nodeRules.length; i++) {
        var r = nodeRules[i];
        if (r.done || !r.name.test(name)) continue;
        var txt = n.nodeType === 3 ? n.nodeValue : n.textContent;
        if (typeof txt !== 'string' || !txt) continue;
        if (r.includes && !hit(r.includes, txt)) continue;
        if (r.excludes && hit(r.excludes, txt)) continue;
        var out;
        if (r.remove) { if (!hit(r.needle, txt)) continue; out = ''; }
        else {
          if (!r.re) continue;
          r.re.lastIndex = 0;
          if (!r.re.test(txt)) continue;
          r.re.lastIndex = 0;
          out = txt.replace(r.re, r.replacement);
          if (r.sedCount > 0 && ++r.hits >= r.sedCount) r.done = true;
        }
        if (out === txt) continue;
        try { if (n.nodeType === 3) n.nodeValue = out; else n.textContent = out; } catch (e) { /* ignore */ }
        txt = out;
      }
    }
    function procTree(root) {
      if (!root) return;
      procNode(root);
      if (root.nodeType === 1 || root.nodeType === 11) {
        var list = root.querySelectorAll ? root.querySelectorAll('script,style') : [];
        for (var i = 0; i < list.length && i < 400; i++) procNode(list[i]);
      }
    }
    var protoN = window.Node && Node.prototype;
    function hook(proto, name, pick) {
      if (!proto) return;
      var orig = proto[name];
      if (typeof orig !== 'function') return;
      proto[name] = function () {
        try { for (var i = 0; i < arguments.length; i++) { var a = arguments[i]; if (a && typeof a === 'object' && a.nodeType) procTree(a); } } catch (e) { /* ignore */ }
        return orig.apply(this, arguments);
      };
    }
    hook(protoN, 'appendChild'); hook(protoN, 'insertBefore'); hook(protoN, 'replaceChild');
    hook(window.Element && Element.prototype, 'append'); hook(window.Element && Element.prototype, 'prepend');
    hook(window.Element && Element.prototype, 'before'); hook(window.Element && Element.prototype, 'after'); hook(window.Element && Element.prototype, 'insertAdjacentElement');
    try { document.addEventListener('beforescriptexecute', function (e) { procNode(e.target); }, true); } catch (e) { /* ignore */ }
    function sweep() {
      var i, list = document.querySelectorAll('script,style');
      for (i = 0; i < list.length && i < 600; i++) procNode(list[i]);
      var wantsText = false;
      for (i = 0; i < nodeRules.length; i++) if (nodeRules[i].name.test('#text')) wantsText = true;
      if (wantsText && document.createTreeWalker) {
        var w = document.createTreeWalker(document.documentElement || document, 4), n, c = 0;
        while ((n = w.nextNode()) && c++ < 4000) procNode(n);
      }
    }
    if (window.MutationObserver) {
      var obs = new MutationObserver(function (recs) {
        for (var i = 0; i < recs.length && i < 200; i++) { var add = recs[i].addedNodes; for (var j = 0; j < add.length && j < 50; j++) procTree(add[j]); }
      });
      try { obs.observe(document, { childList: true, subtree: true }); } catch (e) { /* ignore */ }
    }
    watch(sweep, false);
  }
  function nameRe(s) { try { return new nRegExp('^' + String(s).replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '$', 'i'); } catch (e) { return /^$/; } }
  function removeNodeText(nodeName, needle) {
    var ex = pairs(arguments, 2);
    nodeEdit({ name: nameRe(nodeName), remove: true, needle: pattern(needle), includes: ex.includes ? pattern(ex.includes) : (ex.condition ? pattern(ex.condition) : null), excludes: ex.excludes ? pattern(ex.excludes) : null, hits: 0, sedCount: 0 });
  }
  function replaceNodeText(nodeName, pat, replacement) {
    var ex = pairs(arguments, 3);
    nodeEdit({ name: nameRe(nodeName), remove: false, re: replacer(pat), replacement: replacement === undefined ? '' : String(replacement), includes: ex.includes ? pattern(ex.includes) : (ex.condition ? pattern(ex.condition) : null), excludes: ex.excludes ? pattern(ex.excludes) : null, hits: 0, sedCount: parseInt(ex.sedCount, 10) || 0 });
  }

  // ---- WebRTC: the page cannot learn the device's addresses through it
  function noWebrtc() {
    function make() {
      return {
        close: noop, createDataChannel: function () { return { close: noop, send: noop, addEventListener: noop, removeEventListener: noop }; },
        createOffer: function () { return Promise.resolve({}); }, createAnswer: function () { return Promise.resolve({}); },
        setLocalDescription: function () { return Promise.resolve(); }, setRemoteDescription: function () { return Promise.resolve(); },
        addIceCandidate: function () { return Promise.resolve(); }, addEventListener: noop, removeEventListener: noop, getStats: function () { return Promise.resolve([]); },
        onicecandidate: null, localDescription: null, remoteDescription: null, iceConnectionState: 'closed', connectionState: 'closed', signalingState: 'closed'
      };
    }
    var f = function () { return make(); };
    var names = ['RTCPeerConnection', 'webkitRTCPeerConnection', 'mozRTCPeerConnection'];
    for (var i = 0; i < names.length; i++) { if (names[i] in window) { try { window[names[i]] = f; } catch (e) { /* ignore */ } } }
  }

  // ---- cookies and storage that make a notice go away (the values the lists may set are limited; the `trusted-` forms are only ever sent for lists the app itself built)
  var SAFE = /^(?:0|1|2|3|true|false|t|f|yes|no|y|n|on|off|accept(?:ed)?|reject(?:ed)?|allow(?:ed)?|deny|denied|agree|disagree|enabled?|disabled?|necessary|essentials?|granted|dismiss(?:ed)?|ok|done|hide|hidden|decline[d]?|none|all|minimal|functional)$/i;
  function seconds(spec) {
    var m = /^(\d+)\s*(second|sec|s|minute|min|hour|day|week|month|year)s?$/i.exec(String(spec || '').trim());
    if (!m) { var n = parseInt(spec, 10); return n > 0 ? n : 31536000; }
    var mult = { second: 1, sec: 1, s: 1, minute: 60, min: 60, hour: 3600, day: 86400, week: 604800, month: 2592000, year: 31536000 }[m[2].toLowerCase()] || 1;
    return Math.min(parseInt(m[1], 10) * mult, 31536000 * 2);
  }
  function putCookie(name, value, path, age) {
    if (!name || /[;=\s,]/.test(name)) return;
    if (/[;\r\n]/.test(String(value))) return;
    try { document.cookie = name + '=' + encodeURIComponent(String(value)).replace(/%20/g, ' ') + '; path=' + (path && path.charAt(0) === '/' ? path : '/') + '; max-age=' + age + '; SameSite=Lax'; } catch (e) { /* ignore */ }
  }
  function setCookie(name, value, path) { if (SAFE.test(String(value))) putCookie(name, value, path, 31536000); }
  function trustedSetCookie(name, value, expires, path) { if (String(value).length <= 300) putCookie(name, value, path, seconds(expires)); }
  function removeCookie(needle) {
    var np = pattern(needle);
    function run() {
      var all = document.cookie ? document.cookie.split(/;\s*/) : [];
      var host = location.hostname, hosts = [host], p = host.split('.');
      for (var i = 1; i < p.length - 1; i++) hosts.push(p.slice(i).join('.'));
      for (var c = 0; c < all.length; c++) {
        var nm = all[c].split('=')[0];
        if (!nm || !hit(np, nm)) continue;
        for (var h = 0; h < hosts.length; h++) {
          try { document.cookie = nm + '=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/; domain=' + hosts[h]; } catch (e) { /* ignore */ }
        }
        try { document.cookie = nm + '=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/'; } catch (e) { /* ignore */ }
      }
    }
    watch(run, false);
  }
  function storageItem(which, key, value, trusted) {
    try {
      var st = window[which];
      if (!st || !key) return;
      if (value === '$remove$') { st.removeItem(key); return; }
      var v = String(value);
      if (!trusted && !SAFE.test(v) && v !== '') return;
      if (v.length > 300) return;
      st.setItem(key, v);
    } catch (e) { /* storage may be off (a sandboxed frame) */ }
  }


  // ---- trusted-replace-fetch-response / trusted-replace-xhr-response: the TEXT of the answer to a matching request is rewritten (a pattern replaced everywhere, `$1` ... work in the replacement).
  // How current lists cut the ad fields out of a video player's data, or a feed's sponsored entries. The third argument says which requests (an address needle or `prop:value` pairs, as no-fetch-if).
  function replaceFetch(pat, rep, props) {
    var re = replacer(pat);
    if (!re) return;
    fetchRules.push({ n: parseNeedles(props), prune: null, re: re, rep: rep === undefined ? '' : String(rep) });
    wrapFetch();
  }
  function replaceXhr(pat, rep, props) {
    var re = replacer(pat);
    if (!re) return;
    xhrRules.push({ n: parseNeedles(props), prune: null, re: re, rep: rep === undefined ? '' : String(rep) });
    wrapXhr();
  }

  // ---- nobab (also the "fab" family): BlockAdBlock / FuckAdBlock answer "no ad blocker" to everything that asks. The names are accessors the page can write to but not replace.
  function noBab() {
    function Bab() {
      var t = this;
      t.d = []; t.n = []; t.ok = false;
      nSetTimeout(function () { t.check(); }, 0);
    }
    Bab.prototype.onDetected = function (f) { if (typeof f === 'function') this.d.push(f); return this; };
    Bab.prototype.onNotDetected = function (f) {
      if (typeof f !== 'function') return this;
      if (this.ok) nSetTimeout(f, 0); else this.n.push(f);
      return this;
    };
    Bab.prototype.on = function (detected, f) { return detected ? this.onDetected(f) : this.onNotDetected(f); };
    Bab.prototype.check = function () {
      var t = this, list = t.n;
      t.ok = true; t.n = [];
      nSetTimeout(function () { for (var i = 0; i < list.length; i++) { try { list[i](); } catch (e) { /* ignore */ } } }, 0);
      return true;
    };
    Bab.prototype.emitEvent = function () { return this.check(); };
    Bab.prototype.clearEvent = function () { this.d = []; this.n = []; };
    Bab.prototype.setOption = function () { return this; };
    var inst = new Bab();
    function fixed(name, value) {
      try { defProp(window, name, { configurable: true, get: function () { return value; }, set: noop }); } catch (e) { /* ignore */ }
    }
    fixed('BlockAdBlock', Bab); fixed('FuckAdBlock', Bab); fixed('SniffAdBlock', Bab);
    fixed('blockAdBlock', inst); fixed('fuckAdBlock', inst); fixed('sniffAdBlock', inst);
  }

  // ---- popads-dummy: the globals a pop-under network's script looks for exist and are empty
  function popadsDummy() {
    var names = ['PopAds', 'popns'];
    for (var i = 0; i < names.length; i++) {
      try { delete window[names[i]]; } catch (e) { /* ignore */ }
      (function (n) { try { defProp(window, n, { configurable: true, get: function () { return {}; }, set: noop }); } catch (e) { /* ignore */ } })(names[i]);
    }
  }

  // ---- href-sanitizer: a link that goes through a redirector (`?url=<target>`) points at the target itself. source: `text` (the link text), `?name` / `&name` (a query parameter), optional `-base64` / `-uri`
  function hrefSanitizer(selector, source) {
    var spec = String(source === undefined || source === '' ? 'text' : source).trim().split(/\s+/), src = spec[0] || 'text';
    var b64 = spec.indexOf('-base64') >= 0, uri = spec.indexOf('-uri') >= 0;
    var nURL = window.URL;
    function target(a) {
      var v = '';
      try {
        if (src === 'text') v = String(a.textContent || '').trim();
        else if (src.charAt(0) === '?' || src.charAt(0) === '&') v = new nURL(a.href, location.href).searchParams.get(src.slice(1)) || '';
        else return null;
        if (b64) v = window.atob(v.replace(/-/g, '+').replace(/_/g, '/'));
        if (uri) v = decodeURIComponent(v);
        if (!v) return null; // nothing to point at (an address that was sanitized already has no parameter any more): never "the empty address", which would be this very page
        var u = new nURL(v, location.href);
        return (u.protocol === 'http:' || u.protocol === 'https:') ? u.href : null; // only ever an ordinary web address
      } catch (e) { return null; }
    }
    function run() {
      var list;
      try { list = document.querySelectorAll(selector); } catch (e) { return; }
      for (var i = 0; i < list.length && i < 400; i++) {
        var a = list[i];
        if (!a || a.tagName !== 'A') continue;
        var t = target(a);
        if (t && a.href !== t) { try { a.setAttribute('href', t); } catch (e) { /* ignore */ } }
      }
    }
    var pending = false;
    function soon() { if (pending) return; pending = true; nSetTimeout(function () { pending = false; run(); }, 50); }
    whenReady(function () {
      run();
      try { new MutationObserver(soon).observe(document.documentElement, { childList: true, subtree: true, attributes: true, attributeFilter: ['href'] }); } catch (e) { /* ignore */ }
    });
  }

  // ---- the table of calls (canonical names, see ScriptletRules.kt)
  var LIB = {
    'set': setConstant, 'aopr': abortRead, 'aopw': abortWrite, 'acs': abortScript, 'aost': abortStack,
    'aeld': aeld,
    'nostif': function (n, d) { noTimer('setTimeout', n, d); }, 'nosiif': function (n, d) { noTimer('setInterval', n, d); },
    'nano-stb': function (n, d, b) { nanoTimer('setTimeout', n, d, b); }, 'nano-sib': function (n, d, b) { nanoTimer('setInterval', n, d, b); },
    'nowoif': noWindowOpen,
    'noeval': function () { noEval(''); }, 'noeval-if': noEval,
    'no-fetch-if': noFetch, 'no-xhr-if': noXhr,
    'json-prune': jsonPrune, 'json-prune-fetch-response': jsonPruneFetch, 'json-prune-xhr-response': jsonPruneXhr,
    'json-edit': jsonEdit(false), 'trusted-json-edit': jsonEdit(true),
    'json-edit-fetch-response': jsonEditFetch(false), 'trusted-json-edit-fetch-response': jsonEditFetch(true),
    'json-edit-xhr-response': jsonEditXhr(false), 'trusted-json-edit-xhr-response': jsonEditXhr(true),
    'trusted-edit-inbound-object': editInbound, 'trusted-prevent-dom-bypass': preventDomBypass,
    'trusted-replace-fetch-response': replaceFetch, 'trusted-replace-xhr-response': replaceXhr,
    'nobab': noBab, 'popads-dummy': popadsDummy, 'href-sanitizer': hrefSanitizer,
    'norafif': function (n, d) { noTimer('requestAnimationFrame', n, d); },
    'ra': removeAttr, 'rc': removeClass, 'rmnt': removeNodeText, 'rpnt': replaceNodeText,
    'nowebrtc': noWebrtc,
    'set-cookie': setCookie, 'trusted-set-cookie': trustedSetCookie, 'remove-cookie': removeCookie,
    'set-local-storage-item': function (k, v) { storageItem('localStorage', k, v, false); },
    'trusted-set-local-storage-item': function (k, v) { storageItem('localStorage', k, v, true); },
    'set-session-storage-item': function (k, v) { storageItem('sessionStorage', k, v, false); }
  };

  // Every CALL runs once per document, however often and however it is sent (the early script of the site, the hand-off from content.js after the CSS answer, a page that fires our event itself).
  // A report goes out for the calls that are new: "<ran>:<threw>:<1 when no script of the page had been parsed yet>".
  var seen = {}, total = 0;
  function run(json) {
    var calls;
    try { calls = nParse(String(json)); } catch (e) { return; }
    if (!calls || typeof calls.length !== 'number') return;
    var ok = 0, bad = 0, early = 0;
    try { early = (document.readyState === 'loading' && document.scripts.length === 0) ? 1 : 0; } catch (e) { /* ignore */ }
    for (var i = 0; i < calls.length && i < 70 && total < 300; i++) {
      var c = calls[i];
      if (!c || typeof c.length !== 'number' || typeof c[0] !== 'string') { bad++; continue; }
      var key = '';
      for (var j = 0; j < c.length && j < 12; j++) key += String(c[j]) + '\u0006';
      if (hasOwn.call(seen, key)) continue;
      seen[key] = 1; total++;
      if (!hasOwn.call(LIB, c[0])) { bad++; continue; }
      try { LIB[c[0]].apply(null, Array.prototype.slice.call(c, 1)); ok++; } catch (e) { bad++; }
    }
    if (ok + bad > 0) { try { document.dispatchEvent(new CustomEvent(REPORT, { detail: ok + ':' + bad + ':' + early })); } catch (e) { /* ignore */ } }
  }
  document.addEventListener(EVENT, function (e) {
    var d = e && e.detail;
    if (typeof d !== 'string' || d.length > 40000) return; // the Kotlin side sends at most 32,000 characters per host
    run(d);
  }, true);
  try { document.dispatchEvent(new CustomEvent(READY)); } catch (e) { /* ignore */ }
})();
