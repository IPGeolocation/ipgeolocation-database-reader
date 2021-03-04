package io.ipgeolocation.databaseReader.databases.common

import groovy.transform.CompileStatic

@CompileStatic
@Singleton
class Pool {
    private final Map<String, String> stringPool = new HashMap<String, String>()
    private final Map<Integer, Integer> integerPool = new HashMap<Integer, Integer>()

    String poolString(String string) {
        String alreadyPooledInstance = stringPool.putIfAbsent(string, string)
        alreadyPooledInstance ?: string
    }

    Integer poolInteger(Integer integer) {
        Integer alreadyPooledInstance = integerPool.putIfAbsent(integer, integer)
        alreadyPooledInstance ?: integer
    }

    Integer size() {
        stringPool.size() + integerPool.size()
    }
}
