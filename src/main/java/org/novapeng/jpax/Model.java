package org.novapeng.jpax;

import org.apache.commons.beanutils.PropertyUtils;

import javax.persistence.*;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;

public interface Model {

    void _save();
    void _delete();
    Object _key();

    @SuppressWarnings("UnnecessaryInterfaceModifier")
    public static class Property {

        public String name;
        public Class<?> type;
        public Field field;
        public boolean isSearchable;
        public boolean isMultiple;
        public boolean isRelation;
        public boolean isGenerated;
        public Class<?> relationType;
        public Choices choices;

    }

    interface Choices {

        List<Object> list();

    }

    @SuppressWarnings("unused")
    interface Factory {

        String keyName();
        Class<?> keyType();
        Object keyValue(Model m);
        Model findById(Object id);
        List<Model> fetch(int offset, int length, String orderBy, String orderDirection, List<String> properties, String keywords, String where);
        Long count(List<String> properties, String keywords, String where);
        void deleteAll();
        List<Property> listProperties();
    }

    class Manager {

        public static Factory factoryFor(Class<? extends Model> clazz) {
            if(Model.class.isAssignableFrom(clazz)) {
                Factory factory = modelFactory(clazz);
                if( factory != null) {
                    return factory;
                }
            }
            throw new UnexpectedException("BaseModel " + clazz.getName() + " is not managed by any plugin");
        }

        public static Factory modelFactory(Class<? extends Model> modelClass) {
            if (modelClass.isAnnotationPresent(Entity.class)) {
                return new JPAModelLoader(modelClass);
            }
            return null;
        }

        @SuppressWarnings({"ConstantConditions", "IndexOfReplaceableByContains", "unused", "RedundantCast"})
        public static class JPAModelLoader implements Factory {

            private Class<? extends Model> clazz;
            private Map<String, Property> properties;


            public JPAModelLoader(Class<? extends Model> clazz) {
                this.clazz = clazz;
            }

            public Model findById(Object id) {
                try {
                    if (id == null) {
                        return null;
                    }
                    return JPA.em().find(clazz, id);
                } catch (Exception e) {
                    // Key is invalid, thus nothing was found
                    return null;
                }
            }

            @SuppressWarnings("unchecked")
            public List<Model> fetch(int offset, int size, String orderBy, String order, List<String> searchFields, String keywords, String where) {
                String q = "from " + clazz.getName();
                if (keywords != null && !keywords.equals("")) {
                    String searchQuery = getSearchQuery(searchFields);
                    if (!searchQuery.equals("")) {
                        q += " where (" + searchQuery + ")";
                    }
                    q += (where != null ? " and " + where : "");
                } else {
                    q += (where != null ? " where " + where : "");
                }
                if (orderBy == null && order == null) {
                    orderBy = "id";
                    order = "ASC";
                }
                if (orderBy == null && order != null) {
                    orderBy = "id";
                }
                if (order == null || (!order.equals("ASC") && !order.equals("DESC"))) {
                    order = "ASC";
                }
                q += " order by " + orderBy + " " + order;
                Query query = JPA.em().createQuery(q);
                if (keywords != null && !keywords.equals("") && q.indexOf("?1") != -1) {
                    query.setParameter(1, "%" + keywords.toLowerCase() + "%");
                }
                query.setFirstResult(offset);
                query.setMaxResults(size);
                return query.getResultList();
            }

            public Long count(List<String> searchFields, String keywords, String where) {
                String q = "select count(*) from " + clazz.getName() + " e";
                if (keywords != null && !keywords.equals("")) {
                    String searchQuery = getSearchQuery(searchFields);
                    if (!searchQuery.equals("")) {
                        q += " where (" + searchQuery + ")";
                    }
                    q += (where != null ? " and " + where : "");
                } else {
                    q += (where != null ? " where " + where : "");
                }
                Query query = JPA.em().createQuery(q);
                if (keywords != null && !keywords.equals("") && q.indexOf("?1") != -1) {
                    query.setParameter(1, "%" + keywords.toLowerCase() + "%");
                }
                return Long.decode(query.getSingleResult().toString());
            }

            public void deleteAll() {
                JPA.em().createQuery("delete from " + clazz.getName()).executeUpdate();
            }

            public List<Property> listProperties() {
                List<Property> properties = new ArrayList<Property>();
                Set<Field> fields = new LinkedHashSet<Field>();
                Class<?> tclazz = clazz;
                while (!tclazz.equals(Object.class)) {
                    Collections.addAll(fields, tclazz.getDeclaredFields());
                    tclazz = tclazz.getSuperclass();
                }
                for (Field f : fields) {
                    if (Modifier.isTransient(f.getModifiers())) {
                        continue;
                    }
                    if (f.isAnnotationPresent(Transient.class)) {
                        continue;
                    }
                    Property mp = buildProperty(f);
                    if (mp != null) {
                        properties.add(mp);
                    }
                }
                return properties;
            }

            public String keyName() {
                return keyField().getName();
            }

