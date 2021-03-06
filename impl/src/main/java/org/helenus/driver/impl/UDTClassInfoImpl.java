/*
 * Copyright (C) 2015-2016 The Helenus Driver Project Authors.
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
import java.lang.reflect.InvocationTargetException;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Triple;

import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.TypeCodec;
import com.datastax.driver.core.UDTValue;
import com.datastax.driver.core.UserType;

import org.helenus.commons.lang3.reflect.ReflectionUtils;
import org.helenus.driver.ObjectConversionException;
import org.helenus.driver.info.TableInfo;
import org.helenus.driver.persistence.CQLDataType;
import org.helenus.driver.persistence.DataType;
import org.helenus.driver.persistence.UDTEntity;
import org.helenus.driver.persistence.UDTRootEntity;
import org.helenus.driver.persistence.UDTTypeEntity;

/**
 * The <code>UDTClassInfoImpl</code> class provides information about a
 * particular POJO class.
 *
 * @copyright 2015-2016 The Helenus Driver Project Authors
 *
 * @author  The Helenus Driver Project Authors
 * @version 1 - Mar 3, 2015 - paouelle - Creation
 *
 * @param <T> The type of POJO represented by this class
 *
 * @since 1.0
 */
public abstract class UDTClassInfoImpl<T>
  extends ClassInfoImpl<T> implements CQLDataType {
  /**
   * Holds the reserved user-defined type names.
   *
   * @author paouelle
   */
  private final static String[] RESERVED_UDT_NAMES = {
    "byte",
    "smallint",
    "tinyint",
    "complex",
    "enum",
    "date",
    "time",
    "interval",
    "macaddr",
    "bitstring"
  };

  /**
   * The <code>POJOContext</code> class provides a specific context for the POJO
   * as referenced while building an insert or update statement.
   *
   * @copyright 2015-2015 The Helenus Driver Project Authors
   *
   * @author  The Helenus Driver Project Authors
   * @version 1 - Mar 7, 2015 - paouelle - Creation
   *
   * @since 1.0
   */
  public class POJOContext extends ClassInfoImpl<T>.POJOContext {
    /**
     * Instantiates a new <code>POJOContext</code> object.
     *
     * @author paouelle
     *
     * @param  object the POJO object
     * @throws NullPointerException if <code>object</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>object</code> is not of the
     *         appropriate class
     */
    public POJOContext(T object) {
      super(object);
    }

    /**
     * {@inheritDoc}
     *
     * @author paouelle
     *
     * @see org.helenus.driver.impl.ClassInfoImpl.POJOContext#populateKeyspaceKeys(java.util.Map)
     */
    @Override
    protected final void populateKeyspaceKeys(Map<String, FieldInfoImpl<T>> kkeysFields) {}

    /**
     * Retrieves all columns and their values from the POJO.
     *
     * @author paouelle
     *
     * @return a non-<code>null</code> map of all column/value pairs for the POJO
     * @throws IllegalArgumentException if a mandatory column is missing from the POJO
     */
    @SuppressWarnings("synthetic-access")
    public Map<String, Triple<Object, CQLDataType, TypeCodec<?>>> getColumnValues() {
      if (table == null) { // table not defined so nothing to return; should not happen
        return Collections.emptyMap();
      }
      return table.getColumnValues(getKeyspace(), object);
    }

    /**
     * {@inheritDoc}
     *
     * @author paouelle
     *
     * @see org.helenus.driver.impl.ClassInfoImpl.POJOContext#getColumnValues(java.lang.String)
     */
    @Override
    public final Map<String, Triple<Object, CQLDataType, TypeCodec<?>>> getColumnValues(
      String tname
    ) {
      throw new IllegalArgumentException("user-defined types do not define tables");
    }

    /**
     * {@inheritDoc}
     *
     * @author paouelle
     *
     * @see org.helenus.driver.impl.ClassInfoImpl.POJOContext#getPartitionKeyColumnValues(java.lang.String)
     */
    @Override
    public final Map<String, Triple<Object, CQLDataType, TypeCodec<?>>> getPartitionKeyColumnValues(
      String tname
    ) {
      throw new IllegalArgumentException("user-defined types do not define tables");
    }

    /**
     * {@inheritDoc}
     *
     * @author paouelle
     *
     * @see org.helenus.driver.impl.ClassInfoImpl.POJOContext#getKeyspaceAndPartitionKeyColumnValues(java.lang.String)
     */
    @Override
    public final Map<String, Triple<Object, CQLDataType, TypeCodec<?>>> getKeyspaceAndPartitionKeyColumnValues(
      String tname
    ) {
      throw new IllegalArgumentException("user-defined types do not define tables");
    }

    /**
     * {@inheritDoc}
     *
     * @author paouelle
     *
     * @see org.helenus.driver.impl.ClassInfoImpl.POJOContext#getPrimaryKeyColumnValues(java.lang.String)
     */
    @Override
    public final Map<String, Triple<Object, CQLDataType, TypeCodec<?>>> getPrimaryKeyColumnValues(
      String tname
    ) {
      throw new IllegalArgumentException("user-defined types do not define tables");
    }

    /**
     * {@inheritDoc}
     *
     * @author paouelle
     *
     * @see org.helenus.driver.impl.ClassInfoImpl.POJOContext#getKeyspaceAndPrimaryKeyColumnValues(java.lang.String)
     */
    @Override
    public final Map<String, Triple<Object, CQLDataType, TypeCodec<?>>> getKeyspaceAndPrimaryKeyColumnValues(
      String tname
    ) {
      throw new IllegalArgumentException("user-defined types do not define tables");
    }

    /**
     * Retrieves all mandatory columns and their values from the POJO.
     *
     * @author paouelle
     *
     * @return a non-<code>null</code> map of all mandatory column/value pairs
     *         for the POJO
     * @throws IllegalArgumentException if a column is missing from the POJO
     */
    @SuppressWarnings("synthetic-access")
    public Map<String, Triple<Object, CQLDataType, TypeCodec<?>>> getMandatoryColumnValues() {
      if (table == null) { // table not defined so nothing to return; should not happen
        return Collections.emptyMap();
      }
      return table.getMandatoryAndPrimaryKeyColumnValues(getKeyspace(), object);
    }

     /**
     * {@inheritDoc}
     *
     * @author paouelle
     *
     * @see org.helenus.driver.impl.ClassInfoImpl.POJOContext#getMandatoryAndPrimaryKeyColumnValues(java.lang.String)
     */
    @Override
    public final Map<String, Triple<Object, CQLDataType, TypeCodec<?>>> getMandatoryAndPrimaryKeyColumnValues(
      String tname
    ) {
      throw new IllegalArgumentException("user-defined types do not define tables");
    }

    /**
     * Retrieves the specified column value from the POJO.
     *
     * @author paouelle
     *
     * @param  name the name of the column to retrieve
     * @return the column value for the POJO
     * @throws IllegalArgumentException if the column name is not defined by the
     *         POJO or is mandatory and missing from the POJO
     */
    @SuppressWarnings("synthetic-access")
    public Triple<Object, CQLDataType, TypeCodec<?>> getColumnValue(
      CharSequence name
    ) {
      if (table == null) { // table not defined so nothing to return; should not happen
        return Triple.of(null, null, null);
      }
      return table.getColumnValue(getKeyspace(), object, name);
    }

    /**
     * {@inheritDoc}
     *
     * @author paouelle
     *
     * @see org.helenus.driver.impl.ClassInfoImpl.POJOContext#getColumnValue(java.lang.String, java.lang.CharSequence)
     */
    @Override
    public final Triple<Object, CQLDataType, TypeCodec<?>> getColumnValue(
      String tname, CharSequence name
    ) {
      throw new IllegalArgumentException("user-defined types do not define tables");
    }

    /**
     * Retrieves the specified columns and their values from the POJO.
     *
     * @author paouelle
     *
     * @param  names the names of the columns to retrieve
     * @return a non-<code>null</code> map of all requested column/value pairs
     *         for the POJO
     * @throws IllegalArgumentException if any of the column names are not defined
     *         by the POJO or is mandatory and missing from the POJO
     */
    @SuppressWarnings("synthetic-access")
    public Map<String, Triple<Object, CQLDataType, TypeCodec<?>>> getColumnValues(
      Iterable<CharSequence> names
    ) {
      if (table == null) { // table not defined so nothing to return; should not happen
        return Collections.emptyMap();
      }
      return table.getColumnValues(getKeyspace(), object, names);
    }

    /**
     * {@inheritDoc}
     *
     * @author paouelle
     *
     * @see org.helenus.driver.impl.ClassInfoImpl.POJOContext#getColumnValues(java.lang.String, java.lang.Iterable)
     */
    @Override
    public final Map<String, Triple<Object, CQLDataType, TypeCodec<?>>> getColumnValues(
      String tname, Iterable<CharSequence> names
    ) {
      throw new IllegalArgumentException("user-defined types do not define tables");
    }

    /**
     * Retrieves the specified columns and their values from the POJO.
     *
     * @author paouelle
     *
     * @param  names the names of the columns to retrieve
     * @return a non-<code>null</code> map of all requested column/value pairs
     *         for the POJO
     * @throws IllegalArgumentException if any of the column names are not defined
     *         by the POJO or is mandatory and missing from the POJO
     */
    @SuppressWarnings("synthetic-access")
    public Map<String, Triple<Object, CQLDataType, TypeCodec<?>>> getColumnValues(
      CharSequence... names
    ) {
      if (table == null) { // table not defined so nothing to return; should not happen
        return Collections.emptyMap();
      }
      return table.getColumnValues(getKeyspace(), object, names);
    }

    /**
     * {@inheritDoc}
     *
     * @author paouelle
     *
     * @see org.helenus.driver.impl.ClassInfoImpl.POJOContext#getColumnValues(java.lang.String, java.lang.CharSequence[])
     */
    @Override
    public final Map<String, Triple<Object, CQLDataType, TypeCodec<?>>> getColumnValues(
      String tname, CharSequence... names
    ) {
      throw new IllegalArgumentException("user-defined types do not define tables");
    }
  }

  /**
   * Holds the name for the user-defined type represented by this class.
   *
   * @author paouelle
   */
  private final String name;

  /**
   * Holds the fake table used to represent the user-defined type columns.
   *
   * @author paouelle
   */
  private final TableInfoImpl<T> table;

  /**
   * Holds the codecs for this UDT keyed per keyspace.
   *
   * @author paouelle
   */
  private final Map<String, UDTCodecImpl<T>> codecs;

  /**
   * Instantiates a new <code>UDTClassInfoImpl</code> object.
   *
   * @author paouelle
   *
   * @param  mgr the non-<code>null</code> statement manager
   * @param  clazz the class of POJO for which to get a class info object for
   * @param  entityAnnotationClass the non-<code>null</code> entity annotation
   *         class to compute from
   * @throws NullPointerException if <code>clazz</code> is <code>null</code>
   * @throws IllegalArgumentException if <code>clazz</code> doesn't represent
   *         a valid POJO class
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  protected UDTClassInfoImpl(
    StatementManagerImpl mgr,
    Class<T> clazz,
    Class<? extends Annotation> entityAnnotationClass
  ) {
    super(mgr, clazz, entityAnnotationClass);
    this.name = findName();
    this.table = tablesImpl().findFirst().get();
    this.codecs = new ConcurrentHashMap<>(8);
    // handle special UDT types that extends List, Set, or Map
    if (List.class.isAssignableFrom(clazz)) {
      table.addNonPrimaryColumn(
        new FieldInfoImpl<>(mgr, this, DataType.LIST, (obj, val) -> {
          final List l = (List)obj;

          l.clear();
          l.addAll((Collection)val);
        })
      );
    } else if (Set.class.isAssignableFrom(clazz)) {
      table.addNonPrimaryColumn(
        new FieldInfoImpl<>(
          mgr,
          this,
          (LinkedHashSet.class.isAssignableFrom(clazz)
           ? DataType.ORDERED_SET
           : (SortedSet.class.isAssignableFrom(clazz) ? DataType.SORTED_SET : DataType.SET)),
          (obj, val) -> {
            final Set s = (Set)obj;

            s.clear();
            s.addAll((Collection)val);
          }
        )
      );
    } else if (Map.class.isAssignableFrom(clazz)) {
      table.addNonPrimaryColumn(
        new FieldInfoImpl<>(
          mgr,
          this,
          SortedMap.class.isAssignableFrom(clazz) ? DataType.SORTED_MAP : DataType.MAP,
          (obj, val) -> {
            final Map m = (Map)obj;

            m.clear();
            m.putAll((Map)val);
          }
        )
      );
    }
  }

  /**
   * Instantiates a new <code>UDTClassInfoImpl</code> object.
   *
   * @author paouelle
   *
   * @param  cinfo the non-<code>null</code> class info to link to
   * @param  clazz the class of POJO for which to get a class info object for
   * @throws NullPointerException if <code>clazz</code> is <code>null</code>
   * @throws IllegalArgumentException if <code>clazz</code> doesn't represent
   *         a valid POJO class
   */
  protected UDTClassInfoImpl(UDTClassInfoImpl<T> cinfo, Class<T> clazz) {
    super(cinfo, clazz);
    this.name = cinfo.name;
    this.table = cinfo.table;
    this.codecs = cinfo.codecs;
  }

  /**
   * Finds the annotated type name for this POJO class.
   *
   * @author paouelle
   *
   * @return the non-<code>null</code> annotated type name
   * @throws IllegalArgumentException if the POJO class is improperly annotated
   */
  private String findName() {
    final UDTEntity ue = clazz.getAnnotation(UDTEntity.class);
    final String name;

    if (ue != null) {
      name = ue.name();
    } else {
      final UDTTypeEntity ute = clazz.getAnnotation(UDTTypeEntity.class);

      if (ute != null) {
        name = ute.name();
      } else {
        final UDTRootEntity ure = ReflectionUtils.findFirstAnnotation(clazz, UDTRootEntity.class);

        if (ure != null) {
          name = ure.name();
        } else {
          name = null;
        }
      }
    }
    org.apache.commons.lang3.Validate.isTrue(
      name != null,
      "class '%s' is not annotated with @UDTEntity or @UDTRootEntity", clazz.getSimpleName()
    );
    org.apache.commons.lang3.Validate.isTrue(
      !ArrayUtils.contains(UDTClassInfoImpl.RESERVED_UDT_NAMES, name),
      "user-defined type name cannot be a reserved type name: %s", name
    );
    return name;
  }

  /**
   * Decodes the column fields from a UDT value and sets the decoded value in
   * the POJO object.
   *
   * @author paouelle
   *
   * @param  object the non-<code>null</code> POJO object
   * @param  uval the non-<code>null</code> UDT value being decoded to the POJO
   * @throws ObjectConversionException if unable to decode the UDT value in the
   *         POJO
   */
  private void decodeAndSetColumnFields(T object, UDTValue uval) {
    // get the table for this UDT
    final TableInfoImpl<T> table = getTableImpl();

    if (table != null) {
      for (final UserType.Field coldef: uval.getType()) {
        // find the field in the table for this column
        final FieldInfoImpl<T> field = table.getColumnImpl(coldef.getName());

        if (field != null) {
          // now let's set the value for this column
          field.decodeAndSetValue(object, uval);
        }
      }
    }
  }

  /**
   * Decodes the column fields from a set of formatted values and sets the parsed
   * value in the POJO object.
   *
   * @author paouelle
   *
   * @param  object the non-<code>null</code> POJO object
   * @param  keyspace the keyspace for which to create the object
   * @param  values the formated values to convert into a POJO
   * @throws ObjectConversionException if unable to decode the UDT value in the
   *         POJO
   */
  private void decodeAndSetColumnFields(
    T object, String keyspace, Map<String, String> values
  ) {
    // get the table for this UDT
    final TableInfoImpl<T> table = getTableImpl();

    if (table != null) {
      table.getColumnsImpl().forEach(finfo -> finfo.decodeAndSetValue(object, keyspace, values));
    }
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.impl.ClassInfoImpl#getTablesImpl()
   */
  @Override
  protected Collection<TableInfoImpl<T>> getTablesImpl() {
    return Collections.emptyList();
  }

  /**
   * Gets a codec for this user data type.
   *
   * @author paouelle
   *
   * @param  keyspace the keyspace for which to get a codec
   * @return a suitable codec for this user data type
   */
  protected UDTCodecImpl<T> getCodec(String keyspace) {
    return codecs.compute(keyspace, (k, old) -> {
      if (old == null) {
        // check if we can find the real definition for the keyspace as long as the session is opened
        if (mgr.getSession() != null) {
          final KeyspaceMetadata km = mgr.getCluster().getMetadata().getKeyspace(keyspace);

          if (km != null) {
            final UserType definition = km.getUserType(name);

            if (definition != null) { // use cluster definition
              return new UDTCodecImpl<>(this, definition);
            }
          }
        }
        // initialize it with a default
        old = new UDTCodecImpl<>(this, k);
      }
      return old;
    });
  }

  /**
   * Registers a new cluster-defined definition for this user data type.
   *
   * @author paouelle
   *
   * @param definition the new user definition for this the user type to register
   */
  protected void register(UserType definition) {
    codecs.compute(definition.getKeyspace(), (k, old) -> {
      if (old == null) {
        old = new UDTCodecImpl<>(this, definition);
      } else {
        old.register(definition);
      }
      return old;
    });
  }

  /**
   * Deregisters the corresponding definition for this user data type.
   *
   * @author paouelle
   *
   * @param definition the definition for the user type to deregister
   */
  protected void deregister(UserType definition) {
    final UDTCodecImpl<T> ucodec = codecs.get(definition.getKeyspace());

    if (ucodec != null) {
      ucodec.deregister(definition);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.persistence.CQLDataType#name()
   */
  @Override
  public String name() {
    return name;
  }

  /**
   * Gets a name representation for this data type.
   *
   * @author paouelle
   *
   * @return a non-<code>null</code> name representation for this data type
   */
  public String getName() {
    return name;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.persistence.CQLDataType#isFrozen()
   */
  @Override
  public boolean isFrozen() {
    return true;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.persistence.CQLDataType#isCollection()
   */
  @Override
  public boolean isCollection() {
    return false;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.persistence.CQLDataType#isTuple()
   */
  @Override
  public boolean isTuple() {
    return false;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.persistence.CQLDataType#isUserDefined()
   */
  @Override
  public boolean isUserDefined() {
    return true;
  }

  /**
   * Gets the corresponding default Cassandra user type.
   *
   * @author paouelle
   *
   * @param  keyspace the keyspace for which to get a user type
   * @return the non-<code>null</code> corresponding Cassandra user type
   */
  public UserType getUserType(String keyspace) {
    return getCodec(keyspace).getUserType();
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.persistence.CQLDataType#getDataType()
   */
  @Override
  public com.datastax.driver.core.DataType getDataType() {
    return getUserType("");
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.persistence.CQLDataType#getMainType()
   */
  @Override
  public CQLDataType getMainType() {
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.persistence.CQLDataType#getElementType()
   */
  @Override
  public CQLDataType getElementType() {
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.persistence.CQLDataType#getArgumentTypes()
   */
  @Override
  public List<CQLDataType> getArgumentTypes() {
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.persistence.CQLDataType#getFirstArgumentType()
   */
  @Override
  public CQLDataType getFirstArgumentType() {
    return null;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.persistence.CQLDataType#isAlterableTo(org.helenus.driver.persistence.CQLDataType)
   */
  @Override
  public boolean isAlterableTo(CQLDataType to) {
    return false; // never alterable
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.persistence.CQLDataType#toCQL()
   */
  @Override
  public String toCQL() {
    return "frozen<" + name + '>';
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.impl.ClassInfoImpl#supportsTablesAndIndexes()
   */
  @Override
  public boolean supportsTablesAndIndexes() {
    return false;
  }

  /**
   * Gets the fake table info defined by the user-defined POJO.
   *
   * @author paouelle
   *
   * @return the fake table info defined by the POJO
   */
  public TableInfoImpl<T> getTableImpl() {
    return table;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.impl.ClassInfoImpl#tables()
   */
  @Override
  public Stream<TableInfo<T>> tables() {
    return Stream.empty();
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.impl.ClassInfoImpl#getTableImpl(java.lang.String)
   */
  @Override
  public TableInfoImpl<T> getTableImpl(String name) {
    throw new IllegalArgumentException("user-defined types do not define tables");
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.impl.ClassInfoImpl#getNumTables()
   */
  @Override
  public int getNumTables() {
    return 0;
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.impl.ClassInfoImpl#getTables()
   */
  @Override
  public Collection<TableInfo<T>> getTables() {
    return Collections.emptyList();
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.impl.ClassInfoImpl#newContext(java.lang.Object)
   */
  @Override
  public POJOContext newContext(T object) {
    return new POJOContext(object);
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.impl.ClassInfoImpl#getObject(com.datastax.driver.core.Row, java.util.Map)
   */
  @Override
  public T getObject(Row row, Map<String, Object> kkeys) {
    throw new ObjectConversionException(
      clazz,
      row,
      getEntityAnnotationClass().getSimpleName()
      + " POJOs cannot be retrieved from result rows"
    );
  }

  /**
   * Instantiates a new blank object.
   *
   * @author paouelle
   *
   * @return a newly created blank pojo of this type
   */
  public T newObject() {
    try {
      // create an empty shell for the pojo
      final T object = constructor.newInstance();

      // start by setting back all final fields' values
      finalFields.forEach(
        (field, value) -> {
          try {
            // set it in field directly
            field.set(object, value);
          } catch (IllegalAccessException e) { // should not happen
            throw new IllegalStateException(e);
          }
        }
      );
      return object;
    } catch (IllegalAccessException|InstantiationException e) {
      throw new IllegalStateException(clazz.getName(), e);
    } catch (InvocationTargetException e) {
      final Throwable t = e.getTargetException();

      if (t instanceof Error) {
        throw (Error)t;
      } else if (t instanceof RuntimeException) {
        throw (RuntimeException)t;
      } else {
        throw new ObjectConversionException(
          clazz,
          "failed to instantiate blank POJO",
          t
        );
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.impl.ClassInfoImpl#getObject(com.datastax.driver.core.UDTValue)
   */
  @Override
  public T getObject(UDTValue uval) {
    if (uval == null) {
      return null;
    }
    try {
      // create an empty shell for the pojo
      final T object = constructor.newInstance();

      // start by setting back all final fields' values
      finalFields.forEach(
        (field, value) -> {
          try {
            // set it in field directly
            field.set(object, value);
          } catch (IllegalAccessException e) { // should not happen
            throw new IllegalStateException(e);
          }
        }
      );
      // now take care of the columns
      decodeAndSetColumnFields(object, uval);
      return object;
    } catch (IllegalAccessException|InstantiationException e) {
      throw new IllegalStateException(clazz.getName(), e);
    } catch (InvocationTargetException e) {
      final Throwable t = e.getTargetException();

      if (t instanceof Error) {
        throw (Error)t;
      } else if (t instanceof RuntimeException) {
        throw (RuntimeException)t;
      } else {
        throw new ObjectConversionException(
          clazz,
          uval,
          "failed to instantiate blank POJO",
          t
        );
      }
    }
  }

  /**
   * Converts the specified parsed values into a POJO object defined by this
   * class information.
   *
   * @author paouelle
   *
   * @param  keyspace the keyspace for which to create the object
   * @param  values the formated values to convert into a POJO
   * @return the POJO object corresponding to the given UDT value
   * @throws ObjectConversionException if unable to convert to a POJO
   */
  public T getObject(String keyspace, Map<String, String> values) {
    if (values == null) {
      return null;
    }
    try {
      // create an empty shell for the pojo
      final T object = constructor.newInstance();

      // start by setting back all final fields' values
      finalFields.forEach(
        (field, value) -> {
          try {
            // set it in field directly
            field.set(object, value);
          } catch (IllegalAccessException e) { // should not happen
            throw new IllegalStateException(e);
          }
        }
      );
      // now take care of the columns
      decodeAndSetColumnFields(object, keyspace, values);
      return object;
    } catch (IllegalAccessException|InstantiationException e) {
      throw new IllegalStateException(clazz.getName(), e);
    } catch (InvocationTargetException e) {
      final Throwable t = e.getTargetException();

      if (t instanceof Error) {
        throw (Error)t;
      } else if (t instanceof RuntimeException) {
        throw (RuntimeException)t;
      } else {
        throw new ObjectConversionException(
          clazz,
          values,
          "failed to instantiate blank POJO",
          t
        );
      }
    }
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
    return (
      getClass().getSimpleName()
      + "[name=" + name
      + ",clazz=" + clazz
      + ",keyspace=" + getKeyspace()
      + ",columns=" + getColumns()
      + ",table=" + table
      + "]"
    );
  }
}
