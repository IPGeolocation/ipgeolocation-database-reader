package io.ipgeolocation.databaseReader.databases.common

import groovy.transform.CompileStatic

import static com.google.common.base.Preconditions.checkNotNull
import static com.google.common.base.Strings.isNullOrEmpty

@CompileStatic
class Database {
    public static final String DB_I = "DB-I"
    public static final String DB_II = "DB-II"
    public static final String DB_III = "DB-III"
    public static final String DB_IV = "DB-IV"
    public static final String DB_V = "DB-V"
    public static final String DB_VI = "DB-VI"
    public static final String DB_VII = "DB-VII"

    public static final List<String> DATABASES = [DB_I, DB_II, DB_III, DB_IV, DB_V, DB_VI, DB_VII]
    public static final List<String> IP_TO_COUNTRY_DATABASES = [DB_I, DB_V]
    public static final List<String> IP_TO_CITY_DATABASES = [DB_II, DB_VI]
    public static final List<String> IP_TO_CITY_AND_ISP_DATABASES = [DB_IV, DB_VII]
    public static final List<String> DATABASES_WITH_ISP = [DB_III, DB_IV, DB_VII]
    public static final List<String> DATABASES_WITH_PROXY = [DB_V, DB_VI, DB_VII]

    static void checkGeolocationMapCommonParameters(String lang, List<String> euCountriesISO2CodeList, String selectedDatabase) {
        if (isNullOrEmpty(lang)) {
            throw new NullPointerException("Pre-condition violated: lang must not be null/empty.")
        }

        checkNotNull(euCountriesISO2CodeList, "Pre-condition violated: EU countries' ISO2 code list must not be null.")

        if (isNullOrEmpty(selectedDatabase) || !DATABASES.contains(selectedDatabase)) {
            throw new IllegalArgumentException("Pre-condition violated: selected database to read '${selectedDatabase}' is not valid.")
        }
    }

    static final String getDatabaseName(String database) {
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
