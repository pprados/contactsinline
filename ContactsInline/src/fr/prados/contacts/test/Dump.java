// $codepro.audit.disable caughtExceptions, largeNumberOfParameters
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
package fr.prados.contacts.test;

import static fr.prados.contacts.Constants.D;
import static fr.prados.contacts.Constants.DUMP;
import static fr.prados.contacts.Constants.E;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

public class Dump
{
	@SuppressWarnings("unused")
	public static void dumpTable(
			String tag,
			ContentResolver contentResolver,
			Uri uri,
			String[] projection)
	{
		if (!DUMP)
			return;
		dumpTable(tag,contentResolver,uri,projection,null,null);
	}
	@SuppressWarnings("unused")
	public static void dumpTable(
			String tag,
			ContentResolver contentResolver,
			Uri uri,
			String[] projection,
			String selection,
			String[] selectionsArgs)
	{
		try
		{
			if (!DUMP) return; 
			Log.v(tag,"***** Dump "+uri.toString());
			final Cursor cursor = contentResolver.query(uri,projection,selection,selectionsArgs,null);
			StringBuilder builder=new StringBuilder(200);
			while (cursor.moveToNext())
			{
				builder.setLength(0);
				int i=0;
				for (String s:projection)
				{
					builder.append(s).append('=').append(cursor.getString(i++)).append(' ');
				}
				Log.v(tag,builder.toString());
				
			}
			cursor.close();
		}
		catch (Throwable e)
		{
			if (D) Log.v(tag,"Dump error "+e.getMessage());
			if (E) e.printStackTrace();
		}
		
	}

	public static CharSequence dumpCursor(Cursor cursor)
	{
		if (cursor.isLast()) 
			return "";
		final StringBuilder builder=new StringBuilder(200);
		final int max=cursor.getColumnCount();
		for (int i=0;i<max;++i)
		{
			builder.append(' ').append(cursor.getString(i));
		}
		return builder;
	}

	@SuppressWarnings("unused")
	public static void dump_android(String tag,boolean deleted,ContentResolver contentResolver)
	{
		if (!DUMP)
			return;
		dump(tag,deleted,contentResolver,
			ContactsContract.Contacts.CONTENT_URI,
			ContactsContract.RawContacts.CONTENT_URI,
			ContactsContract.Data.CONTENT_URI);
	}
	@SuppressWarnings("unused")
	public static void dump_android_lite(String tag,boolean deleted,ContentResolver contentResolver)
	{
		if (!DUMP)
			return;
		dump(tag,deleted,contentResolver,
			ContactsContract.Contacts.CONTENT_URI,
			ContactsContract.RawContacts.CONTENT_URI);
	}
	@SuppressWarnings("unused")
	public static void dump(
			String tag,
			boolean deleted,
			ContentResolver contentResolver,
			Uri ...uris)
	{
		if (!DUMP)
			return;
		Log.v(tag,"-------------------");
		// Unit test.
		/*
		Cursor cursor;
		Uri rawContactUri = rawcontactsUri.buildUpon()
			.appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type)
			.build();
		cursor = contentResolver.query(rawContactUri,
			new String[] 
			  { 
				RawContacts._ID, 			//Id de la ligne
				RawContacts.CONTACT_ID, 	// Id du contact en référence
				RawContacts.DELETED,		// Flag si deleted
				RawContacts.ACCOUNT_NAME,
				RawContacts.ACCOUNT_TYPE,
			  },
				RawContacts.DELETED + "=0",
				null, null);
		while (cursor.moveToNext())
		{
			if (cursor.getString(3)!=null)
			{
				Log.v(tag,"Il reste  : _id="+cursor.getInt(0)+"->[_contactid="+cursor.getInt(1)+"]  deleted="+cursor.getString(2) + " accountname="+cursor.getString(3)+" accountype="+cursor.getString(4));
			}
		}
		cursor.close();
		*/
		for (Uri uri:uris)
		{
			if (uri.toString().startsWith(ContactsContract.RawContacts.CONTENT_URI.toString()))
			{
				Dump.dumpTable(tag,contentResolver,
					uri,
					new String[] 
				    { 
		  				RawContacts._ID,
		  				RawContacts.CONTACT_ID,
		  				RawContacts.ACCOUNT_NAME,
//		  				RawContacts.ACCOUNT_TYPE,
//		  				RawContacts.SYNC1,
		  				RawContacts.SYNC4,
		  				RawContacts.AGGREGATION_MODE,
		  				RawContacts.DELETED,
		  				RawContacts.DIRTY,
		  				RawContacts.SOURCE_ID,
		  				RawContacts.STARRED,
		  				RawContacts.VERSION,
					},
					(!deleted) ? RawContacts.DELETED + "=0" : null,null);
			}
			if (uri.toString().startsWith(ContactsContract.Data.CONTENT_URI.toString()))
			{
				Dump.dumpTable(tag,contentResolver,
					uri,
					new String[] 
				    { 
						ContactsContract.Data._ID,
//						ContactsContract.Data.CONTACT_ID,
						ContactsContract.Data.RAW_CONTACT_ID,
						ContactsContract.Data.DATA1,
						ContactsContract.Data.DATA2,
						ContactsContract.Data.MIMETYPE,
						ContactsContract.Data.DISPLAY_NAME, 
//						ContactsContract.Data.IN_VISIBLE_GROUP,
//						ContactsContract.Data.CONTACT_PRESENCE,
//						ContactsContract.Data.CONTACT_STATUS,
					},
					null,//(!deleted) ? RawContacts.DELETED + "=0" : null,
					null);
			}
			if (uri.toString().startsWith(ContactsContract.Contacts.CONTENT_URI.toString()))
			{
				Dump.dumpTable(tag,contentResolver,
					uri,
					new String[] 
					{ 
						Contacts._ID,
						Contacts.DISPLAY_NAME, 
						Contacts.LOOKUP_KEY, 
						Contacts.STARRED,
					});
			}
			
		}
		Log.v(tag,"-------------------");
	}
}
