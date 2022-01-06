package io.ipgeolocation.databaseReader.databases.cloudprovider

import groovy.transform.CompileStatic
import io.ipgeolocation.databaseReader.databases.common.Pool
import io.ipgeolocation.databaseReader.databases.common.PoolString
import org.springframework.util.Assert
import org.supercsv.cellprocessor.ift.CellProcessor
import org.supercsv.io.CsvMapReader
import org.supercsv.prefs.CsvPreference

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.zip.GZIPInputStream

import static java.util.Objects.isNull

@CompileStatic
class DBCloudProviderLoader {
    private final String CLOUD_PROVIDER = "cloud_provider"
    private final String[] CSV_COLUMNS = [
            CLOUD_PROVIDER
    ]
    private final CellProcessor[] cellProcessors
    private final Pool pool = Pool.getInstance()

    DBCloudProviderLoader() {
        CellProcessor string = new PoolString(pool)

        cellProcessors = [
                string  // cloud_provider
        ]

        Assert.state(cellProcessors.length == CSV_COLUMNS.length, "Programmer error: length of columns does not match length of cell processors.")
    }

    void load(String cloudProviderCsvFilePath, CloudProviderIndexer cloudProviderIndexer) {
        Assert.hasText(cloudProviderCsvFilePath, "'cloudProviderCsvFilePath' must not be empty or null.")
        Assert.notNull(cloudProviderIndexer, "'cloudProviderIndexer' must not be null.")

        Path cloudProviderCsvPath = Paths.get(cloudProviderCsvFilePath)

        Assert.state(Files.isRegularFile(cloudProviderCsvPath) && Files.exists(cloudProviderCsvPath), "$cloudProviderCsvFilePath is missing.")

        try {
            InputStream fileInputStream = Files.newInputStream(cloudProviderCsvPath, StandardOpenOption.READ)
            InputStream gzipInputStream = new GZIPInputStream(fileInputStream)
            Reader inputStreamReader = new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8)
            CsvMapReader csvMapReader = new CsvMapReader(inputStreamReader, CsvPreference.STANDARD_PREFERENCE)
            Map<String, Object> record

            while (!isNull(record = csvMapReader.read(CSV_COLUMNS, cellProcessors))) {
                cloudProviderIndexer.index(record.get(CLOUD_PROVIDER) as String)
            }

            inputStreamReader.close()
            gzipInputStream.close()
            fileInputStream.close()
        } catch (Exception e) {
            e.printStackTrace()
        }
    }
}
