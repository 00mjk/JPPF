/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

package org.jppf.test.scenario;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

import org.jppf.test.addons.mbeans.*;
import org.jppf.test.setup.*;
import org.jppf.utils.TypedProperties;
import org.jppf.utils.streams.StreamUtils;

import test.org.jppf.test.setup.ConfigurationHelper;

/**
 * 
 * @author Laurent Cohen
 */
public class Scenario
{
  /**
   * Path to where this scenario's config files are located.
   */
  private final File configDir;
  /**
   * The configuration for this scenario.
   */
  private final ScenarioConfiguration configuration;
  /**
   * The setup of nodes, drivers and client.
   */
  private Setup setup;

  /**
   * 
   * @param args .
   */
  public static void main(final String[] args)
  {
    try
    {
      Scenario scenario = new Scenario(args[0]);
      scenario.execute();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  /**
   * Initialize this scenario with the specified config directory.
   * @param configDir path to where this scenario's config files are located.
   */
  public Scenario(final String configDir)
  {
    if (configDir == null) throw new IllegalArgumentException("config directory cannot be null");
    File file = new File(configDir);
    if (!file.exists()) throw new IllegalArgumentException("config directory '" + configDir + "' does not exist");
    if (!file.isDirectory()) throw new IllegalArgumentException("'" + configDir + "' is not a directory");
    this.configDir = file;
    configuration = new ScenarioConfigurationImpl(this.configDir);
  }

  /**
   * Execute this scenario.
   * @throws Exception if any error occurs.
   */
  public void execute() throws Exception
  {
    try
    {
      Map<String, Object> variables = new HashMap<String, Object>();
      variables.put("$n", 1);
      variables.put("$scenario_dir", configDir.getPath());
      variables.put("$templates_dir", ScenarioConfiguration.TEMPLATES_DIR);
      String jppf = doConfigOverride("client.template.properties", "client.properties", variables);
      System.setProperty("jppf.config", jppf);
      String log4j = doConfigOverride("log4j-client.template.properties", "log4j-client.properties", variables);
      URL url = new File(log4j).toURI().toURL();
      // use reflection to avoid compile-time dependency on log4j lib.
      Class<?> configuratorClass = Class.forName("org.apache.log4j.PropertyConfigurator");
      Method method = configuratorClass.getMethod("configure", URL.class);
      method.invoke(null, url);
      //PropertyConfigurator.configure(url);
      System.setProperty("log4j.configuration", url.toString());
      String logging = doConfigOverride("logging-client.template.properties", "logging-client.properties", variables);
      System.setProperty("java.util.logging.config.file", logging);
      setup = new Setup(configuration);
      String className = configuration.getRunnerClassName();
      Class<?> clazz = Class.forName(className);
      ScenarioRunner runner = (ScenarioRunner) clazz.newInstance();
      setup.setup(configuration.getNbDrivers(), configuration.getNbNodes());
      runner.setSetup(setup);
      runner.setConfiguration(configuration);
      runner.run();
    }
    finally
    {
      try
      {
        printDiagnostics();
      }
      finally
      {
        if (setup != null) setup.cleanup();
      }
    }
  }

  /**
   * Display the diagnostics for all drivers and nodes.
   * @throws Exception if any error occurs.
   */
  private void printDiagnostics() throws Exception
  {
    String fileName = configuration.getDiagnosticsOutputFilename();
    if ("none".equals(fileName)) return;
    PrintStream out = null;
    if ("out".equals(fileName)) out = System.out;
    else if ("err".equals(fileName)) out = System.err;
    else out = new PrintStream(new BufferedOutputStream(new FileOutputStream(fileName)));
    try
    {
      Map<JMXResult<DiagnosticsResult>, List<JMXResult<DiagnosticsResult>>> map =
        setup.getJmxHandler().performJmxOperations(new DiagnosticsGrabber(true), new DiagnosticsGrabber(false));
      for (Map.Entry<JMXResult<DiagnosticsResult>, List<JMXResult<DiagnosticsResult>>> entry: map.entrySet())
      {
        out.println("---------------------------------------------------------");
        out.println("results for driver " + entry.getKey().getJmxId());
        out.println("before GC: " + entry.getKey().getResult().getDiagnosticsInfo());
        out.println("after GC: " + entry.getKey().getResult().getDiagnosticsInfoAfterGC());
        for (JMXResult<DiagnosticsResult> dr: entry.getValue())
        {
          out.println("results for node " + dr.getJmxId());
          out.println("before GC: " + dr.getResult().getDiagnosticsInfo());
          out.println("after GC: " + dr.getResult().getDiagnosticsInfoAfterGC());
        }
      }
    }
    finally
    {
      if ((out != null) && (out != System.err) && (out != System.out)) StreamUtils.closeSilent(out);
    }
  }

  /**
   * Get the configuration for this scenario.
   * @return a {@link ScenarioConfiguration} instance.
   */
  public ScenarioConfiguration getConfiguration()
  {
    return configuration;
  }

  /**
   * Create a config file from the template and override its entries with those in the specified override file.
   * @param template the name of the template file.
   * @param override the name of the override file.
   * @param variables a map of variable names to their value, which can be used in a groovy expression.
   * @return the path of the created file.
   */
  protected String doConfigOverride(final String template, final String override, final Map<String, Object> variables)
  {
    File templateFile = new File(configuration.getConfigDir(), template);
    if (!templateFile.exists()) templateFile = new File(ScenarioConfiguration.TEMPLATES_DIR, template);
    TypedProperties config = ConfigurationHelper.createConfigFromTemplate(templateFile.getPath(), variables);
    File overrideFile = new File(configuration.getConfigDir(), override);
    if (overrideFile.exists()) ConfigurationHelper.overrideConfig(config, overrideFile);
    String path = ConfigurationHelper.createTempConfigFile(config);
    return path;
  }

  /**
   * Instances of this class get the diagnostics information for a driver or a node.
   */
  private class DiagnosticsGrabber extends JmxAwareCallable<DiagnosticsResult>
  {
    /**
     * <code>true</code> if this object connects to a driver's JMX, <code>false</code> for a node.
     */
    private final boolean driver;

    /**
     * Initialize this object with the psecified type of remote JMX server.
     * @param driver <code>true</code> if this object connects to a driver's JMX, <code>false</code> for a node.
     */
    public DiagnosticsGrabber(final boolean driver)
    {
      this.driver = driver;
    }

    @Override
    public JMXResult<DiagnosticsResult> call() throws Exception
    {
      String name = driver ? DiagnosticsMBean.MBEAN_NAME_DRIVER : DiagnosticsMBean.MBEAN_NAME_NODE;
      DiagnosticsInfo info = (DiagnosticsInfo) getJmx().getAttribute(name, "DiagnosticsInfo");
      getJmx().invoke(name, "gc", (Object[]) null, (String[]) null);
      DiagnosticsInfo info2 = (DiagnosticsInfo) getJmx().getAttribute(name, "DiagnosticsInfo");
      return new JMXResult<DiagnosticsResult>(getJmx().getURL().toString(), new DiagnosticsResult(info, info2));
    }
  }
}