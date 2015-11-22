/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package org.jppf.load.balancer.impl;

import org.jppf.load.balancer.*;

/**
 * Bundler based on a reinforcement learning algorithm.
 * @author Laurent Cohen
 * @exclude
 */
public class RLBundler extends AbstractRLBundler {
  /**
   * Creates a new instance with the specified parameters.
   * @param profile the parameters of the algorithm, grouped as a performance analysis profile.
   */
  public RLBundler(final LoadBalancingProfile profile) {
    super(profile);
  }

  @Override
  public Bundler copy() {
    return new RLBundler(profile);
  }

  /**
   * Get the max bundle size that can be used for this bundler.
   * @return the bundle size as an int.
   */
  @Override
  protected int maxSize() {
    if (job != null) return job.getTaskCount();
    if (jppfContext == null) throw new IllegalStateException("jppfContext not set");
    return jppfContext.getMaxBundleSize();
  }
}