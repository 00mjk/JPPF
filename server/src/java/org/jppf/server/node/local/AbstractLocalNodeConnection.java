/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package org.jppf.server.node.local;

import org.jppf.node.AbstractNodeConnection;

/**
 * 
 * @author Laurent Cohen
 * @param <C> .
 * @exclude
 */
public class AbstractLocalNodeConnection<C> extends AbstractNodeConnection<C> {
  /**
   * Initialize this connection with the specified serializer.
   * @param channel the communicationchannel to use.
   */
  public AbstractLocalNodeConnection(final C channel) {
    this.channel = channel;
  }

  @Override
  public void init() throws Exception {
  }

  @Override
  public void close() throws Exception {
  }
}