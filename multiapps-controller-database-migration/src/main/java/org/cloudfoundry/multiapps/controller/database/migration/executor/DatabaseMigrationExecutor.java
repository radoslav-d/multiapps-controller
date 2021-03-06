package org.cloudfoundry.multiapps.controller.database.migration.executor;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.cloudfoundry.multiapps.controller.database.migration.client.DatabaseQueryClient;
import org.cloudfoundry.multiapps.controller.persistence.util.SqlQueryExecutor;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DatabaseMigrationExecutor {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public abstract DataSource getSourceDataSource();

    public abstract DataSource getTargetDataSource();

    public void executeMigration(String entityName) {
        try {
            executeMigrationInternal(entityName);
        } catch (SQLException e) {
            logger.error("Error migrating entity \"{}\": {}", entityName, e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected abstract void executeMigrationInternal(String entityName) throws SQLException;

    protected SqlQueryExecutor getSqlQueryExecutor(DataSource dataSource) {
        return new SqlQueryExecutor(dataSource);
    }

    @Value.Default
    protected DatabaseQueryClient getSourceDatabaseQueryClient() {
        return new DatabaseQueryClient(getSqlQueryExecutor(getSourceDataSource()));
    }

    @Value.Default
    protected DatabaseQueryClient getTargetDatabaseQueryClient() {
        return new DatabaseQueryClient(getSqlQueryExecutor(getTargetDataSource()));
    }
}
