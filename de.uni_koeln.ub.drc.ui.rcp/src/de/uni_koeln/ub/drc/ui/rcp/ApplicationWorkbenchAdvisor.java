/**************************************************************************************************
 * Copyright (c) 2011 Mihail Atanassov. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors: Mihail Atanassov - initial API and implementation
 *************************************************************************************************/
package de.uni_koeln.ub.drc.ui.rcp;

import java.net.URL;

import javax.security.auth.login.LoginException;

import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.css.swt.theme.IThemeManager;
import org.eclipse.equinox.security.auth.ILoginContext;
import org.eclipse.equinox.security.auth.LoginContextFactory;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import de.uni_koeln.ub.drc.ui.ApplicationWorkbenchWindowAdvisor;
import de.uni_koeln.ub.drc.ui.DrcUiActivator;
import de.uni_koeln.ub.drc.ui.Messages;

/**
 * This workbench advisor creates the window advisor, and specifies the
 * perspective id for the initial window.
 * 
 * @author Mihail Atanassov (matana)
 * 
 */
public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

	private static final String JAAS_CONFIG_FILE = "jaas_config"; //$NON-NLS-1$
	private ILoginContext loginContext;
	private static final String PERSPECTIVE_ID = "de.uni_koeln.ub.drc.ui.perspective"; //$NON-NLS-1$

	@Override
	public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(
			IWorkbenchWindowConfigurer configurer) {
		return new ApplicationWorkbenchWindowAdvisor(configurer);
	}

	@Override
	public void initialize(IWorkbenchConfigurer configurer) {
		super.initialize(configurer);
		Bundle b = FrameworkUtil.getBundle(getClass());
		BundleContext context = b.getBundleContext();
		ServiceReference<?> serviceRef = context
				.getServiceReference(IThemeManager.class.getName());
		IThemeManager themeManager = (IThemeManager) context
				.getService(serviceRef);

		final IThemeEngine engine = themeManager.getEngineForDisplay(Display
				.getCurrent());
		engine.setTheme("de.uni_koeln.ub.drc.ui.rcp.theme", true); //$NON-NLS-1$
		if (serviceRef != null) {
			serviceRef = null;
		}
		if (themeManager != null) {
			themeManager = null;
		}
	}

	@Override
	public String getInitialWindowPerspectiveId() {
		return PERSPECTIVE_ID;
	}

	@Override
	public void postStartup() {
		super.postStartup();
		BundleContext bundleContext = DrcUiActivator.getDefault().getBundle()
				.getBundleContext();
		try {
			login(bundleContext);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void login(BundleContext bundleContext) throws Exception {
		String configName = "SIMPLE"; //$NON-NLS-1$
		System.out.println("bundleContext : " //$NON-NLS-1$
				+ bundleContext.getClass().getName().toLowerCase());
		URL configUrl = bundleContext.getBundle().getEntry(JAAS_CONFIG_FILE);
		loginContext = LoginContextFactory.createContext(configName, configUrl);
		try {
			loginContext.login();
			DrcUiActivator.getDefault().setLoginContext(loginContext);
		} catch (LoginException e) {
			e.printStackTrace();
			boolean retry = MessageDialog.openQuestion(null,
					Messages.get().Error, Messages.get().LoginFailed);
			if (!retry) {
				stop(bundleContext);
				System.exit(0);
			} else {
				login(bundleContext);
			}
		}
	}

	private void stop(BundleContext bundleContext) {
		try {
			DrcUiActivator.getDefault().stop(bundleContext);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
