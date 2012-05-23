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

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Build;
import fr.prados.contacts.lib.R;



public class CheckContext
{
	public static boolean isContactInline(final Activity context)
	{
	    Intent intent=new Intent(Intent.ACTION_MAIN);
	    intent.setClassName("fr.prados.contacts", "fr.prados.contacts.ui.VolatileContactsListActivity");
	    List<ResolveInfo> resolve=context.getPackageManager().queryIntentActivities(intent, 0);
	    if (resolve.size()==0)
	    {
			final AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle(R.string.chkcontact_title);
			builder.setCancelable(false);
			builder.setPositiveButton(R.string.chkcontact_accept, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
			    	QueryMarket.startSearchMarket(context,
		    			"fr.prados.contacts",R.string.market_body,true);
					context.finish();
				}
			});
			builder.setMessage(R.string.chkcontact_msg);
			builder.create().show();
			return false;
	    }		
	    return true;
	}
	public static boolean isAndroidMinVersion(final Activity context,int minver)
	{
		if (Build.VERSION.SDK_INT>=minver)
			return true;

    	final AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.help_need_android_version_title);
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				context.finish();
			}
		});
		builder.setMessage(R.string.help_need_android_version);
		builder.create().show();
		return false;
	}

}
