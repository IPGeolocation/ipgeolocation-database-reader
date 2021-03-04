package io.ipgeolocation.databaseReader.databases.ipsecurity

import groovy.transform.CompileStatic

import static com.google.common.base.Preconditions.checkNotNull

@CompileStatic
class IPSecurityIndexer {
    private Map<String, IPSecurity> ipSecurities = [:]

    void add(IPSecurity ipSecurity) {
        checkNotNull(ipSecurity, "Pre-condition violated: IP-Security must not be null.")

        ipSecurities.put(ipSecurity.ipAddress, ipSecurity)
    }

    IPSecurity get(String ipAddress) {
        checkNotNull(ipAddress, "Pre-condition violated: IP address must not be null.")

        ipSecurities.get(ipAddress)
    }

    Integer size() {
        ipSecurities.size()
    }

    List<IPSecurity> getIPSecurities() {
        this.ipSecurities.values().asList()
    }
}
