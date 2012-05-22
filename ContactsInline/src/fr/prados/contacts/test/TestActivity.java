// $codepro.audit.disable caughtExceptions
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
import static fr.prados.contacts.Constants.E;
import static fr.prados.contacts.Constants.V;

import java.lang.reflect.Method;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Relation;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Intents.Insert;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
public class TestActivity extends ListActivity
{
	private static final String TAG="TestActivity";
//	class MyAdapter extends ArrayAdapter<Method>
//	{
//		MyAdapter()
//		{
//			super(TestActivity.this, android.R.layout.simple_list_item_1,_methods);
//		}
//		
//
//	};

	private Method[] _methods;
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		final Method[] methods=getClass().getMethods();
		final Method[] result=new Method[methods.length];

		int i=0;
		for (Method method:methods)
		{
			if (method.getName().startsWith("cmd"))
			{
				result[i++]=method;
			}
		}
		_methods=new Method[i];
		System.arraycopy(result, 0, _methods, 0, i);

		class Wrapper
		{
			private Method method;
			Wrapper(Method m)
			{
				method=m;
			}
			public String toString()
			{
				return method.getName().substring(3);
			}
		}
		final ArrayAdapter<Wrapper> adapter=new ArrayAdapter<Wrapper>(this, android.R.layout.simple_list_item_1);
		for (Method method:_methods)
		{
			adapter.add(new Wrapper(method));
		}
		setListAdapter(adapter);
	}
	

	@Override
	protected void onListItemClick (ListView l, View v, int position, long id)
	{
		try
		{
			final Method method=_methods[position];
			if (method!=null)
			{
				method.invoke(this, new Object[]{});
			}
		}
		catch (Exception e)
		{
			if (E) e.printStackTrace();
		}
	}
	
//	@Override
//    public boolean onCreateOptionsMenu(Menu menu) 
//	{
//		int i=0;
//		for (Method method:_methods)
//		{
//			menu.add(0,i++,0,method.getName().substring(3));
//		}
//        return super.onCreateOptionsMenu(menu);
//    }
//	@Override
//	public boolean onOptionsItemSelected(MenuItem item)
//	{
//		try
//		{
//			Method method=_methods[item.getItemId()];
//			if (method!=null)
//			{
//				method.invoke(this, new Object[]{});
//				return true;
//			}
//		}
//		catch (Exception e)
//		{
//			if (E) e.printStackTrace();
//		}
//		return super.onOptionsItemSelected(item);
//	}
//	public void cmdViewContact()
//	{
//		startActivity(new Intent(Intent.ACTION_VIEW,ContactsContract.Contacts.CONTENT_URI));
//	}
//	public void cmdContactId()
//	{
//		startActivity(new Intent(Intent.ACTION_VIEW,
//			ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI,1)));
//	}
//	public void cmdContactFromGroup()
//	{
//		String group="default";
//		Intent intent=new Intent("com.android.contacts.action.LIST_GROUP");
//		intent.setComponent(new ComponentName("com.android.contacts", "com.android.contacts.ContactsListActivity"));
//		intent.putExtra("com.android.contacts.extra.GROUP", group);
//		startActivity(intent);
//	}
//	public void cmdlistAllContacts()
//	{
//		Intent intent=new Intent("com.android.contacts.action.LIST_ALL_CONTACTS");
//		intent.setComponent(new ComponentName("com.android.contacts", "com.android.contacts.ContactsListActivity"));
//		startActivity(intent);
//	}
//	public void cmdlistStared()
//	{
//		Intent intent=new Intent("com.android.contacts.action.LIST_STARRED");
//		intent.setComponent(new ComponentName("com.android.contacts", "com.android.contacts.ContactsListActivity"));
//		startActivity(intent);
//	}
//	public void cmdlistFrequent()
//	{
//		Intent intent=new Intent("com.android.contacts.action.LIST_STREQUENT");
//		intent.setComponent(new ComponentName("com.android.contacts", "com.android.contacts.ContactsListActivity"));
//		startActivity(intent);
//	}

