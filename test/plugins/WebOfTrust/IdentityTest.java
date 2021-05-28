/* This code is part of WoT, a plugin for Freenet. It is distributed 
 * under the GNU General Public License, version 2 (or at your option
 * any later version). See http://www.gnu.org/ for details of the GPL. */
package plugins.WebOfTrust;

import static plugins.WebOfTrust.util.DateUtil.waitUntilCurrentTimeUTCIsAfter;

import java.net.MalformedURLException;
import java.util.Date;

import org.junit.Ignore;

import plugins.WebOfTrust.Identity.FetchState;
import plugins.WebOfTrust.Identity.IdentityID;
import plugins.WebOfTrust.exceptions.InvalidParameterException;
import plugins.WebOfTrust.exceptions.UnknownIdentityException;

import com.db4o.ObjectSet;

import freenet.keys.FreenetURI;
import freenet.support.Base64;
import freenet.support.CurrentTimeUTC;

/**
 * @author xor (xor@freenetproject.org) where not specified otherwise
 */
public final class IdentityTest extends AbstractJUnit3BaseTest {
	
	private final String requestUriString = "USK@sdFxM0Z4zx4-gXhGwzXAVYvOUi6NRfdGbyJa797bNAg,ZP4aASnyZax8nYOvCOlUebegsmbGQIXfVzw7iyOsXEc,AQACAAE/WebOfTrust/23";
	private final String requestUriStringSSK = "SSK@sdFxM0Z4zx4-gXhGwzXAVYvOUi6NRfdGbyJa797bNAg,ZP4aASnyZax8nYOvCOlUebegsmbGQIXfVzw7iyOsXEc,AQACAAE/WebOfTrust/23";
	private final String requestUriStringSSKPlain = "SSK@sdFxM0Z4zx4-gXhGwzXAVYvOUi6NRfdGbyJa797bNAg,ZP4aASnyZax8nYOvCOlUebegsmbGQIXfVzw7iyOsXEc,AQACAAE/";
	private final String insertUriString = "USK@ZTeIa1g4T3OYCdUFfHrFSlRnt5coeFFDCIZxWSb7abs,ZP4aASnyZax8nYOvCOlUebegsmbGQIXfVzw7iyOsXEc,AQECAAE/WebOfTrust/23";
	private final String insertUriStringSSK = "SSK@ZTeIa1g4T3OYCdUFfHrFSlRnt5coeFFDCIZxWSb7abs,ZP4aASnyZax8nYOvCOlUebegsmbGQIXfVzw7iyOsXEc,AQECAAE/WebOfTrust/23";
	private final String insertUriStringSSKPlain = "SSK@ZTeIa1g4T3OYCdUFfHrFSlRnt5coeFFDCIZxWSb7abs,ZP4aASnyZax8nYOvCOlUebegsmbGQIXfVzw7iyOsXEc,AQECAAE/";
	
	private FreenetURI requestUri;
	private FreenetURI requestUriSSK;
	private FreenetURI requestUriSSKPlain;
	private FreenetURI insertUri;
	private FreenetURI insertUriSSK;
	private FreenetURI insertUriSSKPlain;
	
	private Identity identity;
	
