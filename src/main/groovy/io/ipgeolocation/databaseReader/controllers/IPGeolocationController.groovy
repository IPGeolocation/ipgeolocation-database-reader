package io.ipgeolocation.databaseReader.controllers

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.ipgeolocation.databaseReader.databases.common.InetAddresses
import io.ipgeolocation.databaseReader.services.ipgeolocation.IPGeolocationDatabaseService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

import javax.servlet.http.HttpServletRequest

import static com.google.common.base.Strings.isNullOrEmpty
import static com.google.common.base.Strings.nullToEmpty
import static java.util.Objects.isNull

@CompileStatic
@Slf4j
@RestController
class IPGeolocationController {
    private final IPGeolocationDatabaseService ipGeolocationDatabaseService

    IPGeolocationController(@Autowired IPGeolocationDatabaseService ipGeolocationDatabaseService) {
        this.ipGeolocationDatabaseService = ipGeolocationDatabaseService
    }

    @GetMapping(path = "/ipGeo")
    def singleLookup(@Autowired HttpServletRequest request, String ip, String fields, String excludes, String include, String lang) {
        Map<String, Object> responseMap = [:]

        if (responseMap.isEmpty()) {
            if (isNullOrEmpty(ip)) {
                ip = InetAddresses.getRequestIP(request)
            }

            if (isNullOrEmpty(fields)) {
                fields = "*"
            }

            if (isNullOrEmpty(lang)) {
                lang = "en"
            }

            responseMap = ipGeolocationDatabaseService.lookupIPGeolocation(ip, fields, nullToEmpty(excludes), nullToEmpty(include), lang, Boolean.FALSE)
        }

        if (log.isDebugEnabled()) {
            log.info("${request.getRemoteAddr()}  ${request.getRemoteHost()}  ${request.getRequestURL()}  ${responseMap.get("status") as HttpStatus}  ${responseMap.toMapString()}")
        }

        return ResponseEntity
                .status(responseMap.remove("status") as HttpStatus)
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseMap)
    }

    @PostMapping(path = "/ipGeoBulk")
    def bulkLookup(@Autowired HttpServletRequest request, String fields, String excludes, String include, String lang, @RequestBody IPList ipList) {
        ResponseEntity<?> responseEntity
        Map<String, Object> responseMap = [:]

        if (isNull(ipList)) {
            responseMap.put("status", HttpStatus.BAD_REQUEST)
            responseMap.put("message", "Provide a list of IP addresses to lookup bulk IP geolocations.")
        }

        if (responseMap.isEmpty()) {
            if (isNullOrEmpty(fields)) {
                fields = "*"
            }

            if (isNullOrEmpty(lang)) {
                lang = "en"
            }

            List<Map<String, Object>> responseMapArray = ipGeolocationDatabaseService.lookupIPGeolocationBulk(ipList.ips, fields,nullToEmpty(excludes), nullToEmpty(include), lang)

            if (log.isDebugEnabled()) {
                log.info("${ipList.ips}  ${request.getRemoteHost()}  ${request.getRequestURL()}  ${responseMap.get("status") as HttpStatus}  ${responseMapArray.toListString()}")
            }

            responseEntity = ResponseEntity
                    .status(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseMapArray)
        } else {
            if (log.isDebugEnabled()) {
                log.info("${request.getRemoteAddr()}  ${request.getRemoteHost()}  ${request.getRequestURL()}  ${responseMap.get("status") as HttpStatus}  ${responseMap.toMapString()}")
            }

            responseEntity = ResponseEntity
                    .status(responseMap.remove("status") as HttpStatus)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(responseMap)
        }

        responseEntity
    }
}
