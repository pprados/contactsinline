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
package fr.prados.contacts;

import java.util.Date;
import java.util.Random;

import android.os.Debug;
import fr.prados.contacts.lib.BuildConfig;

public class Constants
{
	public static final String TAG="Application";

	public static boolean DEBUG=BuildConfig.DEBUG;
	public static boolean EMULATOR=false;

	public static final boolean E=BuildConfig.DEBUG;
	public static final boolean W=E;
	public static final boolean I=W;
	public static final boolean D=Debug.isDebuggerConnected();
	public static final boolean V=false;
	public static final boolean DUMP=false;

	// Request photo ?
	public static final boolean PHOTO=true;
	// Detect face to optimize the center of photo ?
	public static final boolean FACE_DETECTOR=true;
	// For debug, show where detect face
	public static final boolean FACE_SHOW=false;
	
	// Re-request the previous one if the process was killed
	public static final boolean REQUERY_AFTER_KILL=true;
	
	// Invoque one request at a time or multiple request at the same time.
	public static final boolean SYNC_SEARCH=false;
	
	// Number of thread to load images.
	public static final int IMAGE_POOL_SIZE=3;
	// Thread priority to load images
	public static final int IMAGE_POOL_PRIORITY=Thread.MIN_PRIORITY;
	
	// Add a request to resolve link to addr ?
	public static final boolean DNTOADDR=false;
	// Add a request to resolve link to name ?
	public static final boolean DNTONAME=false;
	// Sometime the request when DN to resolve name generate a error. Retry ?
	public static final int NB_RETRY_DNTO=2;
	
	/** If false, try first, and only if error, try others forms. */
	public static final boolean USE_PRE_CONNECTION=true;
	public static final Date EXPIRED_BETA=null; //new Date(111,5,1,0,0,0);
	/** Maximum life time for a cached request. */
	public static final long CACHE_TIMEOUT=3*60*1000L; // 3mn
	/** Maximum number of last request in cache. */
	public static final int MAX_LRU=10;
	/** Generate fake exception or null. */
	public static final Random FAKE_ERROR=null; //new Random(); // Ok null
	/** How % to generate fake error. */
	public static final int FAKE_ERROR_PROBABILITY=2;	
	// Limit the number of record ?
	public static final boolean RECORD_LIMIT=true;


}
