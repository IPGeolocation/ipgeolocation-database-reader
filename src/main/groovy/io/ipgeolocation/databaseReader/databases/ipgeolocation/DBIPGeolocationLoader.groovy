package io.ipgeolocation.databaseReader.databases.ipgeolocation

import groovy.transform.CompileStatic
import io.ipgeolocation.databaseReader.databases.common.DatabaseVersion
import io.ipgeolocation.databaseReader.databases.common.ParseInetAddress
import io.ipgeolocation.databaseReader.databases.common.Pool
import io.ipgeolocation.databaseReader.databases.common.PoolInteger
import io.ipgeolocation.databaseReader.databases.common.PoolString
import io.ipgeolocation.databaseReader.databases.country.Country
import io.ipgeolocation.databaseReader.databases.place.Place
import io.ipgeolocation.databaseReader.services.database.CsvDatabaseService
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
class DBIPGeolocationLoader {
    private final String START_IP = "start_ip"
    private final String END_IP = "end_ip"
    private final String COUNTRY_ID = "country_id"
    private final String STATE_PLACE_ID = "state_place_id"
    private final String DISTRICT_PLACE_ID = "district_place_id"
    private final String CITY_PLACE_ID = "city_place_id"
    private final String ZIP_CODE = "zip_code"
    private final String LATITUDE = "latitude"
    private final String LONGITUDE = "longitude"
    private final String GEO_NAME_ID = "geo_name_id"
    private final String TIME_ZONE_NAME = "time_zone_name"
    private final String ISP = "isp"
    private final String CONNECTION_TYPE = "connection_type"
    private final String ORGANIZATION = "organization"
    private final String AS_NUMBER = "as_number"
    private final String[] COUNTRY_CSV_COLUMNS = [
            START_IP,
            END_IP,
            COUNTRY_ID
    ]
    private final String[] COUNTRY_CITY_CSV_COLUMNS = [
            START_IP,
            END_IP,
            COUNTRY_ID,
            STATE_PLACE_ID,
            DISTRICT_PLACE_ID,
            CITY_PLACE_ID,
            ZIP_CODE,
            LATITUDE,
            LONGITUDE,
            GEO_NAME_ID,
            TIME_ZONE_NAME
    ]
    private final String[] COUNTRY_ISP_CSV_COLUMNS = [
            START_IP,
            END_IP,
            COUNTRY_ID,
            ISP,
            CONNECTION_TYPE,
            ORGANIZATION,
            AS_NUMBER
    ]
    private final String[] COUNTRY_CITY_ISP_CSV_COLUMNS = [
            START_IP,
            END_IP,
            COUNTRY_ID,
            STATE_PLACE_ID,
            DISTRICT_PLACE_ID,
            CITY_PLACE_ID,
            ZIP_CODE,
            LATITUDE,
            LONGITUDE,
            GEO_NAME_ID,
            TIME_ZONE_NAME,
            ISP,
            CONNECTION_TYPE,
            ORGANIZATION,
            AS_NUMBER
    ]
    private final CellProcessor[] countryCellProcessors
    private final CellProcessor[] countryCityCellProcessors
    private final CellProcessor[] countryIspCellProcessors
    private final CellProcessor[] countryCityIspCellProcessors
    private final Pool pool = Pool.getInstance()
    private final CsvDatabaseService csvDatabaseService

