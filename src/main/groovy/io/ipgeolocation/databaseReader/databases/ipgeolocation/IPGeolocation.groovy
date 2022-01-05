package io.ipgeolocation.databaseReader.databases.ipgeolocation

import com.google.common.primitives.Ints
import groovy.transform.CompileStatic
import io.ipgeolocation.databaseReader.databases.common.IPGeolocationDatabase
import io.ipgeolocation.databaseReader.databases.country.Country
import io.ipgeolocation.databaseReader.databases.place.Place

import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import static com.google.common.base.Preconditions.checkNotNull
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
    String geonameId
    String timeZoneName
    String isp
    String connectionType
    String organization
    String asNumber

    IPGeolocation(InetAddress startIP, InetAddress endIP, Country country, Place state, Place district, Place city, String zipCode, String latitude, String longitude, String geonameId, String timeZoneName, String isp, String connectionType, String organization, String asNumber) {
        checkNotNull(startIP, "Pre-condition violated: start ip-address must not be null.")
        checkNotNull(endIP, "Pre-condition violated: end ip-address must not be null.")
        checkNotNull(country, "Pre-condition violated: country must not be null.")

        if ((startIP instanceof Inet4Address && endIP instanceof Inet6Address) ||
                (startIP instanceof Inet6Address && endIP instanceof Inet4Address)) {
            throw new IllegalArgumentException("start and end IP addresses must have the same IP version.")
        }

        this.startIP = startIP
        this.endIP = endIP
        this.country = country
        this.state = state
        this.district = district
        this.city = city
        this.zipCode = nullToEmpty(zipCode)
        this.latitude = nullToEmpty(latitude)
        this.longitude = nullToEmpty(longitude)
        this.geonameId = nullToEmpty(geonameId)
        this.timeZoneName = nullToEmpty(timeZoneName)
        this.isp = nullToEmpty(isp)
        this.connectionType = nullToEmpty(connectionType)
        this.organization = nullToEmpty(organization)
        this.asNumber = nullToEmpty(asNumber) ? "AS${nullToEmpty(asNumber)}" : ""

        if (!isNullOrEmpty(this.latitude)) {
            this.latitude = String.format("%.5f", Double.parseDouble(this.latitude))
        }

        if (!isNullOrEmpty(this.longitude)) {
            this.longitude = String.format("%.5f", Double.parseDouble(this.longitude))
        }
    }

    Boolean isInRange(InetAddress i) {
        checkNotNull(i, "Pre-condition violated: IP address must not be null.")

        Boolean inRange = false

        if (isIPv6() && i instanceof Inet6Address) {
            // Assumes that all IPv6 ranges are at least /64 ranges
            Long start = ByteBuffer.wrap(this.startIP.getAddress(), 0, 8).getLong()
            Long end = ByteBuffer.wrap(this.endIP.getAddress(), 0, 8).getLong()
            Long address = ByteBuffer.wrap(i.getAddress(), 0, 8).getLong()
            inRange = address >= start && address <= end
        } else if (i instanceof Inet4Address) {
            Integer start = Ints.fromByteArray(this.startIP.getAddress())
            Integer end = Ints.fromByteArray(this.endIP.getAddress())
            Integer address = Ints.fromByteArray(i.getAddress())
            inRange = address >= start && address <= end
        }

        inRange
    }

    Boolean isIPv6() {
        startIP instanceof Inet6Address
    }

    Map<String, Object> getCompleteGeolocationMap(String lang, List<String> euCountriesISO2CodeList, String selectedDatabase) {
        IPGeolocationDatabase.checkGeolocationMapCommonParameters(lang, euCountriesISO2CodeList, selectedDatabase)

        Map<String, Object> responseMap = [:]

        responseMap.put("continent_code", country.continentCode)
        responseMap.put("continent_name", country.continentPlace.getName(lang))
        responseMap.put("country_code2", country.countryCode2)
        responseMap.put("country_code3", country.countryCode3)
        responseMap.put("country_name", country.countryPlace.getName(lang))
        responseMap.put("country_capital", country.capitalPlace?.getName(lang))

        if (IPGeolocationDatabase.IP_TO_CITY_DATABASES.contains(selectedDatabase) || IPGeolocationDatabase.IP_TO_CITY_AND_ISP_DATABASES.contains(selectedDatabase)) {
            responseMap.put("state_prov", nullToEmpty(state?.getName(lang)))
            responseMap.put("district", nullToEmpty(district?.getName(lang)))
            responseMap.put("city", nullToEmpty(city?.getName(lang)))
            responseMap.put("zipcode", zipCode)
            responseMap.put("latitude", latitude)
            responseMap.put("longitude", longitude)
            responseMap.put("geoname_id", geonameId)
        }

        responseMap.put("is_eu", euCountriesISO2CodeList.contains(country.countryCode2))
        responseMap.put("calling_code", country.callingCode)
        responseMap.put("country_tld", country.tld)
        responseMap.put("languages", country.languages)
        responseMap.put("country_flag", country.flagUrl)

        if (IPGeolocationDatabase.DATABASES_WITH_ISP.contains(selectedDatabase)) {
            responseMap.put("isp", isp)
            responseMap.put("connection_type", connectionType)
            responseMap.put("organization", organization)
            responseMap.put("asn", asNumber)
        }

        responseMap.put("currency", country.getCurrencyMap())

        if (IPGeolocationDatabase.IP_TO_CITY_DATABASES.contains(selectedDatabase) || IPGeolocationDatabase.IP_TO_CITY_AND_ISP_DATABASES.contains(selectedDatabase)) {
            responseMap.put("time_zone", getTimeZoneMap())
        }

        responseMap
    }

    static Map<String, Object> getTimeZoneMap() {
        Map<String, Object> responseMap = [:]
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSZ")
        ZoneId zoneId = ZoneId.of("America/Toronto")
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
        if (isNullOrEmpty(fields)) {
            throw new NullPointerException("Pre-condition violated: fields must not be null/empty.")
        }

        IPGeolocationDatabase.checkGeolocationMapCommonParameters(lang, euCountriesISO2CodeList, selectedDatabase)

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
                    responseMap.put("continent_name", country.continentPlace.getName(lang))
                    break
                case "country_capital":
                    responseMap.put("country_capital", country.capitalPlace?.getName(lang))
                    break
                case "country_code2":
                    responseMap.put("country_code2", country.countryCode2)
                    break
                case "country_code3":
                    responseMap.put("country_code3", country.countryCode3)
                    break
                case "country_name":
                    responseMap.put("country_name", country.countryPlace.getName(lang))
                    break
                case "is_eu":
                    responseMap.put("is_eu", euCountriesISO2CodeList.contains(country.countryCode2))
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
                    if (IPGeolocationDatabase.IP_TO_CITY_DATABASES.contains(selectedDatabase) || IPGeolocationDatabase.IP_TO_CITY_AND_ISP_DATABASES.contains(selectedDatabase)) {
                        responseMap.put("state_prov", nullToEmpty(state?.getName(lang)))
                    }
                    break
                case "district":
                    if (IPGeolocationDatabase.IP_TO_CITY_DATABASES.contains(selectedDatabase) || IPGeolocationDatabase.IP_TO_CITY_AND_ISP_DATABASES.contains(selectedDatabase)) {
                        responseMap.put("district", nullToEmpty(district?.getName(lang)))
                    }
                    break
                case "city":
                    if (IPGeolocationDatabase.IP_TO_CITY_DATABASES.contains(selectedDatabase) || IPGeolocationDatabase.IP_TO_CITY_AND_ISP_DATABASES.contains(selectedDatabase)) {
                        responseMap.put("city", nullToEmpty(city?.getName(lang)))
                    }
                    break
                case "geoname_id":
                    if (IPGeolocationDatabase.IP_TO_CITY_DATABASES.contains(selectedDatabase) || IPGeolocationDatabase.IP_TO_CITY_AND_ISP_DATABASES.contains(selectedDatabase)) {
                        responseMap.put("geoname_id", geonameId)
                    }
                    break
                case "zipcode":
                    if (IPGeolocationDatabase.IP_TO_CITY_DATABASES.contains(selectedDatabase) || IPGeolocationDatabase.IP_TO_CITY_AND_ISP_DATABASES.contains(selectedDatabase)) {
                        responseMap.put("zipcode", zipCode)
                    }
                    break
                case "latitude":
                    if (IPGeolocationDatabase.IP_TO_CITY_DATABASES.contains(selectedDatabase) || IPGeolocationDatabase.IP_TO_CITY_AND_ISP_DATABASES.contains(selectedDatabase)) {
                        responseMap.put("latitude", latitude)
                    }
                    break
                case "longitude":
                    if (IPGeolocationDatabase.IP_TO_CITY_DATABASES.contains(selectedDatabase) || IPGeolocationDatabase.IP_TO_CITY_AND_ISP_DATABASES.contains(selectedDatabase)) {
                        responseMap.put("longitude", longitude)
                    }
                    break
                case "time_zone":
                    if (IPGeolocationDatabase.IP_TO_CITY_DATABASES.contains(selectedDatabase) || IPGeolocationDatabase.IP_TO_CITY_AND_ISP_DATABASES.contains(selectedDatabase)) {
                        responseMap.put("time_zone", getTimeZoneMap())
                    }
                    break
                case "isp":
                    if (IPGeolocationDatabase.DATABASES_WITH_ISP.contains(selectedDatabase)) {
                        responseMap.put("isp", isp)
                    }
                    break
                case "connection_type":
                    if (IPGeolocationDatabase.DATABASES_WITH_ISP.contains(selectedDatabase)) {
                        responseMap.put("connection_type", connectionType)
                    }
                    break
                case "organization":
                    if (IPGeolocationDatabase.DATABASES_WITH_ISP.contains(selectedDatabase)) {
                        responseMap.put("organization", organization)
                    }
                    break
                case "asn":
                    if (IPGeolocationDatabase.DATABASES_WITH_ISP.contains(selectedDatabase)) {
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
        checkNotNull(lang, "Pre-condition violated: lang must not be null.")

        Map<String, Object> responseMap = [:]

        responseMap.put("country_code2", country.countryCode2)
        responseMap.put("country_code3", country.countryCode3)
        responseMap.put("country_name", country.countryPlace.getName(lang))

        if (IPGeolocationDatabase.IP_TO_CITY_DATABASES.contains(selectedDatabase)) {
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
