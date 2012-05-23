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
import android.content.Context;
import android.os.Bundle;
import fr.prados.contacts.providers.AbstractSimpleAuthenticator;

public class MockAuthenticator extends AbstractSimpleAuthenticator
{
	/* package */public static final String KEY_USERNAME="username";
	public MockAuthenticator(Context context)
	{
		super(context, MockWizardActivity.class);
	}

	@Override
	protected void checkOnlineAccount(Bundle result, Account account, String password)
			throws GeneralSecurityException
	{
		// Mock accept all account ?
		if (!"password".equals(password))
			throw new GeneralSecurityException(_context.getString(R.string.err_authent));
	}

}
