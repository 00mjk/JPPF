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

package org.jppf.node.policy;

/**
 * An execution policy predicate which determines whether a a connection endpoint is a local executor (on the client side) or a local node (on the server side).
 * @author Laurent Cohen
 */
public class IsLocalChannel extends NoArgRule {
  /**
   * 
   */
  public IsLocalChannel() {
    super("jppf.channel.local", false);
  }
}
