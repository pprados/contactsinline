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

import java.util.ArrayList;
import java.util.List;

import android.database.MatrixCursor;
import android.os.Parcel;
import android.os.Parcelable;
import fr.prados.contacts.VolatileContact;

public final class ResultsAndExceptions implements Parcelable
{
	public MatrixCursor cursor; // Read-only cursor
	public ArrayList<VolatileContact> contacts;
	public List<QueryException> exceptions=new ArrayList<QueryException>(3);
	public int pendingJob;
	public long timeout;
	
	// ----- Parcel management
	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeTypedList(contacts);
		dest.writeTypedList(exceptions);
		dest.writeInt(pendingJob);
		dest.writeLong(timeout);
	}

	@Override
	public int describeContents()
	{
		return 0;
	}
	public static final Parcelable.Creator<ResultsAndExceptions> CREATOR = new Parcelable.Creator<ResultsAndExceptions>() 
	{
        public ResultsAndExceptions createFromParcel(Parcel in) 
        {
        	final ResultsAndExceptions vd=new ResultsAndExceptions();
    		vd.contacts=in.createTypedArrayList(VolatileContact.CREATOR);
    		vd.exceptions=in.createTypedArrayList(QueryException.CREATOR);
    		vd.pendingJob=in.readInt();
    		vd.timeout=in.readLong();
        	return vd;
        }

        public ResultsAndExceptions[] newArray(int size) 
        {
            return new ResultsAndExceptions[size];
        }
    };
    public String toString()
    {
    	return contacts.toString()+" "+exceptions.toString();
    }
};

