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

import static fr.prados.contacts.Constants.W;

import java.security.GeneralSecurityException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.util.Log;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;

import fr.prados.contacts.providers.AbstractSimpleAuthenticator;
import fr.prados.contacts.tools.TOAPhoneNumberFormats;

/**
 * Service to handle Account authentication. It instantiates the authenticator
 * and returns its IBinder.
 */
public class LdapAuthenticationService extends Service
{
	private static final String TAG="LDAPAuth";
	
	public static String ACCOUNT_TYPE;
	/* package */static final String PARAM_ACCOUNT 	= "ldap.account";
	/* package */static final String PARAM_CRYPT 		= "ldap.crypt";
	/* package */static final String PARAM_HOST 		= "ldap.host";
	/* package */static final String PARAM_PORT 		= "ldap.port";
	/* package */static final String PARAM_USERNAME 	= "ldap.username";
	/* package */static final String PARAM_BASEDN 		= "ldap.basedn";
	/* package */static final String PARAM_MAPPING 	= "ldap.mapping";
	/* package */static final String PARAM_MCC	 		= "ldap.mcc";

	/* package */public static final String KEY_CRYPT=PARAM_CRYPT;
	/* package */public static final String KEY_HOST=PARAM_HOST;
	/* package */public static final String KEY_PORT=PARAM_PORT;
	/* package */public static final String KEY_BASEDN=PARAM_BASEDN;
	/* package */public static final String KEY_MAPPING=PARAM_MAPPING;
	/* package */public static final String KEY_USERNAME=PARAM_USERNAME;
	/* package */public static final String KEY_MCC=PARAM_MCC;

	public static LDAPAuthenticator _authenticator;
	
	@Override
	public IBinder onBind(Intent intent)
	{
		return _authenticator.getIBinder();
	}

	public static abstract class LDAPAuthenticator extends AbstractSimpleAuthenticator
	{
		public LDAPAuthenticator(Context context,Class<?> activityClass)
		{
			super(context,activityClass);
		}

		@Override
		protected void checkOnlineAccount(Bundle result,Account account,String password) throws GeneralSecurityException,Exception
		{
			String[] params=new String[7];
			params[0]=account.name;
			params[1]=_accountManager.getUserData(account,KEY_CRYPT);
			params[2]=_accountManager.getUserData(account,LdapAuthenticationService.KEY_HOST);
			params[3]=_accountManager.getUserData(account,LdapAuthenticationService.KEY_PORT);
			params[4]=_accountManager.getUserData(account, KEY_USERNAME);
			params[5]=password;
			String basedn=_accountManager.getUserData(account,KEY_BASEDN);
			String nbasedn=onlineConfirmPassword(_context,params);
			if (basedn==null)
				basedn=nbasedn;
			if (basedn==null) basedn="";
			params[6]=basedn;
			result.putString(KEY_BASEDN,basedn);
		}
		
	}

	// -----------------------
	/** Use for check connection when confirm a password. */
	private static final int DEFAULT_CONNECTION_TIMEOUT=30000;
	/** Use for check connection when confirm a password. */
	private static final int DEFAULT_MESSAGE_SIZE=10000;

