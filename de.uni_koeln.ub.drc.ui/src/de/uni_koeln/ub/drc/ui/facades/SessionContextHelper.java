package de.uni_koeln.ub.drc.ui.facades;

import de.uni_koeln.ub.drc.ui.SessionContext;

/**
 * Provides one (RCP) or multiple (RAP) session contexts.
 * 
 * @author Claes Neuefeind (claesn)
 * 
 */
public abstract class SessionContextHelper {

	protected static final String BUNDLE_NAME = "plugin"; //$NON-NLS-1$
	private final static SessionContextHelper IMPL;

	static {
		IMPL = (SessionContextHelper) ImplementationLoader
				.newInstance(SessionContextHelper.class);
	}

	/**
	 * @return The session context for the logged in user.
	 */
	public static SessionContext getContext() {
		return IMPL.getContextInternal();
	}

	/**
	 * @return The session context for the logged in user.
	 */
	abstract SessionContext getContextInternal();

}
