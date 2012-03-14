package de.uni_koeln.ub.drc.ui;

import org.eclipse.equinox.security.auth.ILoginContext;

public class SessionContext {

	private ILoginContext loginContext;

	public ILoginContext getLoginContext() {
		return loginContext;
	}

	public void setLoginContext(ILoginContext loginContext) {
		this.loginContext = loginContext;
	}

}