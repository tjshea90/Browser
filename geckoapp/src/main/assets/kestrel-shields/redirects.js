'use strict';
// AW1c: the stand-ins a `$redirect=` rule answers a blocked request with. A rule such as `||ads.example/tag.js$script,redirect=noopjs` does not only cancel the file: the page gets a harmless file
// of the same kind instead, so a script that waits for the ad SDK carries on, a player whose ad request "fails" starts the video, a page that calls a tracker's function does not throw.
//
// Everything here is OUR OWN code, written from what such a script has to answer for the page not to notice (the names of the lists' resources and the behaviour documented on the public uBlock Origin
// wiki are the specification, no code of theirs is used). The ids are the numbers the filter engine carries in its verdict word (core/.../filter/RedirectResources.kt `FILES`; web-tests/redirects.test.js
// checks both tables against each other): never reorder, only append.
//
// The files are answered as `data:` URLs, not as pages of the extension: a `moz-extension://<id>/...` address would show the page the extension's random, per-profile id (a redirected request's final
// address is readable by the page), which is a tracking handle that survives across every site. A stand-in only ever answers a request of a kind it suits (urlFor(id, type) says null otherwise and the
// caller cancels the request as before).
(function (root) {
  var JS = 'text/javascript;charset=utf-8';

  // ------------------------------------------------------------------ the scripts: each is a self-contained function, sent as its own source text
  function stubNoop() { /* nothing to do: the file exists and loads */ }

  function stubIma() { // Google IMA SDK: every ad request is answered with "no ad available" (VAST_EMPTY_RESPONSE), which every player has to handle, so the content starts
    var w = window;
    var g = w.google = w.google || {};
    var ima = g.ima = g.ima || {};
    if (ima.AdsLoader) return;
    function AdError(code, msg) { this.c = code; this.m = msg; }
    AdError.prototype.getErrorCode = function () { return this.c; };
    AdError.prototype.getVastErrorCode = function () { return 303; };
    AdError.prototype.getInnerError = function () { return null; };
    AdError.prototype.getMessage = function () { return this.m; };
    AdError.prototype.getType = function () { return 'adLoadError'; };
    AdError.prototype.toString = function () { return 'AdError ' + this.c + ': ' + this.m; };
    AdError.ErrorCode = { VAST_EMPTY_RESPONSE: 1009, VAST_LOAD_TIMEOUT: 301, UNKNOWN_AD_RESPONSE: 1010, FAILED_TO_REQUEST_ADS: 1005, INVALID_ARGUMENTS: 1101, ADS_REQUEST_NETWORK_ERROR: 1012 };
    AdError.Type = { AD_LOAD: 'adLoadError', AD_PLAY: 'adPlayError' };
    ima.AdError = AdError;
    function AdErrorEvent(err, ctx) { this.type = 'adError'; this.e = err; this.x = ctx; }
    AdErrorEvent.prototype.getError = function () { return this.e; };
    AdErrorEvent.prototype.getUserRequestContext = function () { return this.x; };
    AdErrorEvent.Type = { AD_ERROR: 'adError' };
    ima.AdErrorEvent = AdErrorEvent;
    function AdsManagerLoadedEvent() {}
    AdsManagerLoadedEvent.Type = { ADS_MANAGER_LOADED: 'adsManagerLoaded' };
    ima.AdsManagerLoadedEvent = AdsManagerLoadedEvent;
    function AdEvent() {}
    var names = 'AD_BREAK_READY AD_METADATA AD_PROGRESS ALL_ADS_COMPLETED CLICK COMPLETE CONTENT_PAUSE_REQUESTED CONTENT_RESUME_REQUESTED DURATION_CHANGE FIRST_QUARTILE IMPRESSION INTERACTION LINEAR_CHANGED LOADED LOG MIDPOINT PAUSED RESUMED SKIPPABLE_STATE_CHANGED SKIPPED STARTED THIRD_QUARTILE USER_CLOSE VIDEO_CLICKED VIDEO_ICON_CLICKED VOLUME_CHANGED VOLUME_MUTED'.split(' ');
    AdEvent.Type = {};
    for (var i = 0; i < names.length; i++) AdEvent.Type[names[i]] = names[i].toLowerCase().replace(/_([a-z])/g, function (m, c) { return c.toUpperCase(); });
    ima.AdEvent = AdEvent;
    function AdDisplayContainer() {}
    AdDisplayContainer.prototype.initialize = function () {};
    AdDisplayContainer.prototype.destroy = function () {};
    ima.AdDisplayContainer = AdDisplayContainer;
    function AdsRequest() {}
    AdsRequest.prototype.setAdWillAutoPlay = function () {};
    AdsRequest.prototype.setAdWillPlayMuted = function () {};
    AdsRequest.prototype.setContinuousPlayback = function () {};
    ima.AdsRequest = AdsRequest;
    function AdsRenderingSettings() {}
    ima.AdsRenderingSettings = AdsRenderingSettings;
    function CompanionAdSelectionSettings() {}
    CompanionAdSelectionSettings.CreativeType = { ALL: 'All', FLASH: 'Flash', IMAGE: 'Image' };
    CompanionAdSelectionSettings.ResourceType = { ALL: 'All', HTML: 'Html', IFRAME: 'IFrame', STATIC: 'Static' };
    CompanionAdSelectionSettings.SizeCriteria = { IGNORE: 'IgnoreSize', SELECT_EXACT_MATCH: 'SelectExactMatch', SELECT_NEAR_MATCH: 'SelectNearMatch' };
    ima.CompanionAdSelectionSettings = CompanionAdSelectionSettings;
    function ImaSdkSettings() {}
    var setters = 'setAutoPlayAdBreaks setCompanionBackfill setCookiesEnabled setDisableCustomPlaybackForIOS10Plus setDisableFlashAds setFeatureFlags setLocale setNumRedirects setPlayerType setPlayerVersion setPpid setSessionId setVpaidAllowed setVpaidMode'.split(' ');
    for (var j = 0; j < setters.length; j++) ImaSdkSettings.prototype[setters[j]] = function () {};
    ImaSdkSettings.prototype.getLocale = function () { return 'en'; };
    ImaSdkSettings.prototype.getNumRedirects = function () { return 4; };
    ImaSdkSettings.prototype.getPlayerType = function () { return ''; };
    ImaSdkSettings.prototype.getPlayerVersion = function () { return ''; };
    ImaSdkSettings.prototype.getPpid = function () { return ''; };
    ImaSdkSettings.VpaidMode = { DISABLED: 0, ENABLED: 1, INSECURE: 2 };
    ima.ImaSdkSettings = ImaSdkSettings;
    ima.settings = new ImaSdkSettings();
    ima.ViewMode = { NORMAL: 'normal', FULLSCREEN: 'fullscreen' };
    ima.UiElements = { AD_ATTRIBUTION: 'adAttribution', COUNTDOWN: 'countdown' };
    ima.VERSION = '3.0.0';
    function AdsLoader() { this.l = {}; this.dead = false; }
    AdsLoader.prototype.addEventListener = function (type, fn, capture, ctx) { (this.l[type] = this.l[type] || []).push([fn, ctx]); };
    AdsLoader.prototype.removeEventListener = function (type, fn) { var a = this.l[type] || []; for (var k = a.length - 1; k >= 0; k--) if (a[k][0] === fn) a.splice(k, 1); };
    AdsLoader.prototype.getSettings = function () { return ima.settings; };
    AdsLoader.prototype.contentComplete = function () {};
    AdsLoader.prototype.destroy = function () { this.dead = true; };
    AdsLoader.prototype.requestAds = function (req, userContext) {
      var self = this;
      setTimeout(function () {
        if (self.dead) return;
        var ev = new AdErrorEvent(new AdError(1009, 'No ads are available.'), userContext);
        ev.target = self;
        var a = (self.l.adError || []).slice();
        for (var k = 0; k < a.length; k++) { try { a[k][0].call(a[k][1] || self, ev); } catch (e) { /* the player's own handler failed: not our business */ } }
      }, 0);
    };
    ima.AdsLoader = AdsLoader;
    function AdsManager() {}
    var calls = 'collapse configureAdsManager destroy discardAdBreak expand focus getAdSkippableState getCuePoints getCurrentAd getRemainingTime getVolume init isCustomClickTrackingUsed isCustomPlaybackUsed pause requestNextAdBreak resize resume setVolume skip start stop updateAdsRenderingSettings'.split(' ');
    for (var m = 0; m < calls.length; m++) AdsManager.prototype[calls[m]] = function () {};
    AdsManager.prototype.addEventListener = function () {};
    AdsManager.prototype.removeEventListener = function () {};
    ima.AdsManager = AdsManager;
  }

  function stubGpt() { // Google Publisher Tag: the calls a page makes all succeed and do nothing, the page's queue (googletag.cmd) runs
    var w = window;
    var g = w.googletag = w.googletag || {};
    var queued = (g.cmd && typeof g.cmd.length === 'number') ? Array.prototype.slice.call(g.cmd) : [];
    function chain(names, target) { for (var i = 0; i < names.length; i++) target[names[i]] = function () { return this; }; return target; }
    function Slot(path, size, id) { this.p = path; this.s = size; this.i = id; }
    chain('addService clearCategoryExclusions clearTargeting defineSizeMapping set setCategoryExclusion setClickUrl setCollapseEmptyDiv setForceSafeFrame setSafeFrameConfig setTargeting updateTargetingFromMap'.split(' '), Slot.prototype);
    Slot.prototype.getAdUnitPath = function () { return this.p; };
    Slot.prototype.getSlotElementId = function () { return this.i; };
    Slot.prototype.get = function () { return null; };
    Slot.prototype.getTargeting = function () { return []; };
    Slot.prototype.getTargetingKeys = function () { return []; };
    Slot.prototype.getSizes = function () { return []; };
    Slot.prototype.getServices = function () { return []; };
    Slot.prototype.getResponseInformation = function () { return null; };
    Slot.prototype.getHtml = function () { return ''; };
    var pub = chain('addEventListener clear clearCategoryExclusions clearTagForChildDirectedTreatment clearTargeting collapseEmptyDivs defineOutOfPagePassback definePassback disableInitialLoad display enableAsyncRendering enableLazyLoad enableSingleRequest enableSyncRendering enableVideoAds refresh removeEventListener set setCategoryExclusion setCentering setCookieOptions setForceSafeFrame setLocation setPrivacySettings setPublisherProvidedId setRequestNonPersonalizedAds setSafeFrameConfig setTagForChildDirectedTreatment setTagForUnderAgeOfConsent setTargeting setVideoContent updateCorrelator'.split(' '), {});
    pub.get = function () { return null; };
    pub.getAttributeKeys = function () { return []; };
    pub.getTargeting = function () { return []; };
    pub.getTargetingKeys = function () { return []; };
    pub.getSlots = function () { return []; };
    pub.isInitialLoadDisabled = function () { return false; };
    pub.isSRA = function () { return false; };
    g.apiReady = true;
    g.pubadsReady = true;
    g.pubads = function () { return pub; };
    g.companionAds = function () { return pub; };
    g.content = function () { return pub; };
    g.defineSlot = function (path, size, id) { return new Slot(path, size, id); };
    g.defineOutOfPageSlot = function (path, id) { return new Slot(path, [], id); };
    g.enableServices = function () {};
    g.display = function () {};
    g.destroySlots = function () { return true; };
    g.openConsole = function () {};
    g.setAdIframeTitle = function () {};
    g.getVersion = function () { return ''; };
    g.sizeMapping = function () { var b = { addSize: function () { return b; }, build: function () { return []; } }; return b; };
    g.enums = g.enums || { OutOfPageFormat: { INTERSTITIAL: 5, REWARDED: 4, TOP_ANCHOR: 2, BOTTOM_ANCHOR: 3 } };
    function run(f) { try { if (typeof f === 'function') f.call(g); } catch (e) { /* a page callback that fails is the page's own problem */ } }
    var cmd = [];
    cmd.push = function () { for (var i = 0; i < arguments.length; i++) run(arguments[i]); return 0; };
    g.cmd = cmd;
    for (var q = 0; q < queued.length; q++) run(queued[q]);
  }

  function stubGtm() { // Google Tag Manager: the data layer works, and the callbacks a page hands to it ("when the tag has fired, go on") are called so nothing waits for a tag that never comes
    var w = window;
    var dl = w.dataLayer = w.dataLayer || [];
    function callbacks(o) {
      try {
        if (!o || typeof o !== 'object') return;
        if (typeof o.eventCallback === 'function') setTimeout(o.eventCallback, 0);
        if (typeof o.hitCallback === 'function') setTimeout(o.hitCallback, 0);
      } catch (e) { /* ignore */ }
    }
    var push = dl.push;
    dl.push = function () { for (var i = 0; i < arguments.length; i++) callbacks(arguments[i]); return push.apply(dl, arguments); };
    for (var j = 0; j < dl.length; j++) callbacks(dl[j]);
    var gtm = w.google_tag_manager = w.google_tag_manager || {};
    gtm.dataLayer = gtm.dataLayer || { get: function () { return undefined; }, set: function () {}, reset: function () {} };
  }

  function stubGa() { // Google Analytics, analytics.js and the older ga.js: commands are accepted, "ready" and hit callbacks are called, trackers answer with nothing
    var w = window;
    function hits(args) { for (var i = 0; i < args.length; i++) { var o = args[i]; if (o && typeof o === 'object' && typeof o.hitCallback === 'function') setTimeout(o.hitCallback, 0); } }
    function Tracker() {}
    Tracker.prototype.get = function () { return undefined; };
    Tracker.prototype.set = function () {};
    Tracker.prototype.send = function () { hits(arguments); };
    var tracker = new Tracker();
    var name = w.GoogleAnalyticsObject || 'ga';
    var old = w[name];
    var queued = (old && old.q && typeof old.q.length === 'number') ? Array.prototype.slice.call(old.q) : [];
    var ga = function () {
      var a = arguments;
      if (typeof a[0] === 'function') { try { a[0](tracker); } catch (e) { /* ignore */ } return; }
      hits(a);
    };
    ga.loaded = true;
    ga.create = function () { return tracker; };
    ga.getByName = function () { return tracker; };
    ga.getAll = function () { return [tracker]; };
    ga.remove = function () {};
    w[name] = ga;
    for (var q = 0; q < queued.length; q++) { try { ga.apply(null, queued[q]); } catch (e) { /* ignore */ } }
    var gaq = w._gaq = w._gaq || [];
    var classic = { _trackPageview: function () {}, _trackEvent: function () {}, _setAccount: function () {}, _setDomainName: function () {}, _setAllowLinker: function () {}, _link: function () {}, _anonymizeIp: function () {}, _gat: null };
    var pending = Array.prototype.slice.call(gaq);
    gaq.push = function () {
      for (var i = 0; i < arguments.length; i++) { var c = arguments[i]; if (typeof c === 'function') { try { c(); } catch (e) { /* ignore */ } } }
      return 0;
    };
    gaq._getAsyncTracker = function () { return classic; };
    for (var p = 0; p < pending.length; p++) { if (typeof pending[p] === 'function') { try { pending[p](); } catch (e) { /* ignore */ } } }
    w._gat = w._gat || { _getTracker: function () { return classic; }, _createTracker: function () { return classic; }, _anonymizeIp: function () {} };
  }

  function stubAdsense() { // adsbygoogle.js: the array the page pushes its ad units into exists, says it is loaded and swallows what is pushed
    var w = window;
    var a = w.adsbygoogle = w.adsbygoogle || [];
    try { a.loaded = true; a.push = function () { return 0; }; } catch (e) { /* frozen by the page: leave it */ }
  }

  function stubFingerprint() { // Fingerprint2: the callbacks get an empty answer
    var w = window;
    function hash() { return '00000000000000000000000000000000'; }
    function FP(opts) { this.o = opts; }
    FP.prototype.get = function (a, b) { var cb = typeof a === 'function' ? a : b; setTimeout(function () { if (typeof cb === 'function') cb(hash(), []); }, 0); };
    FP.get = function (a, b) { var cb = typeof a === 'function' ? a : b; setTimeout(function () { if (typeof cb === 'function') cb([]); }, 0); };
    FP.getPromise = function () { return Promise.resolve([]); };
    FP.getV18 = FP.prototype.get;
    FP.x64hash128 = hash;
    FP.VERSION = '2.1.4';
    w.Fingerprint2 = w.Fingerprint2 || FP;
  }

  function stubNobab() { // BlockAdBlock / FuckAdBlock: "no ad blocker found" is reported to everything that asks
    var w = window;
    function Bab() {
      var t = this;
      t.d = []; t.n = []; t.ok = false;
      setTimeout(function () { t.check(); }, 0);
    }
    Bab.prototype.onDetected = function (f) { if (typeof f === 'function') this.d.push(f); return this; };
    Bab.prototype.onNotDetected = function (f) {
      if (typeof f !== 'function') return this;
      if (this.ok) setTimeout(f, 0); else this.n.push(f);
      return this;
    };
    Bab.prototype.on = function (detected, f) { return detected ? this.onDetected(f) : this.onNotDetected(f); };
    Bab.prototype.check = function () {
      var t = this, list = t.n;
      t.ok = true; t.n = [];
      setTimeout(function () { for (var i = 0; i < list.length; i++) { try { list[i](); } catch (e) { /* ignore */ } } }, 0);
      return true;
    };
    Bab.prototype.emitEvent = function () { return this.check(); };
    Bab.prototype.clearEvent = function () { this.d = []; this.n = []; };
    Bab.prototype.setOption = function () { return this; };
    var classes = ['BlockAdBlock', 'FuckAdBlock'];
    for (var i = 0; i < classes.length; i++) if (!w[classes[i]]) w[classes[i]] = Bab;
    if (!w.blockAdBlock) w.blockAdBlock = new Bab();
    if (!w.fuckAdBlock) w.fuckAdBlock = new Bab();
  }

  function stubApstag() { // Amazon's header bidding: no bids, the callback is called
    var w = window;
    var a = w.apstag = w.apstag || {};
    a.init = a.init || function () {};
    a.fetchBids = function (cfg, cb) { if (typeof cb === 'function') setTimeout(function () { try { cb([]); } catch (e) { /* ignore */ } }, 0); };
    a.setDisplayBids = function () {};
    a.targetingKeys = function () { return []; };
    a.renewIdentity = function () {};
    a.debug = function () {};
    a._Q = a._Q || [];
  }

  function stubChartbeat() {
    var w = window;
    var p = w.pSUPERFLY = w.pSUPERFLY || {};
    p.activity = p.activity || function () {};
    p.virtualPage = p.virtualPage || function () {};
  }

  function stubPrebidAds() { // the old "can this page run ads" probes
    var w = window;
    w.canRunAds = true;
    w.isAdBlockActive = false;
  }

  function js(fn) { return 'try{(' + fn.toString() + ')();}catch(e){}'; } // never an error in the page, whatever state the page left the names in

  // ------------------------------------------------------------------ the table (index = id; names as in core RedirectResources.FILES)
  var SCRIPT = ['script'], DATA = ['xmlhttprequest', 'other'], IMAGE = ['image', 'imageset'], MEDIA = ['media'], FRAME = ['sub_frame', 'object'], STYLE = ['stylesheet'];
  var R = [
    null,
    { name: 'noop.js', mime: JS, types: ['script', 'xmlhttprequest', 'other'], text: js(stubNoop) },
    { name: 'noop.txt', mime: 'text/plain;charset=utf-8', types: DATA, text: '' },
    { name: 'noop.html', mime: 'text/html;charset=utf-8', types: FRAME, text: '<!DOCTYPE html><html><head><meta charset="utf-8"><title></title></head><body></body></html>' },
    { name: 'noop.css', mime: 'text/css;charset=utf-8', types: STYLE, text: '/* stand-in */' },
    { name: 'noop.json', mime: 'application/json;charset=utf-8', types: DATA, text: '{}' },
    { name: '1x1.gif', mime: 'image/gif', types: IMAGE, b64: 'R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAICRAEAOw==' },
    { name: '2x2.png', mime: 'image/png', types: IMAGE, b64: 'iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAYAAABytg0kAAAAC0lEQVR42mNgQAcAABIAAeRVjecAAAAASUVORK5CYII=' },
    { name: '3x2.png', mime: 'image/png', types: IMAGE, b64: 'iVBORw0KGgoAAAANSUhEUgAAAAMAAAACCAYAAACddGYaAAAAC0lEQVR42mNgwAUAABoAAS+Yl6YAAAAASUVORK5CYII=' },
    { name: '32x32.png', mime: 'image/png', types: IMAGE, b64: 'iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAGklEQVR42u3BAQEAAACCIP+vbkhAAQAAAO8GECAAAcm1w7EAAAAASUVORK5CYII=' },
    { name: 'noop-0.1s.mp3', mime: 'audio/mpeg', types: MEDIA, b64: '/+MYxAAAAANIAAAAAExBTUUzLjEwMFVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVV/+MYxDsAAANIAAAAAFVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVV/+MYxHYAAANIAAAAAFVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVV/+MYxLEAAANIAAAAAFVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVV' },
    { name: 'noop-1s.mp4', mime: 'video/mp4', types: MEDIA, b64: 'AAAAIGZ0eXBpc29tAAACAGlzb21pc28yYXZjMW1wNDEAAAM5bW9vdgAAAGxtdmhkAAAAAAAAAAAAAAAAAAAD6AAAA+gAAQAAAQAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAAmN0cmFrAAAAXHRraGQAAAADAAAAAAAAAAAAAAABAAAAAAAAA+gAAAAAAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAABAAAAAABAAAAAQAAAAAAAkZWR0cwAAABxlbHN0AAAAAAAAAAEAAAPoAAAAAAABAAAAAAHbbWRpYQAAACBtZGhkAAAAAAAAAAAAAAAAAAAoAAAAKABVxAAAAAAALWhkbHIAAAAAAAAAAHZpZGUAAAAAAAAAAAAAAABWaWRlb0hhbmRsZXIAAAABhm1pbmYAAAAUdm1oZAAAAAEAAAAAAAAAAAAAACRkaW5mAAAAHGRyZWYAAAAAAAAAAQAAAAx1cmwgAAAAAQAAAUZzdGJsAAAAunN0c2QAAAAAAAAAAQAAAKphdmMxAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAAABAAEABIAAAASAAAAAAAAAABFUxhdmM2MC4zMS4xMDIgbGlieDI2NAAAAAAAAAAAAAAAGP//AAAAMGF2Y0MBQsAK/+EAF2dCwAqmEXsBEAAAAwAQAAADAKDxImEYAQAGaMhCA5LIAAAAEHBhc3AAAAABAAAAAQAAABRidHJ0AAAAAAAAFWgAABVoAAAAGHN0dHMAAAAAAAAAAQAAAAUAAAgAAAAAFHN0c3MAAAAAAAAAAQAAAAEAAAAcc3RzYwAAAAAAAAABAAAAAQAAAAUAAAABAAAAKHN0c3oAAAAAAAAAAAAAAAUAAAKFAAAACgAAAAoAAAAKAAAACgAAABRzdGNvAAAAAAAAAAEAAANpAAAAYnVkdGEAAABabWV0YQAAAAAAAAAhaGRscgAAAAAAAAAAbWRpcmFwcGwAAAAAAAAAAAAAAAAtaWxzdAAAACWpdG9vAAAAHWRhdGEAAAABAAAAAExhdmY2MC4xNi4xMDAAAAAIZnJlZQAAArVtZGF0AAACcgYF//9u3EXpvebZSLeWLNgg2SPu73gyNjQgLSBjb3JlIDE2NCByMzEwOCAzMWUxOWY5IC0gSC4yNjQvTVBFRy00IEFWQyBjb2RlYyAtIENvcHlsZWZ0IDIwMDMtMjAyMyAtIGh0dHA6Ly93d3cudmlkZW9sYW4ub3JnL3gyNjQuaHRtbCAtIG9wdGlvbnM6IGNhYmFjPTAgcmVmPTE2IGRlYmxvY2s9MTowOjAgYW5hbHlzZT0weDE6MHgxMzEgbWU9dW1oIHN1Ym1lPTEwIHBzeT0xIHBzeV9yZD0xLjAwOjAuMDAgbWl4ZWRfcmVmPTEgbWVfcmFuZ2U9MjQgY2hyb21hX21lPTEgdHJlbGxpcz0yIDh4OGRjdD0wIGNxbT0wIGRlYWR6b25lPTIxLDExIGZhc3RfcHNraXA9MSBjaHJvbWFfcXBfb2Zmc2V0PS0yIHRocmVhZHM9MSBsb29rYWhlYWRfdGhyZWFkcz0xIHNsaWNlZF90aHJlYWRzPTAgbnI9MCBkZWNpbWF0ZT0xIGludGVybGFjZWQ9MCBibHVyYXlfY29tcGF0PTAgY29uc3RyYWluZWRfaW50cmE9MCBiZnJhbWVzPTAgd2VpZ2h0cD0wIGtleWludD0yNTAga2V5aW50X21pbj01IHNjZW5lY3V0PTQwIGludHJhX3JlZnJlc2g9MCByY19sb29rYWhlYWQ9NjAgcmM9Y3JmIG1idHJlZT0xIGNyZj00MC4wIHFjb21wPTAuNjAgcXBtaW49MCBxcG1heD02OSBxcHN0ZXA9NCBpcF9yYXRpbz0xLjQwIGFxPTE6MS4wMACAAAAAC2WIggI+TFAAEW3AAAAABkGaHAR9QAAAAAZBmioBH1AAAAAGQZo7AQ9QAAAABkGaSQD/UA==' },
    { name: 'google-ima.js', mime: JS, types: SCRIPT, text: js(stubIma) },
    { name: 'googletagservices_gpt.js', mime: JS, types: SCRIPT, text: js(stubGpt) },
    { name: 'googletagmanager_gtm.js', mime: JS, types: SCRIPT, text: js(stubGtm) },
    { name: 'google-analytics_analytics.js', mime: JS, types: SCRIPT, text: js(stubGa) },
    { name: 'googlesyndication_adsbygoogle.js', mime: JS, types: SCRIPT, text: js(stubAdsense) },
    { name: 'noop-vast.xml', mime: 'text/xml;charset=utf-8', types: DATA, text: '<?xml version="1.0" encoding="UTF-8"?><VAST version="3.0"></VAST>' },
    { name: 'fingerprint2.js', mime: JS, types: SCRIPT, text: js(stubFingerprint) },
    { name: 'nobab.js', mime: JS, types: SCRIPT, text: js(stubNobab) },
    { name: 'amazon_apstag.js', mime: JS, types: SCRIPT, text: js(stubApstag) },
    { name: 'chartbeat.js', mime: JS, types: SCRIPT, text: js(stubChartbeat) },
    { name: 'prebid-ads.js', mime: JS, types: SCRIPT, text: js(stubPrebidAds) },
  ];

  var urls = [];
  function build(id) {
    var r = R[id];
    return 'data:' + r.mime + (r.b64 !== undefined ? ';base64,' + r.b64 : ',' + encodeURIComponent(r.text));
  }

  var api = {
    /** index = id, 0 = none */
    names: R.map(function (r) { return r ? r.name : ''; }),
    count: R.length - 1,
    /** can resource [id] answer a request of the webRequest type [type]? */
    suits: function (id, type) { var r = R[id]; return !!r && r.types.indexOf(type) >= 0; },
    /** the data: URL to answer a blocked request of the webRequest type [type] with, or null when this resource does not suit that kind of request (the caller cancels it instead) */
    urlFor: function (id, type) {
      if (!api.suits(id, type)) return null;
      return urls[id] || (urls[id] = build(id));
    },
    /** the text of a script stand-in, or the base64 of a binary one (tests) */
    sourceOf: function (id) { var r = R[id]; return r ? (r.b64 !== undefined ? r.b64 : r.text) : null; },
    mimeOf: function (id) { var r = R[id]; return r ? r.mime : null; },
    typesOf: function (id) { var r = R[id]; return r ? r.types.slice() : []; },
  };
  root.KestrelRedirects = api;
  if (typeof module !== 'undefined' && module.exports) module.exports = api;
})(typeof self !== 'undefined' ? self : this);
