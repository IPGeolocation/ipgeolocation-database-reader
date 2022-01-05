package io.ipgeolocation.databaseReader.databases.place

import groovy.transform.CompileStatic

import static com.google.common.base.Preconditions.checkNotNull

@CompileStatic
class PlaceIndexer {
    private Map<Integer, Place> places

    PlaceIndexer() {
        places = new LinkedHashMap<Integer, Place>()
    }

    void addAt(Place place, Integer index) {
        checkNotNull(place, "Pre-condition violated: place must not be null.")
        checkNotNull(index, "Pre-condition violated: index must not be null.")

        if (index < 1) {
            throw new IndexOutOfBoundsException("Pre-condition violated: index must be greater than 0.")
        }

        places.put(index, place)
    }

    Place findAt(Integer index) {
        checkNotNull(index, "Pre-condition violated: index must not be null.")

        if (index < 1) {
            throw new IndexOutOfBoundsException("Pre-condition violated: index must be greater than 0.")
        }

        places.get(index, null)
    }

    Integer size() {
        places.size()
    }
}
