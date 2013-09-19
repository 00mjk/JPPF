/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

package org.jppf.server.nio.classloader.client;

import java.util.*;

import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.*;
import org.jppf.server.nio.classloader.*;
import org.jppf.utils.*;
import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 * Instances of this class serve class loading requests from the JPPF nodes.
 * @author Laurent Cohen
 */
public class ClientClassNioServer extends ClassNioServer
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ClientClassNioServer.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether TRACE logging level is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * A mapping of the remote resource provider connections handled by this socket server, to their unique uuid.<br>
   * Provider connections represent connections form the clients only. The mapping to a uuid is required to determine in
   * which application classpath to look for the requested resources.
   */
  protected final CollectionMap<String, ChannelWrapper<?>> providerConnections = new CopyOnWriteListConcurrentMap<>();

  /**
   * Initialize this class server.
   * @param driver reference to the driver.
   * @param useSSL determines whether an SSLContext should be created for this server.
   * @throws Exception if the underlying server socket can't be opened.
   */
  public ClientClassNioServer(final JPPFDriver driver, final boolean useSSL) throws Exception
  {
    super(JPPFIdentifiers.CLIENT_CLASSLOADER_CHANNEL, driver, useSSL);
  }

  @Override
  protected NioServerFactory<ClassState, ClassTransition> createFactory()
  {
    return new ClientClassServerFactory(this);
  }

  @Override
  public void postAccept(final ChannelWrapper<?> channel)
  {
    try
    {
      synchronized(channel)
      {
        transitionManager.transitionChannel(channel, ClassTransition.TO_WAITING_INITIAL_PROVIDER_REQUEST);
        if (transitionManager.checkSubmitTransition(channel)) transitionManager.submitTransition(channel);
      }
    }
    catch (Exception e)
    {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.warn(ExceptionUtils.getMessage(e));
      closeConnection(channel);
    }
  }

  /**
   * Close the specified connection.
   * @param channel the channel representing the connection.
   */
  public static void closeConnection(final ChannelWrapper<?> channel)
  {
    if (channel == null)
    {
      log.warn("attempt to close null channel - skipping this step");
      return;
    }
    ClientClassNioServer server = (ClientClassNioServer) JPPFDriver.getInstance().getClientClassServer();
    ClassContext context = (ClassContext) channel.getContext();
    String uuid = context.getUuid();
    if (uuid != null) server.removeProviderConnection(uuid, channel);
    try
    {
      channel.close();
    }
    catch(Exception e)
    {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.warn(e.getMessage());
    }
    String connectionUuid = context.getConnectionUuid();
    JPPFDriver.getInstance().getClientNioServer().closeClientConnection(connectionUuid);
  }

  /**
   * Add a provider connection to the map of existing available providers.
   * @param uuid the provider uuid as a string.
   * @param channel the provider's communication channel.
   */
  public void addProviderConnection(final String uuid, final ChannelWrapper<?> channel)
  {
    if (debugEnabled) log.debug("adding provider connection: uuid=" + uuid + ", channel=" + channel);
    providerConnections.putValue(uuid, channel);
  }

  /**
   * Add a provider connection to the map of existing available providers.
   * @param uuid the provider uuid as a string.
   * @param channel the provider's communication channel.
   */
  public void removeProviderConnection(final String uuid, final ChannelWrapper channel)
  {
    if (debugEnabled) log.debug("removing provider connection: uuid=" + uuid + ", channel=" + channel);
    providerConnections.removeValue(uuid, channel);
  }

  /**
   * Get all the provider connections for the specified client uuid.
   * @param uuid the uuid of the client for which to get connections.
   * @return a list of connection channels.
   */
  public List<ChannelWrapper<?>> getProviderConnections(final String uuid)
  {
    return new ArrayList<>(providerConnections.getValues(uuid));
  }

  /**
   * Get all the provider connections handled by this server.
   * @return a list of connection channels.
   */
  public List<ChannelWrapper<?>> getAllConnections()
  {
    return new ArrayList<>(providerConnections.allValues());
  }

  /**
   * Close and remove all connections accepted by this server.
   * @see org.jppf.server.nio.NioServer#removeAllConnections()
   */
  @Override
  public synchronized void removeAllConnections()
  {
    if (!isStopped()) return;
    providerConnections.clear();
    super.removeAllConnections();
  }

  @Override
  public boolean isIdle(final ChannelWrapper<?> channel)
  {
    return ClassState.IDLE_PROVIDER == channel.getContext().getState();
  }
}