//	public void cmdSearchSuggestion()
//	{
//		startActivity(new Intent(ContactsContract.Intents.SEARCH_SUGGESTION_CLICKED,ContactsContract.Contacts.CONTENT_URI));
//	}
//	public void cmdSearchSuggestionCreate()
//	{
//		startActivity(new Intent(ContactsContract.Intents.SEARCH_SUGGESTION_CREATE_CONTACT_CLICKED,ContactsContract.Contacts.CONTENT_URI));
//	}
//	public void cmdSearchSuggestionDial()
//	{
//		startActivity(new Intent(ContactsContract.Intents.SEARCH_SUGGESTION_DIAL_NUMBER_CLICKED,ContactsContract.Contacts.CONTENT_URI));
//	}


	private static final int NOTHING=0;
	private static final int LEGACY_CONTACT=1;
	private static final int CONTACT=2;
	private static final int DATA=3;

	// ------------------ Command List
	void Assert(boolean t)
	{
		assert(t);
	}
	public void cmd_tools_injectMaxContact()
	{
		try
		{
			final ArrayList<ContentProviderOperation> operationList=new ArrayList<ContentProviderOperation>(5);
			final ContentValues valRaw=new ContentValues();
			valRaw.put(RawContacts.DELETED, 0);
			operationList.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
				.withValues(valRaw)
				.withValue(RawContacts.DIRTY, "1")
				.build());
			final int rawpos=operationList.size()-1;
			
			// Email
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Email.CONTENT_ITEM_TYPE)
				.withValue(Email.TYPE, Email.TYPE_CUSTOM)
				.withValue(Email.DISPLAY_NAME,"DN for email custom")
				.withValue(Email.LABEL, "label")
				.withValue(Email.DATA, "mail@server.org")
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Email.CONTENT_ITEM_TYPE)
				.withValue(Email.TYPE, Email.TYPE_HOME)
				.withValue(Email.DISPLAY_NAME,"DN for email home")
				.withValue(Email.DATA, "mail@server.org")
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Email.CONTENT_ITEM_TYPE)
				.withValue(Email.TYPE, Email.TYPE_MOBILE)
				.withValue(Email.DISPLAY_NAME,"DN for email mobile")
				.withValue(Email.DATA, "mail@server.org")
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Email.CONTENT_ITEM_TYPE)
				.withValue(Email.TYPE, Email.TYPE_OTHER)
				.withValue(Email.DISPLAY_NAME,"DN for email other")
				.withValue(Email.DATA, "mail@server.org")
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Email.CONTENT_ITEM_TYPE)
				.withValue(Email.TYPE, Email.TYPE_WORK)
				.withValue(Email.DISPLAY_NAME,"DN for email work")
				.withValue(Email.DATA, "mail@server.org")
				.build());

			// Event
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Event.CONTENT_ITEM_TYPE)
				.withValue(Event.START_DATE, "1/1/2010")
				.withValue(Event.TYPE, Event.TYPE_ANNIVERSARY)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Event.CONTENT_ITEM_TYPE)
				.withValue(Event.START_DATE, "2/1/2010")
				.withValue(Event.TYPE, Event.TYPE_BIRTHDAY)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Event.CONTENT_ITEM_TYPE)
				.withValue(Event.START_DATE, "3/1/2010")
				.withValue(Event.TYPE, Event.TYPE_OTHER)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Event.CONTENT_ITEM_TYPE)
				.withValue(Event.START_DATE, "4/1/2010")
				.withValue(Event.TYPE, Event.TYPE_CUSTOM)
				.withValue(Event.LABEL,"label")
				.build());
			
			// Group
