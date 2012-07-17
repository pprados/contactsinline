package fr.prados.contacts.ui;

/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

public class AbsListActivity extends Activity
{
	protected ListAdapter	mAdapter;

	protected AbsListView	mList;

	private final Handler			mHandler		= new Handler();

	private boolean			mFinishedStart	= false;

	private final Runnable		mRequestFocus	= new Runnable()
											{
												@Override
												public void run()
												{
													mList.focusableViewAvailable(mList);
												}
											};

	protected void onListItemClick(ListView l, View v, int position, long id)
	{
	}

	@Override
	protected void onRestoreInstanceState(Bundle state)
	{
		ensureList();
		super.onRestoreInstanceState(state);
	}

	@Override
	protected void onDestroy()
	{
		mHandler.removeCallbacks(mRequestFocus);
		super.onDestroy();
	}

	@Override
	public void onContentChanged()
	{
		super.onContentChanged();
		View emptyView = findViewById(android.R.id.empty);
		mList = (AbsListView) findViewById(android.R.id.list);
		if (mList == null)
		{
			throw new RuntimeException("Your content must have a ListView whose id attribute is "
					+ "'android.R.id.list'");
		}
		if (emptyView != null)
		{
			mList.setEmptyView(emptyView);
		}
		mList.setOnItemClickListener(mOnClickListener);
		if (mFinishedStart)
		{
			setListAdapter(mAdapter);
		}
		mHandler.post(mRequestFocus);
		mFinishedStart = true;
	}

	@TargetApi(11)
	public void setListAdapter(ListAdapter adapter)
	{
		synchronized (this)
		{
			ensureList();
			mAdapter = adapter;
			if (mList instanceof ListView)
			{
				((ListView) mList).setAdapter(adapter);
			}
			else
				mList.setAdapter(adapter);
		}
	}

	public void setSelection(int position)
	{
		mList.setSelection(position);
	}

	public int getSelectedItemPosition()
	{
		return mList.getSelectedItemPosition();
	}

	public long getSelectedItemId()
	{
		return mList.getSelectedItemId();
	}

	public AbsListView getListView()
	{
		ensureList();
		return mList;
	}

	public ListAdapter getListAdapter()
	{
		return mAdapter;
	}

	private void ensureList()
	{
		if (mList != null)
		{
			return;
		}
		setContentView(android.R.layout.list_content);

	}

	private final AdapterView.OnItemClickListener	mOnClickListener	= new AdapterView.OnItemClickListener()
																{
																	@Override
																	public void onItemClick(
																			AdapterView<?> parent,
																			View v, int position,
																			long id)
																	{
																		onListItemClick(
																				(ListView) parent,
																				v, position, id);
																	}
																};
}
