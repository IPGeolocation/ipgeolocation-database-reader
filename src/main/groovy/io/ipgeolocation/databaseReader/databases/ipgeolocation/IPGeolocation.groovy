package io.ipgeolocation.databaseReader.databases.ipgeolocation

import com.google.common.primitives.Ints
import groovy.transform.CompileStatic
import io.ipgeolocation.databaseReader.databases.common.IPGeolocationDatabase
import io.ipgeolocation.databaseReader.databases.country.Country
import io.ipgeolocation.databaseReader.databases.place.Place
import org.springframework.util.Assert

import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import static com.google.common.base.Strings.isNullOrEmpty
import static com.google.common.base.Strings.nullToEmpty

@CompileStatic
class IPGeolocation {
    InetAddress startIP
    InetAddress endIP
    Country country
    Place state
    Place district
    Place city
    String zipCode
    String latitude
    String longitude
    String geoNameId
    String timeZoneName
    String isp
    String connectionType
    String organization
    String asNumber

    IPGeolocation() {
    }

    IPGeolocation(InetAddress startIP, InetAddress endIP, Country country, Place state, Place district, Place city,
                  String zipCode, String latitude, String longitude, String geoNameId, String timeZoneName, String isp,
                  String connectionType, String organization, String asNumber) {
        Assert.notNull(startIP, "'startIP' must not be null.")
        Assert.notNull(endIP, "'endIP' must not be null.")
        Assert.notNull(country, "'country' must not be null.")

        Assert.isTrue((startIP instanceof Inet4Address && endIP instanceof Inet4Address) || (startIP instanceof
                Inet6Address && endIP instanceof Inet6Address), "'startIP' and 'endIP' must have the same IP version.")

        this.startIP = startIP
        this.endIP = endIP
        this.country = country
        this.state = state
        this.district = district
        this.city = city
        this.zipCode = nullToEmpty(zipCode)

        if (!isNullOrEmpty(latitude)) {
            this.latitude = String.format("%.5f", Double.parseDouble(latitude))
        }

        if (!isNullOrEmpty(longitude)) {
            this.longitude = String.format("%.5f", Double.parseDouble(longitude))
        }

        this.geoNameId = nullToEmpty(geoNameId)
        this.timeZoneName = nullToEmpty(timeZoneName)
        this.isp = nullToEmpty(isp)
        this.connectionType = nullToEmpty(connectionType)
        this.organization = nullToEmpty(organization)
        this.asNumber = nullToEmpty(asNumber) ? "AS${nullToEmpty(asNumber)}" : ""
    }

    Boolean isInRange(InetAddress inetAddress) {
        Assert.notNull(inetAddress, "'inetAddress' must not be null.")

        Boolean inRange = false

        if (isIPv6() && inetAddress instanceof Inet6Address) {
            // Assumes that all IPv6 ranges are at least /64 ranges
            Long start = ByteBuffer.wrap(this.startIP.getAddress(), 0, 8).getLong()
            Long end = ByteBuffer.wrap(this.endIP.getAddress(), 0, 8).getLong()
            Long address = ByteBuffer.wrap(inetAddress.getAddress(), 0, 8).getLong()
            inRange = address >= start && address <= end
        } else if (inetAddress instanceof Inet4Address) {
            Integer start = Ints.fromByteArray(this.startIP.getAddress())
            Integer end = Ints.fromByteArray(this.endIP.getAddress())
            Integer address = Ints.fromByteArray(inetAddress.getAddress())
            inRange = address >= start && address <= end
        }

        inRange
    }

    Boolean isIPv6() {
        startIP instanceof Inet6Address
    }

