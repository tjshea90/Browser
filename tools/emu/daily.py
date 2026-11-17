"""LAB3 (owner message 62): the daily-use journey for the emulator lab.

ONE continuous session in the order a person uses a browser, on the app's REAL defaults (press-to-play on, web films in Kestrel's own player, follow-the-tablet turning: the other suites
switch these off to test one thing at a time). Every step is timed and carries what the app said while it ran (Health lines, console errors, UI stalls, scroll gaps, page loads);
security and feature probes are pass / fail / info rows. Everything goes to events.jsonl; tools/emu/buildreport.py turns that into the report for the build.

soak.py runs as `python3 -I`, so it cannot import this file by name: it loads it by path and copies the methods of DailyMixin onto Soak (see the end of soak.py).
Nothing here may raise out of a step: a control that cannot be found is a recorded finding, not a crash of the lab.
"""
import json
import os
import re
import shlex
import socket
import time
import xml.etree.ElementTree as ET

# bound by soak.py through bind(): the helpers of the driver
health_events = console_summary = say = ms_of = host_of = tail = num = None
CRASH_PATTERNS = []
T0 = time.time()

PUBLIC_READ = [("wikipedia-article", "en.wikipedia.org/wiki/Firefox"), ("mdn", "developer.mozilla.org/en-US/docs/Web/API/Screen/orientation")]
TAB_SITES = [("hacker-news", "https://news.ycombinator.com/"), ("mdn", "https://developer.mozilla.org/en-US/docs/Web/API/Screen/orientation"),
             ("lab-text", "lab:/page/text"), ("example", "https://example.com/")]
MEMORY_SITES = ["https://www.bbc.com/news", "https://www.theguardian.com/international", "https://github.com/mozilla/gecko-dev", "https://stackoverflow.com/questions/tagged/android",
                "https://en.wikipedia.org/wiki/Android_(operating_system)", "https://en.wikipedia.org/wiki/Mozilla", "https://news.ycombinator.com/", "https://developer.mozilla.org/en-US/docs/Web/CSS"]
BAD_CERTS = [("expired", "https://expired.badssl.com/"), ("self-signed", "https://self-signed.badssl.com/"), ("wrong-host", "https://wrong.host.badssl.com/"), ("untrusted-root", "https://untrusted-root.badssl.com/")]
SCHEME_WORDS = ("tel:", "sms:", "mailto:", "market:", "intent:", "whatsapp", "sip:", "vnd.youtube", "zxing")


def bind(g):
    """The driver hands over its own helpers (a few names, no copy of its state)."""
    for k in ("health_events", "console_summary", "say", "ms_of", "host_of", "tail", "num", "CRASH_PATTERNS", "T0"):
        globals()[k] = g[k]


