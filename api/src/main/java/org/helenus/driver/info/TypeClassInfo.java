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
package org.helenus.driver.info;

/**
 * The <code>TypeClassInfo</code> interface extends on the {@link ClassInfo}
 * interface to provide addition information for type entity POJO class.
 *
 * @copyright 2015-2015 The Helenus Driver Project Authors
 *
 * @author  The Helenus Driver Project Authors
 * @version 1 - Mar 11, 2015 - paouelle - Creation
 *
 * @param <T> The type of POJO represented by this class
 *
 * @since 1.0
 */
public interface TypeClassInfo<T> extends ClassInfo<T> {
  /**
   * Gets the class info for the root entity defined for this type entity.
   *
   * @author paouelle
   *
   * @return the non-<code>null</code> root entity POJO class info defined for
   *         this type entity
   */
  public RootClassInfo<? super T> getRoot();

  /**
   * Gets the type of this POJO class.
   *
   * @author paouelle
   *
   * @return the non-<code>null</code> type of this POJO class
   */
  public String getType();

  /**
   * Checks if this type was dynamically added to its root or if it was known
   * to the root via its @RootEntity annotation.
   *
   * @author paouelle
   *
   * @return <code>true</code> if this type is dynamically being added
   *         to the root; <code>false</code> if it was known to the root via
   *         the @RootEnitty annotation
   */
  public boolean isDynamic();
}
