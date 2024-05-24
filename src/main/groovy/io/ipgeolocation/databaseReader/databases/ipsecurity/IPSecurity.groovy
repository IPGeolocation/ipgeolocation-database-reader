package io.ipgeolocation.databaseReader.databases.ipsecurity

import com.maxmind.db.MaxMindDbConstructor
import com.maxmind.db.MaxMindDbParameter
import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeFields = true)
class IPSecurity {
    final InetAddress startIpAddress
    final InetAddress endIpAddress
    final Integer threatScore
    final String proxyType, isTor, isProxy, isAnonymous, isKnownAttacker, isBot, isSpam

    IPSecurity(InetAddress startIpAddress, InetAddress endIpAddress, Integer threatScore, String isProxy, String proxyType,
               String isTor, String isAnonymous, String isKnownAttacker, String isBot, String isSpam) {
        this.startIpAddress = startIpAddress
        this.endIpAddress = endIpAddress
        this.threatScore = threatScore
        this.isProxy = isProxy
        this.proxyType = proxyType
        this.isTor = isTor
        this.isAnonymous = isAnonymous
        this.isKnownAttacker = isKnownAttacker
        this.isBot = isBot
        this.isSpam = isSpam
    }

    Boolean isIPv6() {
        startIpAddress instanceof Inet6Address
    }

    @MaxMindDbConstructor
    IPSecurity(
            @MaxMindDbParameter(name = "network") String network,
            @MaxMindDbParameter(name = "threat_score") String threatScore,
            @MaxMindDbParameter(name = "proxy_type") String proxyType,
            @MaxMindDbParameter(name = "is_tor") String isTor,
            @MaxMindDbParameter(name = "is_proxy") String isProxy,
            @MaxMindDbParameter(name = "is_anonymous") String isAnonymous,
            @MaxMindDbParameter(name = "is_known_attacker") String isKnownAttacker,
            @MaxMindDbParameter(name = "is_bot") String isBot,
            @MaxMindDbParameter(name = "is_spam") String isSpam) {
        this(null, null, Integer.parseInt(threatScore), isProxy, proxyType, isTor, isAnonymous, isKnownAttacker, isBot, isSpam)
    }
}
