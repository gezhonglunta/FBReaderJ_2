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

package org.geometerplus.zlibrary.core.resources;

abstract public class ZLResource {
	public final String Name;
	
	public static ZLResource resource(String key) {
		ZLTreeResource.buildTree();
		if (ZLTreeResource.ourRoot == null) {
			return ZLMissingResource.Instance;
		}
		return ZLTreeResource.ourRoot.getResource(key);
	}

	protected ZLResource(String name) {
		Name = name;
	}

	abstract public boolean hasValue();
	abstract public String getValue();
	abstract public String getValue(int condition);
	abstract public ZLResource getResource(String key);
}
