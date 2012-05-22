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

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.provider.ContactsContract;

public class MockVerySimpleAuthenticator extends AbstractAccountAuthenticator
{
	private static final String TOKEN="";
	private static final String PASSWORD=TOKEN;
	
	private final AccountManager _accountManager;

	public MockVerySimpleAuthenticator(Context context)
	{
		super(context);
		_accountManager = AccountManager.get(context);
	}
	
	@Override
	public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
			String authTokenType, String[] requiredFeatures, Bundle options)
			throws NetworkErrorException
	{
		  final String accountName=Application.DEFAULT_ACCOUNT_NAME;
		  final Account account = new Account(accountName,accountType);
		  try
		  {
			  _accountManager.addAccountExplicitly(account, PASSWORD, null);
			  final Bundle bundle = new Bundle();
			  bundle.putString(AccountManager.KEY_ACCOUNT_NAME, accountName);
			  bundle.putString(AccountManager.KEY_ACCOUNT_TYPE,accountType);
			  bundle.putString(AccountManager.KEY_AUTHTOKEN, TOKEN);
			  response.onResult(bundle);
			  if (Application.ACCOUNT_WITH_SYNC)
					ContentResolver.setSyncAutomatically(account, ContactsContract.AUTHORITY, true);
			  return bundle;
		  }
		  catch (Throwable e) // $codepro.audit.disable caughtExceptions
		  {
			e.printStackTrace();
			response.onError(AccountManager.ERROR_CODE_BAD_ARGUMENTS, e.getLocalizedMessage());
			return null;
		  }
	}

	@Override
	public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account,
			Bundle options) throws NetworkErrorException
	{
		final Bundle result=new Bundle();
		result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
		result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
		result.putString(AccountManager.KEY_AUTHTOKEN, TOKEN);
		result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT,true);
		response.onResult(result);
		return result;
	}

	@Override
	public Bundle editProperties(AccountAuthenticatorResponse response, String accountType)
	{
		final Bundle result = new Bundle();
		result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
		return result;
	}

	@Override
	public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
			String authTokenType, Bundle options) throws NetworkErrorException
	{
		final Bundle result=new Bundle();
		result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
		result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
		result.putString(AccountManager.KEY_AUTHTOKEN, TOKEN);
		result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT,true);
		response.onResult(result);
		return result;
	}

	@Override
	public String getAuthTokenLabel(String authTokenType)
	{
		if (authTokenType.equals(MockAuthenticationService.AUTHTOKEN_TYPE))
		{
			return "password";
		}
		return null;
	}

	@Override
	public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account,
			String[] features) throws NetworkErrorException
	{
		final Bundle result = new Bundle();
		result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
		return result;
	}

	@Override
	public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account,
			String authTokenType, Bundle options) throws NetworkErrorException
	{
		final Bundle result = new Bundle();
		result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
		return result;
	}

}
