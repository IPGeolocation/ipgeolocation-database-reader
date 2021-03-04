package io.ipgeolocation.databaseReader.services.ipgeolocation

import com.google.common.net.InetAddresses
import groovy.transform.CompileStatic
import io.ipgeolocation.databaseReader.databases.common.Database
import io.ipgeolocation.databaseReader.databases.ipgeolocation.IPGeolocation
import io.ipgeolocation.databaseReader.databases.ipsecurity.IPSecurity
import io.ipgeolocation.databaseReader.services.database.DatabaseService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

import static com.google.common.base.Preconditions.checkNotNull
import static com.google.common.base.Strings.isNullOrEmpty
import static java.util.Objects.isNull

@CompileStatic
@Service
class IPGeolocationDatabaseService {
    @Value('${application.country.euCountriesISO2CodeList}') List<String> euCountriesISO2CodeList

    private final DatabaseService databaseService

    IPGeolocationDatabaseService(@Autowired DatabaseService databaseService) {
        this.databaseService = databaseService
    }

    final List<Map<String, Object>> lookupIPGeolocationBulk(List<String> ips, String fields, String excludes, String include, String lang) {
        checkNotNull(ips, "Pre-condition violated: IP addresses must not be null.")
        checkNotNull(fields, "Pre-condition violated: fields must not be null.")
        checkNotNull(excludes, "Pre-condition violated: excludes must not be null.")
        checkNotNull(include, "Pre-condition violated: include must not be null.")
        checkNotNull(lang, "Pre-condition violated: lang must not be null.")

        List<Map<String, Object>> responseMapArray = []

        for (String ip: ips) {
            responseMapArray.add(lookupIPGeolocation(ip, fields, excludes, include, lang, Boolean.TRUE))
        }

        responseMapArray
    }

    final Map<String, Object> lookupIPGeolocation(String ip, String fields, String excludes, String include, String lang, Boolean removeStatusFromResponse) {
        if (isNullOrEmpty(ip)) {
            throw new NullPointerException("Pre-condition violated: IP address must not be null/empty.")
        }

        if (isNullOrEmpty(fields)) {
            throw new NullPointerException("Pre-condition violated: fields must not be null/empty.")
        }

        if (isNullOrEmpty(lang)) {
            throw new NullPointerException("Pre-condition violated: lang must not be null/empty.")
        }

        checkNotNull(excludes, "Pre-condition violated: excludes must not be null.")
        checkNotNull(include, "Pre-condition violated: include must not be null.")
        checkNotNull(removeStatusFromResponse, "Pre-condition violated: remove status from response must not be null.")

        Map<String, Object> responseMap = [:]
        InetAddress inetAddress = null

        if (InetAddresses.isInetAddress(ip)) {
            inetAddress = InetAddresses.forString(ip)
        } else {
            responseMap.put("status", HttpStatus.BAD_REQUEST)
            responseMap.put("message", "'${ip}' is not a valid IP address.".toString())
        }

        if (responseMap.isEmpty()) {
            IPGeolocation ipGeolocation = databaseService.findIPGeolocation(inetAddress)

            if (isNull(ipGeolocation)) {
                responseMap.put("status", HttpStatus.NOT_FOUND)
                responseMap.put("message", "Provided IP address '${inetAddress.getHostAddress()}' doesn't exist in IPGeolocation database.".toString())
            } else if (ipGeolocation.country.countryCode2 == "ZZ") {
                responseMap.put("status", HttpStatus.LOCKED)
                responseMap.put("message", "${inetAddress.getHostAddress()}: Bogon IP address. Bogon IP addresses are reserved like private, multicast, etc.".toString())
            } else {
                responseMap.put("status", HttpStatus.OK)
                responseMap.put("ip", inetAddress.getHostAddress())

                switch (fields) {
                    case null:
                    case "*":
                    case "all":
                    case "any":
                        responseMap.putAll(ipGeolocation.getCompleteGeolocationMap(lang, euCountriesISO2CodeList, databaseService.getSelectedDatabase()))
                        break
                    default:
                        responseMap.putAll(ipGeolocation.getCustomGeolocationMap(fields, lang, euCountriesISO2CodeList, databaseService.getSelectedDatabase()))
                }

                String[] includeParts = include.replaceAll(" ","").split(",")

                if (includeParts.contains("security") && Database.DATABASES_WITH_PROXY.contains(databaseService.getSelectedDatabase())) {
                    responseMap.put("security", getIPSecurityMap(InetAddresses.toAddrString(inetAddress), ipGeolocation.isp ?: ipGeolocation.organization))
                }

                if (!isNullOrEmpty(excludes)) {
                    responseMap = excludeFromGeolocationMap(excludes, responseMap)
                }
            }
        }

        if (removeStatusFromResponse) {
            responseMap.remove("status")
        }

        responseMap
    }

    final Map<String, Object> getIPSecurityMap(String ipAddress, String organization) {
        checkNotNull(ipAddress, "Pre-condition violated: IP address must not be null.")
        checkNotNull(organization, "Pre-condition violated: organization must not be null.")

        Map<String, Object> responseMap = [:]
        IPSecurity ipSecurity = databaseService.findIPSecurity(ipAddress)
        Boolean isCloudProvider = databaseService.isCloudProvider(organization)
        Integer threatScore = 0

        if (!isNull(ipSecurity)) {
            threatScore = ipSecurity.threatScore
        }

        if (isCloudProvider) {
            threatScore += 15
        }

        responseMap.put("threat_score", threatScore)
        responseMap.put("is_tor", ipSecurity?.isTor ?: Boolean.FALSE)
        responseMap.put("is_proxy", ipSecurity?.isProxy ?: Boolean.FALSE)
        responseMap.put("proxy_type", ipSecurity?.proxyType ?: "")
        responseMap.put("is_anonymous", ipSecurity?.isAnonymous ?: Boolean.FALSE)
        responseMap.put("is_known_attacker", ipSecurity?.isKnownAttacker ?: Boolean.FALSE)
        responseMap.put("is_cloud_provider", isCloudProvider)

        responseMap
    }

    private static final Map<String, Object> excludeFromGeolocationMap(String excludes, Map<String, Object> geolocationMap) {
        String[] excludeList = excludes.split(",")

        for (String exclude: excludeList) {
            if (exclude.toLowerCase().trim() != "ip") {
                geolocationMap.remove(exclude.toLowerCase().trim())
            }
        }

        geolocationMap
    }
}
