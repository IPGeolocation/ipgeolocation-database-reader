package io.ipgeolocation.databaseReader.databases.common

import groovy.transform.CompileStatic

import static io.ipgeolocation.common.Strings.checkNotEmptyOrNull

@CompileStatic
class Preconditions {

    static Boolean isFile(String filePath) {
        checkNotEmptyOrNull(filePath, "Pre-condition violated: file path must not be empty or null.")

        new File(filePath).isFile()
    }
}
