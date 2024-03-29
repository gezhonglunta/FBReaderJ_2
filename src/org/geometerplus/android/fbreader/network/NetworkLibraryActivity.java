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

package org.geometerplus.android.fbreader.network;

import java.util.*;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import org.geometerplus.zlibrary.core.network.ZLNetworkManager;
import org.geometerplus.zlibrary.core.resources.ZLResource;
import org.geometerplus.zlibrary.core.util.ZLBoolean3;

import org.geometerplus.zlibrary.ui.android.network.SQLiteCookieDatabase;

import org.geometerplus.fbreader.tree.FBTree;
import org.geometerplus.fbreader.network.*;
import org.geometerplus.fbreader.network.tree.*;

import org.geometerplus.android.fbreader.tree.TreeActivity;
import org.geometerplus.android.fbreader.network.action.*;

import org.geometerplus.android.util.UIUtil;

public abstract class NetworkLibraryActivity extends TreeActivity implements ListView.OnScrollListener, NetworkLibrary.ChangeListener {
	static final String OPEN_CATALOG_ACTION = "android.fbreader.action.OPEN_NETWORK_CATALOG";

	BookDownloaderServiceConnection Connection;

	final List<Action> myOptionsMenuActions = new ArrayList<Action>();
	final List<Action> myContextMenuActions = new ArrayList<Action>();
	final List<Action> myListClickActions = new ArrayList<Action>();
	private Intent myDeferredIntent;
	private boolean mySingleCatalog;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		AuthenticationActivity.initCredentialsCreator(this);

		SQLiteCookieDatabase.init(this);

		Connection = new BookDownloaderServiceConnection();
		bindService(
			new Intent(getApplicationContext(), BookDownloaderService.class),
			Connection,
			BIND_AUTO_CREATE
		);

		setListAdapter(new NetworkLibraryAdapter(this));
		final Intent intent = getIntent();
		init(intent);
		myDeferredIntent = null;

		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

		if (getCurrentTree() instanceof RootTree) {
			mySingleCatalog = intent.getBooleanExtra("SingleCatalog", false);
			if (!NetworkLibrary.Instance().isInitialized()) {
				Util.initLibrary(this);
				myDeferredIntent = intent;
			} else {
				NetworkLibrary.Instance().fireModelChangedEvent(NetworkLibrary.ChangeListener.Code.SomeCode);
				openTreeByIntent(intent);
			}
		}

