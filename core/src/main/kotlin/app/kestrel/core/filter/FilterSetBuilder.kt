package app.kestrel.core.filter

import java.io.ByteArrayOutputStream
import java.util.Arrays

/** Counters describing what a build consumed and why lines were skipped (shown in About and asserted in tests). */
class BuildReport {
    @JvmField var lines = 0
    @JvmField var hostRules = 0
    @JvmField var hostExceptions = 0
    @JvmField var patternRules = 0
    @JvmField var siteRules = 0
    @JvmField var badfilters = 0
    @JvmField var cosmeticSite = 0
    @JvmField var cosmeticGeneric = 0
    @JvmField var cosmeticExceptions = 0
    @JvmField var queryRules = 0
    @JvmField var scriptlets = 0
    @JvmField val skipped = IntArray(Skip.values().size)

    fun skippedCount(s: Skip): Int = skipped[s.ordinal]
    override fun toString(): String = buildString {
        append("lines=$lines host=$hostRules hostExc=$hostExceptions pattern=$patternRules site=$siteRules badfilter=$badfilters ")
        append("cosSite=$cosmeticSite cosGeneric=$cosmeticGeneric cosExc=$cosmeticExceptions scriptlets=$scriptlets query=$queryRules skipped{")
        for (s in Skip.values()) if (s != Skip.NONE && skipped[s.ordinal] > 0) append("$s=${skipped[s.ordinal]} ")
        append("}")
    }
}

/**
 * Compiles filter-list text into an immutable [FilterSet]. Streaming: feed lines, call [build] once.
 * Not thread-safe. Runs on the desktop (list compiler tool) and on the tablet (weekly updater, background thread).
 */
class FilterSetBuilder {
    private class Pending(@JvmField val r: NetRule, @JvmField val category: Int)

    private val parser = RuleParser()
    val report = BuildReport()
    private val listNames = ArrayList<String>()

    private var hN = 0
    private var hHash = LongArray(4096); private var hFlags = IntArray(4096); private var hKey = LongArray(4096)
    private var eN = 0
    private var eHash = LongArray(512); private var eFlags = IntArray(512); private var eKey = LongArray(512)
    private val siteMap = HashMap<Long, Int>()
    private val pending = ArrayList<Pending>()
    private val badKeys = HashSet<Long>()

    private val cosSite = HashMap<Long, LinkedHashSet<String>>()
    private val cosExc = HashMap<Long, LinkedHashSet<String>>()
    private val cosGeneric = LinkedHashSet<String>()
    private val cosGenericExc = LinkedHashSet<String>()

    private val queryRules = ArrayList<QueryRule>()
    private val queryBad = HashSet<Long>()

    // ------------------------------------------------------------------ input

    /** Adblock Plus / uBlock Origin / Brave syntax. */
    fun addAbpList(name: String, category: Int, lines: Sequence<String>) {
        listNames.add(name)
        for (l in lines) addAbpLine(l, category)
    }

    /** hosts-file (`0.0.0.0 domain`) or one-domain-per-line lists. */
    fun addHostsList(name: String, category: Int, lines: Sequence<String>) {
        listNames.add(name)
        for (l in lines) addHostsLine(l, category)
    }