//			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
//				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
//				.withValue(Data.MIMETYPE,GroupMembership.CONTENT_ITEM_TYPE)
//				.withValue(GroupMembership.GROUP_ROW_ID,  ) // TODO: create record with group
//				.build());

			// IM
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Im.CONTENT_ITEM_TYPE)
				.withValue(Im.DATA,"home@jabber.org")
				.withValue(Im.TYPE,Im.TYPE_HOME)
				.withValue(Im.PROTOCOL, Im.PROTOCOL_JABBER)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Im.CONTENT_ITEM_TYPE)
				.withValue(Im.DATA,"other@jabber.org")
				.withValue(Im.TYPE,Im.TYPE_OTHER)
				.withValue(Im.PROTOCOL, Im.PROTOCOL_JABBER)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Im.CONTENT_ITEM_TYPE)
				.withValue(Im.DATA,"work@jabber.org")
				.withValue(Im.TYPE,Im.TYPE_WORK)
				.withValue(Im.PROTOCOL, Im.PROTOCOL_JABBER)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Im.CONTENT_ITEM_TYPE)
				.withValue(Im.DATA,"custom@jabber.org")
				.withValue(Im.TYPE,Im.TYPE_CUSTOM)
				.withValue(Im.LABEL, "label")
				.withValue(Im.PROTOCOL, Im.PROTOCOL_JABBER)
				.build());
			
			// Nickname
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Nickname.CONTENT_ITEM_TYPE)
				.withValue(Nickname.NAME,"nickname default")
				.withValue(Nickname.TYPE, Nickname.TYPE_DEFAULT)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Nickname.CONTENT_ITEM_TYPE)
				.withValue(Nickname.NAME,"nickname initials")
				.withValue(Nickname.TYPE, Nickname.TYPE_INITIALS)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Nickname.CONTENT_ITEM_TYPE)
				.withValue(Nickname.NAME,"nickname mainden")
				.withValue(Nickname.TYPE, Nickname.TYPE_MAINDEN_NAME)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Nickname.CONTENT_ITEM_TYPE)
				.withValue(Nickname.NAME,"nickname other")
				.withValue(Nickname.TYPE, Nickname.TYPE_OTHER_NAME)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Nickname.CONTENT_ITEM_TYPE)
				.withValue(Nickname.NAME,"nickname short")
				.withValue(Nickname.TYPE, Nickname.TYPE_SHORT_NAME)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Nickname.CONTENT_ITEM_TYPE)
				.withValue(Nickname.NAME,"nickname custom")
				.withValue(Nickname.TYPE, Nickname.TYPE_CUSTOM)
				.withValue(Nickname.LABEL,"label")
				.build());
			
			// Note
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Note.CONTENT_ITEM_TYPE)
				.withValue(Note.NOTE,"note")
				.build());
			
			// Organisation
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Organization.CONTENT_ITEM_TYPE)
				.withValue(Organization.COMPANY,"company other")
				.withValue(Organization.TYPE, Organization.TYPE_OTHER)
				.withValue(Organization.TITLE, "title")
				.withValue(Organization.DEPARTMENT, "departement")
				.withValue(Organization.JOB_DESCRIPTION,"job description")
				.withValue(Organization.SYMBOL, "symbol")
				.withValue(Organization.PHONETIC_NAME, "phon name")
				.withValue(Organization.OFFICE_LOCATION,"office location")
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Organization.CONTENT_ITEM_TYPE)
				.withValue(Organization.COMPANY,"company work")
				.withValue(Organization.TYPE, Organization.TYPE_WORK)
				.withValue(Organization.TITLE, "title")
				.withValue(Organization.DEPARTMENT, "departement")
				.withValue(Organization.JOB_DESCRIPTION,"job description")
				.withValue(Organization.SYMBOL, "symbol")
				.withValue(Organization.PHONETIC_NAME, "phon name")
				.withValue(Organization.OFFICE_LOCATION,"office location")
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Organization.CONTENT_ITEM_TYPE)
				.withValue(Organization.COMPANY,"company custom")
				.withValue(Organization.TYPE, Organization.TYPE_CUSTOM)
				.withValue(Organization.LABEL,"label")
				.withValue(Organization.TITLE, "title")
				.withValue(Organization.DEPARTMENT, "departement")
				.withValue(Organization.JOB_DESCRIPTION,"job description")
				.withValue(Organization.SYMBOL, "symbol")
				.withValue(Organization.PHONETIC_NAME, "phon name")
				.withValue(Organization.OFFICE_LOCATION,"office location")
				.build());
			
			// Phone
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Phone.CONTENT_ITEM_TYPE)
				.withValue(Phone.NUMBER,"0101010101")
				.withValue(Phone.TYPE, Phone.TYPE_ASSISTANT)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Phone.CONTENT_ITEM_TYPE)
				.withValue(Phone.NUMBER,"1101010101")
				.withValue(Phone.TYPE, Phone.TYPE_CALLBACK)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Phone.CONTENT_ITEM_TYPE)
				.withValue(Phone.NUMBER,"2101010101")
				.withValue(Phone.TYPE, Phone.TYPE_CAR)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Phone.CONTENT_ITEM_TYPE)
				.withValue(Phone.NUMBER,"3101010101")
				.withValue(Phone.TYPE, Phone.TYPE_COMPANY_MAIN)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Phone.CONTENT_ITEM_TYPE)
				.withValue(Phone.NUMBER,"4101010101")
				.withValue(Phone.TYPE, Phone.TYPE_FAX_HOME)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Phone.CONTENT_ITEM_TYPE)
				.withValue(Phone.NUMBER,"5101010101")
				.withValue(Phone.TYPE, Phone.TYPE_FAX_WORK)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Phone.CONTENT_ITEM_TYPE)
				.withValue(Phone.NUMBER,"6101010101")
				.withValue(Phone.TYPE, Phone.TYPE_HOME)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Phone.CONTENT_ITEM_TYPE)
				.withValue(Phone.NUMBER,"7101010101")
				.withValue(Phone.TYPE, Phone.TYPE_ISDN)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Phone.CONTENT_ITEM_TYPE)
				.withValue(Phone.NUMBER,"8101010101")
				.withValue(Phone.TYPE, Phone.TYPE_MAIN)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Phone.CONTENT_ITEM_TYPE)
				.withValue(Phone.NUMBER,"9101010101")
				.withValue(Phone.TYPE, Phone.TYPE_MMS)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Phone.CONTENT_ITEM_TYPE)
				.withValue(Phone.NUMBER,"1001010101")
				.withValue(Phone.TYPE, Phone.TYPE_MOBILE)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Phone.CONTENT_ITEM_TYPE)
				.withValue(Phone.NUMBER,"1101010101")
				.withValue(Phone.TYPE, Phone.TYPE_OTHER)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Phone.CONTENT_ITEM_TYPE)
				.withValue(Phone.NUMBER,"1201010101")
				.withValue(Phone.TYPE, Phone.TYPE_OTHER_FAX)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Phone.CONTENT_ITEM_TYPE)
				.withValue(Phone.NUMBER,"1301010101")
				.withValue(Phone.TYPE, Phone.TYPE_PAGER)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Phone.CONTENT_ITEM_TYPE)
				.withValue(Phone.NUMBER,"1401010101")
				.withValue(Phone.TYPE, Phone.TYPE_RADIO)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Phone.CONTENT_ITEM_TYPE)
				.withValue(Phone.NUMBER,"1501010101")
				.withValue(Phone.TYPE, Phone.TYPE_TELEX)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Phone.CONTENT_ITEM_TYPE)
				.withValue(Phone.NUMBER,"1601010101")
				.withValue(Phone.TYPE, Phone.TYPE_TTY_TDD)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Phone.CONTENT_ITEM_TYPE)
				.withValue(Phone.NUMBER,"1701010101")
				.withValue(Phone.TYPE, Phone.TYPE_WORK)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Phone.CONTENT_ITEM_TYPE)
				.withValue(Phone.NUMBER,"1801010101")
				.withValue(Phone.TYPE, Phone.TYPE_WORK_MOBILE)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Phone.CONTENT_ITEM_TYPE)
				.withValue(Phone.NUMBER,"1901010101")
				.withValue(Phone.TYPE, Phone.TYPE_WORK_PAGER)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Phone.CONTENT_ITEM_TYPE)
				.withValue(Phone.NUMBER,"2001010101")
				.withValue(Phone.TYPE, Phone.TYPE_CUSTOM)
				.withValue(Phone.LABEL,"label")
				.build());
			
			// Photo
			// TODO: Record with more photos
			
			// Relation
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Relation.CONTENT_ITEM_TYPE)
				.withValue(Relation.NAME,"rel assistant")
				.withValue(Relation.TYPE,Relation.TYPE_ASSISTANT)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Relation.CONTENT_ITEM_TYPE)
				.withValue(Relation.NAME,"rel brother")
				.withValue(Relation.TYPE,Relation.TYPE_BROTHER)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Relation.CONTENT_ITEM_TYPE)
				.withValue(Relation.NAME,"rel child")
				.withValue(Relation.TYPE,Relation.TYPE_CHILD)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Relation.CONTENT_ITEM_TYPE)
				.withValue(Relation.NAME,"rel dom partner")
				.withValue(Relation.TYPE,Relation.TYPE_DOMESTIC_PARTNER)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Relation.CONTENT_ITEM_TYPE)
				.withValue(Relation.NAME,"rel father")
				.withValue(Relation.TYPE,Relation.TYPE_FATHER)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Relation.CONTENT_ITEM_TYPE)
				.withValue(Relation.NAME,"rel friend")
				.withValue(Relation.TYPE,Relation.TYPE_FRIEND)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Relation.CONTENT_ITEM_TYPE)
				.withValue(Relation.NAME,"rel manager")
				.withValue(Relation.TYPE,Relation.TYPE_MANAGER)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Relation.CONTENT_ITEM_TYPE)
				.withValue(Relation.NAME,"rel mother")
				.withValue(Relation.TYPE,Relation.TYPE_MOTHER)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Relation.CONTENT_ITEM_TYPE)
				.withValue(Relation.NAME,"rel parent")
				.withValue(Relation.TYPE,Relation.TYPE_PARENT)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Relation.CONTENT_ITEM_TYPE)
				.withValue(Relation.NAME,"rel partner")
				.withValue(Relation.TYPE,Relation.TYPE_PARTNER)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Relation.CONTENT_ITEM_TYPE)
				.withValue(Relation.NAME,"rel referred by")
				.withValue(Relation.TYPE,Relation.TYPE_REFERRED_BY)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Relation.CONTENT_ITEM_TYPE)
				.withValue(Relation.NAME,"rel relative")
				.withValue(Relation.TYPE,Relation.TYPE_RELATIVE)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Relation.CONTENT_ITEM_TYPE)
				.withValue(Relation.NAME,"rel sister")
				.withValue(Relation.TYPE,Relation.TYPE_SISTER)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Relation.CONTENT_ITEM_TYPE)
				.withValue(Relation.NAME,"rel spouse")
				.withValue(Relation.TYPE,Relation.TYPE_SPOUSE)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Relation.CONTENT_ITEM_TYPE)
				.withValue(Relation.NAME,"rel custom")
				.withValue(Relation.TYPE,Relation.TYPE_CUSTOM)
				.withValue(Relation.LABEL,"label")
				.build());
			
			// Structured name
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,StructuredName.CONTENT_ITEM_TYPE)
				.withValue(StructuredName.DISPLAY_NAME,"display name")
				.withValue(StructuredName.GIVEN_NAME,"given name")
				.withValue(StructuredName.FAMILY_NAME,"family name")
				.withValue(StructuredName.PREFIX,"prefix")
				.withValue(StructuredName.MIDDLE_NAME,"middle name")
				.withValue(StructuredName.SUFFIX, "suffix")
				.withValue(StructuredName.PHONETIC_GIVEN_NAME,"phon given name")
				.withValue(StructuredName.PHONETIC_MIDDLE_NAME,"phon middle name")
				.withValue(StructuredName.PHONETIC_FAMILY_NAME,"phon family name")
				.build());
			
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,StructuredName.CONTENT_ITEM_TYPE)
				.withValue(StructuredName.DISPLAY_NAME,"2display name")
				.withValue(StructuredName.GIVEN_NAME,"2given name")
				.withValue(StructuredName.FAMILY_NAME,"2family name")
				.withValue(StructuredName.PREFIX,"2prefix")
				.withValue(StructuredName.MIDDLE_NAME,"2middle name")
				.withValue(StructuredName.SUFFIX, "suffix")
				.withValue(StructuredName.PHONETIC_GIVEN_NAME,"2phon given name")
				.withValue(StructuredName.PHONETIC_MIDDLE_NAME,"2phon middle name")
				.withValue(StructuredName.PHONETIC_FAMILY_NAME,"2phon family name")
				.build());
			
			// Structured postal
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,StructuredPostal.CONTENT_ITEM_TYPE)
				.withValue(StructuredPostal.FORMATTED_ADDRESS,"address home")
				.withValue(StructuredPostal.TYPE,StructuredPostal.TYPE_HOME)
			    .withValue(StructuredPostal.STREET,"street")
			    .withValue(StructuredPostal.POBOX,"pobox")
			    .withValue(StructuredPostal.NEIGHBORHOOD,"neighnorhood")
			    .withValue(StructuredPostal.CITY,"city")
			    .withValue(StructuredPostal.REGION,"region")
			    .withValue(StructuredPostal.POSTCODE,"postcode")
			    .withValue(StructuredPostal.COUNTRY,"country")
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,StructuredPostal.CONTENT_ITEM_TYPE)
				.withValue(StructuredPostal.FORMATTED_ADDRESS,"address other")
				.withValue(StructuredPostal.TYPE,StructuredPostal.TYPE_OTHER)
			    .withValue(StructuredPostal.STREET,"street")
			    .withValue(StructuredPostal.POBOX,"pobox")
			    .withValue(StructuredPostal.NEIGHBORHOOD,"neighnorhood")
			    .withValue(StructuredPostal.CITY,"city")
			    .withValue(StructuredPostal.REGION,"region")
			    .withValue(StructuredPostal.POSTCODE,"postcode")
			    .withValue(StructuredPostal.COUNTRY,"country")
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,StructuredPostal.CONTENT_ITEM_TYPE)
				.withValue(StructuredPostal.FORMATTED_ADDRESS,"address work")
				.withValue(StructuredPostal.TYPE,StructuredPostal.TYPE_WORK)
			    .withValue(StructuredPostal.STREET,"street")
			    .withValue(StructuredPostal.POBOX,"pobox")
			    .withValue(StructuredPostal.NEIGHBORHOOD,"neighnorhood")
			    .withValue(StructuredPostal.CITY,"city")
			    .withValue(StructuredPostal.REGION,"region")
			    .withValue(StructuredPostal.POSTCODE,"postcode")
			    .withValue(StructuredPostal.COUNTRY,"country")
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,StructuredPostal.CONTENT_ITEM_TYPE)
				.withValue(StructuredPostal.FORMATTED_ADDRESS,"address custom")
				.withValue(StructuredPostal.TYPE,StructuredPostal.TYPE_CUSTOM)
				.withValue(StructuredPostal.LABEL, "label")
			    .withValue(StructuredPostal.STREET,"street")
			    .withValue(StructuredPostal.POBOX,"pobox")
			    .withValue(StructuredPostal.NEIGHBORHOOD,"neighnorhood")
			    .withValue(StructuredPostal.CITY,"city")
			    .withValue(StructuredPostal.REGION,"region")
			    .withValue(StructuredPostal.POSTCODE,"postcode")
			    .withValue(StructuredPostal.COUNTRY,"country")
				.build());
			
			// Website
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Website.CONTENT_ITEM_TYPE)
				.withValue(Website.URL,"http://blog.website.com")
				.withValue(Website.TYPE, Website.TYPE_BLOG)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Website.CONTENT_ITEM_TYPE)
				.withValue(Website.URL,"http://ftp.website.com")
				.withValue(Website.TYPE, Website.TYPE_FTP)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Website.CONTENT_ITEM_TYPE)
				.withValue(Website.URL,"http://home.website.com")
				.withValue(Website.TYPE, Website.TYPE_HOME)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Website.CONTENT_ITEM_TYPE)
				.withValue(Website.URL,"http://homepage.website.com")
				.withValue(Website.TYPE, Website.TYPE_HOMEPAGE)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Website.CONTENT_ITEM_TYPE)
				.withValue(Website.URL,"http://other.website.com")
				.withValue(Website.TYPE, Website.TYPE_OTHER)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Website.CONTENT_ITEM_TYPE)
				.withValue(Website.URL,"http://profile.website.com")
				.withValue(Website.TYPE, Website.TYPE_PROFILE)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Website.CONTENT_ITEM_TYPE)
				.withValue(Website.URL,"http://work.website.com")
				.withValue(Website.TYPE, Website.TYPE_WORK)
				.build());
			operationList.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, rawpos)
				.withValue(Data.MIMETYPE,Website.CONTENT_ITEM_TYPE)
				.withValue(Website.URL,"http://custom.website.com")
				.withValue(Website.TYPE, Website.TYPE_CUSTOM)
				.withValue(Website.LABEL,"label")
				.build());
			getContentResolver().applyBatch(ContactsContract.AUTHORITY, operationList);
		}
		catch (RemoteException e)
		{
			if (E) e.printStackTrace();
		}
		catch (OperationApplicationException e)
		{
			if (E) e.printStackTrace();
		}
	}
	public void cmd_tools_DUMP()
	{
		//Dump.dump_android("DUMP", getContentResolver());
		final ContentResolver resolver=getContentResolver();
		Cursor c=resolver.query(Contacts.CONTENT_URI, new String[]{Contacts._ID}, Contacts.DISPLAY_NAME+"='Philippe Lang'", null, null);
		if (c.moveToFirst())
		{
			final long id=c.getLong(0);
			Dump.dump("DUMP",false,resolver,
				ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI,id));
			c.close();
			Dump.dumpCursor(c=resolver.query(RawContacts.CONTENT_URI, new String[]{RawContacts._ID}, RawContacts.CONTACT_ID+"="+id, null, null));
			c.close();
			
		}
	}
	public void cmd_DUMP()
	{
		Dump.dump_android("DUMP", false,getContentResolver());
	}
	public void cmd_DUMP_lite()
	{
		Dump.dump_android_lite("DUMP", false,getContentResolver());
	}
