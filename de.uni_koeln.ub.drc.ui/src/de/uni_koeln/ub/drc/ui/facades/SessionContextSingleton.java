package de.uni_koeln.ub.drc.ui.facades;

import org.eclipse.equinox.security.auth.ILoginContext;
import org.eclipse.swt.graphics.Image;

import com.quui.sinist.XmlDb;

import de.uni_koeln.ub.drc.data.User;

/**
 * Provides one (RCP) or multiple (RAP) session contexts.
 * 
 * @author Claes Neuefeind (claesn)
 * 
 */
public class SessionContextSingleton {

	private final static ISessionContextSingletonProvider PROVIDER;

	static {
		PROVIDER = (ISessionContextSingletonProvider) ImplementationLoader
				.newInstance(SessionContextSingleton.class);
	}

	/**
	 * @return The internal instance
	 */
	public static SessionContextSingleton getInstance() {
		return (SessionContextSingleton) PROVIDER.getInstanceInternal();
	}

	/**
	 * @param loginContext
	 *            The ILoginContext
	 */
	public void setLoginContext(ILoginContext loginContext) {
		PROVIDER.setLoginContext(loginContext);
	}

	/**
	 * @return The session context for the logged in user.
	 */
	public ILoginContext getLoginContext() {
		return PROVIDER.getLoginContext();
	}

	/**
	 * @return The user that is currently logged in
	 */
	public User getCurrentUser() {
		return PROVIDER.getCurrentUser();
	}

	/**
	 * @return The users DB we are using
	 */
	public XmlDb getUserDb() {
		return PROVIDER.getUserDb();
	}

	/**
	 * @return The users DB we are using
	 */
	public XmlDb db() {
		return PROVIDER.db();
	}

	/**
	 * @param location
	 *            The bundle path of the image to load
	 * @return The loaded image
	 */
	public Image loadImage(String location) {
		return PROVIDER.loadImage(location);
	}

}
