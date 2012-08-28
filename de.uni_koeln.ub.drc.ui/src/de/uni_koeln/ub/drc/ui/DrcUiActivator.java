/**************************************************************************************************
 * Copyright (c) 2010 Fabian Steeg. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors: Fabian Steeg - initial API and implementation
 *************************************************************************************************/
package de.uni_koeln.ub.drc.ui;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
/**
 * @author Fabian Steeg (fsteeg), Mihail Atanassov (matana), Claes Neuefeind
 *         (claesn)
 * 
 */
public class DrcUiActivator extends Plugin {

	/**
	 * The plug-in ID
	 */
	public static final String PLUGIN_ID = "de.uni_koeln.ub.drc.ui"; //$NON-NLS-1$
	// The shared instance
	private static DrcUiActivator plugin;
	private BundleContext context;

	/**
	 * The user ID of the OCR
	 */
	public static final String OCR_ID = "OCR"; //$NON-NLS-1$

	/**
	 * The portal page root address for a user
	 */
	public static final String PROFILE_ROOT = "http://bob.spinfo.uni-koeln.de:9000/application/user?id="; //$NON-NLS-1$

	/**
	 * The constructor
	 */
	public DrcUiActivator() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		this.context = context;
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static DrcUiActivator getDefault() {
		return plugin;
	}

	/**
	 * @return The BundleContext
	 */
	public BundleContext getBundleContext() {
		return context;
	}

}
