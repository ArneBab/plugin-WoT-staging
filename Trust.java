/**
 * This code is part of WoT, a plugin for Freenet. It is distributed 
 * under the GNU General Public License, version 2 (or at your option
 * any later version). See http://www.gnu.org/ for details of the GPL.
 */
package plugins.WoT;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import plugins.WoT.exceptions.InvalidParameterException;

import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;

/**
 * A trust relationship between two Identities
 * 
 * @author Julien Cornuwel (batosai@freenetproject.org)
 *
 */
public class Trust {

	/* We use a reference to the truster here rather that storing the trustList in the Identity.
	 * This allows us to load only what's needed in memory instead of everything.
	 * Maybe db4o can handle this, I don't know ATM.
	 */
	private final Identity truster;
	private final Identity trustee;
	private byte value;
	private String comment;
	
	/**
	 * Creates a Trust from given parameters.
	 * 
	 * @param truster Identity that gives the trust
	 * @param trustee Identity that receives the trust
	 * @param value Numeric value of the Trust
	 * @param comment A comment to explain the numeric trust value
	 * @throws InvalidParameterException if the trust value is not between -100 and +100
	 */
	public Trust(Identity truster, Identity trustee, byte value, String comment) throws InvalidParameterException {
		this.truster = truster;
		this.trustee = trustee;
		setValue(value);
		setComment(comment);
	}
	
	/**
	 * Counts the number of Trust objects stored in the database
	 * 
	 * @param db A reference to the database
	 * @return the number of Trust objects stored in the database
	 */
	public static int getNb(ObjectContainer db) {
		ObjectSet<Trust> trusts = db.queryByExample(Trust.class);
		return trusts.size();
	}
	
	/**
	 * Exports this trust relationship to XML format.
	 * 
	 * @param xmlDoc the XML {@link Document} this trust will be inserted in 
	 * @return The XML {@link Element} describing this trust relationship
	 */
	public synchronized Element toXML(Document xmlDoc) {
		Element elem = xmlDoc.createElement("Trust");
		elem.setAttribute("Identity", trustee.getRequestURI().toString());
		elem.setAttribute("value", String.valueOf(value));
		elem.setAttribute("Comment", comment);
		
		return elem;
	}
	
	@Override
	public synchronized String toString() {
		return getTruster().getNickName() + " trusts " + getTrustee().getNickName() + " (" + getValue() + " : " + getComment() + ")";
	}

	/**
	 * @return The Identity that gives this trust
	 */
	public Identity getTruster() {
		return truster;
	}

	/**
	 * @return trustee The Identity that receives this trust
	 */
	public Identity getTrustee() {
		return trustee;
	}

	/**
	 * @return value Numeric value of this trust relationship
	 */
	public synchronized byte getValue() {
		return value;
	}

	/**
	 * @param value Numeric value of this trust relationship [-100;+100] 
	 * @throws InvalidParameterException if value isn't in the range
	 */
	public synchronized void setValue(byte newValue) throws InvalidParameterException {
		if(newValue < -100 || newValue > 100) 
			throw new InvalidParameterException("Invalid trust value ("+value+")");
		
		value = newValue;
	}

	/**
	 * @return comment The comment associated to this Trust relationship
	 */
	public synchronized String getComment() {
		return comment;
	}

	/**
	 * @param comment Comment on this trust relationship
	 */
	public synchronized void setComment(String newComment) throws InvalidParameterException {
		assert(newComment != null);
		
		if(newComment != null && newComment.length() > 256)
			throw new InvalidParameterException("Comment is too long (maximum is 256 characters).");
		
		comment = newComment != null ? newComment : "";
	}
}
