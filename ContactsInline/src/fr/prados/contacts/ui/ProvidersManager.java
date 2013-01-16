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
package fr.prados.contacts.ui;

import static fr.prados.contacts.Constants.CACHE_TIMEOUT;
import static fr.prados.contacts.Constants.D;
import static fr.prados.contacts.Constants.DEBUG;
import static fr.prados.contacts.Constants.E;
import static fr.prados.contacts.Constants.EMULATOR;
import static fr.prados.contacts.Constants.I;
import static fr.prados.contacts.Constants.MAX_LRU;
import static fr.prados.contacts.Constants.V;
import static fr.prados.contacts.Constants.W;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

import com.flurry.android.FlurryAgent;

import fr.prados.contacts.Application;
import fr.prados.contacts.ContactId;
import fr.prados.contacts.R;
import fr.prados.contacts.VolatileContact;
import fr.prados.contacts.VolatileContact.Copy;
import fr.prados.contacts.VolatileContact.Import;
import fr.prados.contacts.VolatileData;
import fr.prados.contacts.VolatileRawContact;
import fr.prados.contacts.providers.IProvider;
import fr.prados.contacts.providers.Provider.OnQuery;
import fr.prados.contacts.providers.QueryError;
import fr.prados.contacts.providers.QueryWarning;
import fr.prados.contacts.providers.ResultsAndExceptions;
import fr.prados.contacts.test.Dump;
import fr.prados.contacts.tools.LogMarket;

public final class ProvidersManager
{
	final static String TAG="ProvidersManager";
	
	private static boolean _isInit;
	static final String ACTION=IProvider.class.getName();
	private static Intent _intentAction=new Intent(ACTION);
	private static final int VERSION=1; // Compatibility version to contact provider
	private static AccountManager _manager;
	private static final Map<String,IProvider> _drivers=Collections.synchronizedMap(new HashMap<String,IProvider>());

	private static final String[] MAPPING_IDS=new String[]{RawContacts._ID,RawContacts.CONTACT_ID};
	private static final String[] MAPPING_ID=new String[]{RawContacts._ID};
	private static AtomicInteger _waitInit=new AtomicInteger(9999);
	
	public static boolean isEmpty()
	{
		return _drivers.isEmpty();
	}
	
	public static boolean waitInit()
	{
		final long start=System.currentTimeMillis();
		final int v=_waitInit.get();
		if (v==0) 
		{
			return true;
		}
		if (D) Log.d(TAG,"is not zero ("+_waitInit.get()+")");
		while (_waitInit.get()>0)
		{
			try
			{
				if (D) Log.d(TAG,"wait init...");
				synchronized(_waitInit)
				{
					_waitInit.wait(2000);
				}
			}
			catch (InterruptedException e)
			{
				// Ignore
				if (W) Log.w(TAG,"wait init",e);
				_waitInit.decrementAndGet();
			}
			if (D) Log.d(TAG,"wait init get="+_waitInit.get());
			if (System.currentTimeMillis()-start>10000)
			{
				if (E) Log.e(TAG,"Impossible to bind providers");
				break;
			}
		}
		if (D) Log.d(TAG,"exit with "+_waitInit.get());
		return _waitInit.get()==0;
	}
	
	/**
	 * Initiale connection to all drivers.
	 * @param context
	 */
	private static synchronized void initPlugin()
	{
		if (D) Log.d(TAG,"startup...");
		final Context context=Application.context;
		_manager=AccountManager.get(Application.context);

		_drivers.clear();
		final PackageManager pm=context.getPackageManager();
		final Intent action=_intentAction;
		final List<ResolveInfo> list=pm.queryIntentServices(action, 0/*PackageManager.GET_META_DATA*/);
		if (D) Log.d(TAG,list.size()+" plugins providers found.");
		_waitInit.set(list.size());
		synchronized (_waitInit)
		{
			_waitInit.notifyAll();
		}
		int id=0;
		for (ResolveInfo info:list)
		{
			if (D) Log.d(TAG, "Found plugin " + info);
			
			final int theId=++id;
			final ServiceInfo serviceInfo = info.serviceInfo;
            if (serviceInfo == null) continue; 
			final Intent intent=new Intent(ACTION);
			intent.setClassName(serviceInfo.packageName, serviceInfo.name);

			final boolean rc=context.bindService(intent, 
					new ServiceConnection()
					{
						private IProvider _driver;
						@Override
						public void onServiceConnected(ComponentName className, IBinder service)
						{
							try 
							{
								if (D) Log.d(TAG,"onServiceConnected");
								_driver=IProvider.Stub.asInterface(service);
								_driver.onCreate(VERSION,theId*(0x10000L)); // Init le id generator
								_drivers.put(className.getPackageName(),_driver);
								_waitInit.decrementAndGet();
								synchronized(_waitInit)
								{
									_waitInit.notifyAll();
								}
							} 
							catch (RemoteException e) 
							{
								if (E) Log.e(TAG,"Impossible to register provider.",e);
							}
						}
			
						@Override
						public void onServiceDisconnected(ComponentName className)
						{
							if (D) Log.d(TAG,"onServiceDisconnected");
							_drivers.remove(_driver);
							_driver = null;
							context.bindService(intent,this, Context.BIND_AUTO_CREATE|Context.BIND_NOT_FOREGROUND);
						}
					}, 
					Context.BIND_AUTO_CREATE);
				if (E && !rc) Log.e(TAG,"Impossible to initialise drivers list.");
		}
		
	}
	public static synchronized void init()
	{
		if (!_isInit)
		{
			if (D) Log.d(TAG,"Init plugins");
			Cache.clear();
			initPlugin();
			if (D) Log.d(TAG,"wait wakeup...");
			if (waitInit())
			{
				_isInit=true;
			}
			else
			{
				if (E) Log.e(TAG,"Init error");
			}
			wakeup();
			if (D) Log.d(TAG,"Wakeup done");
		}
	}
	static abstract class CanceledAsync extends AsyncTask<Void, Void, ResultsAndExceptions>
	{
		/*package*/ IProvider _driver;
		/*package*/ String _accountName;
		public void propagateCancel()
		{
			try
			{
				if (_driver!=null)
					_driver.signalCanceled(_accountName);
			}
			catch (RemoteException e)
			{
				// Ignore
				if (W) Log.w(TAG,"cancel async",e);
			}
		}
			
	}
	
	
	private ProvidersManager(AccountManager manager)
	{
		_manager=manager;
	}

