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
package fr.prados.contacts.providers.ldap.generic;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Selection;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.text.util.Linkify.TransformFilter;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;

import fr.prados.contacts.Application;
import fr.prados.contacts.lib.R;
import fr.prados.contacts.providers.AbstractSimpleAuthenticator;
import fr.prados.contacts.providers.QueryError;
import fr.prados.contacts.providers.QueryException;
import fr.prados.contacts.providers.ldap.AbstractWizardActivity;
import fr.prados.contacts.providers.ldap.LdapAuthenticationService;
import fr.prados.contacts.providers.ldap.LdapKnowParameters;
import fr.prados.contacts.providers.ldap.LdapProvider;
import fr.prados.contacts.providers.ldap.PanelSwitcher;
import fr.prados.contacts.tools.CheckContext;
import fr.prados.contacts.tools.Eula;
import fr.prados.contacts.tools.LogMarket;
import fr.prados.contacts.tools.Update;


// BUG si abandon lors du login apres le wizard
public class GenericWizardActivity extends AbstractWizardActivity
{
	private PanelSwitcher _panels;
	// Page 1
	private EditText _accountName;
	private Button _nextp1;
	
	// Page 2
	private TextView _know;
	private TextView _labelQuickUsername;
	private EditText _quickUsername;
	private TextView _labelQuickPassword;
	private EditText _quickPassword;
	private ImageButton _nextp2;
	private Button  _donep2;
	
	// Page 3
	private TextView _labelHostName;
	private RadioGroup 	_crypt;
	private EditText _userName;
	private EditText _password;
	private TextView _labelBaseDN;
	private EditText _basedn;
	private EditText _mapping;
	private Button  _donep3;
	private String _usernameTag;
	
	private AlertDialog.Builder _alertBuilder;
	
	private void init()
	{
		final Intent intent = getIntent();
		AccountManager accountManager=_accountManager = AccountManager.get(this);
		Account account=(Account)intent.getExtras().get("account");
		if (account!=null)
		{
			_confirmCredentials = true;
			setTitle(R.string.wizard_update_label);
			final String crypt=accountManager.getUserData(account,LdapAuthenticationService.KEY_CRYPT);
			final String basedn=accountManager.getUserData(account,LdapAuthenticationService.KEY_BASEDN);
	    	final String username=accountManager.getUserData(account,LdapAuthenticationService.KEY_USERNAME);
	    	final String mapping=accountManager.getUserData(account,LdapAuthenticationService.KEY_MAPPING);

	    	// Populate Wizard
			_labelHostName.setText(account.name);
			_accountName.setText(account.name);
			_mapping.setText(mapping);
			
			// init values
			if ("ssl".equals(crypt))		((RadioButton)findViewById(R.id.account_ssl)).setChecked(true);
			else if ("tls".equals(crypt))	((RadioButton)findViewById(R.id.account_tls)).setChecked(true);
			else							((RadioButton)findViewById(R.id.account_normal)).setChecked(true);
			_basedn.setText(basedn);
			_userName.setText(username);
			if (_confirmCredentials)
			{
				_panels.show(2);
				_password.requestFocus();
				findViewById(R.id.btn_back_p3).setVisibility(View.GONE);
			}
		}
		else
		{
			setTitle(R.string.wizard_activity_label);
			_requestNewAccount = true;
		}
			
//		if (_accountName.getText().length()==0)
//		{
//			_nextp1.setEnabled(false);
//		}
	}