//	public void cmdAAA_PURGE()
//	{
//		getContentResolver().delete(RawContacts.CONTENT_URI, 
//			RawContacts.ACCOUNT_TYPE+ProvidersManager.volatilAccountToIn(), null);
//		
//	}
	public void cmd_tools_PurgeMax()
	{
		getContentResolver().delete(RawContacts.CONTENT_URI,null, null);
		
	}
	public void cmd_tools_PurgeNoCompte()
	{
		//Dump.dump_android_lite("DUMP", getContentResolver());
		final int c=getContentResolver().delete(RawContacts.CONTENT_URI,RawContacts.ACCOUNT_NAME+" is null", null);
		if (D) Log.d(TAG,"Purge "+c+" records");
		//Dump.dump_android_lite("DUMP", getContentResolver());
	}
//	public void cmdAAA_DELETE()
//	{
//		ContentResolver resolver=getContentResolver();
//		resolver.delete(RawContacts.CONTENT_URI, null, null);
//	}
	public void cmdListContact()
	{
		final Intent intent=new Intent("com.android.contacts.action.LIST_CONTACTS");
		startActivity(intent);
	}
	// ------------------------------ PICK
	public void cmdPickContact()
	{
		if (D) Log.d(TAG,"Permission="+checkCallingPermission("android.permission.READ_CONTACTS"));
		final Intent intent=new Intent(Intent.ACTION_PICK);
		intent.setType(Contacts.CONTENT_TYPE);
		startActivityForResult(intent, LEGACY_CONTACT);
	}

	public void cmdPickPhone()
	{
		final Intent intent=new Intent(Intent.ACTION_PICK);
		intent.setType(Phone.CONTENT_TYPE);
		startActivityForResult(intent, DATA);
	}

	public void cmdPickPostal()
	{
		final Intent intent=new Intent(Intent.ACTION_PICK);
		intent.setType(StructuredPostal.CONTENT_TYPE);
		startActivityForResult(intent, DATA);
	}
	
	// ------------------------------ Shortcut
