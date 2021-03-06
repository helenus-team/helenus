/*
 * Copyright (C) 2015-2017 The Helenus Driver Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.helenus.driver.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.text.WordUtils;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.TypeCodec;
import com.datastax.driver.core.UDTValue;
import com.datastax.driver.core.exceptions.InvalidTypeException;

import org.helenus.commons.lang3.reflect.ReflectionUtils;
import org.helenus.driver.ExcludedKeyspaceKeyException;
import org.helenus.driver.ObjectConversionException;
import org.helenus.driver.ObjectMissingException;
import org.helenus.driver.codecs.ArgumentsCodec;
import org.helenus.driver.info.ClassInfo;
import org.helenus.driver.info.FieldInfo;
import org.helenus.driver.info.TableInfo;
import org.helenus.driver.persistence.ClusteringKey;
import org.helenus.driver.persistence.Column;
import org.helenus.driver.persistence.DataType;
import org.helenus.driver.persistence.Index;
import org.helenus.driver.persistence.KeyspaceKey;
import org.helenus.driver.persistence.Mandatory;
import org.helenus.driver.persistence.PartitionKey;
import org.helenus.driver.persistence.Table;
import org.helenus.driver.persistence.TypeKey;

/**
 * The <code>FieldInfo</code> class caches all the field information needed by
 * the class ClassInfo.
 * <p>
 * <i>Note:</i> A fake {@link TableInfoImpl} class with no table annotations
 * might be passed in for user-defined type entities. By design, this class
 * will not allow any type of keys but only columns.
 *
 * @copyright 2015-2016 The Helenus Driver Project Authors
 *
 * @author  The Helenus Driver Project Authors
 * @version 1 - Jan 19, 2015 - paouelle, vasu - Creation
 *
 * @param <T> The type of POJO represented by this field
 *
 * @since 1.0
 */
public class FieldInfoImpl<T> implements FieldInfo<T> {
  /**
   * Holds the class for the POJO.
   *
   * @author vasu
   */
  private final Class<T> clazz;

  /**
   * Holds the class info for the POJO this field is in.
   *
   * @author paouelle
   */
  private final ClassInfoImpl<T> cinfo;

  /**
   * Holds the table information for this field. Can be <code>null</code> if
   * the field is only used as a keyspace key.
   *
   * @author vasu
   */
  public final TableInfoImpl<T> tinfo;

  /**
   * Holds the declaring class for this field
   *
   * @author vasu
   */
  private final Class<?> declaringClass;

  /**
   * Holds the reflection field represented by this field info object
   *
   * @author vasu
   */
  private final Field field;

  /**
   * Holds the name for this field.
   *
   * @author paouelle
   */
  private final String name;

  /**
   * Holds the type for this field.
   *
   * @author paouelle
   */
  private final Class<?> type;

  /**
   * Holds a flag indicating if the field is optional or not.
   *
   * @author paouelle
   */
  private final boolean isOptional;

  /**
   * This variable is used to cache column annotation.
   *
   * @author vasu
   */
  private final Column column;

  /**
   * Keyspace key annotation for this field if any.
   *
   * @author vasu
   */
  private final KeyspaceKey keyspaceKey;

  /**
   * Flag indicating if the field is mandatory (i.e. cannot be <code>null</code>).
   *
   * @author paouelle
   */
  private final boolean mandatory;

  /**
   * Index annotation for the field if any.
   *
   * @author paouelle
   */
  private final Index index;

  /**
   * PartitionKey annotation for the field if any.
   *
   * @author paouelle
   */
  private final PartitionKey partitionKey;

  /**
   * ClusteringKey annotation for the field if any.
   *
   * @author paouelle
   */
  private final ClusteringKey clusteringKey;

  /**
   * TypeKey annotation for the field if any.
   *
   * @author paouelle
   */
  private final TypeKey typeKey;

  /**
   * Element type of the set when this field is a multi-key.
   *
   * @author paouelle
   */
  private final Class<?> multiKeyType;

  /**
   * Holds the data type definition for this field (if it is a column).
   *
   * @author paouelle
   */
  protected final DataTypeImpl.Definition definition;

  /**
   * Holds the codecs for this field (if it is a column) keyed per keyspace.
   *
   * @author paouelle
   */
  private final Map<String, TypeCodec<?>> codecs;

  /**
   * Holds an internal codec valid for parsing, formatting, and validating this
   * field (if it is a column).
   * <p>
   * <i>Note:</i> This codec is not suitable for serializing and deserializing
   * this field's data type.
   *
   * @author paouelle
   */
  private final TypeCodec<?> icodec;

  /**
   * Flag indicating if the field is final.
   *
   * @author paouelle
   */
  private final boolean isFinal;

  /**
   * Holds the final value for the field if defined final.
   *
   * @author paouelle
   */
  private final Object finalValue;

  /**
   * Holds the consumers used to update the value for this field for an instance
   * keyed by the class (used when subclasses are involved and have different setters).
   * <p>
   * The first argument is the instance where to update the value of the field
   * and the second one is the value to set.
   *
   * @author paouelle
   */
  final Map<Class<? extends T>, BiConsumer<Object, Object>> setters;

  /**
   * Holds the functions used to retrieve the value of the field from an instance
   * keyed by the class (used when subclasses are involved and have different getters).
   * <p>
   * The argument is the instance from which to retrieve the value for this field.
   *
   * @author paouelle
   */
  final Map<Class<? extends T>, Function<Object, Object>> getters;

  /**
   * Flag indicating if this is the last key in the partition or the cluster.
   *
   * @author paouelle
   */
  private volatile boolean isLast = false;

  /**
   * Instantiates a new <code>FieldInfoImpl</code> object for a root element pojo
   * class.
   *
   * @author paouelle
   *
   * @param cinfo the non-<code>null</code> class info for the POJO root element
   * @param tinfo the non-<code>null</code> table info from the POJO root element
   * @param field the non-<code>null</code> field to copy
   */
  FieldInfoImpl(
    ClassInfoImpl<T> cinfo,
    TableInfoImpl<T> tinfo,
    FieldInfoImpl<? extends T> field
  ) {
    this(cinfo, tinfo, field, field.mandatory);
  }

  /**
   * Instantiates a new <code>FieldInfoImpl</code> object for a root element pojo
   * class.
   *
   * @author paouelle
   *
   * @param cinfo the non-<code>null</code> class info for the POJO root element
   * @param tinfo the non-<code>null</code> table info from the POJO root element
   * @param field the non-<code>null</code> field to copy
   * @param mandatory <code>true</code> to set the field has a mandatory one;
   *        <code>false</code> otherwise
   */
  FieldInfoImpl(
    ClassInfoImpl<T> cinfo,
    TableInfoImpl<T> tinfo,
    FieldInfoImpl<? extends T> field,
    boolean mandatory
  ) {
    this.clazz = cinfo.getObjectClass();
    this.cinfo = cinfo;
    this.tinfo = tinfo;
    this.declaringClass = field.declaringClass;
    this.field = field.field;
    this.name = field.name;
    this.type = field.type;
    this.isOptional = field.isOptional;
    this.column = field.column;
    this.keyspaceKey = field.keyspaceKey;
    this.mandatory = mandatory;
    this.index = field.index;
    this.partitionKey = field.partitionKey;
    this.clusteringKey = field.clusteringKey;
    this.typeKey = field.typeKey;
    this.multiKeyType = field.multiKeyType;
    this.definition = field.definition;
    this.codecs = field.codecs;
    this.icodec = field.icodec;
    this.isFinal = field.isFinal;
    this.finalValue = field.finalValue;
    this.setters = new HashMap<>(field.setters);
    this.getters = new HashMap<>(field.getters);
    this.isLast = field.isLast;
  }

