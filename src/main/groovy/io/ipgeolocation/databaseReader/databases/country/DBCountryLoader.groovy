package io.ipgeolocation.databaseReader.databases.country

import groovy.transform.CompileStatic
import io.ipgeolocation.databaseReader.databases.common.Pool
import io.ipgeolocation.databaseReader.databases.common.PoolInteger
import io.ipgeolocation.databaseReader.databases.common.PoolString
import io.ipgeolocation.databaseReader.databases.place.Place
import io.ipgeolocation.databaseReader.services.database.DatabaseService
import org.springframework.util.Assert
import org.supercsv.cellprocessor.Optional
import org.supercsv.cellprocessor.ift.CellProcessor
import org.supercsv.io.CsvMapReader
import org.supercsv.prefs.CsvPreference

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.zip.GZIPInputStream

import static java.util.Objects.isNull

@CompileStatic
class DBCountryLoader {
    private final String ID = "id"
    private final String CONTINENT_CODE = "continent_code"
    private final String CONTINENT_PLACE_ID = "continent_place_id"
    private final String COUNTRY_CODE2 = "country_code2"
    private final String COUNTRY_CODE3 = "country_code3"
    private final String COUNTRY_PLACE_ID = "name"
    private final String CAPITAL_PLACE_ID = "capital"
    private final String CURRENCY_CODE = "currency_code"
    private final String CURRENCY_NAME = "currency_name"
    private final String CURRENCY_SYMBOL = "currency_symbol"
    private final String CALLING_CODE = "calling_code"
    private final String TLD = "tld"
    private final String LANGUAGES = "languages"
    private final String[] CSV_COLUMNS = [
            ID,
            CONTINENT_CODE,
            CONTINENT_PLACE_ID,
            COUNTRY_CODE2,
            COUNTRY_CODE3,
            COUNTRY_PLACE_ID,
            CAPITAL_PLACE_ID,
            CURRENCY_CODE,
            CURRENCY_NAME,
            CURRENCY_SYMBOL,
            CALLING_CODE,
            TLD,
            LANGUAGES
    ]
    private final CellProcessor[] cellProcessors
    private final Pool pool = Pool.getInstance()
    private final DatabaseService databaseService

    DBCountryLoader(DatabaseService databaseService) {
        Assert.notNull(databaseService, "'databaseService' must not be null.")

        CellProcessor integer = new PoolInteger(pool)
        CellProcessor optionalInteger = new Optional(new PoolInteger(pool))
        CellProcessor string = new PoolString(pool)
        CellProcessor optionalString = new Optional(new PoolString(pool))

        cellProcessors = [
                integer, // id
                string, // continent_code
                integer, // continent_place_id
                string, // country_code2
                string, // country_code3
                integer, // country_place_id
                optionalInteger, // capital_place_id
                optionalString, // currency_code
                optionalString, // currency_name
                optionalString, // currency_symbol
                optionalString, // calling_code
                optionalString, // tld
                optionalString // languages
        ]
        this.databaseService = databaseService

        Assert.state(cellProcessors.length == CSV_COLUMNS.length, "Programmer error: length of columns does not match length of cell processors.")
    }

    void load(String countryCsvFilePath, CountryIndexer countryIndexer) {
        Assert.hasText(countryCsvFilePath, "'countryCsvFilePath' must not be empty or null.")
        Assert.notNull(countryIndexer, "'countryIndexer' must not be null.")

        Path countryCsvPath = Paths.get(countryCsvFilePath)

        Assert.state(Files.isRegularFile(countryCsvPath) && Files.exists(countryCsvPath), "$countryCsvFilePath file is missing.")

        try {
            InputStream fis = Files.newInputStream(countryCsvPath, StandardOpenOption.READ)
            InputStream gis = new GZIPInputStream(fis)
            Reader inputStreamReader = new InputStreamReader(gis, StandardCharsets.UTF_8)
            CsvMapReader reader = new CsvMapReader(inputStreamReader, CsvPreference.STANDARD_PREFERENCE)
            Map<String, Object> record

            while (!isNull(record = reader.read(CSV_COLUMNS, cellProcessors))) {
                Integer continentPlaceId = record.get(CONTINENT_PLACE_ID) as Integer
                Integer countryPlaceId = record.get(COUNTRY_PLACE_ID) as Integer
                Integer capitalPlaceId = record.get(CAPITAL_PLACE_ID) as Integer
                Place continentPlace = null
                Place countryPlace = null
                Place capitalPlace = null

                if (continentPlaceId) {
                    continentPlace = databaseService.findPlace(continentPlaceId)
                }

                if (countryPlaceId) {
                    countryPlace = databaseService.findPlace(countryPlaceId)
                }

                if (capitalPlaceId) {
                    capitalPlace = databaseService.findPlace(capitalPlaceId)
                }

                countryIndexer.addAt(new Country(
                        record.get(ID) as Integer, record.get(CONTINENT_CODE) as String, continentPlace,
                        record.get(COUNTRY_CODE2) as String, record.get(COUNTRY_CODE3) as String, countryPlace,
                        capitalPlace, record.get(CURRENCY_CODE) as String, record.get(CURRENCY_NAME) as String,
                        record.get(CURRENCY_SYMBOL) as String, record.get(CALLING_CODE) as String,
                        record.get(TLD) as String, record.get(LANGUAGES) as String
                ), record.get(ID) as Integer)
            }

            inputStreamReader.close()
            gis.close()
            fis.close()
        } catch (IOException | NullPointerException e) {
            e.printStackTrace()
        }
    }
}
