package org.novapeng.jpax;

import javax.persistence.EntityManager;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/**
 * sql tool
 *
 * Created by pengchangguo on 16/2/1.
 */
@SuppressWarnings({"unchecked", "unused", "ResultOfMethodCallIgnored"})
public class JPSQL {

    public static JPSQL instance = new JPSQL();

    public static <T> List<T> sql(String entity, String sql, Class<T> clazz, Object... params) {
        EntityManager entityManager = JPQL.instance.em(entity);
        Connection connection = DB.getConnectionByEm(entityManager);
        List<T> result = new ArrayList<T>();
        if (params != null && params.length > 0 && params[0] instanceof Map) {
            List<String> namedParams = NamedParameterUtils.parseSql(sql);
            Map<String, Object> namedParamMap = (Map<String, Object>) params[0];
            List<Object> namedParamsValue = new ArrayList<Object>();
            for (String param : namedParams) {
                if (sql.contains(":" + param)) {
                    sql.replaceFirst(":" + param, "?");
                    namedParamsValue.add(namedParamMap.get(param));
                }
            }
            params = namedParamsValue.toArray();
        }
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.prepareStatement(sql);
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    statement.setObject(i + 1, params[i]);
                }
            }
            if (!sql.trim().toLowerCase().startsWith("select")) {
                statement.executeUpdate();
                return null;
            }
            ResultSetMetaData resultSetMetaData = statement.getMetaData();
            resultSet = statement.executeQuery();
            for (;resultSet.next();) {
                int cols = resultSetMetaData.getColumnCount();
                if (clazz.equals(Map.class)) {
                    Map<String, Object> objectMap = new HashMap<String, Object>();
                    for (int i = 0; i < cols; i++) {
                        String columnName = resultSetMetaData.getColumnName(i + 1);
                        Object value = resultSet.getObject(i + 1);
                        objectMap.put(columnName, value);
                    }
                    result.add((T) objectMap);
                } else {
                    Object object;
                    try {
                        object = clazz.newInstance();
                    } catch (InstantiationException e) {
                        throw new JPAException("can not newInstance for Class : " + clazz.getName(), e);
                    } catch (IllegalAccessException e) {
                        throw new JPAException("can not newInstance for Class : " + clazz.getName(), e);
                    }
                    for (int i = 0; i < cols; i++) {
                        String columnName = resultSetMetaData.getColumnName(i + 1);
                        Object value = resultSet.getObject(i + 1);
                        Field[] fields = clazz.getDeclaredFields();
                        for (Field field : fields) {
                            if (columnName.equalsIgnoreCase(field.getName())) {
                                try {
                                    field.set(object, convertFieldValue(field, value));
                                } catch (IllegalAccessException e) {
                                    throw new JPAException("can not inject value : " + value + " into : " + field.getName(), e);
                                }
                            }
                        }
                        result.add((T) object);
                    }
                }

            }
        } catch (SQLException e) {
            throw new RuntimeException(String.format("execute sql failed! %s", sql), e);
        } finally {
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
                //if (connection != null) connection.close();
            } catch (SQLException e) {
                //noinspection ThrowFromFinallyBlock
                throw new RuntimeException("close connection failed!", e);
            }
        }
        return result;
    }

    public static <T> List<T> sql(String entity, String sql, Object... params) {
        return (List<T>) sql(entity, sql, Map.class, params);
    }

    public static <T> T sqlFirst(String entity, String sql, Class clazz, Object... params) {
        List list = sql(entity, sql, clazz, params);
        if (list == null || list.size() == 0) return null;
        return (T) list.get(0);
    }

    public static <T> T sqlFirst(String entity, String sql, Object... params) {
        List list = sql(entity, sql, Map.class, params);
        if (list == null || list.size() == 0) return null;
        return (T) list.get(0);
    }

    private static Object convertFieldValue(Field field, Object _value) {
        String value = null;
        if (_value != null) {
            value = _value.toString();
        }
        boolean nullOrEmpty = value == null || value.trim().length() == 0;
        Class clazz = field.getType();
        // raw String
        if (clazz.equals(String.class)) {
            return value;
        }

        // Handles the case where the model property is a sole character
        if (clazz.equals(Character.class)) {
            //noinspection ConstantConditions
            return value.charAt(0);
        }

        // Enums
        if (Enum.class.isAssignableFrom(clazz)) {
            return nullOrEmpty ? null : Enum.valueOf((Class<Enum>) clazz, value);
        }

        // int or Integer binding
        if (clazz.getName().equals("int") || clazz.equals(Integer.class)) {
            if (nullOrEmpty) {
                return clazz.isPrimitive() ? 0 : null;
            }

            return Integer.parseInt(value.contains(".") ? value.substring(0, value.indexOf(".")) : value);
        }

        // long or Long binding
        if (clazz.getName().equals("long") || clazz.equals(Long.class)) {
            if (nullOrEmpty) {
                return clazz.isPrimitive() ? 0l : null;
            }

            return Long.parseLong(value.contains(".") ? value.substring(0, value.indexOf(".")) : value);
        }

        // byte or Byte binding
        if (clazz.getName().equals("byte") || clazz.equals(Byte.class)) {
            if (nullOrEmpty) {
                return clazz.isPrimitive() ? (byte) 0 : null;
            }

            return Byte.parseByte(value.contains(".") ? value.substring(0, value.indexOf(".")) : value);
        }

        // short or Short binding
        if (clazz.getName().equals("short") || clazz.equals(Short.class)) {
            if (nullOrEmpty) {
                return clazz.isPrimitive() ? (short) 0 : null;
            }

            return Short.parseShort(value.contains(".") ? value.substring(0, value.indexOf(".")) : value);
        }

        // float or Float binding
        if (clazz.getName().equals("float") || clazz.equals(Float.class)) {
            if (nullOrEmpty) {
                return clazz.isPrimitive() ? 0f : null;
            }

            return Float.parseFloat(value);
        }

        // double or Double binding
        if (clazz.getName().equals("double") || clazz.equals(Double.class)) {
            if (nullOrEmpty) {
                return clazz.isPrimitive() ? 0d : null;
            }

            return Double.parseDouble(value);
        }

        // BigDecimal binding
        if (clazz.equals(BigDecimal.class)) {
            return nullOrEmpty ? null : new BigDecimal(value);
        }

        // boolean or Boolean binding
        if (clazz.getName().equals("boolean") || clazz.equals(Boolean.class)) {
            if (nullOrEmpty) {
                return clazz.isPrimitive() ? false : null;
            }

            return value.equals("1") || value.toLowerCase().equals("on") || value.toLowerCase().equals("yes") || Boolean.parseBoolean(value);

        }

        return null;
    }

    public static class NamedParameterUtils {

        public static List<String> parseSql(String originalSql) {
            List<String> params = new ArrayList<String>();
            Set<String> namedParameters = new HashSet<String>();

            char[] statement = originalSql.toCharArray();

            int i = 0;
            while (i < statement.length) {
                int skipToPosition = skipCommentsAndQuotes(statement, i);
                if (i != skipToPosition) {
                    if (skipToPosition >= statement.length) {
                        break;
                    }
                    i = skipToPosition;
                }
                char c = statement[i];
                if (c == ':' || c == '&') {
                    int j = i + 1;
                    if (j < statement.length && statement[j] == ':' && c == ':') {
                        // Postgres-style "::" casting operator - to be skipped.
                        i = i + 2;
                        continue;
                    }
                    while (j < statement.length && !isParameterSeparator(statement[j])) {
                        j++;
                    }
                    if (j - i > 1) {
                        String parameter = originalSql.substring(i + 1, j);
                        if (!namedParameters.contains(parameter)) {
                            namedParameters.add(parameter);
                        }
                        params.add(parameter);
                    }
                    i = j - 1;
                }
                else {
                    //noinspection StatementWithEmptyBody
                    if (c == '?') {
                        //do nothing
                    }
                }
                i++;
            }
            return params;
        }


        /**
         * Set of characters that qualify as parameter separators,
         * indicating that a parameter name in a SQL String has ended.
         */
        private static final char[] PARAMETER_SEPARATORS =
                new char[] {'"', '\'', ':', '&', ',', ';', '(', ')', '|', '=', '+', '-', '*', '%', '/', '\\', '<', '>', '^'};


        /**
         * Determine whether a parameter name ends at the current position,
         * that is, whether the given character qualifies as a separator.
         */
        private static boolean isParameterSeparator(char c) {
            if (Character.isWhitespace(c)) {
                return true;
            }
            for (char separator : PARAMETER_SEPARATORS) {
                if (c == separator) {
                    return true;
                }
            }
            return false;
        }



        /**
         * Set of characters that qualify as comment or quotes starting characters.
         */
        private static final String[] START_SKIP =
                new String[] {"'", "\"", "--", "/*"};

        /**
         * Set of characters that at are the corresponding comment or quotes ending characters.
         */
        private static final String[] STOP_SKIP =
                new String[] {"'", "\"", "\n", "*/"};


        /**
         * Skip over comments and quoted names present in an SQL statement
         * @param statement character array containing SQL statement
         * @param position current position of statement
         * @return next position to process after any comments or quotes are skipped
         */
        private static int skipCommentsAndQuotes(char[] statement, int position) {
            for (int i = 0; i < START_SKIP.length; i++) {
                if (statement[position] == START_SKIP[i].charAt(0)) {
                    boolean match = true;
                    for (int j = 1; j < START_SKIP[i].length(); j++) {
                        if (!(statement[position + j] == START_SKIP[i].charAt(j))) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        int offset = START_SKIP[i].length();
                        for (int m = position + offset; m < statement.length; m++) {
                            if (statement[m] == STOP_SKIP[i].charAt(0)) {
                                boolean endMatch = true;
                                int endPos = m;
                                for (int n = 1; n < STOP_SKIP[i].length(); n++) {
                                    if (m + n >= statement.length) {
                                        // last comment not closed properly
                                        return statement.length;
                                    }
                                    if (!(statement[m + n] == STOP_SKIP[i].charAt(n))) {
                                        endMatch = false;
                                        break;
                                    }
                                    endPos = m + n;
                                }
                                if (endMatch) {
                                    // found character sequence ending comment or quote
                                    return endPos + 1;
                                }
                            }
                        }
                        // character sequence ending comment or quote not found
                        return statement.length;
                    }

                }
            }
            return position;
        }

    }

}
