package com.example.data.local

object BundledAdultDomains {
    val INITIAL_ADULT_DOMAINS: Set<String> = hashSetOf(
        // Major Adult Video Platforms
        "pornhub.com", "phncdn.com", "pornhubpremium.com", "pornhub.org",
        "xvideos.com", "xvideos-cdn.com", "xvideos.es", "xvideos.in",
        "xnxx.com", "xnxx-cdn.com", "xnxx.tv", "xnxx.club",
        "redtube.com", "redtube.net",
        "youporn.com", "ypncdn.com",
        "xhamster.com", "xhamster.desi", "xhcdn.com", "xhamsterlive.com",
        "spankbang.com", "spankbang.party",
        "eporner.com", "eporner.org",
        "beeg.com", "beeg.live",
        "fapster.xxx", "fapster.com",
        "tubegalore.com",
        "motherless.com",
        "daftsex.com",
        "txxx.com",
        "hclips.com",
        "pornhat.com",
        "porn555.com",
        "voyeurhit.com",
        "tnaflix.com",
        "empflix.com",
        "anysex.com",
        "heavy-r.com",
        "efukt.com",
        "heavyr.com",
        "hqporner.com",
        "porn.com",
        "tube8.com",
        "slutload.com",
        "hardsextube.com",
        "freeones.com",
        "sex.com",
        "gotporn.com",
        "sunporno.com",
        "porn300.com",
        "vxxx.com",
        "porntube.com",
        "pornmd.com",
        "4tube.com",
        "pornpros.com",
        "porndoe.com",
        "pornktube.com",
        "xcafe.com",
        "cumlouder.com",
        "hotmovs.com",
        "cliphunter.com",
        "upornia.com",
        "pornone.com",
        "palimas.com",
        "nuvid.com",
        "drtuber.com",
        "porntrex.com",
        "flyflv.com",
        "fapvid.com",
        "zenra.net",
        "freeomovie.to",
        "clipsex.biz",

        // Live Cam & Adult Chat Networks
        "chaturbate.com", "chaturbate.live",
        "stripchat.com", "stripchat.tv", "stripchatcdn.com",
        "bongacams.com", "bongacams2.com", "bongacams.tv",
        "livejasmin.com", "jasmin.com",
        "cam4.com", "cam4.es",
        "camsoda.com",
        "myfreecams.com", "mfcimg.com",
        "flirt4free.com",
        "imlive.com",
        "streamate.com",
        "camversity.com",
        "babestation.tv",
        "adultwork.com",
        "camzap.com",
        "dirtyroulette.com",
        "chatrandomsex.com",
        "freecams.com",
        "webcamclub.com",

        // Subscription & Fan Platforms
        "onlyfans.com", "fansly.com", "manyvids.com", "clips4sale.com",
        "loyalfans.com", "admireme.vip",

        // Adult Networks & Studios
        "brazzers.com", "brazzerspremium.com",
        "bangbros.com", "bangbrosnetwork.com",
        "realitykings.com", "rk.com",
        "naughtyamerica.com",
        "mofos.com", "twistys.com",
        "babes.com", "digitalplayground.com",
        "wicked.com", "hustler.com", "penthouse.com",
        "playboy.com", "vixen.com", "blacked.com", "tushy.com", "deeper.com",
        "private.com", "evilangel.com", "kink.com",
        "adulttime.com", "scoreland.com", "teamskeet.com",

        // Adult Comics, Hentai & Manga
        "nhentai.net", "nhentai.xxx",
        "hanime.tv",
        "rule34.xxx", "rule34.paheal.net",
        "e-hentai.org", "exhentai.org",
        "tsumino.com", "hitomi.la",
        "hentaihaven.xxx", "hentaihaven.red",
        "luscious.net", "gelbooru.com", "danbooru.donmai.us",
        "hentai2read.com", "pururin.io", "fakku.net",

        // Adult Image, Gif & Short Video Sites
        "redgifs.com", "erome.com", "pornpics.com",
        "imagefap.com", "scrolller.com", "bdsmlr.com",
        "sexhd.xxx", "pornimages.com", "sexy-egirls.com",

        // Adult Dating & Escort Directories
        "adultfriendfinder.com", "aff.com",
        "ashleymadison.com", "fling.com", "passion.com",
        "bedpage.com", "skipthegames.com", "listcrawler.eu",
        "eros.com", "cityvibe.com", "slixa.com",
        "eurogirlsescort.com", "vipbox.xxx"
    )

    fun normalizeDomain(rawInput: String): String {
        var domain = rawInput.trim().lowercase()
        // Strip scheme if entered
        if (domain.startsWith("http://")) {
            domain = domain.substring(7)
        } else if (domain.startsWith("https://")) {
            domain = domain.substring(8)
        }
        // Strip path, parameters, ports
        val slashIdx = domain.indexOf('/')
        if (slashIdx != -1) {
            domain = domain.substring(0, slashIdx)
        }
        val colonIdx = domain.indexOf(':')
        if (colonIdx != -1) {
            domain = domain.substring(0, colonIdx)
        }
        // Remove trailing dot if present in DNS
        if (domain.endsWith('.')) {
            domain = domain.substring(0, domain.length - 1)
        }
        return domain
    }

    /**
     * Checks if a queried domain matches a blocked domain entry or is a subdomain of a blocked domain.
     * E.g., if "pornhub.com" is blocked:
     * - "pornhub.com" -> true
     * - "m.pornhub.com" -> true
     * - "cdn.video.pornhub.com" -> true
     * - "pornhub.com.br" -> false (not a valid subdomain of pornhub.com)
     * - "notpornhub.com" -> false
     */
    fun matchesBlockedDomain(queriedDomain: String, blockedDomain: String): Boolean {
        val q = queriedDomain.lowercase().trimEnd('.')
        val b = blockedDomain.lowercase().trimEnd('.')
        if (q == b) return true
        if (q.endsWith(".$b")) return true
        return false
    }
}