	public static synchronized Account[] getAccounts()
	{
		assert(_waitInit.get()<=0);
		final ArrayList<Account> a=new ArrayList<Account>(5);
		for (final String t:_drivers.keySet())
		{
			for (Account account:_manager.getAccountsByType(t))
				a.add(account);
		}
		return a.toArray(new Account[a.size()]);
	}
	public static boolean isAccountEmpty()
	{
		for (final String t:_drivers.keySet())
		{
			if (_manager.getAccountsByType(t).length!=0)
				return false;
		}
		return true;
	}
	

	private static IProvider getDriver(String accountType)
	{
		return _drivers.get(accountType);
	}
	
	public static void wakeup()
	{
		final ArrayList<IProvider> withError=new ArrayList<IProvider>(2);
		synchronized (_drivers)
		{
			for (IProvider driver:_drivers.values())
			{
				try
				{
					driver.onStart();
				}
				catch (RemoteException e)
				{
					withError.add(driver);
					if (W) Log.w(TAG,"wakeup",e);
				}
			}
			for (IProvider driver:withError)
				_drivers.remove(driver);
		}
			
	}
	public static void wakedown()
	{
		final ArrayList<IProvider> withError=new ArrayList<IProvider>(2);
		for (IProvider driver:_drivers.values())
		{
			try
			{
				driver.onStop();
			}
			catch (RemoteException e)
			{
				withError.add(driver);
				if (W) Log.w(TAG,"wakedone",e);
			}
		}
		for (IProvider driver:withError)
			_drivers.remove(driver);
			
	}
	public static synchronized void reset()
	{
		_isInit=false;
		init();
	}
	private static final List<CanceledAsync> _tasks=
		Collections.synchronizedList(new ArrayList<CanceledAsync>());
	private static String _currentRequest;
	
