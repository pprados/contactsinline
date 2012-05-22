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

import java.util.ArrayList;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import fr.prados.contacts.tools.LogMarket;

public final class VolatileContact implements Comparable<VolatileContact>, Parcelable
{
	private static final String TAG="VolatileContact";
	
	private long 							_id;
	private ArrayList<VolatileRawContact> 	_rawContacts=new ArrayList<VolatileRawContact>(2);
	String									_cachedDisplayName;

	public static volatile long 			_idgenerator=1;
	static long 							_dataidgenerator;

	public interface Import
	{
		String MIMETYPE=Data.MIMETYPE;
		String CONTENT_ITEM_TYPE="vnd.android.cursor.item/vnd.fr.prados.contacts.import";
		String SUMMARY_COLUMN=Data.DATA1;
		String DETAIL_COLUMN=Data.DATA2;
		String LOOKUP_COLUMN=Data.DATA3;
	}
	public interface Copy
	{
		String MIMETYPE=Data.MIMETYPE;
		String CONTENT_ITEM_TYPE="vnd.android.cursor.item/vnd.fr.prados.contacts.copy";
		String SUMMARY_COLUMN=Data.DATA1;
		String DETAIL_COLUMN=Data.DATA2;
		String LOOKUP_COLUMN=Data.DATA3;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeLong(_id);
		dest.writeTypedList(_rawContacts);
	}
	
	private void readFromParcel(Parcel parcel)
	{
		_id=parcel.readLong();
		_rawContacts=parcel.createTypedArrayList(VolatileRawContact.CREATOR);
		for (VolatileRawContact raw:_rawContacts)
		{
			raw._parent=this;
		}
	}
	
	public static final Parcelable.Creator<VolatileContact> CREATOR = new Parcelable.Creator<VolatileContact>() 
	{
        public VolatileContact createFromParcel(Parcel in) 
        {
        	final VolatileContact vc=new VolatileContact();
        	vc.readFromParcel(in);
        	return vc;
        }

        public VolatileContact[] newArray(int size) 
        {
            return new VolatileContact[size];
        }
    };
    
	@Override
	public int describeContents()
	{
		return 0;
	}
    // -- Services
	public VolatileContact()
	{
		_id=++_idgenerator;
	}
	private static final String[] _col_id=new String[] { RawContacts.CONTACT_ID };
	static public Uri contactUriFromRawUri(ContentResolver resolver, Uri rawContactUri)
	{
		Cursor cursor = resolver.query(rawContactUri,_col_id,null, null, null);
		Uri rc=null;
		if (cursor.moveToFirst())
		{
			assert cursor.getString(0/*CONTACT_ID*/) !=null;
			rc=Uri.withAppendedPath(Contacts.CONTENT_URI,cursor.getString(0/*CONTACT_ID*/));
		}
		else
			LogMarket.wtf(TAG, "Contact unknown");
		cursor.close();
		return rc;
	}
	
	public String getLookupkey()
	{
		return Long.toString(_id);
	}
	public ContactId getContactId()
	{
		VolatileRawContact raw=_rawContacts.get(0);
		return new ContactId(
			raw._attrs.getAsString(RawContacts.ACCOUNT_TYPE),
			raw._attrs.getAsString(RawContacts.ACCOUNT_NAME),
			raw._attrs.getAsString(VolatileRawContact.LOOKUP));		
	}

	public VolatileRawContact addNewRawContact()
	{
		VolatileRawContact raw=new VolatileRawContact(this);
		_rawContacts.add(raw);
		return raw;
	}
	public String getDisplayName()
	{
		if (_cachedDisplayName!=null)
			return _cachedDisplayName;
		for (VolatileRawContact raw:_rawContacts)
		{
			VolatileData nameData=raw.get(StructuredName.CONTENT_ITEM_TYPE);
			if (nameData!=null)
			{
				final String givenNames=(String)nameData.getAttr(StructuredName.GIVEN_NAME);
				final String familyName=(String)nameData.getAttr(StructuredName.FAMILY_NAME);
				final String displayName=(String)nameData.getAttr(StructuredName.DISPLAY_NAME);
				final boolean hasGiven = !TextUtils.isEmpty(givenNames);
				final boolean hasFamily = !TextUtils.isEmpty(familyName);
				// TODO: write locale-specific blending logic here
				if (hasGiven && hasFamily)
				{
					return _cachedDisplayName=givenNames + " " + familyName;
				}
				else if (hasFamily)
				{
					if (displayName!=null)
					{
						int idx=displayName.toLowerCase().indexOf(familyName.toLowerCase());
						if (idx!=-1)
						{
							StringBuilder builder=new StringBuilder();
							builder.append(displayName.subSequence(0,idx))
								.append(familyName)
								.append(displayName.substring(idx+familyName.length()));
							builder.setCharAt(0, Character.toUpperCase(builder.charAt(0)));
							builder.trimToSize();
							return _cachedDisplayName=builder.toString();
						}
						else
							return _cachedDisplayName=displayName;
					}
					else
						return _cachedDisplayName=familyName;
				}
				else if (hasGiven)
				{
					if (displayName!=null)
					{
						int idx=displayName.toLowerCase().indexOf(givenNames.toLowerCase());
						if (idx!=-1)
						{
							StringBuilder builder=new StringBuilder();
							builder.append(displayName.subSequence(0,idx))
								.append(givenNames)
								.append(displayName.substring(idx+givenNames.length()));
							builder.setCharAt(0, Character.toUpperCase(builder.charAt(0)));
							builder.trimToSize();
							return _cachedDisplayName=builder.toString();
						}
						else
							return _cachedDisplayName=displayName;
					}
					else
					return _cachedDisplayName=givenNames;
				}
				else
				{
					return _cachedDisplayName=displayName;
				}
			}
			else
			{
				final VolatileData organisationData=raw.get(Organization.CONTENT_ITEM_TYPE);
				return _cachedDisplayName=(String)organisationData.getAttr(Organization.COMPANY);
			}
				
		}
		return null;
	}
	public long getId()
	{
		return _id;
	}
	public ArrayList<VolatileRawContact> getRawContacts()
	{
		return _rawContacts;
	}
	public VolatileRawContact getRawContact()
	{
		return _rawContacts.get(0);
	}
	
	@Override
	public int compareTo(VolatileContact another)
	{
		final String m=getDisplayName();
		final String o=another.getDisplayName();
		if (m==o) // Identity // $codepro.audit.disable stringComparison
			return 0;
		if ((m==null) || (o==null)) 
			return 1;
		return getDisplayName().compareTo(another.getDisplayName());
	}
	public boolean hasPhoneNumber()
	{
		for (VolatileRawContact raw:_rawContacts)
		{
			if (raw.getPhoneNumber()!=null)
				return true;
		}
		return false;
	}
	public boolean hasAddress()
	{
		for (VolatileRawContact raw:_rawContacts)
		{
			if (raw.getPostalFormatted()!=null)
				return true;
		}
		return false;
	}
	
	@Override
	public String toString()
	{
		return getDisplayName()+","+_rawContacts.toString();
	}

}