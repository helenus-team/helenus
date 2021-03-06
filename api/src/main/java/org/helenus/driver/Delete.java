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
package org.helenus.driver;

import java.util.Optional;
import java.util.stream.Stream;

import org.helenus.driver.info.ClassInfo;
import org.helenus.driver.info.TableInfo;

/**
 * The <code>Delete</code> interface extends the functionality of Cassandra's
 * {@link com.datastax.driver.core.querybuilder.Delete} class to provide
 * support for POJOs.
 *
 * @copyright 2015-2016 The Helenus Driver Project Authors
 *
 * @author  The Helenus Driver Project Authors
 * @version 1 - Jan 15, 2015 - paouelle - Creation
 *
 * @param <T> The type of POJO associated with this statement.
 *
 * @since 1.0
 */
public interface Delete<T>
  extends ObjectStatement<T>, BatchableStatement<Void, VoidFuture> {
  /**
   * Gets all the tables included in this statement.
   *
   * @author paouelle
   *
   * @return a non-<code>null</code> stream of all tables included in this
   *         statement
   */
  public Stream<TableInfo<T>> tables();

  /**
   * Adds a WHERE clause to this statement.
   *
   * This is a shorter/more readable version for {@code where().and(clause)}.
   *
   * @author paouelle
   *
   * @param  clause the clause to add.
   * @return the where clause of this query to which more clause can be added.
   * @throws IllegalArgumentException if the clause references a column
   *         not defined in the POJO
   * @throws ExcludedKeyspaceKeyException if the clause reference a keyspace key
   *         and the specified value is marked as excluded
   */
  public Where<T> where(Clause clause);

  /**
   * Returns a Where statement for this query without adding clause.
   *
   * @author paouelle
   *
   * @return the where clause of this query to which more clause can be added
   */
  public Where<T> where();

  /**
   * Adds a conditions clause (IF) to this statement.
   * <p>
   * This is a shorter/more readable version for {@code onlyIf().and(condition)}.
   *
   * @param  condition the condition to add
   * @return the conditions of this query to which more conditions can be added
   * @throws IllegalArgumentException if the condition reference a column not
   *         defined by the POJO or if the {@link #ifExists} option was first
   *         selected
   */
  public Conditions<T> onlyIf(Clause condition);

  /**
   * Adds a conditions clause (IF) to this statement.
   *
   * @return the conditions of this query to which more conditions can be added.
   * @throws IllegalArgumentException if the {@link #ifExists} option was first
   *         selected
   */
  public Conditions<T> onlyIf();

  /**
   * Adds a new options for this DELETE statement.
   *
   * @author paouelle
   *
   * @param  using the option to add.
   * @return the options of this DELETE statement.
   */
  public Options<T> using(Using<?> using);

  /**
   * Gets all options registered with this statement.
   *
   * @author paouelle
   *
   * @return a non-<code>null</code> stream of all options registered with this
   *         statement
   */
  public Stream<Using<?>> usings();

  /**
   * Gets an option registered with this statement given its name
   *
   * @author paouelle
   *
   * @param <U> the type of the option
   *
   * @param  name the name of the option to retrieve
   * @return the registered option with the given name or empty if none registered
   * @throws IllegalArgumentException if conditions where already registered
   *         via {@link #onlyIf} option first
   */
  public <U> Optional<Using<U>> getUsing(String name);

  /**
   * Sets the 'IF EXISTS' option for this DELETE statement.
   * <p>
   * A delete with that option will report whether the statement actually
   * resulted in data being deleted. The existence check and deletion are
   * done transactionally in the sense that if multiple clients attempt to
   * delete a given row with this option, then at most one may succeed.
   * <p>
   * Please keep in mind that using this option has a non negligible
   * performance impact and should be avoided when possible.
   *
   * @author paouelle
   *
   * @return this DELETE statement.
   */
  public Delete<T> ifExists();

  /**
   * The <code>Where</code> interface defines WHERE clause for a DELETE statement.
   *
   * @copyright 2015-2016 The Helenus Driver Project Authors
   *
   * @author  The Helenus Driver Project Authors
   * @version 1 - Jan 15, 2015 - paouelle - Creation
   *
   * @param <T> The type of POJO associated with the statement.
   *
   * @since 1.0
   */
  public interface Where<T>
    extends Statement<T>, BatchableStatement<Void, VoidFuture> {
    /**
     * Adds the provided clause to this WHERE clause.
     *
     * @author paouelle
     *
     * @param  clause the clause to add
     * @return this WHERE clause
     * @throws IllegalArgumentException if the clause references a column
     *         not defined in the POJO
     * @throws ExcludedKeyspaceKeyException if the clause reference a keyspace key
     *         and the specified value is marked as excluded
     */
    public Where<T> and(Clause clause);

    /**
     * Adds an option to the DELETE statement this WHERE clause is part of.
     *
     * @author paouelle
     *
     * @param  using the using clause to add
     * @return the options of the DELETE statement this WHERE clause is part of
     */
    public Options<T> using(Using<?> using);

    /**
     * Sets the 'IF EXISTS' option for the DELETE statement this WHERE clause
     * is part of.
     * <p>
     * A delete with that option will report whether the statement actually
     * resulted in data being deleted. The existence check and deletion are
     * done transactionally in the sense that if multiple clients attempt to
     * delete a given row with this option, then at most one may succeed.
     * <p>
     * Please keep in mind that using this option has a non negligible
     * performance impact and should be avoided when possible.
     *
     * @author paouelle
     *
     * @return the DELETE statement this WHERE clause is part of
     * @throws IllegalArgumentException if conditions where already registered
     *         via {@link #onlyIf} option first
     */
    public Delete<T> ifExists();

    /**
     * Adds a condition to the DELETE statement this WHERE clause is part of.
     *
     * @author paouelle
     *
     * @param  condition the condition to add
     * @return the conditions for the DELETE statement this WHERE clause is part
     *         of
     * @throws IllegalArgumentException if the condition reference a column not
     *         defined by the POJO or if the {@link #ifExists} option was first
     *         selected
     */
    public Conditions<T> onlyIf(Clause condition);
  }

  /**
   * The <code>Options</code> interface defines the options of a DELETE statement.
   *
   * @copyright 2015-2016 The Helenus Driver Project Authors
   *
   * @author  The Helenus Driver Project Authors
   * @version 1 - Jan 15, 2015 - paouelle - Creation
   *
   * @param <T> The type of POJO associated with the statement.
   *
   * @since 1.0
   */
  public interface Options<T>
    extends Statement<T>, BatchableStatement<Void, VoidFuture> {
    /**
     * Adds the provided option.
     *
     * @author paouelle
     *
     * @param  using a DELETE option.
     * @return this {@code Options} object.
     */
    public Options<T> and(Using<?> using);

    /**
     * Adds a where clause to the DELETE statement these options are part of.
     *
     * @author paouelle
     *
     * @param  clause clause to add.
     * @return the WHERE clause of the DELETE statement these options are part of.
     * @throws IllegalArgumentException if the clause references a column
     *         not defined in the POJO
     * @throws ExcludedKeyspaceKeyException if the clause reference a keyspace key
     *         and the specified value is marked as excluded
     */
    public Where<T> where(Clause clause);

    /**
     * Sets the 'IF EXISTS' option for the DELETE statement this WHERE clause
     * is part of.
     * <p>
     * A delete with that option will report whether the statement actually
     * resulted in data being deleted. The existence check and deletion are
     * done transactionally in the sense that if multiple clients attempt to
     * delete a given row with this option, then at most one may succeed.
     * <p>
     * Please keep in mind that using this option has a non negligible
     * performance impact and should be avoided when possible.
     *
     * @author paouelle
     *
     * @return the DELETE statement this WHERE clause is part of
     * @throws IllegalArgumentException if conditions where already registered
     *         via {@link #onlyIf} option first
     */
    public Delete<T> ifExists();

    /**
     * Adds a condition to the DELETE statement this WHERE clause is part of.
     *
     * @author paouelle
     *
     * @param  condition the condition to add
     * @return the conditions for the DELETE statement this WHERE clause is part
     *         of
     * @throws IllegalArgumentException if the condition reference a column not
     *         defined by the POJO or if the {@link #ifExists} option was first
     *         selected
     */
    public Conditions<T> onlyIf(Clause condition);
  }

  /**
   * The <code>Builder</code> interface defines an in-construction DELETE statement.
   *
   * @copyright 2015-2016 The Helenus Driver Project Authors
   *
   * @author  The Helenus Driver Project Authors
   * @version 1 - Jan 15, 2015 - paouelle - Creation
   *
   * @param <T> The type of POJO associated with the statement.
   *
   * @since 1.0
   */
  public interface Builder<T> {
    /**
     * Gets the POJO class associated with this statement builder.
     *
     * @author paouelle
     *
     * @return the POJO class associated with this statement builder
     */
    public Class<T> getObjectClass();

    /**
     * Gets the POJO class information associated with this statement builder.
     *
     * @author paouelle
     *
     * @return the POJO class info associated with this statement builder
     */
    public ClassInfo<T> getClassInfo();

    /**
     * Adds tables to delete from using the keyspace defined in the POJO.
     * <p>
     * This flavor should be used when the POJO doesn't require keyspace keys to the
     * keyspace name.
     *
     * @author paouelle
     *
     * @param  tables the names of the tables to delete from
     * @return a newly build DELETE statement that deletes from the specified
     *         tables
     * @throws NullPointerException if <code>tables</code> is <code>null</code>
     * @throws IllegalArgumentException if any of the tables or any of the
     *         referenced columns are not defined by the POJO
     */
    public Delete<T> from(String... tables);

    /**
     * Adds tables to delete from using the keyspace defined in the POJO.
     * <p>
     * This flavor should be used when the POJO doesn't require keyspace keys to the
     * keyspace name.
     *
     * @author paouelle
     *
     * @param  tables the names of the tables to delete from
     * @return a newly build DELETE statement that deletes from the specified
     *         tables
     * @throws IllegalArgumentException if any of the tables or any of the
     *         referenced columns are not defined by the POJO
     */
    public Delete<T> from(Stream<String> tables);

    /**
     * Specifies to delete from all tables defined in the POJO using the
     * keyspace defined in the POJO.
     * <p>
     * This flavor should be used when the POJO doesn't require keyspace keys to the
     * keyspace name.
     *
     * @author paouelle
     *
     * @return a newly build DELETE statement that deletes from all tables
     *         defined in the POJO
     * @throws IllegalArgumentException if any of the referenced columns are not
     *         defined by the POJO
     */
    public Delete<T> fromAll();
  }

  /**
   * The <code>Selection</code> interface defines a selection clause for an
   * in-construction DELETE statement.
   *
   * @copyright 2015-2016 The Helenus Driver Project Authors
   *
   * @author  The Helenus Driver Project Authors
   * @version 1 - Jan 15, 2015 - paouelle - Creation
   *
   * @param <T> The type of POJO associated with the response from this statement.
   *
   * @since 1.0
   */
  public interface Selection<T> extends Builder<T> {
    /**
     * Deletes all columns (i.e. "DELETE FROM ...")
     *
     * @author paouelle
     *
     * @return an in-build DELETE statement
     * @throws IllegalStateException if some columns had already been selected
     *         for this builder.
     */
    public Builder<T> all();

    /**
     * Deletes the provided column.
     *
     * @author paouelle
     *
     * @param  name the column name to select for deletion.
     * @return this in-build DELETE Selection
     * @throws NullPointerException if <code>name</code> is <code>null</code>
     */
    public Selection<T> column(String name);

    /**
     * Deletes the provided list element.
     *
     * @author paouelle
     *
     * @param  columnName the name of the list column.
     * @param  idx the index of the element to delete.
     * @return this in-build DELETE Selection
     * @throws NullPointerException if <code>columName</code> is <code>null</code>
     * @throws ArrayIndexOutOfBoundsException if <code>idx</code> is less than 0
     */
    public Selection<T> listElt(String columnName, int idx);

    /**
     * Deletes a map element given a key.
     *
     * @author paouelle
     *
     * @param  columnName the name of the map column.
     * @param  key the key for the element to delete.
     * @return this in-build DELETE Selection
     * @throws NullPointerException if <code>columName</code> or <code>key</code>
     *         is <code>null</code>
     */
    public Selection<T> mapElt(String columnName, Object key);
  }

  /**
   * Conditions for a DELETE statement.
   * <p>
   * When provided some conditions, a delete will not apply unless the provided
   * conditions applies.
   * <p>
   * Please keep in mind that provided conditions has a non negligible
   * performance impact and should be avoided when possible.
   *
   * @copyright 2015-2016 The Helenus Driver Project Authors
   *
   * @author  The Helenus Driver Project Authors
   * @version 1 - Aug 5, 2016 - paouelle - Creation
   *
   * @param <T> The type of POJO associated with the statement.
   *
   * @since 1.0
   */
  public interface Conditions<T>
    extends Statement<T>, BatchableStatement<Void, VoidFuture> {
    /**
     * Adds the provided condition for the delete.
     *
     * @author paouelle
     *
     * @param  condition the condition to add
     * @return this {@code Conditions} clause
     * @throws IllegalArgumentException if the condition reference a column not
     *         defined by the POJO
     */
    public Conditions<T> and(Clause condition);

    /**
     * Adds a where clause to the DELETE statement these options are part of.
     *
     * @param  clause clause to add
     * @return the WHERE clause of the DELETE statement these options are part
     *         of
     * @throws IllegalArgumentException if the clause referenced a
     *         column which is not a primary key or an index column in the POJO
     *         or if the clause references columns not defined by the POJO or
     *         invalid values
     */
    public Where<T> where(Clause clause);

    /**
     * Adds an option to the UPDATE statement these conditions are part of.
     *
     * @param  using the using clause to add
     * @return the options of the UPDATE statement these conditions are part of
     */
    public Options<T> using(Using<?> using);
  }
}