	public static void query(
		final Mode match,
		final OnQuery callback,
		final String[] projection,
		final String selection, 
		final String... selectionArgs)
	{
		final Context context=Application.context;
		final String mergedSelection=Cache.generateSelectionId(selection,selectionArgs);
		if (VolatileContactsListActivity.withFlurry)
		{
			final HashMap<String,String> map=new HashMap<String,String>();
			map.put("selection",selection);
			FlurryAgent.onEvent("query",map);
		}
		
		// Manage cache
		final ResultsAndExceptions result=Cache.get(mergedSelection);
		if ((result!=null) && (result.timeout>System.currentTimeMillis()))
		{
			for (int i=ProvidersManager.getAccounts().length-1;i>0;--i)
			{
				callback.onQueryComplete(result, false);
			}
			callback.onQueryComplete(result, true);
			return;
		}
		else
		{
			Cache.remove(mergedSelection);
		}

		try
		{
			final ResultsAndExceptions resultFinal=new ResultsAndExceptions();
			resultFinal.timeout=System.currentTimeMillis()+CACHE_TIMEOUT;
			final ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
			if (
					(cm!=null && (cm.getActiveNetworkInfo()==null || !cm.getActiveNetworkInfo().isConnected()))
				)
			{
				// No network, return error
				resultFinal.exceptions.add(new QueryError(null, context.getString(R.string.err_network_done)));
				callback.onQueryComplete(resultFinal, true);
				return;
			}
			// Add in cache before receive async result
			Cache.add(mergedSelection, resultFinal);
			_currentRequest=mergedSelection;
			if (resultFinal.contacts==null)
			{
				if (V) Log.v(TAG,"---- query...");
				final Account[] accounts=ProvidersManager.getAccounts();
				resultFinal.contacts=new ArrayList<VolatileContact>(10);
				final int len=accounts.length;
				final AtomicInteger jobs=new AtomicInteger(); // Count child thread
				jobs.set(len);
				// Cache is important, because else a infinite loop is started due to content update.
				for (int i=0;i<len;++i)
				{
					final Account account=accounts[i]; // $codepro.audit.disable variableDeclaredInLoop
					final String currentAccountName=account.name;
					class AsyncJob extends CanceledAsync
					{
						final AtomicInteger _jobs;
						AsyncJob(final AtomicInteger jobs)
						{
							_jobs=jobs;
							_accountName=currentAccountName;
						}
						@Override
						public ResultsAndExceptions doInBackground(Void...nil)
						{
							try
							{
								//if (D) Log.d(TAG,_jobs.get()+":"+currentAccountName+" query...");
								_driver=ProvidersManager.getDriver(account.type);
								final ResultsAndExceptions result=_driver.queryContact(_accountName, selection, selectionArgs[0]);
								assert(result!=null);
								if (result==null) return null;
								if (D && result.contacts!=null)
									Log.d(TAG,_jobs.get()+":"+_accountName+" found "+result.contacts.size()+" record(s)");
								// Add to data in cache
								synchronized (Cache.class)
								{
									Cache.mergeResults(resultFinal, result);
									resultFinal.cursor=convContactsToCursor(match,resultFinal.contacts,projection);
								}
								return resultFinal;
							}
							catch (DeadObjectException e)
							{
								if (E) Log.e(TAG,_jobs.get()+":"+_accountName+" Error when contact provider.",e);
								resultFinal.exceptions.add(new QueryError(_accountName,context.getString(R.string.err_provider)));
								ProvidersManager.reset();
								return resultFinal;
							}
							catch (Exception e) // $codepro.audit.disable caughtExceptions
							{
								if (E) Log.e(TAG,_jobs.get()+":"+_accountName+" Error when load contact.",e);
								resultFinal.exceptions.add(new QueryError(_accountName,e));
								return resultFinal;
							}
							catch (Throwable e) // $codepro.audit.disable caughtExceptions
							{
								LogMarket.wtf(TAG,"Error when load contact from provider "+_accountName,e);
								cancel(false);
								throw (Error)e.fillInStackTrace();
							}
						}
						@Override
						public void onPostExecute(ResultsAndExceptions r)
						{
							_tasks.remove(this);
							final int c=_jobs.decrementAndGet();
							if (isCancelled()) return;
							if (r==null) return;
							// If ! QueryWarning, remove from cache
							boolean find=false;
							for (Exception e:r.exceptions)
							{
								if (!(e instanceof QueryWarning))
								{
									find=true;
									break;
								}
							}
							if (find)
								Cache.remove(mergedSelection);
							callback.onQueryComplete(r,c==0);
						}
					}
					final AsyncJob job=new AsyncJob(jobs); // $codepro.audit.disable variableDeclaredInLoop
					_tasks.add(job);
					job.execute();
				}
			}
		}
		finally
		{
			_currentRequest=null;
		}
	}
	
	public static final int cancelQuery()
	{
		Cache.remove(_currentRequest);
		
		final List<CanceledAsync> copy;
		synchronized (_tasks)
		{
			copy=new ArrayList<CanceledAsync>(_tasks);
		}
		for (CanceledAsync task:copy)
		{
			task.propagateCancel();
			task.cancel(false);
			_tasks.remove(task);
		}
		return copy.size();
	}

