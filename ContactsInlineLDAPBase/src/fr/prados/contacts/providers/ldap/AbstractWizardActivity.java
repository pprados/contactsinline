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

import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.os.AsyncTask;

public class AbstractWizardActivity extends AccountAuthenticatorActivity
{
	protected static final String TAG = "LDAP";
	protected static final String DEFAULT_PORT_LDAP="389";
	protected static final String DEFAULT_PORT_LDAPS="636";

	/**
	 * If set we are just checking that the user knows their credentials; this
	 * doesn't cause the user's password to be changed on the device.
	 */
	protected boolean _confirmCredentials = false;

	/** Was the original caller asking for an entirely new account? */
	protected boolean _requestNewAccount = false;

	protected AccountManager _accountManager;
	
	protected LdapKnowParameters _knowParams;
	protected volatile AsyncTask<?,?,?> _asyncTryAuthent;
	
	protected String injectUsername(LdapKnowParameters params,String username)
	{
		return _knowParams._usernamePattern.toString().replace(LdapKnowParameters.USER_TAG, username);
	}
	protected String extractUsername(String username)
	{
		int start=username.indexOf('=');
		int end=username.indexOf(',',start);
		return username.substring(start+1,end);
	}
	
}
