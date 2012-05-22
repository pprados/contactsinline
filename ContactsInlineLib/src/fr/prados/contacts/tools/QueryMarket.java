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

import static fr.prados.contacts.Constants.V;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;
import fr.prados.contacts.lib.R;

public class QueryMarket
{
	private static final String TAG="QueryMarket";
	public static void startSearchMarket(Context context,String q,int msg,boolean pack)
	{
		Intent intent=searchMarket(context,q,pack);
		if (intent!=null)
		{
			context.startActivity(intent);
		}
		else
		{
			final AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle(R.string.market_title);
			builder.setCancelable(true);
			builder.setPositiveButton(R.string.market_ok, null);
			builder.setMessage(msg);
			builder.create().show();
		}
	}
	public static final String	PREFERENCE_STARED			= "rate.done";
	public static final String	PREFERENCES_TIME			= "rate.time";
	public static final String	PREFERENCES_COUNT			= "rate.count";
	public static final String	PREFERENCES_STARS			= "rate";
	private static final long COUNT_PENDING=10;
	private static final long DAY_PENDING=10;
	private static final long EXTRA_DAY_PENDING=3;
	private static boolean _askRate=false;
	
	public static void extendDayPending(Context context)
	{
		final SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_STARS,Activity.MODE_PRIVATE);
		preferences.edit().putLong(QueryMarket.PREFERENCES_TIME, System.currentTimeMillis()+EXTRA_DAY_PENDING*86400L).commit();
	}
	
	public static void checkRate(Context context)
	{
		if (!_askRate)
		{
			final SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_STARS,Activity.MODE_PRIVATE);
			if (!preferences.getBoolean(PREFERENCE_STARED, false))
			{
				long time;
				if ((time=preferences.getLong(PREFERENCES_TIME, -1))==-1)
				{
					preferences.edit().putLong(PREFERENCES_TIME, System.currentTimeMillis()+DAY_PENDING*86400L).commit();
				}
				else
				{
					int count=preferences.getInt(PREFERENCES_COUNT, 0);
					if (time<System.currentTimeMillis())
					{
						if (count>COUNT_PENDING)
						{
							preferences.edit().putInt(PREFERENCES_COUNT, 0).commit();
							context.startActivity(new Intent(context,RateActivity.class));
							_askRate=true;
							return;
						}
					}
					preferences.edit().putInt(PREFERENCES_COUNT, ++count).commit();
				}
			}
		}
	}
	public static Intent searchMarket(Context context,String q,boolean pack)
	{
		q="market://"+(pack? "details?id=" :"search?q=")+q;
		Uri uri=Uri.parse(q);
		if (V) Log.v(TAG,uri.toString());
		Intent intent=new Intent(Intent.ACTION_VIEW,uri);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		// Try google market
		List<ResolveInfo> resolve=context.getPackageManager().queryIntentActivities(intent, 0);
		if (resolve.size()!=0)
		{
			return intent;
		}
		else
		{
			// Try Amazon market
			// http://www.amazonappstoredev.com/2011/02/managing-app-dependencies.html
			uri=Uri.parse("http://www.amazon.com/gp/mas/dl/android?"+(pack ? "p=":"s=")+q);
			intent = new Intent(Intent.ACTION_VIEW,uri);
			// Try amazon market
			resolve=context.getPackageManager().queryIntentActivities(intent, 0);
			for (ResolveInfo info:resolve)
			{
				if (info.activityInfo.applicationInfo.packageName.contains("amazon"))
				{
					return intent;
				}
			}
			return null;
		}
	}
}
