/*
 * Copyright (C) 2015-2015 The Helenus Driver Project Authors.
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
package org.helenus.util.function;

/**
 * The <code>TriFunction</code> interface represents a function that accepts
 * three arguments and produce a result.
 * <p>
 * This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #apply(Object, Object, Object)}.
 *
 * @copyright 2015-2015 The Helenus Driver Project Authors
 *
 * @author  The Helenus Driver Project Authors
 * @version 1 - Jul 10, 2015 - paouelle - Creation
 *
 * @param <T> the type of the first argument to the function
 * @param <U> the type of the second argument to the function
 * @param <V> the type of the second argument to the function
 * @param <R> the type of the result of the function
 *
 * @since 2.0
 */
@FunctionalInterface
public interface TriFunction<T, U, V, R> {
  /**
   * Performs this operation on the given arguments.
   *
   * @author paouelle
   *
   * @param  t the first input argument
   * @param  u the second input argument
   * @param  v the third input argument
   * @return the function result
   */
  public R apply(T t, U u, V v);
}
