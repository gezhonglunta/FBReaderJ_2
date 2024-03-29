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

package org.geometerplus.fbreader.network;

import java.io.*;

import android.net.Uri;

import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.image.ZLFileImage;
import org.geometerplus.zlibrary.core.image.ZLLoadableImage;
import org.geometerplus.zlibrary.core.network.ZLNetworkManager;
import org.geometerplus.zlibrary.core.network.ZLNetworkException;
import org.geometerplus.zlibrary.core.util.MimeType;

import org.geometerplus.fbreader.Paths;

public final class NetworkImage extends ZLLoadableImage {
	public final String Url;

	public NetworkImage(String url, MimeType mimeType) {
		super(mimeType);
		Url = url;
		new File(Paths.networkCacheDirectory()).mkdirs();
	}

	private static final String TOESCAPE = "<>:\"|?*\\";

	public static String makeImageFilePath(String url, MimeType mimeType) {
		final Uri uri = Uri.parse(url);

		String host = uri.getHost();
		if (host == null) {
			host = "host.unknown";
		}

		final StringBuilder path = new StringBuilder(host);
		if (host.startsWith("www.")) {
			path.delete(0, 4);
		}
		path.insert(0, File.separator);
		path.insert(0, Paths.networkCacheDirectory());

		int index = path.length();

		final String uriPath = uri.getPath();
		if (uriPath != null) {
			path.append(uriPath);
		}

		int nameIndex = index;
		while (index < path.length()) {
			char ch = path.charAt(index);
			if (TOESCAPE.indexOf(ch) != -1) {
				path.setCharAt(index, '_');
			}
			if (ch == '/') {
				if (index + 1 == path.length()) {
					path.deleteCharAt(index);
				} else {
					path.setCharAt(index, '_');
					nameIndex = index + 1;
				}
			}
			++index;
		}

		String ext = null;
		if (MimeType.IMAGE_PNG.equals(mimeType)) {
			ext = ".png";
		} else if (MimeType.IMAGE_JPEG.equals(mimeType)) {
			if (path.length() > 5 && path.substring(path.length() - 5).equals(".jpeg")) {
				ext = ".jpeg";
			} else {
				ext = ".jpg";
			}
		}

		if (ext == null) {
			int j = path.lastIndexOf(".");
			if (j > nameIndex) {
				ext = path.substring(j);
				path.delete(j, path.length());
			} else {
				ext = "";
			}
		} else if (path.length() > ext.length() && path.substring(path.length() - ext.length()).equals(ext)) {
			path.delete(path.length() - ext.length(), path.length());
		}

		String query = uri.getQuery();
		if (query != null) {
			index = 0;
			while (index < query.length()) {
				int j = query.indexOf("&", index);
				if (j == -1) {
					j = query.length();
				}
				String param = query.substring(index, j);
				if (!param.startsWith("username=")
					&& !param.startsWith("password=")
					&& !param.endsWith("=")) {
					int k = path.length();
					path.append("_").append(param);
					while (k < path.length()) {
						char ch = path.charAt(k);
						if (TOESCAPE.indexOf(ch) != -1 || ch == '/') {
							path.setCharAt(k, '_');
						}
						++k;
					}
				}
				index = j + 1;
			}
		}
		return path.append(ext).toString();
	}

	public String getFilePath() {
		return makeImageFilePath(Url, mimeType());
	}

	@Override
	public int sourceType() {
		return SourceType.NETWORK;
	}

	@Override
	public String getId() {
		return Url;
	}

	public String getURI() {
		// TODO: implement
		return null;
	}

	@Override
	public void synchronize() {
		synchronizeInternal(false);
	}

	@Override
	public void synchronizeFast() {
		synchronizeInternal(true);
	}

	private final void synchronizeInternal(boolean doFast) {
		if (isSynchronized()) {
			return;
		}
		try {
			final String path = getFilePath();
			if (path == null) {
				// TODO: error message ???
				return;
			}
			final int index = path.lastIndexOf(File.separator);
			if (index != -1) {
				final String dir = path.substring(0, index);
				final File dirFile = new File(dir);
				if (!dirFile.exists() && !dirFile.mkdirs()) {
					// TODO: error message ???
					return;
				}
				if (!dirFile.exists() || !dirFile.isDirectory()) {
					// TODO: error message ???
					return;
				}
			}
			final File imageFile = new File(path);
			if (imageFile.exists()) {
				final long diff = System.currentTimeMillis() - imageFile.lastModified();
				final long valid = 7 * 24 * 60 * 60 * 1000; // one week in milliseconds; FIXME: hardcoded const
				if (diff >= 0 && diff <= valid) {
					return;
				}
				imageFile.delete();
			}
			if (doFast) {
				return;
			}

			try {
				ZLNetworkManager.Instance().downloadToFile(Url, imageFile);
			} catch (ZLNetworkException e) {
			}
		} finally {
			setSynchronized();
		}
	}

	private ZLFileImage myFileImage;
	@Override
	public InputStream inputStream() {
		if (myFileImage == null) {
			if (!isSynchronized()) {
				return null;
			}
			final String path = getFilePath();
			if (path == null) {
				return null;
			}
			final ZLFile file = ZLFile.createFileByPath(path);
			if (file == null) {
				return null;
			}
			myFileImage = new ZLFileImage(mimeType(), file);
		}
		return myFileImage.inputStream();
	}
}
