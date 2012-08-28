/**************************************************************************************************
 * Copyright (c) 2011 Mihail Atanassov. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors: Mihail Atanassov - initial API and implementation
 *************************************************************************************************/
package de.uni_koeln.ub.drc.ui.rcp;

import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.css.swt.theme.IThemeManager;
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

/**
 * This workbench advisor creates the window advisor, and specifies the
 * perspective id for the initial window.
 * 
 * @author Mihail Atanassov (matana)
 * 
 */
public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

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

}
