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

package org.jppf.management.generated;

import org.jppf.management.AbstractMBeanStaticProxy;
import org.jppf.management.JMXConnectionWrapper;
import org.jppf.management.JPPFNodeTaskMonitorMBean;

/**
 * Generated static proxy for the {@link org.jppf.management.JPPFNodeTaskMonitorMBean} MBean interface.
 * @author /common/src/java/org/jppf/utils/generator/MBeanStaticProxyGenerator.java
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class JPPFNodeTaskMonitorMBeanStaticProxy extends AbstractMBeanStaticProxy implements JPPFNodeTaskMonitorMBean {
  /**
   * Initialize this MBean static proxy.
   * @param connection the JMX connection used to invoke remote MBean methods.
   */
  public JPPFNodeTaskMonitorMBeanStaticProxy(final JMXConnectionWrapper connection) {
    super(connection, "org.jppf:name=task.monitor,type=node");
  }

  /**
   * Get the JMX object name for this MBean static proxy.
   * @return the object name as a string.
   */
  public static final String getMBeanName() {
    return "org.jppf:name=task.monitor,type=node";
  }

  @Override
  public Integer getTotalTasksExecuted() {
    return (Integer) getAttribute("TotalTasksExecuted");
  }

  @Override
  public Integer getTotalTasksInError() {
    return (Integer) getAttribute("TotalTasksInError");
  }

  @Override
  public Integer getTotalTasksSucessfull() {
    return (Integer) getAttribute("TotalTasksSucessfull");
  }

  @Override
  public Long getTotalTaskCpuTime() {
    return (Long) getAttribute("TotalTaskCpuTime");
  }

  @Override
  public Long getTotalTaskElapsedTime() {
    return (Long) getAttribute("TotalTaskElapsedTime");
  }

  @Override
  public void reset() {
    invoke("reset", (Object[]) null, (String[]) null);
  }
}
