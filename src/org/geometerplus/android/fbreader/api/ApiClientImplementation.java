/*
 * This code is in the public domain.
 */

package org.geometerplus.android.fbreader.api;

import java.util.*;

import android.content.*;
import android.os.IBinder;

public class ApiClientImplementation implements ServiceConnection, Api, ApiMethods {
	public static interface ConnectionListener {
		void onConnected();
	}

	private static final String ACTION_API = "android.fbreader.action.API";
	static final String ACTION_API_CALLBACK = "android.fbreader.action.API_CALLBACK";
	static final String EVENT_TYPE = "event.type";

	private final Context myContext;
	private ConnectionListener myListener;
	private volatile ApiInterface myInterface;

	private final List<ApiListener> myApiListeners =
		Collections.synchronizedList(new LinkedList<ApiListener>());

	private final BroadcastReceiver myEventReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (myInterface == null || myApiListeners.size() == 0) {
				return;
			}
			final int code = intent.getIntExtra(EVENT_TYPE, -1);
			if (code != -1) {
				synchronized (myApiListeners) {
					for (ApiListener l : myApiListeners) {
						l.onEvent(code);
					}
				}
			}
		}
	};

	public ApiClientImplementation(Context context, ConnectionListener listener) {
		myContext = context;
		myListener = listener;
		connect();
	}

	public synchronized void connect() {
		if (myInterface == null) {
			myContext.bindService(new Intent(ACTION_API), this, Context.BIND_AUTO_CREATE);
			myContext.registerReceiver(myEventReceiver, new IntentFilter(ACTION_API_CALLBACK));
		}
	}

	public synchronized void disconnect() {
		if (myInterface != null) {
			myContext.unregisterReceiver(myEventReceiver);
			try {
				myContext.unbindService(this);
			} catch (IllegalArgumentException e) {
			}
			myInterface = null;
		}
	}

	public void addListener(ApiListener listener) {
		myApiListeners.add(listener);
	}

	public void removeListener(ApiListener listener) {
		myApiListeners.remove(listener);
	}

	public synchronized void onServiceConnected(ComponentName className, IBinder service) {
		myInterface = ApiInterface.Stub.asInterface(service);
		if (myListener != null) {
			myListener.onConnected();
		}
	}

	public synchronized void onServiceDisconnected(ComponentName name) {
		myInterface = null;
	}

	private synchronized void checkConnection() throws ApiException {
		if (myInterface == null) {
			throw new ApiException("Not connected to FBReader");
		}
	}

	private synchronized ApiObject request(int method, ApiObject[] params) throws ApiException {
		checkConnection();
		try {
			final ApiObject object = myInterface.request(method, params);
			if (object instanceof ApiObject.Error) {
				throw new ApiException(((ApiObject.Error)object).Message);
			}
			return object;
		} catch (android.os.RemoteException e) {
			throw new ApiException(e);
		}
	}

	private synchronized List<ApiObject> requestList(int method, ApiObject[] params) throws ApiException {
		checkConnection();
		try {
			final List<ApiObject> list = myInterface.requestList(method, params);
			for (ApiObject object : list) {
				if (object instanceof ApiObject.Error) {
					throw new ApiException(((ApiObject.Error)object).Message);
				}
			}
			return list;
		} catch (android.os.RemoteException e) {
			throw new ApiException(e);
		}
	}

	private String requestString(int method, ApiObject[] params) throws ApiException {
		final ApiObject object = request(method, params);
		if (!(object instanceof ApiObject.String)) {
			throw new ApiException("Cannot cast return type of method " + method + " to String");
		}
		return ((ApiObject.String)object).Value;
	}

	private Date requestDate(int method, ApiObject[] params) throws ApiException {
		final ApiObject object = request(method, params);
		if (!(object instanceof ApiObject.Date)) {
			throw new ApiException("Cannot cast return type of method " + method + " to Date");
		}
		return ((ApiObject.Date)object).Value;
	}

	private int requestInt(int method, ApiObject[] params) throws ApiException {
		final ApiObject object = request(method, params);
		if (!(object instanceof ApiObject.Integer)) {
			throw new ApiException("Cannot cast return type of method " + method + " to int");
		}
		return ((ApiObject.Integer)object).Value;
	}

	private boolean requestBoolean(int method, ApiObject[] params) throws ApiException {
		final ApiObject object = request(method, params);
		if (!(object instanceof ApiObject.Boolean)) {
			throw new ApiException("Cannot cast return type of method " + method + " to boolean");
		}
		return ((ApiObject.Boolean)object).Value;
	}

	private TextPosition requestTextPosition(int method, ApiObject[] params) throws ApiException {
		final ApiObject object = request(method, params);
		if (!(object instanceof TextPosition)) {
			throw new ApiException("Cannot cast return type of method " + method + " to TextPosition");
		}
		return (TextPosition)object;
	}

	private List<String> requestStringList(int method, ApiObject[] params) throws ApiException {
		final List<ApiObject> list = requestList(method, params);
		final ArrayList<String> stringList = new ArrayList<String>(list.size());
		for (ApiObject object : list) {
			if (!(object instanceof ApiObject.String)) {
				throw new ApiException("Cannot cast an element returned from method " + method + " to String");
			}
			stringList.add(((ApiObject.String)object).Value);
		}
		return stringList;
	}

	private static final ApiObject[] EMPTY_PARAMETERS = new ApiObject[0];

	private static ApiObject[] envelope(String value) {
		return new ApiObject[] { ApiObject.envelope(value) };
	}

	private static ApiObject[] envelope(int value) {
		return new ApiObject[] { ApiObject.envelope(value) };
	}

	private static ApiObject[] envelope(long value) {
		return new ApiObject[] { ApiObject.envelope(value) };
	}

	private static ApiObject[] envelope(List<String> value) {
		final ApiObject[] objects = new ApiObject[value.size()];
		int index = 0;
		for (String s : value) {
		    objects[index++] = ApiObject.envelope(s);
		}
		return objects;
	}

	// information about fbreader
	public String getFBReaderVersion() throws ApiException {
		return requestString(GET_FBREADER_VERSION, EMPTY_PARAMETERS);
	}

	// preferences information
	public List<String> getOptionGroups() throws ApiException {
		return requestStringList(LIST_OPTION_GROUPS, EMPTY_PARAMETERS);
	}

	public List<String> getOptionNames(String group) throws ApiException {
		return requestStringList(LIST_OPTION_NAMES, envelope(group));
	}

	public String getOptionValue(String group, String name) throws ApiException {
		return requestString(
			GET_OPTION_VALUE,
			new ApiObject[] { ApiObject.envelope(group), ApiObject.envelope(name) }
		);
	}

	public void setOptionValue(String group, String name, String value) throws ApiException {
		request(
			SET_OPTION_VALUE,
			new ApiObject[] { ApiObject.envelope(group), ApiObject.envelope(name), ApiObject.envelope(value) }
		);
	}

	public String getBookLanguage() throws ApiException {
		return requestString(GET_BOOK_LANGUAGE, EMPTY_PARAMETERS);
	}

	public String getBookTitle() throws ApiException {
		return requestString(GET_BOOK_TITLE, EMPTY_PARAMETERS);
	}

	public List<String> getBookTags() throws ApiException {
		return requestStringList(LIST_BOOK_TAGS, EMPTY_PARAMETERS);
	}

	public String getBookFilePath() throws ApiException {
		return requestString(GET_BOOK_FILE_PATH, EMPTY_PARAMETERS);
	}

	public String getBookHash() throws ApiException {
		return requestString(GET_BOOK_HASH, EMPTY_PARAMETERS);
	}

	public String getBookUniqueId() throws ApiException {
		return requestString(GET_BOOK_UNIQUE_ID, EMPTY_PARAMETERS);
	}

	public Date getBookLastTurningTime() throws ApiException {
		return requestDate(GET_BOOK_LAST_TURNING_TIME, EMPTY_PARAMETERS);
	}

	public String getBookLanguage(long id) throws ApiException {
		return requestString(GET_BOOK_LANGUAGE, envelope(id));
	}

	public String getBookTitle(long id) throws ApiException {
		return requestString(GET_BOOK_TITLE, envelope(id));
	}

	public List<String> getBookTags(long id) throws ApiException {
		return requestStringList(LIST_BOOK_TAGS, envelope(id));
	}

	public String getBookFilePath(long id) throws ApiException {
		return requestString(GET_BOOK_FILE_PATH, envelope(id));
	}

	public String getBookHash(long id) throws ApiException {
		return requestString(GET_BOOK_HASH, envelope(id));
	}

	public String getBookUniqueId(long id) throws ApiException {
		return requestString(GET_BOOK_UNIQUE_ID, envelope(id));
	}

	public Date getBookLastTurningTime(long id) throws ApiException {
		return requestDate(GET_BOOK_LAST_TURNING_TIME, envelope(id));
	}

	public TextPosition getPageStart() throws ApiException {
		return requestTextPosition(GET_PAGE_START, EMPTY_PARAMETERS);
	}

	public TextPosition getPageEnd() throws ApiException {
		return requestTextPosition(GET_PAGE_END, EMPTY_PARAMETERS);
	}

	public boolean isPageEndOfSection() throws ApiException {
		return requestBoolean(IS_PAGE_END_OF_SECTION, EMPTY_PARAMETERS);
	}

	public boolean isPageEndOfText() throws ApiException {
		return requestBoolean(IS_PAGE_END_OF_TEXT, EMPTY_PARAMETERS);
	}

	public int getParagraphsNumber() throws ApiException {
		return requestInt(GET_PARAGRAPHS_NUMBER, EMPTY_PARAMETERS);
	}

	public String getParagraphText(int paragraphIndex) throws ApiException {
		return requestString(GET_PARAGRAPH_TEXT, envelope(paragraphIndex));
	}

	public int getElementsNumber(int paragraphIndex) throws ApiException {
		return requestInt(GET_ELEMENTS_NUMBER, envelope(paragraphIndex));
	}

	public void setPageStart(TextPosition position) throws ApiException {
		request(SET_PAGE_START, new ApiObject[] { position });
	}

	public void highlightArea(TextPosition start, TextPosition end) throws ApiException {
		request(HIGHLIGHT_AREA, new ApiObject[] { start, end });
	}

	public void clearHighlighting() throws ApiException {
		request(CLEAR_HIGHLIGHTING, EMPTY_PARAMETERS);
	}

	// action control
	public String getKeyAction(int key, boolean longPress) throws ApiException {
		return requestString(GET_KEY_ACTION, new ApiObject[] {
			ApiObject.envelope(key),
			ApiObject.envelope(longPress)
		});
	}

	public void setKeyAction(int key, boolean longPress, String action) throws ApiException {
		request(SET_KEY_ACTION, new ApiObject[] {
			ApiObject.envelope(key),
			ApiObject.envelope(longPress),
			ApiObject.envelope(action)
		});
	}

	public List<String> listActions() throws ApiException {
	  	return requestStringList(LIST_ACTIONS, EMPTY_PARAMETERS);
	}

	public List<String> listActionNames(List<String> actions) throws ApiException {
	  	return requestStringList(LIST_ACTION_NAMES, envelope(actions));
	}

	public List<String> listZoneMaps() throws ApiException {
	  	return requestStringList(LIST_ZONEMAPS, EMPTY_PARAMETERS);
	}

	public String getZoneMap() throws ApiException {
	  	return requestString(GET_ZONEMAP, EMPTY_PARAMETERS);
	}

	public void setZoneMap(String name) throws ApiException {
	  	request(SET_ZONEMAP, envelope(name));
	}

	public int getZoneMapHeight(String name) throws ApiException {
		return requestInt(GET_ZONEMAP_HEIGHT, envelope(name));
	}

	public int getZoneMapWidth(String name) throws ApiException {
		return requestInt(GET_ZONEMAP_WIDTH, envelope(name));
	}

	public void createZoneMap(String name, int width, int height) throws ApiException {
		request(CREATE_ZONEMAP, new ApiObject[] {
			ApiObject.envelope(name),
			ApiObject.envelope(width),
			ApiObject.envelope(height)
		});
	}

	public boolean isZoneMapCustom(String name) throws ApiException {
		return requestBoolean(IS_ZONEMAP_CUSTOM, envelope(name));
	}

	public void deleteZoneMap(String name) throws ApiException {
		request(DELETE_ZONEMAP, envelope(name));
	}

	public String getTapZoneAction(String name, int h, int v, boolean singleTap) throws ApiException {
		return requestString(GET_TAPZONE_ACTION, new ApiObject[] {
			ApiObject.envelope(name),
			ApiObject.envelope(h),
			ApiObject.envelope(v),
			ApiObject.envelope(singleTap)
		});
	}

	public void setTapZoneAction(String name, int h, int v, boolean singleTap, String action) throws ApiException {
		request(SET_TAPZONE_ACTION, new ApiObject[] {
			ApiObject.envelope(name),
			ApiObject.envelope(h),
			ApiObject.envelope(v),
			ApiObject.envelope(singleTap),
			ApiObject.envelope(action)
		});
	}
}