	/**
	 * @author Julien Cornuwel (batosai@freenetproject.org)
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		requestUri = new FreenetURI(requestUriString);
		requestUriSSK = new FreenetURI(requestUriStringSSK);
		requestUriSSKPlain = new FreenetURI(requestUriStringSSKPlain);
		insertUri = new FreenetURI(insertUriString);
		insertUriSSK = new FreenetURI(insertUriStringSSK);
		insertUriSSKPlain = new FreenetURI(insertUriStringSSKPlain);

		identity = new Identity(mWoT, requestUri, "test", true);
		identity.addContext("bleh");
		identity.setProperty("testproperty","foo1a");
		identity.storeAndCommit();
		
		// TODO: Modify the test to NOT keep a reference to the identities as member variables so the following also garbage collects them.
		flushCaches();
	}
	
	/**
	 * Tests whether {@link Identity.clone()} returns an Identity which:
	 * - which {@link equals()} the original.
	 * - which is not the same object.
	 * - which meets the requirements of {@link AbstractJUnit3BaseTest#testClone(Class, Object, Object)}
	 */
	public void testClone() throws MalformedURLException, InvalidParameterException, IllegalArgumentException, IllegalAccessException, InterruptedException {
		final Identity original = new Identity(mWoT, getRandomSSKPair()[1], getRandomLatinString(Identity.MAX_NICKNAME_LENGTH), true);
		
		waitUntilCurrentTimeUTCIsAfter(original.getCreationDate());
		original.onFetchedAndParsedSuccessfully(10);
		assertTrue(original.getLastFetchedDate().after(original.getCreationDate()));
		
		waitUntilCurrentTimeUTCIsAfter(original.getLastFetchedDate());
		original.updated();
		assertTrue(original.getLastChangeDate().after(original.getLastFetchedDate()));
		
		waitUntilCurrentTimeUTCIsAfter(original.getLastChangeDate());
		
		original.setNewEditionHint(20); // Make sure to use a non-default edition hint
		original.addContext(getRandomLatinString(Identity.MAX_CONTEXT_NAME_LENGTH));
		original.setProperty(getRandomLatinString(Identity.MAX_PROPERTY_NAME_LENGTH),
		                     getRandomLatinString(Identity.MAX_PROPERTY_VALUE_LENGTH));
		
		final Identity clone = original.clone();
		
		assertEquals(original, clone);
		assertNotSame(original, clone);
		
		testClone(Persistent.class, original, clone);
		testClone(Identity.class, original, clone);
	}
	
	public void testSerializeDeserialize() throws MalformedURLException, InvalidParameterException {
		final Identity original = new Identity(mWoT, getRandomSSKPair()[1], getRandomLatinString(Identity.MAX_NICKNAME_LENGTH), true);
		final Identity deserialized = (Identity)Persistent.deserialize(mWoT, original.serialize());
		
		assertNotSame(original, deserialized);
		assertEquals(original, deserialized);
	}
	
	public void testConstructors() throws MalformedURLException, InvalidParameterException {
		String nickname = getRandomLatinString(Identity.MAX_NICKNAME_LENGTH);
		String uri = "USK@sdFxM0Z4zx4-gXhGwzXAVYvOUi6NRfdGbyJa797bNAg,ZP4aASnyZax8nYOvCOlUebegsmbGQIXfVzw7iyOsXEc,AQACAAE/WebOfTrust/5";
		
		final Identity identity = new Identity(mWoT, uri, nickname, true);
		
		assertSame(mWoT, identity.getWebOfTrust());
		
		// The edition of the URI we provide to the constructor must be ignored for security
		// reasons: The URI may have been obtained from a remote peer, and they might maliciously
		// hand out a very high edition number which does not actually exist in order to block the
		// download of the target Identity indefinitely.
		assertEquals(new FreenetURI(uri).setSuggestedEdition(0), identity.getRequestURI());
		
		assertEquals(FetchState.NotFetched, identity.getCurrentEditionFetchState());
		assertEquals(-1, identity.getLastFetchedEdition());
		assertEquals(-1, identity.getLastFetchedMaybeValidEdition());
		assertEquals(0, identity.getNextEditionToFetch());
		assertEquals(5, identity.getLatestEditionHint());
		
		assertEquals(nickname, identity.getNickname());
		
		assertEquals(true, identity.doesPublishTrustList());
		
		// TODO: Code quality: Test the other constructor(s), currently only tested implicitly by
		// being called by the one we just tested.
		// TODO: Code quality: Test with different / invalid parameters
		// TODO: Code quality: Test handling of a negative edition being passed in the FreenetURI.
	}
	
	public void testInsertRequestUriMixup() throws InvalidParameterException {		
		try {
			new Identity(mWoT, new FreenetURI(insertUriString), "test", true);
			fail("Identity creation with insert URI instead of request URI allowed!");
		} catch (MalformedURLException e) {
			// This is what we expect.
		}
		
		try {
			new OwnIdentity(mWoT, requestUri, "test", true);
			fail("OwnIdentity creation with request URI instead of insert URI allowed!");
		} catch (MalformedURLException e) {
			// This is what we expect.
		}
	}
	
