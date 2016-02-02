package org.novapeng.jpax;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.PersistenceException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * JPA's
 *
 * Created by pengchangguo on 15/10/23.
 */
public class JPAX {

    /* is test mode */
    public static boolean isTestMode = false;

    /* is auto transaction, default is true */
    public static boolean autoTxs = true;

    /**
     *
     * 初始化前，需要enhance model, 自动查找com.ctc.zhengxin 包下的 @Entity 类
     *
     * 由 JPAXEnhancer(这个类继承了spring的ApplicationContextInitializer, 在spring装载bean之前触发) 负责调用
     *
     */
    public static void beforeInit() {
        Set<Class> jpaClasses = new HashSet<Class>();

        String jpaModelPackage = Config.getProperty(Config.JPA_DB_MODEL_PACKAGE, null);
        if (jpaModelPackage != null && !"".equals(jpaModelPackage)) {
            jpaClasses.addAll(ClassUtil.getClasses(jpaModelPackage));
        }

        for (Object key : Config.getProperties().keySet()) {
            if (!key.toString().contains("." + Config.JPA_DB_MODEL_PACKAGE)) continue;
            String otherModelPackage = Config.getProperty(key.toString());
            if (otherModelPackage == null || "".equals(otherModelPackage)) continue;
            Set<Class> tmpSet = ClassUtil.getClasses(otherModelPackage);
            if (tmpSet == null) continue;
            for (Class cls : tmpSet) {
                boolean already = false;
                for (Class cls1 : jpaClasses) {
                    if (cls1.getName().equals(cls.getName())) {
                        already = true;
                    }
                }
                if (!already) {
                    jpaClasses.add(cls);
                }
            }
        }

        for (Class cls : jpaClasses) {
            try {
                new JPAEnhancer().enhanceThisClass(cls);
            } catch (Exception e) {
                throw new JPAException("enhance jpa entity : " + cls + " error!", e);
            }
        }
    }

    /**
     * init~ thread safe!
     *
     * inject the entityManagerFactory
     */
    public static void init() {

        /* initialize DataSource */
        DB.buildDataSource();

        /* create EntityManagerFactory */
        JPA.buildEntityManagerFactory();

        /* new instance JPQL */
        if (JPQL.instance == null) {
            if (JPQL.instance != null)  return;
            JPQL.instance = new JPQL();
        }
    }

    /**
     * before execute
     */
    public static void beforeInvocation() {
        startTx(false);
    }

    /**
     * after execute
     */
    public static void afterInvocation() {
        closeTx(isTestMode);
    }

    /**
     * catch exception
     */
    public static void onException() {
        closeTx(true);
    }

    /**
     * finally
     */
    public static void finalInvocation() {
        closeTx(isTestMode);
    }


    private static void startTx(boolean readonly) {
        if (!JPA.isEnabled()) {
            return;
        }
        EntityManager manager = JPA.entityManagerFactory.createEntityManager();
        manager.setFlushMode(FlushModeType.COMMIT);
        manager.setProperty("org.hibernate.readOnly", readonly);
        if (autoTxs) {
            manager.getTransaction().begin();
        }

        Map<String, EntityManager> managerMap = new HashMap<String, EntityManager>();
        if (JPA.entityManagerFactories != null) {
            for (String dataBaseName : JPA.entityManagerFactories.keySet()) {
                EntityManager entityManager = JPA.entityManagerFactories.get(dataBaseName).createEntityManager();
                if (entityManager == null) continue;
                entityManager.setFlushMode(FlushModeType.COMMIT);
                entityManager.setProperty("org.hibernate.readOnly", readonly);
                if (autoTxs) {
                    entityManager.getTransaction().begin();
                }
                managerMap.put(dataBaseName, entityManager);
            }
        }
        JPA.createContext(manager, managerMap, readonly);
    }

    private static void closeTx(boolean rollback) {
        if (!JPA.isEnabled() || JPA.local.get() == null) {
            return;
        }
        EntityManager manager = JPA.get().entityManager;

        Map<String, EntityManager> managerMap = JPA.get().entityManagers;
        try {
            if (autoTxs) {
                closeEntityManager(manager, rollback);
                for (String dataBaseName : managerMap.keySet()) {
                    closeEntityManager(managerMap.get(dataBaseName), rollback);
                }
            }
        } finally {
            manager.close();
            JPA.clearContext();
        }
    }

    private static void closeEntityManager(EntityManager manager, boolean rollback) {
        // Be sure to set the connection is non-autoCommit mode as some driver will complain about COMMIT statement
        try {
            /* get connection from spring */
            //SessionFactory sessionFactory = ((org.hibernate.jpa.internal.EntityManagerImpl) JPA.em()).getSession().getSessionFactory();
            //Connection connection = SessionFactoryUtils.getDataSource(sessionFactory).getConnection();

            /* get connection from hibernate */
            Connection connection = DB.getConnectionByEm(manager);
            //Connection connection = manager.unwrap(Connection.class);
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new JPAException("Cannot set auto commit false ", e);
        }
        // Commit the transaction
        if (manager.getTransaction().isActive()) {
            if (JPA.get().readonly || rollback || manager.getTransaction().getRollbackOnly()) {
                manager.getTransaction().rollback();
            } else {
                try {
                    if (autoTxs) {
                        manager.getTransaction().commit();
                    }
                } catch (Throwable e) {
                    for (int i = 0; i < 10; i++) {
                        if (e instanceof PersistenceException && e.getCause() != null) {
                            e = e.getCause();
                            break;
                        }
                        e = e.getCause();
                        if (e == null) {
                            break;
                        }
                    }
                    throw new JPAException("Cannot commit", e);
                }
            }
        }
    }
}
