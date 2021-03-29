package io.ipgeolocation.databaseReader.controllers

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.ipgeolocation.databaseReader.services.database.DatabaseUpdateService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping

@CompileStatic
@Slf4j
@Controller
class DatabaseController {
    private final DatabaseUpdateService databaseUpdateService

    @Autowired
    DatabaseController(DatabaseUpdateService databaseUpdateService) {
        this.databaseUpdateService = databaseUpdateService
    }

    @PostMapping(path = "/database/update", produces = "application/jsob")
    def updateDatabase() {
        databaseUpdateService.fetchAndLoadDatabaseIfUpdated()
    }
}
