package io.ipgeolocation.databaseReader.databases.ipsecurity

import groovy.transform.CompileStatic
import io.ipgeolocation.databaseReader.databases.common.Pool
import io.ipgeolocation.databaseReader.databases.common.PoolBool
import io.ipgeolocation.databaseReader.databases.common.PoolInteger
import io.ipgeolocation.databaseReader.databases.common.PoolString
import org.springframework.util.Assert
import org.supercsv.cellprocessor.Optional
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
class DBIPSecurityLoader {
    private final String IP_ADDRESS = "ip_address"
    private final String THREAT_SCORE = "threat_score"
    private final String IS_TOR = "is_tor"
    private final String IS_PROXY = "is_proxy"
    private final String PROXY_TYPE = "proxy_type"
    private final String IS_ANONYMOUS = "is_anonymous"
    private final String IS_KNOWN_ATTACKER = "is_known_attacker"
    private final String IS_BOT = "is_bot"
    private final String IS_SPAM = "is_spam"
    private final String[] CSV_COLUMNS = [
            IP_ADDRESS,
            THREAT_SCORE,
            IS_TOR,
            IS_PROXY,
            PROXY_TYPE,
            IS_ANONYMOUS,
            IS_KNOWN_ATTACKER,
            IS_BOT,
            IS_SPAM
    ]
    private final CellProcessor[] cellProcessors
    private final Pool pool = Pool.getInstance()

    DBIPSecurityLoader() {
        CellProcessor integer = new PoolInteger(pool)
        CellProcessor string = new PoolString(pool)
        CellProcessor optionalString = new Optional(new PoolString(pool))
        CellProcessor optionalBool = new Optional(new PoolBool())

        cellProcessors = [
                string, // ip_address
                integer, // threat_score
                optionalBool, // is_tor
                optionalBool, // is_proxy
                optionalString, // proxy_type
                optionalBool, // is_anonymous
                optionalBool, // is_known_attacker
                optionalBool, // is_bot
                optionalBool // is_spam
        ]

        Assert.state(cellProcessors.length == CSV_COLUMNS.length, "Programmer error: length of columns does not match length of cell processors.")
    }

    void load(String ipSecurityCsvFilePath, IPSecurityIndexer ipSecurityIndexer) {
        Assert.hasText(ipSecurityCsvFilePath, "'ipSecurityCsvFilePath' must not be empty or null.")
        Assert.notNull(ipSecurityIndexer, "'ipSecurityIndexer' must not be null.")

        Path ipSecurityCsvPath = Paths.get(ipSecurityCsvFilePath)

        Assert.state(Files.isRegularFile(ipSecurityCsvPath) && Files.exists(ipSecurityCsvPath), "$ipSecurityCsvFilePath file is missing.")

        try {
            InputStream fis = Files.newInputStream(ipSecurityCsvPath, StandardOpenOption.READ)
            InputStream gis = new GZIPInputStream(fis)
            Reader inputStreamReader = new InputStreamReader(gis, StandardCharsets.UTF_8)
            CsvMapReader reader = new CsvMapReader(inputStreamReader, CsvPreference.STANDARD_PREFERENCE)
            Map<String, Object> record

            reader.getHeader(Boolean.TRUE)

            while (!isNull(record = reader.read(CSV_COLUMNS, cellProcessors))) {
                ipSecurityIndexer.add(
                        new IPSecurity(record.get(IP_ADDRESS) as String, record.get(THREAT_SCORE) as Integer,
                                record.get(IS_PROXY) as String, record.get(PROXY_TYPE) as String,
                                record.get(IS_TOR) as String, record.get(IS_ANONYMOUS) as String,
                                record.get(IS_KNOWN_ATTACKER) as String, record.get(IS_BOT) as String,
                                record.get(IS_SPAM) as String))
            }

            inputStreamReader.close()
            gis.close()
            fis.close()
        } catch (IOException | NullPointerException e) {
            e.printStackTrace()
        }
    }
}
