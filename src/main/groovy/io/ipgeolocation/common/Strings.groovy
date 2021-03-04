package io.ipgeolocation.common

import groovy.transform.CompileStatic

import static com.google.common.base.Strings.isNullOrEmpty

@CompileStatic
class Strings {

    static void checkNotEmptyOrNull(String s) {
        if (isNullOrEmpty(s)) {
            throw new NullPointerException("Pre-condition violated: provided string must not be empty or null.")
        }
    }

    static void checkNotEmptyOrNull(String s, String errorMessage) {
        if (isNullOrEmpty(s)) {
            throw new NullPointerException(errorMessage)
        }
    }
}