  /**
   * Instantiates a new <code>FieldInfoImpl</code> object not part of a defined
   * table.
   *
   * @author vasu
   *
   * @param  cinfo the non-<code>null</code> class info for the POJO
   * @param  field the non-<code>null</code> field to create an info object for
   * @throws IllegalArgumentException if unable to find a getter or setter
   *         method for the field of if improperly annotated
   */
  FieldInfoImpl(ClassInfoImpl<T> cinfo, Field field) {
    this.clazz = cinfo.getObjectClass();
    this.cinfo = cinfo;
    this.tinfo = null;
    this.declaringClass = field.getDeclaringClass();
    this.field = field;
    field.setAccessible(true); // make it accessible in case we need to
    this.isFinal = Modifier.isFinal(field.getModifiers());
    this.name = field.getName();
    this.type = ClassUtils.primitiveToWrapper(
      DataTypeImpl.unwrapOptionalIfPresent(field.getType(), field.getGenericType())
    );
    this.isOptional = Optional.class.isAssignableFrom(field.getType());
    this.column = null;
    this.keyspaceKey = field.getAnnotation(KeyspaceKey.class);
    this.mandatory = true; // keyspace keys are mandatory fields
    this.index = null; // we don't care about this for keyspace keys
    this.partitionKey = null; // we don't care about this for keyspace keys
    this.clusteringKey = null; // we don't care about this for keyspace keys
    this.typeKey = null; // we don't care about this for keyspace keys
    this.multiKeyType = null; // we don't care about this for keyspace keys
    this.definition = null; // we don't care about this for keyspace keys
    this.codecs = null; // we don't care about this for keyspace keys
    this.icodec = null; // we don't care about this for keyspace keys
    this.getters = new HashMap<>(6);
    this.setters = new HashMap<>(6);
    findGetter(declaringClass);
    findSetter(declaringClass);
    this.finalValue = findFinalValue();
  }

