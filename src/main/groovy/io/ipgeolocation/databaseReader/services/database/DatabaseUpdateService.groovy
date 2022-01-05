package io.ipgeolocation.databaseReader.services.database

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.ipgeolocation.common.HttpRequests
import io.ipgeolocation.databaseReader.IpgeolocationDatabaseReaderApplication
import io.ipgeolocation.databaseReader.databases.common.IPGeolocationDatabase
import io.ipgeolocation.databaseReader.jobs.FetchUpdatedDatabaseJob
import kong.unirest.HttpResponse
import kong.unirest.JsonNode
import kong.unirest.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.scheduling.support.CronTrigger
import org.springframework.stereotype.Service
import org.springframework.util.Assert

import javax.annotation.PostConstruct
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

import static java.util.Objects.isNull

@CompileStatic
@Slf4j
@Service
class DatabaseUpdateService {
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @Value('${application.path.homeDir}')
    private String homeDir

    @Value('${application.path.databases.ConfigFile}')
    private String jsonConfigFilePath

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler

    // subscription parameters -- linked to database-config.json
    private String apiKey = null
    private String database = null
    private String updateInterval = null
    private String databaseType = null
    private Boolean autoFetchAndUpdateDatabase = null
    private String lastUpdateDate = null

    @PostConstruct
    private void init() {
        updateSubscriptionParametersFromDatabaseCofigFile()

        if (autoFetchAndUpdateDatabase) {
            taskScheduler.schedule(new FetchUpdatedDatabaseJob(this), new CronTrigger("0 0 * * * *"))
        }
    }

    void fetchAndLoadDatabaseIfUpdated() {
        updateSubscriptionParametersFromDatabaseCofigFile()

        Assert.hasText(lastUpdateDate, "Provided database configuration is not valid: {\"apiKey\": \"${apiKey}\", \"database\": \"${databaseVersion}\", \"updateInterval\": \"${updateInterval}\", \"databaseType\": \"${databaseType}\", \"autoFetchAndUpdateDatabase\": ${autoFetchAndUpdateDatabase}, \"lastUpdateDate\": ${lastUpdateDate}}")

        String lastUpdateDateFromDatabaseStatus = getLastUpdateDateFromDatabaseStatus()

        LocalDateTime parsedLastUpdateDate = LocalDateTime.parse(lastUpdateDate, dateTimeFormatter)
        LocalDateTime parsedLastUpdateDateFromDatabaseStatus = LocalDateTime.parse(lastUpdateDateFromDatabaseStatus, dateTimeFormatter)

        if (parsedLastUpdateDate != parsedLastUpdateDateFromDatabaseStatus) {
            downloadLatestDatabase()
            updateLastUpdateDateInDatabaseConfigJson(lastUpdateDateFromDatabaseStatus)
            IpgeolocationDatabaseReaderApplication.restart()
        }
    }

    void updateSubscriptionParametersFromDatabaseCofigFile() {
        File jsonConfigFile = new File(jsonConfigFilePath)

        Assert.state(jsonConfigFile.isFile() && jsonConfigFile.exists(), "Couldn't find the database configuration at ${jsonConfigFilePath} path.")

        JSONObject databaseConfigJson = null

        try {
            FileReader databaseConfigFileReader = new FileReader(jsonConfigFilePath)
            String line = databaseConfigFileReader.readLine()

            if (line != null) {
                databaseConfigJson = new JSONObject(line)
            }

            databaseConfigFileReader.close()
        } catch (e) {
            e.printStackTrace()
        }

        Assert.notNull(databaseConfigJson, "No database configuration found at ${jsonConfigFilePath} path.")
        Assert.isTrue(databaseConfigJson.has("apiKey") && databaseConfigJson.has("database") &&
                databaseConfigJson.has("updateInterval") && databaseConfigJson.has("databaseType") &&
                databaseConfigJson.has("autoFetchAndUpdateDatabase"),
                "Some of the required database configuration(s) is missing at ${jsonConfigFilePath} path.")

        apiKey = databaseConfigJson.getString("apiKey")
        database = databaseConfigJson.getString("database")
        updateInterval = databaseConfigJson.getString("updateInterval")
        databaseType = databaseConfigJson.getString("databaseType")
        autoFetchAndUpdateDatabase = databaseConfigJson.getBoolean("autoFetchAndUpdateDatabase")
        lastUpdateDate = databaseConfigJson.getString("lastUpdateDate")

        Assert.state(!apiKey || !database || !updateInterval || !databaseType, "Invalid database configuration: " +
                "{\"apiKey\": \"${apiKey}\", \"database\": \"${databaseVersion}\", \"updateInterval\": \"${updateInterval}\", " +
                "\"databaseType\": \"${databaseType}\", \"autoFetchAndUpdateDatabase\": ${autoFetchAndUpdateDatabase}}")
        Assert.state(database in IPGeolocationDatabase.ALL_DATABASES, "'database' must be equal to 'DB-I', 'DB-II', " +
                "'DB-III', 'DB-IV', 'DB-V', 'DB-VI' or 'DB-VII'.")
        Assert.state(updateInterval in ["week", "month"], "'updateInterval' must be equal to 'week' or 'month'.")
        Assert.state(databaseType in ["csv", "mmdb"], "'databaseType' must be equal to 'csv' or 'mmdb'.")
    }