	private void propagateKnowParameters()
	{
		_know.setText((_knowParams!=null) ? R.string.account_know : R.string.account_know_password);
		if (_knowParams!=null)
		{
			int view=(_knowParams._usernamePattern!=null) ? View.VISIBLE : View.GONE;
			_know.setText((_knowParams!=null) ? R.string.account_know : R.string.account_know_password);
			_labelQuickUsername.setVisibility(view);
			_quickUsername.setVisibility(view);
			_labelQuickPassword.setVisibility(view);
			_quickPassword.setVisibility(view);
			_basedn.setText(_knowParams._basedn);
			if ("ssl".equals(_knowParams._crypt))		((RadioButton)findViewById(R.id.account_ssl)).setChecked(true);
			else if ("tls".equals(_knowParams._crypt))	((RadioButton)findViewById(R.id.account_tls)).setChecked(true);
			else										((RadioButton)findViewById(R.id.account_normal)).setChecked(true);
			if (_knowParams._usernamePattern!=null)
			{
				_userName.setText(_knowParams._usernamePattern.toString().replace(LdapKnowParameters.USER_TAG, _usernameTag));
			}
			_mapping.setText(_knowParams._mappingname);
			
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);

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
		
		setContentView(R.layout.wizard);

		_alertBuilder=new AlertDialog.Builder(GenericWizardActivity.this);

		_usernameTag=getString(R.string.account_setup_userid);
		
		// BUG: balculement en cours avec panels
		_panels=(PanelSwitcher)findViewById(R.id.wizard);
		
		// Page 1
		_accountName=(EditText)findViewById(R.id.ldap_server_name);
//		_accountName.setOnEditorActionListener(new TextView.OnEditorActionListener()
//		{
//
//			@Override
//			public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
//			{
//				_nextp1.setEnabled((((EditText)v).getText().length()>0));
//				return false;
//			}
//			
//		});
//		_accountName.setOnKeyListener(new View.OnKeyListener()
//		{
//			@Override
//			public boolean onKey(View v, int keyCode, KeyEvent event)
//			{
//				_nextp1.setEnabled((((EditText)v).getText().length()>0));
//				return false;
//			}
//		});
		_nextp1=(Button)findViewById(R.id.btn_next_p1);
		_nextp1.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				nextPage1();
			}

		});
		
		// Page 2
		_know=(TextView)findViewById(R.id.know);
		_labelQuickUsername=(TextView)findViewById(R.id.label_account_quick_username);
		_quickUsername=(EditText)findViewById(R.id.account_quick_username);
		_labelQuickPassword=(TextView)findViewById(R.id.label_account_quick_password);
		_quickPassword=(EditText)findViewById(R.id.account_quick_password);
		_nextp2=(ImageButton)findViewById(R.id.btn_next_p2);
		_nextp2.setOnClickListener(new View.OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				_password.setText(_quickPassword.getText());
				_userName.requestFocus();
				_panels.showNext();
			}
		});
		((Button)findViewById(R.id.btn_back_p2)).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				_password.setText(_quickPassword.getText());
				_panels.showPrevious();
			}
		});
		_donep2=(Button)findViewById(R.id.btn_done_p2);
		final View.OnClickListener doneListener=new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
 				final String accountName=_accountName.getText().toString();
				String host=accountName;
				String port=null;
				String crypt=getCrypt();
				String mappingName=_mapping.getText().toString();
				if (mappingName.startsWith("/"))
				{
					if (!new File(mappingName).exists())
					{
						_alertBuilder.setTitle(R.string.err_title);
						_alertBuilder.setMessage(R.string.err_mapping_file_not_found);
						_alertBuilder.create().show();
						_accountName.requestFocus();
						return;
					}
				}
				int idx=host.indexOf(':');
				if (idx==-1)
				{
					port=("".equals(crypt)) ? DEFAULT_PORT_LDAP : DEFAULT_PORT_LDAPS;
				}
				else
				{
					port=host.substring(idx+1);
					host=host.substring(0,idx);
				}
				final String basedn = _basedn.getText().toString().trim();
				String user=_userName.getText().toString().trim();
				if (_knowParams!=null)
				{
					String quickUser=_quickUsername.getText().toString().trim();
					idx=user.indexOf(_usernameTag);
					if (idx!=-1)
					{
						if (quickUser.length()!=0)
						{
							// User entern full LDAP id ?
							idx=user.lastIndexOf(',');
							if (idx!=-1 && quickUser.endsWith(user.substring(idx)))
							{
								user=quickUser;
							}
							else
							{
								// Inject quickUsername
								user=user.replace(_usernameTag, quickUser);
							}
						}
					}
				}
				if (_panels.getCurrentIndex()==1) _password.setText(_quickPassword.getText());
				String pass = _password.getText().toString();
				if (user.length()==0) user=null;
				final String password=pass;
				_panels.setVisibility(View.INVISIBLE);
				showDialog(DIALOG_PROGRESS);
				_asyncTryAuthent=new  AsyncTask<String, Void, Exception>()
				{
					private String[] _params;
					
					@Override
					public Exception doInBackground(final String... params)
					{
						_params=params;
						Exception ex=null;
						try
						{
							if (!_confirmCredentials)
							{
								String newbasedn=LdapAuthenticationService.onlineConfirmPassword(
										GenericWizardActivity.this,params);
								if ((params[6]==null) || (params[6].length()==0))
									params[6/*BASEDN*/]=newbasedn;
							}
							else
							{
								// Must invoke confirmCredential to indirectly invoke onResult, for remove the notification
								final AccountManager accountManager = AccountManager.get(Application.context);
								Bundle options=new Bundle();
								options.putString(AccountManager.KEY_PASSWORD, password);
								AccountManagerFuture<Bundle> futur=
									accountManager.confirmCredentials(new Account(accountName,LdapAuthenticationService.ACCOUNT_TYPE), options,GenericWizardActivity.this, null, null);							
								Bundle bundle=futur.getResult();
								if (bundle.getBoolean(AccountManager.KEY_BOOLEAN_RESULT))
								{
									if ((params[6]==null) || (params[6].length()==0))
										params[6/*BASEDN*/]=bundle.getString(LdapAuthenticationService.KEY_BASEDN);
								}
								else
								{
									ex=new LDAPException(ResultCode.AUTHORIZATION_DENIED);
								}
							}
						}
						catch (GeneralSecurityException e)
						{
							ex=e;
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
						_asyncTryAuthent=null;
						removeDialog(DIALOG_PROGRESS);
						if (isCancelled()) return;
						onAuthenticationResult(e,_params);
						_params=null;
					}
				}.execute(accountName,crypt,host,port,user,password,basedn,mappingName);
			}

		};
		_donep2.setOnClickListener(doneListener);
		
		// Page 3
		_labelHostName=(TextView)findViewById(R.id.label_hostname);
		_crypt=(RadioGroup)findViewById(R.id.ldap_crypt);
		_userName=(EditText)findViewById(R.id.account_username);
		_userName.setOnFocusChangeListener(new View.OnFocusChangeListener()
		{
			
			@Override
			public void onFocusChange(View v, boolean hasFocus)
			{
				EditText txt=(EditText)v;
				
				String val=txt.getText().toString();
				int idx=val.indexOf(_usernameTag);
				if (idx!=-1)
					Selection.setSelection(txt.getEditableText(),idx,idx+_usernameTag.length());			}
		});
		_password=(EditText)findViewById(R.id.account_password);
		_labelBaseDN=(TextView)findViewById(R.id.account_label_basedn);
		_basedn=(EditText)findViewById(R.id.account_basedn);
		_mapping=(EditText)findViewById(R.id.account_mapping);
		((ImageButton)findViewById(R.id.account_help)).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				showDialog(MAPPING_HELP);
			}
		});
		
		_donep3=(Button)findViewById(R.id.btn_done_p3);
		_donep3.setOnClickListener(doneListener);
		((Button)findViewById(R.id.btn_back_p3)).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				_quickPassword.setText(_password.getText());
				if (_knowParams!=null)
				{
					_panels.showPrevious();
				}
				else 
					_panels.showPrevious(0);
			}
		});

		init();
	}

	private void nextPage1()
	{
		_nextp1.setEnabled(false);
		_knowParams=null;
		new AsyncTask<String, Void, Boolean>()
		{

			@Override
			protected Boolean doInBackground(String... params)
			{
				try
				{
					// TODO: check if account already exist
					String name=params[0];
					String host;
					int idx=name.indexOf(':');
					if (idx!=-1)
						host=name.substring(0,idx);
					else
						host=name;
					// Check host name
					if (host.length()==0) throw new UnknownHostException();
					InetAddress.getByName(host);
					try
					{
						// And try to find parameters in remote database
						if ((!_confirmCredentials) && getIntent().getAction()==null)
							_knowParams=LdapKnowParameters.getParameters(name);
					}
					catch (Exception e)
					{
						// Ignore
						Log.i(TAG,"IO",e);
					}
					return true;
				}
				catch (UnknownHostException e)
				{
					Log.i(TAG,"host",e);
					return false;
				}
			}
			@Override
			public void onPostExecute(Boolean result)
			{
				_nextp1.setEnabled(true);
				if (result)
				{
					  Animation animation = AnimationUtils.loadAnimation(GenericWizardActivity.this,android.R.anim.slide_in_left);
					  findViewById(R.id.page1).startAnimation(animation);
					  _labelHostName.setText(_accountName.getText());
					  _accountName.setText(_accountName.getText());

					  propagateKnowParameters();
					  
					  if (_knowParams!=null)
					  {
						  _quickUsername.requestFocus();
						  _panels.showNext();
					  }
					  else
					  {
						  _userName.requestFocus();
						  _panels.showNext(2);
					  }
				}
				else
				{
					Log.w(TAG,_accountName.getText()+" not found");
					_alertBuilder.setTitle(R.string.err_title);
					_alertBuilder.setMessage(R.string.err_host_not_found);
					_alertBuilder.create().show();
					_accountName.requestFocus();
				}
			}
			
		}.execute(_accountName.getText().toString());
	}

	@Override
	protected void onRestoreInstanceState(final Bundle state)
	{
		super.onRestoreInstanceState(state);
		_panels.show(state.getInt("page"));
		_knowParams=state.getParcelable("knowparams");
		if (_accountName.length()!=0)
			_nextp1.setEnabled(true);
	}
	
	@Override
	protected void onSaveInstanceState(final Bundle state)
	{
		super.onSaveInstanceState(state);
		if (_panels==null) return;
		state.putInt("page", _panels.getCurrentIndex());
		state.putParcelable("knowparams", _knowParams);
	}
	
	private static final int DIALOG_PROGRESS=1;
	private static final int MAPPING_HELP=2;
	@Override
	protected Dialog onCreateDialog(int id)
	{
		switch (id)
		{
			case DIALOG_PROGRESS:
				ProgressDialog dlg=ProgressDialog.show(
					this, "", getString(R.string.ldap_authenticating), true, false,
					new DialogInterface.OnCancelListener()
					{
						@Override
						public void onCancel(DialogInterface dialog)
						{
							if (_asyncTryAuthent!=null)
								_asyncTryAuthent.cancel(false);
							_panels.setVisibility(View.VISIBLE);
						}
					});
				dlg.setCancelable(true);
				dlg.setCanceledOnTouchOutside(false);
				return dlg;
				
			case MAPPING_HELP:
				AlertDialog.Builder builder=new AlertDialog.Builder(this);
				builder.setTitle(R.string.help_title);
				builder.setCancelable(true);
				builder.setPositiveButton(R.string.help_done, 
						new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int which)
							{
							}
						});
