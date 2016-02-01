package org.novapeng.jpax;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * the configuration keys
 *
 * Created by pengchangguo on 15/11/30.
 */
public class Config {

    /******************************************************************************************************************/
    /** app config
     /******************************************************************************************************************/

    /* the point expression for jpa aspect, example : execution(* com.ctc.zhengxin..services.*.*(..)) ,
       Used to open / close the db connection, and submit / rollback transaction  */
    public static final String JPA_AOP_POINT = "jpa.aop.point";

    /* the package name for jpa models */
    public static final String JPA_DB_MODEL_PACKAGE = "jpa.db.modelPackage";


    /******************************************************************************************************************/
    /** db config
    /******************************************************************************************************************/

    /* HibernateJpaVendorAdapter.database, value is : MYSQL ORACLE ..., See : org.springframework.orm.jpa.vendor.Database */
    @SuppressWarnings("unused")
    public static final String JPA_DB = "jpa.db";

    /* HibernateJpaVendorAdapter.showSql, value is : true / false */
    @SuppressWarnings("unused")
    public static final String JPA_DB_SHOWSQL = "jpa.db.showSql";

    /* HibernateJpaVendorAdapter.generateDdl, value is : true / false */
    @SuppressWarnings("unused")
    public static final String JPA_DB_GENERATEDDL = "jpa.db.generateDdl";

    /* HibernateJpaVendorAdapter.databasePlatform, value is : org.hibernate.dialect.., example : org.hibernate.dialect.MySQL5Dialect */
    @SuppressWarnings("unused")
    public static final String JPA_DB_DATABASEPLATFORM = "jpa.db.databasePlatform";

    /* jdbc driver class */
    @SuppressWarnings("unused")
    public static final String JPA_DB_DRIVER = "jpa.db.driver";

    /* jdbc db url */
    @SuppressWarnings("unused")
    public static final String JPA_DB_URL = "jpa.db.url";

    /* jdbc db username */
    @SuppressWarnings("unused")
    public static final String JPA_DB_USERNAME = "jpa.db.username";

    /* jdbc db password */
    @SuppressWarnings("unused")
    public static final String JPA_DB_PASSWORD = "jpa.db.password";

    /* jdbc connection pool initialSize */
    @SuppressWarnings("unused")
    public static final String JPA_DB_POOL_INITIALSIZE = "jpa.db.pool.initialSize";

    /* jdbc connection pool maxActive */
    @SuppressWarnings("unused")
    public static final String JPA_DB_POOL_MAXACTIVE = "jpa.db.pool.maxActive";

    /* jdbc connection pool maxIdle */
    @SuppressWarnings("unused")
    public static final String JPA_DB_POOL_MAXIDLE = "jpa.db.pool.maxIdle";

    /* jdbc connection pool minIdle */
    @SuppressWarnings("unused")
    public static final String JPA_DB_POOL_MINIDLE = "jpa.db.pool.minIdle";

    /* jdbc connection pool maxWait */
    @SuppressWarnings("unused")
    public static final String JPA_DB_POOL_MAXWAIT = "jpa.db.pool.maxWait";

    /* jdbc connection pool validationQuery */
    @SuppressWarnings("unused")
    public static final String JPA_DB_POOL_VALIDATIONQUERY = "jpa.db.pool.validationQuery";


    private static Properties properties;


    public static Properties getProperties() {
        return properties;
    }

    @SuppressWarnings("unused")
    public static String getProperty(String key) {
        return getProperty(key, null);
    }

    public static String getProperty(String key, String defaultValue) {
        loadProperties();
        String value = properties.getProperty(key);
        if (value == null) value = defaultValue;
        if (value.startsWith("${") && value.endsWith("}")) {
            String refKey = value.substring(2, value.length() - 1);
            return properties.getProperty(refKey);
        }
        return value;
    }

    private static void loadProperties() {
        if (properties == null) {
            synchronized(JPAX.class) {
                properties = new Properties();
                try {
                    InputStream inputStream = Config.class.getResourceAsStream("/application.properties");
                    if (inputStream == null) return;
                    properties.load(inputStream);
                } catch (IOException e) {
                    //do nothing
                }
            }
        }
    }

}


