/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.core.helpers;

import java.util.*;

/**
 * A Properties collection that maintains the order of insertion.
 * <p>
 * This class is used to store properties similar to {@link java.util.Properties}.
 * In particular both keys and values are strings and must be not null.
 * However this class is somewhat simplified and does not implement Cloneable, 
 * Serializable and Hashtable.
 * <p>
 * In contrast to java.util.Properties this class maintains the order by which 
 * properties are added. This is implemented using a {@link LinkedHashMap}.
 * <p>
 * The class does not support default properties as they can be expressed by 
 * creating java.util.Properties hierarchies.
 */
public class OrderedProperties extends Dictionary implements Map {

	LinkedHashMap propertyMap = null;

	public static OrderedProperties unmodifiableProperties(Map properties) {
		return new UnmodifiableProperties(properties);
	}

	public OrderedProperties() {
		super();
	}

	public OrderedProperties(int size) {
		super();
		propertyMap = new LinkedHashMap(size);
	}

	public OrderedProperties(OrderedProperties properties) {
		super();
		propertyMap = new LinkedHashMap(properties.size());
		putAll(properties);
	}

	/**
	 * Set the property value.
	 * <p>
	 * If a property with the key already exists, the previous
	 * value is replaced. Otherwise a new property is added at
	 * the end collection.
	 * 
	 * @param key   must not be null
	 * @param value must not be null
	 * @return previous value associated with specified key, or <tt>null</tt>
	 *	       if there was no mapping for key.
	 */
	public Object setProperty(String key, String value) {
		init();
		return propertyMap.put(key, value);
	}

	public String getProperty(String key) {
		return (String) (propertyMap == null ? null : propertyMap.get(key));
	}

	public void putAll(OrderedProperties properties) {
		putAll((Map) properties);
	}

	/**
	 *	Initialize the map.
	 */
	private void init() {
		if (propertyMap == null) {
			propertyMap = new LinkedHashMap();
		}
	}

	public int size() {
		return propertyMap == null ? 0 : propertyMap.size();
	}

	public boolean isEmpty() {
		return propertyMap == null ? true : propertyMap.isEmpty();
	}

	public synchronized void clear() {
		propertyMap = null;
	}

	public Object put(Object arg0, Object arg1) {
		init();
		return propertyMap.put(arg0, arg1);
	}

	public boolean containsKey(Object key) {
		return propertyMap != null ? propertyMap.containsKey(key) : false;
	}

	public boolean containsValue(Object value) {
		return propertyMap != null ? propertyMap.containsValue(value) : false;
	}

	public Set entrySet() {
		return propertyMap != null ? propertyMap.entrySet() : Collections.EMPTY_SET;
	}

	public Object get(Object key) {
		return propertyMap != null ? propertyMap.get(key) : null;
	}

	public Set keySet() {
		return propertyMap != null ? propertyMap.keySet() : Collections.EMPTY_SET;
	}

	public void putAll(Map arg0) {
		init();
		propertyMap.putAll(arg0);
	}

	public Object remove(Object key) {
		return propertyMap != null ? propertyMap.remove(key) : null;
	}

	public Collection values() {
		return propertyMap != null ? propertyMap.values() : Collections.EMPTY_LIST;
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (o instanceof OrderedProperties) {
			OrderedProperties rhs = (OrderedProperties) o;
			if (rhs.propertyMap == this.propertyMap)
				return true;
			if (rhs.propertyMap == null)
				return this.propertyMap.isEmpty();
			else if (this.propertyMap == null)
				return rhs.isEmpty();
			return rhs.propertyMap.equals(this.propertyMap);
		}
		if (this.propertyMap == null) {
			if (o instanceof Map)
				return ((Map) o).isEmpty();
			return false;
		}
		return this.propertyMap.equals(o);
	}

	public int hashCode() {
		return propertyMap == null || propertyMap.isEmpty() ? 0 : propertyMap.hashCode();
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(propertyMap);
		return sb.toString();
	}

	private class ElementsEnum implements Enumeration {

		Iterator iterator = null;

		public ElementsEnum(OrderedProperties properties) {
			iterator = properties.propertyMap.values().iterator();
		}

		public boolean hasMoreElements() {
			return iterator.hasNext();
		}

		public Object nextElement() {
			return iterator.next();
		}
	}

	public Enumeration elements() {
		return new ElementsEnum(this);
	}

	private class KeysEnum implements Enumeration {

		Iterator iterator = null;

		public KeysEnum(OrderedProperties properties) {
			iterator = properties.propertyMap.keySet().iterator();
		}

		public boolean hasMoreElements() {
			return iterator.hasNext();
		}

		public Object nextElement() {
			return iterator.next();
		}
	}

	public Enumeration keys() {
		return new KeysEnum(this);
	}

	private static class UnmodifiableProperties extends OrderedProperties {

		UnmodifiableProperties(Map properties) {
			super();
			for (Iterator iter = properties.entrySet().iterator(); iter.hasNext();) {
				Map.Entry entry = (Map.Entry) iter.next();
				super.put(entry.getKey(), entry.getValue());
			}
		}

		public synchronized Object setProperty(String key, String value) {
			throw new UnsupportedOperationException();
		}

		public synchronized Object put(Object key, Object value) {
			throw new UnsupportedOperationException();
		}

		public synchronized Object remove(Object key) {
			throw new UnsupportedOperationException();
		}

		public synchronized void putAll(Map t) {
			throw new UnsupportedOperationException();
		}

		public synchronized void clear() {
			throw new UnsupportedOperationException();
		}

	}

}