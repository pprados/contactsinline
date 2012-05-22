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
import java.util.HashMap;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import fr.prados.contacts.tools.LogMarket;

public final class VolatileRawContact implements Parcelable
	{
		public static final String LOOKUP=RawContacts.SYNC1;
		public static final String MUST_DELETED=RawContacts.SYNC4;
		
		VolatileContact _parent;
		
		private long	_id;
		private int		_cachedPhoneType=-2;
		private String	_cachedPhoneNumber;
		private int		_cachedPostalType=-2;
		private String	_cachedPostalFormatted;
		ContentValues _attrs=new ContentValues();
		public int _withPhoto; // 0: unknown, 1:yes, 2:no

		public static final int PHOTO_UNKNWON=0;
		public static final int PHOTO_YES=1;
		public static final int PHOTO_NO=2;
		
		public HashMap<String,ArrayList<VolatileData>> _datas=new HashMap<String,ArrayList<VolatileData>>();
		
		// -- Parcel managment
		private VolatileRawContact()
		{
			
		}
		@Override
		public void writeToParcel(Parcel dest, int flags)
		{
			dest.writeLong(_id);
			dest.writeParcelable(_attrs, flags);
			for (ArrayList<VolatileData> al:_datas.values())
			{
				for (Parcelable p:al)
				{
					dest.writeParcelable(p, flags);
				}
			}
			dest.writeParcelable(null, flags);
			if (_attrs.getAsString(RawContacts.ACCOUNT_TYPE)==null)
				LogMarket.wtf("TAG", "account type is null");
		}
		
		private void readFromParcel(Parcel parcel)
		{
			final ClassLoader loader=getClass().getClassLoader();
			_id=parcel.readLong();
			_attrs=parcel.readParcelable(loader);
			VolatileData p=null;
			do
			{
				p=(VolatileData)parcel.readParcelable(loader);
				if (p!=null)
				{
					p._rawid=_id;
					put((String)p.getAttr(Data.MIMETYPE),p);
				}
			} while (p!=null);
			if (_attrs.getAsString(RawContacts.ACCOUNT_TYPE)==null)
				LogMarket.wtf("TAG", "account type is null");
		}
		
		@Override
		public int describeContents()
		{
			return 0;
		}
	
		public static final Parcelable.Creator<VolatileRawContact> CREATOR = new Parcelable.Creator<VolatileRawContact>() 
		{
			@Override
	        public VolatileRawContact createFromParcel(Parcel in) 
	        {
				final VolatileRawContact vrc=new VolatileRawContact();
				vrc.readFromParcel(in);
				return vrc;
	        }

			@Override
	        public VolatileRawContact[] newArray(int size) 
	        {
	            return new VolatileRawContact[size];
	        }
	    };

	    // -- Services
		public VolatileRawContact(VolatileContact parent)
		{
			_parent=parent;
			_id=VolatileContact._idgenerator++;
		}
		public synchronized void put(String key,VolatileData value)
		{
			ArrayList<VolatileData> values=_datas.get(key);
			if (values==null)
			{
				values=new ArrayList<VolatileData>(5);
				_datas.put(key, values);
			}
			values.add(value);
			value.put(Data.RAW_CONTACT_ID, _id);
		}
		public void remove(String key,VolatileData value)
		{
			ArrayList<VolatileData> values=_datas.get(key);
			if (values!=null)
			{
				values.remove(value);
			}
		}
		public void removeAll(String key)
		{
			_datas.remove(key);
		}
		
		public VolatileData get(String key)
		{
			ArrayList<VolatileData> values=_datas.get(key);
			if (values==null)
				return null;
			if (values.size()==0) return null;
			//assert values.size()==1; // FIXME: Déclenche assertion et ne devrait pas
			return values.get(0);
		}
		public ArrayList<VolatileData> gets(String key)
		{
			return _datas.get(key);
		}
		public void init(Account account,String name)
		{
			setAttr(RawContacts.ACCOUNT_NAME, account.name);
			setAttr(RawContacts.ACCOUNT_TYPE, account.type);
			VolatileData data=new VolatileData();
			data.put(StructuredName.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
			data.put(StructuredName.DISPLAY_NAME, name);
			put(StructuredName.MIMETYPE, data);
		}
		public Object getAttr(String name)
		{
			if (RawContacts._ID.equals(name))
				return _id;
			return _attrs.get(name);
		}
		public void removeAttr(String name)
		{
			resetCachedValues();
			_attrs.remove(name);
		}
		public void setAttr(String name,String value)
		{
			resetCachedValues();
			_attrs.put(name, value);
		}
		public void setAttr(String name,boolean value)
		{
			resetCachedValues();
			_attrs.put(name, value);
		}
		public void setAttr(String name,byte value)
		{
			resetCachedValues();
			_attrs.put(name, value);
		}
		public void setAttr(String name,byte[] value)
		{
			resetCachedValues();
			_attrs.put(name, value);
		}
		public void setAttr(String name,short value)
		{
			resetCachedValues();
			_attrs.put(name, value);
		}
		public void setAttr(String name,int value)
		{
			resetCachedValues();
			_attrs.put(name, value);
		}
		public void setAttr(String name,long value)
		{
			if (RawContacts._ID.equals(name))
			{
				_id=value;
				return;
			}
			resetCachedValues();
			_attrs.put(name, value);
		}
		public void setAttr(String name,float value)
		{
			resetCachedValues();
			_attrs.put(name, value);
		}
		public void setAttr(String name,double value)
		{
			resetCachedValues();
			_attrs.put(name, value);
		}
		
		public void setDisplayName(String displayName)
		{
			VolatileData nameData = get(StructuredName.CONTENT_ITEM_TYPE);
			if (nameData==null)
			{
				nameData=new VolatileData();
				nameData.put(StructuredName.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
				put(StructuredName.MIMETYPE,nameData);
			}
			nameData.put(StructuredName.DISPLAY_NAME, displayName);
			_parent._cachedDisplayName=displayName;
		}
	
		public final String getLookupKey()
		{
			return (String)getAttr(LOOKUP);
		}
		
		public void setLookupKey(String lookupKey)
		{
			setAttr(LOOKUP,lookupKey);
		}
		
		public int getPhoneType()
		{
			if (_cachedPhoneType!=-2)
				return _cachedPhoneType;
			final VolatileData phoneData = get(Phone.CONTENT_ITEM_TYPE);
			if (phoneData==null) return _cachedPhoneType=-1;
			return _cachedPhoneType=(Integer)phoneData.getAttr(Phone.TYPE);
		}
		
		public String getPhoneNumber()
		{
			if (_cachedPhoneNumber!=null)
				return _cachedPhoneNumber;
			String number=null;
			ArrayList<VolatileData> datas=gets(Phone.CONTENT_ITEM_TYPE);
			if ((datas==null) || datas.size()==0)
				return null;
			for (VolatileData data:datas)
			{
				if (data.getAttr(Phone.IS_SUPER_PRIMARY)!=null)
				{
					number=(String)data.getAttr(Phone.NUMBER);
				}
			}
			if (number==null)
			{
				number=(String)datas.get(0).getAttr(Phone.NUMBER);
			}
			
			return _cachedPhoneNumber=number;
		}
		
		public int getPostalType()
		{
			if (_cachedPostalType!=-2)
				return _cachedPostalType;
			final VolatileData postalData = get(StructuredPostal.CONTENT_ITEM_TYPE);
			if (postalData==null) return _cachedPostalType=-1;
			return _cachedPostalType=(Integer)postalData.getAttr(StructuredPostal.TYPE);
		}
		public String getPostalFormatted()
		{
			if (_cachedPostalFormatted!=null)
				return _cachedPostalFormatted;
			final VolatileData postalData = get(StructuredPostal.CONTENT_ITEM_TYPE);
			if (postalData==null) return null;
			return _cachedPostalFormatted=(String)postalData.getAttr(StructuredPostal.FORMATTED_ADDRESS);
		}
		
		private void resetCachedValues()
		{
			if (_parent!=null)
				_parent._cachedDisplayName=null;
			_cachedPhoneType=-2;
		}
		
		@Override
		public String toString()
		{
			return getLookupKey()
				+':'+((_parent!=null) ? _parent.getDisplayName() : "(null)");
		}
		
		public Uri updateInAndroid(ContentResolver resolver,Uri rawUri,ArrayList<ContentProviderOperation> operationList)
		{
			final long rawId=Long.parseLong(rawUri.getLastPathSegment());
			// Update current raw contact
			operationList.add(
				ContentProviderOperation.newUpdate(rawUri)
				.withValues(_attrs)
				.build()
			);
			
			// Remove all datas
			operationList.add(
				ContentProviderOperation.newDelete(Data.CONTENT_URI)
				.withSelection(Data.RAW_CONTACT_ID+"="+rawId, null)
				.build()
				);
			
			// Add new datas
			for (ArrayList<VolatileData> values:_datas.values())
			{
				for (VolatileData d:values)
				{
					operationList.add(
						ContentProviderOperation.newInsert(Data.CONTENT_URI)
						.withValue(StructuredName.RAW_CONTACT_ID, rawId)
						.withValues(d._attrs)
						.build());
				}
			}
			return rawUri;
		}
		
		private static final String[] _col_id=new String[]{RawContacts._ID};
		/**
		 * Search a raw record with the same display name and account.
		 * @param resolver
		 * @return Raw contact uri for the volatile record or null.
		 */
		private Uri getRawUriInAndroid(ContentResolver resolver)
		{
			Cursor cursor=null;
			try
			{
				// BUG: les records sont double lors d'un edit record apres import
				// Search in rawcontact, a record with same account name and type, and same lookup key
				cursor=resolver.query(RawContacts.CONTENT_URI, _col_id, 
					RawContacts.ACCOUNT_NAME+"='"+_attrs.getAsString(RawContacts.ACCOUNT_NAME)+"' and "
					+RawContacts.ACCOUNT_TYPE+"='"+_attrs.getAsString(RawContacts.ACCOUNT_TYPE)+"' and "
					//+VolatileRawContact.MUST_DELETED+"=1 and "
					+VolatileRawContact.LOOKUP+"='"+getLookupKey()+"' and "+RawContacts.DELETED+"=0",
					null, null);
				if (cursor.moveToFirst())
				{
					return ContentUris.withAppendedId(RawContacts.CONTENT_URI,cursor.getLong(0/*_ID*/));
				}
			}
			finally
			{
				if (cursor!=null)
					cursor.close();
			}
			return null;
		}
		/**
		 * Copy the volatile contact in android contacts.
		 * @param resources
		 * @param resolver
		 * @param temp
		 * @return Contact Uri
		 * @throws RemoteException
		 * @throws OperationApplicationException
		 */
		public Uri copyToAndroid(Resources resources,ContentResolver resolver,boolean temp) 
		throws RemoteException, OperationApplicationException
		{
			ArrayList<ContentProviderOperation> operationList=new ArrayList<ContentProviderOperation>(5);
			Uri rawContactUri=copyToAndroid(resources,resolver,temp,operationList);
			if (operationList.size()!=0)
			{
				ContentProviderResult[] result=resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
				if (rawContactUri==null)
					rawContactUri=result[0].uri;
			}
			return rawContactUri;
		}
		
		// FIXME: semble ne pas fonctionner si le contact existe, avec d'autres contacts en providers
		public Uri copyToAndroid(Resources resources,ContentResolver resolver,boolean temp,ArrayList<ContentProviderOperation> operationList) 
		throws RemoteException, OperationApplicationException
		{
			// Search a previous imported rawcontact
			Uri previous=getRawUriInAndroid(resolver);
			if (previous!=null)
			{
				if (temp)
				{
					//updateInAndroid(resolver,previous,operationList); // Update raw data from new values
					return previous;
				}
				else
				{
					operationList.add(ContentProviderOperation.newDelete(previous).build());
				}
			}
			
			operationList.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
				.withValues(_attrs)
				.withValue(RawContacts.DIRTY, "1")
				.build());
			int backReference=operationList.size()-1;
			for (ArrayList<VolatileData> values:_datas.values())
			{
				for (VolatileData d:values)
				{
					operationList.add(
						ContentProviderOperation.newInsert(Data.CONTENT_URI)
						.withValueBackReference(StructuredName.RAW_CONTACT_ID, backReference)
						.withValues(d._attrs)
						.build());
				}
			}
			return null;
		}

		public void copyDataToAndroid(long rawid,ArrayList<ContentProviderOperation> operationList)
		{
			for (ArrayList<VolatileData> values:_datas.values())
			{
				for (VolatileData d:values)
				{
					operationList.add(
						ContentProviderOperation.newInsert(Data.CONTENT_URI)
						.withValue(StructuredName.RAW_CONTACT_ID, rawid)
						.withValues(d._attrs)
						.build());
				}
			}
			
		}
	}