  /**
   * Instantiates a new <code>FieldInfoImpl</code> object as a column part of a
   * defined table.
   *
   * @author vasu
   *
   * @param  mgr the non-<code>null</code> statement manager
   * @param  tinfo the table info for the field
   * @param  field the non-<code>null</code> field to create an info object for
   * @throws IllegalArgumentException if unable to find a getter or setter
   *         method for the field of if improperly annotated
   */
  FieldInfoImpl(
    StatementManagerImpl mgr, TableInfoImpl<T> tinfo, Field field
  ) {
    this.clazz = tinfo.getObjectClass();
    this.cinfo = (ClassInfoImpl<T>)tinfo.getClassInfo();
    this.tinfo = tinfo;
    this.declaringClass = field.getDeclaringClass();
    this.field = field;
    field.setAccessible(true); // make it accessible in case we need to
    this.isFinal = Modifier.isFinal(field.getModifiers());
    this.name = field.getName();
    this.type = ClassUtils.primitiveToWrapper(
      DataTypeImpl.unwrapOptionalIfPresent(field.getType(), field.getGenericType())
    );
    this.isOptional = Optional.class.isAssignableFrom(field.getType());
    this.keyspaceKey = field.getAnnotation(KeyspaceKey.class);
    this.mandatory = (
      // primitive types for fields must be mandatory since null is not possible
      field.getType().isPrimitive() || (field.getAnnotation(Mandatory.class) != null)
    );
    org.apache.commons.lang3.Validate.isTrue(
      !isOptional || !mandatory,
      "field cannot be annotated with @Mandatory if it is optional: %s.%s",
      declaringClass.getName(),
      field.getName()
    );
    final Map<String, Column> columns
      = ReflectionUtils.getAnnotationsByType(String.class, Column.class, field);
    final Map<String, Index> indexes
      = ReflectionUtils.getAnnotationsByType(String.class, Index.class, field);
    final Map<String, PartitionKey> partitionKeys
      = ReflectionUtils.getAnnotationsByType(String.class, PartitionKey.class, field);
    final Map<String, ClusteringKey> clusteringKeys
      = ReflectionUtils.getAnnotationsByType(String.class, ClusteringKey.class, field);
    final Map<String, TypeKey> typeKeys
      = ReflectionUtils.getAnnotationsByType(String.class, TypeKey.class, field);
    final boolean isInTable = tinfo.getTable() != null;

    if (isInTable) {
      org.apache.commons.lang3.Validate.isTrue(
        !(!indexes.isEmpty() && columns.isEmpty()),
        "field must be annotated with @Column if it is annotated with @Index: %s.%s",
        declaringClass.getName(),
        field.getName()
      );
      org.apache.commons.lang3.Validate.isTrue(
        !(!partitionKeys.isEmpty() && columns.isEmpty()),
        "field must be annotated with @Column if it is annotated with @PartitionKey: %s.%s",
        declaringClass.getName(),
        field.getName()
      );
      org.apache.commons.lang3.Validate.isTrue(
        !(!clusteringKeys.isEmpty() && columns.isEmpty()),
        "field must be annotated with @Column if it is annotated with @ClusteringKey: %s.%s",
        declaringClass.getName(),
        field.getName()
      );
      org.apache.commons.lang3.Validate.isTrue(
        !(!typeKeys.isEmpty() && columns.isEmpty()),
        "field must be annotated with @Column if it is annotated with @TypeKey: %s.%s",
        declaringClass.getName(),
        field.getName()
      );
    }
    // Note: while searching for the matching table, uses the name from the
    // table annotation instead of the one returned by getName() as the later
    // might have been cleaned and hence would not match what was defined in
    // the POJO
    final String tname = isInTable ? tinfo.getTable().name() : Table.ALL;

    Column column = columns.get(tname);
    Index index = indexes.get(tname);
    PartitionKey partitionKey = partitionKeys.get(tname);
    ClusteringKey clusteringKey = clusteringKeys.get(tname);
    TypeKey typeKey = typeKeys.get(tname);

    if (column == null) { // fallback to special Table.ALL name
      column = columns.get(Table.ALL);
    }
    this.column = column;
    if (index == null) { // fallback to special Table.ALL name
      index = indexes.get(Table.ALL);
    }
    this.index = index;
    if (partitionKey == null) { // fallback to special Table.ALL name
      partitionKey = partitionKeys.get(Table.ALL);
    }
    this.partitionKey = partitionKey;
    if (clusteringKey == null) { // fallback to special Table.ALL name
      clusteringKey = clusteringKeys.get(Table.ALL);
    }
    this.clusteringKey = clusteringKey;
    if (typeKey == null) { // fallback to special Table.ALL name
      typeKey = typeKeys.get(Table.ALL);
    }
    this.typeKey = typeKey;
    // validate some UDT stuff
    if (!isInTable) {
      org.apache.commons.lang3.Validate.isTrue(
        !isIndex(),
        "field cannot be annotated with @Index: %s.%s",
        declaringClass.getName(),
        field.getName()
      );
      org.apache.commons.lang3.Validate.isTrue(
        !isStatic(),
        "field cannot be annotated with @Column(isStatic=true): %s.%s",
        declaringClass.getName(),
        field.getName()
      );
      org.apache.commons.lang3.Validate.isTrue(
        !isPartitionKey(),
        "field cannot be annotated with @PartitionKey: %s.%s",
        declaringClass.getName(),
        field.getName()
      );
      org.apache.commons.lang3.Validate.isTrue(
        !isClusteringKey(),
        "field cannot be annotated with @ClusteringKey: %s.%s",
        declaringClass.getName(),
        field.getName()
      );
      if (!(cinfo instanceof UDTRootClassInfoImpl)
          && !(cinfo instanceof UDTTypeClassInfoImpl)) {
        org.apache.commons.lang3.Validate.isTrue(
          !isTypeKey(),
          "field cannot be annotated with @TypeKey: %s.%s",
          declaringClass.getName(),
          field.getName()
        );
      }
    }
    if (isColumn()) {
      this.definition = DataTypeImpl.inferDataTypeFrom(mgr, field, column.isFrozen());
      this.codecs = new ConcurrentHashMap<>(8);
      this.icodec = getCodec("");
      if (isInTable
          && ((clusteringKey != null) || (partitionKey != null))
          && ((definition.getMainType() == DataType.SET)
              || (definition.getMainType() == DataType.ORDERED_SET))) {
        final Type type = field.getGenericType();

        if (type instanceof ParameterizedType) {
          final ParameterizedType ptype = (ParameterizedType)type;

          this.multiKeyType = ReflectionUtils.getRawClass(
            ptype.getActualTypeArguments()[0]
          ); // sets will always have 1 argument
        } else {
          throw new IllegalArgumentException(
            "unable to determine the element type of multi-field in table '"
            + tname
            + "': "
            + declaringClass.getName()
            + "."
            + field.getName()
          );
        }
      } else {
        this.multiKeyType = null;
      }
    } else {
      this.definition = null;
      this.codecs = null;
      this.icodec = null;
      this.multiKeyType = null;
    }
    this.getters = new HashMap<>(6);
    this.setters = new HashMap<>(6);
    findGetter(declaringClass);
    findSetter(declaringClass);
    this.finalValue = findFinalValue();
    // validate some stuff
    if (isInTable) {
      org.apache.commons.lang3.Validate.isTrue(
        !(isIndex() && !isColumn()),
        "field in table '%s' must be annotated with @Column if it is annotated with @Index: %s.%s",
        tname,
        declaringClass.getName(),
        field.getName()
      );
      org.apache.commons.lang3.Validate.isTrue(
        !(isPartitionKey() && isClusteringKey()),
        "field in table '%s' must not be annotated with @ClusteringKey if it is annotated with @PartitionKey: %s.%s",
        tname,
        declaringClass.getName(),
        field.getName()
      );
      org.apache.commons.lang3.Validate.isTrue(
        !(isPartitionKey() && !isColumn()),
        "field in table '%s' must be annotated with @Column if it is annotated with @PartitionKey: %s.%s",
        tname,
        declaringClass.getName(),
        field.getName()
      );
      org.apache.commons.lang3.Validate.isTrue(
        !(isClusteringKey() && !isColumn()),
        "field in table '%s' must be annotated with @Column if it is annotated with @ClusteringKey: %s.%s",
        tname,
        declaringClass.getName(),
        field.getName()
      );
      org.apache.commons.lang3.Validate.isTrue(
        !(isTypeKey() && !isColumn()),
        "field in table '%s' must be annotated with @Column if it is annotated with @TypeKey: %s.%s",
        tname,
        declaringClass.getName(),
        field.getName()
      );
      org.apache.commons.lang3.Validate.isTrue(
        !(isTypeKey() && !isMandatory()),
        "field in table '%s' must be annotated with @Mandatory if it is annotated with @TypeKey: %s.%s",
        tname,
        declaringClass.getName(),
        field.getName()
      );
      org.apache.commons.lang3.Validate.isTrue(
        !(isTypeKey() && !String.class.equals(getType())),
        "field in table '%s' must be a String if it is annotated with @TypeKey: %s.%s",
        tname,
        declaringClass.getName(),
        field.getName()
      );
      org.apache.commons.lang3.Validate.isTrue(
        !(isTypeKey() && isFinal()),
        "field in table '%s' must not be final if it is annotated with @TypeKey: %s.%s",
        tname,
        declaringClass.getName(),
        field.getName()
      );
      org.apache.commons.lang3.Validate.isTrue(
        !(isTypeKey()
          && !(cinfo instanceof RootClassInfoImpl)
          && !(cinfo instanceof TypeClassInfoImpl)
          && !(cinfo instanceof UDTRootClassInfoImpl)
          && !(cinfo instanceof UDTTypeClassInfoImpl)),
        "field in table '%s' must not be annotated with @TypeKey if class is annotated with @Entity: %s.%s",
        tname,
        declaringClass.getName(),
        field.getName()
      );
      if (isColumn() && definition.isCollection()) {
        org.apache.commons.lang3.Validate.isTrue(
          !((isClusteringKey() || isPartitionKey()) && (multiKeyType == null)),
          "field in table '%s' cannot be '%s' if it is annotated with @ClusteringKey or @PartitionKey: %s.%s",
          tname,
          definition,
          declaringClass.getName(),
          field.getName()
        );
      }
      if (isCaseInsensitiveKey()) {
        // make sure data type is string or element type is string for multi keys
        if (multiKeyType != null) {
          org.apache.commons.lang3.Validate.isTrue(
            String.class.equals(multiKeyType),
            "field in table '%s' must be a Set of Strings if it is annotated with @%s(ignoreCase=true): %s.%s",
            tname,
            (isPartitionKey() ? "PartitionKey" : "ClusteringKey"),
            declaringClass.getName(),
            field.getName()
          );
        } else {
          org.apache.commons.lang3.Validate.isTrue(
            String.class.equals(getType()),
            "field in table '%s' must be a String if it is annotated with @%s(ignoreCase=true): %s.%s",
            tname,
            (isPartitionKey() ? "PartitionKey" : "ClusteringKey"),
            declaringClass.getName(),
            field.getName()
          );
        }
      }
      if (isStatic()) {
        org.apache.commons.lang3.Validate.isTrue(
          !isPartitionKey(),
          "field in table '%s' cannot be annotated with @Column(isStatic=true) if it is annotated with @PartitionKey: %s.%s",
          tname,
          declaringClass.getName(),
          field.getName()
        );
        org.apache.commons.lang3.Validate.isTrue(
          !isClusteringKey(),
          "field in table '%s' cannot be annotated with @Column(isStatic=true) if it is annotated with @ClusteringKey: %s.%s",
          tname,
          declaringClass.getName(),
          field.getName()
        );
        org.apache.commons.lang3.Validate.isTrue(
          !isKeyspaceKey(),
          "field in table '%s' cannot be annotated with @Column(isStatic=true) if it is annotated with @KeyspaceKey: %s.%s",
          tname,
          declaringClass.getName(),
          field.getName()
        );
      }
    }
  }

