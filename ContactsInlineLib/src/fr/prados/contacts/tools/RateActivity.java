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
package fr.prados.contacts.tools;

import static fr.prados.contacts.Constants.E;
import static fr.prados.contacts.Constants.EMULATOR;
import static fr.prados.contacts.Constants.V;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import fr.prados.contacts.lib.R;
/**
 * Star activity.
 * Use a transparent activity.
 * 
 * @version 1.0
 * @since 1.0
 * @author Philippe PRADOS
 */
public final class RateActivity extends Activity
{
	private static final String TAG="Star";
	
	private static final int DIALOG_STAR=1;
	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		if (V) Log.v(TAG,"LIFE onCreate");
		if (EMULATOR) HelpDebug.strictDeath();
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		if (V) Log.v(TAG,"LIFE onResume");
		showDialog(DIALOG_STAR);
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		removeDialog(DIALOG_STAR);
	}

	@Override
	public Dialog onCreateDialog(int id)
	{
		switch (id)
		{
			case DIALOG_STAR:
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
        	   final SharedPreferences preferences = 
        		   getSharedPreferences(QueryMarket.PREFERENCES_STARS,Activity.MODE_PRIVATE);
				builder
					   .setTitle(R.string.market_rate)
					   .setMessage(R.string.market_rate_body)
				       .setCancelable(true)
				       .setPositiveButton(R.string.market_rate_done, 
				    	   new DialogInterface.OnClickListener() 
					       {
					           public void onClick(DialogInterface dialog, int id) 
					           {
					        	   preferences.edit().putBoolean(QueryMarket.PREFERENCE_STARED, true).commit();
				           		   QueryMarket.startSearchMarket(RateActivity.this, "fr.prados.contacts",R.string.market_rate_no_market,true);
					        	   finish();
					           }
					       })
					   .setNeutralButton(R.string.market_rate_no, 
					    	new DialogInterface.OnClickListener() 
				       		{
				           		public void onClick(DialogInterface dialog, int id) 
				           		{
						        	preferences.edit().putBoolean(QueryMarket.PREFERENCE_STARED, true).commit();
				           			dialog.cancel();
				           			finish();
				           		}
				       		}
							)
				       .setNegativeButton(R.string.market_rate_later, 
				    		new DialogInterface.OnClickListener() 
				       		{
				           		public void onClick(DialogInterface dialog, int id) 
				           		{
				           			QueryMarket.extendDayPending(RateActivity.this);
				           			dialog.cancel();
				           			finish();
				           		}
				       });
				return builder.create();
			default:
				LogMarket.wtf(TAG, "Invalide dialog id "+id);
				return null;
		}
	}
	
}