    DBIPGeolocationLoader(CsvDatabaseService csvDatabaseService) {
        Assert.notNull(csvDatabaseService, "'csvDatabaseService' must not be null.")

        CellProcessor inetAddress = new ParseInetAddress()
        CellProcessor integer = new PoolInteger(pool)
        CellProcessor optionalString = new Optional(new PoolString(pool))
        CellProcessor optionalInteger = new Optional(new PoolInteger(pool))

        countryCellProcessors = [
                inetAddress, // start_ip
                inetAddress, // end_ip
                integer // country_id
        ]
        countryCityCellProcessors = [
                inetAddress, // start_ip
                inetAddress, // end_ip
                integer, // country_id
                optionalInteger, // state_place_id
                optionalInteger, // district_place_id
                optionalInteger, // city_place_id
                optionalString, // zip_code
                optionalString, // latitude
                optionalString, // longitude
                optionalString, // geoname_id
                optionalString // time_zone_name
        ]
        countryIspCellProcessors = [
                inetAddress, // start_ip
                inetAddress, // end_ip
                integer, // country_id
                optionalString, // isp
                optionalString, // connection_type
                optionalString, // organization
                optionalString // as_number
        ]
        countryCityIspCellProcessors = [
                inetAddress, // start_ip
                inetAddress, // end_ip
                integer, // country_id
                optionalInteger, // state_place_id
                optionalInteger, // district_place_id
                optionalInteger, // city_place_id
                optionalString, // zip_code
                optionalString, // latitude
                optionalString, // longitude
                optionalString, // geoname_id
                optionalString, // time_zone_name
                optionalString, // isp
                optionalString, // connection_type
                optionalString, // organization
                optionalString // as_number
        ]
        this.csvDatabaseService = csvDatabaseService

        Assert.state(countryCellProcessors.length == COUNTRY_CSV_COLUMNS.length, "Programmer error: length of columns" +
                " does not match length of cell processors.")
        Assert.state(countryCityCellProcessors.length == COUNTRY_CITY_CSV_COLUMNS.length, "Programmer error: length " +
                "of columns does not match length of cell processors.")
        Assert.state(countryIspCellProcessors.length == COUNTRY_ISP_CSV_COLUMNS.length, "Programmer error: length of " +
                "columns does not match length of cell processors.")
        Assert.state(countryCityIspCellProcessors.length == COUNTRY_CITY_ISP_CSV_COLUMNS.length, "Programmer error: " +
                "length of columns does not match length of cell processors.")
    }

