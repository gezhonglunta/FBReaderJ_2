/*
 * Copyright (C) 2010-2012 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.fbreader.network.authentication.litres;

import java.util.*;

import org.geometerplus.zlibrary.core.options.ZLBooleanOption;
import org.geometerplus.zlibrary.core.options.ZLStringOption;
import org.geometerplus.zlibrary.core.util.ZLNetworkUtil;
import org.geometerplus.zlibrary.core.network.ZLNetworkManager;
import org.geometerplus.zlibrary.core.network.ZLNetworkException;
import org.geometerplus.zlibrary.core.network.ZLNetworkRequest;
import org.geometerplus.zlibrary.core.money.Money;

import org.geometerplus.fbreader.network.*;
import org.geometerplus.fbreader.network.opds.OPDSNetworkLink;
import org.geometerplus.fbreader.network.authentication.*;
import org.geometerplus.fbreader.network.urlInfo.*;

public class LitResAuthenticationManager extends NetworkAuthenticationManager {
	private volatile boolean myFullyInitialized;

	private final ZLStringOption mySidOption;
	private final ZLStringOption myUserIdOption;
	private final ZLBooleanOption myCanRebillOption;

	private volatile String myInitializedDataSid;
	private volatile Money myAccount;
	private final BookCollection myPurchasedBooks = new BookCollection();

	private final class BookCollection {
		private final Map<String,NetworkBookItem> myMap = new HashMap<String,NetworkBookItem>();
		private final List<NetworkBookItem> myList = new LinkedList<NetworkBookItem>();

		public void clear() {
			myMap.clear();
			myList.clear();
		}

		public boolean isEmpty() {
			return myList.isEmpty();
		}

		public void addToStart(NetworkBookItem book) {
			myMap.put(book.Id, book);
			myList.add(0, book);
		}

		public void addToEnd(NetworkBookItem book) {
			myMap.put(book.Id, book);
			myList.add(book);
		}

		public boolean contains(NetworkBookItem book) {
			return myMap.containsKey(book.Id);
		}

		public List<NetworkBookItem> list() {
			return Collections.unmodifiableList(myList);
		}
	}

	public LitResAuthenticationManager(OPDSNetworkLink link) {
		super(link);

		mySidOption = new ZLStringOption(link.getSiteName(), "sid", "");
		myUserIdOption = new ZLStringOption(link.getSiteName(), "userId", "");
		myCanRebillOption = new ZLBooleanOption(link.getSiteName(), "canRebill", false);
	}

	public synchronized boolean initUser(String username, String sid, String userId, boolean canRebill) {
		boolean changed = false;
		if (username == null) {
			username = UserNameOption.getValue();
		} else if (!username.equals(UserNameOption.getValue())) {
			changed = true;
			UserNameOption.setValue(username);
		}
		changed |= !sid.equals(mySidOption.getValue());
		mySidOption.setValue(sid);
		changed |= !userId.equals(myUserIdOption.getValue());
		myUserIdOption.setValue(userId);
		changed |= canRebill != myCanRebillOption.getValue();
		myCanRebillOption.setValue(canRebill);
		final boolean fullyInitialized =
			!"".equals(username) && !"".equals(sid) && !"".equals(userId);
		if (fullyInitialized != myFullyInitialized) {
			changed = true;
			myFullyInitialized = fullyInitialized;
		}
		return changed;
	}

	@Override
	public void logOut() {
		logOut(true);
	}

	private synchronized void logOut(boolean full) {
		boolean changed = false;
		if (full) {
			changed = initUser(null, "", "", false);
		} else {
			changed = myFullyInitialized;
			myFullyInitialized = false;
		}
		changed |= myInitializedDataSid != null;
		myInitializedDataSid = null;
		changed |= !myPurchasedBooks.isEmpty();
		myPurchasedBooks.clear();
		if (changed) {
			NetworkLibrary.Instance().fireModelChangedEvent(NetworkLibrary.ChangeListener.Code.SignedIn);
		}
	}

	@Override
	public boolean isAuthorised(boolean useNetwork) throws ZLNetworkException {
		final String sid;
		synchronized (this) {
			boolean authState =
				UserNameOption.getValue().length() != 0 &&
				mySidOption.getValue().length() != 0;

			if (myFullyInitialized || !useNetwork) {
				return authState;
			}

			if (!authState) {
				logOut(false);
				return false;
			}
			sid = mySidOption.getValue();
		}

		try {
			final LitResLoginXMLReader xmlReader = new LitResLoginXMLReader(Link.getSiteName());
			final Map<String,String> params = new HashMap<String,String>();
			final String url = parseUrl(Link.getUrl(UrlInfo.Type.SignIn), params);
			if (url == null) {
				throw new ZLNetworkException(NetworkException.ERROR_UNSUPPORTED_OPERATION);
			}
			final LitResNetworkRequest request = new LitResNetworkRequest(url, xmlReader);
			for (Map.Entry<String,String> entry : params.entrySet()) {
				request.addPostParameter(entry.getKey(), entry.getValue());
			}
			request.addPostParameter("sid", sid);
			ZLNetworkManager.Instance().perform(request);
			initUser(null, xmlReader.Sid, xmlReader.UserId, xmlReader.CanRebill);
			return true;
		} catch (ZLNetworkException e) {
			if (NetworkException.ERROR_AUTHENTICATION_FAILED.equals(e.getCode())) {
				throw e;
			}
			logOut(false);
			return false;
		}
	}

	@Override
	public void authorise(String username, String password) throws ZLNetworkException {
		final Map<String,String> params = new HashMap<String,String>();
		final String url = parseUrl(Link.getUrl(UrlInfo.Type.SignIn), params);
		if (url == null) {
			throw new ZLNetworkException(NetworkException.ERROR_UNSUPPORTED_OPERATION);
		}
		synchronized (this) {
			UserNameOption.setValue(username);
		}

		try {
			final LitResLoginXMLReader xmlReader = new LitResLoginXMLReader(Link.getSiteName());
			final LitResNetworkRequest request = new LitResNetworkRequest(url, xmlReader);
			for (Map.Entry<String,String> entry : params.entrySet()) {
				request.addPostParameter(entry.getKey(), entry.getValue());
			}
			request.addPostParameter("login", username);
			request.addPostParameter("pwd", password);
			ZLNetworkManager.Instance().perform(request);
			NetworkLibrary.Instance().fireModelChangedEvent(NetworkLibrary.ChangeListener.Code.SignedIn);
			initUser(null, xmlReader.Sid, xmlReader.UserId, xmlReader.CanRebill);
		} catch (ZLNetworkException e) {
			logOut(false);
			throw e;
		}
	}

	@Override
	public BookUrlInfo downloadReference(NetworkBookItem book) {
		final String sid;
		synchronized (this) {
			sid = mySidOption.getValue();
		}
		if (sid.length() == 0) {
			return null;
		}
		BookUrlInfo reference = book.reference(UrlInfo.Type.BookConditional);
		if (reference == null) {
			return null;
		}
		final String url = ZLNetworkUtil.appendParameter(reference.Url, "sid", sid);
		return new DecoratedBookUrlInfo(reference, url);
	}

	@Override
	public synchronized boolean needPurchase(NetworkBookItem book) {
		return !myPurchasedBooks.contains(book);
	}

	@Override
	public void purchaseBook(NetworkBookItem book) throws ZLNetworkException {
		final String sid;
		synchronized (this) {
			sid = mySidOption.getValue();
		}
		if (sid.length() == 0) {
			throw new ZLNetworkException(NetworkException.ERROR_AUTHENTICATION_FAILED);
		}

		final BookUrlInfo reference = book.reference(UrlInfo.Type.BookBuy);
		if (reference == null) {
			throw new ZLNetworkException(NetworkException.ERROR_BOOK_NOT_PURCHASED); // TODO: more correct error message???
		}

		final LitResPurchaseXMLReader xmlReader = new LitResPurchaseXMLReader(Link.getSiteName());

		ZLNetworkException exception = null;
		try {
			final LitResNetworkRequest request = new LitResNetworkRequest(reference.Url, xmlReader);
			request.addPostParameter("sid", sid);
			ZLNetworkManager.Instance().perform(request);
		} catch (ZLNetworkException e) {
			exception = e;
		}

		synchronized (this) {
			if (xmlReader.Account != null) {
				myAccount = new Money(xmlReader.Account, "RUB");
			}
			if (exception != null) {
				final String code = exception.getCode();
				if (NetworkException.ERROR_AUTHENTICATION_FAILED.equals(code)) {
					logOut(false);
				} else if (NetworkException.ERROR_PURCHASE_ALREADY_PURCHASED.equals(code)) {
					myPurchasedBooks.addToStart(book);
				}
				throw exception;
			}
			if (xmlReader.BookId == null || !xmlReader.BookId.equals(book.Id)) {
				throw new ZLNetworkException(NetworkException.ERROR_SOMETHING_WRONG, Link.getSiteName());
			}
			myPurchasedBooks.addToStart(book);
			final BasketItem basket = book.Link.getBasketItem();
			if (basket != null) {
				basket.remove(book);
			}
		}
	}

	@Override
	public String topupLink(Money sum) {
		final String sid;
		synchronized (this) {
			sid = mySidOption.getValue();
		}
		if (sid.length() == 0) {
			return null;
		}
		String url = Link.getUrl(UrlInfo.Type.TopUp);
		if (url == null) {
			return null;
		}
		url = ZLNetworkUtil.appendParameter(url, "sid", sid);
		if (sum != null) {
			url = ZLNetworkUtil.appendParameter(url, "summ", String.valueOf(sum.Amount));
		}
		return url;
	}

	@Override
	public synchronized Money currentAccount() {
		return myAccount;
	}

	void reloadPurchasedBooks() throws ZLNetworkException {
		final LitResNetworkRequest networkRequest;
		synchronized (this) {
			final String sid = mySidOption.getValue();
			if (sid.length() == 0) {
				throw new ZLNetworkException(NetworkException.ERROR_AUTHENTICATION_FAILED);
			}
			if (!sid.equals(myInitializedDataSid)) {
				logOut(false);
				throw new ZLNetworkException(NetworkException.ERROR_AUTHENTICATION_FAILED);
			}
			networkRequest = loadPurchasedBooksRequest(sid);
		}

		ZLNetworkException exception = null;
		try {
			ZLNetworkManager.Instance().perform(networkRequest);
		} catch (ZLNetworkException e) {
			exception = e;
		}

		synchronized (this) {
			if (exception != null) {
				if (NetworkException.ERROR_AUTHENTICATION_FAILED.equals(exception.getCode())) {
					logOut(false);
				}
				throw exception;
			}
			loadPurchasedBooksOnSuccess(networkRequest);
		}
	}

	@Override
	public synchronized List<NetworkBookItem> purchasedBooks() {
		return myPurchasedBooks.list();
	}

	@Override
	public synchronized boolean needsInitialization() {
		final String sid = mySidOption.getValue();
		if (sid.length() == 0) {
			return false;
		}
		return !sid.equals(myInitializedDataSid);
	}

	@Override
	public void initialize() throws ZLNetworkException {
		final String sid;
		final LitResNetworkRequest purchasedBooksRequest;
		final LitResNetworkRequest accountRequest;
		synchronized (this) {
			sid = mySidOption.getValue();
			if (sid.length() == 0) {
				throw new ZLNetworkException(NetworkException.ERROR_AUTHENTICATION_FAILED);
			}
			if (sid.equals(myInitializedDataSid) || !isAuthorised(true)) {
				return;
			}

			purchasedBooksRequest = loadPurchasedBooksRequest(sid);
			accountRequest = loadAccountRequest(sid);
		}

		final LinkedList<ZLNetworkRequest> requests = new LinkedList<ZLNetworkRequest>();
		requests.add(purchasedBooksRequest);
		requests.add(accountRequest);

		try {
			ZLNetworkManager.Instance().perform(requests);
			synchronized (this) {
				myInitializedDataSid = sid;
				loadPurchasedBooksOnSuccess(purchasedBooksRequest);
				myAccount = new Money(((LitResPurchaseXMLReader)accountRequest.Reader).Account, "RUB");
			}
		} catch (ZLNetworkException e) {
			synchronized (this) {
				myInitializedDataSid = null;
				loadPurchasedBooksOnError();
				myAccount = null;
			}
			throw e;
		}
	}

	@Override
	public void refreshAccountInformation() throws ZLNetworkException {
		final LitResNetworkRequest accountRequest = loadAccountRequest(mySidOption.getValue());
		ZLNetworkManager.Instance().perform(accountRequest);
		synchronized (this) {
			myAccount = new Money(((LitResPurchaseXMLReader)accountRequest.Reader).Account, "RUB");
		}
	}

	private LitResNetworkRequest loadPurchasedBooksRequest(String sid) {
		final String query = "pages/catalit_browser/";

		final LitResNetworkRequest request = new LitResNetworkRequest(
			LitResUtil.url(Link, query),
			new LitResXMLReader((OPDSNetworkLink)Link)
		);
		request.addPostParameter("my", "1");
		request.addPostParameter("sid", sid);
		return request;
	}

	private void loadPurchasedBooksOnError() {
		myPurchasedBooks.clear();
	}

	private void loadPurchasedBooksOnSuccess(LitResNetworkRequest purchasedBooksRequest) {
		LitResXMLReader reader = (LitResXMLReader)purchasedBooksRequest.Reader;
		myPurchasedBooks.clear();
		for (NetworkBookItem book : reader.Books) {
			myPurchasedBooks.addToEnd(book);
		}
	}

	private LitResNetworkRequest loadAccountRequest(String sid) {
		final String query = "pages/purchase_book/";

		final LitResNetworkRequest request = new LitResNetworkRequest(
			LitResUtil.url(Link, query),
			new LitResPurchaseXMLReader(Link.getSiteName())
		);
		request.addPostParameter("sid", sid);
		request.addPostParameter("art", "0");
		return request;
	}

	@Override
	public boolean passwordRecoverySupported() {
		return true;
	}

	@Override
	public void recoverPassword(String email) throws ZLNetworkException {
		final String url = Link.getUrl(UrlInfo.Type.RecoverPassword);
		if (url == null) {
			throw new ZLNetworkException(NetworkException.ERROR_UNSUPPORTED_OPERATION);
		}
		final LitResPasswordRecoveryXMLReader xmlReader = new LitResPasswordRecoveryXMLReader(Link.getSiteName());
		final LitResNetworkRequest request = new LitResNetworkRequest(url, xmlReader);
		request.addPostParameter("mail", email);
		ZLNetworkManager.Instance().perform(request);
	}

	@Override
	public Map<String,String> getTopupData() {
		final HashMap<String,String> map = new HashMap<String,String>();
		map.put("litres:userId", myUserIdOption.getValue());
		map.put("litres:canRebill", myCanRebillOption.getValue() ? "true" : "false");
		map.put("litres:sid", mySidOption.getValue());
		return map;
	}

	private static String parseUrl(String url, Map<String,String> params) {
		if (url == null) {
			return null;
		}
		final String[] parts = url.split("\\?");
		if (parts.length != 2) {
			return url;
		}
		for (String s : parts[1].split("&")) {
			final String[] pair = s.split("=");
			if (pair.length == 2) {
				params.put(pair[0], pair[1]);
			}
		}
		return parts[0];
	}
}