    Map<String, Object> getCompleteGeolocationMap(String lang, List<String> euCountriesISO2CodeList, String databaseVersion) {
        Assert.hasText(lang, "'lang' must not be empty or null.")
        Assert.isTrue(databaseVersion && databaseVersion in IPGeolocationDatabase.values(), "'databaseVersion' " +
                "($databaseVersion) must be equal to 'DB-I', 'DB-II', 'DB-III', 'DB-IV', 'DB-V', 'DB-VI', or 'DB-VII'.")
        Assert.notEmpty(euCountriesISO2CodeList, "'euCountriesISO2CodeList' must not be empty or null.")

        Map<String, Object> responseMap = [:]

        responseMap.put("continent_code", country.continentCode)
        responseMap.put("continent_name", country.continentName.getName(lang))
        responseMap.put("country_code2", country.countryCodeISO2)
        responseMap.put("country_code3", country.countryCodeISO3)
        responseMap.put("country_name", country.countryName.getName(lang))
        responseMap.put("country_capital", country.countryCapital?.getName(lang))

        if (databaseVersion in IPGeolocationDatabase.IP_TO_CITY_DATABASES ||
                databaseVersion in IPGeolocationDatabase.IP_TO_CITY_AND_ISP_DATABASES) {
            responseMap.put("state_prov", nullToEmpty(state?.getName(lang)))
            responseMap.put("district", nullToEmpty(district?.getName(lang)))
            responseMap.put("city", nullToEmpty(city?.getName(lang)))
            responseMap.put("zipcode", zipCode)
            responseMap.put("latitude", latitude)
            responseMap.put("longitude", longitude)
            responseMap.put("geoname_id", geoNameId)
        }

        responseMap.put("is_eu", country.countryCodeISO2 in euCountriesISO2CodeList)
        responseMap.put("calling_code", country.callingCode)
        responseMap.put("country_tld", country.tld)
        responseMap.put("languages", country.languages)
        responseMap.put("country_flag", country.flagUrl)

        if (databaseVersion in IPGeolocationDatabase.DATABASES_WITH_ISP) {
            responseMap.put("isp", isp)
            responseMap.put("connection_type", connectionType)
            responseMap.put("organization", organization)
            responseMap.put("asn", asNumber)
        }

        responseMap.put("currency", country.getCurrencyMap())

        if (databaseVersion in IPGeolocationDatabase.IP_TO_CITY_DATABASES ||
                databaseVersion in IPGeolocationDatabase.IP_TO_CITY_AND_ISP_DATABASES) {
            responseMap.put("time_zone", getTimeZoneMap())
        }

        responseMap
    }

    final Map<String, Object> getTimeZoneMap() {
        Map<String, Object> responseMap = [:]
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSZ")
        ZoneId zoneId = ZoneId.of(timeZoneName)
        LocalDateTime localDateTimeNow = LocalDateTime.now(zoneId)
        ZoneOffset zoneOffset = zoneId.getRules().getOffset(localDateTimeNow)
        ZonedDateTime zonedDateTimeNow = ZonedDateTime.now(zoneId)

        responseMap.put("name", zoneId.getId())
        responseMap.put("offset", (BigDecimal) zoneOffset.getTotalSeconds() / 3600)
        responseMap.put("current_time", zonedDateTimeNow.format(dateTimeFormatter))
        responseMap.put("current_time_unix", zonedDateTimeNow.toEpochSecond())
        responseMap.put("is_dst", zoneId.getRules().isDaylightSavings(zonedDateTimeNow.toInstant()))
        responseMap.put("dst_savings", zoneId.getRules().getDaylightSavings(zonedDateTimeNow.toInstant()).toMillis() / 3600)

        responseMap
    }

