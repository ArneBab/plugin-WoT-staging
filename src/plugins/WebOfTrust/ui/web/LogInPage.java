/* This code is part of Freenet. It is distributed under the GNU General
 * Public License, version 2 (or at your option any later version). See
 * http://www.gnu.org/ for further details of the GPL. */
package plugins.WebOfTrust.ui.web;

import java.util.ArrayList;
import java.util.List;

import plugins.WebOfTrust.OwnIdentity;
import plugins.WebOfTrust.WebOfTrust;
import plugins.WebOfTrust.ui.web.WebInterface.CreateOwnIdentityWebInterfaceToadlet;
import plugins.WebOfTrust.ui.web.WebInterface.LogOutWebInterfaceToadlet;
import plugins.WebOfTrust.ui.web.WebInterface.LoginWebInterfaceToadlet;
import freenet.clients.http.SessionManager;
import freenet.clients.http.ToadletContext;
import freenet.support.HTMLNode;
import freenet.support.api.HTTPRequest;

/**
 * Can be used by third-party plugins by specifying a HTTPRequest param <code>redirect-target</code> in the URI such as:
 * <p><code>http://127.0.0.1:8888/WebOfTrust/LogIn?redirect-target=/my-plugin</code></p>
 * This would redirect the user to <code>http://127.0.0.1:8888/my-plugin</code> after log in.
 * 
 * <p>To use the log-in session cookie to obtain the logged in {@link OwnIdentity}, you would then use {@link SessionManager}.</p>
 * <p>For examples of its usage, see {@link LoginWebInterfaceToadlet}, {@link LogOutWebInterfaceToadlet} and  {@link MyIdentityPage}.</p>
 * 
 * TODO: Improve this documentation. To do so, ask operhiem1 how his plugin uses the session management.
 */
public final class LogInPage extends WebPageImpl {

	/**
	 * To which URI to redirect the client browser after log in. Can be used to allow third-party plugins to use the session management of WOT.
	 */
	private final String target;
	
	/**
	 * Default value of {@link #target}.
	 */
	public static final String DEFAULT_REDIRECT_TARGET_AFTER_LOGIN = WebOfTrust.SELF_URI;

	/**
	 * @param request Checked for param "redirect-target", a node-relative target that the user is redirected to after logging in. This can include a path,
	 *                query, and fragment, but any scheme, host, or port will be ignored. If this parameter is empty or not specified it redirects to
	 *                {@link #DEFAULT_REDIRECT_TARGET_AFTER_LOGIN}. 
	 *                This allows third party plugins to use the session-management of WOT.
	 */
	public LogInPage(WebInterfaceToadlet toadlet, HTTPRequest request, ToadletContext context) {
		super(toadlet, request, context);
		target = request.getParam("redirect-target", DEFAULT_REDIRECT_TARGET_AFTER_LOGIN);
	}

	/**
	 * This does NOT handle the actual login request: When you press the log in button, the {@link LoginWebInterfaceToadlet} deals with processing
	 * the request.
	 * 
	 * The reason for this is that we need to send a HTTP redirect to the page which shall be visible after login.
	 * WebPage is not able to do this.
	 * 
	 * @param mayWrite Is ignored because as said in the description this function does not handle the form submission, {@link LoginWebInterfaceToadlet} does.
	 */
	@Override
	public void make(final boolean mayWrite) {
		makeWelcomeBox();
		
        // TODO: Performance: The synchronized() can be removed after this is fixed:
        // https://bugs.freenetproject.org/view.php?id=6247
		synchronized (mWebOfTrust) {
			// TODO: Performance: Don't convert to ArrayList once the issue which caused this
			// workaround is fixed: https://bugs.freenetproject.org/view.php?id=6646
			final List<OwnIdentity> ownIdentities
				= new ArrayList<OwnIdentity>(mWebOfTrust.getAllOwnIdentities());
			
			if (!ownIdentities.isEmpty()) {
				makeLoginBox(ownIdentities);
				makeCreateIdentityBox();
			} else {
				// Cast because the casted version does not throw RedirectException.
				((CreateOwnIdentityWebInterfaceToadlet)mWebInterface.getToadlet(CreateOwnIdentityWebInterfaceToadlet.class))
					.makeWebPage(mRequest, mContext).addToPage(this);
			}
		}
	}

	private final void makeWelcomeBox() {
		final String[] l10nBoldSubstitutionInput = new String[] { "bold" };
		final HTMLNode[] l10nBoldSubstitutionOutput = new HTMLNode[] { HTMLNode.STRONG };
		
		HTMLNode welcomeBox = addContentBox(l10n().getString("LoginPage.Welcome.Header"));
		l10n().addL10nSubstitution(welcomeBox.addChild("p"), "LoginPage.Welcome.Text1", l10nBoldSubstitutionInput, l10nBoldSubstitutionOutput);
		l10n().addL10nSubstitution(welcomeBox.addChild("p"), "LoginPage.Welcome.Text2", l10nBoldSubstitutionInput, l10nBoldSubstitutionOutput);
		l10n().addL10nSubstitution(welcomeBox.addChild("p"), "LoginPage.Welcome.Text3", l10nBoldSubstitutionInput, l10nBoldSubstitutionOutput);
	}

	private final void makeLoginBox(List<OwnIdentity> ownIdentities) {
		HTMLNode loginBox = addContentBox(l10n().getString("LoginPage.LogIn.Header"));

		HTMLNode selectForm = pr.addFormChild(loginBox, mToadlet.getURI().toString(), mToadlet.pageTitle);
		HTMLNode selectBox = selectForm.addChild("select", "name", "OwnIdentityID");
		for(OwnIdentity ownIdentity : ownIdentities) {
			selectBox.addChild("option", "value", ownIdentity.getID(),
			    ownIdentity.getShortestUniqueNickname());
		}
		// HTMLNode escapes the target value.
		selectForm.addChild("input",
				new String[] { "type", "name", "value" },
				new String[] { "hidden", "redirect-target", target });
		selectForm.addChild("input",
				new String[] { "type", "value" },
				new String[] { "submit", l10n().getString("LoginPage.LogIn.Button") });
		selectForm.addChild("p", l10n().getString("LoginPage.CookiesRequired.Text"));
	}
	
	/**
	 * @param redirectTarget See {@link LogInPage} and {@link #target}.
	 */
	protected static final void addLoginButton(final WebPageImpl page, final HTMLNode contentNode, final OwnIdentity identity, final String redirectTarget) {
		final WebInterfaceToadlet logIn = page.mWebInterface.getToadlet(LoginWebInterfaceToadlet.class);
		final HTMLNode logInForm = page.pr.addFormChild(contentNode, logIn.getURI().toString() , logIn.pageTitle);
		logInForm.addChild("input", new String[] { "type", "name", "value" }, new String[] { "hidden", "OwnIdentityID", identity.getID() });
		logInForm.addChild("input", new String[] { "type", "name", "value" }, new String[] { "hidden", "redirect-target", redirectTarget });
		logInForm.addChild("input", new String[] { "type", "name", "value" }, new String[] { "submit", "submit", page.l10n().getString("LoginPage.LogIn.Button") });
		logInForm.addChild("p", page.l10n().getString("LoginPage.CookiesRequired.Text"));
	}

	private void makeCreateIdentityBox() {
		CreateOwnIdentityWizardPage.addLinkToCreateOwnIdentityWizard(this, target);
	}
}
