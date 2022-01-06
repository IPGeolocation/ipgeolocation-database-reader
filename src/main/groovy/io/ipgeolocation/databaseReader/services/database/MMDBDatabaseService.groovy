package io.ipgeolocation.databaseReader.services.database

import com.google.common.net.InetAddresses
import com.maxmind.db.CHMCache
import com.maxmind.db.MaxMindDbConstructor
import com.maxmind.db.MaxMindDbParameter
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

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

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

        Path ipGeolocationMMDBPath = Paths.get(ipGeolocationMMDBFilePath)
        Assert.state(Files.isRegularFile(ipGeolocationMMDBPath) && Files.exists(ipGeolocationMMDBPath), "$ipGeolocationMMDBFilePath is missing.")

        log.info("Initializing ip-geolocation MMDB reader.")
        ipGeolocationMMDBReader = new Reader(ipGeolocationMMDBPath.toFile(), new CHMCache())

        if (databaseUpdateService.getDatabaseVersion() in IPGeolocationDatabase.DATABASES_WITH_PROXY) {
            Path ipSecurityMMDBPath = Paths.get(ipSecurityMMDBFilePath)
            Assert.state(Files.isRegularFile(ipSecurityMMDBPath) && Files.exists(ipSecurityMMDBPath), "$ipSecurityMMDBFilePath is missing.")

            log.info("Loading cloud providers from: ${cloudProviderCsvDatabaseFilePath}")
            cloudProviderLoader.load(cloudProviderCsvDatabaseFilePath, cloudProviderIndexer)
            log.info("Loaded (${cloudProviderIndexer.size()}) cloud providers successfully.")

            log.info("Initializing ip-geolocation MMDB reader.")
            ipSecurityMMDBReader = new Reader(ipSecurityMMDBPath.toFile(), new CHMCache())
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
    IPGeolocation findIPGeolocation(String ipAddress) {
        return null
    }

    @Override
    IPGeolocationMMDB findIPGeolocation(InetAddress inetAddress) {
        ipGeolocationMMDBReader.get(inetAddress, IPGeolocationMMDB.class)
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

@CompileStatic
class IPGeolocationMMDB {
    final String continentCode
    final MultiLangValueMMDB continentName
    final String countryCodeISO2
    final String countryCodeISO3
    final MultiLangValueMMDB countryName
    final MultiLangValueMMDB countryCapital
    final MultiLangValueMMDB stateProvince
    final MultiLangValueMMDB district
    final MultiLangValueMMDB city
    final String zipCode
    final float latitude
    final float longitude
    final String geoNameId
    final String timeZone
    final String isp
    final String connectionType
    final String organization
    final BigInteger asNumber
    final CurrencyMMDB currency
    final String callingCode
    final String languages
    final String tld

    @MaxMindDbConstructor
    IPGeolocationMMDB(
            @MaxMindDbParameter(name = "continent_code") String continentCode,
            @MaxMindDbParameter(name = "continent_name") MultiLangValueMMDB continentName,
            @MaxMindDbParameter(name = "country_code2") String countryCodeISO2,
            @MaxMindDbParameter(name = "country_code3") String countryCodeISO3,
            @MaxMindDbParameter(name = "country_name") MultiLangValueMMDB countryName,
            @MaxMindDbParameter(name = "country_capital") MultiLangValueMMDB countryCapital,
            @MaxMindDbParameter(name = "state_prov") MultiLangValueMMDB stateProvince,
            @MaxMindDbParameter(name = "district") MultiLangValueMMDB district,
            @MaxMindDbParameter(name = "city") MultiLangValueMMDB city,
            @MaxMindDbParameter(name = "zip_code") String zipCode,
            @MaxMindDbParameter(name = "latitude") float latitude,
            @MaxMindDbParameter(name = "longitude") float longitude,
            @MaxMindDbParameter(name = "geoname_id") String geoNameId,
            @MaxMindDbParameter(name = "time_zone") String timeZone,
            @MaxMindDbParameter(name = "isp") String isp,
            @MaxMindDbParameter(name = "connection_type") String connectionType,
            @MaxMindDbParameter(name = "organization") String organization,
            @MaxMindDbParameter(name = "as_number") BigInteger asNumber,
            @MaxMindDbParameter(name = "currency") CurrencyMMDB currency,
            @MaxMindDbParameter(name = "calling_code") String callingCode,
            @MaxMindDbParameter(name = "languages") String languages,
            @MaxMindDbParameter(name = "tld") String tld) {
        this.continentCode = continentCode
        this.continentName = continentName
        this.countryCodeISO2 = countryCodeISO2
        this.countryCodeISO3 = countryCodeISO3
        this.countryName = countryName
        this.countryCapital = countryCapital
        this.stateProvince = stateProvince
        this.district = district
        this.city = city
        this.zipCode = zipCode
        this.latitude = latitude
        this.longitude = longitude
        this.geoNameId = geoNameId
        this.timeZone = timeZone
        this.isp = isp
        this.connectionType = connectionType
        this.organization = organization
        this.asNumber = asNumber
        this.currency = currency
        this.callingCode = callingCode
        this.languages = languages
        this.tld = tld
    }
}

@CompileStatic
class MultiLangValueMMDB {
    final String en
    final String de
    final String cs
    final String es
    final String fr
    final String it
    final String ja
    final String ru
    final String zh

    @MaxMindDbConstructor
    MultiLangValueMMDB(
            @MaxMindDbParameter(name = "en") String en,
            @MaxMindDbParameter(name = "de") String de,
            @MaxMindDbParameter(name = "cs") String cs,
            @MaxMindDbParameter(name = "es") String es,
            @MaxMindDbParameter(name = "fr") String fr,
            @MaxMindDbParameter(name = "it") String it,
            @MaxMindDbParameter(name = "ja") String ja,
            @MaxMindDbParameter(name = "ru") String ru,
            @MaxMindDbParameter(name = "zh") String zh) {
        this.en = en
        this.de = de
        this.cs = cs
        this.es = es
        this.fr = fr
        this.it = it
        this.ja = ja
        this.ru = ru
        this.zh = zh
    }
}

@CompileStatic
class CurrencyMMDB {
    final String code
    final String name
    final String symbol

    @MaxMindDbConstructor
    CurrencyMMDB(
            @MaxMindDbParameter(name = "code") String code,
            @MaxMindDbParameter(name = "name") String name,
            @MaxMindDbParameter(name = "symbol") String symbol) {
        this.code = code
        this.symbol = symbol
        this.name = name
    }
}