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
package fr.prados.contacts.providers.ldap;

import static fr.prados.contacts.Constants.EMULATOR;

import java.security.GeneralSecurityException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;

import fr.prados.contacts.Application;
import fr.prados.contacts.providers.AbstractSimpleAuthenticator;
import fr.prados.contacts.providers.QueryError;
import fr.prados.contacts.providers.QueryException;
import fr.prados.contacts.tools.CheckContext;
import fr.prados.contacts.tools.Eula;
import fr.prados.contacts.tools.HelpDebug;
import fr.prados.contacts.tools.LogMarket;
import fr.prados.contacts.tools.Update;
import fr.prados.provider.contacts.ldap.R;

// BUG si abandon lors du login apres le wizard
public class PredefinedServerWizardActivity extends AbstractWizardActivity
{
	
	/**
	 * If set we are just checking that the user knows their credentials; this
	 * doesn't cause the user's password to be changed on the device.
	 */
	private boolean _confirmCredentials = false;

	/** Was the original caller asking for an entirely new account? */
	protected boolean _requestNewAccount = false;

	private View _login;
	private TextView _invite;
	private TextView _labelUsername;
	private EditText _username;
	private TextView _labelPassword;
	private EditText _password;
	private Button  _donep2;
	
	AlertDialog.Builder _alertBuilder;

	protected int _logoId;
	protected int _activityLabelId;
	protected int _updateLabelId;
	protected int _inviteId;
	protected int _account_setup_incoming_username_labelId;
	protected int _account_setup_username_hintId;
	protected int _account_setup_incoming_password_labelId;
	protected int _account_setup_password_hintId;
	protected String _accountName;
	protected String _hostName;
	protected String _hostPort;
	
	protected PredefinedServerWizardActivity(
			int logo,
			int activityLabel,
			int updateLabel,
			int invite,
			int account_setup_incoming_username_label,
			int account_setup_username_hint,
			int account_setup_incoming_password_label,
			int account_setup_password_hint,
			String accountName)
	{
		_logoId=logo;
		_activityLabelId=activityLabel;
		_updateLabelId=updateLabel;
		_inviteId=invite;
		_account_setup_incoming_username_labelId=account_setup_incoming_username_label;
		_account_setup_username_hintId=account_setup_username_hint;
		_account_setup_incoming_password_labelId=account_setup_incoming_password_label;
		_account_setup_password_hintId=account_setup_password_hint;
		_accountName=accountName;
		int idx=accountName.indexOf(':');
		if (idx!=-1)
		{
			_hostName=accountName.substring(0,idx);
			_hostPort=accountName.substring(idx+1);
		}
		else
		{
			_hostName=accountName;
			_hostPort="";
		}
	}
	private void init()
	{
		final Intent intent = getIntent();
		_accountManager = AccountManager.get(this);
		Account account=(Account)intent.getExtras().get("account");
		if (account!=null)
		{
			setTitle(_updateLabelId);
			_confirmCredentials = true;
			String longUsername=_accountManager.getUserData(account,LdapAuthenticationService.KEY_USERNAME);
	    	_username.setText(extractUsername(longUsername));
			_password.requestFocus();
		}
		else
		{
			setTitle(_activityLabelId);
			_requestNewAccount = true;
		}
			
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);

		if (EMULATOR)	HelpDebug.strictDefaults();
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
		
		setContentView(R.layout.predefinedwizard);

		_alertBuilder=new AlertDialog.Builder(PredefinedServerWizardActivity.this);

