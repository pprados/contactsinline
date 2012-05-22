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
package fr.prados.contacts.ui;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.QuickContact;
import android.util.AttributeSet;
import android.view.View;
import android.widget.QuickContactBadge;

public class ExQuickContactBadge extends QuickContactBadge
{
	private long _itemId;
	public ExQuickContactBadge(Context context)
	{
		super(context);
	}

	public ExQuickContactBadge(Context context, AttributeSet attrs) 
	{
        super(context, attrs, 0);
    }

    public ExQuickContactBadge(Context context, AttributeSet attrs, int defStyle)
	{
		super(context,attrs,defStyle);
	}

    public final void setItemId(long itemId)
    {
    	_itemId=itemId;
    }
	public void onClick(View v)
	{
		new AsyncTask<Void,Void,Uri>()
		{
			@Override
			protected void onPreExecute()
			{
				super.onPreExecute();
				((VolatileContactsListActivity)getContext()).incProgressBar();
			}
			@Override
			protected Uri doInBackground(Void... params)
			{
				return ProvidersManager.importVolatileContactToAndroid(_itemId, true,getContext());
			}
			@Override
			protected void onPostExecute(Uri contactUri)
			{
				super.onPostExecute(contactUri);
				((VolatileContactsListActivity)getContext()).decProgressBar();
				if (contactUri==null)
					return;
				assignContactUri(contactUri);
				final Uri lookupUri = Contacts.getLookupUri(getContext().getContentResolver(), contactUri);
				QuickContact.showQuickContact(getContext(), ExQuickContactBadge.this, lookupUri, QuickContact.MODE_MEDIUM, mExcludeMimes);
			}
		}.execute();
	}
}

