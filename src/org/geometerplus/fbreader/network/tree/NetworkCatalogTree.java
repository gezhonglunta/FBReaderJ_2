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

package org.geometerplus.fbreader.network.tree;

import java.util.*;

import org.geometerplus.zlibrary.core.image.ZLImage;
import org.geometerplus.zlibrary.core.util.ZLBoolean3;

import org.geometerplus.fbreader.tree.FBTree;
import org.geometerplus.fbreader.network.*;
import org.geometerplus.fbreader.network.urlInfo.UrlInfo;

public class NetworkCatalogTree extends NetworkTree {
	private final INetworkLink myLink;

	public final NetworkCatalogItem Item;
	protected final ArrayList<NetworkCatalogItem> myChildrenItems =
		new ArrayList<NetworkCatalogItem>();
	private volatile int myLastTotalChildren = -1;

	private long myLoadedTime = -1;

	public NetworkCatalogTree(RootTree parent, INetworkLink link, NetworkCatalogItem item, int position) {
		super(parent, position);
		myLink = link;
		if (item == null) {
			throw new IllegalArgumentException("item cannot be null");
		}
		Item = item;
		addSpecialTrees();
	}

	NetworkCatalogTree(NetworkCatalogTree parent, NetworkCatalogItem item, int position) {
		super(parent, position);
		myLink = parent.myLink;
		if (item == null) {
			throw new IllegalArgumentException("item cannot be null");
		}
		Item = item;
		addSpecialTrees();
	}

	@Override
	public INetworkLink getLink() {
		return myLink;
	}

	public ZLBoolean3 getVisibility() {
		return Item.getVisibility();
	}

	public final boolean canBeOpened() {
		return Item.canBeOpened();
	}

	private SearchItem mySearchItem;

	protected void addSpecialTrees() {
		if ((Item.getFlags() & NetworkCatalogItem.FLAG_ADD_SEARCH_ITEM) != 0) {
			final INetworkLink link = getLink();
			if (link != null && link.getUrl(UrlInfo.Type.Search) != null) {
				if (mySearchItem == null) {
					mySearchItem = new SingleCatalogSearchItem(link);
				}
				myChildrenItems.add(mySearchItem);
				new SearchCatalogTree(this, mySearchItem, -1);
			}
		}
	}

	synchronized void addItem(final NetworkItem item) {
		if (item instanceof NetworkCatalogItem) {
			myChildrenItems.add((NetworkCatalogItem)item);
		}
		myUnconfirmedTrees.add(NetworkTreeFactory.createNetworkTree(this, item));
		NetworkLibrary.Instance().fireModelChangedEvent(NetworkLibrary.ChangeListener.Code.SomeCode);
	}

	@Override
	public String getName() {
		final CharSequence title = Item.Title;
		return title != null ? String.valueOf(title) : "";
	}

	@Override
	public String getSummary() {
		final CharSequence summary = Item.getSummary();
		return summary != null ? summary.toString() : "";
	}

	@Override
	public String getTreeTitle() {
		final INetworkLink link = getLink();
		return link != null ? getName() + " - " + link.getSiteName() : getName();
	}

	@Override
	protected ZLImage createCover() {
		return createCover(Item);
	}

	public boolean isContentValid() {
		if (myLoadedTime < 0) {
			return false;
		}
		final int reloadTime = 15 * 60 * 1000; // 15 minutes in milliseconds
		return System.currentTimeMillis() - myLoadedTime < reloadTime;
	}

	public void updateLoadedTime() {
		myLoadedTime = System.currentTimeMillis();
	}

	public void updateVisibility() {
		final LinkedList<FBTree> toRemove = new LinkedList<FBTree>();

		ListIterator<FBTree> nodeIterator = subTrees().listIterator();
		FBTree currentTree = null;
		int nodeCount = 0;

		for (int i = 0; i < myChildrenItems.size(); ++i) {
			final NetworkCatalogItem currentItem = myChildrenItems.get(i);
			boolean processed = false;
			while (currentTree != null || nodeIterator.hasNext()) {
				if (currentTree == null) {
					currentTree = nodeIterator.next();
				}
				if (!(currentTree instanceof NetworkCatalogTree)) {
					currentTree = null;
					++nodeCount;
					continue;
				}
				NetworkCatalogTree child = (NetworkCatalogTree)currentTree;
				if (child.Item == currentItem) {
					switch (child.Item.getVisibility()) {
						case B3_TRUE:
							child.updateVisibility();
							break;
						case B3_FALSE:
							toRemove.add(child);
							break;
						case B3_UNDEFINED:
							child.clearCatalog();
							break;
					}
					currentTree = null;
					++nodeCount;
					processed = true;
					break;
				} else {
					boolean found = false;
					for (int j = i + 1; j < myChildrenItems.size(); ++j) {
						if (child.Item == myChildrenItems.get(j)) {
							found = true;
							break;
						}
					}
					if (!found) {
						toRemove.add(currentTree);
						currentTree = null;
						++nodeCount;
					} else {
						break;
					}
				}
			}
			final int nextIndex = nodeIterator.nextIndex();
			if (!processed && NetworkTreeFactory.createNetworkTree(this, currentItem, nodeCount) != null) {
				++nodeCount;
				nodeIterator = subTrees().listIterator(nextIndex + 1);
			}
		}

		while (currentTree != null || nodeIterator.hasNext()) {
			if (currentTree == null) {
				currentTree = nodeIterator.next();
			}
			if (currentTree instanceof NetworkCatalogTree) {
				toRemove.add(currentTree);
			}
			currentTree = null;
		}

		for (FBTree tree : toRemove) {
			tree.removeSelf();
		}
	}

	@Override
	public void removeTrees(Set<NetworkTree> trees) {
		for (NetworkTree t : trees) {
			if (t instanceof NetworkCatalogTree) {
				myChildrenItems.remove(((NetworkCatalogTree)t).Item);
			}
		}
		super.removeTrees(trees);
	}

	@Override
	protected String getStringId() {
		return Item.getStringId();
	}

	public void startItemsLoader(boolean checkAuthentication, boolean resumeNotLoad) {
		new CatalogExpander(this, checkAuthentication, resumeNotLoad).start();
	}

	public synchronized void clearCatalog() {
		myChildrenItems.clear();
		myLastTotalChildren = -1;
		clear();
		addSpecialTrees();
		NetworkLibrary.Instance().fireModelChangedEvent(NetworkLibrary.ChangeListener.Code.SomeCode);
	}

	private final Set<NetworkTree> myUnconfirmedTrees =
		Collections.synchronizedSet(new HashSet<NetworkTree>());

	public final void confirmAllItems() {
		myUnconfirmedTrees.clear();
	}

	public final void removeUnconfirmedItems() {
		synchronized (myUnconfirmedTrees) {
			removeTrees(myUnconfirmedTrees);
		}
	}

	public synchronized void loadMoreChildren(int currentTotal) {
		if (currentTotal == subTrees().size()
			&& myLastTotalChildren < currentTotal
			&& !NetworkLibrary.Instance().isLoadingInProgress(this)) {
			myLastTotalChildren = currentTotal;
			startItemsLoader(false, true);
		}
	}
}