	public static Bitmap getPhoto(ContactId id)
	{
		final VolatileContact contact=Cache.getVolatileContactByContactId(id);
		// Only image for contact in cache
		if (contact==null) return null;
		
		final VolatileRawContact rawcontact=contact.getRawContacts().get(0);
		byte[] jpeg;
		switch (rawcontact._withPhoto)
		{
			case VolatileRawContact.PHOTO_YES :
				jpeg=(byte[])rawcontact.get(Photo.CONTENT_ITEM_TYPE).getAttr(Photo.PHOTO);
				return BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
			case VolatileRawContact.PHOTO_NO :
				return null;
			case VolatileRawContact.PHOTO_UNKNWON :
				break;
			default:
				if (E) Log.e(TAG,"Default in getPhoto");
		}

		try
		{
			jpeg=ProvidersManager.getDriver(id.accountType.toString()).getAccountPhoto(id);
		}
		catch (RemoteException e)
		{
			if (W) Log.w(TAG,"getPhoto",e);
			jpeg=null;
		}
		if (jpeg==null)
		{
			rawcontact._withPhoto=VolatileRawContact.PHOTO_NO;
			return null;
		}
		else
		{
			final VolatileData data=new VolatileData();
			data.put(Photo.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
			data.put(Photo.PHOTO, jpeg);
			rawcontact.put(Photo.CONTENT_ITEM_TYPE,data);
			rawcontact._withPhoto=VolatileRawContact.PHOTO_YES;
			return BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
		}
	}
	
	
	public enum Mode { CONTACT,CONTACT_JOIN_PHONE,CONTACT_JOIN_POSTAL,RAWCONTACT,RAWCONTACT_ID,RAWCONTACT_ID_DATA};
	private static MatrixCursor convContactsToCursor(Mode mode,List<VolatileContact> contacts,String[] projection)
	{
		assert(projection!=null);
		final MatrixCursor cursor=new MatrixCursor(projection);
		switch (mode)
		{
			case CONTACT :
				synchronized (contacts)
				{
					final int length=projection.length;
					for (final VolatileContact contact:contacts)
					{
						Object[] columnValues=new Object[length];
						for (int i=0;i<length;++i)
						{
							final String proj=projection[i];
							if (BaseColumns._ID.equals(proj))
							{
								columnValues[i]=contact.getId();
							}
							else if (Contacts.DISPLAY_NAME.equals(proj))
							{
								columnValues[i]=contact.getDisplayName();
							}
							else if (RawContacts.ACCOUNT_TYPE.equals(proj))
							{
								columnValues[i]=contact.getRawContacts().get(0).getAttr(RawContacts.ACCOUNT_TYPE);
							}
							else if (RawContacts.ACCOUNT_NAME.equals(proj))
							{
								columnValues[i]=contact.getRawContacts().get(0).getAttr(RawContacts.ACCOUNT_NAME);
							}
							else if (VolatileRawContact.LOOKUP.equals(proj))
							{
								columnValues[i]=contact.getRawContacts().get(0).getAttr(VolatileRawContact.LOOKUP);
							}
							else
							{
								for (final VolatileRawContact rawContact:contact.getRawContacts())
								{
									if (Phone.TYPE.equals(proj))
									{
										if (columnValues[i]==null)
											columnValues[i]=rawContact.getPhoneType();
									}
									else if (Phone.NUMBER.equals(proj))
									{
										if (columnValues[i]==null)
											columnValues[i]=rawContact.getPhoneNumber();
									}
									else if (StructuredPostal.TYPE.equals(proj))
									{
										if (columnValues[i]==null)
											columnValues[i]=rawContact.getPostalType();
									}
									else if (StructuredPostal.FORMATTED_ADDRESS.equals(proj))
									{
										if (columnValues[i]==null)
											columnValues[i]=rawContact.getPostalFormatted();
									}
									else
										LogMarket.wtf(TAG, "unknown columns");
								}
							}
						}
						cursor.addRow(columnValues);
					}
				}
				return cursor;
				
			case CONTACT_JOIN_PHONE:
			case CONTACT_JOIN_POSTAL:
				synchronized (contacts)
				{
					assert(contacts!=null);
					final String filter=(mode==Mode.CONTACT_JOIN_PHONE) 
						? Phone.CONTENT_ITEM_TYPE
						: StructuredPostal.CONTENT_ITEM_TYPE;
					final int length=projection.length;
					for (final VolatileContact contact:contacts)
					{
						// Find the raw contact id
						for (VolatileRawContact raw:contact.getRawContacts())
						{
							for (final ArrayList<VolatileData> datas:raw._datas.values())
							{
								for (final VolatileData data:datas)
								{
									if (!filter.equals(data.getAttr(Data.MIMETYPE)))
										continue;
									Object[] columnValues=new Object[length];
									for (int i=0;i<length;++i)
									{
										final String proj=projection[i];
										if (BaseColumns._ID.equals(proj))
										{
											columnValues[i]=contact.getId();
										}
										else if (Contacts.DISPLAY_NAME.equals(proj))
										{
											columnValues[i]=contact.getDisplayName();
										}
										else if (RawContacts.ACCOUNT_TYPE.equals(proj))
										{
											columnValues[i]=contact.getRawContacts().get(0).getAttr(RawContacts.ACCOUNT_TYPE);
										}
										else if (RawContacts.ACCOUNT_NAME.equals(proj))
										{
											columnValues[i]=contact.getRawContacts().get(0).getAttr(RawContacts.ACCOUNT_NAME);
										}
										else if (VolatileRawContact.LOOKUP.equals(proj))
										{
											columnValues[i]=contact.getRawContacts().get(0).getAttr(VolatileRawContact.LOOKUP);
										}
										else if (StructuredPostal.RAW_CONTACT_ID.equals(proj))
										{
											columnValues[i]=contact.getId(); // Warning. It's contact id and not raw contact id
										}
										else 
										{
											columnValues[i]=data.getAttr(proj);
										}
										// Filter empty phone or postal address
										if (columnValues[i]==null)
										{
											if (((mode==Mode.CONTACT_JOIN_PHONE) && Phone.NUMBER.equals(proj))
													|| (StructuredPostal.FORMATTED_ADDRESS.equals(proj)))
											{
												columnValues=null;
												break;
											}
										}
									}
									if (columnValues!=null)
										cursor.addRow(columnValues);
								}
							}
						}
					}
				}
				return cursor;
			default:
				if (E) Log.e(TAG,"Default in convContactsToCursor");
				break;
		}
		return null;
	}
	
	//-------------
	static public final class Cache
	{

		private static final Map<CharSequence,ResultsAndExceptions> _cache=Collections.synchronizedMap(new HashMap<CharSequence,ResultsAndExceptions>());
		private static final Map<ContactId,WeakReference<VolatileContact>> _cacheByContactId=Collections.synchronizedMap(new HashMap<ContactId,WeakReference<VolatileContact>>());
		private static final Map<Long,WeakReference<VolatileContact>> _cacheByLongId=Collections.synchronizedMap(new HashMap<Long,WeakReference<VolatileContact>>());
		private static final List<ResultsAndExceptions> _lru=new ArrayList<ResultsAndExceptions>(MAX_LRU+1);
		
		private static synchronized void add(String mergedSelection,ResultsAndExceptions result)
		{
			if (D) Log.d(TAG,"Add request "+mergedSelection+" in cache.");
			_cache.put(mergedSelection, result);
			_lru.add(0,result);
			if (_lru.size()>MAX_LRU)
			{
				final int pos=_lru.size()-1;
				final ResultsAndExceptions last=_lru.get(pos);
				_lru.remove(pos);
				_cache.remove(last);
			}
		}
		private static synchronized void remove(String mergedSelection)
		{
			if (mergedSelection!=null)
				_lru.remove(_cache.remove(mergedSelection));
		}
		
		private static synchronized void mergeResults(ResultsAndExceptions resultFinal,ResultsAndExceptions result)
		{
			synchronized (resultFinal)
			{
				// Assume cache is not empty
				--resultFinal.pendingJob;
				if ((result!=null) && (result.contacts!=null))
				{
					// Aliment all direct caches
					for (VolatileContact contact:result.contacts)
					{
						final WeakReference<VolatileContact> weak=new WeakReference<VolatileContact>(contact);
						_cacheByContactId.put(contact.getContactId(), weak);
						_cacheByLongId.put(contact.getId(),weak);
					}
					resultFinal.contacts.addAll(result.contacts);
					if (resultFinal.contacts.size()>1)
						Collections.sort(resultFinal.contacts);
				}
				resultFinal.exceptions.addAll(result.exceptions); // Merge exceptions
			}
		}
		private static synchronized ResultsAndExceptions get(String mergedSelection)
		{
			if (D) Log.d(TAG,"get cached request for "+mergedSelection+"...");
			final ResultsAndExceptions pair=_cache.get(mergedSelection);
			if (pair==null)
			{
				if (D) Log.d(TAG,"Request "+mergedSelection+" is not in cache.");
				return null;
			}
			if (D) Log.d(TAG,"Request "+mergedSelection+" is now in cache.");
			_lru.remove(pair);
			_lru.add(0,pair);
			return pair;
		}
		
		/** Generate unique key for each request. */
		private static String generateSelectionId(String selection, String... selectionArgs)
		{
			if (selection==null) selection="";
			if (selectionArgs==null) return selection;
			final StringBuilder builder=new StringBuilder(30);
			builder.append(selection).append(':');
			int start=0;
			int cur;
			int idx=0;
			while ((cur=selection.indexOf('?',start))!=-1)
			{
				builder.append(selection.substring(start,cur));
				builder.append(selectionArgs[idx++]);
				start=cur+1;
			}
			builder.append(selection.substring(start));
			if (idx!=selectionArgs.length)
				throw new IllegalArgumentException();
			return builder.toString();
		}
		/** Clear all caches. */
		private static void clear()
		{
			synchronized (_cache)
			{
				_cache.clear();
				_cacheByLongId.clear();
				_cacheByContactId.clear();
			}
		}
		
		private static VolatileContact getVolatileContactsById(long id)
		{
			final WeakReference<VolatileContact> ref=_cacheByLongId.get(id);
			if (ref==null) return null;
			final VolatileContact rc=ref.get();
			if (rc==null)
			{
				_cacheByLongId.remove(id);
				return null;
			}
			return rc;
		}
	
		private static VolatileContact getVolatileContactByContactId(ContactId id)
		{
			final WeakReference<VolatileContact> ref=_cacheByContactId.get(id);
			if (ref==null) 
			{
				VolatileContact rc=null;
				try
				{
					if (W) Log.w(TAG,"getVolatileContactByContactId("+id+") return null !");
					waitInit();
					final IProvider driver=getDriver(id.accountType.toString());
					rc = driver.getVolatileContact(id);
					if (rc!=null) _cacheByContactId.put(id,new WeakReference<VolatileContact>(rc));
				}
				catch (RemoteException e)
				{
					LogMarket.wtf(TAG,e.getLocalizedMessage(),e);
				}
				return rc;
			}
			final VolatileContact rc=ref.get();
			if (rc==null)
			{
				_cacheByContactId.remove(id);
				return null;
			}
			return rc;
		}
		
		public static void onLowMemory()
		{
			if (I) Log.i(TAG,"onLowMemory");
			// Keep only one request
			while (_lru.size()>1)
			{
				_lru.remove(_lru.size()-1);
			}
			// Remove image in this request
			if (_lru.size()>0)
			{
				for (VolatileContact contact:_lru.get(0).contacts)
				{
					for (VolatileRawContact rawContact:contact.getRawContacts())
					{
						rawContact._withPhoto=VolatileRawContact.PHOTO_UNKNWON;
						rawContact.removeAll(Photo.CONTENT_ITEM_TYPE);
					}
				}
			}
			
			// Clear _cacheByLongId
			for (Map.Entry<Long,WeakReference<VolatileContact>> entry:_cacheByLongId.entrySet())
			{
				if (entry.getValue().get()==null)
					_cacheByLongId.remove(entry.getKey());
			}

			// Clear _cacheByContactId
			for (Map.Entry<ContactId,WeakReference<VolatileContact>> entry:_cacheByContactId.entrySet())
			{
				if (entry.getValue().get()==null)
					_cacheByLongId.remove(entry.getKey());
			}
		}
	}
	
	/**
	 * 
	 * @param id
	 * @param deleted
	 * @param context
	 * @return Contact uri
	 */
	public static Uri importVolatileContactToAndroid(final long id, final boolean temp, final Context context)
	{
		if (I) Log.i(TAG,"Import contact in android");
		try
		{
			final VolatileContact contact = ProvidersManager.Cache.getVolatileContactsById(id);
			if (contact == null)
				return null;
			final ContentResolver resolver=context.getContentResolver();
			final VolatileRawContact rawcontact=contact.getRawContact();
			if (DEBUG)
			{
				Dump.dump_android("DUMP", false,resolver);
			}

			final ArrayList<ContentProviderOperation> operationList=new ArrayList<ContentProviderOperation>(5);
			Uri last=null;
			final Resources resources=context.getResources();
			for (final VolatileRawContact raw:contact.getRawContacts())
			{
				if (temp)
				{
					raw.setAttr(VolatileRawContact.MUST_DELETED, 1); // Say it's may be deleted
				}
				else
				{
					rawcontact._datas.remove(Import.CONTENT_ITEM_TYPE);
					rawcontact._datas.remove(Copy.CONTENT_ITEM_TYPE);
					rawcontact.setAttr(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE_DEFAULT);
				}
				final Uri rawContactUri=raw.copyToAndroid(resources,resolver,temp,operationList);
				if (rawContactUri!=null)
					last=rawContactUri;
			}
			if (operationList.size()!=0)
			{
				final ContentProviderResult[] result=resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
				if (last==null)
					last=result[(temp)?0:1].uri;
			}
			if (DEBUG)
			{
				if (D) Log.d("DUMP","*************************");
				Dump.dump_android("DUMP", false,resolver);
			}
			return VolatileContact.contactUriFromRawUri(resolver,last);
		}
		catch (final RemoteException e)
		{
			if (I) Log.i(TAG, "importMemoryContact", e);
			return null;
		}
		catch (final OperationApplicationException e)
		{
			LogMarket.wtf(TAG, "importMemoryContact", e);
			return null;
		}
	}

	/**
	 * Import definitively data in contacts. 
	 * A previous volatile record must be present in contacts.
	 * @param resolver
	 * @param data The data record with import mime type, because come from the "import" field.
	 * @return
	 */
	private static final String[] DATAS_COLUMN=new String[]{Data.CONTACT_ID,Data.DISPLAY_NAME,Data.RAW_CONTACT_ID,Import.LOOKUP_COLUMN};
	private static final String[] RAW_ACCOUNT=new String[]{RawContacts.ACCOUNT_NAME, RawContacts.ACCOUNT_TYPE};
	private static final String[] RAW_CONTACT_ID_COLUMN=new String[]{RawContacts.CONTACT_ID};
	private static final String[] RAW_ID_COLUMN=new String[]{RawContacts._ID};
	
	public static Uri fixeSyncContactInAndroid(Resources resources,ContentResolver resolver,Uri data)
	{
		if (I) Log.i(TAG,"Save contact in android");
		if (EMULATOR) 
		{
			if (D) Log.d("DUMP","BEFORE...");
			Dump.dump_android_lite("DUMP", false,resolver);
		}
		Cursor cursor=null;
		try
		{
			// Retrieve rawid and contactid for this specific import button
			cursor=resolver.query(data,DATAS_COLUMN,null,null,null);
			if (!cursor.moveToFirst()) 
				throw new IllegalArgumentException("Illegal uri "+data);
			final long rawContactId=cursor.getLong(2/*Data.RAW_CONTACT_ID*/);
			final String lookupKey=cursor.getString(3/*LOOKUP_COLUMN*/);
			assert(lookupKey!=null);
			cursor.close();
			
			return fixeSyncContactInAndroid(resources,resolver,rawContactId);
		}
		finally
		{
			if (cursor!=null)
				cursor.close();
		}
	}
	
	public static Uri fixeSyncContactInAndroid(Resources resources,ContentResolver resolver,long rawContactId)
	{
		if (I) Log.i(TAG,"Save contact in android");
		if (DEBUG /*&& Application.EMULATOR*/) 
		{
			if (D) Log.d("DUMP","BEFORE...");
			Dump.dump_android("DUMP", false,resolver);
		}
		Cursor cursor=null;
		try
		{
			cursor=resolver.query(Data.CONTENT_URI, DATAS_COLUMN, 
					Data.MIMETYPE+"='"+Import.CONTENT_ITEM_TYPE+"\' and "+
					Data.RAW_CONTACT_ID+"="+rawContactId, null, null);
			if (!cursor.moveToFirst())
				throw new IllegalArgumentException("Illegal raw contact id "+rawContactId);
			long contactId=cursor.getLong(0/*Data.CONTACT_ID*/);
			final String displayName=cursor.getString(1/*Data.DISPLAY_NAME*/);
//			final long rawContactId=cursor.getLong(2/*Data.RAW_CONTACT_ID*/);
			final String lookupKey=cursor.getString(3/*LOOKUP_COLUMN*/);
			cursor.close();
			
			final ArrayList<ContentProviderOperation> operationList=new ArrayList<ContentProviderOperation>(5);

			// Start with the Import Data.
			// Retrieve rawid and contactid for this specific import button
//			cursor=resolver.query(ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),RAW_CONTACT_ID_COLUMN,null,null,null);
//			if (!cursor.moveToFirst()) 
//				throw new IllegalArgumentException("Illegal raw contact id "+rawContactId);
//			long contactId=cursor.getLong(0/*RawContact.CONTACT_ID*/);
//			cursor.close();
			
			// Retreive the raw contact, and keep the account name and type
			cursor=resolver.query(ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId), RAW_ACCOUNT, 
				null,null, null);
			cursor.moveToFirst();
			final String accountName=cursor.getString(0/*ACCOUNT_NAME*/);
			final String accountType=cursor.getString(1/*ACCOUNT_TYPE*/);
			cursor.close();
			
			// Remove all imports data for this record (normally only one)
			operationList.add( 
				ContentProviderOperation.newDelete(Data.CONTENT_URI)
				.withSelection(Data.MIMETYPE+"='"+Import.CONTENT_ITEM_TYPE+"' and "+Data.CONTACT_ID+"="+contactId, null)
				.build()
				);
			

			// Delete raw contact and rebuild a new one. Contacts don't like to update the aggregation mode.
			operationList.add(
				ContentProviderOperation.newDelete(ContentUris.withAppendedId(RawContacts.CONTENT_URI,rawContactId))
				.build()
				);
			// Re-create record with normal AGGREGATION_MODE
			final VolatileRawContact raw;
			final VolatileContact contact=Cache.getVolatileContactByContactId(new ContactId(accountType, accountName, lookupKey));
			assert(contact!=null); // Si le processus meurt lors de l'affichage du contact !
			raw=contact.getRawContacts().get(0);
//			raw=new VolatileRawContact(null);
			raw.setAttr(VolatileRawContact.MUST_DELETED, 0); // Say it's may not be deleted
			raw.setAttr(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE_DEFAULT);
			raw._datas.remove(Import.CONTENT_ITEM_TYPE);
			raw._datas.remove(Import.CONTENT_ITEM_TYPE);
			raw.copyToAndroid(resources,resolver,false,operationList);
			final ContentProviderResult[] results=resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
			if (DEBUG || EMULATOR) 
				Dump.dump_android("DUMP", false,resolver);
			
			final Uri newRawUri=results[3].uri; // The raw contract uri
			
			// Search the Contact id for this new raw contact
			cursor=resolver.query(newRawUri,RAW_CONTACT_ID_COLUMN,null,null,null);
			cursor.moveToFirst();
			contactId=cursor.getLong(0/*CONTACT_ID*/);
			cursor.close();

			// Count the number of raw for this contact
			cursor=resolver.query(RawContacts.CONTENT_URI,RAW_ID_COLUMN,
					RawContacts.CONTACT_ID+"="+contactId+" and "+RawContacts.DELETED+"= 0",null,null);
			final int cnt=cursor.getCount();
			cursor.close();
			if (EMULATOR) 
				if (D) Log.d("DUMP","cnt="+cnt);

			// If find only the new record,
			// create an raw contact without account, with the same displayname
			if (cnt < 2) // One build by android
			{
				operationList.clear();
				operationList.add(
					ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
					.withValue(RawContacts.DIRTY, 1)
					.withValue(RawContacts.CONTACT_ID, contactId)
					.build()
					);
				
				operationList.add(
					ContentProviderOperation.newInsert(Data.CONTENT_URI)
					.withValue(StructuredName.MIMETYPE,StructuredName.CONTENT_ITEM_TYPE)
					.withValue(StructuredName.DISPLAY_NAME, displayName)
					.withValueBackReference(Data.RAW_CONTACT_ID, operationList.size()-1)
					.build()
					);
				resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
			}
			
			if (DEBUG /*&& Application.EMULATOR*/) 
			{
				if (D) Log.d("DUMP","BEFORE...");
				Dump.dump_android("DUMP", false,resolver);
			}
			return ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
		}
		catch (RemoteException e)
		{
			LogMarket.wtf(TAG,e);
			return null;
		}
		catch (OperationApplicationException e)
		{
			LogMarket.wtf(TAG,e);
			return null;
		}
		finally
		{
			if (cursor!=null)
				cursor.close();
		}
	}
	
