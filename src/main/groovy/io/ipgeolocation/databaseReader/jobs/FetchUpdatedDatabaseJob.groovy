package io.ipgeolocation.databaseReader.jobs

import io.ipgeolocation.databaseReader.IpgeolocationDatabaseReaderApplication
import io.ipgeolocation.databaseReader.services.database.DatabaseUpdateService

class FetchUpdatedDatabaseJob implements Runnable {
    private DatabaseUpdateService databaseUpdateService

    FetchUpdatedDatabaseJob(DatabaseUpdateService databaseUpdateService) {
        this.databaseUpdateService = databaseUpdateService
    }

    @Override
    void run() {
        if (databaseUpdateService.downloadLatestDatabaseIfUpdated()) {
            IpgeolocationDatabaseReaderApplication.restart()
        }
    }
}
