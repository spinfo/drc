/**************************************************************************************************
 * Copyright (c) 2010 Fabian Steeg. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors: Fabian Steeg - initial API and implementation
 *************************************************************************************************/

package de.uni_koeln.ub.drc.ui.login;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import com.quui.sinist.XmlDb;

import de.uni_koeln.ub.drc.data.Index;
import de.uni_koeln.ub.drc.data.User;
import de.uni_koeln.ub.drc.ui.Messages;
import de.uni_koeln.ub.drc.ui.facades.SessionContextSingleton;

/**
 * Simple login module implementation.
 * 
 * @author Fabian Steeg (fsteeg), Mihail Atanassov (matana)
 */
public final class SimpleLoginModule implements LoginModule {

	private CallbackHandler callbackHandler;
	private boolean loggedIn;
	private Subject subject;
	private User currentUser;

	/**
	 * {@inheritDoc}
	 * 
	 * @see javax.security.auth.spi.LoginModule#initialize(javax.security.auth.Subject,
	 *      javax.security.auth.callback.CallbackHandler, java.util.Map,
	 *      java.util.Map)
	 */
	@SuppressWarnings("rawtypes")
	/* from implemented API */
	@Override
	public void initialize(final Subject subject,
			final CallbackHandler callbackHandler, final Map sharedState,
			final Map options) {
		this.subject = subject;
		this.callbackHandler = callbackHandler;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see javax.security.auth.spi.LoginModule#login()
	 */
	@Override
	public boolean login() throws LoginException {
		String userName = System.getProperty("user.name"); //$NON-NLS-1$
		String userPass = System.getProperty("user.pass"); //$NON-NLS-1$
		if (userName == null || userPass == null) {
			NameCallback userCallback = new NameCallback(Messages.get().User);
			PasswordCallback passCallback = new PasswordCallback(
					Messages.get().Password, false);
			try {
				callbackHandler.handle(new Callback[] { userCallback,
						passCallback });
			} catch (IOException e) {
				e.printStackTrace();
			} catch (UnsupportedCallbackException e) {
				e.printStackTrace();
			}
			userName = userCallback.getName();
			userPass = passCallback.getPassword() != null ? new String(
					passCallback.getPassword()) : ""; //$NON-NLS-1$
		}
		return authenticate(userName, userPass);
	}

	private boolean authenticate(final String name, final String pass) {
		User candidate = null;
		try {
			String defaultCollection = Index.DefaultCollection();
			SessionContextSingleton instance = SessionContextSingleton
					.getInstance();
			XmlDb userDb = instance.getUserDb();
			candidate = User.withId(defaultCollection, userDb, name);
		} catch (Throwable x) {
			x.printStackTrace();
		}
		if (validLogin(candidate, pass)) {
			currentUser = candidate;
			loggedIn = true;
			System.out.println("Logged in " + getDate() + " : " + currentUser); //$NON-NLS-1$
		}
		return loggedIn;
	}

	private boolean validLogin(User candidate, final String pass) {
		return candidate.pass().equals(pass);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see javax.security.auth.spi.LoginModule#commit()
	 */
	@Override
	public boolean commit() throws LoginException {
		subject.getPublicCredentials()
				.add(String
						.format("%s " + "" + " %s (%s)", currentUser.name(), currentUser.region(), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								currentUser.id()));
		subject.getPrivateCredentials().add(currentUser);
		return loggedIn;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see javax.security.auth.spi.LoginModule#abort()
	 */
	@Override
	public boolean abort() throws LoginException {
		loggedIn = false;
		return true;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see javax.security.auth.spi.LoginModule#logout()
	 */
	@Override
	public boolean logout() throws LoginException {
		loggedIn = false;
		return true;
	}

	private String getDate() {
		SimpleDateFormat dateformatter = new SimpleDateFormat(
				"E yyyy.MM.dd 'at' hh:mm:ss a"); //$NON-NLS-1$
		return dateformatter.format(Calendar.getInstance().getTime());
	}

}
