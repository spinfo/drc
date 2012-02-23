/**************************************************************************************************
 * Copyright (c) 2011 Mihail Atanassov. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors: Mihail Atanassov - initial API and implementation
 *************************************************************************************************/
package de.uni_koeln.ub.drc.ui;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

/**
 * Creates, adds and disposes actions for the menus and action bars of each
 * workbench window.
 * 
 * @author Mihail Atanassov (matana)
 * 
 */
public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

	/**
	 * @param configurer
	 *            The IActionBarConfigurer
	 */
	public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
		super(configurer);
	}

	/*
	 * Actions - important to allocate these only in makeActions, and then use
	 * them in the fill methods. This ensures that the actions aren't recreated
	 * in the fill methods.
	 */

	@Override
	protected void makeActions(IWorkbenchWindow window) {
		// Commands are registered in plugin.xml
	}

	@Override
	protected void fillMenuBar(IMenuManager menuBar) {
		// Menu is filled in plugin.xml
	}

}
