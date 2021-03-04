package io.ipgeolocation.databaseReader.databases.ipgeolocation

import com.google.common.primitives.Ints
import groovy.transform.CompileStatic

import java.nio.ByteBuffer

import static com.google.common.base.Preconditions.checkNotNull

@CompileStatic
class IPGeolocationIndexer {
    private final TreeMap<Integer, IPGeolocation> ipv4Entries = new TreeMap<Integer, IPGeolocation>()
    private final TreeMap<Long, IPGeolocation> ipv6Entries = new TreeMap<Long, IPGeolocation>()

    void add(IPGeolocation entry) {
        checkNotNull(entry, "Pre-condition violated: ip-geolocation must not be null.")

        if(entry.isIPv6()) {
            // Assume that all ranges are at least /64
            Long start = ipv6AddressToLong(entry.startIP)
            ipv6Entries.put(start, entry)
        } else {
            Integer start = ipv4AddressToInt(entry.startIP)
            ipv4Entries.put(start, entry)
        }
    }

    private static Long ipv6AddressToLong(InetAddress addr) {
        ByteBuffer.wrap(addr.getAddress(), 0, 8).getLong()
    }

    private static Integer ipv4AddressToInt(InetAddress addr) {
        Ints.fromByteArray(addr.getAddress())
    }

    IPGeolocation get(InetAddress inetAddress) {
        checkNotNull(inetAddress, "Pre-condition violated: inetAddress must not be null.")

        Map.Entry<?, IPGeolocation> candidate
        IPGeolocation ipGeolocation = null

        // find and check candiate
        if(inetAddress instanceof Inet4Address) {
            candidate = ipv4Entries.floorEntry(ipv4AddressToInt(inetAddress))
        } else {
            candidate = ipv6Entries.floorEntry(ipv6AddressToLong(inetAddress))
        }

        if(candidate && candidate.getValue().isInRange(inetAddress)) {
            ipGeolocation = candidate.getValue()
        }
        ipGeolocation
    }

    Integer size() {
        ipv4Entries.size() + ipv6Entries.size()
    }
}
