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
          if (!r.prune && !r.re) return Promise.resolve(emptyResponse(vals.url));
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
            if (!r.prune && !r.re) { fakeXhr(this, info.url); return undefined; }
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
  function jsonPrune(prune, needle) {
    var ex = pairs(arguments, 2);
    pruneRules.push({ paths: pathsOf(prune), needles: pathsOf(needle), stack: ex.stack ? pattern(ex.stack) : null });
    if (parseWrapped) return;
    parseWrapped = true;
    JSON.parse = function () {
      var r = nParse.apply(this, arguments);
      try {
        var st = null;
        for (var i = 0; i < pruneRules.length; i++) {
          var p = pruneRules[i];
          if (p.stack) { if (st === null) { try { st = new Error().stack || ''; } catch (e) { st = ''; } } if (!hit(p.stack, st)) continue; }
          pruneObject(r, p.paths, p.needles);
        }
      } catch (e) { /* ignore */ }
      return r;
    };
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
    for (var i = 0; i < calls.length && i < 60 && total < 300; i++) {
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
    if (typeof d !== 'string' || d.length > 20000) return;
    run(d);
  }, true);
  try { document.dispatchEvent(new CustomEvent(READY)); } catch (e) { /* ignore */ }
})();
