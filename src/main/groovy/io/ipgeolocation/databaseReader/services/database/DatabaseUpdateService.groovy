package io.ipgeolocation.databaseReader.services.database

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.ipgeolocation.common.HttpRequests
import io.ipgeolocation.databaseReader.databases.common.DatabaseType
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
@Service
@Slf4j
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
            taskScheduler.schedule(new FetchUpdatedDatabaseJob(this), new CronTrigger("0 0,30 * * * *"))
        }
    }

    boolean downloadLatestDatabaseIfUpdated() {
        // updateSubscriptionParametersFromDatabaseCofigFile()

        String lastUpdateDateFromDatabaseAPIStatus = getLastUpdateDateFromDatabaseStatus()
        LocalDateTime parsedLastUpdateDate = LocalDateTime.parse(lastUpdateDate, dateTimeFormatter)
        LocalDateTime parsedLastUpdateDateFromDatabaseStatus = LocalDateTime.parse(lastUpdateDateFromDatabaseAPIStatus, dateTimeFormatter)
        boolean updated = (parsedLastUpdateDate != parsedLastUpdateDateFromDatabaseStatus)

        if (updated) {
            // updating cached lastUpdateDate with latest value from database.ipgeolocation.io/status
            lastUpdateDate = lastUpdateDateFromDatabaseAPIStatus

            downloadDatabaseFromDatabaseDownloadAPI()
            updateLastUpdateDateInDatabaseConfigFile()
        }

        updated
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
        lastUpdateDate = databaseConfigJson.optString("lastUpdateDate", "1970-01-01 00:00:00")

        Assert.state(apiKey && database && updateInterval && databaseType, "Invalid database configuration: {\"apiKey\": \"${apiKey}\", \"database\": \"${databaseVersion}\", \"updateInterval\": \"${updateInterval}\", \"databaseType\": \"${databaseType}\", \"autoFetchAndUpdateDatabase\": ${autoFetchAndUpdateDatabase}}")
        Assert.state(database in IPGeolocationDatabase.values(), "'database' must be equal to 'DB-I', 'DB-II', 'DB-III', 'DB-IV', 'DB-V', 'DB-VI' or 'DB-VII'.")
        Assert.state(updateInterval in ["week", "month"], "'updateInterval' must be equal to 'week' or 'month'.")
        Assert.state(databaseType in DatabaseType.values(), "'databaseType' must be equal to '${DatabaseType.CSV}' or '${DatabaseType.MMDB}'.")

        log.info("Database config (JSON) is: {\"apiKey\": \"$apiKey\", \"database\": \"$databaseVersion\", " +
                "\"updateInterval\": \"$updateInterval\", \"databaseType\": \"$databaseType\", " +
                "\"autoFetchAndUpdateDatabase\": $autoFetchAndUpdateDatabase, \"lastUpdateDate\": \"$lastUpdateDate\"}")
    }

    private String getLastUpdateDateFromDatabaseStatus() {
        String lastUpdateDate = this.lastUpdateDate
        HttpResponse<JsonNode> httpResponse = HttpRequests.getAndJsonResponse("https://database.ipgeolocation.io/status")

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

    private void downloadDatabaseFromDatabaseDownloadAPI() {
        log.info("Downloading latest ${databaseVersion} (${updateInterval == "day" ? "dai" : updateInterval}ly) database.")

        try {
            HttpResponse<File> downloadDatabaseFileResponse = HttpRequests.getAndFileResponse(
                    IPGeolocationDatabase.getDatabaseUri(database), "$homeDir/${UUID.randomUUID()}.zip",
                    ["apiKey": apiKey as Object])

            if (downloadDatabaseFileResponse?.status == 200) {
                File downloadedDatabaseFile = downloadDatabaseFileResponse.getBody()
                ZipInputStream zis = new ZipInputStream(new FileInputStream(downloadedDatabaseFile))
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
                downloadedDatabaseFile.delete()
            } else {
                log.error("Either your database subscription or the API key ($apiKey) is not valid. Please contact ipgeolocation.io support at support@ipgeolocation.io.")
            }
        } catch (e) {
            e.printStackTrace()
        }
    }

    private void updateLastUpdateDateInDatabaseConfigFile() {
        Assert.hasText(lastUpdateDate, "'lastUpdateDate' must not be empty or null.")

        try {
            // updating lastUpdateDate in database-config.json
            FileWriter jsonConfigFile = new FileWriter(jsonConfigFilePath)
            JSONObject configJson = new JSONObject()

            configJson.put("apiKey", apiKey)
            configJson.put("database", database)
            configJson.put("updateInterval", updateInterval)
            configJson.put("databaseType", databaseType)
            configJson.put("autoFetchAndUpdateDatabase", autoFetchAndUpdateDatabase)
            configJson.put("lastUpdateDate", lastUpdateDate)

            jsonConfigFile.write(configJson.toString())
            jsonConfigFile.close()
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
