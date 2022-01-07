package io.ipgeolocation.databaseReader.databases.ipgeolocation

import com.maxmind.db.MaxMindDbConstructor
import com.maxmind.db.MaxMindDbParameter
import groovy.transform.CompileStatic

@CompileStatic
class Currency {
    final String code
    final String name
    final String symbol

    @MaxMindDbConstructor
    Currency(
            @MaxMindDbParameter(name = "code") String code,
            @MaxMindDbParameter(name = "name") String name,
            @MaxMindDbParameter(name = "symbol") String symbol) {
        this.code = code
        this.symbol = symbol
        this.name = name
    }

    final Map<String, Object> asMap() {
        Map<String, Object> responseMap = [:]

        responseMap.put("code", code)
        responseMap.put("name", name)
        responseMap.put("symbol", symbol)

        responseMap
    }
}
