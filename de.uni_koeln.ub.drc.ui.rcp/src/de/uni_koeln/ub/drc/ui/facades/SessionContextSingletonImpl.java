/**************************************************************************************************
 * Copyright (c) 2012 Mihail Atanassov. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors: Mihail Atanassov - initial API and implementation
 *************************************************************************************************/
package de.uni_koeln.ub.drc.ui.facades;

import java.net.URL;
import java.util.Collections;

import javax.security.auth.login.LoginException;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.security.auth.ILoginContext;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import com.quui.sinist.XmlDb;

import de.uni_koeln.ub.drc.data.Index;
import de.uni_koeln.ub.drc.data.User;
import de.uni_koeln.ub.drc.ui.DrcUiActivator;

/**
 * Provides separate session contexts for each logged in user (RAP).
 * 
 * @author Claes Neuefeind (claesn), Mihaial Atanassov (matana)
 * 
 */
public class SessionContextSingletonImpl implements
		ISessionContextSingletonProvider {

	private static SessionContextSingleton singleton;
	private ILoginContext loginContext;
	private XmlDb db;

	@Override
	public Object getInstanceInternal() {
		if (singleton == null)
			singleton = new SessionContextSingleton();
		return singleton;
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
		return new XmlDb("http://bob.spinfo.uni-koeln.de", 8080, "drc", "crd"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
		logout();
		stopBundle();

	}

	private void stopBundle() {
		try {
			DrcUiActivator.getDefault().stop(
					DrcUiActivator.getDefault().getBundleContext());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void logout() {
		try {
			loginContext.logout();
		} catch (LoginException e) {
			e.printStackTrace();
		}
	}

}
