package io.ipgeolocation.databaseReader.controllers

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.ipgeolocation.databaseReader.IpgeolocationDatabaseReaderApplication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping

@CompileStatic
@Slf4j
@Controller
class DatabaseController {

    @CrossOrigin("*")
    @PostMapping(path = "/database/update", produces = "application/json")
    def updateDatabase() {
        IpgeolocationDatabaseReaderApplication.restart()
    }
}
