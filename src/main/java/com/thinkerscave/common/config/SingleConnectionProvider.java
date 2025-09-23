//package com.thinkerscave.common.config;
//
//import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
//import org.hibernate.service.UnknownUnwrapTypeException;
//
//import java.sql.Connection;
//import java.sql.SQLException;
//
///**
// * SingleConnectionProvider is a basic implementation of Hibernate's ConnectionProvider.
// *
// * It wraps a single pre-established JDBC Connection instance. This class is typically used
// * in tests or custom scenarios where a fixed connection is manually managed outside of a
// * connection pool.
// */
//public class SingleConnectionProvider implements ConnectionProvider {
//
//    // Holds a pre-configured JDBC connection to be reused
//    private final Connection connection;
//
//    // Constructor initializes the provider with a fixed connection
//    public SingleConnectionProvider(Connection connection) {
//        this.connection = connection;
//    }
//
//    // Returns the stored JDBC connection whenever requested
//    @Override
//    public Connection getConnection() throws SQLException {
//        return this.connection;
//    }
//
//    // Intentionally does nothing since the connection is managed externally
//    @Override
//    public void closeConnection(Connection conn) throws SQLException {
//        // Do nothing - we're managing it manually
//    }
//
//    // Hibernate-specific flag: indicates that connections shouldn't be aggressively released
//    @Override
//    public boolean supportsAggressiveRelease() {
//        return false;
//    }
//
//    // Allows Hibernate to check if this can be unwrapped to a specific type
//    @Override
//    public boolean isUnwrappableAs(Class unwrapType) {
//        return ConnectionProvider.class.equals(unwrapType);
//    }
//
//    // Returns this instance if the requested type is ConnectionProvider, else throws an error
//    @Override
//    public <T> T unwrap(Class<T> unwrapType) {
//        if (isUnwrappableAs(unwrapType)) {
//            return (T) this;
//        } else {
//            throw new UnknownUnwrapTypeException(unwrapType);
//        }
//    }
//}