//	public void cmdCreateDialShortCut()
//	{
//		Intent intent=new Intent(Intent.ACTION_CREATE_SHORTCUT);
//		intent.setComponent(new ComponentName("com.android.contacts","alias.DialShortcut"));
//		startActivityForResult(intent,DATA);
//	}
//	public void cmdCreateMessageShortCut()
//	{
//		Intent intent=new Intent(Intent.ACTION_CREATE_SHORTCUT);
//		intent.setComponent(new ComponentName("com.android.contacts","alias.MessageShortcut"));
//		startActivityForResult(intent,DATA);
//	}
//	public void cmdCreateShortCut()
//	{
//		Intent intent=new Intent(Intent.ACTION_CREATE_SHORTCUT);
//		startActivityForResult(intent,DATA);
//	}
	// ------------------------------ GET_CONTENT
	public void cmdGetContentContact()
	{
		final Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType(Contacts.CONTENT_ITEM_TYPE);
		startActivityForResult(intent, CONTACT);
	}

	public void cmdGetContentContactPhone()
	{
		final Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType(Phone.CONTENT_ITEM_TYPE);
		startActivityForResult(intent, DATA);
	}

	public void cmdGetContentContactPostal()
	{
		final Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType(StructuredPostal.CONTENT_ITEM_TYPE);
		startActivityForResult(intent, DATA);
	}

	// ---------------- INSERT or EDIT