		getListView().setOnScrollListener(this);
	}

	@Override
	protected NetworkTree getTreeByKey(FBTree.Key key) {
		final NetworkLibrary library = NetworkLibrary.Instance();
		final NetworkTree tree = library.getTreeByKey(key);
		return tree != null ? tree : library.getRootTree();
	}

	@Override
	protected void onStart() {
		super.onStart();

		NetworkLibrary.Instance().addChangeListener(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		getListView().setOnCreateContextMenuListener(this);
		NetworkLibrary.Instance().fireModelChangedEvent(NetworkLibrary.ChangeListener.Code.SomeCode);
	}

	@Override
	protected void onStop() {
		NetworkLibrary.Instance().removeChangeListener(this);
		super.onStop();
	}

	@Override
	public void onDestroy() {
		if (Connection != null) {
			unbindService(Connection);
			Connection = null;
		}
		super.onDestroy();
	}

	private boolean openTreeByIntent(Intent intent) {
		if (OPEN_CATALOG_ACTION.equals(intent.getAction())) {
			final Uri uri = intent.getData();
			if (uri != null) {
				final NetworkTree tree =
					NetworkLibrary.Instance().getCatalogTreeByUrl(uri.toString());
				if (tree != null) {
					checkAndRun(new OpenCatalogAction(this), tree);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		if (!openTreeByIntent(intent)) {
			super.onNewIntent(intent);
		}
	}

	@Override
	public boolean onSearchRequested() {
		final NetworkTree tree = (NetworkTree)getCurrentTree();
		final RunSearchAction action = new RunSearchAction(this, false);
		if (action.isVisible(tree) && action.isEnabled(tree)) {
			action.run(tree);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean isTreeSelected(FBTree tree) {
		return false;
	}

	@Override
	protected boolean isTreeInvisible(FBTree tree) {
		return tree instanceof RootTree && (mySingleCatalog || ((RootTree)tree).IsFake);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			final NetworkItemsLoader loader =
				NetworkLibrary.Instance().getStoredLoader((NetworkTree)getCurrentTree());
			if (loader != null) {
				loader.interrupt();
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private void fillOptionsMenuList() {
		myOptionsMenuActions.add(new RunSearchAction(this, false));
		myOptionsMenuActions.add(new AddCustomCatalogAction(this));
		myOptionsMenuActions.add(new RefreshRootCatalogAction(this));
		myOptionsMenuActions.add(new LanguageFilterAction(this));
		myOptionsMenuActions.add(new ReloadCatalogAction(this));
		myOptionsMenuActions.add(new SignInAction(this));
		myOptionsMenuActions.add(new SignUpAction(this));
		myOptionsMenuActions.add(new SignOutAction(this));
		myOptionsMenuActions.add(new TopupAction(this));
		myOptionsMenuActions.add(new BuyBasketBooksAction(this));
		myOptionsMenuActions.add(new ClearBasketAction(this));
	}

	private void fillContextMenuList() {
		myContextMenuActions.add(new OpenCatalogAction(this));
		myContextMenuActions.add(new OpenInBrowserAction(this));
		myContextMenuActions.add(new RunSearchAction(this, true));
		myContextMenuActions.add(new AddCustomCatalogAction(this));
		myContextMenuActions.add(new SignOutAction(this));
		myContextMenuActions.add(new TopupAction(this));
		myContextMenuActions.add(new SignInAction(this));
		myContextMenuActions.add(new EditCustomCatalogAction(this));
		myContextMenuActions.add(new RemoveCustomCatalogAction(this));
		myContextMenuActions.add(new BuyBasketBooksAction(this));
		myContextMenuActions.add(new ClearBasketAction(this));
	}

	private void fillListClickList() {
		myListClickActions.add(new OpenCatalogAction(this));
		myListClickActions.add(new OpenInBrowserAction(this));
		myListClickActions.add(new RunSearchAction(this, true));
		myListClickActions.add(new AddCustomCatalogAction(this));
		myListClickActions.add(new TopupAction(this));
		myListClickActions.add(new ShowBookInfoAction(this));
	}

	private List<? extends Action> getContextMenuActions(NetworkTree tree) {
		return tree instanceof NetworkBookTree
			? NetworkBookActions.getContextMenuActions(this, (NetworkBookTree)tree, Connection)
			: myContextMenuActions;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
		if (myContextMenuActions.isEmpty()) {
			fillContextMenuList();
		}

		final int position = ((AdapterView.AdapterContextMenuInfo)menuInfo).position;
		final NetworkTree tree = (NetworkTree)getListAdapter().getItem(position);
		if (tree != null) {
			menu.setHeaderTitle(tree.getName());
			for (Action a : getContextMenuActions(tree)) {
				if (a.isVisible(tree) && a.isEnabled(tree)) {
					menu.add(0, a.Code, 0, a.getContextLabel(tree));
				}
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final int position = ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).position;
		final NetworkTree tree = (NetworkTree)getListAdapter().getItem(position);
		if (tree != null) {
			for (Action a : getContextMenuActions(tree)) {
				if (a.Code == item.getItemId()) {
					checkAndRun(a, tree);
					return true;
				}
			}
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position, long rowId) {
		if (myListClickActions.isEmpty()) {
			fillListClickList();
		}

		final NetworkTree tree = (NetworkTree)getListAdapter().getItem(position);
		for (Action a : myListClickActions) {
			if (a.isVisible(tree) && a.isEnabled(tree)) {
				checkAndRun(a, tree);
				return;
			}
		}

		listView.showContextMenuForChild(view);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		if (myOptionsMenuActions.isEmpty()) {
			fillOptionsMenuList();
		}

//		final NetworkTree tree = (NetworkTree)getCurrentTree();
		for (Action a : myOptionsMenuActions) {
			final MenuItem item = menu.add(0, a.Code, Menu.NONE, "");
			if (a.IconId != -1) {
				item.setIcon(a.IconId);
			}
		}
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		final NetworkTree tree = (NetworkTree)getCurrentTree();
		for (Action a : myOptionsMenuActions) {
			final MenuItem item = menu.findItem(a.Code);
			if (a.isVisible(tree)) {
				item.setVisible(true);
				item.setEnabled(a.isEnabled(tree));
				item.setTitle(a.getOptionsLabel(tree));
			} else {
				item.setVisible(false);
			}
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final NetworkTree tree = (NetworkTree)getCurrentTree();
		for (Action a : myOptionsMenuActions) {
			if (a.Code == item.getItemId()) {
				checkAndRun(a, tree);
				break;
			}
		}
		return true;
	}

	private void updateLoadingProgress() {
		final NetworkLibrary library = NetworkLibrary.Instance();
		final NetworkTree tree = (NetworkTree)getCurrentTree();
		final NetworkTree lTree = getLoadableNetworkTree(tree);
		final NetworkTree sTree = RunSearchAction.getSearchTree(tree);
		setProgressBarIndeterminateVisibility(
			library.isUpdateInProgress() ||
			library.isLoadingInProgress(lTree) ||
			library.isLoadingInProgress(sTree)
		);
	}

	// method from NetworkLibrary.ChangeListener
	public void onLibraryChanged(final NetworkLibrary.ChangeListener.Code code, final Object[] params) {
		runOnUiThread(new Runnable() {
			public void run() {
				switch (code) {
					default:
						updateLoadingProgress();
						getListAdapter().replaceAll(getCurrentTree().subTrees());
						//getListView().invalidateViews();
						break;
					case InitializationFailed:
						showInitLibraryDialog((String)params[0]);
						break;
					case InitializationFinished:
						NetworkLibrary.Instance().runBackgroundUpdate(false);
						if (myDeferredIntent != null) {
							openTreeByIntent(myDeferredIntent);
							myDeferredIntent = null;
						}
						break;
					case Found:
						openTree((NetworkTree)params[0]);
						break;
					case NotFound:
						UIUtil.showErrorMessage(NetworkLibraryActivity.this, "emptyNetworkSearchResults");
						break;
					case EmptyCatalog:
						UIUtil.showErrorMessage(NetworkLibraryActivity.this, "emptyCatalog");
						break;
					case NetworkError:
						UIUtil.showMessageText(NetworkLibraryActivity.this, (String)params[0]);
						break;
				}
			}
		});
	}

	private static NetworkTree getLoadableNetworkTree(NetworkTree tree) {
		while (tree instanceof NetworkAuthorTree || tree instanceof NetworkSeriesTree) {
			if (tree.Parent instanceof NetworkTree) {
				tree = (NetworkTree)tree.Parent;
			} else {
				return null;
			}
		}
		return tree;
	}

	@Override
	protected void onCurrentTreeChanged() {
		NetworkLibrary.Instance().fireModelChangedEvent(NetworkLibrary.ChangeListener.Code.SomeCode);
	}

	private void showInitLibraryDialog(String error) {
		final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (which == DialogInterface.BUTTON_POSITIVE) {
					Util.initLibrary(NetworkLibraryActivity.this);
				} else {
					finish();
				}
			}
		};

		final ZLResource dialogResource = ZLResource.resource("dialog");
		final ZLResource boxResource = dialogResource.getResource("networkError");
		final ZLResource buttonResource = dialogResource.getResource("button");
		new AlertDialog.Builder(this)
			.setTitle(boxResource.getResource("title").getValue())
			.setMessage(error)
			.setIcon(0)
			.setPositiveButton(buttonResource.getResource("tryAgain").getValue(), listener)
			.setNegativeButton(buttonResource.getResource("cancel").getValue(), listener)
			.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					listener.onClick(dialog, DialogInterface.BUTTON_NEGATIVE);
				}
			})
			.create().show();
	}

	private void checkAndRun(final Action action, final NetworkTree tree) {
		if (tree instanceof NetworkCatalogTree) {
			final NetworkCatalogTree catalogTree = (NetworkCatalogTree)tree;
			switch (catalogTree.getVisibility()) {
				case B3_FALSE:
					break;
				case B3_TRUE:
					action.run(tree);
					break;
				case B3_UNDEFINED:
					Util.runAuthenticationDialog(this, tree.getLink(), new Runnable() {
						public void run() {
							if (catalogTree.getVisibility() != ZLBoolean3.B3_TRUE) {
								return;
							}
							if (action.Code != ActionCode.SIGNIN) {
								action.run(tree);
							}
						}
					});
					break;
			}
		} else {
			action.run(tree);
		}
	}

	public void onScroll(AbsListView view, int firstVisible, int visibleCount, int totalCount) {
		if (firstVisible + visibleCount + 1 >= totalCount) {
			final FBTree tree = getCurrentTree();
			if (tree instanceof NetworkCatalogTree) {
				((NetworkCatalogTree)tree).loadMoreChildren(totalCount);
			}
		}
	}

	public void onScrollStateChanged(AbsListView view, int state) {
	}
}
