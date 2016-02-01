package org.novapeng.jpax;

import org.apache.commons.dbcp.BasicDataSource;
import org.hibernate.Session;
import org.hibernate.internal.SessionImpl;

import javax.persistence.EntityManager;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * DataSource
 *
 * Created by pengchangguo on 16/1/28.
 */
public class DB {

    static DataSource dataSource;

    static Map<String, DataSource> dataSourceMap;

    synchronized static void buildDataSource() {
        if (dataSource == null) {
            dataSource = buildDbcpDataSource(null);
        }

        if (dataSourceMap == null) {
            dataSourceMap = new HashMap<String, DataSource>();
            Set<String> dbs = new HashSet<String>();
            for (Object key : Config.getProperties().keySet()) {
                if (!key.toString().contains(".jpa.db.url")) continue;
                if (key.toString().startsWith(".jpa.db.url")) continue;
                dbs.add(key.toString().split("\\.")[0]);
            }
            for (String dataBaseName : dbs) {
                dataSourceMap.put(dataBaseName, buildDbcpDataSource(dataBaseName));
            }
        }

    }

    private static DataSource buildDbcpDataSource(String dataBaseName) {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(p(dataBaseName, Config.JPA_DB_DRIVER));
        dataSource.setUrl(p(dataBaseName, Config.JPA_DB_URL));
        dataSource.setUsername(p(dataBaseName, Config.JPA_DB_USERNAME));
        dataSource.setPassword(p(dataBaseName, Config.JPA_DB_PASSWORD));
        dataSource.setInitialSize(Integer.parseInt(p(dataBaseName, Config.JPA_DB_POOL_INITIALSIZE)));
        dataSource.setMaxActive(Integer.parseInt(p(dataBaseName, Config.JPA_DB_POOL_MAXACTIVE)));
        dataSource.setMaxIdle(Integer.parseInt(p(dataBaseName, Config.JPA_DB_POOL_MAXIDLE)));
        dataSource.setMinIdle(Integer.parseInt(p(dataBaseName, Config.JPA_DB_POOL_MINIDLE)));
        dataSource.setMaxWait(Integer.parseInt(p(dataBaseName, Config.JPA_DB_POOL_MAXWAIT)));
        dataSource.setValidationQuery(p(dataBaseName, Config.JPA_DB_POOL_VALIDATIONQUERY));
        dataSource.setRemoveAbandoned(true);
        dataSource.setRemoveAbandonedTimeout(60);
        dataSource.setLogAbandoned(true);
        dataSource.setTestOnBorrow(true);
        dataSource.setTestWhileIdle(true);
        return dataSource;
    }

    private static String p(String dataBaseName, String key) {
        if (dataBaseName == null) {
            return Config.getProperty(key);
        }
        return Config.getProperty(dataBaseName + "." + key);
    }

    @SuppressWarnings("unused")
    public static Connection getConnection(String dataBaseName) {
        return getConnectionByEm(JPA.em(dataBaseName));
    }

    public static Connection getConnectionByEm(EntityManager manager) {
        SessionImpl session = (SessionImpl) manager.unwrap(Session.class);
        Connection connection = session.connection();
        try {
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new JPAException("get connection failed!", e);
        }
        return connection;
    }
}