    void load(String databaseVersion, String ipGeolocationCsvFilePath, IPGeolocationIndexer ipGeolocationIndexer) {
        Assert.isTrue(databaseVersion in DatabaseVersion.ALL_DATABASES, "'databaseVersion' must be equal to " +
                "one of these values: ${DatabaseVersion.ALL_DATABASES}")
        Assert.hasText(ipGeolocationCsvFilePath, "'ipGeolocationCsvFilePath' must not be empty or null.")
        Assert.notNull(ipGeolocationIndexer, "'ipGeolocationIndexer' must not be null.")

        Path ipGeolocationCsvPath = Paths.get(ipGeolocationCsvFilePath)

        Assert.state(Files.isRegularFile(ipGeolocationCsvPath) && Files.exists(ipGeolocationCsvPath), "$ipGeolocationCsvFilePath is missing.")

        try {
            InputStream fileInputStream = Files.newInputStream(ipGeolocationCsvPath, StandardOpenOption.READ)
            InputStream gzipInputStream = new GZIPInputStream(fileInputStream)
            Reader inputStreamReader = new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8)
            CsvMapReader csvMapReader = new CsvMapReader(inputStreamReader, CsvPreference.STANDARD_PREFERENCE)
            Map<String, Object> record

            if (databaseVersion in DatabaseVersion.IP_TO_COUNTRY_DATABASES) {
                while (!isNull(record = csvMapReader.read(COUNTRY_CSV_COLUMNS, countryCellProcessors))) {
                    Integer countryId = record.get(COUNTRY_ID) as Integer
                    Country country = null

                    if (countryId) {
                        country = csvDatabaseService.findCountry(countryId)
                    }

                    ipGeolocationIndexer.add(
                            new IPGeolocation(record.get(START_IP) as InetAddress, record.get(END_IP) as InetAddress,
                                    country, null, null, null, null, null, null,
                                    null, null, null, null, null,
                                    null))
                }
            } else if (databaseVersion == DatabaseVersion.DB_III) {
                while (!isNull(record = csvMapReader.read(COUNTRY_ISP_CSV_COLUMNS, countryIspCellProcessors))) {
                    Integer countryId = record.get(COUNTRY_ID) as Integer
                    Country country = null

                    if (countryId) {
                        country = csvDatabaseService.findCountry(countryId)
                    }

                    ipGeolocationIndexer.add(
                            new IPGeolocation(record.get(START_IP) as InetAddress, record.get(END_IP) as InetAddress,
                                    country, null, null, null, null, null, null,
                                    null, null, record.get(ISP) as String,
                                    record.get(CONNECTION_TYPE) as String, record.get(ORGANIZATION) as String,
                                    record.get(AS_NUMBER) as String))
                }
            } else if (databaseVersion in DatabaseVersion.IP_TO_CITY_DATABASES) {
                while (!isNull(record = csvMapReader.read(COUNTRY_CITY_CSV_COLUMNS, countryCityCellProcessors))) {
                    Integer countryId = record.get(COUNTRY_ID) as Integer
                    Integer statePlaceId = record.get(STATE_PLACE_ID) as Integer
                    Integer districtPlaceId = record.get(DISTRICT_PLACE_ID) as Integer
                    Integer cityPlaceId = record.get(CITY_PLACE_ID) as Integer
                    Country country = null
                    Place state = null
                    Place district = null
                    Place city = null

                    if (countryId) {
                        country = csvDatabaseService.findCountry(countryId)
                    }

                    if (statePlaceId) {
                        state = csvDatabaseService.findPlace(statePlaceId)
                    }

                    if (districtPlaceId) {
                        district = csvDatabaseService.findPlace(districtPlaceId)
                    }

                    if (cityPlaceId) {
                        city = csvDatabaseService.findPlace(cityPlaceId)
                    }

                    ipGeolocationIndexer.add(
                            new IPGeolocation(record.get(START_IP) as InetAddress, record.get(END_IP) as InetAddress,
                                    country, state, district, city, record.get(ZIP_CODE) as String,
                                    record.get(LATITUDE) as String, record.get(LONGITUDE) as String,
                                    record.get(GEO_NAME_ID) as String, record.get(TIME_ZONE_NAME) as String, null,
                                    null, null, null))
                }
            } else if (databaseVersion in DatabaseVersion.IP_TO_CITY_AND_ISP_DATABASES) {
                while (!isNull(record = csvMapReader.read(COUNTRY_CITY_ISP_CSV_COLUMNS, countryCityIspCellProcessors))) {
                    Integer countryId = record.get(COUNTRY_ID) as Integer
                    Integer statePlaceId = record.get(STATE_PLACE_ID) as Integer
                    Integer districtPlaceId = record.get(DISTRICT_PLACE_ID) as Integer
                    Integer cityPlaceId = record.get(CITY_PLACE_ID) as Integer
                    Country country = null
                    Place state = null
                    Place district = null
                    Place city = null

                    if (countryId) {
                        country = csvDatabaseService.findCountry(countryId)
                    }

                    if (statePlaceId) {
                        state = csvDatabaseService.findPlace(statePlaceId)
                    }

                    if (districtPlaceId) {
                        district = csvDatabaseService.findPlace(districtPlaceId)
                    }

                    if (cityPlaceId) {
                        city = csvDatabaseService.findPlace(cityPlaceId)
                    }

                    ipGeolocationIndexer.add(
                        new IPGeolocation(record.get(START_IP) as InetAddress, record.get(END_IP) as InetAddress,
                            country, state, district, city, record.get(ZIP_CODE) as String,
                            record.get(LATITUDE) as String, record.get(LONGITUDE) as String,
                            record.get(GEO_NAME_ID) as String, record.get(TIME_ZONE_NAME) as String,
                            record.get(ISP) as String, record.get(CONNECTION_TYPE) as String,
                            record.get(ORGANIZATION) as String, record.get(AS_NUMBER) as String))
                }
            }

            inputStreamReader.close()
            gzipInputStream.close()
            fileInputStream.close()
        } catch (IOException | NullPointerException e) {
            e.printStackTrace()
        }
    }
}
