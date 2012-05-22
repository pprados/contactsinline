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

import android.os.Parcel;
import android.os.Parcelable;

public final class ContactId implements Parcelable
{
	public final CharSequence accountType;
	public final CharSequence accountName;
	public final CharSequence lookupKey;
	private final CharSequence _hashKey;

	// -- Parcel managment
	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeString(accountType.toString());
		dest.writeString(accountName.toString());
		dest.writeString(lookupKey.toString());
	}
	
	@Override
	public int describeContents()
	{
		return 0;
	}

	public static final Parcelable.Creator<ContactId> CREATOR = new Parcelable.Creator<ContactId>() 
	{
		@Override
        public ContactId createFromParcel(Parcel in) 
        {
			final String accountType=in.readString();
			final String accountName=in.readString();
			final String lookupKey=in.readString();
			return new ContactId(accountType,accountName,lookupKey);
        }

		@Override
        public ContactId[] newArray(int size) 
        {
            return new ContactId[size];
        }
    };
    
    // --- Services
	public ContactId(CharSequence accountType,CharSequence accountName,CharSequence lookupKey)
	{
		this.accountType=accountType;
		this.accountName=accountName;
		this.lookupKey=lookupKey;
		_hashKey=(accountType+"|"+accountName+"|"+lookupKey).toString();
	}
	
	@Override
	public String toString()
	{
		return new StringBuilder().append(accountType).append(':').append(accountName).append(':').append(lookupKey).toString();
	}
	
	@Override
	public int hashCode()
	{
		return _hashKey.hashCode();
	}
	@Override
	public boolean equals(Object x)
	{
		if (this==x) return true;
		if (!(x instanceof ContactId)) return false;
		return _hashKey.equals(((ContactId)x)._hashKey);
	}
}
