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
package com.datastax.driver.core;

/**
 * The <code>MetadataBridge</code> class is used to access protected methods of
 * the {@link Metadata}.
 *
 * @copyright 2015-2017 The Helenus Driver Project Authors
 *
 * @author  The Helenus Driver Project Authors
 * @version 1 - Jan 21, 2016 - paouelle - Creation
 *
 * @since 1.0
 */
public class MetadataBridge {
  /**
   * Gets the number of known hosts of the cluster based on its meta data.
   * <p>
   * <i>Note:</i> The implementation here avoids creating a temporary read-only
   * collection.
   *
   * @author paouelle
   *
   * @param  mdata the meta data for the cluster
   * @return the number of known hosts in the cluster
   */
  public static int getNumHosts(Metadata mdata) {
    return mdata.allHosts().size();
  }

  /**
   * Escape a CQL3 identifier based on its value as read from the schema tables.
   * Because it comes from Cassandra, we could just always quote it, but to get
   * a nicer output we don't do it if it's not necessary.
   *
   * @author paouelle
   *
   * @param  id the identifier to escape
   * @return the escaped identifier
   */
  public static String escapeId(String id) {
    return Metadata.escapeId(id);
  }
}
