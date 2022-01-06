package io.ipgeolocation.databaseReader.databases.place

import groovy.transform.CompileStatic
import io.ipgeolocation.databaseReader.databases.common.Pool
import io.ipgeolocation.databaseReader.databases.common.PoolInteger
import io.ipgeolocation.databaseReader.databases.common.PoolString
import org.springframework.util.Assert
import org.supercsv.cellprocessor.Optional
import org.supercsv.cellprocessor.ift.CellProcessor
import org.supercsv.io.CsvMapReader
import org.supercsv.prefs.CsvPreference

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.zip.GZIPInputStream

import static java.util.Objects.isNull

@CompileStatic
class DBPlaceLoader {
    private final String ID = "id"
    private final String NAME_EN = "name_en"
    private final String NAME_DE = "name_de"
    private final String NAME_RU = "name_ru"
    private final String NAME_JA = "name_ja"
    private final String NAME_FR = "name_fr"
    private final String NAME_ZH = "name_zh"
    private final String NAME_ES = "name_es"
    private final String NAME_CS = "name_cs"
    private final String NAME_IT = "name_it"
    private final String[] CSV_COLUMNS = [
            ID,
            NAME_EN,
            NAME_DE,
            NAME_RU,
            NAME_JA,
            NAME_FR,
            NAME_ZH,
            NAME_ES,
            NAME_CS,
            NAME_IT
    ]
    private final CellProcessor[] cellProcessors
    private final Pool pool = Pool.getInstance()

    DBPlaceLoader() {
        CellProcessor integer = new PoolInteger(pool)
        CellProcessor string = new PoolString(pool)
        CellProcessor optionalString = new Optional(new PoolString(pool))

        cellProcessors = [
                integer, // id
                string, // english
                optionalString, // german
                optionalString, // russian
                optionalString, // japanese
                optionalString, // french
                optionalString, // chinese
                optionalString, // spanish
                optionalString, // czech
                optionalString // italian
        ]

        Assert.state(cellProcessors.length == CSV_COLUMNS.length, "Programmer error: length of columns does not match length of cell processors.")
    }

    void load(String placeCsvFilePath, PlaceIndexer placeIndexer) {
        Assert.hasText(placeCsvFilePath, "'placeCsvFilePath' must not be empty or null.")
        Assert.notNull(placeIndexer, "'placeIndexer' must not be null.")

        Path placeCsvPath = Paths.get(placeCsvFilePath)

        Assert.state(Files.isRegularFile(placeCsvPath) && Files.exists(placeCsvPath), "$placeCsvFilePath is missing.")

        try {
            InputStream fis = Files.newInputStream(placeCsvPath, StandardOpenOption.READ)
            InputStream gis = new GZIPInputStream(fis)
            Reader inputStreamReader = new InputStreamReader(gis, StandardCharsets.UTF_8)
            CsvMapReader csvReader = new CsvMapReader(inputStreamReader, CsvPreference.STANDARD_PREFERENCE)
            Map<String, Object> record

            while (!isNull(record = csvReader.read(CSV_COLUMNS, cellProcessors))) {
                placeIndexer.addAt(
                        new Place(record.get(ID) as Integer, record.get(NAME_EN) as String,
                                record.get(NAME_DE) as String, record.get(NAME_RU) as String,
                                record.get(NAME_JA) as String, record.get(NAME_FR) as String,
                                record.get(NAME_ZH) as String, record.get(NAME_ES) as String,
                                record.get(NAME_CS) as String, record.get(NAME_IT) as String),
                        record.get(ID) as Integer)
            }

            inputStreamReader.close()
            gis.close()
            fis.close()
        } catch (IOException | NullPointerException e) {
            e.printStackTrace()
        }
    }
}
