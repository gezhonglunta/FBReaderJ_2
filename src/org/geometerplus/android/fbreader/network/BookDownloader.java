/*
 * Copyright (C) 2009-2012 Geometer Plus <contact@geometerplus.com>
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

import java.util.List;

import android.os.Bundle;
import android.app.Activity;
import android.net.Uri;
import android.content.Intent;

import org.geometerplus.fbreader.network.urlInfo.BookUrlInfo;

public class BookDownloader extends Activity {

	public static boolean acceptsUri(Uri uri) {
		final List<String> path = uri.getPathSegments();
		if ((path == null) || path.isEmpty()) {
			return false;
		}

		final String scheme = uri.getScheme();
		if ("epub".equals(scheme) || "book".equals(scheme)) {
			return true;
		}

		final String fileName = path.get(path.size() - 1).toLowerCase();
		return
			fileName.endsWith(".fb2.zip") ||
			fileName.endsWith(".fb2") ||
			fileName.endsWith(".epub") ||
			fileName.endsWith(".mobi") ||
			fileName.endsWith(".prc");
	}

	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		Thread.setDefaultUncaughtExceptionHandler(new org.geometerplus.zlibrary.ui.android.library.UncaughtExceptionHandler(this));

		final Intent intent = getIntent();
		Uri uri = intent.getData();
		intent.setData(null);
		if (uri == null || !acceptsUri(uri)) {
			finish();
			return;
		}

		if (!intent.hasExtra(BookDownloaderService.SHOW_NOTIFICATIONS_KEY)) {
			intent.putExtra(BookDownloaderService.SHOW_NOTIFICATIONS_KEY, 
				BookDownloaderService.Notifications.ALREADY_DOWNLOADING);
		}
		if ("epub".equals(uri.getScheme())) {
			uri = uri.buildUpon().scheme("http").build();
			intent.putExtra(BookDownloaderService.BOOK_FORMAT_KEY,
					BookUrlInfo.Format.EPUB);
		}

		startService(
			new Intent(Intent.ACTION_DEFAULT, uri, this, BookDownloaderService.class)
				.putExtras(intent.getExtras())
		);
		finish();
	}
}
