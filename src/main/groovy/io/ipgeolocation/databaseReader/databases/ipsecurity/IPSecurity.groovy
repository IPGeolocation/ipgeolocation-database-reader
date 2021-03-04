package io.ipgeolocation.databaseReader.databases.ipsecurity

import groovy.transform.CompileStatic

@CompileStatic
class IPSecurity {
    String ipAddress
    Integer threatScore
    Boolean isTor
    Boolean isProxy
    String proxyType
    Boolean isAnonymous
    Boolean isKnownAttacker

    final String toCSV() {
        "${ipAddress},${threatScore},${isTor},${isProxy},${proxyType},${isAnonymous},${isKnownAttacker}".toString()
    }
}