//	public void cmdInsertOrEditContact()
//	{
//		Intent intent=new Intent(Intent.ACTION_INSERT_OR_EDIT);
//		intent.setType(Contacts.CONTENT_ITEM_TYPE);
//		startActivity(intent);
//	}
//	public void cmdInsertOrEditRawContact()
//	{
//		Intent intent=new Intent(Intent.ACTION_INSERT_OR_EDIT);
//		intent.setType(RawContacts.CONTENT_ITEM_TYPE);
//		startActivity(intent);
//	}
	
	// ---------------- SEARCH
	public void cmdSearch()
	{
		final Intent intent=new Intent(Intent.ACTION_SEARCH);
		intent.putExtra(SearchManager.QUERY,"Philippe Lang");  // FIXME: Confidentialité
		startActivity(intent);
	}
	
	public void cmdSearchCall()
	{
		final Intent intent=new Intent(Intent.ACTION_SEARCH);
		intent.putExtra(SearchManager.ACTION_MSG,"call");
		intent.putExtra(SearchManager.QUERY,"01 73 26 14 65"); // FIXME: Confidentialité
		startActivity(intent);
	}
	
	public void cmdSearchEMail()
	{
		
			
		final Intent intent=new Intent(Intent.ACTION_SEARCH);
		intent.putExtra(Insert.EMAIL, "philippe.prados@atos.net");  // FIXME: Confidentialité
		startActivity(intent);
	}
	public void cmdSearchPhone()
	{
		Intent intent=new Intent(Intent.ACTION_SEARCH);
		intent.putExtra(Insert.PHONE, "01 73 26 14 65");
		startActivity(intent);
	}

	// -----------MANQUE LES SEARCH_SUGGESTION
	// ----------- JOIN
