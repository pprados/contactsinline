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

import java.text.MessageFormat;
import java.util.ArrayList;

import android.os.Debug;
import android.os.RemoteException;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import fr.prados.contacts.ContactId;
import fr.prados.contacts.VolatileContact;
import fr.prados.contacts.VolatileContact.Copy;
import fr.prados.contacts.VolatileContact.Import;
import fr.prados.contacts.VolatileData;
import fr.prados.contacts.VolatileRawContact;
import fr.prados.contacts.providers.Provider;
import fr.prados.contacts.providers.QueryException;
import fr.prados.contacts.providers.ResultsAndExceptions;

public class MockProvider extends Provider
{

	private VolatileContact createVC(String accountName,String name) 
	{
		final VolatileContact vc=new VolatileContact();
		final VolatileRawContact raw=vc.addNewRawContact();
		VolatileData data;

		final String lookup="mock-"+name;
		
		 // [[----Don't forget, if you would like to import contact from the contact view !
		raw.setAttr(RawContacts.ACCOUNT_NAME, accountName);
		raw.setLookupKey(lookup);
		raw.setAttr(RawContacts.ACCOUNT_TYPE, Application.ACCOUNT_TYPE);

		final boolean sync=Application.ACCOUNT_WITH_SYNC;
		data=new VolatileData();
		final String copyOrImport=(sync) ? Import.CONTENT_ITEM_TYPE : Copy.CONTENT_ITEM_TYPE;
		final int summary=(sync) ? R.string.copy_summary : R.string.import_summary;
		final int detail=(sync) ? R.string.copy_detail : R.string.import_detail;
		data.put(Data.MIMETYPE, copyOrImport); 
		data.put(Import.SUMMARY_COLUMN, Application.context.getString(summary));
		data.put(Import.DETAIL_COLUMN,
			MessageFormat.format(
					Application.context.getString(detail),
					RawContacts.ACCOUNT_NAME));
		data.put(Import.LOOKUP_COLUMN,lookup);
		raw.put(copyOrImport,data);
		
		raw.setAttr(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE_DISABLED);
		// ---]]
		
		data=new VolatileData();
		data.put(StructuredName.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
		data.put(StructuredName.DISPLAY_NAME,name);
		raw.put(StructuredName.CONTENT_ITEM_TYPE,data);

		data=new VolatileData();
		data.put(Email.MIMETYPE, Email.CONTENT_ITEM_TYPE);
		data.put(Email.DATA,name+"@"+accountName+".org");
		raw.put(Email.CONTENT_ITEM_TYPE,data);

		data=new VolatileData();
		data.put(StructuredPostal.MIMETYPE,StructuredPostal.CONTENT_ITEM_TYPE);
		data.put(StructuredPostal.TYPE,StructuredPostal.TYPE_HOME);
		data.put(StructuredPostal.FORMATTED_ADDRESS, "1, rue chez moi 99000 Berk, France");
		raw.put(StructuredPostal.CONTENT_ITEM_TYPE,data);

		
		return vc;
	}

	@Override
	public ResultsAndExceptions queryContact(String accountName,
			String selection, String selectionArg) throws RemoteException 
	{
		if (D) Log.d("MEM",""+Debug.getGlobalAllocSize());
		if (I) Log.i(TAG,"queryContact("+accountName+","+selection+","+selectionArg+")...");
		final ResultsAndExceptions result=new ResultsAndExceptions();
		final ArrayList<VolatileContact> list=result.contacts=new ArrayList<VolatileContact>();
		for (int i=0;i<20;++i)
			list.add(createVC(accountName,selectionArg+i));
		if (I) Log.i(TAG,"queryContact("+accountName+","+selection+","+selectionArg+")="+result);
		if (D) Log.d("MEM",""+Debug.getGlobalAllocSize());
		return result;
	}

	@Override
	public byte[] getAccountPhoto(ContactId id) throws RemoteException
	{
		return null;
	}
	
	public VolatileContact getContact(String accountName,String lookup) throws QueryException
	{
		
		return createVC(accountName,lookup.substring("mock-".length()));
	}

	@Override
	public VolatileContact getVolatileContact(ContactId id) throws RemoteException
	{
		try
		{
			return getContact(id.accountName.toString(),id.lookupKey.toString());
		}
		catch (QueryException e)
		{
			if (W) Log.w(TAG,"getVolatileContact",e);
			return null;
		}
	}
}
