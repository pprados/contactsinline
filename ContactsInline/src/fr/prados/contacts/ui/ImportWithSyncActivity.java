/*******************************************************************************
 * Copyright 2012 Philippe PRADOS 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package fr.prados.contacts.ui;

import static fr.prados.contacts.Constants.EMULATOR;
import static fr.prados.contacts.Constants.*;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import fr.prados.contacts.lib.R;
import fr.prados.contacts.tools.HelpDebug;
import fr.prados.contacts.tools.LogMarket;
/**
 * Import inline contact in classical contacts and show it.
 * Use a transparent activity.
 * 
 * @version 1.0
 * @since 1.0
 * @author Philippe PRADOS
 */
public final class ImportWithSyncActivity extends Activity
{
	private static final String TAG="Import";
	
	private static final int DIALOG_IMPORT=1;
	
	private static volatile ImportTask _importTask;
	
	private final class ImportTask extends AsyncTask<Void, Void, Uri>
	{
		private ContentResolver _resolver;
		private final Uri _data;
		
		ImportTask(Uri data,ContentResolver resolver)
		{
			_data=data;
			_resolver=resolver;
		}

		@Override
		protected Uri doInBackground(Void... params)
		{
			assert(_data!=null);
			try
			{
				ProvidersManager.init();
				return ProvidersManager.fixeSyncContactInAndroid(getResources(),_resolver, _data);
			}
			catch (IllegalArgumentException e)
			{
				if (E) Log.e(TAG,"Error with contacts",e);
				return null;
			}
		}
		@Override
		protected void onPostExecute(Uri result)
		{
			removeDialog(DIALOG_IMPORT);
			finish();
			if (result!=null)
			{
				final Intent intent=new Intent(Intent.ACTION_VIEW,result);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
			_resolver=null;
			_importTask=null;
		}
	};
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if (V) Log.v(TAG,"LIFE onCreate");
		if (EMULATOR) HelpDebug.strictDeath();

		if (_importTask!=null)
		{
			_importTask.cancel(true);
		}
		_importTask=new ImportTask(getIntent().getData(),getContentResolver());
		_importTask.execute();
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		if (V) Log.v(TAG,"LIFE onResume");
		showDialog(DIALOG_IMPORT);
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		removeDialog(DIALOG_IMPORT);
	}

	@Override
	public Dialog onCreateDialog(int id)
	{
		switch (id)
		{
			case DIALOG_IMPORT:
				return ProgressDialog.show(this, getString(R.string.importing_title),getString(R.string.importing_wait), true,true);
			default:
				LogMarket.wtf(TAG, "Invalide dialog id "+id);
				return null;
		}
	}
	
}