    fun addAbpLine(rawLine: String, category: Int) {
        report.lines++
        val line = rawLine.trim()
        if (line.isEmpty()) return
        val c0 = line[0]
        if (c0 == '!' || (c0 == '[' && line.startsWith("[Adblock", ignoreCase = true))) return
        if (RuleParser.looksCosmetic(line)) { addCosmetic(line); return }
        if (QueryRuleParser.looks(line)) { addQuery(line); return }

        val r = parser.parseNetwork(line)
        if (r == null) { report.skipped[parser.skip.ordinal]++; return }
        if (r.badfilter) { badKeys.add(r.key); report.badfilters++; return }

        if (r.siteFlags != 0) {
            val host = parser.hostOnlyPattern(r.leftHost, r.leftAnchor, r.rightAnchor, r.pattern)!!
            val h = Hash.h64(host)
            siteMap[h] = (siteMap[h] ?: 0) or r.siteFlags
            report.siteRules++
            return
        }

        var types = r.types
        if (category == Category.MALWARE && types == RequestType.ALL_SUBRESOURCES) types = RequestType.ALL // also block the page itself
        val host = if (r.include == null && r.exclude == null) parser.hostOnlyPattern(r.leftHost, r.leftAnchor, r.rightAnchor, r.pattern) else null
        // A bare "||host" without '^' also matches "host.evil.com": only the '^' form is a pure host rule.
        if (host != null && r.pattern.endsWith("^")) {
            val flags = Bits.pack(types, r.party, r.important, category)
            if (r.exception) { addException(Hash.h64(host), flags, r.key); report.hostExceptions++ }
            else { addHost(Hash.h64(host), flags, r.key); report.hostRules++ }
            return
        }
        pending.add(Pending(if (types == r.types) r else copyWithTypes(r, types), category))
        report.patternRules++
    }

    fun addHostsLine(rawLine: String, category: Int) {
        report.lines++
        var line = rawLine
        val hash = line.indexOf('#')
        if (hash >= 0) line = line.substring(0, hash)
        line = line.trim()
        if (line.isEmpty()) return
        val parts = line.split(' ', '\t').filter { it.isNotEmpty() }
        val names = if (parts.size >= 2 && (parts[0] == "0.0.0.0" || parts[0] == "127.0.0.1" || parts[0] == "::1" || parts[0] == "::" || parts[0] == "0"))
            parts.subList(1, parts.size) else if (parts.size == 1) parts else return
        val types = if (category == Category.MALWARE) RequestType.ALL else RequestType.ALL_SUBRESOURCES
        for (n in names) {
            val host = n.lowercase().trimEnd('.')
            if (host in HOSTS_SKIP || !RuleParser.isHostName(host) || host.indexOf('.') < 0) { report.skipped[Skip.BAD_SYNTAX.ordinal]++; continue }
            addHost(Hash.h64(host), Bits.pack(types, 0, false, category), Hash.h64("||$host^"))
            report.hostRules++
        }
    }

    private fun addCosmetic(line: String) {
        val c = parser.parseCosmetic(line)
        if (c == null) { report.skipped[parser.skip.ordinal]++; return }
        if (c.exception) {
            report.cosmeticExceptions++
            if (c.domains.isEmpty()) cosGenericExc.add(c.selector)
            else for (d in c.domains) cosExc.getOrPut(Hash.h64(d)) { LinkedHashSet() }.add(c.selector)
        } else {
            if (ScriptletRules.isStored(c.selector)) report.scriptlets++
            if (c.domains.isEmpty()) {
                if (cosGeneric.add(c.selector)) report.cosmeticGeneric++
            } else {
                report.cosmeticSite++
                for (d in c.domains) cosSite.getOrPut(Hash.h64(d)) { LinkedHashSet() }.add(c.selector)
            }
            // `~site.com`: off on that site = an exception of the same text there (what `site.com#@#...` means)
            for (x in c.excluded) cosExc.getOrPut(Hash.h64(x)) { LinkedHashSet() }.add(c.selector)
        }
    }

    /** `$removeparam` (AA3-3b): kept in its own table, applied to top-level navigations only ([QueryRules]). */
    private fun addQuery(line: String) {
        val res = QueryRuleParser.parse(line)
        val rule = res.rule
        if (rule == null) { report.skipped[res.skip.ordinal]++; return }
        if (rule.badfilter) { queryBad.add(rule.key); report.badfilters++; return }
        if (queryRules.size >= QueryRules.MAX_RULES) { report.skipped[Skip.BAD_SYNTAX.ordinal]++; return }
        queryRules.add(rule)
    }

    private fun copyWithTypes(r: NetRule, types: Int) = NetRule(
        r.pattern, r.exception, r.leftHost, r.leftAnchor, r.rightAnchor, types, r.party, r.important, r.include, r.exclude,
        r.badfilter, r.siteFlags, r.key,
    )

