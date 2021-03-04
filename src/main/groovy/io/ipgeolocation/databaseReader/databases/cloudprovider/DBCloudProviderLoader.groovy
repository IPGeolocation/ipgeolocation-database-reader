package io.ipgeolocation.databaseReader.databases.cloudprovider

import groovy.transform.CompileStatic
import io.ipgeolocation.databaseReader.databases.common.Pool
import io.ipgeolocation.databaseReader.databases.common.PoolString
import org.supercsv.cellprocessor.ift.CellProcessor
import org.supercsv.io.CsvMapReader

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.zip.GZIPInputStream

import static com.google.common.base.Preconditions.checkNotNull
import static org.supercsv.prefs.CsvPreference.STANDARD_PREFERENCE

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

        this.cellProcessors = [
                string  // cloud_provider
        ]

        if (cellProcessors.length != CSV_COLUMNS.length) {
            throw new Exception("Programmer error: length of columns does not match length of cell processors.")
        }
    }

    void load(String databasePath, CloudProviderIndexer indexer) {
        checkNotNull(databasePath, "Pre-condition violated: database path must not be null.")

        try {
            InputStream fis = Files.newInputStream(Paths.get(databasePath), StandardOpenOption.READ)
            InputStream gis = new GZIPInputStream(fis)
            Reader inputStreamReader = new InputStreamReader(gis, StandardCharsets.UTF_8)

            parse(inputStreamReader, indexer)

            inputStreamReader.close()
            gis.close()
            fis.close()
        } catch (Exception e) {
            e.printStackTrace()
        }
    }

    private void parse(Reader inputStreamReader, CloudProviderIndexer cloudProviderIndexer) {
        try {
            Map<String, Object> record
            CsvMapReader reader = new CsvMapReader(inputStreamReader, STANDARD_PREFERENCE)

            while ((record = reader.read(CSV_COLUMNS, cellProcessors)) != null) {
                cloudProviderIndexer.index(record.get(CLOUD_PROVIDER) as String)
            }
        } catch (IOException e) {
            e.printStackTrace()
        }
    }
}
