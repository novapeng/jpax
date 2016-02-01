package org.novapeng.jpax;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 *
 * the annotation for MultiDataBase
 *
 * Created by pengchangguo on 16/1/28.
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface DataBase {

    /**
     * the database aliasName,
     *
     * example :
     *
     * in application.properties :
     *
     *  A.jpa.db=MYSQL
     *  A.jpa.db.driver=...
     *
     *  "A" is dataBase aliasName
     *
     *  in Model :
     *
     *  @DataBase(name="A")
     *  public class Model extends JPAModel {
     *
     *  }
     *
     *
     * @return dataBase aliasName
     */
    String name();
}