	/**
	 * @author Julien Cornuwel (batosai@freenetproject.org)
	 */
	public void testIdentityStored() {
		ObjectSet<Identity> result = mWoT.getAllIdentities();
		assertEquals(1, result.size());
		
		assertEquals(identity, result.next());
	}

	/**
	 * TODO: Move to WoTTest
	 * @author Julien Cornuwel (batosai@freenetproject.org)
	 */
	public void testGetByURI() throws MalformedURLException, UnknownIdentityException {
		assertEquals(identity, mWoT.getIdentityByURI(requestUri));
		assertEquals(identity, mWoT.getIdentityByURI(requestUriSSK));
		assertEquals(identity, mWoT.getIdentityByURI(requestUriSSKPlain));
		assertEquals(identity, mWoT.getIdentityByURI(requestUriString));
		assertEquals(identity, mWoT.getIdentityByURI(requestUriStringSSK));
		assertEquals(identity, mWoT.getIdentityByURI(requestUriStringSSKPlain));
		assertEquals(identity, mWoT.getIdentityByURI(insertUri));
		assertEquals(identity, mWoT.getIdentityByURI(insertUriSSK));
		assertEquals(identity, mWoT.getIdentityByURI(insertUriSSKPlain));
		assertEquals(identity, mWoT.getIdentityByURI(insertUriString));
		assertEquals(identity, mWoT.getIdentityByURI(insertUriStringSSK));
		assertEquals(identity, mWoT.getIdentityByURI(insertUriStringSSKPlain));
	}

	/**
	 * @author Julien Cornuwel (batosai@freenetproject.org)
	 */
	public void testContexts() throws InvalidParameterException {
		assertFalse(identity.hasContext("foo"));
		identity.addContext("test");
		assertTrue(identity.hasContext("test"));
		identity.removeContext("test");
		assertFalse(identity.hasContext("test"));
		
		/* TODO: Obtain the identity from the db between each line ... */
	}

	/**
	 * @author Julien Cornuwel (batosai@freenetproject.org)
	 */
	public void testProperties() throws InvalidParameterException {
		identity.setProperty("foo", "bar");
		assertEquals("bar", identity.getProperty("foo"));
		identity.removeProperty("foo");
		
		try {
			identity.getProperty("foo");
			fail();
		} catch (InvalidParameterException e) {
			
		}
	}
	
	/**
	 * @author Julien Cornuwel (batosai@freenetproject.org)
	 */
	public void testPersistence() throws MalformedURLException, UnknownIdentityException {
		flushCaches();
		
		assertEquals(1, mWoT.getAllIdentities().size());
		
		Identity stored = mWoT.getIdentityByURI(requestUriString);
		assertSame(identity, stored);
		
		identity.checkedActivate(10);
		
		stored = null;
		mWoT.terminate();
		assertTrue(mWoT.isTerminated());
		mWoT = null;
		
		flushCaches();
		
		mWoT = new WebOfTrust(getDatabaseFilename());
		
		identity.initializeTransient(mWoT);  // Prevent DatabaseClosedException in .equals()
		
		assertEquals(1, mWoT.getAllIdentities().size());	
		
		stored = mWoT.getIdentityByURI(requestUriString);
		assertNotSame(identity, stored);
		assertEquals(identity, stored);
		assertEquals(identity.getAddedDate(), stored.getAddedDate());
		assertEquals(identity.getLastChangeDate(), stored.getLastChangeDate());
	}
	
	public void testValidateNickname() {
		try {
            // '@' needs to be disallowed because we use it in Identity.getShortestUniqueNickname()
            // to separate the nickname from the the Identity public key hash.
            // If it was allowed, people could spoof nicknames by suffixing them with
            // '@attack-target-hash'
			Identity.validateNickname("a@b");
			fail();
		} catch(InvalidParameterException e) {}
		
		// TODO: Implement a full test.
	}

	public final void testGetID() {
		assertEquals(Base64.encode(identity.getRequestURI().getRoutingKey()), identity.getID());
	}

