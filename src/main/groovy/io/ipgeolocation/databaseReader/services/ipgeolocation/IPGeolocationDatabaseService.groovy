package io.ipgeolocation.databaseReader.services.ipgeolocation

import com.google.common.net.InetAddresses
import groovy.transform.CompileStatic
import io.ipgeolocation.databaseReader.databases.common.DatabaseVersion
import io.ipgeolocation.databaseReader.databases.ipgeolocation.IPGeolocation
import io.ipgeolocation.databaseReader.databases.ipsecurity.IPSecurity
import io.ipgeolocation.databaseReader.services.database.CsvDatabaseService
import io.ipgeolocation.databaseReader.services.database.DatabaseService
import io.ipgeolocation.databaseReader.services.database.DatabaseUpdateService
import io.ipgeolocation.databaseReader.services.database.MMDBDatabaseService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.util.Assert

import javax.annotation.PostConstruct

import static com.google.common.base.Strings.isNullOrEmpty
import static java.util.Objects.isNull

@CompileStatic
@Service
class IPGeolocationDatabaseService {
    @Value('${application.country.EUCountryCodeISO2List}')
    List<String> euCountryCodeISO2List

    private final DatabaseUpdateService databaseUpdateService
    private final CsvDatabaseService csvDatabaseService
    private final MMDBDatabaseService mmdbDatabaseService

    private DatabaseService databaseService = null
    private String selectedDatabaseType

    @Autowired
    IPGeolocationDatabaseService(DatabaseUpdateService databaseUpdateService, CsvDatabaseService csvDatabaseService,
                                 MMDBDatabaseService mmdbDatabaseService) {
        this.databaseUpdateService = databaseUpdateService
        this.csvDatabaseService = csvDatabaseService
        this.mmdbDatabaseService = mmdbDatabaseService
    }

    @PostConstruct
    void initDatabase() {
        selectedDatabaseType = databaseUpdateService.getDatabaseType()

        if (selectedDatabaseType == "csv") {
            databaseService = csvDatabaseService
        } else if (selectedDatabaseType == "mmdb") {
            databaseService = mmdbDatabaseService
        }

        databaseService.loadDatabases()
    }

    final List<Map<String, Object>> lookupIPGeolocationBulk(List<String> ips, String fields, String excludes,
                                                            String include, String lang) {
        Assert.notEmpty(ips, "'ips' must not be empty or null.")
        Assert.hasText(fields, "'fields' must not be empty or null.")
        Assert.notNull(lang, "'lang' must not be null.")

        List<Map<String, Object>> responseMapArray = []

        for (String ip: ips) {
            responseMapArray.add(lookupIPGeolocation(ip, fields, excludes, include, lang, Boolean.TRUE))
        }

        responseMapArray
    }

    final Map<String, Object> lookupIPGeolocation(String ip, String fields, String excludes, String include,
                                                  String lang, Boolean removeStatusFromResponse) {
        Assert.hasText(ip, "'ip' must not be empty or null.")
        Assert.hasText(fields, "'fields' must not be empty or null.")
        Assert.notNull(lang, "'lang' must not be null.")
        Assert.notNull(removeStatusFromResponse, "'removeStatusFromResponse' must not be null.")

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
            } else if (ipGeolocation.country.countryCodeISO2 == "ZZ") {
                responseMap.put("status", HttpStatus.LOCKED)
                responseMap.put("message", "${inetAddress.getHostAddress()}: Bogon IP address. Bogon IP addresses are reserved like private, multicast, etc.".toString())
            } else {
                responseMap.put("status", HttpStatus.OK)
                responseMap.put("ip", inetAddress.getHostAddress())

                if (fields == "*" || fields == "all" || fields == "any") {
                    responseMap.putAll(ipGeolocation.getCompleteGeolocationMap(lang, euCountryCodeISO2List, databaseUpdateService.getDatabaseVersion()))
                } else {
                    responseMap.putAll(ipGeolocation.getCustomGeolocationMap(fields, lang, euCountryCodeISO2List, databaseUpdateService.getDatabaseVersion()))
                }

                String[] includeParts = include.replaceAll(" ","").split(",")

                if ("security" in includeParts && databaseUpdateService.getDatabaseVersion() in DatabaseVersion.DATABASES_WITH_PROXY) {
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
        Assert.hasText(ipAddress, "'ipAddress' must not be empty or null.")
        Assert.hasText(organization, "'organization' must not be empty or null.")

        Map<String, Object> responseMap = [:]
        IPSecurity ipSecurity = databaseService.findIPSecurity(ipAddress)
        Boolean isCloudProvider = databaseService.isCloudProvider(organization)
        Integer threatScore = 0

        if (!isNull(ipSecurity)) {
            threatScore = ipSecurity.threatScore
        }

        if (isCloudProvider) {
            threatScore += 5
        }

        responseMap.put("threat_score", threatScore)
        responseMap.put("is_tor", ipSecurity?.isTor ?: Boolean.FALSE)
        responseMap.put("is_proxy", ipSecurity?.isProxy ?: Boolean.FALSE)
        responseMap.put("proxy_type", ipSecurity?.proxyType ?: "")
        responseMap.put("is_anonymous", ipSecurity?.isAnonymous ?: Boolean.FALSE)
        responseMap.put("is_known_attacker", ipSecurity?.isKnownAttacker ?: Boolean.FALSE)
        responseMap.put("is_cloud_provider", isCloudProvider)
        responseMap.put("is_bot", ipSecurity?.isBot ?: Boolean.FALSE)
        responseMap.put("is_spam", ipSecurity?.isSpam ?: Boolean.FALSE)

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
