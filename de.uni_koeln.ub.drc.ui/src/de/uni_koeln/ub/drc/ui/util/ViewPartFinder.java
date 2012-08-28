package de.uni_koeln.ub.drc.ui.util;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;

/**
 * @author matana (saeko.bjagai@gmail.com)
 * 
 */
public class ViewPartFinder {

	/**
	 * @param clazz
	 *            The class literal of the view to find (needs an ID field)
	 * @return The view corresponding to the given class literal, or null
	 */
	public static <T extends IViewPart> T find(Class<T> clazz) {
		try {
			String id = (String) clazz.getField("ID").get(null); //$NON-NLS-1$
			@SuppressWarnings("unchecked")
			T view = (T) PlatformUI.getWorkbench().getActiveWorkbenchWindow()
					.getActivePage().findView(id);
			return view;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		}
		return null;
	}

}