	/**
	 * Copy the contact in local contact, without synchronization.
	 * If no local contact is present, create a new one. Else, update the current contact.
	 * @param resources
	 * @param resolver
	 * @param data The data record with import mime type, because come from the "import" field.
	 * @return
	 */
	public static Uri fixeCopyContactInAndroid(Resources resources,ContentResolver resolver,Uri data)
	{
		//Dump.dump_android_lite("DUMP", false,resolver);
		Cursor cursor=null;
		try
		{
			// Start with the Import Data.
			// Retrieve rawid and contactid for this specific import button
			cursor=resolver.query(data,DATAS_COLUMN,null,null,null);
			if (!cursor.moveToFirst()) 
				throw new IllegalArgumentException("Illegal uri "+data);
			final long rawContactId=cursor.getLong(2/*Data.RAW_CONTACT_ID*/);
			cursor.close();

			return fixeCopyContactInAndroid(resources,resolver,rawContactId);
		}
		finally
		{
			if (cursor!=null)
				cursor.close();
		}
	}
	
	/**
	 * Copy the contact in local contact, without synchronization.
	 * If no local contact is present, create a new one. Else, update the current contact.
	 * @param resources
	 * @param resolver
	 * @param rawContactId The raw contact id.
	 * @return
	 */
	public static Uri fixeCopyContactInAndroid(Resources resources,ContentResolver resolver,long rawContactId)
	{
		//Dump.dump_android_lite("DUMP", false,resolver);
		Cursor cursor=null;
		try
		{
			final ArrayList<ContentProviderOperation> operationList=new ArrayList<ContentProviderOperation>(5);

			// Start with the Import Data.
			// Retrieve rawid and contactid for this specific import button
			
			// Retrieve the raw contact, and keep the account name and type
			Uri uriRawContact=ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId);
			cursor=resolver.query(uriRawContact, RAW_ACCOUNT,null,null, null);
			cursor.moveToFirst();
			
			// Delete raw contact and rebuild a new one. Contacts don't like to update the aggregation mode.
			operationList.add(
				ContentProviderOperation.newDelete(ContentUris.withAppendedId(RawContacts.CONTENT_URI,rawContactId))
				.build()
				);

			// Re-create record with normal AGGREGATION_MODE and no account name
			VolatileRawContact raw;
			raw=new VolatileRawContact(null);
			raw.setAttr(VolatileRawContact.MUST_DELETED, 0); // Say it's may be not deleted
			raw.setAttr(RawContacts.ACCOUNT_NAME,(String)null);
			raw.setAttr(RawContacts.ACCOUNT_TYPE,(String)null);
			raw.setAttr(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE_DEFAULT);
			raw.remove(Copy.CONTENT_ITEM_TYPE, raw.get(Copy.CONTENT_ITEM_TYPE));
			raw.copyToAndroid(resources, resolver,false,operationList);

			ContentProviderResult[] results=resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
			//Dump.dump_android("DUMP", false,resolver);

			Uri newRawUri=results[1].uri; // The raw record uri
			
			// Search the Contact id for this raw contact
			cursor=resolver.query(newRawUri,MAPPING_IDS,null,null,null);
			cursor.moveToFirst();
			long rawid=cursor.getLong(0/*_ID*/);
			long contactid=cursor.getLong(1/*CONTACT_ID*/);
			cursor.close();

			// Check if two internal raw contact with the same contact_id.
			// It's because a raw contact without account name is already present.
			// Then merge this two record.
			// So, I delete the new record and populate the old one.
			cursor=resolver.query(RawContacts.CONTENT_URI,MAPPING_ID,RawContacts.CONTACT_ID+"="+contactid,null,null);
			if (cursor.getCount()!=1)
			{
				long mergedid; // The record to populate
				do
				{
					cursor.moveToNext();
					mergedid=cursor.getLong(0/*RawContacts._ID*/);
					
				} while (mergedid==rawid);
				assert(mergedid!=rawid);

				operationList.clear();
				operationList.add(
						ContentProviderOperation.newDelete(ContentUris.withAppendedId(RawContacts.CONTENT_URI,rawid))
						.build()
						);
				raw.copyDataToAndroid(mergedid,operationList);
				resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
			}
			//Dump.dump_android("DUMP", false,resolver);
			return ContentUris.withAppendedId(Contacts.CONTENT_URI, contactid);
		}
		catch (RemoteException e)
		{
			LogMarket.wtf(TAG,e);
			return null;
		}
		catch (OperationApplicationException e)
		{
			LogMarket.wtf(TAG,e);
			return null;
		}
		finally
		{
			if (cursor!=null)
				cursor.close();
		}
	}
	
	private static StringBuilder volatilAccountToIn()
	{
		final StringBuilder vAccounts=new StringBuilder(" in (");
		for (final String v:ProvidersManager._drivers.keySet())
		{
			vAccounts.append(DatabaseUtils.sqlEscapeString(v)).append(',');
		}
		vAccounts.setLength(vAccounts.length()-1);
		vAccounts.append(')');
		vAccounts.trimToSize();
		return vAccounts;
	}
	
	/** Tools to remove all contacts inline. */
	public static void purgeVolatileContact(ContentResolver resolver)
	{
		resolver.delete(RawContacts.CONTENT_URI, 
			VolatileRawContact.MUST_DELETED+"=1 and "+RawContacts.ACCOUNT_TYPE+volatilAccountToIn(), null);
	}
}
