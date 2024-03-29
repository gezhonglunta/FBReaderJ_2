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

package org.geometerplus.fbreader.network.opds;

import org.geometerplus.zlibrary.core.xml.ZLStringMap;

import org.geometerplus.fbreader.network.atom.*;

class OPDSEntry extends ATOMEntry {
	public String DCLanguage;
	public String DCPublisher;
	public DCDate DCIssued;

	public String SeriesTitle;
	public float SeriesIndex;

	protected OPDSEntry(ZLStringMap attributes) {
		super(attributes);
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder("[");
		buf.append(super.toString());
		buf.append(",DCLanguage=").append(DCLanguage);
		buf.append(",DCPublisher=").append(DCPublisher);
		buf.append(",DCIssued=").append(DCIssued);
		buf.append(",SeriesTitle=").append(SeriesTitle);
		buf.append(",SeriesIndex=").append(SeriesIndex);
		buf.append("]");
		return buf.toString();
	}
}
