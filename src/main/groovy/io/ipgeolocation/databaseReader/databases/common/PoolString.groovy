package io.ipgeolocation.databaseReader.databases.common

import groovy.transform.CompileStatic

import org.supercsv.cellprocessor.CellProcessorAdaptor
import org.supercsv.cellprocessor.ift.StringCellProcessor
import org.supercsv.exception.SuperCsvCellProcessorException
import org.supercsv.util.CsvContext

@CompileStatic
class PoolString extends CellProcessorAdaptor implements StringCellProcessor {
    private final Pool pool

    PoolString(Pool pool) {
        this.pool = pool
    }

    @Override
    Object execute(Object value, CsvContext context) {
        if (!(value instanceof String)) {
            throw new SuperCsvCellProcessorException(String.class, value, context, this)
        }

        String result = pool.poolString(value as String)
        this.next.execute(result, context)
    }
}
