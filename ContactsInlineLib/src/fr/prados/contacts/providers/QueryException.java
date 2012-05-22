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

public abstract class QueryException extends Exception implements Parcelable
{
	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;
	
	String _account;
	String _msg;
	
	// -- Parcel managment
	protected QueryException()
	{
		super();
	}
	protected QueryException(String account,String msg)
	{
		super();
		_account=account;
		_msg=msg;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		int type=0;
		if (this instanceof AuthQueryException) type=2; // $codepro.audit.disable useOfInstanceOfWithThis
		else if (this instanceof QueryError) type=1; // $codepro.audit.disable useOfInstanceOfWithThis
		dest.writeInt(type);
		dest.writeString(_account);
		dest.writeString(_msg);
	}
	
	@Override
	public int describeContents()
	{
		return 0;
	}

	public static final Parcelable.Creator<QueryException> CREATOR = new Parcelable.Creator<QueryException>() 
	{
		@Override
        public QueryException createFromParcel(Parcel in) 
        {
			QueryException vrc=null;
			switch (in.readInt())
			{
				case 2 :
					vrc=new AuthQueryException();
					break;
				case 1:
					vrc=new QueryError();
					break;
				default:
					vrc=new QueryWarning();
					
			}
			vrc._account=in.readString();
			vrc._msg=in.readString();
			return vrc;
        }

		@Override
        public QueryException[] newArray(int size) 
        {
            return new QueryError[size];
        }
    };
    
    //--- Services
	protected QueryException(String account,String msg,Throwable e)
	{
		super(e);
		_account=account;
		_msg=msg;
	}
	@Override
	public final String getMessage()
	{
		return _msg;
	}
	public final String getAccountName()
	{
		return _account;
	}
	public final String toString()
	{
		return getMessage();
	}
}
