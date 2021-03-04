package io.ipgeolocation.databaseReader.databases.country

import groovy.transform.CompileStatic

import static com.google.common.base.Preconditions.checkNotNull

@CompileStatic
class CountryIndexer {
    private Map<Integer, Country> countries

    CountryIndexer() {
        countries = new LinkedHashMap<Integer, Country>()
    }

    void addAt(Country country, Integer index) {
        checkNotNull(country, "Pre-condition violated: country must not be null.")
        checkNotNull(index, "Pre-condition violated: index must not be null.")

        if (index < 1) {
            throw new IndexOutOfBoundsException("Pre-condition violated: index must be greater than 0.")
        }

        countries.put(index, country)
    }

    Country getAt(Integer index) {
        checkNotNull(index, "Pre-condition violated: index must not be null.")

        if (index < 1) {
            throw new IndexOutOfBoundsException("Pre-condition violated: index must be greater than 0.")
        }

        countries.get(index, null)
    }

    Integer size() {
        countries.size()
    }
}
