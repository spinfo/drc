/**************************************************************************************************
 * Copyright (c) 2012 Mihail Atanassov. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors: Mihail Atanassov - initial API and implementation
 *************************************************************************************************/
package de.uni_koeln.ub.drc.ui.facades;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;

import javax.security.auth.login.LoginException;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.security.auth.ILoginContext;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.rwt.RWT;
import org.eclipse.rwt.SessionSingletonBase;
import org.eclipse.rwt.internal.lifecycle.JavaScriptResponseWriter;
import org.eclipse.rwt.internal.service.ContextProvider;
import org.eclipse.rwt.lifecycle.PhaseEvent;
import org.eclipse.rwt.lifecycle.PhaseId;
import org.eclipse.rwt.lifecycle.PhaseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.quui.sinist.XmlDb;

import de.uni_koeln.ub.drc.data.Index;
import de.uni_koeln.ub.drc.data.User;
import de.uni_koeln.ub.drc.ui.DrcUiActivator;
import de.uni_koeln.ub.drc.ui.views.EditView;

/**
 * Provides separate session contexts for each logged in user (RAP).
 * 
 * @author Claes Neuefeind (claesn), Mihail Atanassov (matana)
 * 
 */
public class SessionContextSingletonImpl implements
		ISessionContextSingletonProvider {

	private ILoginContext loginContext;
	private XmlDb db;

	@Override
	public Object getInstanceInternal() {
		return SessionSingletonBase.getInstance(SessionContextSingleton.class);
	}

	@Override
	public void setLoginContext(ILoginContext loginContext) {
		this.loginContext = loginContext;
	}

	@Override
	public ILoginContext getLoginContext() {
		return loginContext;
	}

	@Override
	public User getCurrentUser() {
		User user = null;
		try {
			user = (User) loginContext.getSubject().getPrivateCredentials()
					.iterator().next();
		} catch (LoginException e) {
			e.printStackTrace();
		}
		return user;
	}

	@Override
	public XmlDb getUserDb() {
		if (Index.LocalDb().isAvailable())
			return Index.LocalDb();
		//		return new XmlDb("hydra1.spinfo.uni-koeln.de", 8080, "guest", "guest"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return new XmlDb("bob.spinfo.uni-koeln.de", 8080, "drc", "crd"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Override
	public XmlDb db() {
		if (db == null) {
			db = Index.LocalDb().isAvailable() ? Index.LocalDb()
					: getCurrentUser().db();
			if (!db.isAvailable()) {
				throw new IllegalStateException(
						"Could not connect to DB: " + db); //$NON-NLS-1$
			}
			DrcUiActivator
					.getDefault()
					.getLog()
					.log(new Status(IStatus.INFO, DrcUiActivator.PLUGIN_ID,
							"Using DB: " + db)); //$NON-NLS-1$
		}
		return db;
	}

	/**
	 * @param location
	 *            The bundle path of the image to load
	 * @return The loaded image
	 */
	@Override
	public Image loadImage(String location) {
		IPath path = new Path(location);
		URL url = FileLocator.find(DrcUiActivator.getDefault().getBundle(),
				path, Collections.EMPTY_MAP);
		ImageDescriptor desc = ImageDescriptor.createFromURL(url);
		return desc.createImage();
	}

	@Override
	public void exit() {
		// saveIfDirty();
		logout();
		redirect();
	}

	private void redirect() {
		final Display display = Display.getCurrent();
		RWT.getLifeCycle().addPhaseListener(new PhaseListener() {

			private static final long serialVersionUID = 1L;

			@Override
			public void afterPhase(PhaseEvent event) {
				if (Display.getCurrent() == null
						|| display == Display.getCurrent()) {
					// Uses a non-public API, but
					// currently this is the only
					// solution
					JavaScriptResponseWriter writer = ContextProvider
							.getStateInfo().getResponseWriter();
					String url = "http://www.crestomazia.ch/application/users"; //$NON-NLS-1$
					writer.write("window.location.href=\"" + url + "\";"); //$NON-NLS-1$ //$NON-NLS-2$
					RWT.getRequest().getSession().setMaxInactiveInterval(1);
					RWT.getLifeCycle().removePhaseListener(this);
				}
			}

			@Override
			public PhaseId getPhaseId() {
				return PhaseId.ANY;
			}

			@Override
			public void beforePhase(PhaseEvent event) {
			};

		});
	}

	private void logout() {
		try {
			System.out
					.println("Logged out " + getDate() + " : " + getCurrentUser()); //$NON-NLS-1$ //$NON-NLS-2$
			loginContext.logout();
		} catch (LoginException e) {
			e.printStackTrace();
		}
	}

	private void saveIfDirty() {
		// TODO Save changes automatically before logout
		EditView ev = (EditView) PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage()
				.findView(EditView.ID);
		if (ev != null && ev.isDirty()) {
			System.out.println("Save last changes..."); //$NON-NLS-1$
			ev.doSave(new NullProgressMonitor());
		}
	}

	private String getDate() {
		SimpleDateFormat dateformatter = new SimpleDateFormat(
				"E yyyy.MM.dd 'at' hh:mm:ss a"); //$NON-NLS-1$
		return dateformatter.format(Calendar.getInstance().getTime());
	}
}
