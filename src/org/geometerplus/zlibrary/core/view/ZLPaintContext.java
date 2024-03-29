/*
 * Copyright (C) 2007-2012 Geometer Plus <contact@geometerplus.com>
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

package org.geometerplus.zlibrary.core.view;

import java.util.*;

import org.geometerplus.zlibrary.core.util.ZLColor;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.image.ZLImageData;

abstract public class ZLPaintContext {
	private final ArrayList<String> myFamilies = new ArrayList<String>();

	protected ZLPaintContext() {
	}

	public static enum WallpaperMode {
		TILE,
		TILE_MIRROR
	}
	abstract public void clear(ZLFile wallpaperFile, WallpaperMode mode);
	abstract public void clear(ZLColor color);
	abstract public ZLColor getBackgroundColor();

	private boolean myResetFont = true;
	private String myFontFamily = "";
	private int myFontSize;
	private boolean myFontIsBold;
	private boolean myFontIsItalic;
	private boolean myFontIsUnderlined;
	private boolean myFontIsStrikedThrough;

	public final void setFont(String family, int size, boolean bold, boolean italic, boolean underline, boolean strikeThrough) {
		if ((family != null) && !myFontFamily.equals(family)) {
			myFontFamily = family;
			myResetFont = true;
		}
		if (myFontSize != size) {
			myFontSize = size;
			myResetFont = true;
		}
		if (myFontIsBold != bold) {
			myFontIsBold = bold;
			myResetFont = true;
		}
		if (myFontIsItalic != italic) {
			myFontIsItalic = italic;
			myResetFont = true;
		}
		if (myFontIsUnderlined != underline) {
			myFontIsUnderlined = underline;
			myResetFont = true;
		}
		if (myFontIsStrikedThrough != strikeThrough) {
			myFontIsStrikedThrough = strikeThrough;
			myResetFont = true;
		}
		if (myResetFont) {
			myResetFont = false;
			setFontInternal(myFontFamily, size, bold, italic, underline, strikeThrough);
			mySpaceWidth = -1;
			myStringHeight = -1;
			myDescent = -1;
		}
	}

	abstract protected void setFontInternal(String family, int size, boolean bold, boolean italic, boolean underline, boolean strikeThrough);

	abstract public void setTextColor(ZLColor color);
	abstract public void setLineColor(ZLColor color);
	abstract public void setLineWidth(int width);

	final public void setFillColor(ZLColor color) {
		setFillColor(color, 0xFF);
	}
	abstract public void setFillColor(ZLColor color, int alpha);

	abstract public int getWidth();
	abstract public int getHeight();
	
	public final int getStringWidth(String string) {
		return getStringWidth(string.toCharArray(), 0, string.length());
	}
	abstract public int getStringWidth(char[] string, int offset, int length);

	private int mySpaceWidth = -1;
	public final int getSpaceWidth() {
		int spaceWidth = mySpaceWidth;
		if (spaceWidth == -1) {
			spaceWidth = getSpaceWidthInternal();
			mySpaceWidth = spaceWidth;
		}
		return spaceWidth;
	}
	abstract protected int getSpaceWidthInternal();

	private int myStringHeight = -1;
	public final int getStringHeight() {
		int stringHeight = myStringHeight;
		if (stringHeight == -1) {
			stringHeight = getStringHeightInternal();
			myStringHeight = stringHeight;
		}
		return stringHeight;
	}
	abstract protected int getStringHeightInternal();

	private int myDescent = -1;
	public final int getDescent() {
		int descent = myDescent;
		if (descent == -1) {
			descent = getDescentInternal();
			myDescent = descent;
		}
		return descent;
	}
	abstract protected int getDescentInternal();

	public final void drawString(int x, int y, String string) {
		drawString(x, y, string.toCharArray(), 0, string.length());
	}
	abstract public void drawString(int x, int y, char[] string, int offset, int length);

	public static final class Size {
		public final int Width;
		public final int Height;

		public Size(int w, int h) {
			Width = w;
			Height = h;
		}

		@Override
		public boolean equals(Object other) {
			if (other == this) {
				return true;
			}
			if (!(other instanceof Size)) {
				return false;
			}
			final Size s = (Size)other;
			return Width == s.Width && Height == s.Height;
		}
	}
	public static enum ScalingType {
		OriginalSize,
		IntegerCoefficient,
		FitMaximum
	}

	abstract public Size imageSize(ZLImageData image, Size maxSize, ScalingType scaling);
	abstract public void drawImage(int x, int y, ZLImageData image, Size maxSize, ScalingType scaling);

	abstract public void drawLine(int x0, int y0, int x1, int y1);
	abstract public void fillRectangle(int x0, int y0, int x1, int y1);

	abstract public void drawPolygonalLine(int[] xs, int ys[]);
	abstract public void fillPolygon(int[] xs, int[] ys);
	abstract public void drawOutline(int[] xs, int ys[]);
}
