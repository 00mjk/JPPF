/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

package org.jppf.server.protocol;

import java.io.*;

/**
 * 
 * @param <T> the type of this location.
 * @author Laurent Cohen
 */
public interface Location<T>
{
	/**
	 * Get the path for this location.
	 * @return the path as a string.
	 */
	T getPath();
	/**
	 * Obtain an input stream to read from this location.
	 * @return an <code>InputStream</code> instance.
	 * @throws Exception if an I/O error occurs.
	 */
	InputStream getInputStream() throws Exception;
	/**
	 * Obtain an output stream to write to this location.
	 * @return an <code>OutputStream</code> instance.
	 * @throws Exception if an I/O error occurs.
	 */
	OutputStream getOutputStream() throws Exception;
	/**
	 * Copy the content at this location to another location.
	 * @param location the location to copy to.
	 * @throws Exception if an I/O error occurs.
	 */
	void copyTo(Location location) throws Exception;
	/**
	 * Get the size of the data this location points to.
	 * @return the size as a long value, or -1 if the size is not available.
	 */
	long size();
}