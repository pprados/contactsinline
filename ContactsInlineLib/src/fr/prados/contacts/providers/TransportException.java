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

import android.os.Parcel;
import android.os.Parcelable;

public final class TransportException implements Parcelable
{
	private QueryException _e;
	public void setException(QueryException e)
	{
		_e=e;
	}
	public void getException() throws QueryException
	{
		if (_e!=null) throw _e;
	}
	@Override
	public int describeContents()
	{
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeParcelable(_e, flags);
	}
	
	public void readFromParcel(Parcel in)
	{
		final ClassLoader loader=getClass().getClassLoader();
		_e=in.readParcelable(loader);
	}
	
	public static final Parcelable.Creator<TransportException> CREATOR = new Parcelable.Creator<TransportException>() 
	{
		@Override
        public TransportException createFromParcel(Parcel in) 
        {
			TransportException rc=new TransportException();
			rc.readFromParcel(in);
			return rc;
        }

		@Override
        public TransportException[] newArray(int size) 
        {
            return new TransportException[size];
        }
    };
}