    Map<String, Object> getCustomGeolocationMap(String fields, String lang, List<String> euCountriesISO2CodeList, String selectedDatabase) {
        Assert.hasText(fields, "'fields' must not be empty or null.")
        Assert.hasText(lang, "'lang' must not be empty or null.")
        Assert.notEmpty(euCountriesISO2CodeList, "'euCountriesISO2CodeList' must not be empty or null.")
        Assert.isTrue(selectedDatabase && selectedDatabase in IPGeolocationDatabase.ALL_DATABASES, "'selectedDatabase' " +
                "($selectedDatabase) must be equal to 'DB-I', 'DB-II', 'DB-III', 'DB-IV', 'DB-V', 'DB-VI', or 'DB-VII'.")

        Map<String, Object> responseMap = [:]
        String[] fieldList = fields.replaceAll(" ", "").split(",")

        for (String field: fieldList) {
            switch (field.toLowerCase().trim()) {
                case "geo":
                    responseMap.putAll(getShortGeolocationMap(lang, selectedDatabase))
                    break
                case "continent_code":
                    responseMap.put("continent_code", country.continentCode)
                    break
                case "continent_name":
                    responseMap.put("continent_name", country.continentName.getName(lang))
                    break
                case "country_capital":
                    responseMap.put("country_capital", country.countryCapital?.getName(lang))
                    break
                case "country_code2":
                    responseMap.put("country_code2", country.countryCodeISO2)
                    break
                case "country_code3":
                    responseMap.put("country_code3", country.countryCodeISO3)
                    break
                case "country_name":
                    responseMap.put("country_name", country.countryName.getName(lang))
                    break
                case "is_eu":
                    responseMap.put("is_eu", country.countryCodeISO2 in euCountriesISO2CodeList)
                    break
                case "currency":
                    responseMap.put("currency", country.getCurrencyMap())
                    break
                case "calling_code":
                    responseMap.put("calling_code", country.callingCode)
                    break
                case "country_tld":
                    responseMap.put("country_tld", country.tld)
                    break
                case "languages":
                    responseMap.put("languages", country.languages)
                    break
                case "country_flag":
                    responseMap.put("country_flag", country.flagUrl)
                    break
                case "state_prov":
                    if (selectedDatabase in IPGeolocationDatabase.IP_TO_CITY_DATABASES || selectedDatabase in IPGeolocationDatabase.IP_TO_CITY_AND_ISP_DATABASES) {
                        responseMap.put("state_prov", nullToEmpty(state?.getName(lang)))
                    }
                    break
                case "district":
                    if (selectedDatabase in IPGeolocationDatabase.IP_TO_CITY_DATABASES || selectedDatabase in IPGeolocationDatabase.IP_TO_CITY_AND_ISP_DATABASES) {
                        responseMap.put("district", nullToEmpty(district?.getName(lang)))
                    }
                    break
                case "city":
                    if (selectedDatabase in IPGeolocationDatabase.IP_TO_CITY_DATABASES || selectedDatabase in IPGeolocationDatabase.IP_TO_CITY_AND_ISP_DATABASES) {
                        responseMap.put("city", nullToEmpty(city?.getName(lang)))
                    }
                    break
                case "geoname_id":
                    if (selectedDatabase in IPGeolocationDatabase.IP_TO_CITY_DATABASES || selectedDatabase in IPGeolocationDatabase.IP_TO_CITY_AND_ISP_DATABASES) {
                        responseMap.put("geoname_id", geoNameId)
                    }
                    break
                case "zipcode":
                    if (selectedDatabase in IPGeolocationDatabase.IP_TO_CITY_DATABASES || selectedDatabase in IPGeolocationDatabase.IP_TO_CITY_AND_ISP_DATABASES) {
                        responseMap.put("zipcode", zipCode)
                    }
                    break
                case "latitude":
                    if (selectedDatabase in IPGeolocationDatabase.IP_TO_CITY_DATABASES || selectedDatabase in IPGeolocationDatabase.IP_TO_CITY_AND_ISP_DATABASES) {
                        responseMap.put("latitude", latitude)
                    }
                    break
                case "longitude":
                    if (selectedDatabase in IPGeolocationDatabase.IP_TO_CITY_DATABASES || selectedDatabase in IPGeolocationDatabase.IP_TO_CITY_AND_ISP_DATABASES) {
                        responseMap.put("longitude", longitude)
                    }
                    break
                case "time_zone":
                    if (selectedDatabase in IPGeolocationDatabase.IP_TO_CITY_DATABASES || selectedDatabase in IPGeolocationDatabase.IP_TO_CITY_AND_ISP_DATABASES) {
                        responseMap.put("time_zone", getTimeZoneMap())
                    }
                    break
                case "isp":
                    if (selectedDatabase in IPGeolocationDatabase.DATABASES_WITH_ISP) {
                        responseMap.put("isp", isp)
                    }
                    break
                case "connection_type":
                    if (selectedDatabase in IPGeolocationDatabase.DATABASES_WITH_ISP) {
                        responseMap.put("connection_type", connectionType)
                    }
                    break
                case "organization":
                    if (selectedDatabase in IPGeolocationDatabase.DATABASES_WITH_ISP) {
                        responseMap.put("organization", organization)
                    }
                    break
                case "asn":
                    if (selectedDatabase in IPGeolocationDatabase.DATABASES_WITH_ISP) {
                        responseMap.put("asn", asNumber)
                    }
                    break
                default:
                    responseMap.put(field, "Field '${field}' is not supported.".toString())
            }
        }

        responseMap
    }

    Map<String, Object> getShortGeolocationMap(String lang, String selectedDatabase) {
        Assert.hasText(lang, "'lang' must not be null.")

        Map<String, Object> responseMap = [:]

        responseMap.put("country_code2", country.countryCodeISO2)
        responseMap.put("country_code3", country.countryCodeISO3)
        responseMap.put("country_name", country.countryName.getName(lang))

        if (selectedDatabase in IPGeolocationDatabase.IP_TO_CITY_DATABASES) {
            responseMap.put("state_prov", nullToEmpty(state?.getName(lang)))
            responseMap.put("district",  nullToEmpty(district?.getName(lang)))
            responseMap.put("city", nullToEmpty(city?.getName(lang)))
            responseMap.put("zipcode", zipCode)
            responseMap.put("latitude", latitude)
            responseMap.put("longitude", longitude)
        }

        responseMap
    }
}
