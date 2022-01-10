package io.ipgeolocation.common

import groovy.transform.CompileStatic
import kong.unirest.HttpResponse
import kong.unirest.JsonNode
import kong.unirest.Unirest
import kong.unirest.UnirestException

import static com.google.common.base.Preconditions.checkNotNull
import static Strings.checkNotEmptyOrNull

@CompileStatic
class HttpRequests {

    static HttpResponse<JsonNode> getAndJsonResponse(String uri, Map<String, Object> params = [:],
                                                     Map<String, String> headers = [:]) throws UnirestException {
        checkNotEmptyOrNull(uri, "Pre-condition violated: URI must not be empty or null.")
        checkNotNull(params, "Pre-condition violated: query parameters must not be null.")

        headers.put("accept", "application/json")

        Unirest.get(uri)
                .queryString(params)
                .headers(headers)
                .asJson()
    }

    static HttpResponse<File> getAndFileResponse(String uri, String path, Map<String, Object> params = [:],
                                                 Map<String, String> headers = [:]) throws UnirestException {
        checkNotEmptyOrNull(uri, "Pre-condition violated: URI must not be empty or null.")
        checkNotEmptyOrNull(path, "Pre-condition violated: path must not be empty or null.")
        checkNotNull(params, "Pre-condition violated: query parameters must not be null.")

        Unirest.get(uri)
                .queryString(params)
                .headers(headers)
                .asFile(path)
    }
}
