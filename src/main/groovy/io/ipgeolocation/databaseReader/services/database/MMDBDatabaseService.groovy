package io.ipgeolocation.databaseReader.services.database

import com.google.common.net.InetAddresses
import com.maxmind.db.MaxMindDbConstructor
import com.maxmind.db.MaxMindDbParameter
import com.maxmind.db.NoCache
import com.maxmind.db.NodeCache
import com.maxmind.db.Reader
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.ipgeolocation.databaseReader.databases.cloudprovider.CloudProviderIndexer
import io.ipgeolocation.databaseReader.databases.cloudprovider.DBCloudProviderLoader
import io.ipgeolocation.databaseReader.databases.common.IPGeolocationDatabase
import io.ipgeolocation.databaseReader.databases.country.Country
import io.ipgeolocation.databaseReader.databases.ipgeolocation.Currency
import io.ipgeolocation.databaseReader.databases.ipgeolocation.IPGeolocation
import io.ipgeolocation.databaseReader.databases.ipsecurity.IPSecurity

import io.ipgeolocation.databaseReader.databases.place.Place

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.Assert

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@CompileStatic
@Qualifier("mmdbDatabaseService")
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
        databaseUpdateService.downloadLatestDatabaseIfUpdated()

        Path ipGeolocationMMDBPath = Paths.get(ipGeolocationMMDBFilePath)
        Assert.state(Files.isRegularFile(ipGeolocationMMDBPath) && Files.exists(ipGeolocationMMDBPath), "$ipGeolocationMMDBFilePath is missing.")

        NodeCache noCache = NoCache.getInstance()

        log.info("Initializing ip-geolocation MMDB reader.")
        ipGeolocationMMDBReader = new Reader(ipGeolocationMMDBPath.toFile(), noCache)

        if (databaseUpdateService.getDatabaseVersion() in IPGeolocationDatabase.DATABASES_WITH_PROXY) {
            Path ipSecurityMMDBPath = Paths.get(ipSecurityMMDBFilePath)
            Assert.state(Files.isRegularFile(ipSecurityMMDBPath) && Files.exists(ipSecurityMMDBPath), "$ipSecurityMMDBFilePath is missing.")

            log.info("Loading cloud providers from: ${cloudProviderCsvDatabaseFilePath}")
            cloudProviderLoader.load(cloudProviderCsvDatabaseFilePath, cloudProviderIndexer)
            log.info("Loaded (${cloudProviderIndexer.size()}) cloud providers successfully.")

            log.info("Initializing ip-geolocation MMDB reader.")
            ipSecurityMMDBReader = new Reader(ipSecurityMMDBPath.toFile(), noCache)
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
        IPGeolocation ipGeolocation = null

        if (databaseUpdateService.getDatabaseVersion() in IPGeolocationDatabase.IP_TO_COUNTRY_DATABASES) {
            ipGeolocation = ipGeolocationMMDBReader.get(inetAddress, IPCountryResponse.class)
        } else if (databaseUpdateService.getDatabaseVersion() == IPGeolocationDatabase.DB_III) {
            ipGeolocation = ipGeolocationMMDBReader.get(inetAddress, IPISPResponse.class)
        } else if (databaseUpdateService.getDatabaseVersion() in IPGeolocationDatabase.IP_TO_CITY_DATABASES) {
            ipGeolocation = ipGeolocationMMDBReader.get(inetAddress, IPCityResponse.class)
        } else if (databaseUpdateService.getDatabaseVersion() in IPGeolocationDatabase.IP_TO_CITY_AND_ISP_DATABASES) {
            ipGeolocation = ipGeolocationMMDBReader.get(inetAddress, IPCityAndISPResponse.class)
        }

        ipGeolocation
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

class IPCountryResponse extends IPGeolocation {
    @MaxMindDbConstructor
    IPCountryResponse(
            @MaxMindDbParameter(name = "continent_code") String continentCode,
            @MaxMindDbParameter(name = "continent_name") Place continentName,
            @MaxMindDbParameter(name = "country_code2") String countryCodeISO2,
            @MaxMindDbParameter(name = "country_code3") String countryCodeISO3,
            @MaxMindDbParameter(name = "country_name") Place countryName,
            @MaxMindDbParameter(name = "country_capital") Place countryCapital,
            @MaxMindDbParameter(name = "currency") Currency currency,
            @MaxMindDbParameter(name = "calling_code") String callingCode,
            @MaxMindDbParameter(name = "languages") String languages,
            @MaxMindDbParameter(name = "tld") String tld) {
        super(InetAddresses.forString("0.0.0.0"), InetAddresses.forString("0.0.0.0"),
                new Country(-1, continentCode, continentName, countryCodeISO2, countryCodeISO3, countryName,
                        countryCapital, currency?.code, currency?.name, currency?.symbol, callingCode, tld, languages),
                null, null, null, null, null, null, null,
                null, null, null, null, null)
    }
}

@CompileStatic
class IPCityResponse extends IPGeolocation {
    @MaxMindDbConstructor
    IPCityResponse(
            @MaxMindDbParameter(name = "continent_code") String continentCode,
            @MaxMindDbParameter(name = "continent_name") Place continentName,
            @MaxMindDbParameter(name = "country_code2") String countryCodeISO2,
            @MaxMindDbParameter(name = "country_code3") String countryCodeISO3,
            @MaxMindDbParameter(name = "country_name") Place countryName,
            @MaxMindDbParameter(name = "country_capital") Place countryCapital,
            @MaxMindDbParameter(name = "state_prov") Place state,
            @MaxMindDbParameter(name = "district") Place district,
            @MaxMindDbParameter(name = "city") Place city,
            @MaxMindDbParameter(name = "zip_code") String zipCode,
            @MaxMindDbParameter(name = "latitude") float latitude,
            @MaxMindDbParameter(name = "longitude") float longitude,
            @MaxMindDbParameter(name = "geoname_id") String geoNameId,
            @MaxMindDbParameter(name = "time_zone") String timeZoneName,
            @MaxMindDbParameter(name = "currency") Currency currency,
            @MaxMindDbParameter(name = "calling_code") String callingCode,
            @MaxMindDbParameter(name = "languages") String languages,
            @MaxMindDbParameter(name = "tld") String tld) {
        super(InetAddresses.forString("0.0.0.0"), InetAddresses.forString("0.0.0.0"),
                new Country(-1, continentCode, continentName, countryCodeISO2, countryCodeISO3, countryName,
                        countryCapital, currency?.code, currency?.name, currency?.symbol, callingCode, tld, languages),
                state, district, city, zipCode, latitude ? String.format("%.5f", latitude) : null,
                longitude ? String.format("%.5f", longitude) : null, geoNameId, timeZoneName, null, null,
                null, null)
    }
}

class IPISPResponse extends IPGeolocation {
    @MaxMindDbConstructor
    IPISPResponse(
            @MaxMindDbParameter(name = "continent_code") String continentCode,
            @MaxMindDbParameter(name = "continent_name") Place continentName,
            @MaxMindDbParameter(name = "country_code2") String countryCodeISO2,
            @MaxMindDbParameter(name = "country_code3") String countryCodeISO3,
            @MaxMindDbParameter(name = "country_name") Place countryName,
            @MaxMindDbParameter(name = "country_capital") Place countryCapital,
            @MaxMindDbParameter(name = "isp") String isp,
            @MaxMindDbParameter(name = "connection_type") String connectionType,
            @MaxMindDbParameter(name = "organization") String organization,
            @MaxMindDbParameter(name = "as_number") BigInteger asNumber,
            @MaxMindDbParameter(name = "currency") Currency currency,
            @MaxMindDbParameter(name = "calling_code") String callingCode,
            @MaxMindDbParameter(name = "languages") String languages,
            @MaxMindDbParameter(name = "tld") String tld) {
        super(InetAddresses.forString("0.0.0.0"), InetAddresses.forString("0.0.0.0"),
                new Country(-1, continentCode, continentName, countryCodeISO2, countryCodeISO3, countryName,
                        countryCapital, currency?.code, currency?.name, currency?.symbol, callingCode, tld, languages),
                null, null, null, null, null, null, null,
                null, isp, connectionType, organization, asNumber.toString())
    }
}

@CompileStatic
class IPCityAndISPResponse extends IPGeolocation {
    @MaxMindDbConstructor
    IPCityAndISPResponse(
            @MaxMindDbParameter(name = "continent_code") String continentCode,
            @MaxMindDbParameter(name = "continent_name") Place continentName,
            @MaxMindDbParameter(name = "country_code2") String countryCodeISO2,
            @MaxMindDbParameter(name = "country_code3") String countryCodeISO3,
            @MaxMindDbParameter(name = "country_name") Place countryName,
            @MaxMindDbParameter(name = "country_capital") Place countryCapital,
            @MaxMindDbParameter(name = "state_prov") Place state,
            @MaxMindDbParameter(name = "district") Place district,
            @MaxMindDbParameter(name = "city") Place city,
            @MaxMindDbParameter(name = "zip_code") String zipCode,
            @MaxMindDbParameter(name = "latitude") float latitude,
            @MaxMindDbParameter(name = "longitude") float longitude,
            @MaxMindDbParameter(name = "geoname_id") String geoNameId,
            @MaxMindDbParameter(name = "time_zone") String timeZoneName,
            @MaxMindDbParameter(name = "isp") String isp,
            @MaxMindDbParameter(name = "connection_type") String connectionType,
            @MaxMindDbParameter(name = "organization") String organization,
            @MaxMindDbParameter(name = "as_number") BigInteger asNumber,
            @MaxMindDbParameter(name = "currency") Currency currency,
            @MaxMindDbParameter(name = "calling_code") String callingCode,
            @MaxMindDbParameter(name = "languages") String languages,
            @MaxMindDbParameter(name = "tld") String tld) {
        super(InetAddresses.forString("0.0.0.0"), InetAddresses.forString("0.0.0.0"),
                new Country(-1, continentCode, continentName, countryCodeISO2, countryCodeISO3, countryName,
                        countryCapital, currency?.code, currency?.name, currency?.symbol, callingCode, tld, languages),
                state, district, city, zipCode, latitude ? String.format("%.5f", latitude) : null,
                longitude ? String.format("%.5f", longitude) : null, geoNameId, timeZoneName, isp, connectionType,
                organization, asNumber.toString())
    }
}