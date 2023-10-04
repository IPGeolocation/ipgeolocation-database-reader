package io.ipgeolocation.databaseReader.services.database

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.ipgeolocation.common.HttpRequests
import io.ipgeolocation.databaseReader.databases.common.DatabaseType
import io.ipgeolocation.databaseReader.databases.common.DatabaseVersion
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
    private final ThreadPoolTaskScheduler taskScheduler

    @Value('${ipgeolocation.database.workingDirectory}')
    private String workingDirectory

    @Value('${ipgeolocation.database.apiKey}')
    private String apiKey

    @Value('${ipgeolocation.database.version}')
    private String version

    @Value('${ipgeolocation.database.updateInterval}')
    private String updateInterval

    @Value('${ipgeolocation.database.type:mmdb}')
    private String type

    @Value('${ipgeolocation.database.autoFetchAndUpdate:true}')
    private Boolean autoFetchAndUpdateDatabase

    private LocalDateTime lastFetched = LocalDateTime.of(1970, 1, 1, 0, 0, 0)

    private final DateTimeFormatter DEFAULT_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @Autowired
    DatabaseUpdateService(ThreadPoolTaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler
    }

    @PostConstruct
    private void init() {
        Assert.state(workingDirectory && apiKey && version && updateInterval && type, "Invalid database config (JSON): {\"workingDirectory\": \"$workingDirectory\", \"apiKey\": \"$apiKey\", \"version\": \"$version\", \"updateInterval\": \"$updateInterval\", \"type\": \"$type\", \"autoFetchAndUpdateDatabase\": $autoFetchAndUpdateDatabase}")
        Assert.state(version in DatabaseVersion.values(), "'ipgeolocation.database.version' must be equal to 'DB-I', 'DB-II', 'DB-III', 'DB-IV', 'DB-V', 'DB-VI' or 'DB-VII'.")
        Assert.state(updateInterval in ["week", "month"], "'ipgeolocation.database.updateInterval' must be equal to 'week' or 'month'.")
        Assert.state(type in DatabaseType.values(), "'ipgeolocation.database.type' must be equal to '${DatabaseType.CSV}' or '${DatabaseType.MMDB}'.")

        log.info("Provided database config (JSON): {\"workingDirectory\": \"$workingDirectory\", \"apiKey\": \"$apiKey\", \"version\": \"$version\", \"updateInterval\": \"$updateInterval\", \"type\": \"$type\", \"autoFetchAndUpdateDatabase\": $autoFetchAndUpdateDatabase}")

        if (autoFetchAndUpdateDatabase) {
            taskScheduler.schedule(new FetchUpdatedDatabaseJob(this), new CronTrigger("0 0,30 * * * *"))
        }
    }

    boolean downloadLatestDatabaseIfUpdated() {
        String lastUpdateDateFromDatabaseAPIStatus = getLastUpdateDateFromDatabaseStatus()
        LocalDateTime parsedLastUpdateDateFromDatabaseStatus = LocalDateTime.parse(lastUpdateDateFromDatabaseAPIStatus, DEFAULT_DATE_TIME_FORMAT)
        boolean updated = lastFetched != parsedLastUpdateDateFromDatabaseStatus

        if (updated) {
            // updating cached lastFetched with latest value from database.ipgeolocation.io/status
            lastFetched = parsedLastUpdateDateFromDatabaseStatus

            downloadDatabaseFromDatabaseDownloadAPI()
        }

        updated
    }

    private String getLastUpdateDateFromDatabaseStatus() {
        String lastUpdateDate = this.lastFetched
        HttpResponse<JsonNode> httpResponse = HttpRequests.getAndJsonResponse("https://database.ipgeolocation.io/status")

        if (httpResponse?.status == 200) {
            JSONObject jsonResponse = httpResponse.getBody().getObject()
            String databaseName = DatabaseVersion.getDatabaseName(version)

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
                    DatabaseVersion.getDatabaseUri(version), "${workingDirectory}/${UUID.randomUUID()}.zip",
                    ["apiKey": apiKey as Object])

            if (downloadDatabaseFileResponse?.status == 200) {
                File downloadedDatabaseFile = downloadDatabaseFileResponse.getBody()
                ZipInputStream zis = new ZipInputStream(new FileInputStream(downloadedDatabaseFile))
                File destDir = new File(workingDirectory)
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
        return version
    }

    final String getDatabaseType() {
        return type
    }
}
