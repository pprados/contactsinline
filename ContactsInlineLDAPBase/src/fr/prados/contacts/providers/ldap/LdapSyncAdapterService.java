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
package fr.prados.contacts.providers.ldap;

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
import fr.prados.contacts.VolatileContact.Copy;
import fr.prados.contacts.VolatileContact.Import;
import fr.prados.contacts.VolatileRawContact;
import fr.prados.contacts.providers.AuthQueryException;
import fr.prados.contacts.providers.QueryException;
/**
 * @author Philippe Prados
 * 
 */
// * Faire un ajout d'un attribut a chaque contact lors de la synchronisation.
// * Déclarer la synchronisation par défaut lors de l'installation
// * L'attribut ou les attributs permettent d'avoir accès aux suggestions pour le contact,
//   avec import possible dans ce dernier pour les types de bases.
// * Un autre type est porté par une classe pour les données volatile, récupéré sur le net 
//   (status, twitte, blog, etc.)

public class LdapSyncAdapterService extends Service
{
	private static final String TAG = "LDAPSyncAdapter";
	private static SyncAdapterImpl _syncAdapter = null;

	/*
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate()
	{
	}

	/**
	 * 
	 * @version 1.0
	 * @since 1.0
	 * @author Philippe PRADOS
	 */
	private static class SyncAdapterImpl extends AbstractThreadedSyncAdapter
	{
		ContentResolver _resolver;
		public SyncAdapterImpl(Context context)
		{
			super(context, true);
			_resolver=context.getContentResolver();
		}

		private static final String[] COLS_RAW=new String[]{RawContacts._ID,VolatileRawContact.LOOKUP};
		/*
		 * {@inheritDoc}
		 */
		@Override
		public void onPerformSync(Account account, Bundle extras,
				String authority, ContentProviderClient provider,
				SyncResult syncResult)
			throws SecurityException
		{
			if (D) Log.d(TAG,"onPerformSync(...");
			ArrayList<ContentProviderOperation> operationList=new ArrayList<ContentProviderOperation>(10);

			final LdapProvider driver=LdapProviderService._binder;
			Cursor cursor=null;
			try
			{
				if (I) Log.i(TAG,"delete records...");
				syncResult.stats.numDeletes=_resolver.delete(RawContacts.CONTENT_URI, 
					VolatileRawContact.MUST_DELETED+"=1 and "+
					RawContacts.ACCOUNT_NAME+"="+DatabaseUtils.sqlEscapeString(account.name)+
					" and "+RawContacts.ACCOUNT_TYPE+"='"+account.type+"'"+
					" and "+RawContacts.DELETED+"=0", null);
				if (I) Log.i(TAG,"Records deleted.");

				int numEntries=0;
				int numUpdates=0;
				if (I) Log.i(TAG,"Manage impoted records...");
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
						volatileContact._datas.remove(Import.CONTENT_ITEM_TYPE);
						volatileContact._datas.remove(Copy.CONTENT_ITEM_TYPE);
						volatileContact.updateInAndroid(_resolver, 
							ContentUris.withAppendedId(RawContacts.CONTENT_URI, id), operationList);
						++numUpdates;
					}
					catch (AuthQueryException e)
					{
						if (W) Log.w(TAG,"onPerformSync",e);
						operationList.add(
							ContentProviderOperation.newUpdate(
								ContentUris.withAppendedId(RawContacts.CONTENT_URI,id))
							.withValue(RawContacts.DIRTY, 1)
							.build());
						syncResult.fullSyncRequested=true;
						++syncResult.stats.numAuthExceptions;
					}
					catch (QueryException e)
					{
						if (W) Log.w(TAG,"onPerformSync",e);
						operationList.add(
							ContentProviderOperation.newUpdate(
								ContentUris.withAppendedId(RawContacts.CONTENT_URI,id))
							.withValue(RawContacts.DIRTY, 1)
							.build());
						syncResult.fullSyncRequested=true;
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
			catch (Exception e)
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
