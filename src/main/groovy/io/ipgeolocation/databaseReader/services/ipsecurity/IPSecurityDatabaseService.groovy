package io.ipgeolocation.databaseReader.services.ipsecurity

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.ipgeolocation.databaseReader.databases.ipsecurity.DBIPSecurityLoader
import io.ipgeolocation.databaseReader.databases.ipsecurity.IPSecurity
import io.ipgeolocation.databaseReader.databases.ipsecurity.IPSecurityIndexer
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.zip.GZIPInputStream

@CompileStatic
@Service
@Slf4j
class IPSecurityDatabaseService {
    @Value('${application.path.databaseHomeDir}') String databaseHomeDir
    @Value('${application.path.ipSecurityDatabase}') String ipSecurityDatabasePath

    private final DBIPSecurityLoader dbIpSecurityLoader = new DBIPSecurityLoader()
    private final IPSecurityIndexer ipSecurityIndexer = new IPSecurityIndexer()

    void loadDatabase() {
        try {
            Path ipSecurityDatabaseFile = Paths.get(ipSecurityDatabasePath)

            if (Files.exists(ipSecurityDatabaseFile)) {
                InputStream inputStream = new GZIPInputStream(Files.newInputStream(ipSecurityDatabaseFile, StandardOpenOption.READ))

                log.info("Loading ip-securities from: ${ipSecurityDatabasePath}")

                dbIpSecurityLoader.load(ipSecurityIndexer, new InputStreamReader(inputStream, StandardCharsets.UTF_8))

                log.info("Loaded ${ipSecurityIndexer.size()} ip-securities successfully.")
            }
        } catch (e) {
            e.printStackTrace()
        }
    }

    IPSecurity findIPSecurity(String ipAddress) {
        ipSecurityIndexer.get(ipAddress)
    }
}
