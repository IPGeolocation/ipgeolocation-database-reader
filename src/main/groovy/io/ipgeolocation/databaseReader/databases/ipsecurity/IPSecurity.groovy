package io.ipgeolocation.databaseReader.databases.ipsecurity

import groovy.transform.CompileStatic

@CompileStatic
class IPSecurity {
    String ipAddress
    Integer threatScore
    String proxyType
    Boolean isTor, isProxy, isAnonymous, isKnownAttacker, isBot, isSpam

    final String toCSV() {
        "${ipAddress},${threatScore},${isTor},${isProxy},${proxyType},${isAnonymous},${isKnownAttacker}".toString()
    }
}
