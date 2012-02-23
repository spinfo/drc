/**************************************************************************************************
 * Copyright (c) 2010 Mihail Atanassov. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors: Mihail Atanassov - initial API and implementation
 *************************************************************************************************/
package de.uni_koeln.ub.drc.ui.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import de.uni_koeln.ub.drc.ui.Messages;
import de.uni_koeln.ub.drc.ui.facades.IDialogConstantsHelper;
import de.uni_koeln.ub.drc.ui.views.EditView;

/**
 * @author Mihail Atanassov (matana)
 */
public class ExitHandler extends AbstractHandler {
	/**
	 * The class / ExitHandler ID
	 */
	public static final String ID = ExitHandler.class.getName().toLowerCase();

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow activeWorkbenchWindow = HandlerUtil
				.getActiveWorkbenchWindow(event);
		EditView ev = (EditView) activeWorkbenchWindow.getActivePage()
				.findView(EditView.ID);
		if (ev != null && ev.isDirty()) {
			MessageDialog dialog = new MessageDialog(ev.getSite().getShell(),
					Messages.get().SavePage, null,
					Messages.get().CurrentPageModified, MessageDialog.CONFIRM,
					new String[] { IDialogConstantsHelper.getYesLabel(),
							IDialogConstantsHelper.getNoLabel() }, 0);
			dialog.create();
			if (dialog.open() == Window.OK) {
				ev.doSave(new NullProgressMonitor());
			}
		}
		return PlatformUI.getWorkbench().close();
	}

}
