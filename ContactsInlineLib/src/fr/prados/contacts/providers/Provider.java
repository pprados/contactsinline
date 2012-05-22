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
package fr.prados.contacts.providers;

import static fr.prados.contacts.Constants.D;
import android.Manifest;
import android.content.pm.PackageManager;
import android.util.Log;
import fr.prados.contacts.Application;
import fr.prados.contacts.VolatileContact;
import fr.prados.contacts.lib.R;

public abstract class Provider extends IProvider.Stub
{
	protected static final String TAG=Provider.class.getSimpleName();
	
	public static final String JOIN_PHONE_SUFFIX="join_phone";
	public static final String JOIN_POSTAL_SUFFIX="join_postal";
	
	public interface OnQuery
	{
		void onQueryComplete(ResultsAndExceptions result,boolean finish);
	}
	/** Query for all possible items. */
	public static final String QUERY_MODE_ALL="*=?";
	/** Query for all possible items with phone number. */
	public static final String QUERY_MODE_ALL_WITH_PHONE="hasPhone=true && *=?";
	/** Query for all possible items with phone number. */
	public static final String QUERY_MODE_ALL_WITH_ADDRESS="hasAddress=true && *=?";
	/** Ask only e-mail. */
	public static final String QUERY_MODE_MAILTO="mail=?";
	/** Ask only phone number. */
	public static final String QUERY_MODE_TEL="tel=?";
	/** Ask only name. */
	public static final String QUERY_MODE_NAME="name=?";
	
	volatile private boolean _isCanceled;
	
	public void signalCanceled(String accountName)
	{
		_isCanceled=true;
	}
	final public void resetCanceled()
	{
		_isCanceled=false;
	}
	final public boolean isCanceled()
	{
		return _isCanceled;
	}

	public static void checkPermission() throws SecurityException
	{
		if (!"fr.prados.contacts".equals(Application.context.getApplicationInfo().processName)
		    && (Application.context.checkCallingPermission(Manifest.permission.READ_CONTACTS)==PackageManager.PERMISSION_DENIED)
		   )
				throw new SecurityException(Manifest.permission.READ_CONTACTS);
	}
	
	@Override
	public void onCreate(int version,long i) throws SecurityException
	{
		checkPermission();
		if (version!=1)
		{
			throw new Error(Application.context.getString(R.string.err_invalide_version));
		}
		VolatileContact._idgenerator=i;
	}

	@Override
	public void onStart() throws SecurityException
	{
		checkPermission();
		if (D) Log.d(TAG,"onStart()");
	}
	@Override
	public void onStop() throws SecurityException
	{
		checkPermission();
		if (D) Log.d(TAG,"onStop()");
	}
	
}