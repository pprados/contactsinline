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

import java.security.GeneralSecurityException;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import fr.prados.contacts.tools.CheckContext;
import fr.prados.contacts.tools.Eula;
import fr.prados.contacts.tools.LogMarket;
import fr.prados.contacts.tools.Update;


public class MockWizardActivity extends AccountAuthenticatorActivity
{
	private static final String TAG="MOCK";
	
	/**
	 * If set we are just checking that the user knows their credentials; this
	 * doesn't cause the user's password to be changed on the device.
	 */
	private boolean _confirmCredentials = false;
	/** Was the original caller asking for an entirely new account? */ 
	/*package*/ boolean _requestNewAccount = false; // $codepro.audit.disable com.instantiations.assist.eclipse.analysis.avoidPackageScopeAuditRule

	private AccountManager _accountManager;
	
	private EditText _username;
	private EditText _password;
	private Button _done;
	
	@Override
	protected void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);

		if (!Application.ACCOUNT_WITH_PASSWORD) 
			finish();
		if (!CheckContext.isContactInline(this))
		{
			return;
		}
		if (!CheckContext.isAndroidMinVersion(this, 7))
		{
			return;
		}
		Update.showUdpate(this,Application.VERSION,R.raw.update);
		Eula.showEula(this,R.raw.eula);
		
		setContentView(R.layout.mockwizard);
		_username=(EditText)findViewById(R.id.account_username);
		_password=(EditText)findViewById(R.id.account_password);

		_done=(Button)findViewById(R.id.btn_done);
		_done.setOnClickListener(new View.OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				final String username=_username.getText().toString();
				final String password=_password.getText().toString();
				final String accountName=username;
				new AsyncTask<Void, Void, Exception>()
				{
					@Override
					protected Exception doInBackground(Void...params)
					{
						Exception ex=null;
						try
						{
							if (!_confirmCredentials)
							{
								MockAuthenticationService.onlineConfirmPassword(username,password);
							}
							else
							{
								// Must invoke confirmCredential to indirectly invoke onResult, for remove the notification
								final AccountManager accountManager = AccountManager.get(Application.context);
								final Bundle options=new Bundle();
								options.putString(AccountManager.KEY_PASSWORD, password);
								final AccountManagerFuture<Bundle> futur=
									accountManager.confirmCredentials(
											new Account(accountName,
													Application.ACCOUNT_TYPE), options,
													MockWizardActivity.this, null, null);							
								final Bundle bundle=futur.getResult();
								if (!bundle.getBoolean(AccountManager.KEY_BOOLEAN_RESULT))
								{
									ex=new SecurityException();
								}
							}
							
						}
						catch (Exception e) // $codepro.audit.disable caughtExceptions
						{
							ex=e;
						}
						return ex;
					}
					@Override
					public void onPostExecute(Exception e)
					{
						removeDialog(DIALOG_PROGRESS);
						if (isCancelled()) 
						{
							_done.setVisibility(View.VISIBLE);
							return;
						}
						if (e!=null)
							_done.setVisibility(View.VISIBLE);
						onAuthenticationResult(e,accountName,username,password);
					}
					
				}.execute();
			}
		});
		
		// Analyse intent
		final Intent intent = getIntent();
		_accountManager = AccountManager.get(this);
		final Account account=(Account)intent.getExtras().get("account");
		if (account!=null)
		{
			_confirmCredentials = true;
	    	_username.setText(account.name);
	    	_username.setEnabled(false);
			_password.requestFocus();
		}
		else
		{
			//setTitle(_activityLabelId);
			_requestNewAccount = true;
		}
	}

	private static final int DIALOG_PROGRESS=1;
	
	@Override
	protected Dialog onCreateDialog(int id)
	{
		switch (id)
		{
			case DIALOG_PROGRESS:
				final ProgressDialog dlg=ProgressDialog.show(
					this, "", getString(R.string.authenticating), true, false);
				dlg.setCancelable(true);
				return dlg;
			default:
				LogMarket.wtf(TAG, "Invalide dialog id "+id);
				return null;
		}
	}
	

	private void onAuthenticationResult(Exception exception,String accountName,String username,String password)
	{
		final String accountType=Application.ACCOUNT_TYPE;
		if (exception==null)
		{
            if (!_confirmCredentials) 
            {
            	final Account account=new Account(accountName, accountType);
            	if (_requestNewAccount)
        		{
            		_accountManager.addAccountExplicitly(account, password, null);
        		}
        		else
        		{
        			_accountManager.setPassword(account,password);
        		}
            	final Bundle bundle = new Bundle();
	        	bundle.putString(AccountManager.KEY_ACCOUNT_NAME, accountName);
	        	bundle.putString(AccountManager.KEY_ACCOUNT_TYPE,accountType);
	        	bundle.putString(AccountManager.KEY_AUTHTOKEN, password);
        		setAccountAuthenticatorResult(bundle);
            	if (Application.ACCOUNT_WITH_SYNC)
					ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);
        		finish();
            } 
            else 
            {
            	try
            	{
            		MockAuthenticationService.onlineConfirmPassword(username, password);
	        		final Account account = new Account(accountName, accountType);
	        		_accountManager.setPassword(account, password);
	        		final Bundle extra=new Bundle();
	        		extra.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
	        		setAccountAuthenticatorResult(extra);
            	}
            	catch (GeneralSecurityException e) // $codepro.audit.disable logExceptions
            	{
	        		final Bundle extra=new Bundle();
	        		extra.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
	        		setAccountAuthenticatorResult(extra);
            	}
        		finish();
            }
		}
		else
		{
			CharSequence msg;
			if (exception instanceof GeneralSecurityException)
			{
				msg=getString(R.string.err_authent);
			}
			else
			{
				msg=exception.getLocalizedMessage();
				if (msg==null)
					msg=exception.getMessage();
				if (msg==null)
					msg=exception.toString();
			}
			final AlertDialog dialog=new AlertDialog.Builder(this)
				.setMessage(msg)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() 
					{
			           public void onClick(DialogInterface dialog, int id)
			           {
			               dialog.dismiss();
			               _done.setVisibility(View.VISIBLE);
			               _password.requestFocus();
			           }
					})
				.create();
			if (!isFinishing())
			{
				dialog.setOwnerActivity(this);
				dialog.show();
			}
		}
	}
}
