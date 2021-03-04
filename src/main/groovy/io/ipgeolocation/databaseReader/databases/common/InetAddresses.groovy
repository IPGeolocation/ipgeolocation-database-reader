package io.ipgeolocation.databaseReader.databases.common

import groovy.transform.CompileStatic

import javax.servlet.http.HttpServletRequest

import static com.google.common.base.Preconditions.checkNotNull

@CompileStatic
class InetAddresses {

    static String getRequestIP(HttpServletRequest request) {
        checkNotNull(request, "Pre-condition violated: HTTP Servlet Request must not be null.")

        String ip = request.getHeader("X-Forwarded-For")

        if (!ip || ip == "unknown") {
            ip = request.getHeader("CF-Connecting-IP")
        }

        if (ip) {
            String[] ips = ip.split(",")
            ip = ips[0].trim()
        }

        if (!ip || ip == "unknown") {
            ip = request.remoteAddr
        }

        ip
    }
}
