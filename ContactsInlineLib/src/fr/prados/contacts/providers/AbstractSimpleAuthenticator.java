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

import static fr.prados.contacts.Constants.D;
import static fr.prados.contacts.Constants.E;
import static fr.prados.contacts.Constants.W;

import java.security.GeneralSecurityException;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import fr.prados.contacts.Application;
import fr.prados.contacts.lib.R;

public abstract class AbstractSimpleAuthenticator extends AbstractAccountAuthenticator
{
	private static final String TAG = "Auth";
	private static final String PARAM_ACCOUNT_TYPE		= "accountType";
	public static final String PARAM_AUTHTOKEN_TYPE 	= "authtokenType";
	public static final String AUTHTOKEN_TYPE = "password";


	public static AccountManager _accountManager;
	protected Context _context;
	protected Class<?> _activityClass;
	
	public AbstractSimpleAuthenticator(Context context,Class<?> activityClass)
	{
		super(context);
		_context=context;
		_activityClass=activityClass;
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Bundle editProperties(AccountAuthenticatorResponse response,String accountType)
	{
		// Create intent to launch the Wizard, with response
		final Bundle result=new Bundle();
		final Intent intent = new Intent(_context,_activityClass);
		intent.putExtra(PARAM_ACCOUNT_TYPE, accountType);
		intent.putExtra(PARAM_AUTHTOKEN_TYPE, AUTHTOKEN_TYPE);
		intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
		result.putParcelable(AccountManager.KEY_INTENT, intent);
		return result;
	}
	protected Bundle editAccount(AccountAuthenticatorResponse response,Account account)
	{
		// Create intent to launch the Wizard, with response
		final Bundle result=new Bundle();
		final Intent intent = new Intent(_context,_activityClass);
		intent.putExtra("account", account);
		intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
		result.putParcelable(AccountManager.KEY_INTENT, intent);
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Bundle addAccount(AccountAuthenticatorResponse response,
			String accountType, String authTokenType,
			String[] requiredFeatures, Bundle options)
	{
		return editProperties(response, accountType);
	}

	@Override
	public Bundle confirmCredentials(AccountAuthenticatorResponse response,Account account, Bundle options)
	{
		Bundle result=null;
		// confirm with a specific password ?
		if (options != null && options.containsKey(AccountManager.KEY_PASSWORD))
		{
			final String password = options.getString(AccountManager.KEY_PASSWORD);
			try
			{
				result = new Bundle();
				if (password==null)
					throw new GeneralSecurityException();
				checkOnlineAccount(result,account,password);
				_accountManager.clearPassword(account);
				result.putString(AccountManager.KEY_AUTHTOKEN,password);
				result.putString(AccountManager.KEY_ACCOUNT_NAME,account.name);
				result.putString(AccountManager.KEY_ACCOUNT_TYPE,account.type);
				result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
				response.onResult(result);
			}
			catch (GeneralSecurityException e)
			{
				if (E) Log.e(TAG,e.getMessage());
				_accountManager.invalidateAuthToken(account.type, password);
				_accountManager.clearPassword(account);
				result=editAccount(response,account);
				result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
				result.putString(AccountManager.KEY_AUTH_FAILED_MESSAGE, _context.getString(R.string.err_authent));
				response.onResult(result);
			}
			catch (Exception e)
			{
				result = new Bundle();
				result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
				result.putString(AccountManager.KEY_ERROR_MESSAGE,e.getMessage());
				response.onError(AccountManager.ERROR_CODE_NETWORK_ERROR, _context.getString(R.string.err_unknown));
			}
		}
		return result;
	}

	/**
	 * {@inheritDoc}
	 * Invoked only if the cache is empty.
	 */
	@Override
	public Bundle getAuthToken(AccountAuthenticatorResponse response,
			Account account, String authTokenType, Bundle loginOptions)
	{
		Bundle result=new Bundle();
		String password=null;
		try
		{
			if (D) Log.d(TAG,"getAuthToken");
			password = _accountManager.getPassword(account);
			if (password==null)
				throw new GeneralSecurityException("Remember is an invalide password");
			if (!authTokenType.equals(AUTHTOKEN_TYPE))
			{
				result.putString(AccountManager.KEY_ERROR_MESSAGE, 
						Application.context.getString(R.string.err_invalide_token_type));
				return result;
			}
			
			checkOnlineAccount(result,account,password);
			result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
			result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
			result.putString(AccountManager.KEY_AUTHTOKEN, password); // The token
			result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT,true);
			response.onResult(result);
		}
		catch (GeneralSecurityException e)
		{
			if (W) Log.w(TAG,"getAuthToken",e);
			// the password was incorrect, return an Intent to an
			// Activity that will prompt the user for the password.
			if (password!=null) 
			{
				_accountManager.invalidateAuthToken(account.type, password);
				_accountManager.clearPassword(account);
			}
			result=editAccount(response,account);
			result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT,false);
			response.onResult(result);
		}
		catch (Exception e)
		{
			// the connexion was incorrect
			if (E) Log.e(TAG,"Error when logging",e);
			response.onError(AccountManager.ERROR_CODE_NETWORK_ERROR, _context.getString(R.string.err_unknown));
			result=new Bundle();
			result.putString(AccountManager.KEY_ERROR_MESSAGE,e.getMessage());
			result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT,false);
		}
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAuthTokenLabel(String authTokenType)
	{
		if (authTokenType.equals(AUTHTOKEN_TYPE))
		{
			return _context.getString(R.string.password_label);
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Bundle hasFeatures(AccountAuthenticatorResponse response,Account account, String[] features)
	{
		final Bundle result = new Bundle();
		result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
		return result;
	}

	/**
	 * It's possible to update the remote ldap password.
	 */
	@Override
	public Bundle updateCredentials(AccountAuthenticatorResponse response,
			Account account, String authTokenType, Bundle loginOptions)
	{
		final Bundle result = new Bundle();
		result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
		return result;
	}

	public static void invalideCredential(Account account,String password)
	{
		if (_accountManager==null)
			return;
		_accountManager.invalidateAuthToken(account.type, password);
		_accountManager.clearPassword(account);
		// Re-confirm password for generate a notification
		try
		{
			_accountManager.blockingGetAuthToken(account, AUTHTOKEN_TYPE, true);
		}
		catch (Exception e)
		{
			// Ignore
			if (W) Log.w(TAG,"generate notif",e);
		}
	}
	
	public static Bundle createResultBundle(Account account,String password)
	{
		Bundle extra=new Bundle();
		extra.putString(AccountManager.KEY_ACCOUNT_NAME,account.name);
		extra.putString(AccountManager.KEY_ACCOUNT_TYPE,account.type);
		extra.putString(AccountManager.KEY_AUTHTOKEN, password);
		extra.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
		return extra;
	}
	
	/**
	 * Check authentication.
	 * 
	 * @param result For add some values
	 * @param account Account to use
	 * @throws GeneralSecurityException If authentication error
	 * @throws Exception otherwise
	 */
	protected abstract void checkOnlineAccount(Bundle result,Account account,String password) throws GeneralSecurityException,Exception;

}
