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

package fr.prados.contacts.providers.mock;

import static fr.prados.contacts.Constants.D;
import static fr.prados.contacts.Constants.I;
import static fr.prados.contacts.Constants.W;

import java.util.ArrayList;

import android.accounts.Account;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import fr.prados.contacts.VolatileRawContact;

public class MockSyncAdapterService extends Service
{
	private static final String TAG = "MockSyncAdapter";
	private static SyncAdapterImpl _syncAdapter = null;

	private static class SyncAdapterImpl extends AbstractThreadedSyncAdapter
	{
		private final ContentResolver _resolver;
		public SyncAdapterImpl(Context context)
		{
			super(context, true);
			_resolver=context.getContentResolver();
		}

		private static final String[] COLS_RAW=new String[]{RawContacts._ID,VolatileRawContact.LOOKUP};
		/*
		 * {@inheritDoc}
		 */
		@SuppressWarnings("unused")
		@Override
		public void onPerformSync(Account account, Bundle extras,
				String authority, ContentProviderClient provider,
				SyncResult syncResult)
			throws SecurityException
		{
			if (D) Log.d(TAG,"onPerformSync(...");
			if (!Application.ACCOUNT_WITH_SYNC) return;
			final ArrayList<ContentProviderOperation> operationList=new ArrayList<ContentProviderOperation>(10);

			final MockProvider driver=MockProviderService._binder;
			Cursor cursor=null;
			try
			{
				syncResult.stats.numDeletes=_resolver.delete(RawContacts.CONTENT_URI, 
					VolatileRawContact.MUST_DELETED+"=1 and "+
					RawContacts.ACCOUNT_NAME+"="+DatabaseUtils.sqlEscapeString(account.name)+
					" and "+RawContacts.ACCOUNT_TYPE+"='"+account.type+"'"+
					" and "+RawContacts.DELETED+"=0", null);

				int numEntries=0;
				int numUpdates=0;
				cursor=_resolver.query(RawContacts.CONTENT_URI, 
						COLS_RAW,
					RawContacts.ACCOUNT_NAME+"="+DatabaseUtils.sqlEscapeString(account.name)+
					" and "+RawContacts.ACCOUNT_TYPE+"='"+account.type+"'"+
					" and "+RawContacts.DELETED+"=0"+
					" and "+VolatileRawContact.MUST_DELETED+"=0", 
					null,null);
				while (cursor.moveToNext())
				{
					final long id=cursor.getLong(0/*_ID*/);
					try
					{
						final String lookup=cursor.getString(1/*LOOKUP*/);
						if (I) Log.i(TAG,"Sync."+lookup);
						++numEntries;
						VolatileRawContact volatileContact=driver.getContact(account.name, lookup).getRawContact();
						volatileContact.updateInAndroid(_resolver, 
							ContentUris.withAppendedId(RawContacts.CONTENT_URI, id), operationList);
						++numUpdates;
					}
					catch (Exception e) // $codepro.audit.disable caughtExceptions
					{
						if (W) Log.w(TAG,"onPerformSync",e);
						operationList.add(
							ContentProviderOperation.newUpdate(
								ContentUris.withAppendedId(RawContacts.CONTENT_URI,id))
							.withValue(RawContacts.DIRTY, 1)
							.build());
						syncResult.fullSyncRequested=true;
						//++syncResult.stats.numAuthExceptions;
					}
				}
				_resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
				syncResult.stats.numEntries=numEntries;
				syncResult.stats.numUpdates=numUpdates;
			}
			catch (RemoteException e)
			{
				syncResult.databaseError=true;
				if (W) Log.w(TAG,"onPerformSync",e);
			}
			catch (OperationApplicationException e)
			{
				if (W) Log.w(TAG,"onPerformSync",e);
				syncResult.databaseError=true;
			}
			finally
			{
				if (cursor!=null)
					cursor.close();
			}
		}
		
		//@Override //For Android 2.2
		public void onSyncCanceled()
		{
			
		}
	}

	/*
	 * {@inheritDoc}
	 */
	@Override
	public IBinder onBind(Intent intent)
	{
		if (_syncAdapter == null)
			_syncAdapter = new SyncAdapterImpl(this);
		return _syncAdapter.getSyncAdapterBinder();
	}
}
