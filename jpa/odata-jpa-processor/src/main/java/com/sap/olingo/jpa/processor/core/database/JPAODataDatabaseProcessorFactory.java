package com.sap.olingo.jpa.processor.core.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sap.olingo.jpa.processor.core.api.JPAODataDatabaseProcessor;

public class JPAODataDatabaseProcessorFactory {
  private static final Log LOGGER = LogFactory.getLog(JPAODataDatabaseProcessorFactory.class);
  private static final String PRODUCT_NAME_H2 = "H2";
  private static final String PRODUCT_NAME_HSQLDB = "HSQL Database Engine";
  private static final String PRODUCT_NAME_POSTGRESQL = "PostgreSQL";

  public JPAODataDatabaseProcessor create(final DataSource dataSource) throws SQLException {
    if (dataSource != null) {
      try (Connection connection = dataSource.getConnection()) {
        final DatabaseMetaData dbMetadata = connection.getMetaData();
        if (dbMetadata.getDatabaseProductName().equals(PRODUCT_NAME_POSTGRESQL)) {
          LOGGER.trace("Create database-processor for PostgreSQL");
          return new JPAPostgresqlDatabaseProcessor();
        } else if (dbMetadata.getDatabaseProductName().equals(PRODUCT_NAME_HSQLDB)) {
          LOGGER.trace("Create database-processor for HSQLDB");
          return new JPA_HSQLDB_DatabaseProcessor();
        } else if (dbMetadata.getDatabaseProductName().equals(PRODUCT_NAME_H2)) {
          LOGGER.trace("Create database-processor for H2");
          return new JPA_HSQLDB_DatabaseProcessor();
        } else {
          LOGGER.trace("Create default database-processor");
          return new JPADefaultDatabaseProcessor();
        }
      }
    } else {
      LOGGER.trace("Create default database-processor");
      return new JPADefaultDatabaseProcessor();
    }
  }
}
