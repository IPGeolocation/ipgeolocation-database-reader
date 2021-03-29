package io.ipgeolocation.databaseReader.jobs

import io.ipgeolocation.databaseReader.services.database.DatabaseUpdateService

class FetchUpdatedDatabaseJob implements Runnable {
    private DatabaseUpdateService databaseUpdateService

    FetchUpdatedDatabaseJob(DatabaseUpdateService databaseUpdateService) {
        this.databaseUpdateService = databaseUpdateService
    }

    @Override
    void run() {
        databaseUpdateService.fetchAndLoadDatabaseIfUpdated()
    }
}
