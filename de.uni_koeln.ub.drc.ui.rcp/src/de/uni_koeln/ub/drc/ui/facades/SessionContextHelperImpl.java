package de.uni_koeln.ub.drc.ui.facades;

import de.uni_koeln.ub.drc.ui.SessionContext;

/**
 * Provides a session context (RCP).
 * 
 * @author Claes Neuefeind (claesn)
 */

public class SessionContextHelperImpl extends SessionContextHelper {

	private SessionContext sessionContext = new SessionContext();

	/**
	 * @return The session context for the logged in user.
	 */
	@Override
	public SessionContext getContextInternal() {
		return this.sessionContext;
	}

}