  /**
   * Instantiates a new fake <code>FieldInfoImpl</code> object to represent a
   * keyspace key associated with the POJO class of a user-defined type.
   *
   * @author paouelle
   *
   * @param  cinfo the non-<code>null</code> class info for the POJO
   * @param  kkey the non-<code>null</code> keyspace key annotation for the POJO class
   */
  FieldInfoImpl(ClassInfoImpl<T> cinfo, KeyspaceKey kkey) {
    this.clazz = cinfo.getObjectClass();
    this.cinfo = cinfo;
    this.tinfo = null;
    this.declaringClass = cinfo.getObjectClass();
    this.field = null;
    this.isFinal = true;
    this.name = kkey.name();
    this.type = String.class;
    this.isOptional = false;
    this.column = null;
    this.keyspaceKey = kkey;
    this.mandatory = true; // keyspace keys are mandatory fields
    this.index = null; // we don't care about this for keyspace keys
    this.partitionKey = null; // we don't care about this for keyspace keys
    this.clusteringKey = null; // we don't care about this for keyspace keys
    this.typeKey = null; // we don't care about this for keyspace keys
    this.multiKeyType = null; // we don't care about this for keyspace keys
    this.definition = null; // we don't care about this for keyspace keys
    this.codecs = null; // we don't care about this for keyspace keys
    this.icodec = null; // we don't care about this for keyspace keys
    this.getters = null;
    this.setters = null;
    this.finalValue = null;
  }

  /**
   * Instantiates a new fake <code>FieldInfoImpl</code> object to holds the
   * collection of elements for a user-defined type that extends {@link List},
   * {@link Set}, or {@link Map}.
   *
   * @author paouelle
   *
   * @param  mgr the non-<code>null</code> statement manager
   * @param  cinfo the non-<code>null</code> class info for the POJO
   * @param  type the non-<code>null</code> data type for the collection for the
   *         POJO
   * @param  setter the non-<code>null</code> consumer to use for setting the
   *         collection values stored in Cassandra back into the instance
   */
  FieldInfoImpl(
    StatementManagerImpl mgr,
    ClassInfoImpl<T> cinfo,
    DataType type,
    BiConsumer<Object, Object> setter
  ) {
    this.clazz = cinfo.getObjectClass();
    this.cinfo = cinfo;
    this.tinfo = null;
    this.declaringClass = cinfo.getObjectClass();
    this.field = null;
    this.isFinal = false;
    this.name = StatementImpl.UDT_C_PREFIX + type.CQL;
    this.type = clazz.getSuperclass();
    this.isOptional = false;
    this.column = new Column() { // fake annotation so we can properly identify the field as a column with a special name
      @Override
      public Class<? extends Annotation> annotationType() {
        return Column.class;
      }
      @Override
      public String table() {
        return Table.ALL;
      }
      @Override
      public String name() {
        return getName();
      }
      @Override
      public boolean isFrozen() {
        return false;
      }
      @Override
      public boolean isStatic() {
        return false;
      }
    };
    this.keyspaceKey = null;
    this.mandatory = true; // collection column is mandatory
    this.index = null;
    this.partitionKey = null;
    this.clusteringKey = null;
    this.typeKey = null;
    this.multiKeyType = null;
    this.definition = DataTypeImpl.inferDataTypeFrom(mgr, type, column.isFrozen(), clazz);
    this.codecs = new ConcurrentHashMap<>(8);
    this.icodec = getCodec("");
    this.getters = new HashMap<>(6);
    this.setters = new HashMap<>(6);
    getters.put(cinfo.getObjectClass(), obj -> obj); // return the instance itself as the value for the field
    setters.put(cinfo.getObjectClass(), setter);
    this.finalValue = null;
  }

  /**
   * Finds the getter from the declaring class suitable to get a
   * reference to the field.
   *
   * @author paouelle
   *
   * @param  declaringClass the non-<code>null</code> class declaring the field
   * @throws IllegalArgumentException if unable to find a suitable getter
   */
  private void findGetter(Class<?> declaringClass) {
    Method getter = findGetterMethod(declaringClass, "get");

    if ((getter == null) && (type == Boolean.class) || (type == Boolean.TYPE)) {
      // try for "is"
      getter = findGetterMethod(declaringClass, "is");
    }
    if (getter != null) {
      final Method g = getter;

      getters.put(cinfo.getObjectClass(), obj -> {
        try {
          return g.invoke(obj);
        } catch (IllegalAccessException e) { // should not happen
          throw new IllegalStateException(declaringClass.getName(), e);
        } catch (InvocationTargetException e) {
          final Throwable t = e.getTargetException();

          if (t instanceof Error) {
            throw (Error)t;
          } else if (t instanceof RuntimeException) {
            throw (RuntimeException)t;
          } else { // we don't expect any of those
            throw new IllegalStateException(declaringClass.getName(), t);
          }
        }
      });
    } else {
      getters.put(cinfo.getObjectClass(), obj -> {
        try {
          return field.get(obj);
        } catch (IllegalAccessException e) { // should not happen
          throw new IllegalStateException(declaringClass.getName(), e);
        }
      });
    }
  }