//	public void cmdJoinAgregate()
//	{
//		Intent intent=new Intent("com.android.contacts.action.JOIN_AGGREGATE");
//		intent.putExtra("com.android.contacts.action.AGGREGATE_ID",1l);
//		startActivity(intent);
//	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		try
		{
			if (V) Log.v(TAG,"onActivityResult...");
			AlertDialog dialog=new AlertDialog.Builder(this).create();
			if (resultCode==RESULT_OK)
			{
				Uri uri=data.getData();
				System.out.println(uri);
				Cursor cursor;
				switch (requestCode)
				{
					case NOTHING:
						break;
					case CONTACT:
						cursor=getContentResolver().query(uri, new String[]{Contacts._ID,Contacts.DISPLAY_NAME}, null,null,null);
						if (cursor!=null && cursor.moveToFirst())
						{
							dialog.setMessage(cursor.getLong(0)+" "+cursor.getString(1));
						}
						cursor.close();
						break;
					case DATA:
						cursor=getContentResolver().query(uri, new String[]{Data._ID,Data.DATA1}, null,null,null);
						if (cursor!=null && cursor.moveToFirst())
						{
							dialog.setMessage(cursor.getLong(0)+" "+cursor.getString(1));
						}
						cursor.close();
						break;
					default:
						if (E) Log.e(TAG,"Default in onActivityResult");
				}
				dialog.show();
			}
			else
			{
				dialog.setMessage("Result error");
			}
			if (D) Log.d(TAG,"result="+((data==null) ? "null" : data.toString()));
		}
		catch (Exception e)
		{
			if (E) e.printStackTrace();
		}
	}
}
