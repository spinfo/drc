/**************************************************************************************************
 * Copyright (c) 2011 Mihail Atanassov. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors: Mihail Atanassov - initial API and implementation
 *************************************************************************************************/
package de.uni_koeln.ub.drc.ui.rap;

import java.net.URL;

import javax.security.auth.login.LoginException;

import org.eclipse.equinox.security.auth.ILoginContext;
import org.eclipse.equinox.security.auth.LoginContextFactory;
import org.eclipse.rwt.RWT;
import org.eclipse.rwt.lifecycle.IEntryPoint;
import org.eclipse.rwt.service.SessionStoreEvent;
import org.eclipse.rwt.service.SessionStoreListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.osgi.framework.BundleContext;

import de.uni_koeln.ub.drc.ui.DrcUiActivator;
import de.uni_koeln.ub.drc.ui.facades.SessionContextSingleton;

/**
 * @author Mihail Atanassov (matana)
 */
public class EntryPoint implements IEntryPoint {

	/**
	 * The class ID
	 */
	public static final String ID = EntryPoint.class.getName().toLowerCase();
	private final String JAAS_CONFIG_FILE = "jaas_config"; //$NON-NLS-1$
	private String configName = "SIMPLE"; //$NON-NLS-1$

	@Override
	public int createUI() {
		Display display = PlatformUI.createDisplay();
		WorkbenchAdvisor advisor = new ApplicationWorkbenchAdvisor();
		BundleContext bundleContext = DrcUiActivator.getDefault().getBundle()
				.getBundleContext();
		login(bundleContext);
		RWT.getSessionStore().getHttpSession().setMaxInactiveInterval(300);
		RWT.getSessionStore().addSessionStoreListener(
				new SessionStoreListener() {

					@Override
					public void beforeDestroy(SessionStoreEvent event) {
						System.out
								.println("Session time out - changes were saved before destroying the session: ID=" //$NON-NLS-1$
										+ event.getSessionStore()
												.getHttpSession().getId());
					}
				});
		return PlatformUI.createAndRunWorkbench(display, advisor);
	}

	private void login(BundleContext bundleContext) {
		URL configUrl = bundleContext.getBundle().getEntry(JAAS_CONFIG_FILE);
		ILoginContext loginContext = LoginContextFactory.createContext(
				configName, configUrl);
		try {
			loginContext.login();
			SessionContextSingleton.getInstance().setLoginContext(loginContext);
		} catch (LoginException e) {
			e.printStackTrace();
		}
	}
}
