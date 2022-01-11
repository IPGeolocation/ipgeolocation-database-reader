package io.ipgeolocation.databaseReader.databases.ipsecurity

import com.maxmind.db.MaxMindDbConstructor
import com.maxmind.db.MaxMindDbParameter
import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString(includeFields = true)
class IPSecurity {
    final String ipAddress
    final Integer threatScore
    final String proxyType, isTor, isProxy, isAnonymous, isKnownAttacker, isBot, isSpam

    IPSecurity(String ipAddress, Integer threatScore, String isProxy, String proxyType, String isTor,
               String isAnonymous, String isKnownAttacker, String isBot, String isSpam) {
        this.ipAddress = ipAddress
        this.threatScore = threatScore
        this.isProxy = isProxy
        this.proxyType = proxyType
        this.isTor = isTor
        this.isAnonymous = isAnonymous
        this.isKnownAttacker = isKnownAttacker
        this.isBot = isBot
        this.isSpam = isSpam
    }

    @MaxMindDbConstructor
    IPSecurity(
            @MaxMindDbParameter(name = "threat_score") Integer threatScore,
            @MaxMindDbParameter(name = "proxy_type") String proxyType,
            @MaxMindDbParameter(name = "is_tor") String isTor,
            @MaxMindDbParameter(name = "is_proxy") String isProxy,
            @MaxMindDbParameter(name = "is_anonymous") String isAnonymous,
            @MaxMindDbParameter(name = "is_known_attacker") String isKnownAttacker,
            @MaxMindDbParameter(name = "is_bot") String isBot,
            @MaxMindDbParameter(name = "isSpam") String isSpam) {
        this(null, threatScore, isProxy, proxyType, isTor, isAnonymous, isKnownAttacker, isBot, isSpam)
    }
}
