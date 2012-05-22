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

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.PhoneNumberUtils;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import fr.prados.contacts.lib.R;
import fr.prados.contacts.ui.Collapser.Collapsible;

/**
 * Class used for displaying a dialog with a list of phone numbers of which one
 * will be chosen to make a call or initiate an sms message.
 */
public final class PhoneDisambigDialog implements DialogInterface.OnClickListener,
		DialogInterface.OnDismissListener
{

	private Context _context;

	private AlertDialog _dialog;

	private boolean _sendSms;

	private Cursor _phonesCursor;

	private ListAdapter _phonesAdapter;

	private ArrayList<PhoneItem> _phoneItemList;

	public PhoneDisambigDialog(Context context, Cursor phonesCursor)
	{
		this(context, phonesCursor, false /* make call */);
	}

	public PhoneDisambigDialog(Context context, Cursor phonesCursor,
			boolean sendSms)
	{
		_context = context;
		_sendSms = sendSms;
		_phonesCursor = phonesCursor;

		_phoneItemList = makePhoneItemsList(phonesCursor);
		Collapser.collapseList(_phoneItemList);

		_phonesAdapter = new PhonesAdapter(_context, _phoneItemList);

		// Need to show disambig dialogue.
		final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(_context)
				.setAdapter(
					_phonesAdapter, this).setTitle(
					sendSms ? R.string.sms_disambig_title
							: R.string.call_disambig_title);

		_dialog = dialogBuilder.create();
	}

	/**
	 * Show the dialog.
	 */
	public void show()
	{
		if (_phoneItemList.size() == 1)
		{
			// If there is only one after collapse, just select it, and close;
			onClick(_dialog, 0);
			return;
		}
		_dialog.show();
	}

	public void onClick(DialogInterface dialog, int which)
	{
		if (_phoneItemList.size() > which && which >= 0)
		{
			final PhoneItem phoneItem = _phoneItemList.get(which);
			final String phoneNumber = phoneItem.phoneNumber;

			if (_sendSms)
			{
				final Intent intent = new Intent(Intent.ACTION_SENDTO,Uri.fromParts("sms", phoneNumber.toString(), null));
		        _context.startActivity(intent);
			}
			else
			{
				final Intent intent = new Intent(Intent.ACTION_DIAL,Uri.fromParts("tel", phoneNumber.toString(), null));
		        _context.startActivity(intent);
			}
		}
		else
		{
			dialog.dismiss();
		}
	}

	public void onDismiss(DialogInterface dialog)
	{
		_phonesCursor.close();
	}

	private static final class PhonesAdapter extends ArrayAdapter<PhoneItem>
	{

		public PhonesAdapter(Context context, List<PhoneItem> objects)
		{
			super(context, android.R.layout.simple_dropdown_item_1line,
					android.R.id.text1, objects);
		}
	}

	private final class PhoneItem implements Collapsible<PhoneItem>
	{

		private String phoneNumber;

		public PhoneItem(String newPhoneNumber)
		{
			phoneNumber = newPhoneNumber;
		}

		public boolean collapseWith(PhoneItem phoneItem)
		{
			if (!shouldCollapseWith(phoneItem))
			{
				return false;
			}
			// Just keep the number and id we already have.
			return true;
		}

		public boolean shouldCollapseWith(PhoneItem phoneItem)
		{
			if (PhoneNumberUtils.compare(
				PhoneDisambigDialog.this._context, phoneNumber,
				phoneItem.phoneNumber))
			{
				return true;
			}
			return false;
		}

		public String toString()
		{
			return phoneNumber;
		}
	}

	private ArrayList<PhoneItem> makePhoneItemsList(Cursor phonesCursor)
	{
		final ArrayList<PhoneItem> phoneList = new ArrayList<PhoneItem>(5);

		phonesCursor.moveToPosition(-1);
		while (phonesCursor.moveToNext())
		{
			final String phone = phonesCursor.getString(phonesCursor
					.getColumnIndex(Phone.NUMBER));
			phoneList.add(new PhoneItem(phone));
		}

		return phoneList;
	}
}
