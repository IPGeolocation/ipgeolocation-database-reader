package io.ipgeolocation.databaseReader.services.database

import com.google.common.net.InetAddresses
import com.maxmind.db.Reader
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.ipgeolocation.databaseReader.databases.cloudprovider.CloudProviderIndexer
import io.ipgeolocation.databaseReader.databases.cloudprovider.DBCloudProviderLoader
import io.ipgeolocation.databaseReader.databases.common.IPGeolocationDatabase
import io.ipgeolocation.databaseReader.databases.country.Country
import io.ipgeolocation.databaseReader.databases.ipgeolocation.IPGeolocation
import io.ipgeolocation.databaseReader.databases.ipsecurity.IPSecurity
import io.ipgeolocation.databaseReader.databases.place.Place

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.Assert

@CompileStatic
@Service
@Slf4j
class MMDBDatabaseService implements DatabaseService {
    @Value('${application.path.databases.IPGeolocationMMDBFile}')
    private String ipGeolocationMMDBFilePath

    @Value('${application.path.databases.CloudProviderCsvFile}')
    private String cloudProviderCsvDatabaseFilePath

    @Value('${application.path.databases.IPSecurityMMDBFile}')
    private String ipSecurityMMDBFilePath

    private final CloudProviderIndexer cloudProviderIndexer = new CloudProviderIndexer()
    private final DBCloudProviderLoader cloudProviderLoader = new DBCloudProviderLoader()

    private final DatabaseUpdateService databaseUpdateService

    private Reader ipGeolocationMMDBReader
    private Reader ipSecurityMMDBReader

    @Autowired
    MMDBDatabaseService(DatabaseUpdateService databaseUpdateService) {
        this.databaseUpdateService = databaseUpdateService
    }

    @Override
    void loadDatabases() {
        databaseUpdateService.updateSubscriptionParametersFromDatabaseCofigFile()
        databaseUpdateService.downloadLatestDatabase()

        File ipGeolocationDatabaseFile = new File(ipGeolocationMMDBFilePath)

        log.info("Initializing ip-geolocation MMDB reader.")
        ipGeolocationMMDBReader = new Reader(ipGeolocationDatabaseFile)

        Assert.state(ipGeolocationDatabaseFile.isFile() && ipGeolocationDatabaseFile.exists(), "db-ip-geolocation.mmdb is missing at $ipGeolocationMMDBFilePath path.")

        if (databaseUpdateService.getDatabaseVersion() in IPGeolocationDatabase.DATABASES_WITH_PROXY) {
            File cloudProviderDatabaseFile = new File(cloudProviderCsvDatabaseFilePath)
            File ipSecurityDatabaseFile = new File(ipSecurityMMDBFilePath)

            Assert.state(cloudProviderDatabaseFile.isFile() && cloudProviderDatabaseFile.exists(), "db-cloud-provider.csv.gz is missing at $cloudProviderDatabaseFile path.")
            Assert.state(ipSecurityDatabaseFile.isFile() && ipSecurityDatabaseFile.exists(), "db-ip-security.mmdb is missing at $ipSecurityDatabaseFile path.")

            log.info("Loading cloud providers from: ${cloudProviderCsvDatabaseFilePath}")
            cloudProviderLoader.load(cloudProviderCsvDatabaseFilePath, cloudProviderIndexer)
            log.info("Loaded (${cloudProviderIndexer.size()}) cloud providers successfully.")

            log.info("Initializing ip-geolocation MMDB reader.")
            ipSecurityMMDBReader = new Reader(ipSecurityDatabaseFile)
        }
    }

    @Override
    Place findPlace(Integer indexer) {
        return null
    }

    @Override
    Country findCountry(Integer indexer) {
        return null
    }

    @Override
    IPGeolocation findIPGeolocation(InetAddress inetAddress) {
        ipGeolocationMMDBReader.get(inetAddress, IPGeolocation.class)
    }

    @Override
    IPSecurity findIPSecurity(String ipAddress) {
        ipSecurityMMDBReader.get(InetAddresses.forString(ipAddress), IPSecurity.class)
    }

    @Override
    Boolean isCloudProvider(String name) {
        cloudProviderIndexer.isCloudProvider(name)
    }
}
