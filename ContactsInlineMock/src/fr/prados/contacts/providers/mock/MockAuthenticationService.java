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

import android.accounts.AbstractAccountAuthenticator;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;


public class MockAuthenticationService extends Service
{
	public static final String AUTHTOKEN_TYPE = "password";

	@Override
	public void onCreate()
	{
		if (Application.ACCOUNT_WITH_PASSWORD)
		{
			// MockAuthenticator for account with wizard
			_authenticator = new MockAuthenticator(this);
		}
		else
		{
			// MockVerySimpleAuthenticator for account without user/password and wizard
			_authenticator = new MockVerySimpleAuthenticator(this);
		}
		
		super.onCreate();
	}
	@Override
	public IBinder onBind(Intent intent)
	{
		 return _authenticator.getIBinder();
	}

	private AbstractAccountAuthenticator _authenticator;
	
	public static void onlineConfirmPassword(String username,String password)
		throws GeneralSecurityException
	{
		if (!"password".equalsIgnoreCase(password))
			throw new GeneralSecurityException("Bad password");
	}
}
