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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.StrictMode;

public class HelpDebug
{
	interface Debug
	{
		void strictDefault();
		void strictDeath();
	}
	private static Debug _debug;
	static
	{
		// Pour version 2.2
		if (Build.VERSION.SDK_INT>=9)
		{
			_debug=new Debug()
			{

				@TargetApi(9)
				@Override
				public void strictDefault()
				{
	         		StrictMode.enableDefaults();
				}
				@SuppressLint("NewApi")
				@Override
				public void strictDeath()
				{
					StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
						.detectAll()
						.penaltyLog()
						.penaltyDeath()
						.build());
					StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
						.detectLeakedSqlLiteObjects()
						.penaltyLog()
						.penaltyDeath()
						.build());
				}
			};
		}
		else
		{
			_debug=new Debug()
			{

				@Override
				public void strictDefault()
				{
					// Nothing
				}

				@Override
				public void strictDeath()
				{
					// Nothing
				}
				
			};
		}
	}
	public static void strictDefaults()
	{
		_debug.strictDefault();
	}
	public static void strictDeath()
	{
		_debug.strictDeath();
	}
}
