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
package org.helenus.driver.codecs.provider;

import com.datastax.driver.core.TypeCodec;
import com.datastax.driver.core.exceptions.CodecNotFoundException;
import com.google.common.reflect.TypeToken;

import org.helenus.driver.persistence.DataType;

/**
 * The <code>SmallIntCodecProvider</code> class defines a provider suitable to
 * decode the {@link DataType#SMALLINT} data type.
 *
 * @copyright 2015-2017 The Helenus Driver Project Authors
 *
 * @author The Helenus Driver Project Authors
 * @version 1 - Jan 9, 2017 - paouelle - Creation
 *
 * @since 1.0
 */
public final class SmallIntCodecProvider implements CodecProvider {
  /**
   * Holds the single instance of this provider.
   *
   * @author paouelle
   */
  public final static CodecProvider INSTANCE = new SmallIntCodecProvider();

  /**
   * Instantiates a new <code>SmallIntCodecProvider</code> object.
   *
   * @author paouelle
   */
  private SmallIntCodecProvider() {}

  /**
   * {@inheritDoc}
   *
   * @author paouelle
   *
   * @see org.helenus.driver.codecs.provider.CodecProvider#codecFor(java.lang.Class)
   */
  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public <T> TypeCodec<T> codecFor(Class<T> clazz)
    throws CodecNotFoundException {
    if ((Short.class == clazz) || (Short.TYPE == clazz)) {
      return (TypeCodec<T>)TypeCodec.smallInt();
    }
    throw new CodecNotFoundException(
      "unsupported Helenus codec from '"
      + DataType.SMALLINT.toCQL()
      + "' to class: "
      + clazz.getName(),
      com.datastax.driver.core.DataType.smallint(),
      TypeToken.of(clazz)
    );
  }
}
