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

/*******************************************************************************
 * Copyright 2011 Philippe PRADOS 
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
 * Displays an EULA ("End User License Agreement") that the user has to accept before using the
 * application. Your application should call {@link Eula#showEula(android.app.Activity)} in the
 * onCreate() method of the first activity. If the user accepts the EULA, it will never be shown
 * again. If the user refuses, {@link android.app.Activity#finish()} is invoked on your activity.
 */
public class Eula
{
	private static String TAG="Eula";
	private static final String	PREFERENCE_EULA_ACCEPTED	= "eula.accepted";
	private static final String	PREFERENCES_EULA			= "eula";

	/**
	 * Displays the EULA if necessary. This method should be called from the onCreate() method of
	 * your main Activity.
	 * 
	 * @param activity
	 *            The Activity to finish if the user rejects the EULA.
	 */
	public static void showEula(final Activity activity,int id)
	{
		final SharedPreferences preferences = activity.getSharedPreferences(PREFERENCES_EULA,Activity.MODE_PRIVATE);
		if (!preferences.getBoolean(PREFERENCE_EULA_ACCEPTED, false))
		{
			final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
			builder.setTitle(R.string.eula_title);
			builder.setCancelable(true);
			builder.setPositiveButton(R.string.eula_accept, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
					preferences.edit().putBoolean(PREFERENCE_EULA_ACCEPTED, true).commit();
				}
			});
			builder.setNegativeButton(R.string.eula_refuse, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
					activity.finish();
				}
			});
			builder.setOnCancelListener(new DialogInterface.OnCancelListener()
			{
				public void onCancel(DialogInterface dialog)
				{
					activity.finish();
				}
			});
			builder.setMessage(readFile(activity, id));
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