	// TODO: Move to a seperate test class for IdentityID
	public final void testGetIDFromURI() throws MalformedURLException {
		assertEquals(Base64.encode(requestUri.getRoutingKey()), IdentityID.constructAndValidateFromURI(new FreenetURI(requestUriString)).toString());
		assertEquals(Base64.encode(requestUri.getRoutingKey()), IdentityID.constructAndValidateFromURI(new FreenetURI(insertUriString)).toString());
	}

	public final void testGetRequestURI() throws InvalidParameterException, MalformedURLException {
		// We generate a new identity because we want to make sure that the edition of the URI is 0 - the identity constructor will set it to 0 otherwise
		// We need the edition not to change so the assertNotSame test makes sense.
		
		final FreenetURI uriWithProperEdition = new FreenetURI("USK@R3Lp2s4jdX-3Q96c0A9530qg7JsvA9vi2K0hwY9wG-4,ipkgYftRpo0StBlYkJUawZhg~SO29NZIINseUtBhEfE,AQACAAE/WebOfTrust/0");
		final Identity identity = new Identity(mWoT, uriWithProperEdition, "test", true);
		
		assertEquals(uriWithProperEdition, identity.getRequestURI());
		
		// Constructors should clone all objects which they receive to ensure that deletion of one object does not destruct the database integrity of another.
		assertNotSame(uriWithProperEdition, identity.getRequestURI());
	}
	
	public final void testGetRawEdition() throws InvalidParameterException {
		assertEquals(23, requestUri.getSuggestedEdition());
		// The edition which is passed in during construction of the identity MUST NOT be stored as the current edition
		// - it should be stored as an edition hint as we cannot be sure whether the edition really exists because
		// identity URIs are usually obtained from not trustworthy sources.
		assertEquals(0, identity.getRawEdition());
		identity.onFetchedAndParsedSuccessfully(10);
		assertEquals(10, identity.getRawEdition());
	}
	
	public final void testGetCurrentEditionFetchState() {
		assertEquals(-1, identity.getLastFetchedEdition());
		assertEquals(-1, identity.getLastFetchedMaybeValidEdition());
		assertEquals(FetchState.NotFetched, identity.getCurrentEditionFetchState());
		assertEquals(0, identity.getNextEditionToFetch());
		
		identity.onFetchedAndParsedSuccessfully(identity.getNextEditionToFetch());
		assertEquals(0, identity.getLastFetchedEdition());
		assertEquals(0, identity.getLastFetchedMaybeValidEdition());
		assertEquals(FetchState.Fetched, identity.getCurrentEditionFetchState());
		assertEquals(1, identity.getNextEditionToFetch());
		
		identity.onFetchedAndParsingFailed(identity.getNextEditionToFetch());
		assertEquals(1, identity.getLastFetchedEdition());
		assertEquals(0, identity.getLastFetchedMaybeValidEdition());
		assertEquals(FetchState.ParsingFailed, identity.getCurrentEditionFetchState());
		assertEquals(2, identity.getNextEditionToFetch());
		
		identity.onFetchedAndParsedSuccessfully(identity.getNextEditionToFetch());
		assertEquals(2, identity.getLastFetchedEdition());
		assertEquals(2, identity.getLastFetchedMaybeValidEdition());
		assertEquals(FetchState.Fetched, identity.getCurrentEditionFetchState());
		assertEquals(3, identity.getNextEditionToFetch());
		identity.markForRefetch();
		assertEquals(1, identity.getLastFetchedEdition());
		assertEquals(1, identity.getLastFetchedMaybeValidEdition());
		assertEquals(FetchState.NotFetched, identity.getCurrentEditionFetchState());
		assertEquals(2, identity.getNextEditionToFetch());
		identity.onFetchedAndParsedSuccessfully(identity.getNextEditionToFetch());
		assertEquals(2, identity.getLastFetchedEdition());
		assertEquals(2, identity.getLastFetchedMaybeValidEdition());
		assertEquals(FetchState.Fetched, identity.getCurrentEditionFetchState());
		assertEquals(3, identity.getNextEditionToFetch());
	}

