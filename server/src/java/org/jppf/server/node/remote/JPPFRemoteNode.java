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
package org.jppf.server.node.remote;

import org.jppf.node.connection.DriverConnectionInfo;
import org.jppf.persistence.JPPFDatasourceFactory;
import org.jppf.utils.JPPFConfiguration;

/**
 * Instances of this class encapsulate execution nodes.
 * @author Laurent Cohen
 */
public class JPPFRemoteNode extends AbstractRemoteNode {
  /**
   * Default constructor.
   * @param connectionInfo the server connection information.
   */
  public JPPFRemoteNode(final DriverConnectionInfo connectionInfo) {
    super(connectionInfo);
  }

  @Override
  protected void initClassLoaderManager() {
    classLoaderManager = new RemoteClassLoaderManager(this);
  }

  @Override
  protected synchronized void init() throws Exception {
    super.init();
    final JPPFDatasourceFactory factory = JPPFDatasourceFactory.getInstance();
    factory.configure(JPPFConfiguration.getProperties(), JPPFDatasourceFactory.Scope.LOCAL, getSystemInformation());
  }
}
