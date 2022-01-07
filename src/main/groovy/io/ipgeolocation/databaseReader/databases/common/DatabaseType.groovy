package io.ipgeolocation.databaseReader.databases.common

import groovy.transform.CompileStatic

@CompileStatic
class DatabaseType {
    static final String CSV = "csv", MMDB = "mmdb"

    static final List<String> values () {
        List.of(CSV, MMDB)
    }
}