	/**
	 * TODO: Code quality: This used to test the function Identity.setEdition() which has been
	 * removed in favor of the rewrite {@link Identity#onFetchedAndParsedSuccessfully(long)}.
	 * The test was changed to use the new function without reconsidering what it tests.
	 * Review the replacement function and adapt the test to be more suitable for it. */
	public final void testOnFetchedAndParsedSuccessfullyLong()
			throws InvalidParameterException, InterruptedException {
		
		// Test preconditions
		identity.onFetchedAndParsedSuccessfully(0);
		assertEquals(0, identity.getLastFetchedEdition());
		assertEquals(0, identity.getRequestURI().getEdition());
		assertEquals(23, requestUri.getEdition());
		assertEquals(requestUri.getEdition(), identity.getLatestEditionHint());
		@Ignore
		class OldLastChangedDate {
			Date self;
			public OldLastChangedDate() throws InterruptedException {
				update();
			}
			public void update() throws InterruptedException {
				self = identity.getLastChangeDate();
				waitUntilCurrentTimeUTCIsAfter(self);
			}
			@Override
			public boolean equals(Object other) {
				return self.equals(other);
			}
		};
		OldLastChangedDate oldLastChangedDate = new OldLastChangedDate();
		
		// Test fetching of a new edition while edition hint stays valid
		identity.onFetchedAndParsedSuccessfully(10);
		assertEquals(10, identity.getLastFetchedEdition());
		assertEquals(10, identity.getRequestURI().getEdition());
		assertEquals(23, identity.getLatestEditionHint());
		assertEquals(FetchState.Fetched, identity.getCurrentEditionFetchState());
		assertFalse(oldLastChangedDate.equals(identity.getLastChangeDate()));
		// Test done.
		
		// Test fetching of a new edition which invalidates the edition hint
		oldLastChangedDate.update();
		identity.onFetchedAndParsedSuccessfully(24);
		assertEquals(24, identity.getLastFetchedEdition());
		assertEquals(24, identity.getLatestEditionHint());
		assertFalse(oldLastChangedDate.equals(identity.getLastChangeDate()));
		// Test done.
		
		// Test whether setting a new hint does NOT mark the identity as changed:
		// We receive hints from other identities, not the identity itself. Other identities should not be allowed to mark someone as changed.
		oldLastChangedDate.update();
		identity.setNewEditionHint(50);
		assertTrue(oldLastChangedDate.equals(identity.getLastChangeDate()));
		// Test done.
		
		// Test whether decreasing of edition is correctly disallowed
		oldLastChangedDate.update();
		try {
			identity.onFetchedAndParsedSuccessfully(24);
			fail("Decreasing/refetching the edition should not be allowed");
		} catch(IllegalStateException e) {
			assertEquals(FetchState.Fetched, identity.getCurrentEditionFetchState());
			assertEquals(24, identity.getLastFetchedEdition());
			assertEquals(24, identity.getRequestURI().getEdition());
			assertEquals(50, identity.getLatestEditionHint());
			assertTrue(oldLastChangedDate.equals(identity.getLastChangeDate()));
		}
		// Test done.
	}
	

// TODO: Finish implementation and enable.	
//	public void testEquals() {
//		do {
//			Thread.sleep(1);
//		} while(identity.getAddedDate().equals(CurrentTimeUTC.get()));
//		
//		assertEquals(identity, identity);
//		assertEquals(identity, identity.clone());
//		
//		
//	
//		
//		Object[] inequalObjects = new Object[] {
//			new Object(),
//			new Identity(uriB, identity.getNickname(), identity.doesPublishTrustList());
//		};
//		
//		for(Object other : inequalObjects)
//			assertFalse(score.equals(other));
//	}
//	
//	public void testClone() {
//		do {
//		    Thread.sleep(1);
//		} while(identity.getAddedDate().equals(CurrentTimeUTC.get()));
//		
//		Identity clone = identity.clone();
//		assertNotSame(clone, identity);
//		assertEquals(identity.getEdition(), clone.getEdition());
//		assertEquals(identity.getID(), clone.getID());
//		assertEquals(identity.getLatestEditionHint(), clone.getLatestEditionHint());
//		assertNotSame(identity.getNickname(), clone.getNickname());
//		assertEquals(identity.getNickname(), clone.getNickname());
//		assertEquals(identity.getProperties())
//	}
}
