/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.server.queue;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.*;
import org.jppf.server.protocol.JPPFTaskBundle;

/**
 * Abstract superclass for all JPPFQueue implementations.
 * @author Laurent Cohen
 */
public abstract class AbstractJPPFQueue implements JPPFQueue
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(AbstractJPPFQueue.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The current list of listeners to this queue.
	 */
	protected List<QueueListener> listeners = new LinkedList<QueueListener>();
	/**
	 * Used for synchronized access to the queue.
	 */
	protected ReentrantLock lock = new ReentrantLock();
	/**
	 * An ordered map of bundle sizes, mapping to a list of bundles of this size.
	 */
	protected TreeMap<Integer, List<JPPFTaskBundle>> sizeMap = new TreeMap<Integer, List<JPPFTaskBundle>>();
	/**
	 * 
	 */
	protected int latestMaxSize = 0;

	/**
	 * Get the bundle size to use for bundle size tuning.
	 * @param bundle the bundle to get the size from.
	 * @return the bundle size as an int.
	 */
	protected int getSize(JPPFTaskBundle bundle)
	{
		//return bundle.getTaskCount();
		return bundle.getInitialTaskCount();
	}

	/**
	 * Add a listener to the current list of listeners to this queue.
	 * @param listener the listener to add.
	 * @see org.jppf.server.queue.JPPFQueue#addListener(org.jppf.server.queue.QueueListener)
	 */
	public void addListener(QueueListener listener)
	{
		listeners.add(listener);
	}

	/**
	 * Remove a listener from the current list of listeners to this queue.
	 * @param listener the listener to remove.
	 * @see org.jppf.server.queue.JPPFQueue#removeListener(org.jppf.server.queue.QueueListener)
	 */
	public void removeListener(QueueListener listener)
	{
		listeners.remove(listener);
	}

	/**
	 * Compute and log information about the sizeMap as a debug statement. 
	 */
	protected void logSizeMapInfo()
	{
		log.debug("sizeMap size = " + sizeMap.size());
		int count = 0;
		for (List<JPPFTaskBundle> list: sizeMap.values()) count += list.size();
		log.debug("total bundles in sizeMap = " + count);
	}

	/**
	 * Get the next object in the queue.
	 * This method only returns null and must be overriden by subclasses that want to use it.
	 * @param nbTasks the maximum number of tasks to get out of the bundle.
	 * @return the most recent object that was added to the queue.
	 * @see org.jppf.server.queue.JPPFQueue#nextBundle(int)
	 */
	public JPPFTaskBundle nextBundle(int nbTasks)
	{
		return null;
	}

	/**
	 * Get the next object in the queue.
	 * This method only returns null and must be overriden by subclasses that want to use it.
	 * @param bundle the bundle to either remove or extract a sub-bundle from.
	 * @param nbTasks the maximum number of tasks to get out of the bundle.
	 * @return the most recent object that was added to the queue.
	 * @see org.jppf.server.queue.JPPFQueue#nextBundle(int)
	 */
	public JPPFTaskBundle nextBundle(JPPFTaskBundle bundle, int nbTasks)
	{
		return null;
	}
}