    private fun addHost(h: Long, f: Int, key: Long) {
        if (hN == hHash.size) { val n = hN * 2; hHash = hHash.copyOf(n); hFlags = hFlags.copyOf(n); hKey = hKey.copyOf(n) }
        hHash[hN] = h; hFlags[hN] = f; hKey[hN] = key; hN++
    }

    private fun addException(h: Long, f: Int, key: Long) {
        if (eN == eHash.size) { val n = eN * 2; eHash = eHash.copyOf(n); eFlags = eFlags.copyOf(n); eKey = eKey.copyOf(n) }
        eHash[eN] = h; eFlags[eN] = f; eKey[eN] = key; eN++
    }

    // ------------------------------------------------------------------ output

    fun build(createdAtMillis: Long = System.currentTimeMillis()): FilterSet {
        val hosts = sortedTable(hHash, hFlags, hKey, hN)
        val excs = sortedTable(eHash, eFlags, eKey, eN)

        val siteKeys = siteMap.keys.toLongArray().also { Arrays.sort(it) }
        val siteFlagArr = IntArray(siteKeys.size) { siteMap[siteKeys[it]]!! }

        val live = pending.filter { it.r.key !in badKeys }
        val patterns = compilePatterns(live)
        val cos = compileCosmetic()
        val query = QueryRules.compile(queryRules.filter { it.key !in queryBad })
        report.queryRules = query.count
        val meta = FilterMeta(
            createdAtMillis, listNames.toList(), hosts.hashes.size, excs.hashes.size, patterns.count, siteKeys.size,
            report.cosmeticSite, cos.genericCount,
        )
        return FilterSet(hosts, excs, siteKeys, siteFlagArr, patterns, cos, meta, query)
    }

    private fun sortedTable(hash: LongArray, flags: IntArray, key: LongArray, n: Int): HostTable {
        val keep = IntArray(n)
        var m = 0
        for (i in 0 until n) if (key[i] !in badKeys) keep[m++] = i
        val keys = LongArray(m) { hash[keep[it]] }
        val order = sortedOrder(keys, m)
        val outH = LongArray(m)
        val outF = IntArray(m)
        var w = 0
        for (o in order) {
            val h = keys[o]
            val f = flags[keep[o]]
            if (w > 0 && outH[w - 1] == h && outF[w - 1] == f) continue
            outH[w] = h; outF[w] = f; w++
        }
        return HostTable(outH.copyOf(w), outF.copyOf(w))
    }

