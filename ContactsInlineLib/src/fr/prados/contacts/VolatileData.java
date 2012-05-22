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

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract.Data;

public final class VolatileData implements Parcelable
{
	ContentValues _attrs=new ContentValues();
	long _id=++VolatileContact._dataidgenerator;
	long _rawid;
	
	// ----- Parcel management
	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeLong(_id);
		dest.writeParcelable(_attrs, flags);
	}
	private void readFromParcel(Parcel parcel)
	{
		final ClassLoader loader=getClass().getClassLoader();
		_id=parcel.readLong();
		_attrs=parcel.readParcelable(loader);
	}
	@Override
	public int describeContents()
	{
		return 0;
	}
	public static final Parcelable.Creator<VolatileData> CREATOR = new Parcelable.Creator<VolatileData>() 
	{
        public VolatileData createFromParcel(Parcel in) 
        {
        	final VolatileData vd=new VolatileData();
        	vd.readFromParcel(in);
        	return vd;
        }

        public VolatileData[] newArray(int size) 
        {
            return new VolatileData[size];
        }
    };
    
    // -- Services
	public VolatileData()
	{
		
	}
	public Object getAttr(String name)
	{
		if (Data._ID.equals(name))
			return _id;
		if (Data.RAW_CONTACT_ID.equals(name))
			return _rawid;
		return _attrs.get(name);
	}
	public void put(String name,String value)
	{
		_attrs.put(name, value);
	}
	public void put(String name,boolean value)
	{
		_attrs.put(name, value);
	}
	public void put(String name,byte value)
	{
		_attrs.put(name, value);
	}
	public void put(String name,byte[] value)
	{
		_attrs.put(name, value);
	}
	public void put(String name,short value)
	{
		_attrs.put(name, value);
	}
	public void put(String name,int value)
	{
		_attrs.put(name, value);
	}
	public void put(String name,long value)
	{
		if (Data._ID.equals(name))
			_id=value;
		else if (Data.RAW_CONTACT_ID.equals(name))
			_rawid=value;
		else
			_attrs.put(name, value);
	}
	public void put(String name,float value)
	{
		_attrs.put(name, value);
	}
	public void put(String name,double value)
	{
		_attrs.put(name, value);
	}
	@Override
	public String toString()
	{
		return _attrs.toString();
	}
}