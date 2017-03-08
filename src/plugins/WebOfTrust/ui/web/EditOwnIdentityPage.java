/* This code is part of WoT, a plugin for Freenet. It is distributed 
 * under the GNU General Public License, version 2 (or at your option
 * any later version). See http://www.gnu.org/ for details of the GPL. */
package plugins.WebOfTrust.ui.web;

import plugins.WebOfTrust.OwnIdentity;
import plugins.WebOfTrust.exceptions.UnknownIdentityException;
import plugins.WebOfTrust.introduction.IntroductionPuzzle;
import freenet.clients.http.RedirectException;
import freenet.clients.http.SessionManager.Session;
import freenet.clients.http.ToadletContext;
import freenet.support.HTMLNode;
import freenet.support.api.HTTPRequest;


/**
 * The page where users can edit the currently logged in {@link OwnIdentity}.
 * 
 * @author xor (xor@freenetproject.org)
 * @author Julien Cornuwel (batosai@freenetproject.org)
 */
public class EditOwnIdentityPage extends WebPageImpl {
	
    private OwnIdentity mIdentity;

	/**
	 * @throws RedirectException If the {@link Session} has expired. 
	 */
	public EditOwnIdentityPage(WebInterfaceToadlet toadlet, HTTPRequest myRequest, ToadletContext context) throws UnknownIdentityException, RedirectException {
		super(toadlet, myRequest, context, true);
		
		// super.mLoggedInOwnIdentity is final, but we need to replace the identity in make().
		// Thus, copy it to our own member variable so we can modify it.
		mIdentity = super.mLoggedInOwnIdentity;
	}
	
	@Override
	public void make(final boolean mayWrite) {
			if(mayWrite && mRequest.isPartSet("Edit")) {
				final boolean newPublishTrustList = mRequest.getPartAsStringFailsafe("PublishTrustList", 4).equals("true");
				final boolean newPublishPuzzles = mRequest.getPartAsStringFailsafe("PublishPuzzles", 4).equals("true");
				
				try {
					mWebOfTrust.setPublishTrustList(mIdentity.getID(), newPublishTrustList);
					mWebOfTrust.setPublishIntroductionPuzzles(mIdentity.getID(), newPublishTrustList && newPublishPuzzles);

                    // Update the identity to get the latest state so we display the changed version
                    // in the UI properly.
                    // TODO: Performance: This can be removed once the TODO at
                    // WebPageImpl.getLoggedInOwnIdentityFromHTTPSession() of not cloning the
                    // OwnIdentity has been been resolved. If we didn't clone it, the database would
                    // apply the updates to it as it would be the same object as was updated by the
                    // above setPublish...()
                    synchronized (mWebOfTrust) {
                        mIdentity = mWebOfTrust.getOwnIdentityByID(mIdentity.getID()).clone();
                    }

		            HTMLNode aBox = addContentBox(l10n().getString("EditOwnIdentityPage.SettingsSaved.Header"));
		            aBox.addChild("p", l10n().getString("EditOwnIdentityPage.SettingsSaved.Text"));
				}
				catch(Exception e) {
					new ErrorPage(mToadlet, mRequest, mContext, e).addToPage(this);
				}
			}

			HTMLNode box = addContentBox(l10n().getString("EditOwnIdentityPage.EditIdentityBox.Header", "nickname", mIdentity.getNickname()));
			
			HTMLNode createForm = pr.addFormChild(box, uri.toString(), "EditIdentity");
			
			createForm.addChild("p", new String[] { "style" }, new String[] { "font-size: x-small" },
					l10n().getString("EditOwnIdentityPage.EditIdentityBox.RequestUri") + ": " + mIdentity.getRequestURI().toString());
			
			createForm.addChild("p", new String[] { "style" }, new String[] { "font-size: x-small" },
					l10n().getString("EditOwnIdentityPage.EditIdentityBox.InsertUri") + ": " + mIdentity.getInsertURI().toString());
			
			// TODO Give the user the ability to edit these.
			createForm.addChild("p", l10n().getString("EditOwnIdentityPage.EditIdentityBox.Contexts") + ": " + mIdentity.getContexts());
			createForm.addChild("p", l10n().getString("EditOwnIdentityPage.EditIdentityBox.Properties") + ": " + mIdentity.getProperties());
			
			HTMLNode p = createForm.addChild("p", l10n().getString("EditOwnIdentityPage.EditIdentityBox.PublishTrustList") + ": ");
			if(mIdentity.doesPublishTrustList()) {
				p.addChild("input", new String[] { "type", "name", "value", "checked" },
						new String[] { "checkbox", "PublishTrustList", "true", "checked"});
			}
			else
				p.addChild("input", new String[] { "type", "name", "value" }, new String[] { "checkbox", "PublishTrustList", "true"});
			
			
			p = createForm.addChild("p", l10n().getString("EditOwnIdentityPage.EditIdentityBox.PublishIntroductionPuzzles") + ": ");
			if(mIdentity.hasContext(IntroductionPuzzle.INTRODUCTION_CONTEXT)) {
				p.addChild("input", new String[] { "type", "name", "value", "checked" },
						new String[] { "checkbox", "PublishPuzzles", "true", "checked"});
			}
			else
				p.addChild("input", new String[] { "type", "name", "value" }, new String[] { "checkbox", "PublishPuzzles", "true"});
			
			createForm.addChild("input", new String[] { "type", "name", "value" }, new String[] { "submit", "Edit", l10n().getString("EditOwnIdentityPage.EditIdentityBox.SaveButton") });
	}
}
