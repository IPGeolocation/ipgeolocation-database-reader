package io.ipgeolocation.databaseReader.databases.ipsecurity

import groovy.transform.CompileStatic
import org.springframework.util.Assert

import static com.google.common.base.Preconditions.checkNotNull

@CompileStatic
class IPSecurityIndexer {
    private List<IPSecurity> ipSecurities = []
    private final TreeMap<BigInteger, IPSecurity> ipv4Entries = new TreeMap<BigInteger, IPSecurity>()
    private final TreeMap<BigInteger, IPSecurity> ipv6Entries = new TreeMap<BigInteger, IPSecurity>()

    void add(IPSecurity ipSecurity) {
        checkNotNull(ipSecurity, "Pre-condition violated: IP-Security must not be null.")

        if (ipSecurity.isIPv6()) {
            ipv6Entries.put(inetAddressToBigInteger(ipSecurity.startIpAddress), ipSecurity)
        } else {
            ipv4Entries.put(inetAddressToBigInteger(ipSecurity.startIpAddress), ipSecurity)
        }
    }

    private static BigInteger inetAddressToBigInteger(InetAddress inetAddress) {
        Assert.notNull(inetAddress, "'inetAddress' must not be null.")

        new BigInteger(1, inetAddress.getAddress())
    }

    IPSecurity get(InetAddress inetAddress) {
        Assert.notNull(inetAddress, "'inetAddress' must not be null.")

        Map.Entry<?, IPSecurity> candidate
        IPSecurity ipSecurity = null

        // find and check candidate
        if (inetAddress instanceof Inet4Address) {
            candidate = ipv4Entries.floorEntry(inetAddressToBigInteger(inetAddress))
        } else {
            candidate = ipv6Entries.floorEntry(inetAddressToBigInteger(inetAddress))
        }

        if (candidate) {
            BigInteger start = inetAddressToBigInteger(candidate.value.startIpAddress)
            BigInteger end = inetAddressToBigInteger(candidate.value.endIpAddress)
            BigInteger address = inetAddressToBigInteger(inetAddress)

            // find the address is in range of the candidate
            ipSecurity = start <= address && address <= end ? candidate.value : null
        }

        ipSecurity
    }

    Integer size() {
        ipv4Entries.size() + ipv6Entries.size()
    }

    List<IPSecurity> getIPSecurities() {
        ipSecurities.addAll(ipv4Entries.values())
        ipSecurities.addAll(ipv6Entries.values())
        ipSecurities
    }
}
