package io.ipgeolocation.databaseReader.databases.ipgeolocation

import groovy.transform.CompileStatic
import io.ipgeolocation.databaseReader.databases.common.Database
import io.ipgeolocation.databaseReader.databases.common.ParseInetAddress
import io.ipgeolocation.databaseReader.databases.common.Pool
import io.ipgeolocation.databaseReader.databases.common.PoolInteger
import io.ipgeolocation.databaseReader.databases.common.PoolString
import io.ipgeolocation.databaseReader.databases.country.Country
import io.ipgeolocation.databaseReader.databases.place.Place
import io.ipgeolocation.databaseReader.services.database.DatabaseService

import org.springframework.lang.NonNull
import org.springframework.stereotype.Service
import org.supercsv.cellprocessor.Optional
import org.supercsv.cellprocessor.ift.CellProcessor
import org.supercsv.io.CsvMapReader

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.zip.GZIPInputStream

import static com.google.common.base.Preconditions.checkNotNull
import static com.google.common.base.Strings.isNullOrEmpty
import static org.supercsv.prefs.CsvPreference.STANDARD_PREFERENCE
import static io.ipgeolocation.common.Strings.checkNotEmptyOrNull

@CompileStatic
@Service
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
    private final String GEONAME_ID = "geoname_id"
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
            GEONAME_ID,
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
            GEONAME_ID,
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
    private final DatabaseService databaseService

    DBIPGeolocationLoader(@NonNull DatabaseService databaseService) {
        CellProcessor inetAddress = new ParseInetAddress()
        CellProcessor integer = new PoolInteger(pool)
        CellProcessor optionalString = new Optional(new PoolString(pool))
        CellProcessor optionalInteger = new Optional(new PoolInteger(pool))
        this.countryCellProcessors = [
                inetAddress, // start_ip
                inetAddress, // end_ip
                integer // country_id
        ]
        this.countryCityCellProcessors = [
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
        this.countryIspCellProcessors = [
                inetAddress, // start_ip
                inetAddress, // end_ip
                integer, // country_id
                optionalString, // isp
                optionalString, // connection_type
                optionalString, // organization
                optionalString // as_number
        ]
        this.countryCityIspCellProcessors = [
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
        this.databaseService = databaseService

        if (countryCityIspCellProcessors.length != COUNTRY_CITY_ISP_CSV_COLUMNS.length) {
            throw new Exception("Programmer error: length of columns does not match length of cell processors.")
        }
    }

    void load(String selectedDatabase, String databasePath, IPGeolocationIndexer indexer) {
        if (isNullOrEmpty(selectedDatabase) || !Database.DATABASES.contains(selectedDatabase)) {
            throw new IllegalArgumentException("Pre-condition violated: selected database to read '${selectedDatabase}' is not valid.")
        }

        checkNotEmptyOrNull(databasePath, "Pre-condition violated: database path must not be null or empty.")
        checkNotNull(indexer, "Pre-condition violated: indexer must not be null.")

        try {
            InputStream fis = Files.newInputStream(Paths.get(databasePath), StandardOpenOption.READ)
            InputStream gis = new GZIPInputStream(fis)
            Reader inputStreamReader = new InputStreamReader(gis, StandardCharsets.UTF_8)

            if (Database.IP_TO_COUNTRY_DATABASES.contains(selectedDatabase)) {
                parseIPToCountryDatabase(inputStreamReader, indexer)
            } else if (Database.DB_III == selectedDatabase) {
                parseIPToISPDatabase(inputStreamReader, indexer)
            } else if (Database.IP_TO_CITY_DATABASES.contains(selectedDatabase)) {
                parseIPToCityDatabase(inputStreamReader, indexer)
            } else if (Database.IP_TO_CITY_AND_ISP_DATABASES.contains(selectedDatabase)) {
                parseIPToCityAndISPDatabase(inputStreamReader, indexer)
            }

            inputStreamReader.close()
            gis.close()
            fis.close()
        } catch (Exception e) {
            e.printStackTrace()
        }
    }

    private void parseIPToCountryDatabase(Reader input, IPGeolocationIndexer indexer) {
        try {
            Map<String, Object> record
            CsvMapReader reader = new CsvMapReader(input, STANDARD_PREFERENCE)
            // Integer counter = 0

            while ((record = reader.read(COUNTRY_CSV_COLUMNS, countryCellProcessors)) != null) {
                /* if (Environment.current != Environment.PRODUCTION && ++counter > 50) {
                    break
                } */

                Integer countryId = record.get(COUNTRY_ID) as Integer
                Country country = null

                if (countryId) {
                    country = databaseService.findCountry(countryId)
                }

                indexer.add(
                        new IPGeolocation(
                                record.get(START_IP) as InetAddress,
                                record.get(END_IP) as InetAddress,
                                country,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null))
            }
        } catch (IOException e) {
            e.printStackTrace()
        }
    }

    private void parseIPToISPDatabase(Reader input, IPGeolocationIndexer indexer) {
        try {
            Map<String, Object> record
            CsvMapReader reader = new CsvMapReader(input, STANDARD_PREFERENCE)
            // Integer counter = 0

            while ((record = reader.read(COUNTRY_ISP_CSV_COLUMNS, countryIspCellProcessors)) != null) {
                /* if (++counter > 50) {
                    break
                } */

                Integer countryId = record.get(COUNTRY_ID) as Integer
                Country country = null

                if (countryId) {
                    country = databaseService.findCountry(countryId)
                }

                indexer.add(
                        new IPGeolocation(
                                record.get(START_IP) as InetAddress,
                                record.get(END_IP) as InetAddress,
                                country,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                record.get(ISP) as String,
                                record.get(CONNECTION_TYPE) as String,
                                record.get(ORGANIZATION) as String,
                                record.get(AS_NUMBER) as String))
            }
        } catch (IOException e) {
            e.printStackTrace()
        }
    }

    private void parseIPToCityDatabase(Reader input, IPGeolocationIndexer indexer) {
        try {
            Map<String, Object> record
            CsvMapReader reader = new CsvMapReader(input, STANDARD_PREFERENCE)
            // Integer counter = 0

            while ((record = reader.read(COUNTRY_CITY_CSV_COLUMNS, countryCityCellProcessors)) != null) {
                /* if (Environment.current != Environment.PRODUCTION && ++counter > 50) {
                    break
                } */

                Integer countryId = record.get(COUNTRY_ID) as Integer
                Integer statePlaceId = record.get(STATE_PLACE_ID) as Integer
                Integer districtPlaceId = record.get(DISTRICT_PLACE_ID) as Integer
                Integer cityPlaceId = record.get(CITY_PLACE_ID) as Integer
                Country country = null
                Place state = null
                Place district = null
                Place city = null

                if (countryId) {
                    country = databaseService.findCountry(countryId)
                }

                if (statePlaceId) {
                    state = databaseService.findPlace(statePlaceId)
                }

                if (districtPlaceId) {
                    district = databaseService.findPlace(districtPlaceId)
                }

                if (cityPlaceId) {
                    city = databaseService.findPlace(cityPlaceId)
                }

                indexer.add(
                        new IPGeolocation(
                                record.get(START_IP) as InetAddress,
                                record.get(END_IP) as InetAddress,
                                country,
                                state,
                                district,
                                city,
                                record.get(ZIP_CODE) as String,
                                record.get(LATITUDE) as String,
                                record.get(LONGITUDE) as String,
                                record.get(GEONAME_ID) as String,
                                record.get(TIME_ZONE_NAME) as String,
                                null,
                                null,
                                null,
                                null))
            }
        } catch (IOException e) {
            e.printStackTrace()
        }
    }

    private void parseIPToCityAndISPDatabase(Reader input, IPGeolocationIndexer indexer) {
        try {
            Map<String, Object> record
            CsvMapReader reader = new CsvMapReader(input, STANDARD_PREFERENCE)
            // Integer counter = 0

            while ((record = reader.read(COUNTRY_CITY_ISP_CSV_COLUMNS, countryCityIspCellProcessors)) != null) {
                /* if (Environment.current != Environment.PRODUCTION && ++counter > 50) {
                    break
                } */

                Integer countryId = record.get(COUNTRY_ID) as Integer
                Integer statePlaceId = record.get(STATE_PLACE_ID) as Integer
                Integer districtPlaceId = record.get(DISTRICT_PLACE_ID) as Integer
                Integer cityPlaceId = record.get(CITY_PLACE_ID) as Integer
                Country country = null
                Place state = null
                Place district = null
                Place city = null

                if (countryId) {
                    country = databaseService.findCountry(countryId)
                }

                if (statePlaceId) {
                    state = databaseService.findPlace(statePlaceId)
                }

                if (districtPlaceId) {
                    district = databaseService.findPlace(districtPlaceId)
                }

                if (cityPlaceId) {
                    city = databaseService.findPlace(cityPlaceId)
                }

                indexer.add(
                        new IPGeolocation(
                                record.get(START_IP) as InetAddress,
                                record.get(END_IP) as InetAddress,
                                country,
                                state,
                                district,
                                city,
                                record.get(ZIP_CODE) as String,
                                record.get(LATITUDE) as String,
                                record.get(LONGITUDE) as String,
                                record.get(GEONAME_ID) as String,
                                record.get(TIME_ZONE_NAME) as String,
                                record.get(ISP) as String,
                                record.get(CONNECTION_TYPE) as String,
                                record.get(ORGANIZATION) as String,
                                record.get(AS_NUMBER) as String))
            }
        } catch (IOException e) {
            e.printStackTrace()
        }
    }
}
