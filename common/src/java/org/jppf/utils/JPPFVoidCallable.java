/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.utils;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * An extension of the {@link JPPFCallable} whose {@link #call()} method always invokes the {@link #run()} method and returns null.
 * @author Laurent Cohen
 */
@FunctionalInterface
public interface JPPFVoidCallable extends Callable<Void>, Serializable {
  @Override
  default Void call() throws Exception {
    run();
    return null;
  }

  /**
   * 
   * @throws Exception if any errror occurs.
   */
  void run() throws Exception;
}
