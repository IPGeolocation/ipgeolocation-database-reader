package io.ipgeolocation.databaseReader.databases.common

import groovy.transform.CompileStatic
import org.springframework.util.Assert

@CompileStatic
class IPGeolocationDatabase {
    public static final String DB_I = "DB-I"
    public static final String DB_II = "DB-II"
    public static final String DB_III = "DB-III"
    public static final String DB_IV = "DB-IV"
    public static final String DB_V = "DB-V"
    public static final String DB_VI = "DB-VI"
    public static final String DB_VII = "DB-VII"
    public static final List<String> ALL_DATABASES = [DB_I, DB_II, DB_III, DB_IV, DB_V, DB_VI, DB_VII]
    public static final List<String> IP_TO_COUNTRY_DATABASES = [DB_I, DB_V]
    public static final List<String> IP_TO_CITY_DATABASES = [DB_II, DB_VI]
    public static final List<String> IP_TO_CITY_AND_ISP_DATABASES = [DB_IV, DB_VII]
    public static final List<String> DATABASES_WITH_ISP = [DB_III, DB_IV, DB_VII]
    public static final List<String> DATABASES_WITH_PROXY = [DB_V, DB_VI, DB_VII]

    static final String getDatabaseName(String database) {
        Assert.hasText(database, "'database' must not be empty or null.")

        String databaseName

        switch (database) {
            case DB_I:
                databaseName = "ipToCountryDatabase"
                break
            case DB_II:
                databaseName = "ipToCityDatabase"
                break
            case DB_III:
                databaseName = "ipToISPDatabase"
                break
            case DB_IV:
                databaseName = "ipToCityAndISPDatabase"
                break
            case DB_V:
                databaseName = "ipToProxyDatabase"
                break
            case DB_VI:
                databaseName = "ipToCityAndProxyDatabase"
                break
            case DB_VII:
                databaseName = "ipToCityAndISPAndProxyDatabase"
                break
            default:
                databaseName = ""
        }

        databaseName
    }

    static final String getDatabaseUri(String database) {
        Assert.hasText(database, "'database' must not be empty or null.")
        
        String databaseUri

        switch (database) {
            case DB_I:
                databaseUri = "https://database.ipgeolocation.io/download/ipToCountryDatabase"
                break
            case DB_II:
                databaseUri = "https://database.ipgeolocation.io/download/ipToCityDatabase"
                break
            case DB_III:
                databaseUri = "https://database.ipgeolocation.io/download/ipToISPDatabase"
                break
            case DB_IV:
                databaseUri = "https://database.ipgeolocation.io/download/ipToCityAndISPDatabase"
                break
            case DB_V:
                databaseUri = "https://database.ipgeolocation.io/download/ipToProxyDatabase"
                break
            case DB_VI:
                databaseUri = "https://database.ipgeolocation.io/download/ipToCityAndProxyDatabase"
                break
            case DB_VII:
                databaseUri = "https://database.ipgeolocation.io/download/ipToCityAndISPAndProxyDatabase"
                break
            default:
                databaseUri = ""
        }

        databaseUri
    }
}