            public Class<?> keyType() {
                return keyField().getType();
            }

            public Class<?>[] keyTypes() {
                Field[] fields = keyFields();
                Class<?>[] types = new Class<?>[fields.length];
                int i = 0;
                for (Field field : fields) {
                    types[i++] = field.getType();
                }
                return types;
            }

            public String[] keyNames() {
                Field[] fields = keyFields();
                String[] names = new String[fields.length];
                int i = 0;
                for (Field field : fields) {
                    names[i++] = field.getName();
                }
                return names;
            }

            private Class<?> getCompositeKeyClass() {
                Class<?> tclazz = clazz;
                while (!tclazz.equals(Object.class)) {
                    // Only consider mapped types
                    if (tclazz.isAnnotationPresent(Entity.class)
                            || tclazz.isAnnotationPresent(MappedSuperclass.class)) {
                        IdClass idClass = tclazz.getAnnotation(IdClass.class);
                        if (idClass != null)
                            return idClass.value();
                    }
                    tclazz = tclazz.getSuperclass();
                }
                throw new UnexpectedException("Invalid mapping for class " + clazz + ": multiple IDs with no @IdClass annotation");
            }


            private void initProperties() {
                synchronized(this){
                    if(properties != null)
                        return;
                    properties = new HashMap<String,Property>();
                    Set<Field> fields = getModelFields(clazz);
                    for (Field f : fields) {
                        if (Modifier.isTransient(f.getModifiers())) {
                            continue;
                        }
                        if (f.isAnnotationPresent(Transient.class)) {
                            continue;
                        }
                        Property mp = buildProperty(f);
                        if (mp != null) {
                            properties.put(mp.name, mp);
                        }
                    }
                }
            }

            private Object makeCompositeKey(Model model) throws Exception {
                initProperties();
                Class<?> idClass = getCompositeKeyClass();
                Object id = idClass.newInstance();
                PropertyDescriptor[] idProperties = PropertyUtils.getPropertyDescriptors(idClass);
                if(idProperties == null || idProperties.length == 0)
                    throw new UnexpectedException("Composite id has no properties: "+idClass.getName());
                for (PropertyDescriptor idProperty : idProperties) {
                    // do we have a field for this?
                    String idPropertyName = idProperty.getName();
                    // skip the "class" property...
                    if(idPropertyName.equals("class"))
                        continue;
                    Property modelProperty = this.properties.get(idPropertyName);
                    if(modelProperty == null)
                        throw new UnexpectedException("Composite id property missing: "+clazz.getName()+"."+idPropertyName
                                +" (defined in IdClass "+idClass.getName()+")");
                    // sanity check
                    Object value = modelProperty.field.get(model);

                    if(modelProperty.isMultiple)
                        throw new UnexpectedException("Composite id property cannot be multiple: "+clazz.getName()+"."+idPropertyName);
                    // now is this property a relation? if yes then we must use its ID in the key (as per specs)
                    if(modelProperty.isRelation){
                        // get its id
                        if(!Model.class.isAssignableFrom(modelProperty.type))
                            throw new UnexpectedException("Composite id property entity has to be a subclass of BaseModel: "
                                    +clazz.getName()+"."+idPropertyName);
                        // we already checked that cast above
                        @SuppressWarnings("unchecked")
                        Model.Factory factory = Manager.factoryFor((Class<? extends Model>) modelProperty.type);
                        if(factory == null)
                            throw new UnexpectedException("Failed to find factory for Composite id property entity: "
                                    +clazz.getName()+"."+idPropertyName);
                        // we already checked that cast above
                        if(value != null)
                            value = factory.keyValue((Model) value);
                    }
                    // now affect the composite id with this id
                    PropertyUtils.setSimpleProperty(id, idPropertyName, value);
                }
                return id;
            }



            public Object keyValue(Model m) {
                try {
                    if (m == null) {
                        return null;
                    }

                    // Do we have a @IdClass or @Embeddable?
                    if (m.getClass().isAnnotationPresent(IdClass.class)) {
                        return makeCompositeKey(m);
                    }

                    // Is it a composite key? If yes we need to return the matching PK
                    final Field[] fields = keyFields();
                    final Object[] values = new Object[fields.length];
                    int i = 0;
                    for (Field f : fields) {
                        final Object o = f.get(m);
                        if (o != null) {
                            values[i++] = o;
                        }
                    }

                    // If we have only one id return it
                    if (values.length == 1) {
                        return values[0];
                    }

                    return values;
                } catch (Exception ex) {
                    throw new UnexpectedException(ex);
                }
            }

            public static Set<Field> getModelFields(Class<?> clazz){
                Set<Field> fields = new LinkedHashSet<Field>();
                Class<?> tclazz = clazz;
                while (!tclazz.equals(Object.class)) {
                    // Only add fields for mapped types
                    if(tclazz.isAnnotationPresent(Entity.class)
                            || tclazz.isAnnotationPresent(MappedSuperclass.class))
                        Collections.addAll(fields, tclazz.getDeclaredFields());
                    tclazz = tclazz.getSuperclass();
                }
                return fields;
            }

