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

package org.geometerplus.android.fbreader.network.action;

import java.util.Map;

import android.app.Activity;
import android.content.Intent;

import org.geometerplus.fbreader.network.*;
import org.geometerplus.fbreader.network.tree.*;

import org.geometerplus.android.fbreader.network.NetworkLibraryActivity;
import org.geometerplus.android.fbreader.network.NetworkLibrarySecondaryActivity;

import org.geometerplus.android.util.PackageUtil;

public class OpenCatalogAction extends Action {
	public OpenCatalogAction(Activity activity) {
		super(activity, ActionCode.OPEN_CATALOG, "openCatalog", -1);
	}

	@Override
	public boolean isVisible(NetworkTree tree) {
		if (tree instanceof NetworkAuthorTree || tree instanceof NetworkSeriesTree) {
			return true;
		} else if (tree instanceof NetworkCatalogTree) {
			return ((NetworkCatalogTree)tree).canBeOpened();
		} else {
			return false;
		}
	}

	@Override
	public void run(NetworkTree tree) {
		if (tree instanceof NetworkCatalogTree) {
			doExpandCatalog((NetworkCatalogTree)tree);
		} else {
			doOpenTree(tree);
		}
	}

	private void doOpenTree(NetworkTree tree) {
		if (myActivity instanceof NetworkLibraryActivity) {
			((NetworkLibraryActivity)myActivity).openTree(tree);
		} else {
			myActivity.startActivity(
				new Intent(myActivity.getApplicationContext(), NetworkLibrarySecondaryActivity.class)
					.putExtra(NetworkLibraryActivity.TREE_KEY_KEY, tree.getUniqueKey())
			);
		}
	}

	private void doExpandCatalog(final NetworkCatalogTree tree) {
		final NetworkItemsLoader loader = NetworkLibrary.Instance().getStoredLoader(tree);
		if (loader != null && loader.canResumeLoading()) {
			doOpenTree(tree);
		} else if (loader != null) {
			loader.setPostRunnable(new Runnable() {
				public void run() {
					doLoadCatalog(tree);
				}
			});
		} else {
			doLoadCatalog(tree);
		}
	}

	private void doLoadCatalog(final NetworkCatalogTree tree) {
		boolean resumeNotLoad = false;
		if (tree.hasChildren()) {
			if (tree.isContentValid()) {
				if (tree.Item.supportsResumeLoading()) {
					resumeNotLoad = true;
				} else {
					doOpenTree(tree);
					return;
				}
			} else {
				tree.clearCatalog();
			}
		}

		tree.startItemsLoader(true, resumeNotLoad);
		processExtraData(tree.Item.extraData(), new Runnable() {
			public void run() {
				doOpenTree(tree);
			}
		});
	}

	private void processExtraData(Map<String,String> extraData, final Runnable postRunnable) {
		if (extraData != null && !extraData.isEmpty()) {
			PackageUtil.runInstallPluginDialog(myActivity, extraData, postRunnable);
		} else {
			postRunnable.run();
		}
	}
}
