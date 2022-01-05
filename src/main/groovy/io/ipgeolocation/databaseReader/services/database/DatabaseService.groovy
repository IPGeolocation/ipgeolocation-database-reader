package io.ipgeolocation.databaseReader.services.database

import com.google.common.base.Strings
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.ipgeolocation.databaseReader.databases.cloudprovider.CloudProviderIndexer
import io.ipgeolocation.databaseReader.databases.cloudprovider.DBCloudProviderLoader
import io.ipgeolocation.databaseReader.databases.common.Database

import io.ipgeolocation.databaseReader.databases.common.Preconditions
import io.ipgeolocation.databaseReader.databases.country.Country
import io.ipgeolocation.databaseReader.databases.country.CountryIndexer
import io.ipgeolocation.databaseReader.databases.country.DBCountryLoader
import io.ipgeolocation.databaseReader.databases.ipgeolocation.DBIPGeolocationLoader
import io.ipgeolocation.databaseReader.databases.ipgeolocation.IPGeolocation
import io.ipgeolocation.databaseReader.databases.ipgeolocation.IPGeolocationIndexer
import io.ipgeolocation.databaseReader.databases.ipsecurity.IPSecurity
import io.ipgeolocation.databaseReader.databases.place.DBPlaceLoader
import io.ipgeolocation.databaseReader.databases.place.Place
import io.ipgeolocation.databaseReader.databases.place.PlaceIndexer
import io.ipgeolocation.databaseReader.services.ipsecurity.IPSecurityDatabaseService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@CompileStatic
@Service
@Slf4j
class DatabaseService {
    @Value('${application.path.placeDatabase}') private String placeDatabasePath
    @Value('${application.path.countryDatabase}') private String countryDatabasePath
    @Value('${application.path.ipGeolocationDatabase}') private String ipGeolocationDatabasePath
    @Value('${application.path.cloudProviderDatabase}') private String cloudProviderDatabasePath
    @Value('${application.path.ipSecurityDatabase}') private String ipSecurityDatabasePath

    private final PlaceIndexer placeIndexer
    private final CountryIndexer countryIndexer
    private final IPGeolocationIndexer ipgeolocationIndexer
    private final CloudProviderIndexer cloudProviderIndexer
    private final DBPlaceLoader placeLoader
    private final DBCountryLoader countryLoader
    private final DBIPGeolocationLoader ipGeolocationLoader
    private final DBCloudProviderLoader cloudProviderLoader

    private final DatabaseUpdateService databaseUpdateService
    private final IPSecurityDatabaseService ipSecurityDatabaseService

    private String selectedDatabase

    @Autowired
    DatabaseService(DatabaseUpdateService databaseUpdateService, IPSecurityDatabaseService ipSecurityDatabaseService) {
        this.databaseUpdateService = databaseUpdateService
        this.ipSecurityDatabaseService = ipSecurityDatabaseService

        placeIndexer = new PlaceIndexer()
        placeLoader = new DBPlaceLoader()
        countryIndexer = new CountryIndexer()
        countryLoader = new DBCountryLoader(this)
        ipgeolocationIndexer = new IPGeolocationIndexer()
        ipGeolocationLoader = new DBIPGeolocationLoader(this)
        cloudProviderIndexer = new CloudProviderIndexer()
        cloudProviderLoader = new DBCloudProviderLoader()
    }

    void loadDatabases() {
        databaseUpdateService.updateSubscriptionParametersFromDatabaseCofigFile()
        databaseUpdateService.downloadLatestDatabase()

        if (databaseConfigJson && databaseConfigJson.getString("apiKey") && databaseConfigJson.getString("database") && databaseConfigJson.getString("updateInterval")) {


            String lastUpdateDateStr = databaseUpdateService.getLastUpdateDate(databaseConfigJson.getString("database"), databaseConfigJson.getString("updateInterval"))

            if (lastUpdateDateStr) {
                databaseUpdateService.updateLastUpdateDateInDatabaseConfigJson(databaseConfigJson, lastUpdateDateStr)
            } else {
                log.error("Last update date must not be empty or null.")
                System.exit(1)
            }

            selectedDatabase = databaseConfigJson.getString("database")

            if (!selectedDatabase) {
                log.error("No database has been selected to read.")
                System.exit(0)
            }

            if (Database.DATABASES.contains(selectedDatabase)) {
                if (!Preconditions.isFile(countryDatabasePath)) {
                    log.error("Missing File: ${countryDatabasePath}")
                    System.exit(1)
                }

                if (!Preconditions.isFile(placeDatabasePath)) {
                    log.error("Missing File: ${placeDatabasePath}")
                    System.exit(1)
                }

                if (!Preconditions.isFile(ipGeolocationDatabasePath)) {
                    log.error("Missing File: ${ipGeolocationDatabasePath}")
                    System.exit(1)
                }

                if (Database.DATABASES_WITH_PROXY.contains(selectedDatabase)) {
                    if (!Preconditions.isFile(cloudProviderDatabasePath)) {
                        log.error("Missing File: ${cloudProviderDatabasePath}")
                        System.exit(1)
                    }

                    if (!Preconditions.isFile(ipSecurityDatabasePath)) {
                        log.error("Missing File: ${ipSecurityDatabasePath}")
                        System.exit(1)
                    }
                }
            } else {
                log.error("Selected database to read '${selectedDatabase}' is not vaild. Valid databases are: ${Database.DATABASES.toString()}.")
                System.exit(1)
            }

            log.info("Loading places from: ${placeDatabasePath}")
            placeLoader.load(placeDatabasePath, placeIndexer)
            log.info("Loaded (${placeIndexer.size()}) places successfully.")

            log.info("Loading countries from: ${countryDatabasePath}")
            countryLoader.load(countryDatabasePath, countryIndexer)
            log.info("Loaded (${countryIndexer.size()}) countries successfully.")

            log.info("Loading ip-geolocations from: ${ipGeolocationDatabasePath}")
            ipGeolocationLoader.load(selectedDatabase, ipGeolocationDatabasePath, ipgeolocationIndexer)
            log.info("Loaded (${ipgeolocationIndexer.size()}) ip-geolocations successfully.")

            if (selectedDatabase == Database.DB_V || selectedDatabase == Database.DB_VI || selectedDatabase == Database.DB_VII) {
                log.info("Loading cloud providers from: ${cloudProviderDatabasePath}")
                cloudProviderLoader.load(cloudProviderDatabasePath, cloudProviderIndexer)
                log.info("Loaded (${cloudProviderIndexer.size()}) cloud providers successfully.")

                ipSecurityDatabaseService.loadDatabase()
            }
        } else {
            throw new IllegalStateException("Provided database configuration is not valid: {\"apiKey\": \"${databaseConfigJson.getString("apiKey")}\", \"database\": \"${databaseConfigJson.getString("database")}\", \"updateInterval\": \"${databaseConfigJson.getString("updateInterval")}\"}")
        }
    }

    final String getSelectedDatabase() {
        selectedDatabase
    }

    Place findPlace(Integer indexer) {
        placeIndexer.getAt(indexer)
    }

    Country findCountry(Integer indexer) {
        countryIndexer.getAt(indexer)
    }

    IPGeolocation findIPGeolocation(InetAddress inetAddress) {
        ipgeolocationIndexer.get(inetAddress)
    }

    IPSecurity findIPSecurity(String ipAddress) {
        ipSecurityDatabaseService.findIPSecurity(ipAddress)
    }

    Boolean isCloudProvider(String name) {
        cloudProviderIndexer.isCloudProvider(Strings.nullToEmpty(name))
    }
}
