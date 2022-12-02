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
            // Assume that all ranges are at least /64
            BigInteger start = inetAddressToBigInteger(entry.startIP)
            ipv6Entries.put(start, entry)
        } else {
            BigInteger start = inetAddressToBigInteger(entry.startIP)
            ipv4Entries.put(start, entry)
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

        // find and check candiate
        if (inetAddress instanceof Inet4Address) {
            candidate = ipv4Entries.floorEntry(inetAddressToBigInteger(inetAddress))
        } else {
            candidate = ipv6Entries.floorEntry(inetAddressToBigInteger(inetAddress))
        }

        if (candidate && candidate.getValue().isInRange(inetAddress)) {
            ipGeolocation = candidate.getValue()
        }

        ipGeolocation
    }

    Integer size() {
        ipv4Entries.size() + ipv6Entries.size()
    }
}
