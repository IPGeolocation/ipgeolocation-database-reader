package io.ipgeolocation.databaseReader.services.database

import groovy.transform.CompileStatic
import io.ipgeolocation.databaseReader.databases.country.Country
import io.ipgeolocation.databaseReader.databases.ipgeolocation.IPGeolocation
import io.ipgeolocation.databaseReader.databases.ipsecurity.IPSecurity
import io.ipgeolocation.databaseReader.databases.place.Place

@CompileStatic
interface DatabaseService {
    void loadDatabases()
    Place findPlace(Integer indexer)
    Country findCountry(Integer indexer)
    IPGeolocation findIPGeolocation(InetAddress inetAddress)
    IPSecurity findIPSecurity(InetAddress ipAddress)
    Boolean isCloudProvider(String name)
}
