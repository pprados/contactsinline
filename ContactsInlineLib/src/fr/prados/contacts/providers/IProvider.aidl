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

import fr.prados.contacts.VolatileContact;
import fr.prados.contacts.VolatileRawContact;
import fr.prados.contacts.ContactId;
import fr.prados.contacts.providers.ResultsAndExceptions;
import fr.prados.contacts.providers.TransportException;

interface IProvider
{
	void onCreate(in int version,in long i);
	void onStart();
    void onStop();
	ResultsAndExceptions queryContact(in String accountName,in String selection,in String selectionArg);
	byte[] getAccountPhoto(in ContactId id);
	VolatileContact getVolatileContact(in ContactId id);
    void signalCanceled(in String accountName);
    void resetCanceled();
    boolean isCanceled();
}