	public static String onlineConfirmPassword(Context context,String... params) throws LDAPException, GeneralSecurityException
	{
		try
		{
			final String crypt = params[1];
			final String host = params[2];
			
			final int port = Integer.parseInt(params[3]);
			final String username = params[4];
			final String password = params[5];
			String basedn = params[6];
			LDAPConnection conn=LdapProvider.getConnection(crypt,host, port, username, password,
				DEFAULT_CONNECTION_TIMEOUT,DEFAULT_MESSAGE_SIZE);
			if (basedn==null || basedn.length()==0)
			{
				int cntNumber=0;
				for (int i=0;i<host.length();++i)
				{
					if (Character.isDigit(host.charAt(i)))
						++cntNumber;
				}
				// Enought char but not digit ?
				if ((cntNumber*100/host.length()*100)<60)
				{
					// Keep only last two parts
					StringBuilder builder=new StringBuilder();
					int lastidx=host.lastIndexOf('.');
					int idx=host.substring(0,lastidx).lastIndexOf('.');
					builder.append("dc=").append(host.substring(idx+1,lastidx))
						.append(",dc=").append(host.substring(lastidx+1));
					String proposeddn=builder.toString();
					if ((conn.getRootDSE()!=null) && (conn.getRootDSE().getAttributeValues("namingContexts")!=null))
					{
						for (String dn:conn.getRootDSE().getAttributeValues("namingContexts"))
						{
							try
							{
								conn.getEntry(dn);
								// Have access and...
								// Assertion 1: same end with username ?
								if ((username!=null) && (username.endsWith(dn)))
								{
									basedn=dn;
									break;
								}
								// Assertion 2: end with similare dc ?
								if (dn.endsWith(proposeddn))
								{
									basedn=dn;
									break;
								}
								// Assertion 3: Priority for first
								if (basedn==null)
									basedn=dn;
							}
							catch (LDAPException e)
							{
								// Ignore and continue
								if (W) Log.w(TAG,"confirm password",e);
							}
						}
					}
				}
			}
			conn.close();
			return basedn;
		}
		catch (LDAPException e)
		{
			if (W) Log.w(TAG,e);
			if (
		    		ResultCode.AUTHORIZATION_DENIED_INT_VALUE==e.getResultCode().intValue()
		    		|| ResultCode.INVALID_CREDENTIALS_INT_VALUE==e.getResultCode().intValue()
				)
			{
				throw new GeneralSecurityException();
			}
			throw (LDAPException)e.fillInStackTrace();
		}
	}
	/**
	 * Authentification is completed. Add new account with all informations.
	 * 
	 * @param accountName
	 * @param crypt
	 * @param host
	 * @param port
	 * @param basedn
	 * @param username
	 * @param password
	 * @param mapping
	 */
	public static void addAccount(
			final AccountManager accountManager,
			final String accountName,
			final String crypt,
			final String host,
			final String port,
			final String basedn,
			final String username,
			final String password,
			final String mapping)
	{
		new AsyncTask<Void,Void,Integer>()
		{
			@Override
			protected Integer doInBackground(Void... params)
			{
				// TODO Auto-generated method stub
				return TOAPhoneNumberFormats.getServerMCC(host);
			}
			@Override
			protected void onPostExecute(Integer result)
			{
				// TODO Auto-generated method stub
				super.onPostExecute(result);
				final Account account = new Account(accountName, ACCOUNT_TYPE);
				Bundle extra=new Bundle();        	
				extra.putString(KEY_CRYPT, crypt);
				extra.putString(KEY_HOST, host);
				extra.putString(KEY_PORT, port);
				extra.putString(KEY_BASEDN, basedn);
				extra.putString(KEY_USERNAME,username);
				extra.putString(KEY_MAPPING,mapping);
				extra.putString(KEY_MCC,String.valueOf(result));
				accountManager.addAccountExplicitly(account, password, extra);
				accountManager.setPassword(account, password);
				// Set contacts sync for this account.
				ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);
			}
		}.execute();
	}
	
	/**
	 * Confirm credential is completed. Update all informations.
	 * 
	 * @param accountName
	 * @param crypt
	 * @param host
	 * @param port
	 * @param basedn
	 * @param username
	 * @param password
	 * @param mapping
	 * @return
	 */
	public static Bundle confirmCredential(AccountManager accountManager,String accountName,String crypt,String host,String port,String basedn,String username,String password,String mapping)
	{
		final Account account = new Account(accountName, ACCOUNT_TYPE);
    	// Confirm credential
		accountManager.setUserData(account,KEY_CRYPT,crypt);
		accountManager.setUserData(account,KEY_HOST,host);
		accountManager.setUserData(account,KEY_PORT,port);
		accountManager.setUserData(account,KEY_BASEDN,basedn);
		accountManager.setUserData(account,KEY_USERNAME,username);
		accountManager.setUserData(account,LdapAuthenticationService.KEY_MAPPING,mapping);
		accountManager.setUserData(account,LdapAuthenticationService.KEY_MCC,String.valueOf(TOAPhoneNumberFormats.getServerMCC(host)));
		accountManager.setPassword(account, password);
		final Bundle extra=new Bundle();
		extra.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
		return extra;
	}
}
