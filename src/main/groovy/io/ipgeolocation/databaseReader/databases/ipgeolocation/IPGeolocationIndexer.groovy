package io.ipgeolocation.databaseReader.databases.ipgeolocation

import groovy.transform.CompileStatic
import org.springframework.util.Assert

@CompileStatic
class IPGeolocationIndexer {
    private final TreeMap<BigInteger, IPGeolocation> ipv4Entries = new TreeMap<BigInteger, IPGeolocation>()
    private final TreeMap<BigInteger, IPGeolocation> ipv6Entries = new TreeMap<BigInteger, IPGeolocation>()

    void add(IPGeolocation entry) {
        Assert.notNull(entry, "'entry' must not be null.")

        if (entry.isIPv6()) {
            ipv6Entries.put(inetAddressToBigInteger(entry.startIP), entry)
        } else {
            ipv4Entries.put(inetAddressToBigInteger(entry.startIP), entry)
        }
    }

    private static BigInteger inetAddressToBigInteger(InetAddress inetAddress) {
        Assert.notNull(inetAddress, "'inetAddress' must not be null.")

        new BigInteger(1, inetAddress.getAddress())
    }

    IPGeolocation get(InetAddress inetAddress) {
        Assert.notNull(inetAddress, "'inetAddress' must not be null.")

        Map.Entry<?, IPGeolocation> candidate
        IPGeolocation ipGeolocation = null

        // find and check candidate
        if (inetAddress instanceof Inet4Address) {
            candidate = ipv4Entries.floorEntry(inetAddressToBigInteger(inetAddress))
        } else {
            candidate = ipv6Entries.floorEntry(inetAddressToBigInteger(inetAddress))
        }

        if (candidate) {
            BigInteger start = inetAddressToBigInteger(candidate.value.startIP)
            BigInteger end = inetAddressToBigInteger(candidate.value.endIP)
            BigInteger address = inetAddressToBigInteger(inetAddress)

            // find the address is in range of the candidate
            ipGeolocation = start <= address && address <= end ? candidate.value : null
        }

        ipGeolocation
    }

    Integer size() {
        ipv4Entries.size() + ipv6Entries.size()
    }
}
