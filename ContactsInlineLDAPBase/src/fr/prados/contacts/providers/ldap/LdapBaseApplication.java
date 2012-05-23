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

import java.util.Properties;

import fr.prados.contacts.Application;
import com.unboundid.util.Debug;
import static fr.prados.contacts.Constants.*;
public class LdapBaseApplication extends Application
{
	public static final boolean TRACE_LDAP=false;/*Application.DEBUG*/;
	@Override
	public void onCreate()
	{
		super.onCreate();
		Properties prop=new Properties();
		if (TRACE_LDAP)
		{
			prop.setProperty(Debug.PROPERTY_DEBUG_ENABLED,"true");
			prop.setProperty(Debug.PROPERTY_DEBUG_LEVEL,"ALL");
		}
		else
		{
			prop.setProperty(Debug.PROPERTY_DEBUG_ENABLED,String.valueOf(DEBUG));
			prop.setProperty(Debug.PROPERTY_DEBUG_LEVEL,"OFF");
		}
		Debug.initialize(prop);
	}
}
