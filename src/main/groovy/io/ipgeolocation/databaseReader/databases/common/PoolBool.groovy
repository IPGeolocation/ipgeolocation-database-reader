package io.ipgeolocation.databaseReader.databases.common

import org.supercsv.cellprocessor.CellProcessorAdaptor
import org.supercsv.cellprocessor.ift.BoolCellProcessor
import org.supercsv.exception.SuperCsvCellProcessorException
import org.supercsv.util.CsvContext

class PoolBool extends CellProcessorAdaptor implements BoolCellProcessor {

    @Override
    Object execute(Object value, CsvContext context) {
        if (!(value instanceof String)) {
            throw new SuperCsvCellProcessorException(String.class, value, context, this)
        }

        Boolean result = Boolean.FALSE

        if (value == "true") {
            result = Boolean.TRUE
        }

        this.next.execute(result, context)
    }
}
