/**************************************************************************************************
 * Copyright (c) 2010 Fabian Steeg. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors: Fabian Steeg - initial API and implementation
 *************************************************************************************************/

package de.uni_koeln.ub.drc.ui.views;

import java.security.Principal;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import de.uni_koeln.ub.drc.ui.facades.SessionContextSingleton;

/**
 * View to show user details (currently for debugging purpose only).
 * 
 * @author Fabian Steeg (fsteeg)
 */
public final class UserView extends ViewPart {

	/**
	 * The class / UserView ID
	 */
	public static final String ID = UserView.class.getName().toLowerCase();

	private TreeViewer subjectViewer;
	private Subject subject;

	/**
	 * @param parent
	 *            The parent composite for this part
	 */
	@Override
	public void createPartControl(final Composite parent) {
		subjectViewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL);
		try {
			subject = SessionContextSingleton.getInstance().getLoginContext()
					.getSubject();
		} catch (LoginException e) {
			e.printStackTrace();
		}
		subjectViewer.setContentProvider(new SubjectContentProvider());
		subjectViewer.setLabelProvider(new SubjectLabelProvider(subject));
		subjectViewer.setInput(subject);
		GridLayoutFactory.fillDefaults().generateLayout(parent);
	}

	@Override
	public void setFocus() {
	}

	private static class SubjectLabelProvider extends LabelProvider {
		private final Subject subject;

		public SubjectLabelProvider(final Subject subject) {
			this.subject = subject;
		}

		@Override
		public String getText(final Object object) {
			if (object == this.subject) {
				return "User Subject (" + object.getClass().getName() + ")"; //$NON-NLS-1$//$NON-NLS-2$
			} else if (object == this.subject.getPrincipals()) {
				return "Principals (" + Set.class.getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			} else if (object == this.subject.getPublicCredentials()) {
				return "Public Credentials (" + Set.class.getName() + ")";//$NON-NLS-1$ //$NON-NLS-2$
			} else if (object == this.subject.getPrivateCredentials()) {
				return "Private Credentials (" + Set.class.getName() + ")";//$NON-NLS-1$ //$NON-NLS-2$
			} else if (object instanceof Principal) {
				return "Name: " + ((Principal) object).getName() + " ("//$NON-NLS-1$ //$NON-NLS-2$
						+ object.getClass().getName() + ")"; //$NON-NLS-1$
			}
			return object.getClass().getName() + " [" + object.toString() + "]";//$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private static class SubjectContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getElements(final Object inputElement) {
			/* For a subject, get the children: */
			if (inputElement instanceof Subject) {
				return getChildren(inputElement);
			}
			return new Object[] {};
		}

		@Override
		public Object[] getChildren(final Object parentElement) {
			if (parentElement instanceof Subject) {
				return new Object[] {
						((Subject) parentElement).getPrincipals(),
						((Subject) parentElement).getPublicCredentials(),
						((Subject) parentElement).getPrivateCredentials() };
			} else if (parentElement instanceof Set) {
				return ((Set<?>) parentElement).toArray();
			} else {
				return null;
			}
		}

		@Override
		public Object getParent(final Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(final Object element) {
			if (element instanceof Subject) {
				return true;
			} else if (element instanceof Set) {
				return !((Set<?>) element).isEmpty();
			}
			return false;
		}

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(final Viewer viewer, final Object oldInput,
				final Object newInput) {
		}
	}

}
