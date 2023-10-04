package io.ipgeolocation.databaseReader.services.path

import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct

@CompileStatic
@Service
class PathsService {
    @Value('${ipgeolocation.database.workingDirectory}')
    private String homeDir

    private String placeCsvDatabaseFilePath
    private String countryCsvDatabaseFilePath
    private String ipGeolocationCsvDatabaseFilePath
    private String cloudProviderCsvDatabaseFilePath
    private String ipSecurityCsvDatabaseFilePath
    private String ipGeolocationMMDBDatabaseFilePath
    private String ipSecurityMMDBDatabaseFilePath

    @PostConstruct
    private void init() {
        final String osName = System.getProperty("os.name").toLowerCase()

        if (osName.contains("win")) {
            placeCsvDatabaseFilePath = String.format("%s\\db-place.csv.gz", homeDir)
            countryCsvDatabaseFilePath = String.format("%s\\db-country.csv.gz", homeDir)
            ipGeolocationCsvDatabaseFilePath = String.format("%s\\db-ip-geolocation.csv.gz", homeDir)
            cloudProviderCsvDatabaseFilePath = String.format("%s\\db-cloud-provider.csv.gz", homeDir)
            ipSecurityCsvDatabaseFilePath = String.format("%s\\db-ip-security.csv.gz", homeDir)
            ipGeolocationMMDBDatabaseFilePath = String.format("%s\\db-ip-geolocation.mmdb", homeDir)
            ipSecurityMMDBDatabaseFilePath = String.format("%s\\db-ip-security.mmdb", homeDir)
        } else {
            placeCsvDatabaseFilePath = String.format("%s/db-place.csv.gz", homeDir)
            countryCsvDatabaseFilePath = String.format("%s/db-country.csv.gz", homeDir)
            ipGeolocationCsvDatabaseFilePath = String.format("%s/db-ip-geolocation.csv.gz", homeDir)
            cloudProviderCsvDatabaseFilePath = String.format("%s/db-cloud-provider.csv.gz", homeDir)
            ipSecurityCsvDatabaseFilePath = String.format("%s/db-ip-security.csv.gz", homeDir)
            ipGeolocationMMDBDatabaseFilePath = String.format("%s/db-ip-geolocation.mmdb", homeDir)
            ipSecurityMMDBDatabaseFilePath = String.format("%s/db-ip-security.mmdb", homeDir)
        }
    }

    String getPlaceCsvDatabaseFilePath() {
        return placeCsvDatabaseFilePath
    }

    String getCountryCsvDatabaseFilePath() {
        return countryCsvDatabaseFilePath
    }

    String getIPGeolocationCsvDatabaseFilePath() {
        return ipGeolocationCsvDatabaseFilePath
    }

    String getCloudProviderCsvDatabaseFilePath() {
        return cloudProviderCsvDatabaseFilePath
    }

    String getIPSecurityCsvDatabaseFilePath() {
        return ipSecurityCsvDatabaseFilePath
    }

    String getIPGeolocationMMDBDatabaseFilePath() {
        return ipGeolocationMMDBDatabaseFilePath
    }

    String getIPSecurityMMDBDatabaseFilePath() {
        return ipSecurityMMDBDatabaseFilePath
    }
}
