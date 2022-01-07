package io.ipgeolocation.databaseReader.databases.country

import groovy.transform.CompileStatic
import io.ipgeolocation.databaseReader.databases.place.Place
import org.springframework.util.Assert

import static com.google.common.base.Strings.nullToEmpty

@CompileStatic
class Country {
    Integer id
    String continentCode
    Place continentPlace
    String countryCode2
    String countryCode3
    Place countryPlace
    Place capitalPlace
    String currencyCode
    String currencyName
    String currencySymbol
    String callingCode
    String tld
    String languages
    String flagUrl

    Country(Integer id, String continentCode, Place continentPlace, String countryCode2, String countryCode3,
            Place countryPlace, Place capitalPlace, String currencyCode, String currencyName, String currencySymbol,
            String callingCode, String tld, String languages) {
        Assert.notNull(id, "'id' must not be null.")
        Assert.hasText(continentCode, "'continentCode' must not be empty or null.")
        Assert.notNull(continentPlace, "'continentPlace' must not be null.")
        Assert.hasText(countryCode2, "'countryCode2' must not be empty or null.")
        Assert.hasText(countryCode2, "'countryCode3' must not be empty or null.")
        Assert.notNull(countryPlace, "'countryPlace' must not be null.")

        this.id = id
        this.continentCode = continentCode
        this.continentPlace = continentPlace
        this.countryCode2 = countryCode2
        this.countryCode3 = countryCode3
        this.countryPlace = countryPlace
        this.capitalPlace = capitalPlace
        this.currencyCode = nullToEmpty(currencyCode)
        this.currencyName = nullToEmpty(currencyName)
        this.currencySymbol = nullToEmpty(currencySymbol)
        this.callingCode = nullToEmpty(callingCode)
        this.tld = nullToEmpty(tld)
        this.languages = nullToEmpty(languages)
        this.flagUrl = countryCode2 == "ZZ" ? "" : generateFlagUrl(countryCode2)
    }

    final Map<String, Object> getCurrencyMap() {
        Map<String, Object> responseMap = [:]

        responseMap.put("code", currencyCode)
        responseMap.put("name", currencyName)
        responseMap.put("symbol", currencySymbol)

        responseMap
    }

    static final String generateFlagUrl(String countryCodeISO2) {
        Assert.isTrue(countryCodeISO2 && countryCodeISO2.length() == 2, "'countryCodeISO2' must not be null or empty or have length >2.")

        "https://ipgeolocation.io/static/flags/${countryCodeISO2.toLowerCase()}_64.png"
    }
}
