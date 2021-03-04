package io.ipgeolocation.databaseReader.databases.common

import com.google.common.net.InetAddresses
import groovy.transform.CompileStatic
import org.supercsv.cellprocessor.CellProcessorAdaptor
import org.supercsv.cellprocessor.ift.StringCellProcessor
import org.supercsv.exception.SuperCsvCellProcessorException
import org.supercsv.util.CsvContext

@CompileStatic
class ParseInetAddress extends CellProcessorAdaptor implements StringCellProcessor {

    @Override
    Object execute(Object value, CsvContext context) {
        if (!(value instanceof String)) {
            throw new SuperCsvCellProcessorException(String.class, value, context, this)
        }

        InetAddress result = InetAddresses.forString(value as String)
        this.next.execute(result, context)
    }
}
