package io.ipgeolocation.databaseReader.services.database

import com.google.common.base.Strings
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.ipgeolocation.databaseReader.databases.cloudprovider.CloudProviderIndexer
import io.ipgeolocation.databaseReader.databases.cloudprovider.DBCloudProviderLoader
import io.ipgeolocation.databaseReader.databases.common.DatabaseVersion
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
import io.ipgeolocation.databaseReader.services.path.PathsService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@CompileStatic
@Qualifier("csvDatabaseService")
@Service
@Slf4j
class CsvDatabaseService implements DatabaseService {
    private final PlaceIndexer placeIndexer = new PlaceIndexer()
    private final DBPlaceLoader placeLoader = new DBPlaceLoader()
    private final CountryIndexer countryIndexer = new CountryIndexer()
    private final DBCountryLoader countryLoader = new DBCountryLoader(this)
    private final IPGeolocationIndexer ipgeolocationIndexer = new IPGeolocationIndexer()
    private final DBIPGeolocationLoader ipGeolocationLoader = new DBIPGeolocationLoader(this)
    private final CloudProviderIndexer cloudProviderIndexer = new CloudProviderIndexer()
    private final DBCloudProviderLoader cloudProviderLoader = new DBCloudProviderLoader()
    private final DBIPSecurityLoader dbIpSecurityLoader = new DBIPSecurityLoader()
    private final IPSecurityIndexer ipSecurityIndexer = new IPSecurityIndexer()

    private final PathsService pathsService
    private final DatabaseUpdateService databaseUpdateService

    @Autowired
    CsvDatabaseService(PathsService pathsService, DatabaseUpdateService databaseUpdateService) {
        this.pathsService = pathsService
        this.databaseUpdateService = databaseUpdateService
    }

    @Override
    void loadDatabases() {
        databaseUpdateService.downloadLatestDatabaseIfUpdated()

        log.info("Loading places from: ${pathsService.getPlaceCsvDatabaseFilePath()}")
        placeLoader.load(pathsService.getPlaceCsvDatabaseFilePath(), placeIndexer)
        log.info("Loaded (${placeIndexer.size()}) places successfully.")

        log.info("Loading countries from: ${pathsService.getCountryCsvDatabaseFilePath()}")
        countryLoader.load(pathsService.getCountryCsvDatabaseFilePath(), countryIndexer)
        log.info("Loaded (${countryIndexer.size()}) countries successfully.")

        log.info("Loading ip-geolocations from: ${pathsService.getIPGeolocationCsvDatabaseFilePath()}")
        ipGeolocationLoader.load(databaseUpdateService.getDatabaseVersion(), pathsService.getIPGeolocationCsvDatabaseFilePath(), ipgeolocationIndexer)
        log.info("Loaded (${ipgeolocationIndexer.size()}) ip-geolocations successfully.")

        if (databaseUpdateService.getDatabaseVersion() in DatabaseVersion.DATABASES_WITH_PROXY) {
            log.info("Loading cloud providers from: ${pathsService.getCloudProviderCsvDatabaseFilePath()}")
            cloudProviderLoader.load(pathsService.getCloudProviderCsvDatabaseFilePath(), cloudProviderIndexer)
            log.info("Loaded (${cloudProviderIndexer.size()}) cloud providers successfully.")

            log.info("Loading ip-securities from: ${pathsService.getIPSecurityCsvDatabaseFilePath()}")
            dbIpSecurityLoader.load(pathsService.getIPSecurityCsvDatabaseFilePath(), ipSecurityIndexer)
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
