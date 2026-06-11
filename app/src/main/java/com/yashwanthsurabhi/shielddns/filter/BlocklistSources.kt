package com.yashwanthsurabhi.shielddns.filter

/**
 * Public open-source blocklist URLs. Files are downloaded and cached on device;
 * no per-query API is used.
 *
 * Each source maps to a REAL category. We no longer guess malware/phishing/adult/
 * gambling from an ad list — those come from curated, category-specific feeds:
 *  - HaGeZi "Pro" : ads + tracking (the only mixed list; split coarsely)
 *  - HaGeZi "TIF" + URLhaus + Phishing Army : threat intelligence (malware/phishing)
 *  - HaGeZi "NSFW" : adult
 *  - HaGeZi "Gambling" : gambling
 */
object BlocklistSources {
    private const val HAGEZI_HOSTS =
        "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/hosts"

    // General ads + tracker "annoyance" list (mixed; split coarsely into ads/trackers/telemetry).
    const val HAGEZI_PRO = "$HAGEZI_HOSTS/pro.txt"

    // Fallback general list if the primary annoyance source is unreachable.
    const val STEVEN_BLACK_HOSTS =
        "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"

    // Threat intelligence — genuine malware / phishing / scam domains.
    const val HAGEZI_TIF = "$HAGEZI_HOSTS/tif.txt"
    const val URLHAUS_HOSTFILE = "https://urlhaus.abuse.ch/downloads/hostfile/"
    const val PHISHING_ARMY = "https://phishing.army/download/phishing_army_blocklist.txt"

    // Dedicated category feeds.
    const val HAGEZI_NSFW = "$HAGEZI_HOSTS/nsfw.txt"
    const val HAGEZI_GAMBLING = "$HAGEZI_HOSTS/gambling.txt"
}
