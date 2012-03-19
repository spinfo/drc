package de.uni_koeln.ub.drc.ui.facades;

import org.eclipse.rwt.SessionSingletonBase;

import de.uni_koeln.ub.drc.ui.SessionContext;

/**
 * Provides separate session contexts for each logged in user (RAP).
 * 
 * @author Claes Neuefeind (claesn)
 * 
 */
public class SessionContextHelperImpl extends SessionContextHelper {

	private SessionContextKeeper msck = new SessionContextKeeper();

	@Override
	public SessionContext getContextInternal() {
		return msck.getContext();
	}

	private class SessionContextKeeper extends SessionSingletonBase {

		/**
		 * @return The session context for the logged in user.
		 */
		private SessionContext getContext() {
			return (SessionContext) getInstance(SessionContext.class);
		}
	}
}
