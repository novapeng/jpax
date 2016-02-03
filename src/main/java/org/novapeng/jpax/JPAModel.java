package org.novapeng.jpax;

import javax.persistence.EntityTransaction;
import javax.persistence.MappedSuperclass;
import javax.persistence.Query;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A super class for JPA entities
 *
 * Created by pengchangguo on 15/10/23.
 */
@SuppressWarnings({"UnusedParameters", "unused"})
@MappedSuperclass
public class JPAModel extends JPABase {

    /**
     * store (ie insert) the entity.
     */
    public <T extends JPABase> T save() {
        _save();
        //noinspection unchecked
        return (T) this;
    }

    /**
     * store (ie insert) the entity.
     */
    public boolean create() {
        if (!em().contains(this)) {
            _save();
            return true;
        }
        return false;
    }

    /**
     * Refresh the entity state.
     */
    public <T extends JPABase> T refresh() {
        em().refresh(this);
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Merge this object to obtain a managed entity (usefull when the object comes from the Cache).
     */
    public <T extends JPABase> T merge() {
        //noinspection unchecked
        return (T) em().merge(this);
    }

    /**
     * Delete the entity.
     * @return The deleted entity.
     */
    public <T extends JPABase> T delete() {
        _delete();
        //noinspection unchecked
        return (T) this;
    }

    /**
     * Count entities
     * @return number of entities of this class
     */
    public static Long count() {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * Count entities with a special query.
     * Example : Long moderatedPosts = Post.count("moderated", true);
     * @param query HQL query or shortcut
     * @param params Params to bind to the query
     * @return A long
     */
    @SuppressWarnings("UnusedParameters")
    public static Long count(String query, Object... params) {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * Find all entities of this type
     */
    @SuppressWarnings("unused")
    public static <T extends JPABase> List<T> findAll() {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * Find the entity with the corresponding id.
     * @param id The entity id
     * @return The entity
     */
    public static <T extends JPABase> T findById(@SuppressWarnings("UnusedParameters") Object id) {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * Prepare a query to find entities.
     * @param query HQL query or shortcut
     * @param params Params to bind to the query
     * @return A JPAQuery
     */
    @SuppressWarnings("UnusedParameters")
    public static JPAQuery find(String query, Object... params) {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * Prepare a query to find *all* entities.
     * @return A JPAQuery
     */
    public static JPAQuery all() {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * Batch delete of entities
     * @param query HQL query or shortcut
     * @param params Params to bind to the query
     * @return Number of entities deleted
     */
    @SuppressWarnings("UnusedParameters")
    public static int delete(String query, Object... params) {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * Delete all entities
     * @return Number of entities deleted
     */
    @SuppressWarnings("unused")
    public static int deleteAll() {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * execute by native sql
     *
     * @param sql sql
     * @param clazz element type in result
     * @param params sql params
     * @param <T> element type
     * @return  result collection
     */
    public static <T> List<T> sql(String sql, Class<T> clazz, Object... params) {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * execute by native sql
     *
     * @param sql sql
     * @param params sql params
     * @return result collection, element is Map
     */
    public static List<Map> sql(String sql, Object... params) {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }


    /**
     * query first record by native sql
     *
     * @param sql sql
     * @param clazz element type in result
     * @param params sql params
     * @param <T> element type
     * @return first record
     */
    public static <T> T sqlFirst(String sql, Class<T> clazz, Object... params) {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * query first record by native sql
     *
     * @param sql sql
     * @param params sql params
     * @return first record
     */
    public static Map<String, Object> sqlFirst(String sql, Object... params) {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * get transaction
     *
     * @return transaction
     */
    public static EntityTransaction getTransaction() {
        throw new UnsupportedOperationException("Please annotate your JPA model with @javax.persistence.Entity annotation.");
    }

    /**
     * A JPAQuery
     */
    @SuppressWarnings("unchecked")
    public static class JPAQuery {

        public Query query;
        public String sq;

        public JPAQuery(String sq, Query query) {
            this.query = query;
            this.sq = sq;
        }

        @SuppressWarnings("unused")
        public JPAQuery(Query query) {
            this.query = query;
            this.sq = query.toString();
        }

        @SuppressWarnings("unchecked")
        public <T> T first() {
            try {
                List<T> results = query.setMaxResults(1).getResultList();
                if (results.isEmpty()) {
                    return null;
                }
                return results.get(0);
            } catch (Exception e) {
                throw new JPAQueryException("Error while executing query <strong>" + sq + "</strong>", JPAQueryException.findBestCause(e));
            }
        }

        /**
         * Bind a JPQL named parameter to the current query.
         */
        public JPAQuery bind(String name, Object param) {
            if (param.getClass().isArray()) {
                param = Arrays.asList((Object[]) param);
            }
            if (param instanceof Integer) {
                param = ((Integer) param).longValue();
            }
            query.setParameter(name, param);
            return this;
        }

        /**
         * Retrieve all results of the query
         * @return A list of entities
         */
        public <T> List<T> fetch() {
            try {
                return query.getResultList();
            } catch (Exception e) {
                throw new JPAQueryException("Error while executing query <strong>" + sq + "</strong>", JPAQueryException.findBestCause(e));
            }
        }

        /**
         * Retrieve results of the query
         * @param max Max results to fetch
         * @return A list of entities
         */
        @SuppressWarnings("unused")
        public <T> List<T> fetch(int max) {
            try {
                query.setMaxResults(max);
                return query.getResultList();
            } catch (Exception e) {
                throw new JPAQueryException("Error while executing query <strong>" + sq + "</strong>", JPAQueryException.findBestCause(e));
            }
        }

        /**
         * Set the position to start
         * @param position Position of the first element
         * @return A new query
         */
        @SuppressWarnings("unused")
        public <T> JPAQuery from(int position) {
            query.setFirstResult(position);
            return this;
        }

        /**
         * Retrieve a page of result
         * @param page Page number (start at 1)
         * @param length (page length)
         * @return a list of entities
         */
        public <T> List<T> fetch(int page, int length) {
            if (page < 1) {
                page = 1;
            }
            query.setFirstResult((page - 1) * length);
            query.setMaxResults(length);
            try {
                return query.getResultList();
            } catch (Exception e) {
                throw new JPAQueryException("Error while executing query <strong>" + sq + "</strong>", JPAQueryException.findBestCause(e));
            }
        }
    }

}
