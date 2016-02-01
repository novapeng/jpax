package org.novapeng.jpax;

import org.apache.log4j.Level;
import org.hibernate.EmptyInterceptor;
import org.hibernate.ejb.Ejb3Configuration;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.io.Serializable;
import java.util.*;

/**
 * JPA Support
 *
 * Created by pengchangguo on 15/10/23.
 */
public class JPA {

    /* entityManagerFactory for simple database  */
    static EntityManagerFactory entityManagerFactory = null;

    /* entityManagerFactory for multi database  */
    static Map<String, EntityManagerFactory> entityManagerFactories = null;

    /* jpa context */
    static ThreadLocal<JPA> local = new ThreadLocal<JPA>();

    /* jpa entity manager for simple database */
    EntityManager entityManager;

    /* jpa entity manager for multi database */
    Map<String, EntityManager> entityManagers;

    //Vector<EntityManager> entityManagers = new Vector<EntityManager>();

    /* transaction  isReadonly */
    boolean readonly = true;

    /* transaction  isAutoCommit */
    @SuppressWarnings("unused")
    boolean autoCommit = false;

    static JPA get() {
        if (local.get() == null) {
            throw new JPAException("The JPA context is not initialized. JPA Entity Manager automatically start when one or more classes annotated with the @javax.persistence.Entity annotation are found in the application.");
        }
        return local.get();
    }

    static void clearContext() {
        local.remove();
    }

    @SuppressWarnings("unused")
    static void createContext(EntityManager entityManager, boolean readonly) {
        createContext(entityManager, new HashMap<String, EntityManager>(), readonly);
    }

    static void createContext(EntityManager entityManager, Map<String, EntityManager> entityManagers, boolean readonly) {
        if (local.get() == null) {
            JPA context = new JPA();
            context.entityManager = entityManager;
            context.readonly = readonly;
            context.entityManagers = entityManagers;
            local.set(context);
        } else {
            local.get().entityManager = entityManager;
            local.get().entityManagers = entityManagers;
        }
    }

    @SuppressWarnings("unused")
    private void close() {
        entityManager.close();
    }


    // ~~~~~~~~~~~
    /*
     * Retrieve the current entityManager
     */
    public static EntityManager em() {
        return em(null);
    }

    /*
     * Retrieve the current entityManager
     */
    public static EntityManager em(String dataBaseName) {
        if (dataBaseName == null || "".equals(dataBaseName.trim())) return get().entityManager;
        return get().entityManagers.get(dataBaseName);
    }

    /*
     * Tell to JPA do not commit the current transaction
     */
    @SuppressWarnings("unused")
    public static void setRollbackOnly() {
        em().getTransaction().setRollbackOnly();
    }

    /**
     * @return true if an entityManagerFactory has started
     */
    public static boolean isEnabled() {
        return entityManagerFactory != null;
    }

    /**
     * Execute a JPQL query
     */
    @SuppressWarnings("unused")
    public static int execute(String query) {
        return em().createQuery(query).executeUpdate();
    }

    /**
     * Execute a JPQL query
     */
    @SuppressWarnings("unused")
    public static int execute(String dataBaseName, String query) {
        return em(dataBaseName).createQuery(query).executeUpdate();
    }

    /*
     * Build a new entityManager.
     * (In most case you want to use the local entityManager with em)
     */
    @SuppressWarnings("unused")
    public static EntityManager newEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

    @SuppressWarnings("unused")
    public static EntityManager newEntityManager(String dataBaseName) {
        if (dataBaseName == null) return entityManagerFactory.createEntityManager();
        return entityManagerFactories.get(dataBaseName).createEntityManager();
    }

    /**
     * @return true if current thread is running inside a transaction
     */
    @SuppressWarnings("unused")
    public static boolean isInsideTransaction() {
        try {
            EntityManager manager = JPA.get().entityManager;
            EntityTransaction transaction = manager.getTransaction();
            return transaction != null;
        } catch (JPAException e) {
            return false;
        }
    }

    synchronized static void buildEntityManagerFactory() {
        if (entityManagerFactory == null) {
            entityManagerFactory = buildHibernateEntityManagerFactory(null);
        }

        if (entityManagerFactories == null) {
            Set<String> dbs = new HashSet<String>();
            for (Object key : Config.getProperties().keySet()) {
                if (!key.toString().contains(".jpa.db.url")) continue;
                if (key.toString().startsWith(".jpa.db.url")) continue;
                dbs.add(key.toString().split("\\.")[0]);
            }
            entityManagerFactories = new HashMap<String, EntityManagerFactory>();
            for (String dataBaseName : dbs) {
                entityManagerFactories.put(dataBaseName, buildHibernateEntityManagerFactory(dataBaseName));
            }
        }
    }

    private static EntityManagerFactory buildHibernateEntityManagerFactory(final String dataBaseName) {
        //noinspection deprecation
        Ejb3Configuration cfg = new Ejb3Configuration();

        if (dataBaseName == null) {
            cfg.setDataSource(DB.dataSource);
        } else {
            cfg.setDataSource(DB.dataSourceMap.get(dataBaseName));
        }

        if ("true".equalsIgnoreCase(p(dataBaseName, Config.JPA_DB_GENERATEDDL))) {
            cfg.setProperty("hibernate.hbm2ddl.auto", "update");
        }

        String dialect = p(dataBaseName, Config.JPA_DB_DATABASEPLATFORM);
        if (dialect == null || "".equals(dialect.trim())) {
            dialect = getDefaultDialect(p(dataBaseName, Config.JPA_DB_DRIVER));
        }
        cfg.setProperty("hibernate.dialect", dialect);
        cfg.setProperty("javax.persistence.transaction", "RESOURCE_LOCAL");


        cfg.setInterceptor(new EmptyInterceptor(){

            @Override
            public int[] findDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, org.hibernate.type.Type[] types) {
                if (entity instanceof JPABase && !((JPABase) entity).willBeSaved) {
                    return new int[0];
                }
                return super.findDirty(entity, id, currentState, previousState, propertyNames, types);
            }

        });

