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

import java.util.*;

import org.geometerplus.zlibrary.core.xml.ZLStringMap;
import org.geometerplus.zlibrary.core.xml.ZLXMLReaderAdapter;
import org.geometerplus.zlibrary.core.filesystem.*;

final class ZLTreeResource extends ZLResource {
	private static interface Condition {
		abstract boolean accepts(int number);
	}

	private static class ValueCondition implements Condition {
		private final int myValue;

		ValueCondition(int value) {
			myValue = value;
		}

		public boolean accepts(int number) {
			return myValue == number;
		}
	}

	private static class RangeCondition implements Condition {
		private final int myMin;
		private final int myMax;

		RangeCondition(int min, int max) {
			myMin = min;
			myMax = max;
		}

		public boolean accepts(int number) {
			return myMin <= number && number <= myMax;
		}
	}

	private static class ModRangeCondition implements Condition {
		private final int myMin;
		private final int myMax;
		private final int myBase;

		ModRangeCondition(int min, int max, int base) {
			myMin = min;
			myMax = max;
			myBase = base;
		}

		public boolean accepts(int number) {
			number = number % myBase;
			return myMin <= number && number <= myMax;
		}
	}

	private static class ModCondition implements Condition {
		private final int myMod;
		private final int myBase;

		ModCondition(int mod, int base) {
			myMod = mod;
			myBase = base;
		}

		public boolean accepts(int number) {
			return number % myBase == myMod;
		}
	}

	static private Condition parseCondition(String description) {
		final String[] parts = description.split(" ");
		try {
			if ("range".equals(parts[0])) {
				return new RangeCondition(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
			} else if ("mod".equals(parts[0])) {
				return new ModCondition(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
			} else if ("modrange".equals(parts[0])) {
				return new ModRangeCondition(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
			} else if ("value".equals(parts[0])) {
				return new ValueCondition(Integer.parseInt(parts[1]));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	static volatile ZLTreeResource ourRoot;
	private static final Object ourLock = new Object();

	private static long ourTimeStamp = 0;
	private static String ourLanguage = null;
	private static String ourCountry = null;

	private boolean myHasValue;
	private	String myValue;
	private HashMap<String,ZLTreeResource> myChildren;
	private LinkedHashMap<Condition,String> myConditionalValues;

	static void buildTree() {
		synchronized (ourLock) {
			if (ourRoot == null) {
				ourRoot = new ZLTreeResource("", null);
				ourLanguage = "en";
				ourCountry = "UK";
				loadData();
			}
		}
	}

	private static void updateLanguage() {
		final long timeStamp = System.currentTimeMillis();
		if (timeStamp > ourTimeStamp + 1000) {
			synchronized (ourLock) {
				if (timeStamp > ourTimeStamp + 1000) {
					ourTimeStamp = timeStamp;
					final String language = Locale.getDefault().getLanguage();
					final String country = Locale.getDefault().getCountry();
					if ((language != null && !language.equals(ourLanguage)) ||
						(country != null && !country.equals(ourCountry))) {
						ourLanguage = language;
						ourCountry = country;
						loadData();
					}
				}
			}
		}
	}

	private static void loadData(ResourceTreeReader reader, String fileName) {
		reader.readDocument(ourRoot, ZLResourceFile.createResourceFile("resources/zlibrary/" + fileName));
		reader.readDocument(ourRoot, ZLResourceFile.createResourceFile("resources/application/" + fileName));
	}

	private static void loadData() {
		ResourceTreeReader reader = new ResourceTreeReader();
		loadData(reader, ourLanguage + ".xml");
		loadData(reader, ourLanguage + "_" + ourCountry + ".xml");
	}

	private	ZLTreeResource(String name, String value) {
		super(name);
		setValue(value);
	}

	private void setValue(String value) {
		myHasValue = value != null;
		myValue = value;
	}

	@Override
	public boolean hasValue() {
		return myHasValue;
	}

	@Override
	public String getValue() {
		updateLanguage();
		return myHasValue ? myValue : ZLMissingResource.Value;
	}

	@Override
	public String getValue(int number) {
		updateLanguage();
		if (myConditionalValues != null) {
			for (Map.Entry<Condition,String> entry: myConditionalValues.entrySet()) {
				if (entry.getKey().accepts(number)) {
					return entry.getValue();
				}
			}
		}
		return myHasValue ? myValue : ZLMissingResource.Value;
	}

	@Override
	public ZLResource getResource(String key) {
		final HashMap<String,ZLTreeResource> children = myChildren;
		if (children != null) {
			ZLTreeResource child = children.get(key);
			if (child != null) {
				return child;
			}
		}
		return ZLMissingResource.Instance;
	}

	private static class ResourceTreeReader extends ZLXMLReaderAdapter {
		private static final String NODE = "node";
		private final ArrayList<ZLTreeResource> myStack = new ArrayList<ZLTreeResource>();

		public void readDocument(ZLTreeResource root, ZLFile file) {
			myStack.clear();
			myStack.add(root);
			readQuietly(file);
		}

		@Override
		public boolean dontCacheAttributeValues() {
			return true;
		}

		@Override
		public boolean startElementHandler(String tag, ZLStringMap attributes) {
			final ArrayList<ZLTreeResource> stack = myStack;
			if (!stack.isEmpty() && (NODE.equals(tag))) {
				final String name = attributes.getValue("name");
				final String condition = attributes.getValue("condition");
				final String value = attributes.getValue("value");
				final ZLTreeResource peek = stack.get(stack.size() - 1);
				if (name != null) {
					ZLTreeResource node;
					HashMap<String,ZLTreeResource> children = peek.myChildren;
					if (children == null) {
						node = null;
						children = new HashMap<String,ZLTreeResource>();
						peek.myChildren = children;
					} else {
						node = children.get(name);
					}
					if (node == null) {
						node = new ZLTreeResource(name, value);
						children.put(name, node);
					} else {
						if (value != null) {
							node.setValue(value);
							node.myConditionalValues = null;
						}
					}
					stack.add(node);
				} else if (condition != null && value != null) {
					final Condition compiled = parseCondition(condition);
					if (compiled != null) {
						if (peek.myConditionalValues == null) {
							peek.myConditionalValues = new LinkedHashMap<Condition,String>();
						}
						peek.myConditionalValues.put(compiled, value);
					}
					stack.add(peek);
				}
			}
			return false;
		}

		@Override
		public boolean endElementHandler(String tag) {
			final ArrayList<ZLTreeResource> stack = myStack;
			if (!stack.isEmpty() && (NODE.equals(tag))) {
				stack.remove(stack.size() - 1);
			}
			return false;
		}
	}
}