    private fun compilePatterns(list: List<Pending>): PatternTable {
        val n = list.size
        if (n == 0) return PatternTable.EMPTY
        val offs = IntArray(n + 1)
        var total = 0
        for (i in 0 until n) { offs[i] = total; total += list[i].r.pattern.length }
        offs[n] = total
        val bytes = ByteArray(total)
        val meta = IntArray(n)
        val dom = IntArray(n)
        var pool = LongArray(256)
        var poolN = 0
        for (i in 0 until n) {
            val r = list[i].r
            val p = r.pattern
            for (k in p.indices) bytes[offs[i] + k] = p[k].code.toByte()
            var w = Bits.pack(r.types, r.party, r.important, list[i].category)
            if (r.exception) w = w or Bits.EXCEPTION
            if (r.leftHost) w = w or Bits.LEFT_HOST
            if (r.leftAnchor) w = w or Bits.LEFT_ANCHOR
            if (r.rightAnchor) w = w or Bits.RIGHT_ANCHOR
            meta[i] = w
            val inc = r.include; val exc = r.exclude
            if (inc != null || exc != null) {
                val ni = inc?.size ?: 0; val ne = exc?.size ?: 0
                val need = 2 + ni + ne
                if (poolN + need > pool.size) pool = pool.copyOf(maxOf(pool.size * 2, poolN + need))
                dom[i] = poolN + 1
                pool[poolN++] = ni.toLong(); pool[poolN++] = ne.toLong()
                if (inc != null) for (h in inc) pool[poolN++] = h
                if (exc != null) for (h in exc) pool[poolN++] = h
            }
        }

        // ---- token selection: pick, per rule, its least-frequent "whole URL token" candidate
        val cand = arrayOfNulls<IntArray>(n)
        val freq = HashMap<Int, Int>()
        for (i in 0 until n) {
            val r = list[i].r
            val c = candidateTokens(r.pattern, r.leftHost || r.leftAnchor, r.rightAnchor)
            if (c.isNotEmpty()) {
                cand[i] = c
                for (k in 0 until c.size step 2) freq[c[k]] = (freq[c[k]] ?: 0) + 1
            }
        }
        val chosen = IntArray(n)
        val hasTok = BooleanArray(n)
        val noTok = ArrayList<Int>()
        for (i in 0 until n) {
            val c = cand[i]
            if (c == null) { noTok.add(i); continue }
            var best = c[0]; var bestF = freq[best]!!; var bestLen = c[1]
            var k = 2
            while (k < c.size) {
                val f = freq[c[k]]!!
                if (f < bestF || (f == bestF && c[k + 1] > bestLen)) { best = c[k]; bestF = f; bestLen = c[k + 1] }
                k += 2
            }
            chosen[i] = best; hasTok[i] = true
        }
        val tokenIndex = buildIndex(chosen, hasTok, n)

        // ---- fallback for rules with no safe whole token (e.g. "foo.js|", "adserver."): index by the rarest literal 4-gram,
        //      so they cost one probe per URL position instead of being scanned for every single request.
        val gramCand = arrayOfNulls<IntArray>(n)
        val gramFreq = HashMap<Int, Int>()
        for (i in noTok) {
            val g = literalGrams(list[i].r.pattern)
            if (g.isNotEmpty()) {
                gramCand[i] = g
                for (k in g) gramFreq[k] = (gramFreq[k] ?: 0) + 1
            }
        }
        val gramChosen = IntArray(n)
        val hasGram = BooleanArray(n)
        val stillNone = ArrayList<Int>()
        for (i in noTok) {
            val g = gramCand[i]
            if (g == null) { stillNone.add(i); continue }
            var best = g[0]
            var bestF = gramFreq[best]!!
            for (k in 1 until g.size) {
                val f = gramFreq[g[k]]!!
                if (f < bestF) { best = g[k]; bestF = f }
            }
            gramChosen[i] = best; hasGram[i] = true
        }
        val gramIndex = buildIndex(gramChosen, hasGram, n)
        return PatternTable(
            n, bytes, offs, meta, dom, pool.copyOf(poolN),
            tokenIndex.key, tokenIndex.start, tokenIndex.len, tokenIndex.ids,
            gramIndex.key, gramIndex.start, gramIndex.len, gramIndex.ids,
            stillNone.toIntArray(),
        )
    }

    private class Index(val key: IntArray, val start: IntArray, val len: IntArray, val ids: IntArray)

    /** Open-addressing key -> bucket-of-rule-ids table (CSR layout). Probing must match FilterSet.scanBucket exactly. */
    private fun buildIndex(keyOf: IntArray, has: BooleanArray, n: Int): Index {
        val counts = HashMap<Int, Int>()
        for (i in 0 until n) if (has[i]) counts[keyOf[i]] = (counts[keyOf[i]] ?: 0) + 1
        var cap = 16
        while (cap < counts.size * 2) cap = cap shl 1
        val key = IntArray(cap); val start = IntArray(cap); val len = IntArray(cap); val cursor = IntArray(cap)
        var running = 0
        for ((k, c) in counts) {
            var slot = Hash.slot(k, cap - 1)
            while (len[slot] != 0) slot = (slot + 1) and (cap - 1)
            key[slot] = k; start[slot] = running; len[slot] = c; cursor[slot] = running
            running += c
        }
        val ids = IntArray(running)
        for (i in 0 until n) {
            if (!has[i]) continue
            var slot = Hash.slot(keyOf[i], cap - 1)
            while (!(len[slot] != 0 && key[slot] == keyOf[i])) slot = (slot + 1) and (cap - 1)
            ids[cursor[slot]++] = i
        }
        return Index(key, start, len, ids)
    }