        if ("true".equalsIgnoreCase(p(dataBaseName, Config.JPA_DB_SHOWSQL))) {
            org.apache.log4j.Logger.getLogger("org.hibernate.SQL").setLevel(Level.ALL);
        } else {
            org.apache.log4j.Logger.getLogger("org.hibernate.SQL").setLevel(Level.OFF);
        }
        // inject additional  hibernate.* settings declared in Play! configuration
        if (dataBaseName == null) {
            cfg.addProperties((Properties) Utils.Maps.filterMap(Config.getProperties(), "^\\.hibernate\\..*"));
        } else {
            Properties hibernateProperties = new Properties();
            for (Object key : Config.getProperties().keySet()) {
                if (!key.toString().contains(dataBaseName + ".hibernate.")) continue;
                hibernateProperties.put(key.toString().replace(dataBaseName + ".", ""), Config.getProperty(key.toString()));
            }
            if (hibernateProperties.size() > 0) {
                cfg.addProperties(hibernateProperties);
            }
        }

        String jpaModelPackage = p(dataBaseName, Config.JPA_DB_MODEL_PACKAGE);
        Set<Class> classes = ClassUtil.getClasses(jpaModelPackage);

        for (Class<?> clazz : classes) {
            if (!clazz.isAnnotationPresent(Entity.class)) continue;
            if (dataBaseName == null) {
                if (clazz.getAnnotation(DataBase.class) != null) continue;
            } else {
                if (clazz.getAnnotation(DataBase.class) == null) continue;
            }
            cfg.addAnnotatedClass(clazz);
        }
        cfg.addPackage(jpaModelPackage);
        return cfg.buildEntityManagerFactory();
    }

    private static String p(String dataBaseName, String key) {
        if (dataBaseName == null) {
            return Config.getProperty(key);
        }
        return Config.getProperty(dataBaseName + "." + key);
    }

    private static String getDefaultDialect(String driver) {
        if (driver.equals("org.h2.Driver")) {
            return "org.hibernate.dialect.H2Dialect";
        } else if (driver.equals("org.hsqldb.jdbcDriver")) {
            return "org.hibernate.dialect.HSQLDialect";
        } else if (driver.equals("com.mysql.jdbc.Driver")) {
            return "org.hibernate.dialect.MySQL5Dialect";
        } else if (driver.equals("org.postgresql.Driver")) {
            return "org.hibernate.dialect.PostgreSQLDialect";
        } else if (driver.toLowerCase().equals("com.ibm.db2.jdbc.app.DB2Driver")) {
            return "org.hibernate.dialect.DB2Dialect";
        } else if (driver.equals("com.ibm.as400.access.AS400JDBCDriver")) {
            return "org.hibernate.dialect.DB2400Dialect";
        } else if (driver.equals("com.ibm.as400.access.AS390JDBCDriver")) {
            return "org.hibernate.dialect.DB2390Dialect";
        } else if (driver.equals("oracle.jdbc.driver.OracleDriver")) {
            return "org.hibernate.dialect.OracleDialect";
        } else if (driver.equals("com.sybase.jdbc2.jdbc.SybDriver")) {
            return "org.hibernate.dialect.SybaseAnywhereDialect";
        } else if ("com.microsoft.jdbc.sqlserver.SQLServerDriver".equals(driver)) {
            return "org.hibernate.dialect.SQLServerDialect";
        } else if ("com.sap.dbtech.jdbc.DriverSapDB".equals(driver)) {
            return "org.hibernate.dialect.SAPDBDialect";
        } else if ("com.informix.jdbc.IfxDriver".equals(driver)) {
            return "org.hibernate.dialect.InformixDialect";
        } else if ("com.ingres.jdbc.IngresDriver".equals(driver)) {
            return "org.hibernate.dialect.IngresDialect";
        } else if ("progress.sql.jdbc.JdbcProgressDriver".equals(driver)) {
            return "org.hibernate.dialect.ProgressDialect";
        } else if ("com.mckoi.JDBCDriver".equals(driver)) {
            return "org.hibernate.dialect.MckoiDialect";
        } else if ("InterBase.interclient.Driver".equals(driver)) {
            return "org.hibernate.dialect.InterbaseDialect";
        } else if ("com.pointbase.jdbc.jdbcUniversalDriver".equals(driver)) {
            return "org.hibernate.dialect.PointbaseDialect";
        } else if ("com.frontbase.jdbc.FBJDriver".equals(driver)) {
            return "org.hibernate.dialect.FrontbaseDialect";
        } else if ("org.firebirdsql.jdbc.FBDriver".equals(driver)) {
            return "org.hibernate.dialect.FirebirdDialect";
        } else {
            throw new UnsupportedOperationException("I do not know which hibernate dialect to use with "
                    + driver + " and I cannot guess it, use the property jpa.dialect in config file");
        }
    }

}
