/*
 * Copyright (C) 2009 Apps Organizer
 *
 * This file is part of Apps Organizer
 *
 * Apps Organizer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Apps Organizer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Apps Organizer.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.google.code.appsorganizer.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public abstract class DbDao<T> {

	protected final String name;

	protected SQLiteDatabase db;

	protected final List<DbColumns<T>> columns = new ArrayList<DbColumns<T>>();

	public DbDao(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public List<DbColumns<T>> getColumns() {
		return columns;
	}

	protected void addColumn(DbColumns<T> c) {
		columns.add(c);
	}

	public String getCreateTableScript() {
		StringBuilder b = new StringBuilder("create table ");
		b.append(name);
		b.append(" (");
		boolean first = true;
		for (DbColumns<T> c : columns) {
			if (first) {
				first = false;
			} else {
				b.append(',');
			}
			b.append(c.getName());
			b.append(' ');
			b.append(c.getDescription());
		}
		b.append(");");
		return b.toString();
	}

	public String getSelectAllQuery() {
		StringBuilder b = new StringBuilder();
		b.append(name);
		b.append(" (");
		for (DbColumns<T> c : columns) {
			if (b.length() > 0) {
				b.append(',');
			}
			b.append(c.getName());
		}
		return "select " + b.toString() + " from " + name;
	}

	public String getDropTableScript() {
		return "DROP TABLE IF EXISTS " + name;
	}

	public abstract T createNewObject();

	public List<T> queryForList(List<DbColumns<T>> cols, DbColumns<T> filterCol, String filterValue, String orderBy, String groupBy,
			String having) {
		Cursor c = query(cols, filterCol, filterValue, orderBy, groupBy, having);
		return convertCursorToList(c, cols);
	}

	public ArrayList<T> queryForList(List<DbColumns<T>> cols, HashMap<DbColumns<T>, String> filter, String orderBy, String groupBy,
			String having) {
		Cursor c = query(cols, filter, orderBy, groupBy, having);
		return convertCursorToList(c, cols);
	}

	public T queryForObject(List<DbColumns<T>> cols, HashMap<DbColumns<T>, String> filter, String groupBy, String having) {
		Cursor c = query(cols, filter, null, groupBy, having);
		return convertCursorToObject(c, cols);
	}

	public T queryForObject(List<DbColumns<T>> cols, DbColumns<T> filterCol, String filterValue, String groupBy, String having) {
		Cursor c = query(cols, filterCol, filterValue, null, groupBy, having);
		return convertCursorToObject(c, cols);
	}

	public List<String> queryForStringList(boolean distinct, DbColumns<T> col, HashMap<DbColumns<T>, String> filter, String orderBy,
			String groupBy, String having) {
		Cursor c = query(distinct, Collections.singletonList(col), filter, orderBy, groupBy, having);
		return convertCursorToStringList(c, col);
	}

	protected List<String> convertCursorToStringList(Cursor c, DbColumns<T> col) {
		List<String> l = new ArrayList<String>();
		try {
			while (c.moveToNext()) {
				l.add(col.getString(c));
			}
		} finally {
			c.close();
		}
		return l;
	}

	public HashMap<String, String> queryForStringMap(boolean distinct, DbColumns<T> colKey, DbColumns<T> colValue,
			HashMap<DbColumns<T>, String> filter, String orderBy, String groupBy, String having) {
		@SuppressWarnings("unchecked")
		List<DbColumns<T>> l = Arrays.asList(colKey, colValue);
		Cursor c = query(distinct, l, filter, orderBy, groupBy, having);
		return convertCursorToStringMap(c, colKey, colValue);
	}

	protected HashMap<String, String> convertCursorToStringMap(Cursor c, DbColumns<T> colKey, DbColumns<T> colValue) {
		HashMap<String, String> m = new HashMap<String, String>();
		try {
			while (c.moveToNext()) {
				m.put(colKey.getString(c), colValue.getString(c));
			}
		} finally {
			c.close();
		}
		return m;
	}

	protected ArrayList<T> convertCursorToList(Cursor c, List<DbColumns<T>> cols) {
		ArrayList<T> l = new ArrayList<T>();
		try {
			while (c.moveToNext()) {
				T t = createNewObject();
				for (DbColumns<T> col : cols) {
					col.populateObject(t, c);
				}
				l.add(t);
			}
		} finally {
			c.close();
		}
		return l;
	}

	protected T convertCursorToObject(Cursor c, List<DbColumns<T>> cols) {
		try {
			while (c.moveToNext()) {
				T t = createNewObject();
				for (DbColumns<T> col : cols) {
					col.populateObject(t, c);
				}
				if (c.moveToNext()) {
					throw new RuntimeException("Query returned more than one object");
				} else {
					return t;
				}
			}
		} finally {
			c.close();
		}
		return null;
	}

	public Cursor query(boolean distinct, List<DbColumns<T>> cols, HashMap<DbColumns<T>, String> filter, String orderBy, String groupBy,
			String having) {
		return db.query(distinct, name, columnsToStringArray(cols), filterToSelection(filter), filterToSelectionArgs(filter), groupBy,
				having, orderBy, null);
	}

	public Cursor query(List<DbColumns<T>> cols, HashMap<DbColumns<T>, String> filter, String orderBy, String groupBy, String having) {
		return db.query(name, columnsToStringArray(cols), filterToSelection(filter), filterToSelectionArgs(filter), groupBy, having,
				orderBy);
	}

	public Cursor query(List<DbColumns<T>> cols, DbColumns<T> filterCol, String filterValue, String orderBy, String groupBy, String having) {
		return db.query(name, columnsToStringArray(cols), filterCol.getName() + "=?", new String[] { filterValue }, groupBy, having,
				orderBy);
	}

	private String filterToSelection(Map<DbColumns<T>, String> filter) {
		if (filter != null) {
			StringBuilder b = new StringBuilder();
			for (Entry<DbColumns<T>, String> e : filter.entrySet()) {
				if (b.length() > 0) {
					b.append(" and ");
				}
				b.append(e.getKey().getName());
				b.append("=?");
			}
			return b.toString();
		} else {
			return null;
		}
	}

	private String[] filterToSelectionArgs(Map<DbColumns<T>, String> filter) {
		if (filter != null) {
			String[] ret = new String[filter.size()];
			int pos = 0;
			for (Entry<DbColumns<T>, String> e : filter.entrySet()) {
				ret[pos++] = e.getValue();
			}
			return ret;
		} else {
			return null;
		}
	}

	protected String[] columnsToStringArray(List<DbColumns<T>> cols) {
		String[] ret = new String[cols.size()];
		int pos = 0;
		for (DbColumns<T> dbColumns : cols) {
			ret[pos++] = dbColumns.getName();
		}
		return ret;
	}

	public long insert(T obj) {
		ContentValues v = new ContentValues();
		for (DbColumns<T> col : columns) {
			col.populateContent(obj, v);
		}
		return db.insert(name, null, v);
	}

	public void setDb(SQLiteDatabase db) {
		this.db = db;
	}

	protected T[] convertCursorToArray(Cursor c, T[] l) {
		try {
			int i = 0;
			while (c.moveToNext()) {
				l[i++] = createObject(c);
			}
		} finally {
			c.close();
		}
		return l;
	}

	protected T createObject(Cursor c) {
		T t = createNewObject();
		for (DbColumns<T> col : columns) {
			col.populateObject(t, c);
		}
		return t;
	}

}
