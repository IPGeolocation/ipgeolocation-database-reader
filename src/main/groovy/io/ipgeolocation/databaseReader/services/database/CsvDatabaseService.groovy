package io.ipgeolocation.databaseReader.services.database

import com.google.common.base.Strings
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.ipgeolocation.databaseReader.databases.cloudprovider.CloudProviderIndexer
import io.ipgeolocation.databaseReader.databases.cloudprovider.DBCloudProviderLoader
import io.ipgeolocation.databaseReader.databases.common.IPGeolocationDatabase
import io.ipgeolocation.databaseReader.databases.country.Country
import io.ipgeolocation.databaseReader.databases.country.CountryIndexer
import io.ipgeolocation.databaseReader.databases.country.DBCountryLoader
import io.ipgeolocation.databaseReader.databases.ipgeolocation.DBIPGeolocationLoader
import io.ipgeolocation.databaseReader.databases.ipgeolocation.IPGeolocation
import io.ipgeolocation.databaseReader.databases.ipgeolocation.IPGeolocationIndexer
import io.ipgeolocation.databaseReader.databases.ipsecurity.DBIPSecurityLoader
import io.ipgeolocation.databaseReader.databases.ipsecurity.IPSecurity
import io.ipgeolocation.databaseReader.databases.ipsecurity.IPSecurityIndexer
import io.ipgeolocation.databaseReader.databases.place.DBPlaceLoader
import io.ipgeolocation.databaseReader.databases.place.Place
import io.ipgeolocation.databaseReader.databases.place.PlaceIndexer

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.Assert

@CompileStatic
@Service
@Slf4j
class CsvDatabaseService implements DatabaseService {
    @Value('${application.path.databases.PlaceCsvFile}')
    private String placeCsvDatabaseFilePath

    @Value('${application.path.databases.CountryCsvFile}')
    private String countryCsvDatabaseFilePath

    @Value('${application.path.databases.IPGeolocationCsvFile}')
    private String ipGeolocationCsvDatabaseFilePath

    @Value('${application.path.databases.CloudProviderCsvFile}')
    private String cloudProviderCsvDatabaseFilePath

    @Value('${application.path.databases.IPSecurityCsvFile}')
    private String ipSecurityCsvDatabaseFilePath

    private final PlaceIndexer placeIndexer = new PlaceIndexer()
    private final CountryIndexer countryIndexer = new CountryIndexer()
    private final IPGeolocationIndexer ipgeolocationIndexer = new IPGeolocationIndexer()
    private final CloudProviderIndexer cloudProviderIndexer = new CloudProviderIndexer()
    private final DBPlaceLoader placeLoader = new DBPlaceLoader()
    private final DBCountryLoader countryLoader = new DBCountryLoader(this)
    private final DBIPGeolocationLoader ipGeolocationLoader = new DBIPGeolocationLoader(this)
    private final DBCloudProviderLoader cloudProviderLoader = new DBCloudProviderLoader()
    private final DBIPSecurityLoader dbIpSecurityLoader = new DBIPSecurityLoader()
    private final IPSecurityIndexer ipSecurityIndexer = new IPSecurityIndexer()

    private final DatabaseUpdateService databaseUpdateService

    @Autowired
    CsvDatabaseService(DatabaseUpdateService databaseUpdateService) {
        this.databaseUpdateService = databaseUpdateService
    }

    @Override
    void loadDatabases() {
        databaseUpdateService.updateSubscriptionParametersFromDatabaseCofigFile()
        databaseUpdateService.downloadLatestDatabase()

        File countryDatabaseFile = new File(countryCsvDatabaseFilePath)
        File placeDatabaseFile = new File(placeCsvDatabaseFilePath)
        File ipGeolocationDatabaseFile = new File(ipGeolocationCsvDatabaseFilePath)

        Assert.state(countryDatabaseFile.isFile() && countryDatabaseFile.exists(), "db-country.csv.gz is missing at $countryCsvDatabaseFilePath path.")
        Assert.state(placeDatabaseFile.isFile() && placeDatabaseFile.exists(), "db-place.csv.gz is missing at $placeCsvDatabaseFilePath path.")
        Assert.state(ipGeolocationDatabaseFile.isFile() && ipGeolocationDatabaseFile.exists(), "db-ip-geolocation.csv.gz is missing at $ipGeolocationCsvDatabaseFilePath path.")

        if (databaseUpdateService.getDatabaseVersion() in IPGeolocationDatabase.DATABASES_WITH_PROXY) {
            File cloudProviderDatabaseFile = new File(cloudProviderCsvDatabaseFilePath)
            File ipSecurityDatabaseFile = new File(ipSecurityCsvDatabaseFilePath)

            Assert.state(cloudProviderDatabaseFile.isFile() && cloudProviderDatabaseFile.exists(), "db-cloud-provider.csv.gz is missing at $cloudProviderDatabaseFile path.")
            Assert.state(ipSecurityDatabaseFile.isFile() && ipSecurityDatabaseFile.exists(), "db-ip-security.csv.gz is missing at $ipSecurityCsvDatabaseFilePath path.")
        }

        log.info("Loading places from: ${placeCsvDatabaseFilePath}")
        placeLoader.load(placeCsvDatabaseFilePath, placeIndexer)
        log.info("Loaded (${placeIndexer.size()}) places successfully.")

        log.info("Loading countries from: ${countryCsvDatabaseFilePath}")
        countryLoader.load(countryCsvDatabaseFilePath, countryIndexer)
        log.info("Loaded (${countryIndexer.size()}) countries successfully.")

        log.info("Loading ip-geolocations from: ${ipGeolocationCsvDatabaseFilePath}")
        ipGeolocationLoader.load(databaseUpdateService.getDatabaseVersion(), ipGeolocationCsvDatabaseFilePath, ipgeolocationIndexer)
        log.info("Loaded (${ipgeolocationIndexer.size()}) ip-geolocations successfully.")

        if (databaseUpdateService.getDatabaseVersion() in IPGeolocationDatabase.DATABASES_WITH_PROXY) {
            log.info("Loading cloud providers from: ${cloudProviderCsvDatabaseFilePath}")
            cloudProviderLoader.load(cloudProviderCsvDatabaseFilePath, cloudProviderIndexer)
            log.info("Loaded (${cloudProviderIndexer.size()}) cloud providers successfully.")

            log.info("Loading ip-securities from: ${ipSecurityCsvDatabaseFilePath}")
            dbIpSecurityLoader.load(ipSecurityCsvDatabaseFilePath, ipSecurityIndexer)
            log.info("Loaded ${ipSecurityIndexer.size()} ip-securities successfully.")
        }
    }

    @Override
    Place findPlace(Integer indexer) {
        placeIndexer.findAt(indexer)
    }

    @Override
    Country findCountry(Integer indexer) {
        countryIndexer.findAt(indexer)
    }

    @Override
    IPGeolocation findIPGeolocation(InetAddress inetAddress) {
        ipgeolocationIndexer.get(inetAddress)
    }

    @Override
    IPSecurity findIPSecurity(String ipAddress) {
        ipSecurityIndexer.get(ipAddress)
    }

    @Override
    Boolean isCloudProvider(String name) {
        cloudProviderIndexer.isCloudProvider(Strings.nullToEmpty(name))
    }
}