		_login=findViewById(R.id.login);
		_invite=(TextView)findViewById(R.id.invite);
		_labelUsername=(TextView)findViewById(R.id.label_account_quick_username);
		_username=(EditText)findViewById(R.id.account_quick_username);
		_labelPassword=(TextView)findViewById(R.id.label_account_quick_password);
		_password=(EditText)findViewById(R.id.account_quick_password);
		_donep2=(Button)findViewById(R.id.btn_done);
		((ImageView)findViewById(R.id.logo)).setImageResource(_logoId);
		_invite.setText(_inviteId);
		_labelUsername.setText(_account_setup_incoming_username_labelId);
		_username.setHint(_account_setup_username_hintId);
		_labelPassword.setText(_account_setup_incoming_password_labelId);
		_password.setHint(_account_setup_password_hintId);
		_password.setOnEditorActionListener(new TextView.OnEditorActionListener()
		{
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
			{
				if (actionId==EditorInfo.IME_NULL)
				{
					doLogin();
					return true;
				}
				return false;
			}
		});
		final View.OnClickListener doneListener=new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				hideKeyboard();
				doLogin();
			}
		};
		_donep2.setOnClickListener(doneListener);
		

		init();
	}
	private void hideKeyboard()
	{
		((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(
				_donep2.getWindowToken(), 0);
	}

	@Override
	protected String injectUsername(LdapKnowParameters params,String username)
	{
		return _knowParams._usernamePattern.toString().replace(LdapKnowParameters.USER_TAG, username);
	}

	@Override
	protected void onRestoreInstanceState(final Bundle state)
	{
		super.onRestoreInstanceState(state);
		_knowParams=state.getParcelable("knowparams");
	}
	
	@Override
	protected void onSaveInstanceState(final Bundle state)
	{
		super.onSaveInstanceState(state);
		state.putParcelable("knowparams", _knowParams);
	}
	
	private static final int DIALOG_PROGRESS=1;

	@Override
	protected Dialog onCreateDialog(int id)
	{
		switch (id)
		{
			case DIALOG_PROGRESS:
				ProgressDialog dlg=ProgressDialog.show(
					this, "", getString(R.string.ldap_authenticating), true, false);
				dlg.setCancelable(true);
				dlg.setCanceledOnTouchOutside(false);
				return dlg;
			default:
				LogMarket.wtf(TAG, "Invalide dialog id "+id);
				return null;
		}
	}
	protected LdapKnowParameters getKnowParams()
	{
		return null;
	}
	private void doLogin()
	{
		_login.setVisibility(View.INVISIBLE);
		((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(
				_login.getWindowToken(), 0);
		showDialog(DIALOG_PROGRESS);
		String[] params=new String[8];
			params[0]=_accountName;
		params[1]=null;
			params[2]=_hostName;
		params[3]=_hostPort;
		params[4]=_username.getText().toString().trim();
		params[5]=_password.getText().toString();
		
		_asyncTryAuthent=new AsyncTask<String, Void, Exception>()
		{
			private String[] _params;
			
			@Override
			protected Exception doInBackground(String... params)
			{
				Exception ex=null;
				try
				{
					_params=params;
					if (_knowParams==null) _knowParams=LdapKnowParameters.getParameters(params[0]);
					if (_knowParams==null) _knowParams=getKnowParams();
					if (_knowParams==null)
						LogMarket.wtf(TAG,"Unknown parameters for "+params[0]);
					params[1/*ssl*/]=_knowParams._crypt.toString();
					if ("".equals(params[3])) 
						params[3/*port*/]=("".equals(params[1])) ? DEFAULT_PORT_LDAP : DEFAULT_PORT_LDAPS;
					params[4/*username*/]=injectUsername(_knowParams, params[4]);
					params[6/*BASEDN*/]=_knowParams._basedn.toString();
					params[7/*Mapping*/]=_knowParams._mappingname.toString();
					LdapAuthenticationService.onlineConfirmPassword(PredefinedServerWizardActivity.this,params);
				}
				catch (GeneralSecurityException e)
				{
					ex=new LDAPException(ResultCode.AUTHORIZATION_DENIED);
				}
				catch (Exception e)
				{
					ex=e;
				}
				return ex;
			}
			@Override
			public void onPostExecute(Exception e)
			{
				onAuthenticationResult(e,_params);
				_asyncTryAuthent=null;
				removeDialog(DIALOG_PROGRESS);
				if (isCancelled()) 
				{
					_login.setVisibility(View.VISIBLE);
					return;
				}
				if (e!=null)
					_login.setVisibility(View.VISIBLE);
				_params=null;
				
			}
		}.execute(params);
	}
	
	/**
	 * Called when the authentication process completes.
	 */
	private void onAuthenticationResult(Exception exception,String... params)
	{
		final LdapKnowParameters knowParams=new LdapKnowParameters();

		final String accountName = params[0];
		final String crypt = params[1];
		final String host = params[2];
		final String port = params[3];
		final String username = params[4];
		final String password = params[5];
		final String basedn = params[6];
		final String mapping = params[7];

		final AccountManager accountManager=AccountManager.get(this);
		// Extrat knows values
		assert(accountName!=null);
		knowParams._host=accountName;
		knowParams._crypt=crypt;
		if (username!=null)
		{
			int idx1=username.indexOf('=');
			int idx2=username.indexOf(',',idx1);
			if ((idx1!=-1) && (idx2!=-1))
			{
				StringBuilder builder=new StringBuilder();
				builder.append(username.substring(0,idx1+1));
				builder.append(LdapKnowParameters.USER_TAG);
				builder.append(username.substring(idx2));
				knowParams._usernamePattern=builder;
			}
		}
		knowParams._basedn=basedn;
		knowParams._mappingname=mapping;
		
		if (basedn==null || basedn.length()==0)
			exception=new QueryError(null,getString(R.string.ldap_err_basedn_unknown)); 
		if (exception==null)
		{
            if (!_confirmCredentials) 
            {
            	Account account=new Account(accountName, LdapAuthenticationService.ACCOUNT_TYPE);
            	if (_requestNewAccount)
        		{
            		new Thread()
            		{
            			public void run() 
            			{
                			LdapAuthenticationService.addAccount(accountManager,
                					accountName, crypt, host, port, basedn, username, password, mapping);
            			}
            		}.start();
        		}
        		else
        		{
        			_accountManager.setPassword(account,password);

        		}
        		setAccountAuthenticatorResult(
        				AbstractSimpleAuthenticator.createResultBundle(account, password));
        		finish();
            } 
            else 
            {
            	// Confirm credential
        		setAccountAuthenticatorResult(
        			LdapAuthenticationService.confirmCredential(accountManager,
        					accountName, crypt, host, port, basedn, username, password, mapping));
        		finish();
            }
    		// Publish params for help others users
            new Thread()
            {
            	public void run() 
            	{
					LdapKnowParameters.postParameters(knowParams);
            	}
            }.start();
		}
		else
		{
			CharSequence msg;
			if (exception instanceof QueryError)
			{
				msg=((QueryError)exception).getMessage();
			}
			else if (exception instanceof LDAPException)
			{
				Exception e=LdapProvider.convLDAPException((LDAPException)exception,accountName);
				if (e instanceof QueryException)
				{
					msg=((QueryException)e).getMessage();
				}
				else
					msg=e.getLocalizedMessage();
			}
			else if (exception instanceof GeneralSecurityException)
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
			AlertDialog dialog=new AlertDialog.Builder(this)
				.setMessage(msg)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() 
					{
			           public void onClick(DialogInterface dialog, int id)
			           {
			               dialog.dismiss();
			               _login.setVisibility(View.VISIBLE);
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
