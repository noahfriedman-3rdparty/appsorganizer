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
package com.google.code.appsorganizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Dialog;
import android.app.ListActivity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.google.code.appsorganizer.db.DatabaseHelper;
import com.google.code.appsorganizer.db.DbChangeListener;
import com.google.code.appsorganizer.dialogs.GenericDialogManager;

public class AppsListActivity extends ListActivity {

	private DatabaseHelper dbHelper;

	private List<Application> apps;

	private ChooseLabelDialogCreator chooseLabelDialog;

	private GenericDialogManager genericDialogManager;

	private List<? extends Map<String, ?>> convertToMapArray(List<Application> apps) {
		List<Map<String, Object>> l = new ArrayList<Map<String, Object>>();
		for (Application application : apps) {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("image", application.getIcon());
			m.put("name", application.getLabel());
			m.put("appInfo", application);
			l.add(m);
		}
		return l;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		dbHelper = new DatabaseHelper(this);
		setContentView(R.layout.main);
		genericDialogManager = new GenericDialogManager(this);
		chooseLabelDialog = new ChooseLabelDialogCreator(dbHelper);
		genericDialogManager.addDialog(chooseLabelDialog);
		apps = ApplicationInfoManager.singleton(getPackageManager()).getAppsArray(null);
		getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				chooseLabelDialog.setCurrentApp(apps.get(position));
				showDialog(chooseLabelDialog.getDialogId());
			}
		});
		final SimpleAdapter appsAdapter = new AppsListAdapter(AppsListActivity.this, convertToMapArray(apps), R.layout.app_row,
				new String[] { "image", "name", "appInfo" }, new int[] { R.id.image, R.id.name, R.id.labels });
		dbHelper.appsLabelDao.addListener(new DbChangeListener() {
			public void notifyDataSetChanged() {
				appsAdapter.notifyDataSetChanged();
			}
		});
		setListAdapter(appsAdapter);

		registerForContextMenu(getListView());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (dbHelper != null) {
			dbHelper.close();
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		Application app = apps.get(info.position);
		ApplicationContextMenuManager.singleton().createMenu(menu, app);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Application app = apps.get(info.position);
		ApplicationContextMenuManager.singleton().onContextItemSelected(item, app, this, chooseLabelDialog);
		return true;
	}

	public final class AppsListAdapter extends SimpleAdapter {

		public AppsListAdapter(AppsListActivity homeActivity, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
			super(homeActivity, data, resource, from, to);

			setViewBinder(new SimpleAdapter.ViewBinder() {

				public boolean setViewValue(View view, Object data, String textRepresentation) {
					switch (view.getId()) {
					case R.id.image:
						((ImageView) view).setImageDrawable((Drawable) data);
						return true;
					case R.id.labels:
						((TextView) view).setText(dbHelper.labelDao.getLabelsString((Application) data));
						return true;
					default:
						return false;
					}
				}
			});
		}
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		genericDialogManager.onPrepareDialog(id, dialog);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		return genericDialogManager.onCreateDialog(id);
	}

	public ChooseLabelDialogCreator getChooseLabelDialog() {
		return chooseLabelDialog;
	}

}