            //
            Field keyField() {
                Class c = clazz;
                try {
                    while (!c.equals(Object.class)) {
                        for (Field field : c.getDeclaredFields()) {
                            if (field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(EmbeddedId.class)) {
                                field.setAccessible(true);
                                return field;
                            }
                        }
                        c = c.getSuperclass();
                    }
                } catch (Exception e) {
                    throw new UnexpectedException("Error while determining the object @Id for an object of type " + clazz);
                }
                throw new UnexpectedException("Cannot get the object @Id for an object of type " + clazz);
            }

            Field[] keyFields() {
                Class c = clazz;
                try {
                    List<Field> fields = new ArrayList<Field>();
                    while (!c.equals(Object.class)) {
                        for (Field field : c.getDeclaredFields()) {
                            if (field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(EmbeddedId.class)) {
                                field.setAccessible(true);
                                fields.add(field);
                            }
                        }
                        c = c.getSuperclass();
                    }
                    final Field[] f = fields.toArray(new Field[fields.size()]);
                    if (f.length == 0) {
                        throw new UnexpectedException("Cannot get the object @Id for an object of type " + clazz);
                    }
                    return f;
                } catch (Exception e) {
                    throw new UnexpectedException("Error while determining the object @Id for an object of type " + clazz);
                }
            }

            String getSearchQuery(List<String> searchFields) {
                String q = "";
                for (Property property : listProperties()) {
                    if (property.isSearchable && (searchFields == null || searchFields.isEmpty() || searchFields.contains(property.name))) {
                        if (!q.equals("")) {
                            q += " or ";
                        }
                        q += "lower(" + property.name + ") like ?1";
                    }
                }
                return q;
            }

            Property buildProperty(final Field field) {
                Property modelProperty = new Property();
                modelProperty.type = field.getType();
                modelProperty.field = field;
                if (Model.class.isAssignableFrom(field.getType())) {
                    if (field.isAnnotationPresent(OneToOne.class)) {
                        if (field.getAnnotation(OneToOne.class).mappedBy().equals("")) {
                            modelProperty.isRelation = true;
                            modelProperty.relationType = field.getType();
                            modelProperty.choices = new Choices() {

                                @SuppressWarnings("unchecked")
                                public List<Object> list() {
                                    return JPA.em().createQuery("from " + field.getType().getName()).getResultList();
                                }
                            };
                        }
                    }
                    if (field.isAnnotationPresent(ManyToOne.class)) {
                        modelProperty.isRelation = true;
                        modelProperty.relationType = field.getType();
                        modelProperty.choices = new Choices() {

                            @SuppressWarnings("unchecked")
                            public List<Object> list() {
                                return JPA.em().createQuery("from " + field.getType().getName()).getResultList();
                            }
                        };
                    }
                }
                if (Collection.class.isAssignableFrom(field.getType())) {
                    final Class<?> fieldType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                    if (field.isAnnotationPresent(OneToMany.class)) {
                        if (field.getAnnotation(OneToMany.class).mappedBy().equals("")) {
                            modelProperty.isRelation = true;
                            modelProperty.isMultiple = true;
                            modelProperty.relationType = fieldType;
                            modelProperty.choices = new Choices() {

                                @SuppressWarnings("unchecked")
                                public List<Object> list() {
                                    return JPA.em().createQuery("from " + fieldType.getName()).getResultList();
                                }
                            };
                        }
                    }
                    if (field.isAnnotationPresent(ManyToMany.class)) {
                        if (field.getAnnotation(ManyToMany.class).mappedBy().equals("")) {
                            modelProperty.isRelation = true;
                            modelProperty.isMultiple = true;
                            modelProperty.relationType = fieldType;
                            modelProperty.choices = new Choices() {

                                @SuppressWarnings("unchecked")
                                public List<Object> list() {
                                    return JPA.em().createQuery("from " + fieldType.getName()).getResultList();
                                }
                            };
                        }
                    }
                }
                if (field.getType().isEnum()) {
                    modelProperty.choices = new Choices() {

                        @SuppressWarnings("unchecked")
                        public List<Object> list() {
                            return (List<Object>) Arrays.asList(field.getType().getEnumConstants());
                        }
                    };
                }
                modelProperty.name = field.getName();
                if (field.getType().equals(String.class)) {
                    modelProperty.isSearchable = true;
                }
                if (field.isAnnotationPresent(GeneratedValue.class)) {
                    modelProperty.isGenerated = true;
                }
                if (field.isAnnotationPresent(Id.class) || field.isAnnotationPresent(EmbeddedId.class)) {
                    // Look if the target is an embeddable class
                    if (field.getType().isAnnotationPresent(Embeddable.class) || field.getType().isAnnotationPresent(IdClass.class) ) {
                        modelProperty.isRelation = true;
                        modelProperty.relationType =  field.getType();
                    }
                }
                return modelProperty;
            }
        }

    }

}
