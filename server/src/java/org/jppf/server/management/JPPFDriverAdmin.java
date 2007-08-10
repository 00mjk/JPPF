/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
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

package org.jppf.server.management;

import static org.jppf.server.protocol.BundleParameter.*;

import javax.crypto.SecretKey;

import org.apache.commons.logging.*;
import org.jppf.JPPFException;
import org.jppf.management.*;
import org.jppf.security.*;
import org.jppf.server.*;
import org.jppf.server.protocol.BundleParameter;
import org.jppf.server.scheduler.bundle.*;
import org.jppf.utils.LocalizationUtils;

/**
 * Instances of this class encapsulate the administration functionalities for a JPPF driver.
 * @author Laurent Cohen
 */
public class JPPFDriverAdmin implements JPPFDriverAdminMBean
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFDriverAdmin.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Base name used for localization lookups";
	 */
	private static final String I18N_BASE = "org.jppf.server.i18n.messages";

	/**
	 * Perform an administration request specified by its parameters.
	 * @param request an object specifying the request parameters.
	 * @return a <code>JPPFManagementResponse</code> instance.
	 * @see org.jppf.management.JPPFAdminMBean#performAdminRequest(java.util.Map)
	 */
	public JPPFManagementResponse performAdminRequest(JPPFManagementRequest<BundleParameter, Object> request)
	{
		if (debugEnabled) log.debug("received request: " + request);
		JPPFManagementResponse response = null;
		try
		{
			BundleParameter command = (BundleParameter) request.getParameter(COMMAND_PARAM);
			switch(command)
			{
				case SHUTDOWN:
				case SHUTDOWN_RESTART:
					response = restartShutdown(request, command);
					break;
	
				case CHANGE_PASSWORD:
					response = changePassword(request);
					break;
	
				case CHANGE_SETTINGS:
					response = changeBundleSizeSettings(request);
					break;
	
				case REFRESH_SETTINGS:
					break;
	
				case READ_STATISTICS:
					response = readStatistics();
					break;
			}
		}
		catch(Exception e)
		{
			response = new JPPFManagementResponse(e);
		}
		return response;
	}

	/**
	 * Change the administration password.
	 * @return the statistics encapsulated in a <code>JPPFManagementResponse</code> instance.
	 * @throws Exception if the statistics could not be obtained.
	 */
	private JPPFManagementResponse readStatistics() throws Exception
	{
		return new JPPFManagementResponse(JPPFStatsUpdater.getStats(), null);
	}

	/**
	 * Get the latest server statistics.
	 * @param request an object specifying the request parameters.
	 * @return the statistics encapsulated in a <code>JPPFManagementResponse</code> instance.
	 * @throws Exception if the statistics could not be obtained.
	 */
	private JPPFManagementResponse changePassword(JPPFManagementRequest<BundleParameter, Object> request) throws Exception
	{
		checkPassword(request);
		SecretKey tmpKey = getSecretKey(request);
		byte[] b = (byte[]) request.getParameter(NEW_PASSWORD_PARAM);
		String newPwd = new String(CryptoUtils.decrypt(tmpKey, b));
		PasswordManager pm = new PasswordManager();
		pm.savePassword(CryptoUtils.encrypt(newPwd.getBytes()));
		return new JPPFManagementResponse("Password changed", null);
	}

	/**
	 * Change the bundle size tuning settings.
	 * @param request an object specifying the request parameters.
	 * @return the statistics encapsulated in a <code>JPPFManagementResponse</code> instance.
	 * @throws Exception if an error occurred while updating the settings.
	 */
	private JPPFManagementResponse changeBundleSizeSettings(
		JPPFManagementRequest<BundleParameter, Object> request) throws Exception
	{
		checkPassword(request);
		Bundler bundler = BundlerFactory.createBundler(request.getParametersMap(), false);
		JPPFDriver.getInstance().getNodeNioServer().setBundler(bundler);
		boolean manual =
			"manual".equalsIgnoreCase((String) request.getParameter(BUNDLE_TUNING_TYPE_PARAM));
		return new JPPFManagementResponse(
			LocalizationUtils.getLocalized(I18N_BASE, (manual ? "manual" : "automatic") + ".settings.changed"), null);
	}

	/**
	 * Perform a shutdown or restart of the server.
	 * @param request an object specifying the request parameters.
	 * @param command determines whether a restart should be initiated after the shutdown.
	 * @return the statistics encapsulated in a <code>JPPFManagementResponse</code> instance.
	 * @throws Exception if an error occurred while updating the settings.
	 */
	private JPPFManagementResponse restartShutdown(
		JPPFManagementRequest<BundleParameter, Object> request, BundleParameter command) throws Exception
	{
		checkPassword(request);
		long shutdownDelay = (Long) request.getParameter(SHUTDOWN_DELAY_PARAM);
		boolean restart = !SHUTDOWN.equals(command);
		long restartDelay = (Long) request.getParameter(RESTART_DELAY_PARAM);
		JPPFDriver.getInstance().initiateShutdownRestart(shutdownDelay, restart, restartDelay);
		return new JPPFManagementResponse(LocalizationUtils.getLocalized(I18N_BASE, "request.acknowledged"), null);
	}

	/**
	 * Check that a management request contains a valid admin password.
	 * @param request the management request to check.
	 * @throws Exception if the request does not have a valid password.
	 */
	private void checkPassword(JPPFManagementRequest<BundleParameter, Object> request) throws Exception
	{
		SecretKey tmpKey = getSecretKey(request);
		byte[] b = (byte[]) request.getParameter(PASSWORD_PARAM);
		String remotePwd = new String(CryptoUtils.decrypt(tmpKey, b));
		PasswordManager pm = new PasswordManager();
		b = pm.readPassword();
		String localPwd = new String(CryptoUtils.decrypt(b));

		if (!localPwd.equals(remotePwd))
			throw new JPPFException(LocalizationUtils.getLocalized(I18N_BASE, "invalid.password"));
	}

	/**
	 * Get the secret key embedded in a management request.
	 * @param request the request to get the key from. 
	 * @return a <code>SecretKey</code> instance.
	 * @throws Exception if the key could not be obtained.
	 */
	private SecretKey getSecretKey(JPPFManagementRequest<BundleParameter, Object> request) throws Exception
	{
		byte[] b = (byte[]) request.getParameter(KEY_PARAM);
		b = CryptoUtils.decrypt(b);
		return CryptoUtils.getSecretKeyFromEncoded(b);
	}
}