//				builder.setMessage(R.string.help_body);
				final TextView message = new TextView(this);
				SpannableString s=new SpannableString(this.getText(R.string.help_body));
				//Linkify.addLinks(s, Linkify.WEB_URLS);
				
			    Linkify.addLinks(s, 
			    		Pattern.compile(getResources().getText(R.string.help_body_link).toString()), 
			    		"http://code.google.com/p/contactsinline/source/browse/trunk/" 
			    		,null,new TransformFilter()
						{
							
							@Override
							public String transformUrl(Matcher paramMatcher, String paramString)
							{
								return "ContactsInlineLDAPBase/assets/standard.xml";
							}
						});
				message.setText(s);
				message.setMovementMethod(LinkMovementMethod.getInstance());
				builder.setView(message);
				Dialog rc= builder.create();
				return rc;
				
			default:
				LogMarket.wtf(TAG, "Invalide dialog id "+id);
				return null;
		}
	}
	
	private String getCrypt()
	{
		String crypt="";
		switch(_crypt.getCheckedRadioButtonId())
		{
			case R.id.account_ssl:
				crypt="ssl";
				break;
			case R.id.account_tls:
				crypt="tls";
				break;
		}
		return crypt;
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
		
		if (basedn.length()==0)
			exception=new QueryError(null,Application.context.getString(R.string.ldap_err_basedn_unknown)); 
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
                			LdapAuthenticationService.addAccount(accountManager,accountName, crypt, host, port, basedn, username, password, mapping);
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
        			LdapAuthenticationService.confirmCredential(accountManager,accountName, crypt, host, port, basedn, username, password, mapping));
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
			               _panels.setVisibility(View.VISIBLE);
			               _labelBaseDN.setText(R.string.account_setup_basedn_label);
				           _basedn.requestFocus();
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
