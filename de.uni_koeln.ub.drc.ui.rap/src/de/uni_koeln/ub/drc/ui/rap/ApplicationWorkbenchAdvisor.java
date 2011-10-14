package de.uni_koeln.ub.drc.ui.rap;

import java.net.URL;

import javax.security.auth.login.LoginException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.security.auth.ILoginContext;
import org.eclipse.equinox.security.auth.LoginContextFactory;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.rwt.RWT;
import org.eclipse.rwt.internal.lifecycle.JavaScriptResponseWriter;
import org.eclipse.rwt.internal.service.ContextProvider;
import org.eclipse.rwt.lifecycle.PhaseEvent;
import org.eclipse.rwt.lifecycle.PhaseId;
import org.eclipse.rwt.lifecycle.PhaseListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.osgi.framework.BundleContext;

import de.uni_koeln.ub.drc.ui.ApplicationWorkbenchWindowAdvisor;
import de.uni_koeln.ub.drc.ui.DrcUiActivator;
import de.uni_koeln.ub.drc.ui.Messages;

/**
 * This workbench advisor creates the window advisor, and specifies the
 * perspective id for the initial window.
 */
@SuppressWarnings("restriction")
public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

	private static final String JAAS_CONFIG_FILE = "jaas_config"; //$NON-NLS-1$
	private ILoginContext loginContext;
	private static final String PERSPECTIVE_ID = "de.uni_koeln.ub.drc.ui.perspective";

	public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(
			IWorkbenchWindowConfigurer configurer) {
		return new ApplicationWorkbenchWindowAdvisor(configurer);
	}

	public String getInitialWindowPerspectiveId() {
		return PERSPECTIVE_ID;
	}

	@Override
	public void initialize(IWorkbenchConfigurer configurer) {
		super.initialize(configurer);
	}

	@Override
	public void postStartup() {
		super.postStartup();
		BundleContext bundleContext = DrcUiActivator.getDefault().getBundle()
				.getBundleContext();
		try {
			login(bundleContext);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void login(BundleContext bundleContext) throws Exception {
		String configName = "SIMPLE"; //$NON-NLS-1$
		System.out.println("bundleContext : "
				+ bundleContext.getClass().getName().toLowerCase());
		URL configUrl = bundleContext.getBundle().getEntry(JAAS_CONFIG_FILE);
		loginContext = LoginContextFactory.createContext(configName, configUrl);
		try {
			loginContext.login();
			DrcUiActivator.getDefault().setLoginContext(loginContext);

		} catch (LoginException e) {
			e.printStackTrace();
			IStatus status = new Status(IStatus.ERROR,
					"de.uni_koeln.ub.drc.ui", "Login failed", e); //$NON-NLS-1$ //$NON-NLS-2$
			int result = ErrorDialog.openError(null, Messages.get().Error,
					Messages.get().LoginFailed, status);
			if (result == ErrorDialog.CANCEL) {
				// stop(bundleContext);
				// System.exit(0);
				redirect();
			} else {
				login(bundleContext);
			}
		}
	}

	@SuppressWarnings("unused")
	private void stop(BundleContext bundleContext) {
		try {
			DrcUiActivator.getDefault().stop(bundleContext);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void postShutdown() {
		super.postShutdown();
		redirect();
	}

	private void redirect() {
		final Display display = Display.getCurrent();
		RWT.getLifeCycle().addPhaseListener(new PhaseListener() {

			private static final long serialVersionUID = 1L;

			public void afterPhase(PhaseEvent event) {
				if (Display.getCurrent() == null
						|| display == Display.getCurrent()) {
					// Uses a non-public API, but currently this is the only
					// solution
					JavaScriptResponseWriter writer = ContextProvider
							.getStateInfo().getResponseWriter();
					String url = "http://www.crestomazia.ch/";
					writer.write("window.location.href=\"" + url + "\";");
					RWT.getRequest().getSession().setMaxInactiveInterval(1);
					RWT.getLifeCycle().removePhaseListener(this);
					logout();
				}
			}

			public PhaseId getPhaseId() {
				return PhaseId.ANY;
			}

			public void beforePhase(PhaseEvent event) {
			};

		});
	}

	private void logout() {
		DrcUiActivator drcUiActivator = DrcUiActivator.getDefault();
		try {
			if (drcUiActivator != null
					&& drcUiActivator.getLoginContext() != null)
				drcUiActivator.getLoginContext().logout();
			// instance.stop(instance.getBundle().getBundleContext());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}