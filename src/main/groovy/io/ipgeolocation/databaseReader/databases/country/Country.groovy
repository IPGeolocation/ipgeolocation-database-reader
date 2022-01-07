package io.ipgeolocation.databaseReader.databases.country

import groovy.transform.CompileStatic
import io.ipgeolocation.databaseReader.databases.place.Place
import org.springframework.util.Assert

import static com.google.common.base.Strings.nullToEmpty

@CompileStatic
class Country {
    Integer id
    String continentCode
    Place continentName
    String countryCodeISO2
    String countryCodeISO3
    Place countryName
    Place countryCapital
    String currencyCode
    String currencyName
    String currencySymbol
    String callingCode
    String tld
    String languages
    String flagUrl

    Country(Integer id, String continentCode, Place continentName, String countryCodeISO2, String countryCodeISO3,
            Place countryName, Place countryCapital, String currencyCode, String currencyName, String currencySymbol,
            String callingCode, String tld, String languages) {
        Assert.notNull(id, "'id' must not be null.")
        Assert.hasText(continentCode, "'continentCode' must not be empty or null.")
        Assert.notNull(continentName, "'continentName' must not be null.")
        Assert.hasText(countryCodeISO2, "'countryCode2' must not be empty or null.")
        Assert.hasText(countryCodeISO3, "'countryCodeISO3' must not be empty or null.")
        Assert.notNull(countryName, "'countryName' must not be null.")

        this.id = id
        this.continentCode = continentCode
        this.continentName = continentName
        this.countryCodeISO2 = countryCodeISO2
        this.countryCodeISO3 = countryCodeISO3
        this.countryName = countryName
        this.countryCapital = countryCapital
        this.currencyCode = nullToEmpty(currencyCode)
        this.currencyName = nullToEmpty(currencyName)
        this.currencySymbol = nullToEmpty(currencySymbol)
        this.callingCode = nullToEmpty(callingCode)
        this.tld = nullToEmpty(tld)
        this.languages = nullToEmpty(languages)
        this.flagUrl = countryCodeISO2 == "ZZ" ? "" : "https://ipgeolocation.io/static/flags/${countryCodeISO2.toLowerCase()}_64.png"
    }

    final Map<String, Object> getCurrencyMap() {
        Map<String, Object> responseMap = [:]

        responseMap.put("code", currencyCode)
        responseMap.put("name", currencyName)
        responseMap.put("symbol", currencySymbol)

        responseMap
    }
}
