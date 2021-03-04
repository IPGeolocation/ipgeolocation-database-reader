package io.ipgeolocation.databaseReader.databases.common

import groovy.transform.CompileStatic
import org.supercsv.cellprocessor.CellProcessorAdaptor
import org.supercsv.cellprocessor.ift.StringCellProcessor
import org.supercsv.exception.SuperCsvCellProcessorException
import org.supercsv.util.CsvContext

@CompileStatic
class PoolInteger extends CellProcessorAdaptor implements StringCellProcessor {
    private final Pool pool

    PoolInteger(Pool pool) {
        this.pool = pool
    }

    @Override
    Object execute(Object value, CsvContext context) {
        if (!(value instanceof String)) {
            throw new SuperCsvCellProcessorException(String.class, value, context, this)
        }

        Integer result = pool.poolInteger(new Integer(value as String))
        this.next.execute(result, context)
    }
}
