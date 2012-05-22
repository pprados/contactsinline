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

import static fr.prados.contacts.Constants.W;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.util.Log;
import fr.prados.contacts.lib.R;

/**
 * Displays an last Update information.
 */
public class Update
{
	private static final String TAG="Update";
	
	private static final String	PREFERENCE_UPDATE_VERSION	= "update.show";
	private static final String	PREFERENCES_UPDATE			= "update";

	/**
	 * Displays the EULA if necessary. This method should be called from the onCreate() method of
	 * your main Activity.
	 * 
	 * @param activity
	 *            The Activity to finish if the user rejects the EULA.
	 */
	public static void showUdpate(final Activity activity,final int version,int update)
	{
		final SharedPreferences preferences = activity.getSharedPreferences(PREFERENCES_UPDATE,Activity.MODE_PRIVATE);
		int knowver=preferences.getInt(PREFERENCE_UPDATE_VERSION, 0);
		if (knowver==0) 
		{
			preferences.edit().putInt(PREFERENCE_UPDATE_VERSION, version).commit();
			return;
		}
		if (knowver<version)
		{
			// FIXME: Reset rate
			if (knowver==10)
			{
				activity.getSharedPreferences(QueryMarket.PREFERENCES_STARS,Activity.MODE_PRIVATE)
        	   		.edit().putBoolean(QueryMarket.PREFERENCE_STARED, false).commit();
			}
			final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			builder.setTitle(R.string.update_title);
			builder.setCancelable(true);
			builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
					preferences.edit().putInt(PREFERENCE_UPDATE_VERSION, version).commit();
				}
			});
			builder.setMessage(readFile(activity, update));
			builder.create().show();
		}
	}

	private static CharSequence readFile(Activity activity, int id)
	{
		BufferedReader in = null;
		try
		{
			in = new BufferedReader(new InputStreamReader(activity.getResources().openRawResource(id)));
			String line;
			StringBuilder buffer = new StringBuilder();
			while ((line = in.readLine()) != null)
				buffer.append(line).append('\n');
			return buffer;
		}
		catch (IOException e)
		{
			if (W) Log.w(TAG,"readFile",e);
			return "";
		}
		finally
		{
			try
			{
				in.close();
			}
			catch (IOException e)
			{
				// Ignore
				if (W) Log.w(TAG,"readFile",e);
			}
		}
	}

}
