/**************************************************************************************************
 * Copyright (c) 2012 Mihail Atanassov. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors: Mihail Atanassov - initial API and implementation
 *************************************************************************************************/
package de.uni_koeln.ub.drc.ui.facades;

import org.eclipse.equinox.security.auth.ILoginContext;
import org.eclipse.swt.graphics.Image;

import com.quui.sinist.XmlDb;

import de.uni_koeln.ub.drc.data.User;

/**
 * @author Mihail Atanassov (matana)
 * 
 */
public interface ISessionContextSingletonProvider {

	/**
	 * @return Implementation
	 */
	Object getInstanceInternal();

	/**
	 * @param loginContext
	 *            The session context for the logged in user.
	 */
	public void setLoginContext(ILoginContext loginContext);

	/**
	 * @return Login context
	 */
	public ILoginContext getLoginContext();

	/**
	 * @return The user that is currently logged in
	 */
	public User getCurrentUser();

	/**
	 * @return The users DB we are using
	 */
	public XmlDb getUserDb();

	/**
	 * @return The data DB we are using
	 */
	public XmlDb db();

	/**
	 * @param location
	 *            The bundle path of the image to load
	 * @return The loaded image
	 */
	public Image loadImage(String location);

	/**
	 * Exit the application.
	 */
	public void exit();

}