class DailyMixin:
    # ------------------------------------------------------------------ small helpers
    def d_nodes(self):
        """The native views on screen with their bounds: [{text, desc, id, cls, b=(x1,y1,x2,y2), c=(x,y), click}] (the web page is not in it). [] when the dump fails."""
        xml = ""
        for _ in range(3):
            self.ph.a.sh("uiautomator dump /sdcard/kestrel-ui.xml", timeout=40)
            xml = self.ph.a.sh("cat /sdcard/kestrel-ui.xml", timeout=20)
            if "<hierarchy" in xml:
                break
            time.sleep(1.5)
        else:
            return []
        try:
            root = ET.fromstring(xml[xml.index("<?xml"):] if "<?xml" in xml else xml)
        except ET.ParseError:
            return []
        out = []
        for n in root.iter("node"):
            m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", n.get("bounds", ""))
            if not m:
                continue
            x1, y1, x2, y2 = (int(v) for v in m.groups())
            out.append({"text": n.get("text", ""), "desc": n.get("content-desc", ""), "id": n.get("resource-id", ""), "cls": n.get("class", "").split(".")[-1],
                        "b": (x1, y1, x2, y2), "c": ((x1 + x2) // 2, (y1 + y2) // 2), "click": n.get("clickable") == "true"})
        return out

    def d_dump(self, tag):
        """Write the native view tree to ui-<tag>.txt (the next run is tuned from these) and return the nodes."""
        nodes = self.d_nodes()
        try:
            with open(os.path.join(self.out, "ui-%s.txt" % tag), "w") as f:
                for n in nodes:
                    f.write("%s | text=%r | desc=%r | id=%s | %s | click=%s\n" % (n["cls"], n["text"][:60], n["desc"][:60], n["id"].split("/")[-1], n["b"], n["click"]))
        except OSError:
            pass
        return nodes

    def d_address_node(self, nodes):
        """The address control: lab run 44003337190 (ui-home.txt) showed it is a clickable EditText at the bottom of the page area, its text is the page's full address (or the hint on Home)."""
        cands = [n for n in nodes if n["cls"] == "EditText" and n["click"]]
        if cands:
            return sorted(cands, key=lambda n: -n["b"][1])[0]
        return self.d_find(r"Address\. Tap|Search or type address|Search Google or type", nodes)

    def d_find(self, pattern, nodes=None):
        rx = re.compile(pattern, re.I)
        for n in (nodes if nodes is not None else self.d_nodes()):
            if rx.search(n["text"]) or rx.search(n["desc"]):
                return n
        return None

    def d_type_in_address(self, text):
        """Tap the address control, type like a person (key events, one string), press Enter. Returns (found_control, (x, y))."""
        n = None
        for attempt in range(3):                                   # (uiautomator cannot dump while a page animates: ask again a little later)
            n = self.d_address_node(self.d_nodes())
            if n:
                break
            time.sleep(2.5)
        found = n is not None
        if n:
            self.addr_at = n["c"]                                  # the dock does not move: the next step can tap here even when its own dump fails
        at = n["c"] if n else (getattr(self, "addr_at", None) or (self.screen[0] // 2, self.screen[1] - 70))
        self.ph.tap(*at)
        time.sleep(1.6)
        self.ph.a.sh("input text %s" % shlex.quote(text.replace(" ", "%s")), timeout=30)
        time.sleep(1.0)
        self.ph.key("KEYCODE_ENTER")
        return found or hasattr(self, "addr_at"), at

    def d_t(self, seconds):
        """A wait limit; the driver's selftest (a fake phone that never answers) must not wait it out."""
        return min(seconds, 1.5) if self.a.selftest else seconds

    def d_wait_page(self, mark, timeout=60):
        """The first 'page loaded' / 'page error' line since the log offset [mark]: (kind, kv, seconds) or (None, {}, seconds)."""
        timeout = self.d_t(timeout)
        t0 = time.time()
        while time.time() - t0 < timeout:
            for e in health_events(self.log.since(mark), "page"):
                if e["ev"] in ("loaded", "error"):
                    return e["ev"], e["kv"], time.time() - t0
            time.sleep(1.0)
        return None, {}, time.time() - t0

    def d_fling(self, n, up=True, ms=150, pause=1.1):
        for _ in range(n):
            if up:
                self.ph.swipe(300, 820, 300, 220, ms)
            else:
                self.ph.swipe(300, 260, 300, 840, ms)
            time.sleep(pause)

    def d_digest(self, lines):
        """What the app said during a step, in a few lines: loads, errors, scroll, stalls, tabs, memory, video."""
        pages, scroll = [], {"bursts": 0, "frames": 0, "gaps50": 0, "gaps100": 0, "worst_ms": 0}
        for e in health_events(lines, "page"):
            kv = e["kv"]
            if e["ev"] == "loaded":
                pages.append({"host": kv.get("host", ""), "paint_ms": ms_of(kv.get("paint", "")), "done_ms": ms_of(kv.get("done", "")), "blocked": num(kv.get("blocked")), "visit": num(kv.get("visit"), 1), "ok": kv.get("ok")})
            elif e["ev"] == "error":
                pages.append({"host": kv.get("host", ""), "error": kv.get("kind", "?")})
        for b in health_events(lines, "scroll", "burst"):
            kv = b["kv"]
            scroll["bursts"] += 1
            scroll["frames"] += num(kv.get("n"))
            scroll["gaps50"] += num(kv.get("a"))
            scroll["gaps100"] += num(kv.get("b"))
            scroll["worst_ms"] = max(scroll["worst_ms"], num(kv.get("mx")))
        keep = []
        for e in health_events(lines):
            if e["layer"] in ("tabs", "mem", "life", "inline", "native", "video", "media", "back", "download", "fullscreen", "field", "gecko", "look", "engine") or (e["layer"] == "ui" and e["ev"] in ("STALL", "slow", "late-beat")):
                keep.append("+%.1fs %s %s %s" % (e["t"], e["layer"], e["ev"], e["rest"][:110]))
        return pages, (scroll if scroll["bursts"] else None), keep[:14]

    def step(self, name, fn, area="usage"):
        """One step of the person's day. Timed; carries what the app said while it ran; never raises."""
        say("daily step: %s" % name)
        mark, rmark, t0 = self.log.size(), self.lab.size(), time.time()
        ev = {"kind": "step", "name": name, "area": area}
        try:
            out = fn()
            if isinstance(out, dict):
                ev.update(out)
            ev.setdefault("status", "ok")
        except Exception as e:                                       # a bug of the lab must never end the day
            ev.update(status="tooling-error", note=repr(e)[:200])
            self.issue("low", "tooling", "daily step %s raised %r" % (name, e))
        ev["wall_s"] = round(time.time() - t0, 1)
        lines = self.log.since(mark)
        crashes, stalls = self.scan_log(lines, name)
        pages, scroll, digest = self.d_digest(lines)
        ev["pages"], ev["scroll"], ev["digest"] = pages, scroll, digest
        ev["console"] = console_summary(lines)
        ev["ui_stalls"] = len([s for s in stalls if s["ev"] == "STALL"])
        ev["ui_late"] = len(stalls)
        ev["ui_worst_ms"] = max([int(re.sub(r"\D", "", s["rest"].split("ms")[0]) or 0) for s in stalls] or [0])
        try:
            ev["pss"], ev["avail"], ev["procs"] = self.sample()
        except Exception:
            pass
        if ev["ui_stalls"]:
            self.issue("medium", "freeze", "%s: the UI thread was blocked %d time(s), worst %d ms" % (name, ev["ui_stalls"], ev["ui_worst_ms"]))
        if crashes:
            ev["status"] = "crash"
        if not self.check_alive(name):
            ev["status"] = "crash"
            ev["note"] = (ev.get("note", "") + " the app died or restarted during this step").strip()
        self.record(ev)
        return ev

    def d_dismiss_dialogs(self, limit=4):
        """The app asks before a page may use the camera, the microphone or the location: that is the wanted behaviour. Record each question's title, answer BLOCK, return the titles."""
        titles = []
        for _ in range(limit):
            nodes = self.d_nodes()
            title = next((n["text"] for n in nodes if n["id"].endswith("alertTitle")), None)
            btn = self.d_find(r"^(BLOCK|DENY|DON.T ALLOW|NOT NOW|CANCEL)$", nodes)
            if not title and not btn:
                break
            if title:
                titles.append(title)
            if btn:
                self.ph.tap(*btn["c"])
            else:
                self.ph.key("KEYCODE_BACK")
            time.sleep(1.5)
        return titles

    def d_check(self, name, result, detail="", sev="high", area="security"):
        """One pass / fail / info row (a security or feature probe). A fail is an issue and a row of the report."""
        self.record({"kind": "sec" if area == "security" else "feat", "name": name, "status": "ok" if result != "fail" else "fail", "result": result, "detail": str(detail)[:300]})
        if result == "fail":
            self.issue(sev, area, "%s: %s" % (name, detail))

    # ------------------------------------------------------------------ the device the lab really ran on (LAB3a)
    def d_facts(self):
        g = lambda p: self.ph.a.sh("getprop %s" % p).strip()
        facts = {"android": g("ro.build.version.release"), "sdk": g("ro.build.version.sdk"), "abi": g("ro.product.cpu.abi"), "abilist": g("ro.product.cpu.abilist"),
                 "low_ram": g("ro.config.low_ram"), "heap": g("dalvik.vm.heapsize"), "heap_growth": g("dalvik.vm.heapgrowthlimit"), "build": g("ro.build.flavor"),
                 "wm_size": self.ph.a.sh("wm size").strip().replace("\n", " "), "wm_density": self.ph.a.sh("wm density").strip().replace("\n", " "),
                 "font_scale": self.ph.a.sh("settings get system font_scale").strip(), "cores": self.ph.a.sh("nproc").strip(), "bits": self.ph.a.sh("getconf LONG_BIT").strip(),
                 "memtotal": (self.ph.a.sh("grep MemTotal /proc/meminfo").strip()), "cpu": (self.ph.a.sh("grep -m1 'model name' /proc/cpuinfo").strip())[:90], "profile": getattr(self.a, "profile", "?"),
                 "crypto": g("ro.crypto.state"), "gms": "yes" if self.ph.a.sh("pm list packages com.google.android.gms").strip() else "no"}
        with open(os.path.join(self.out, "device.json"), "w") as f:
            json.dump(facts, f, indent=1)
        self.record({"kind": "device", "name": "device-facts", "status": "ok", "facts": facts})
        return facts

    def d_bench(self, tag):
        """The CPU calibration: the same fixed work in the page every run, so a report can say how fast ITS runner was."""
        r0 = self.lab.size()
        self.ph.open_url("%s/bench?tag=%s" % (self.lab_base, tag))
        t0 = time.time()
        row = None
        while time.time() - t0 < self.d_t(60) and row is None:
            rows = [r for r in self.lab.since(r0) if r.get("kind") == "bench"]
            row = rows[-1] if rows else None
            time.sleep(1.0)
        if row is None:
            self.record({"kind": "bench", "name": "bench-" + tag, "status": "timeout"})
            return None
        vals = {k: num(row.get(k), -1) for k in ("loop", "sort", "json", "regex", "dom", "canvas", "total")}
        self.record({"kind": "bench", "name": "bench-" + tag, "status": "ok", "ms": vals})
        return vals

    # ------------------------------------------------------------------ the steps of the day
    def d_cold_start(self):
        self.ph.a.sh("am force-stop %s" % self.a.pkg)
        time.sleep(3)
        mark, t0 = self.log.size(), time.time()
        out = self.ph.a.sh("am start -W -n %s/%s" % (self.a.pkg, "app.kestrel.browser.MainActivity"), timeout=90)
        tot = re.search(r"TotalTime:\s*(\d+)", out)
        ready = None
        while time.time() - t0 < self.d_t(120):
            if health_events(self.log.since(mark), "engine", "runtime-ready"):
                ready = time.time() - t0
                break
            time.sleep(0.5)
        time.sleep(3)
        self.main_pid = None
        res = {"am_total_ms": int(tot.group(1)) if tot else None, "engine_ready_s": round(ready, 1) if ready else None, "status": "ok" if ready else "no-engine"}
        if not ready:
            self.issue("high", "start", "after a force-stop the engine did not report ready within 120 s")
        return res

    def d_home(self):
        home = self.d_find(r"^Home$")
        if home:
            self.ph.tap(*home["c"])                          # the Home screen of the browser (the page before it was the lab's calibration page)
            time.sleep(3)
        nodes = self.d_dump("home")
        has_search = self.d_address_node(nodes) is not None
        has_menu = self.d_find(r"^Menu$", nodes) is not None
        if not (has_search and has_menu):
            self.issue("low", "access", "the Home screen's search box / Menu are not found by Android's accessibility tree (uiautomator): TalkBack would not find them either (nodes seen: %d)" % len(nodes))
        return {"nodes": len(nodes), "search_box": has_search, "menu": has_menu}

    def d_search_typed(self):
        mark = self.log.size()
        found, at = self.d_type_in_address("weather in london today")
        kind, kv, secs = self.d_wait_page(mark, 75)
        host = (kv.get("host") or "")
        res = {"control_found": found, "tap": at, "page_event": kind, "host": host, "nav_s": round(secs, 1)}
        if kind is None:
            res["status"] = "no-navigation"
            self.d_check("typed-search-navigates", "fail", "text typed into the address control and Enter pressed (control found by accessibility: %s) but the browser never navigated: either key events do not reach the address control (a hardware-keyboard bug) or the lab cannot type there" % found, sev="medium", area="feature")
            self.shot("daily-typed-search")
        elif "google" in host:
            self.d_check("typed-search-goes-to-google", "pass", "navigated to %s (owner decision: anything typed that is not an address is searched on Google)" % host, area="feature")
        else:
            self.d_check("typed-search-goes-to-google", "fail", "typed text went to %s, not Google" % host, area="feature")
        return res

    def d_read_article(self):
        name, addr = PUBLIC_READ[0]
        up_state, up_note = self.upstream("https://" + addr)
        if up_state == "unreachable":
            return {"status": "upstream", "note": "the runner cannot reach %s (%s)" % (addr, up_note)}
        mark = self.log.size()
        found, at = self.d_type_in_address(addr)
        if not found:
            self.ph.open_url("https://" + addr)                     # the lab cannot type: carry on with the person's reading
        kind, kv, secs = self.d_wait_page(mark, 90)
        res = {"typed": found, "page_event": kind, "host": kv.get("host", ""), "nav_s": round(secs, 1), "paint_ms": ms_of(kv.get("paint", "")), "done_ms": ms_of(kv.get("done", ""))}
        if kind == "loaded" and kv.get("host", "").endswith("wikipedia.org"):
            self.d_check("typed-address-without-scheme-loads", "pass", "%s -> %s (HTTPS-first)" % (addr, kv.get("host")), area="feature")
        elif found:
            self.d_check("typed-address-without-scheme-loads", "fail", "typed %s, the page event was %s host=%s" % (addr, kind, kv.get("host")), sev="medium", area="feature")
        time.sleep(2)
        self.shot("daily-article-top")
        self.d_fling(10, True, 150, 1.2)            # reading down with flings (the scroll gaps are counted by the app: Health 'scroll burst')
        self.d_fling(4, False, 150, 1.0)
        self.shot("daily-article-scrolled")
        return res

    def d_tabs(self):
        res = {"opened": 0}
        for name, url in TAB_SITES:
            self.ph.debug("normtab", self.url(url))
            res["opened"] += 1
            time.sleep(7)
        nodes = self.d_dump("tabs-before-grid")
        tab_btn = self.d_find(r"^Tabs, \d+ open", nodes)
        res["tab_button"] = bool(tab_btn)
        if not tab_btn:
            self.issue("low", "access", "the tab button ('Tabs, N open') is not found by the accessibility tree")
            return res
        res["tab_count_label"] = tab_btn["desc"]
        t0 = time.time()
        mark = self.log.size()
        self.ph.tap(*tab_btn["c"])
        time.sleep(2.5)
        grid = self.d_dump("tab-grid")
        res["grid_nodes"] = len(grid)
        self.shot("daily-tab-grid")
        # a tab card = a big clickable node (taller than 120 px) that is not part of the dock; the middle one is switched to
        cards = [n for n in grid if n["click"] and (n["b"][3] - n["b"][1]) > 120 and (n["b"][2] - n["b"][0]) > 150]
        res["cards"] = len(cards)
        if cards:
            pick = cards[len(cards) // 2]
            m2 = self.log.size()
            t1 = time.time()
            self.ph.tap(*pick["c"])
            for _ in range(24):
                if health_events(self.log.since(m2), "tabs", "cover-hide") or health_events(self.log.since(m2), "page", "painted"):
                    break
                time.sleep(0.25)
            res["switch_s"] = round(time.time() - t1, 2)
            time.sleep(2)
        else:
            self.d_check("tab-grid-has-tab-cards", "fail", "the tab grid shows %d nodes but none looks like a tab card (clickable, 150 x 120 px or bigger)" % len(grid), sev="low", area="access")
        return res

    def d_back_forward(self):
        res = {"backs": 0}
        for _ in range(3):
            m = self.log.size()
            self.ph.key("KEYCODE_BACK")
            time.sleep(3.5)
            res["backs"] += 1
            res.setdefault("back_lines", []).extend(["%s %s" % (e["ev"], e["rest"][:120]) for e in health_events(self.log.since(m), "back")][:3])
        return res

    def d_form(self):
        r0 = self.lab.size()
        mark = self.log.size()
        self.ph.open_url("%s/page/form" % self.lab_base)
        kind, kv, secs = self.d_wait_page(mark, 40)
        pos = None
        t0 = time.time()
        while time.time() - t0 < self.d_t(15) and pos is None:
            rows = [r for r in self.lab.since(r0) if r.get("kind") == "formpos"]
            pos = rows[-1] if rows else None
            time.sleep(0.7)
        text = "Kestrel tablet test"
        res = {"page_reported_position": bool(pos)}
        m2 = self.log.size()
        if pos:
            self.ph.tap(int(num(pos.get("nx"))), int(num(pos.get("ny"))))
        else:
            self.ph.tap(300, 120)
        time.sleep(2.0)
        res["field_focused"] = bool(health_events(self.log.since(m2), "field", "focused"))
        t1 = time.time()
        m3 = self.log.size()
        self.ph.a.sh("input text %s" % shlex.quote(text.replace(" ", "%s")), timeout=40)
        res["typing_s"] = round(time.time() - t1, 2)
        time.sleep(1.2)
        self.ph.key("KEYCODE_ENTER")                       # submits the form: the lab server then holds exactly what the page received
        time.sleep(3)
        stalls = [e for e in health_events(self.log.since(m3), "ui") if e["ev"] in ("STALL", "slow", "late-beat")]
        res["typing_ui_late"] = len(stalls)
        res["typing_worst"] = max([int(re.sub(r"\D", "", st["rest"].split("ms")[0]) or 0) for st in stalls] or [0])
        got = [r for r in self.lab.since(r0) if r.get("kind") == "form"]
        arrived = got[-1].get("name") if got else None
        res["typed_text_arrived"] = arrived
        if arrived == text:
            self.d_check("typing-reaches-the-page", "pass", "19 key events typed into the page's Name field arrived intact; focus line seen: %s" % res["field_focused"], area="feature")
        else:
            self.d_check("typing-reaches-the-page", "fail", "typed %r, the page received %r (page reported its field position: %s, focus line: %s)" % (text, arrived, bool(pos), res["field_focused"]), sev="medium", area="feature")
            self.shot("daily-form")
        return res

    def d_film_in_page(self):
        """A film on a page with the app's real defaults: press play like a person (the page's own button / picture), Kestrel's player takes it over, the person leaves with Back."""
        rm, mark = self.lab.size(), self.log.size()
        tag = "daily-film"
        self.ph.open_url("%s/ho?v=file&tag=%s" % (self.lab_base, tag))
        t0 = time.time()
        lay = []
        while time.time() - t0 < self.d_t(40) and not lay:
            lay = [r for r in self.lab.since(rm) if r.get("kind") == "ap" and r.get("ev") == "layout" and str(r.get("tag", "")).startswith(tag)]
            time.sleep(1)
        if not lay:
            return {"status": "no-layout", "note": "the film page never reported where its picture is"}
        time.sleep(5)
        L = lay[-1]
        tx, ty = int(num(L.get("vx"))), int(num(L.get("vy")))
        if tx <= 0 or ty <= 0:
            tx, ty = self.screen[0] // 2, 260
        self.shot("daily-film-before")
        m2 = self.log.size()
        t1 = time.time()
        self.ph.tap(tx, ty)
        first = None
        while time.time() - t1 < self.d_t(25) and first is None:
            for layer, ev in (("native", "first-picture"), ("inline", "placed")):
                if health_events(self.log.since(m2), layer, ev):
                    first = time.time() - t1
            time.sleep(0.5)
        time.sleep(8)
        self.shot("daily-film-playing")
        lines = self.log.since(m2)
        took = health_events(lines, "video", "hand-over")
        res = {"tap_to_picture_s": round(first, 1) if first else None, "kestrel_player_took_it": bool(took), "stats": [e["rest"][:160] for e in health_events(lines, "native") if "stats" in e["ev"] or "playable" in e["rest"]][:2]}
        if first is None:
            res["status"] = "no-picture"
            self.d_check("film-starts-when-pressed", "fail", "pressed play on the lab film page and no picture appeared within 25 s (hand-over lines: %d)" % len(took), area="video", sev="high")
        else:
            self.d_check("film-starts-when-pressed", "pass", "first picture %.1f s after the tap" % first, area="video")
        self.ph.key("KEYCODE_BACK")
        time.sleep(4)
        return res

    def d_real_film(self):
        """A film on a real site, as a person would: open it, press play, watch a little (what the runner's network allows)."""
        out = {}
        for name, url, watch in (("wikimedia-commons", "https://commons.wikimedia.org/wiki/File:Big_Buck_Bunny_4K.webm", 25),):
            up, note = self.upstream(url)
            if up == "unreachable":
                out[name] = "upstream (%s)" % note
                continue
            try:
                self.video_site(name, url, watch)
                out[name] = "done"
            except Exception as e:
                out[name] = "raised %r" % e
        return {"sites": out}

    def d_download(self):
        res = {}
        try:
            if not self.a.selftest:                              # (the fake phone never shows the question the download waits for)
                self.download("lab-5mb", "lab:/dl/5mb.bin", 5 * 1048576, 120)
            res["download"] = "done"
        except Exception as e:
            res["download"] = "raised %r" % e
        nodes = self.d_dump("before-menu")
        menu = self.d_find(r"^Menu$", nodes)
        if menu:
            self.ph.tap(*menu["c"])
            time.sleep(2)
            items = self.d_dump("menu")
            res["menu_items"] = [n["text"] or n["desc"] for n in items if (n["text"] or n["desc"]) and n["click"]][:30]
            dl = self.d_find(r"Downloads", items)
            if dl:
                self.ph.tap(*dl["c"])
                time.sleep(3)
                self.shot("daily-downloads-screen")
                res["downloads_screen"] = True
                self.ph.key("KEYCODE_BACK")
                time.sleep(1.5)
            else:
                res["downloads_screen"] = False
                self.ph.key("KEYCODE_BACK")
        else:
            res["menu"] = "not found"
            self.issue("low", "access", "the Menu button is not found by the accessibility tree")
        return res

    def d_settings_walk(self):
        """Open the app's own screens (Settings, Diagnostics, History ...) from the menu and come back: any crash or freeze shows in the step."""
        opened = []
        for label in (r"^Settings", r"Diagnostics", r"History", r"Bookmarks"):
            menu = self.d_find(r"^Menu$")
            if not menu:
                break
            self.ph.tap(*menu["c"])
            time.sleep(1.8)
            item = self.d_find(label)
            if item:
                self.ph.tap(*item["c"])
                time.sleep(3)
                if label == r"^Settings":
                    self.d_dump("settings")
                self.shot("daily-" + re.sub(r"\W", "", label))
                opened.append(re.sub(r"\W", "", label))
                self.ph.key("KEYCODE_BACK")
                time.sleep(1.5)
            else:
                self.ph.key("KEYCODE_BACK")
                time.sleep(1)
        return {"opened": opened}

    def d_private_and_shields(self):
        res = {}
        mark = self.log.size()
        self.ph.debug("privtab", "https://example.com/")
        k, kv, s = self.d_wait_page(mark, 40)
        res["private_tab_load"] = k
        time.sleep(2)
        # Shields on / off for one tracker-heavy page: what the blocking costs and saves (the compat suite does 26 sites; one is enough for a day)
        out = {}
        for mode in ("on", "off"):
            self.ph.shields(mode, ["www.theguardian.com", "theguardian.com"])
            time.sleep(2)
            self.ph.clear_cache()
            self.ph.debug("closetabs")
            time.sleep(4)
            m2 = self.log.size()
            self.ph.open_url("https://www.theguardian.com/international")
            k, kv, s = self.d_wait_page(m2, 90)
            self.d_fling(3, True, 150, 1.0)
            out[mode] = {"event": k, "done_ms": ms_of(kv.get("done", "")), "paint_ms": ms_of(kv.get("paint", "")), "blocked": num(kv.get("blocked"))}
        self.ph.shields("on", ["www.theguardian.com", "theguardian.com"])
        res["shields_ab"] = out
        return res

    def d_leave_and_return(self):
        res = {}
        mark = self.log.size()
        self.ph.home()
        time.sleep(20)
        t0 = time.time()
        m2 = self.log.size()
        self.ph.start_app()
        back = None
        while time.time() - t0 < self.d_t(20) and back is None:
            if health_events(self.log.since(m2), "life", "resume"):
                back = time.time() - t0
            time.sleep(0.3)
        time.sleep(4)
        lines = self.log.since(m2)
        res["resume_s"] = round(back, 2) if back else None
        res["page_reloaded"] = any(e["ev"] == "loaded" for e in health_events(lines, "page"))
        res["engine_died"] = bool(health_events(self.log.since(mark), "gecko", "onCrash") or health_events(self.log.since(mark), "gecko", "onKill"))
        # the screen off and on again (a person puts the tablet down)
        self.ph.a.sh("input keyevent 26")
        time.sleep(15)
        self.ph.key("KEYCODE_WAKEUP")
        self.ph.a.sh("wm dismiss-keyguard")
        time.sleep(4)
        res["alive_after_screen_off"] = bool(self.ph.pids())
        return res

    def d_memory(self):
        """Many tabs on heavy pages, then Android's own 'running critical' memory warning: does the app stay up, and what does it give back?"""
        res = {"tabs": 0, "pss": [], "avail": []}
        self.ph.debug("closetabs")
        time.sleep(3)
        for url in MEMORY_SITES:
            self.ph.debug("normtab", url)
            res["tabs"] += 1
            time.sleep(9)
            pss, avail, procs = self.sample()
            res["pss"].append(pss)
            res["avail"].append(avail)
        m = self.log.size()
        self.ph.a.sh("am send-trim-memory %s RUNNING_CRITICAL" % self.a.pkg, timeout=30)
        time.sleep(6)
        pss, avail, procs = self.sample()
        res["after_trim_pss"], res["after_trim_avail"] = pss, avail
        lines = self.log.since(m)
        res["trim_lines"] = [e["rest"][:120] for e in health_events(lines, "mem", "trim")][:3]
        res["hibernated"] = len([e for e in health_events(self.log.since(0), "tabs", "hibernate")])
        res["max_pss"] = max(res["pss"] + [pss])
        res["min_avail"] = min(res["avail"] + [avail])
        self.ph.debug("closetabs")
        time.sleep(3)
        return res

    # ------------------------------------------------------------------ security and feature probes (LAB3c)
    def d_reach(self, url):
        """Can the runner open a TCP connection to the site? (urllib would refuse a bad certificate and call a reachable test site unreachable.)"""
        host = host_of(url)
        if self.a.selftest:
            return True
        try:
            socket.create_connection((host, 443), 8).close()
            return True
        except OSError:
            return False

    def d_sec_rows(self, tag, since):
        return [r for r in self.lab.since(since) if r.get("kind") == "sec" and r.get("tag") == tag]

    def d_security(self):
        res = {"battery": 0}
        tag = "sec1"
        # A. the in-page battery: what a hostile page can do without a tap, and what a tracker can see
        r0 = self.lab.size()
        m_sec = self.log.size()
        self.ph.open_url("%s/sec?tag=%s" % (self.lab_base, tag))
        t0 = time.time()
        while time.time() - t0 < self.d_t(45) and not [r for r in self.d_sec_rows(tag, r0) if r.get("name") == "battery-done"]:
            time.sleep(1.5)
        asked = self.d_dismiss_dialogs()                       # (the battery's camera and location requests are still waiting for a tap)
        time.sleep(3)
        rows = self.d_sec_rows(tag, r0)
        seen = set()
        chips = [e for e in health_events(self.log.since(m_sec), "guard", "chip-show")]
        for r in rows:
            nm = r.get("name", "?")
            if nm in seen or nm == "battery-done":
                continue
            seen.add(nm)
            res["battery"] += 1
            verdict, detail = r.get("r", "info"), r.get("v", "")
            if nm == "popup-without-tap" and verdict in ("info", "fail"):       # lab run 43997607118: the app blocks the pop-up and shows its chip, but hands the page a stand-in window
                verdict = "pass" if chips else "fail"
                detail = ("window.open returned a stand-in and the app showed its blocked-page chip (%s)" % chips[0]["rest"][:60]) if chips else "window.open opened a window and no blocked-page chip was shown"
            if nm in ("camera-microphone", "location") and verdict == "info":
                hit = [t for t in asked if re.search("camera|microphone" if nm == "camera-microphone" else "location", t, re.I)]
                if hit:
                    verdict, detail = "pass", "the app asked the person first (dialog: '%s' with BLOCK / ALLOW) and nothing was granted without a tap" % hit[0][:90]
                elif not asked:
                    detail += "; no dialog was visible to the lab either"
            self.d_check(nm, verdict, detail, sev="medium" if nm.startswith("referer") else "high")
        if not rows:
            self.d_check("security-battery-ran", "fail", "the in-page battery reported nothing within 45 s", sev="medium", area="tooling")
        # B. the other site opened on its own must not find the cookie its frame set under this site
        self.ph.open_url("http://127.0.0.1:%d/sec/read?tag=%s" % (self.port, tag))
        time.sleep(7)
        for r in self.d_sec_rows(tag, r0):
            if r.get("name") == "third-party-cookie-read":
                self.d_check("third-party-cookie-read", r.get("r", "info"), r.get("v", ""))
        # C. addresses that belong to other apps, tried by a page without a tap
        m = self.log.size()
        self.ph.open_url("%s/sec/scheme?tag=%s" % (self.lab_base, tag))
        time.sleep(14)
        focus = self.ph.a.sh("dumpsys window | grep -m1 mCurrentFocus", timeout=30)
        started = [ln for ln in self.log.since(m) if "START u0" in ln and any(w in ln for w in SCHEME_WORDS)]
        if started or (self.a.pkg not in focus and "mCurrentFocus" in focus):
            self.d_check("page-cannot-launch-other-apps", "fail", "after a page tried tel:/sms:/mailto:/market:/intent: with no tap, focus is '%s' and %d START lines name those schemes: %s" % (focus.strip()[:80], len(started), (started[0][:150] if started else "")))
            self.ph.start_app()
            time.sleep(3)
        else:
            self.d_check("page-cannot-launch-other-apps", "pass", "the app stayed in front, no other app was started by the 8 addresses")
        # D. a file:// page must not run
        r1 = self.lab.size()
        local = os.path.join(self.out, "file-test.html")
        with open(local, "w") as f:
            f.write("<!doctype html><title>file page</title><h1>FILE PAGE</h1><script>new Image().src='http://localhost:%d/report?kind=sec&tag=%s&name=file-url-script&r=fail&v=a+file+page+RAN';</script>" % (self.port, tag))
        self.ph.a.sh("mkdir -p /sdcard/Download", timeout=30)
        self.ph.a.run("push", local, "/sdcard/Download/kestrel-file-test.html", timeout=60)
        m2 = self.log.size()
        self.ph.open_url("file:///sdcard/Download/kestrel-file-test.html")
        time.sleep(8)
        ran = [r for r in self.lab.since(r1) if r.get("name") == "file-url-script"]
        errs = [e["kv"].get("kind", "?") for e in health_events(self.log.since(m2), "page", "error")]
        self.d_check("file-url-script-does-not-run", "fail" if ran else "pass", "a file:// page's script %s (the app said: %s)" % ("RAN and phoned home" if ran else "did not run", errs or "no error page"))
        # E. bad certificates are never clicked through
        for nm, url in BAD_CERTS:
            if not self.d_reach(url):
                self.d_check("bad-certificate-%s" % nm, "info", "the runner cannot reach %s: not measured" % host_of(url), area="security")
                continue
            m3 = self.log.size()
            self.ph.open_url(url)
            k, kv, s = self.d_wait_page(m3, 40)
            if nm == "expired":
                self.shot("daily-badcert-expired")
            # the app's 'page loaded' line is written when the load ENDS, also for its own error page: ok=false (no content) is what a refused certificate looks like (lab run 43997607118: the
            # four pages logged ok=false and the Diagnostics page report said '1 FAILED'); ok=true would mean content was shown
            if k == "error" or (k == "loaded" and kv.get("ok") == "false"):
                self.d_check("bad-certificate-%s" % nm, "pass", "the load was refused (page %s, ok=%s, %s ms): no content shown" % (k, kv.get("ok"), kv.get("done", "?")))
            elif k == "loaded":
                self.d_check("bad-certificate-%s" % nm, "fail", "the page LOADED with ok=true although its certificate is %s" % nm)
            else:
                self.d_check("bad-certificate-%s" % nm, "info", "no page event in 40 s")
        # F. plain http: upgraded to https (Shields), and mixed content
        before = self.dump_diag("sec-before-http")
        for nm, url in (("http-upgrade", "http://example.com/"), ("mixed-content", "https://mixed.badssl.com/")):
            if not self.d_reach(url):
                self.d_check(nm, "info", "the runner cannot reach %s: not measured" % host_of(url))
                continue
            m4 = self.log.size()
            self.ph.open_url(url)
            k, kv, s = self.d_wait_page(m4, 40)
            time.sleep(3)
            lines4 = self.log.since(m4)
            con = console_summary(lines4)
            up = [ln for ln in lines4 if re.search(r"Upgrading insecure (display )?request", ln)]
            blocked = [ln for ln in lines4 if re.search(r"Blocked loading mixed", ln)]
            detail = "page event %s host=%s ok=%s; console errors %d" % (k, kv.get("host"), kv.get("ok"), con["errors"])
            if nm == "mixed-content":
                self.d_check(nm, "pass" if (up or blocked) else "info", detail + ("; the engine upgraded %d insecure picture request(s) to https" % len(up) if up else "; blocked %d" % len(blocked) if blocked else "; no mixed-content line seen"))
            else:
                self.shot("daily-http-upgrade")
                addr = self.d_address_node(self.d_dump("after-http-upgrade"))
                shown = addr["text"] if addr else ""
                if shown.startswith("https://"):
                    self.d_check(nm, "pass", detail + "; typed http://example.com/, the address control shows %s" % shown[:80])
                elif shown.startswith("http://"):
                    self.d_check(nm, "fail", detail + "; typed http://example.com/ but the address control shows %s: the page was NOT upgraded to https" % shown[:80])
                else:
                    self.d_check(nm, "info", detail + "; the address control's text was %r" % shown[:80])
        after = self.dump_diag("sec-after-http")
        up_b = [ln for ln in before.splitlines() if re.search(r"upgrad", ln, re.I)]
        up_a = [ln for ln in after.splitlines() if re.search(r"upgrad", ln, re.I)]
        self.d_check("https-upgrade-counter", "pass" if up_a != up_b else "info", "Diagnostics before: %s | after: %s" % (up_b[:1], up_a[:1]))
        # G. the older guards: Safe Browsing pages, installers that pretend to be Kestrel, downloads nobody tapped
        try:
            if not self.a.selftest:
                self.safe_browsing("daily")
                self.apk_guard()
                self.guard_downloads()
        except Exception as e:
            self.issue("low", "tooling", "daily security guards raised %r" % (e,))
        return res

    # ------------------------------------------------------------------ the whole day
    def daily(self, minutes):
        if self.a.selftest:
            minutes = max(minutes, 30)                          # (the fake phone: every step must run once so that a Python error in one shows)
        deadline = time.time() + minutes * 60
        self.ph.measure_on()
        time.sleep(2)
        self.ph.debug("nolook")                                 # the page picture read-back stalls the UI thread for seconds on the emulator's software GL (not seen on the tablet)
        time.sleep(2)
        self.d_facts()
        self.d_bench("start")
        plan = [("cold-start", self.d_cold_start, "start"), ("home-screen", self.d_home, "usage"), ("search-typed-in-address-bar", self.d_search_typed, "feature"),
                ("read-a-long-article", self.d_read_article, "scroll"), ("several-tabs-and-switching", self.d_tabs, "usage"), ("back-presses", self.d_back_forward, "usage"),
                ("typing-in-a-form-field", self.d_form, "typing"), ("film-in-a-page-press-play", self.d_film_in_page, "video"), ("film-on-a-real-site", self.d_real_film, "video"),
                ("download-then-downloads-screen", self.d_download, "usage"), ("app-screens-from-the-menu", self.d_settings_walk, "usage"),
                ("private-tab-and-shields-switch", self.d_private_and_shields, "usage"), ("leave-the-app-and-return", self.d_leave_and_return, "usage"),
                ("many-tabs-and-memory-warning", self.d_memory, "memory"), ("security-and-feature-probes", self.d_security, "security")]
        for name, fn, area in plan:
            if time.time() > deadline:
                self.record({"kind": "step", "name": name, "area": area, "status": "skipped", "note": "the day's time budget (%d min) was used up" % minutes})
                continue
            self.step(name, fn, area)
            self.ph.debug("closetabs") if name in ("several-tabs-and-switching", "film-on-a-real-site", "private-tab-and-shields-switch") else None
            time.sleep(2)
        self.d_bench("end")
        self.dump_diag("daily-end")


def selftest_ok():
    return True
