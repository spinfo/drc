/**************************************************************************************************
 * Copyright (c) 2011 Mihail Atanassov. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors: Mihail Atanassov - initial API and implementation
 *************************************************************************************************/
package de.uni_koeln.ub.drc.ui;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.contexts.IContextService;

/**
 * @author Mihail Atanassov (matana)
 */
public class PerspectiveAlternative implements IPerspectiveFactory {

	private static final String Perspective_CONTEXT_ID = "de.uni_koeln.ub.drc.ui.alternativecontext"; //$NON-NLS-1$

	@Override
	public void createInitialLayout(IPageLayout layout) {
		layout.setEditorAreaVisible(false);
		layout.setFixed(true);
		activateContext();
	}

	private void activateContext() {
		IContextService contextService = ((IContextService) PlatformUI
				.getWorkbench().getService(IContextService.class));
		contextService.activateContext(Perspective_CONTEXT_ID);
	}
}