    private String getLastUpdateDateFromDatabaseStatus() {
        String lastUpdateDate = this.lastUpdateDate
        HttpResponse<JsonNode> httpResponse = HttpRequests.get("https://database.ipgeolocation.io/status")

        if (httpResponse?.status == 200) {
            JSONObject jsonResponse = httpResponse.getBody().getObject()
            String databaseName = IPGeolocationDatabase.getDatabaseName(database)

            Assert.hasText(databaseName, "'databaseName' must not be empty or null.")

            JSONObject databaseUpdatedDates = jsonResponse.getJSONObject(databaseName)

            if (updateInterval == "week") {
                lastUpdateDate = databaseUpdatedDates.getString("lastWeeklyUpdate")
            } else if (updateInterval == "month") {
                lastUpdateDate = databaseUpdatedDates.getString("lastMonthlyUpdate")
            }
        }

        lastUpdateDate
    }

    void downloadLatestDatabase() {
        log.info("Downloading latest ${databaseVersion} (${updateInterval == "day" ? "dai" : updateInterval}ly) database.")

        try {
            File latestDatabaseFile = HttpRequests.getFile(IPGeolocationDatabase.getDatabaseUri(database),
                    "$homeDir/${UUID.randomUUID()}.zip", ["apiKey": apiKey as Object])
            ZipInputStream zis = new ZipInputStream(new FileInputStream(latestDatabaseFile))
            File destDir = new File(homeDir)
            byte[] buffer = new byte[1024]
            ZipEntry zipEntry

            while (!isNull(zipEntry = zis.getNextEntry())) {
                File newFile = newFile(destDir, zipEntry)

                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory: " + newFile)
                    }
                } else {
                    File parent = newFile.getParentFile()

                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory: " + parent)
                    }

                    FileOutputStream fos = new FileOutputStream(newFile)
                    int len

                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len)
                    }

                    fos.close()
                }
            }

            zis.close()
            latestDatabaseFile.delete()
        } catch (e) {
            e.printStackTrace()
        }
    }

    void updateLastUpdateDateInDatabaseConfigJson(String lastUpdateDate) {
        Assert.hasText(lastUpdateDate, "'lastUpdateDate' must not be empty or null.")

        try {
            // updating lastUpdateDate in database-config.json
            FileWriter jsonConfigFile = new FileWriter(jsonConfigFilePath)
            JSONObject configJson = new JSONObject()

            configJson.append("apiKey", apiKey)
            configJson.append("database", database)
            configJson.append("updateInterval", updateInterval)
            configJson.append("databaseType", databaseType)
            configJson.append("autoFetchAndUpdateDatabase", autoFetchAndUpdateDatabase)
            configJson.append("lastUpdateDate", lastUpdateDate)

            jsonConfigFile.write(configJson.toString())
            jsonConfigFile.close()

            // updating local value
            this.lastUpdateDate = lastUpdateDate
        } catch (e) {
            e.printStackTrace()
        }
    }

    private static final File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destinationFile = new File(destinationDir, zipEntry.getName())
        String destDirPath = destinationDir.getCanonicalPath()
        String destFilePath = destinationFile.getCanonicalPath()

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target directory: " + zipEntry.getName())
        }

        destinationFile
    }

    final String getDatabaseVersion() {
        return database
    }

    final String getDatabaseType() {
        return databaseType
    }
}
