/**************************************************************************************************
 * Copyright (c) 2011 Mihail Atanassov. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors: Mihail Atanassov - initial API and implementation
 *************************************************************************************************/
package de.uni_koeln.ub.drc.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

import de.uni_koeln.ub.drc.ui.facades.SessionContextSingleton;

/**
 * Configures the initial size and appearance of a workbench window.
 * 
 * @author Mihail Atanassov (matana)
 * 
 */
public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

	/**
	 * @param configurer
	 *            The IWorkbenchWindowConfigurer
	 */
	public ApplicationWorkbenchWindowAdvisor(
			IWorkbenchWindowConfigurer configurer) {
		super(configurer);
	}

	@Override
	public ActionBarAdvisor createActionBarAdvisor(
			IActionBarConfigurer configurer) {
		return new ApplicationActionBarAdvisor(configurer);
	}

	@Override
	public void preWindowOpen() {
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		configurer.setShowCoolBar(false);
		configurer.setShowStatusLine(false);
		configurer.setTitle("DRC"); //$NON-NLS-1$
		Rectangle rect = Display.getCurrent().getBounds();
		configurer.setInitialSize(new Point(rect.width, rect.height));
		configurer.setShellStyle(SWT.TITLE | SWT.RESIZE | SWT.CLOSE | SWT.MAX
				| SWT.MIN);
		configurer.getWindow().getWorkbench()
				.addWindowListener(new IWindowListener() {

					@Override
					public void windowOpened(IWorkbenchWindow window) {
					}

					@Override
					public void windowDeactivated(IWorkbenchWindow window) {
					}

					@Override
					public void windowClosed(IWorkbenchWindow window) {
						System.out
								.println("Window closed: " + window.toString()); //$NON-NLS-1$
						SessionContextSingleton.getInstance().exit();
					}

					@Override
					public void windowActivated(IWorkbenchWindow window) {
					}
				});
	}
}