  /**
   * Finds the getter method from the declaring class suitable to get a
   * reference to the field.
   *
   * @author paouelle
   *
   * @param  declaringClass the non-<code>null</code> class declaring the field
   * @param  prefix the non-<code>null</code> getter prefix to use
   * @return the getter method for the field or <code>null</code> if none found
   * @throws IllegalArgumentException if unable to find a suitable getter
   */
  private Method findGetterMethod(Class<?> declaringClass, String prefix) {
    final String mname = prefix + WordUtils.capitalize(name, '_', '-');

    try {
      final Method m = declaringClass.getDeclaredMethod(mname);
      final int mods = m.getModifiers();

      if (Modifier.isAbstract(mods) || Modifier.isStatic(mods)) {
        return null;
      }
      final Class<?> wtype = ClassUtils.primitiveToWrapper(type);
      final Class<?> wrtype = ClassUtils.primitiveToWrapper(
        DataTypeImpl.unwrapOptionalIfPresent(m.getReturnType(), m.getGenericReturnType())
      );

      org.apache.commons.lang3.Validate.isTrue(
        wtype.isAssignableFrom(wrtype),
        "expecting getter for field '%s' with return type: %s",
        field,
        type.getName()
      );
      m.setAccessible(true);
      return m;
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  /**
   * Finds the setter from the declaring class suitable to set a
   * value for the field.
   *
   * @author paouelle
   *
   * @param  declaringClass the non-<code>null</code> class declaring the field
   * @throws IllegalArgumentException if unable to find a suitable setter
   */
  private void findSetter(Class<?> declaringClass) {
    final String mname = "set" + WordUtils.capitalize(name, '_', '-');

    try {
      final Method m = declaringClass.getDeclaredMethod(mname, field.getType());
      final int mods = m.getModifiers();

      if (Modifier.isAbstract(mods) || Modifier.isStatic(mods)) {
        return;
      }
      org.apache.commons.lang3.Validate.isTrue(
        m.getParameterCount() == 1,
        "expecting setter for field '%s' with one parameter",
        field
      );
      final Class<?> wtype = ClassUtils.primitiveToWrapper(type);
      final Class<?> wptype = ClassUtils.primitiveToWrapper(
        DataTypeImpl.unwrapOptionalIfPresent(m.getParameterTypes()[0], m.getParameters()[0].getParameterizedType())
      );

      if (isOptional) {
        org.apache.commons.lang3.Validate.isTrue(
          wtype.isAssignableFrom(wptype),
          "expecting setter for field '%s' with parameter type: Optional<%s>",
          field,
          type.getName()
        );
      } else {
        org.apache.commons.lang3.Validate.isTrue(
          wtype.isAssignableFrom(wptype),
          "expecting setter for field '%s' with parameter type: %s",
          field,
          type.getName()
        );
      }
      m.setAccessible(true);
      setters.put(cinfo.getObjectClass(), (obj, val) -> {
        try {
          m.invoke(obj, val);
        } catch (IllegalAccessException e) { // should not happen
          throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
          final Throwable t = e.getTargetException();

          if (t instanceof Error) {
            throw (Error)t;
          } else if (t instanceof RuntimeException) {
            throw (RuntimeException)t;
          } else { // we don't expect any of those
            throw new IllegalStateException(t);
          }
        }
      });
    } catch (NoSuchMethodException e) {
      // fallback to the field itself unless it is marked final
      if (!isFinal) {
        setters.put(cinfo.getObjectClass(), (obj, val) -> {
          try {
            field.set(obj, val);
          } catch (IllegalAccessException iae) { // should not happen
            throw new IllegalStateException(iae);
          }
        });
      }
    }
  }

  /**
   * If the field is defined as final then finds and encode its default value.
   *
   * @author paouelle
   *
   * @return the encoded default value for this field or <code>null</code> if the
   *         field is not defined as final
   * @throws IllegalArgumentException if the field is final and we are unable to
   *         instantiate a dummy version of the pojo or access the field's final
   *         value or again we failed to encode it
   *         encode its default value
   */
  private Object findFinalValue() {
    if (isFinal) {
      Object val;

      try {
        val = cinfo.getDefaultValue(field);
      } catch (IllegalArgumentException e) {
        // final field was not introspected by class info (declared class
        // must not be annotated with @Entity)
        // instantiates a dummy version and access its value
        try {
          // find default ctor even if private
          final Constructor<T> ctor = clazz.getDeclaredConstructor();

          ctor.setAccessible(true); // in case it was private
          final T t = ctor.newInstance();

          val = field.get(t);
        } catch (NoSuchMethodException|IllegalAccessException|InstantiationException ee) {
          throw new IllegalArgumentException(
            "unable to instantiate object: " + clazz.getName(), ee
          );
        } catch (InvocationTargetException ee) {
          final Throwable t = ee.getTargetException();

          if (t instanceof Error) {
            throw (Error)t;
          } else if (t instanceof RuntimeException) {
            throw (RuntimeException)t;
          } else { // we don't expect any of those
            throw new IllegalArgumentException(
              "unable to instantiate object: " + clazz.getName(), t
            );
          }
        }
      }
      return val;
    }
    return null;
  }

  /**
   * Gets a registered getter for the specified class or one of its base class.
   *
   * @author paouelle
   *
   * @param  clazz the class for which to get a getter
   * @return the corresponding getter or <code>null</code> if none found
   */
  private Function<Object, Object> getGetter(Class<?> clazz) {
    while (clazz != null) {
      final Function<Object, Object> getter = getters.get(clazz);

      if (getter != null) {
        return getter;
      }
      clazz = clazz.getSuperclass();
    }
    return null;
  }

  /**
   * Gets a registered setter for the specified class or one of its base class.
   *
   * @author paouelle
   *
   * @param  clazz the class for which to get a getter
   * @return the corresponding getter or <code>null</code> if none found
   */
  private BiConsumer<Object, Object> getSetter(Class<?> clazz) {
    while (clazz != null) {
      final BiConsumer<Object, Object> setter = setters.get(clazz);

      if (setter != null) {
        return setter;
      }
      clazz = clazz.getSuperclass();
    }
    return null;
  }

  /**
   * Marks this field as being the last key in the partition or the cluster.
   *
   * @author paouelle
   */
  void setLast() {
    this.isLast = true;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.info.FieldInfo#getObjectClass()
   */
  @Override
  public Class<T> getObjectClass() {
    return clazz;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.info.FieldInfo#getDeclaringClass()
   */
  @Override
  public Class<?> getDeclaringClass() {
    return declaringClass;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.info.FieldInfo#getClassInfo()
   */
  @Override
  public ClassInfo<T> getClassInfo() {
    return cinfo;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.info.FieldInfo#getTableInfo()
   */
  @Override
  public TableInfo<T> getTableInfo() {
    return tinfo;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.info.FieldInfo#getName()
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.info.FieldInfo#getType()
   */
  @Override
  public Class<?> getType() {
    return type;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.info.FieldInfo#isColumn()
   */
  @Override
  public boolean isColumn() {
    return column != null;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.info.FieldInfo#isStatic()
   */
  @Override
  public boolean isStatic() {
    return (column != null) && column.isStatic();
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.info.FieldInfo#getColumnName()
   */
  @Override
  public String getColumnName() {
    return (column != null) ? column.name() : null;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.info.FieldInfo#getKeyspaceKeyName()
   */
  @Override
  public String getKeyspaceKeyName() {
    return (keyspaceKey != null) ? keyspaceKey.name() : null;
  }

  /**
   * Gets the column data type for this field.
   *
   * @author paouelle
   *
   * @return the column data type for this field if it is annotated as a
   *         column; <code>null</code> otherwise
   */
  public DataTypeImpl.Definition getDataType() {
    return definition;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.info.FieldInfo#isKeyspaceKey()
   */
  @Override
  public boolean isKeyspaceKey() {
    return keyspaceKey != null;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.info.FieldInfo#getKeyspaceKey()
   */
  @Override
  public KeyspaceKey getKeyspaceKey() {
    return keyspaceKey;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.info.FieldInfo#isMandatory()
   */
  @Override
  public boolean isMandatory() {
    return mandatory;
  }

  /**
   * Checks if the field is defined as optional.
   *
   * @author paouelle
   *
   * @return <code>true</code> if the field is defined as optional; <code>false</code>
   *         otherwise
   */
  public boolean isOptional() {
    return isOptional;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.info.FieldInfo#isIndex()
   */
  @Override
  public boolean isIndex() {
    return index != null;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.info.FieldInfo#getIndex()
   */
  @Override
  public Index getIndex() {
    return index;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.info.FieldInfo#isCounter()
   */
  @Override
  public boolean isCounter() {
    return (definition != null) ? definition.getMainType() == DataType.COUNTER : false;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.info.FieldInfo#isLast()
   */
  @Override
  public boolean isLast() {
    return isLast;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.info.FieldInfo#isPartitionKey()
   */
  @Override
  public boolean isPartitionKey() {
    return partitionKey != null;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.info.FieldInfo#getPartitionKey()
   */
  @Override
  public PartitionKey getPartitionKey() {
    return partitionKey;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.info.FieldInfo#isClusteringKey()
   */
  @Override
  public boolean isClusteringKey() {
    return clusteringKey != null;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.info.FieldInfo#getClusteringKey()
   */
  @Override
  public ClusteringKey getClusteringKey() {
    return clusteringKey;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.info.FieldInfo#isCaseInsensitiveKey()
   */
  @Override
  public boolean isCaseInsensitiveKey() {
    return ((isPartitionKey() && partitionKey.ignoreCase())
            || (isClusteringKey() && clusteringKey.ignoreCase()));
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.info.FieldInfo#isTypeKey()
   */
  @Override
  public boolean isTypeKey() {
    return typeKey != null;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.info.FieldInfo#getClusteringKey()
   */
  @Override
  public TypeKey getTypeKey() {
    return typeKey;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.info.FieldInfo#isMultiKey()
   */
  @Override
  public boolean isMultiKey() {
    return (multiKeyType != null);
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.info.FieldInfo#getAnnotation(java.lang.Class)
   */
  @Override
  public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
    return field.getAnnotation(annotationClass);
  }

  /**
   * Validate the provided value for this field.
   *
   * @author paouelle
   *
   * @param  value the value to be validated
   * @throws IllegalArgumentException if the specified value is not of the
   *         right type or is <code>null</code> when the field is mandatory
   * @throws ExcludedKeyspaceKeyException if this field is a keyspace key and the
   *         specified value is marked as excluded
   */
  public void validateValue(Object value) {
    if (value instanceof Optional) { // unwrapped optional if present
      value = ((Optional<?>)value).orElse(null);
    }
    if (value == null) {
      org.apache.commons.lang3.Validate.isTrue(
        !isMandatory(),
        "invalid null value for mandatory column '%s'",
        getColumnName()
      );
      if (isPartitionKey() || isClusteringKey()) {
        if (isOptional()) {
          throw new EmptyOptionalPrimaryKeyException(
            "invalid null value for primary key column '"
            + getColumnName()
            + "'"
          );
        }
        throw new IllegalArgumentException(
          "invalid null value for primary key column '"
          + getColumnName()
          + "'"
        );
      }
      org.apache.commons.lang3.Validate.isTrue(
        !isTypeKey(),
        "invalid null value for type key column '%s'",
        getColumnName()
      );
    }
    if (isColumn()) {
      if (value != null) {
        if (!((definition.getMainType() == DataType.BLOB) ? byte[].class : type).isInstance(value)) { // persisted columns will be serialized later
          if (isMultiKey()) {
            // in such case, the value can also be an element of the set
            if (!multiKeyType.isInstance(value)) {
              throw new IllegalArgumentException(
                "invalid value for column '"
                + getColumnName()
                + "'; expecting class '"
                + multiKeyType.getName()
                + "' or '"
                + type.getName()
                + "' but found '"
                + value.getClass().getName()
                + "'"
              );
            }
          } else {
            throw new IllegalArgumentException(
              "invalid value for column '"
              + getColumnName()
              + "'; expecting class '"
              + type.getName()
              + "' but found '"
              + value.getClass().getName()
              + "'"
            );
          }
        }
      }
    } else {
      org.apache.commons.lang3.Validate.isTrue(
        type.isInstance(value),
        "invalid value for keyspace key '%s'; expecting class '%s' but found '%s'",
        this.getKeyspaceKey().name(),
        type.getName(),
        (value != null) ? value.getClass().getName() : "null"
      );
      if (ArrayUtils.contains(keyspaceKey.exclude(), value)) {
        throw new ExcludedKeyspaceKeyException(
          "excluded keyspace key '"
          + keyspaceKey.name()
          + "' value '"
          + value
          + "' for object class: "
          + clazz.getName()
        );
      }
    }
  }

  /**
   * Validate the provided element value for this collection field.
   *
   * @author paouelle
   *
   * @param  codec the codec to use to decode the element
   * @param  value the element value to be validated
   * @throws IllegalArgumentException if the specified value is not of the
   *         right type or is <code>null</code> when the field is mandatory
   */
  private void validateCollectionValue(TypeCodec<?> codec, Object value) {
    org.apache.commons.lang3.Validate.isTrue(
      definition.isCollection(),
      "column '%s' is not a collection", getColumnName()
    );
    if (value == null) {
      org.apache.commons.lang3.Validate.isTrue(
        !isPartitionKey() && !isClusteringKey(),
        "invalid null element value for primary key column '%s'",
        getColumnName()
      );
      org.apache.commons.lang3.Validate.isTrue(
        !isTypeKey(),
        "invalid null element value for type key column '%s'",
        getColumnName()
      );
    }
    org.apache.commons.lang3.Validate.isTrue(
      codec.accepts(value),
      "invalid element value for column '%s'; expecting type '%s': %s",
      getColumnName(), codec.getCqlType().getName(), value
    );
  }

  /**
   * Gets the default codec for this field.
   *
   * @author paouelle
   *
   * @return the default codec for this field
   */
  public TypeCodec<?> getDefaultCodec() {
    return icodec;
  }

  /**
   * Gets a codec for this field for a given keyspace.
   *
   * @author paouelle
   *
   * @param  keyspace the keyspace for which to get a codec
   * @return a suitable codec for this field and for the given keyspace
   */
  public TypeCodec<?> getCodec(String keyspace) {
    final String ks = (keyspace != null) ? keyspace : "";

    if (codecs == null) {
      throw new IllegalStateException("should not be called");
    }
    if (field == null) {
      return codecs.compute(ks, (k, old) -> {
        if (old == null) {
          old = definition.getCodec(k, clazz, cinfo.mgr.getCodecRegistry());
        }
        return old;
      });
    } else if (isColumn()) {
      return codecs.compute(ks, (k, old) -> {
        if (old == null) {
          old = definition.getCodec(
            k,
            field,
            isMandatory() || isPartitionKey() || isClusteringKey(),
            cinfo.mgr.getCodecRegistry()
          );
        }
        return old;
      });
    }
    throw new IllegalStateException("should not be called");
  }

  /**
   * Validate the provided element value for this collection field.
   *
   * @author paouelle
   *
   * @param  value the element value to be validated
   * @throws IllegalArgumentException if the specified value is not of the
   *         right type or is <code>null</code> when the column is mandatory
   */
  public void validateCollectionValue(Object value) {
    if (icodec instanceof ArgumentsCodec) {
      validateCollectionValue(((ArgumentsCodec<?>)icodec).codec(0), value);
    }
  }

  /**
   * Validate the provided value for this list field.
   *
   * @author paouelle
   *
   * @param  value the element value to be validated
   * @throws IllegalArgumentException if the specified value is not of the
   *         right element type or the value is <code>null</code> when the
   *         column is mandatory
   */
  public void validateListValue(Object value) {
    validateCollectionValue(value);
  }

  /**
   * Validate the provided value for this set field.
   *
   * @author paouelle
   *
   * @param  value the element value to be validated
   * @throws IllegalArgumentException if the specified value is not of the
   *         right element type or the value is <code>null</code> when the
   *         column is mandatory
   */
  public void validateSetValue(Object value) {
    validateCollectionValue(value);
  }

  /**
   * Validate the provided mapping key/value for this map field.
   *
   * @author paouelle
   *
   * @param  key the mapping key to be validated
   * @param  value the mapping value to be validated
   * @throws IllegalArgumentException if the specified key/value are not
   *         of the right mapping types or the value is <code>null</code>
   *         when the column is mandatory
   */
  public void validateMapKeyValue(Object key, Object value) {
    if (icodec instanceof ArgumentsCodec) {
      validateCollectionValue(((ArgumentsCodec<?>)icodec).codec(1), value);
    }
    validateMapKey(key);
  }

  /**
   * Validate the provided mapping key for this map field.
   *
   * @author paouelle
   *
   * @param  key the mapping key to be validated
   * @throws IllegalArgumentException if the specified key is not
   *         of the right mapping types
   */
  public void validateMapKey(Object key) {
    if (icodec instanceof ArgumentsCodec) {
      final TypeCodec<?> kcodec = ((ArgumentsCodec<?>)icodec).codec(0);

      org.apache.commons.lang3.Validate.isTrue(
        kcodec.accepts(key),
        "invalid element key for column '%s'; expecting type '%s': %s",
        getColumnName(), kcodec.getCqlType().getName(), key
      );
    }
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.info.FieldInfo#isFinal()
   */
  @Override
  public boolean isFinal() {
    return isFinal;
  }

  /**
   * Gets the final value for the field if it is defined as final.
   *
   * @author paouelle
   *
   * @return the final value for the field if defined as final; <code>null</code>
   *         otherwise
   */
  public Object getFinalValue() {
    return finalValue;
  }

  /**
   * Retrieves the field's encoded value from the specified POJO.
   *
   * @author paouelle
   *
   * @param  object the POJO from which to retrieve the field's value
   * @return the POJO's field value
   * @throws NullPointerException if <code>object</code> is <code>null</code>
   */
  public Object getValue(T object) {
    return getValue(Object.class, object);
  }

  /**
   * Retrieves the field's encoded value from the specified POJO.
   *
   * @author paouelle
   *
   * @param  clazz the class for the expected value
   * @param  object the POJO from which to retrieve the field's value
   * @return the POJO's field value
   * @throws NullPointerException if <code>object</code> is <code>null</code>
   * @throws ClassCastException if the field value from the given object
   *         cannot be type casted to the specified class
   */
  @SuppressWarnings("unchecked")
  public Object getValue(Class<?> clazz, T object) {
    org.apache.commons.lang3.Validate.notNull(object, "invalid null object");
    final Function<Object, Object> getter = getGetter(object.getClass());
    Object val = (getter != null) ? getter.apply(object) : null;

    if (val instanceof Optional) {
      val = ((Optional<?>)val).orElse(null);
    }
    val = clazz.cast(val);
    if (isTypeKey()) { // this is the type key
      final String type;

      // the value is fixed by the schema so get it from the type class info
      if (cinfo instanceof TypeClassInfoImpl) {
        type = ((TypeClassInfoImpl<T>)cinfo).getType();
      } else if (cinfo instanceof RootClassInfoImpl) {
        type = ((RootClassInfoImpl<T>)cinfo).getType((Class<? extends T>)object.getClass()).getType();
      } else if (cinfo instanceof UDTTypeClassInfoImpl) {
        type = ((UDTTypeClassInfoImpl<T>)cinfo).getType();
      } else {
        type = ((UDTRootClassInfoImpl<T>)cinfo).getType((Class<? extends T>)object.getClass()).getType();
      }
      if (!type.equals(val)) { // force value to the type and re-update in pojo
        setValue(object, type);
        val = clazz.cast(type);
      }
    }
    return val;
  }

  /**
   * Sets the field's value in the specified POJO with the given value.
   *
   * @author paouelle
   *
   * @param  object the POJO in which to set the field's value
   * @param  value the value to set the field with
   * @throws NullPointerException if <code>object</code> is <code>null</code>
   *         or if the column is a primary key or mandatory and
   *         <code>value</code> is <code>null</code>
   */
  public void setValue(T object, Object value) {
    org.apache.commons.lang3.Validate.notNull(object, "invalid null object");
    final BiConsumer<Object, Object> setter = getSetter(object.getClass());

    // nothing to update if no setter was defined
    if (setter == null) {
      return;
    }
    if (isOptional) {
      value = Optional.ofNullable(value);
    }
    if (value == null) {
      org.apache.commons.lang3.Validate.isTrue(
        !isMandatory(),
        "invalid null value for mandatory column '%s'",
        getColumnName()
      );
      org.apache.commons.lang3.Validate.isTrue(
        !isPartitionKey() && !isClusteringKey(),
        "invalid null value for primary key column '%s'",
        getColumnName()
      );
      org.apache.commons.lang3.Validate.isTrue(
        !isTypeKey(),
        "invalid null value for type key column '%s'",
        getColumnName()
      );
    }
    setter.accept(object, value);
  }

  /**
   * Decodes the field's specified value.
   *
   * @author paouelle
   *
   * @param  val the field value to decode
   * @param  failOnlyIfMandatory <code>false</code> not to fail if the value is
   *         <code>null</code> and the column is not mandatory; otherwise fails
   *         if the value is <code>null</code>
   * @param  trace a trace string indicating where the value was extracted
   * @return the decoded value for this field
   * @throws ObjectMissingException if the column value is not defined and is
   *         mandatory
   */
  public Object verifyValue(
    Object val, boolean failOnlyIfMandatory, String trace
  ) {
    if (val == null) {
      if (isPartitionKey()) {
        throw new ObjectMissingException(
          clazz,
          "missing partition key '"
          + getColumnName()
          +  "' from "
          + trace
          + " for field '"
          + declaringClass.getName()
          + "."
          + name
          + "'"
        );
      }
      if (isClusteringKey()) {
        throw new ObjectMissingException(
          clazz,
          "missing clustering key '"
          + getColumnName()
          +  "' from "
          + trace
          + " for field '"
          + declaringClass.getName()
          + "."
          + name
          + "'"
        );
      }
      if (isTypeKey()) {
        throw new ObjectMissingException(
          clazz,
          "missing type key '"
          + getColumnName()
          +  "' from "
          + trace
          + " for field '"
          + declaringClass.getName()
          + "."
          + name
          + "'"
        );
      }
      if (isMandatory()) {
        throw new ObjectMissingException(
          clazz,
          "missing mandatory column '"
          + getColumnName()
          + "' from "
          + trace
          + " for field '"
          + declaringClass.getName()
          + "."
          + name
          + "'"
        );
      }
      if (failOnlyIfMandatory) {
        throw new ObjectMissingException(
          clazz,
          "missing column '"
          + getColumnName()
          + "' from "
          + trace
          + " for field '"
          + declaringClass.getName()
          + "."
          + name
          + "'"
        );
      }
    }
    return val;
  }

  /**
   * Decodes the field's value based on the given row.
   *
   * @author paouelle
   *
   * @param  row the row where the column encoded value is defined
   * @return the decoded value for this field from the given row
   * @throws NullPointerException if <code>row</code> is  <code>null</code>
   * @throws ObjectConversionException if unable to decode the column
   * @throws ObjectMissingException if the column value is not defined and is
   *         mandatory
   */
  public Object decodeValue(Row row) {
    org.apache.commons.lang3.Validate.notNull(row, "invalid null row");
    Object val;

    try {
      // check if the column is defined in the row
      final int index = row.getColumnDefinitions().getIndexOf(getColumnName());

      if (index != -1) {
        val = verifyValue(
          row.get(index, getCodec(row.getColumnDefinitions().getKeyspace(index))),
          false,
          "result set"
        );
      } else {
        val = verifyValue(null, true, "result set");
      }
    } catch (ObjectConversionException e) {
      e.setRow(row);
      throw e;
    } catch (IllegalArgumentException|InvalidTypeException e) {
      throw new ObjectConversionException(
        clazz,
        row,
        "unable to decode value for field '"
        + declaringClass.getName()
        + "."
        + name
        + "'",
        e
      );
    }
    return val;
  }

  /**
   * Decodes the field's value based on the given UDT value.
   *
   * @author paouelle
   *
   * @param  uval the UDT value where the column encoded value is defined
   * @return the decoded value for this field from the given UDT value
   * @throws NullPointerException if <code>uval</code> is <code>null</code>
   * @throws ObjectConversionException if unable to decode the column
   * @throws ObjectMissingException if the column value is not defined and is
   *         mandatory
   */
  public Object decodeValue(UDTValue uval) {
    org.apache.commons.lang3.Validate.notNull(uval, "invalid null UDT value");
    Object val;

    try {
      // check if the column is defined in the row
      if (uval.getType().contains(getColumnName())) {
        val = verifyValue(
          uval.get(
            getColumnName(),
            getCodec(uval.getType().getKeyspace())
          ),
          false,
          "UDT value"
        );
      } else {
        val = verifyValue(null, true, "UDT value");
      }
    } catch (ObjectConversionException e) {
      e.setUDTValue(uval);
      throw e;
    } catch (IllegalArgumentException|InvalidTypeException e) {
      throw new ObjectConversionException(
        clazz,
        uval,
        "unable to decode value for field '"
        + declaringClass.getName()
        + "."
        + name
        + "'",
        e
      );
    }
    return val;
  }

  /**
   * Decodes the field's value based on the given formated values.
   *
   * @author paouelle
   *
   * @param  keyspace the keyspace for which to create the object
   * @param  values the formated values to convert into a POJO
   * @return the decoded value for this field from the given UDT value
   * @throws ObjectConversionException if unable to decode the column
   * @throws ObjectMissingException if the column value is not defined and is
   *         mandatory
   */
  public Object decodeValue(String keyspace, Map<String, String> values) {
    Object val;

    try {
      // check if the column is defined in the row
      if (values.containsKey(getColumnName())) {
        val = verifyValue(
          getCodec(keyspace).parse(values.get(getColumnName())),
          false,
          "formatted values"
        );
      } else {
        val = verifyValue(null, true, "formatted values");
      }
    } catch (ObjectConversionException e) {
      e.setValues(values);
      throw e;
    } catch (IllegalArgumentException|InvalidTypeException e) {
      throw new ObjectConversionException(
        clazz,
        values,
        "unable to decode value for field '"
        + declaringClass.getName()
        + "."
        + name
        + "'",
        e
      );
    }
    return val;
  }
  /**
   * Decodes and sets the field's value in the specified POJO based on the given
   * row.
   *
   * @author paouelle
   *
   * @param  object the POJO in which to set the field's decoded value
   * @param  row the row where the column encoded value is defined
   * @throws NullPointerException if <code>object</code> or <code>row</code> is
   *         <code>null</code>
   * @throws ObjectConversionException if unable to decode the column and store
   *         the corresponding value into the POJO object
   * @throws ObjectMissingException if the column value is not defined and is
   *         mandatory
   */
  public void decodeAndSetValue(T object, Row row) {
    org.apache.commons.lang3.Validate.notNull(object, "invalid null object");
    Object val;

    try {
      val = decodeValue(row);
    } catch (ObjectMissingException e) {
      // verify if the column is not mandatory in which case we just skip it
      if (isPartitionKey() || isClusteringKey() || isTypeKey() || isMandatory()) {
        throw e;
      } // else - not mandatory and not defined in the row so skip it
      return;
    }
    try {
      setValue(object, val);
    } catch (NullPointerException|IllegalArgumentException e) {
      throw new ObjectConversionException(
        clazz,
        row,
        "unable to set field '"
        + declaringClass.getName()
        + "."
        + name
        + "' with: "
        + val,
        e
      );
    }
  }

  /**
   * Decodes and sets the field's value in the specified POJO based on the given
   * UDT value.
   *
   * @author paouelle
   *
   * @param  object the POJO in which to set the field's decoded value
   * @param  uval the UDT value where the column encoded value is defined
   * @throws NullPointerException if <code>object</code> or <code>uval</code> is
   *         <code>null</code>
   * @throws ObjectConversionException if unable to decode the column and store
   *         the corresponding value into the POJO object
   * @throws ObjectMissingException if the column value is not defined and is
   *         mandatory
   */
  public void decodeAndSetValue(T object, UDTValue uval) {
    org.apache.commons.lang3.Validate.notNull(object, "invalid null object");
    Object val;

    try {
      val = decodeValue(uval);
    } catch (ObjectMissingException e) {
      // verify if the column is not mandatory in which case we just skip it
      if (isMandatory()) {
        throw e;
      } // else - not mandatory and not defined in the row so skip it
      return;
    }
    try {
      setValue(object, val);
    } catch (NullPointerException|IllegalArgumentException e) {
      throw new ObjectConversionException(
        clazz,
        uval,
        "unable to set field '"
        + declaringClass.getName()
        + "."
        + name
        + "' with: "
        + val,
        e
      );
    }
  }

  /**
   * Decodes and sets the field's value in the specified POJO based on the given
   * UDT value.
   *
   * @author paouelle
   *
   * @param  object the POJO in which to set the field's decoded value
   * @param  keyspace the keyspace for which to create the object
   * @param  values the formated values to convert into a POJO
   * @throws NullPointerException if <code>object</code> is <code>null</code>
   * @throws ObjectConversionException if unable to decode the column and store
   *         the corresponding value into the POJO object
   * @throws ObjectMissingException if the column value is not defined and is
   *         mandatory
   */
  public void decodeAndSetValue(
    T object, String keyspace, Map<String, String> values
  ) {
    org.apache.commons.lang3.Validate.notNull(object, "invalid null object");
    Object val;

    try {
      val = decodeValue(keyspace, values);
    } catch (ObjectMissingException e) {
      // verify if the column is not mandatory in which case we just skip it
      if (isMandatory()) {
        throw e;
      } // else - not mandatory and not defined in the row so skip it
      return;
    }
    try {
      setValue(object, val);
    } catch (NullPointerException|IllegalArgumentException e) {
      throw new ObjectConversionException(
        clazz,
        values,
        "unable to set field '"
        + declaringClass.getName()
        + "."
        + name
        + "' with: "
        + val,
        e
      );
    }
  }

  /**
   * Verifies and sets the specified field's value in the specified POJO.
   *
   * @author paouelle
   *
   * @param  object the POJO in which to set the field's decoded value
   * @param  trace a trace string indicating where the value was extracted
   * @param  val the value to verify and set
   * @throws NullPointerException if <code>object</code> is <code>null</code>
   * @throws ObjectConversionException if unable to decode the column and store
   *         the corresponding value into the POJO object
   * @throws ObjectMissingException if the column value is not defined and is
   *         mandatory
   */
  public void verifyAndSetValue(T object, Object val, String trace) {
    org.apache.commons.lang3.Validate.notNull(object, "invalid null object");
    try {
      val = verifyValue(val, true, trace);
    } catch (ObjectMissingException e) {
      // verify if the column is not mandatory in which case we just skip it
      if (isMandatory()) {
        throw e;
      } // else - not mandatory and not defined in the row so skip it
      return;
    }
    try {
      setValue(object, val);
    } catch (NullPointerException|IllegalArgumentException e) {
      throw new ObjectConversionException(
        clazz,
        "unable to set field '"
        + declaringClass.getName()
        + "."
        + name
        + "' with: "
        + val,
        e
      );
    }
  }

  /**
   * Gets all user-defined types this field is dependent on.
   *
   * @author paouelle
   *
   * @return a stream of all class infos for the user-defined types this field
   *         depends on
   */
  public Stream<UDTClassInfoImpl<?>> udts() {
    return (definition != null) ? definition.udts() : Stream.empty();
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    final ReflectionToStringBuilder sb = new ReflectionToStringBuilder(
      this, ToStringStyle.SHORT_PREFIX_STYLE
    );

    sb.setAppendTransients(true);
    sb.setExcludeFieldNames("cinfo", "tinfo");
    return sb.toString();
  }
}
