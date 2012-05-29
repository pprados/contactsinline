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
package fr.prados.contacts;

import static fr.prados.contacts.Constants.*;
import static fr.prados.contacts.Constants.TAG;
import static fr.prados.contacts.Constants.V;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Debug;
import android.util.Log;
import fr.prados.contacts.providers.AbstractSimpleAuthenticator;
import fr.prados.contacts.tools.LogMarket;

@ReportsCrashes(formKey="dHJxV0tJdE9jQ2h0d29KX0ZPbGR5WXc6MQ")
public class Application extends android.app.Application
{
	public static Context context;
	public static ExecutorService _executor=Executors.newCachedThreadPool();
	public static String sPackageName;

	public static int VERSION;

	@Override
	public void onCreate()
	{
		try
		{
			// True if Emulator
			EMULATOR=(android.os.Build.PRODUCT.indexOf("sdk")!=-1) || android.os.Build.PRODUCT.equals("full_x86");
			if (V) Log.v("LIFE","Application onCreate");
			context=getApplicationContext();
			PackageManager pm=context.getPackageManager();
			PackageInfo info=pm.getPackageInfo(context.getPackageName(), 0);
			VERSION=info.versionCode;
			if (EMULATOR && D) Log.d("MEM",""+Debug.getGlobalAllocSize());
			if (!DEBUG) ACRA.init(this);
			AbstractSimpleAuthenticator._accountManager = AccountManager.get(this);
		}
		catch (NameNotFoundException e) // $codepro.audit.disable logExceptions
		{
			LogMarket.wtf(TAG, "impossible to find version");
			VERSION=1;
		}
	}
	
	@Override
	public void onTerminate()
	{
		if (D) Log.d("LIFE","Application onTerminate");
		context=null;
	}
}
