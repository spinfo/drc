package de.uni_koeln.ub.drc.ui;

import org.eclipse.equinox.security.auth.ILoginContext;

/**
 * Session based LoginContext for multi-user administration.
 * 
 * @author Claes Neuefeind (claesn)
 * 
 */
public class SessionContext {

	private ILoginContext loginContext;

	/**
	 * @return The login context of current user.
	 */
	public ILoginContext getLoginContext() {
		return loginContext;
	}

	/**
	 * @param loginContext
	 *            The login context of current user.
	 */
	public void setLoginContext(ILoginContext loginContext) {
		this.loginContext = loginContext;
	}

}