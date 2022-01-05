package io.ipgeolocation.databaseReader.databases.ipsecurity

import groovy.transform.CompileStatic
import io.ipgeolocation.databaseReader.databases.common.Pool
import io.ipgeolocation.databaseReader.databases.common.PoolBool
import io.ipgeolocation.databaseReader.databases.common.PoolInteger
import io.ipgeolocation.databaseReader.databases.common.PoolString
import org.supercsv.cellprocessor.Optional
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

        this.cellProcessors = [
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

        if (cellProcessors.length != CSV_COLUMNS.length) {
            throw new Exception("Programmer error: length of columns does not match length of cell processors.")
        }
    }

    void load(String databasePath, IPSecurityIndexer indexer) {
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

    private void parse(Reader inputStreamReader, IPSecurityIndexer securityIndexer) {
        try {
            Map<String, Object> record
            CsvMapReader reader = new CsvMapReader(inputStreamReader, STANDARD_PREFERENCE)

            reader.getHeader(Boolean.TRUE)

            while ((record = reader.read(CSV_COLUMNS, cellProcessors)) != null) {
                securityIndexer.add(new IPSecurity(ipAddress: record.get(IP_ADDRESS) as String, threatScore: record.get(THREAT_SCORE) as Integer, isTor: record.get(IS_TOR) as Boolean, isProxy: record.get(IS_PROXY) as Boolean, proxyType: record.get(PROXY_TYPE) as String, isAnonymous: record.get(IS_ANONYMOUS) as Boolean, isKnownAttacker: record.get(IS_KNOWN_ATTACKER) as Boolean, isBot: record.get(IS_BOT) as Boolean, isSpam: record.get(IS_SPAM) as Boolean))
            }
        } catch (IOException e) {
            e.printStackTrace()
        }
    }
}