    /** Every 4-char window inside the pattern's literal runs (runs are broken by '*' and '^'); at most 64 per rule. */
    private fun literalGrams(p: String): IntArray {
        val out = ArrayList<Int>()
        var i = 0
        while (i < p.length && out.size < 64) {
            if (p[i] == '*' || p[i] == '^') { i++; continue }
            var j = i
            while (j < p.length && p[j] != '*' && p[j] != '^') j++
            var g = i
            while (g + 4 <= j && out.size < 64) { out.add(Hash.gram(p, g)); g++ }
            i = j
        }
        return out.toIntArray()
    }

    /** (hash, length) pairs of pattern runs that are guaranteed to appear as WHOLE tokens in any URL the rule matches. */
    private fun candidateTokens(p: String, leftBounded: Boolean, rightBounded: Boolean): IntArray {
        var out = IntArray(0)
        var i = 0
        while (i < p.length) {
            if (FilterSet.isAlnum(p[i])) {
                var j = i + 1
                while (j < p.length && FilterSet.isAlnum(p[j])) j++
                val len = j - i
                val leftOk = if (i == 0) leftBounded else p[i - 1] != '*'
                val rightOk = if (j == p.length) rightBounded else p[j] != '*'
                if (len in 3..64 && leftOk && rightOk) {
                    out = out.copyOf(out.size + 2)
                    out[out.size - 2] = Hash.token32(p, i, j)
                    out[out.size - 1] = len
                }
                i = j
            } else {
                i++
            }
        }
        return out
    }

    private fun compileCosmetic(): CosmeticIndex {
        val (sh, so, sp) = cosPool(cosSite)
        val (eh, eo, ep) = cosPool(cosExc)
        val gen = cosGeneric.joinToString("\n").toByteArray(Charsets.UTF_8)
        val genExc = cosGenericExc.joinToString("\n").toByteArray(Charsets.UTF_8)
        return CosmeticIndex(sh, so, sp, eh, eo, ep, gen, cosGeneric.size, genExc)
    }

    private fun cosPool(map: HashMap<Long, LinkedHashSet<String>>): Triple<LongArray, IntArray, ByteArray> {
        val keys = map.keys.toLongArray().also { Arrays.sort(it) }
        val out = ByteArrayOutputStream()
        val offs = IntArray(keys.size + 1)
        for (i in keys.indices) {
            offs[i] = out.size()
            out.write(map[keys[i]]!!.joinToString("\n").toByteArray(Charsets.UTF_8))
        }
        offs[keys.size] = out.size()
        return Triple(keys, offs, out.toByteArray())
    }

    companion object {
        private val HOSTS_SKIP = setOf(
            "localhost", "localhost.localdomain", "local", "broadcasthost", "ip6-localhost", "ip6-loopback", "ip6-localnet",
            "ip6-mcastprefix", "ip6-allnodes", "ip6-allrouters", "ip6-allhosts", "0.0.0.0",
        )

        /** Stable merge sort of indices by [keys] (primitive arrays, no boxing; used for the big host tables). */
        internal fun sortedOrder(keys: LongArray, n: Int): IntArray {
            var src = IntArray(n) { it }
            var dst = IntArray(n)
            var width = 1
            while (width < n) {
                var i = 0
                while (i < n) {
                    val mid = minOf(i + width, n)
                    val hi = minOf(i + 2 * width, n)
                    var a = i; var b = mid; var k = i
                    while (a < mid && b < hi) { if (keys[src[a]] <= keys[src[b]]) dst[k++] = src[a++] else dst[k++] = src[b++] }
                    while (a < mid) dst[k++] = src[a++]
                    while (b < hi) dst[k++] = src[b++]
                    i += 2 * width
                }
                val t = src; src = dst; dst = t
                width *= 2
            }
            return src
        }
    }
}
