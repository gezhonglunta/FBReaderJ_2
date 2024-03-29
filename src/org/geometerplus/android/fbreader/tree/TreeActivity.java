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

package org.geometerplus.android.fbreader.tree;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;

import org.geometerplus.android.util.UIUtil;

import org.geometerplus.fbreader.tree.FBTree;

public abstract class TreeActivity extends ListActivity {
	private static final String OPEN_TREE_ACTION = "android.fbreader.action.OPEN_TREE";

	public static final String TREE_KEY_KEY = "TreeKey";
	public static final String SELECTED_TREE_KEY_KEY = "SelectedTreeKey";
	public static final String HISTORY_KEY = "HistoryKey";

	private FBTree myCurrentTree;
	// we store the key separately because
	// it will be changed in case of myCurrentTree.removeSelf() call
	private FBTree.Key myCurrentKey;
	private ArrayList<FBTree.Key> myHistory;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Thread.setDefaultUncaughtExceptionHandler(new org.geometerplus.zlibrary.ui.android.library.UncaughtExceptionHandler(this));
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
	}

	@Override
	public TreeAdapter getListAdapter() {
		return (TreeAdapter)super.getListAdapter();
	}

	protected FBTree getCurrentTree() {
		return myCurrentTree;
	}

	@Override
	protected void onNewIntent(final Intent intent) {
		if (OPEN_TREE_ACTION.equals(intent.getAction())) {
			runOnUiThread(new Runnable() {
				public void run() {
					init(intent);
				}
			});
		} else {
			super.onNewIntent(intent);
		}
	}

	protected abstract FBTree getTreeByKey(FBTree.Key key);
	public abstract boolean isTreeSelected(FBTree tree);

	protected boolean isTreeInvisible(FBTree tree) {
		return false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			FBTree parent = null;
			while (parent == null && !myHistory.isEmpty()) {
				parent = getTreeByKey(myHistory.remove(myHistory.size() - 1));
			}
			if (parent == null) {
				parent = myCurrentTree.Parent;
			}
			if (parent != null && !isTreeInvisible(parent)) {
				openTree(parent, myCurrentTree, false);
				return true;
			}
		}

		return super.onKeyDown(keyCode, event);
	}

	// TODO: change to protected
	public void openTree(final FBTree tree) {
		openTree(tree, null, true);
	}

	protected void onCurrentTreeChanged() {
	}

	private void openTree(final FBTree tree, final FBTree treeToSelect, final boolean storeInHistory) {
		switch (tree.getOpeningStatus()) {
			case WAIT_FOR_OPEN:
			case ALWAYS_RELOAD_BEFORE_OPENING:
				final String messageKey = tree.getOpeningStatusMessage();
				if (messageKey != null) {
					UIUtil.runWithMessage(
						TreeActivity.this, messageKey,
						new Runnable() {
							public void run() {
								tree.waitForOpening();
							}
						},
						new Runnable() {
							public void run() {
								openTreeInternal(tree, treeToSelect, storeInHistory);
							}
						},
						true
					);
				} else {
					tree.waitForOpening();
					openTreeInternal(tree, treeToSelect, storeInHistory);
				}
				break;
			default:
				openTreeInternal(tree, treeToSelect, storeInHistory);
				break;
		}
	}

	protected void init(Intent intent) {
		final FBTree.Key key = (FBTree.Key)intent.getSerializableExtra(TREE_KEY_KEY);
		final FBTree.Key selectedKey = (FBTree.Key)intent.getSerializableExtra(SELECTED_TREE_KEY_KEY);
		myCurrentTree = getTreeByKey(key);
		// not myCurrentKey = key
		// because key might be null
		myCurrentKey = myCurrentTree.getUniqueKey();
		final TreeAdapter adapter = getListAdapter();
		adapter.replaceAll(myCurrentTree.subTrees());
		setTitle(myCurrentTree.getTreeTitle());
		final FBTree selectedTree =
			selectedKey != null ? getTreeByKey(selectedKey) : adapter.getFirstSelectedItem();
		final int index = adapter.getIndex(selectedTree);
		if (index != -1) {
			setSelection(index);
			getListView().post(new Runnable() {
				public void run() {
					setSelection(index);
				}
			});
		}

		myHistory = (ArrayList<FBTree.Key>)intent.getSerializableExtra(HISTORY_KEY);
		if (myHistory == null) {
			myHistory = new ArrayList<FBTree.Key>();
		}
		onCurrentTreeChanged();
	}

	private void openTreeInternal(FBTree tree, FBTree treeToSelect, boolean storeInHistory) {
		switch (tree.getOpeningStatus()) {
			case READY_TO_OPEN:
			case ALWAYS_RELOAD_BEFORE_OPENING:
				if (storeInHistory && !myCurrentKey.equals(tree.getUniqueKey())) {
					myHistory.add(myCurrentKey);
				}
				onNewIntent(new Intent(this, getClass())
					.setAction(OPEN_TREE_ACTION)
					.putExtra(TREE_KEY_KEY, tree.getUniqueKey())
					.putExtra(
						SELECTED_TREE_KEY_KEY,
						treeToSelect != null ? treeToSelect.getUniqueKey() : null
					)
					.putExtra(HISTORY_KEY, myHistory)
				);
				break;
			case CANNOT_OPEN:
				UIUtil.showErrorMessage(TreeActivity.this, tree.getOpeningStatusMessage());
				break;
		}
	}
}
