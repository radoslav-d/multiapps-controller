package org.cloudfoundry.multiapps.controller.database.migration.executor.type;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class LongDatabaseTypeSetter implements DatabaseTypeSetter {

    @Override
    public List<String> getSupportedTypes() {
        return Arrays.asList("int8");
    }

    @Override
    public void setType(int columnIndex, PreparedStatement insertStatement, Object value) throws SQLException {
        insertStatement.setLong(columnIndex, (Long) value);
    }

}
