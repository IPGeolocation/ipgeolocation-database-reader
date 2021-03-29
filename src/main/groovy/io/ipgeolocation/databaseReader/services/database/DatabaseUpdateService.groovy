package io.ipgeolocation.databaseReader.services.database

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.ipgeolocation.common.HttpRequests
import io.ipgeolocation.databaseReader.IpgeolocationDatabaseReaderApplication
import io.ipgeolocation.databaseReader.databases.common.Database
import io.ipgeolocation.databaseReader.jobs.FetchUpdatedDatabaseJob
import kong.unirest.HttpResponse
import kong.unirest.JsonNode
import kong.unirest.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.scheduling.support.CronTrigger
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

import static com.google.common.base.Preconditions.checkNotNull
import static java.util.Objects.isNull
import static io.ipgeolocation.common.Strings.checkNotEmptyOrNull

@CompileStatic
@Slf4j
@Service
class DatabaseUpdateService {
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @Value('${application.path.databaseConfigFile}') private String databaseConfigFilePath
    @Value('${application.path.databaseHomeDir}') private String databaseHomePath

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler

    @PostConstruct
    private void init() {
        JSONObject databaseConfigJson = getValidDatabaseConfigJson()

        if (databaseConfigJson && databaseConfigJson.getBoolean("autoFetchAndUpdateDatabase")) {
            taskScheduler.schedule(new FetchUpdatedDatabaseJob(this), new CronTrigger("0 0 * * * *"))
        }
    }

    void fetchAndLoadDatabaseIfUpdated() {
        JSONObject databaseConfigJson = getValidDatabaseConfigJson()

        if (databaseConfigJson && databaseConfigJson.getString("apiKey") && databaseConfigJson.getString("database") && databaseConfigJson.getString("updateInterval") && databaseConfigJson.getString("lastDatabaseUpdateDate")) {
            String lastUpdateDateStr = getLastUpdateDate(databaseConfigJson.getString("database"), databaseConfigJson.getString("updateInterval"))

            if (lastUpdateDateStr) {
                LocalDateTime lastDatabaseUpdateDate = LocalDateTime.parse(databaseConfigJson.getString("lastDatabaseUpdateDate"), dateTimeFormatter)
                LocalDateTime lastUpdateDate = LocalDateTime.parse(lastUpdateDateStr, dateTimeFormatter)

                if (lastDatabaseUpdateDate != lastUpdateDate) {
                    downloadLatestDatabase(databaseConfigJson.getString("database"), databaseConfigJson.getString("apiKey"))
                    updateDatabaseUpdated(databaseConfigJson, lastUpdateDateStr)
                    IpgeolocationDatabaseReaderApplication.restart()
                }
            } else {
                throw new IllegalStateException("Unable to find the last database update date.")
            }
        } else {
            throw new IllegalStateException("Provided database configuration is not valid: {\"apiKey\": \"${databaseConfigJson.getString("apiKey")}\", \"database\": \"${databaseConfigJson.getString("database")}\", \"updateInterval\": \"${databaseConfigJson.getString("updateInterval")}\", \"lastDatabaseUpdateDate\": ${databaseConfigJson.getString("lastDatabaseUpdateDate")}}")
        }
    }

    JSONObject getValidDatabaseConfigJson() {
        JSONObject databaseConfigJson = null
        File databaseConfigFile = new File(databaseConfigFilePath)

        if (!databaseConfigFile.isFile() && !databaseConfigFile.exists()) {
            throw new IllegalStateException("Pre-condition violated: database configuration file doesn't exist.")
        }

        try {
            FileReader lastUpdateFileReader = new FileReader(databaseConfigFilePath)
            String line = lastUpdateFileReader.readLine()

            while (!isNull(line)) {
                databaseConfigJson = new JSONObject(line)
                line = lastUpdateFileReader.readLine()
            }

            lastUpdateFileReader.close()
        } catch (e) {
            e.printStackTrace()
        }

        if (!databaseConfigJson) {
            throw new IllegalStateException("Couldn't find the database configuration at ${databaseConfigFilePath}".toString())
        }

        if (!databaseConfigJson.getString("apiKey") || !databaseConfigJson.getString("database") || !databaseConfigJson.getString("updateInterval") || isNull(databaseConfigJson.getBoolean("autoFetchAndUpdateDatabase"))) {
            throw new IllegalStateException("Provided database configuration is not valid: {\"apiKey\": \"${databaseConfigJson.getString("apiKey")}\", \"database\": \"${databaseConfigJson.getString("database")}\", \"updateInterval\": \"${databaseConfigJson.getString("updateInterval")}\", \"autoFetchAndUpdateDatabase\": ${databaseConfigJson.getBoolean("autoFetchAndUpdateDatabase")}}")
        }

        databaseConfigJson
    }

    String getLastUpdateDate(String database, String updateInterval) {
        checkNotEmptyOrNull(database, "Pre-condition violated: database must not be empty or null.")
        checkNotEmptyOrNull(updateInterval, "Pre-condition violated: update interval must not be empty or null.")

        String lastUpdateDateStr = null
        HttpResponse<JsonNode> httpResponse = HttpRequests.get("https://database.ipgeolocation.io/status")

        if (httpResponse?.status == 200) {
            JSONObject jsonResponse = httpResponse.getBody().getObject()
            String databaseName = Database.getDatabaseName(database)

            if (databaseName) {
                JSONObject databaseUpdatedDates = jsonResponse.getJSONObject(databaseName)

                if (updateInterval == "week") {
                    lastUpdateDateStr = databaseUpdatedDates.getString("lastWeeklyUpdate")
                } else if (updateInterval == "month") {
                    lastUpdateDateStr = databaseUpdatedDates.getString("lastMonthlyUpdate")
                }
            }
        }

        lastUpdateDateStr
    }

    void downloadLatestDatabase(String database, String apiKey) {
        checkNotEmptyOrNull(database, "Pre-condition violated: database must not be empty or null.")
        checkNotEmptyOrNull(apiKey, "Pre-condition violated: API key must not be empty or null.")

        log.info("Downloading latest ${database} database.")

        try {
            File latestDatabaseFile = HttpRequests.getFile(Database.getDatabaseUri(database), "$databaseHomePath/${UUID.randomUUID()}.zip", ["apiKey": apiKey as Object])
            ZipInputStream zis = new ZipInputStream(new FileInputStream(latestDatabaseFile))
            File destDir = new File(databaseHomePath)
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

    void updateDatabaseUpdated(JSONObject databaseConfigJson, String lastUpdateDateStr) {
        checkNotNull(databaseConfigJson, "Pre-condition violated: database config must not be null.")
        checkNotEmptyOrNull(lastUpdateDateStr, "Pre-condition violated: last update date must not be empty or null.")

        try {
            FileWriter databaseConfigFile = new FileWriter(databaseConfigFilePath)

            databaseConfigJson.put("lastDatabaseUpdateDate", lastUpdateDateStr)
            databaseConfigFile.write(databaseConfigJson.toString())
            databaseConfigFile.close()
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
}
