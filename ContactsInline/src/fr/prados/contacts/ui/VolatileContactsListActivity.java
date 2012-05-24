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

import static fr.prados.contacts.Constants.D;
import static fr.prados.contacts.Constants.DEBUG;
import static fr.prados.contacts.Constants.E;
import static fr.prados.contacts.Constants.EMULATOR;
import static fr.prados.contacts.Constants.IMAGE_POOL_PRIORITY;
import static fr.prados.contacts.Constants.IMAGE_POOL_SIZE;
import static fr.prados.contacts.Constants.REQUERY_AFTER_KILL;
import static fr.prados.contacts.Constants.V;
import static fr.prados.contacts.Constants.W;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.provider.Contacts.Intents.UI;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Intents.Insert;
import android.provider.ContactsContract.RawContacts;
import android.provider.SearchRecentSuggestions;
import android.provider.Settings;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AlphabetIndexer;
import android.widget.CursorAdapter;
import android.widget.HeaderViewListAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.SearchView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;

import fr.prados.contacts.Application;
import fr.prados.contacts.ContactId;
import fr.prados.contacts.VolatileRawContact;
import fr.prados.contacts.lib.R;
import fr.prados.contacts.providers.Provider;
import fr.prados.contacts.providers.QueryError;
import fr.prados.contacts.providers.QueryException;
import fr.prados.contacts.providers.QueryWarning;
import fr.prados.contacts.providers.ResultsAndExceptions;
import fr.prados.contacts.tools.CheckContext;
import fr.prados.contacts.tools.Eula;
import fr.prados.contacts.tools.LogMarket;
import fr.prados.contacts.tools.QueryMarket;
import fr.prados.contacts.tools.Update;

@SuppressWarnings("deprecation")
public final class VolatileContactsListActivity extends AbsListActivity 
implements OnCreateContextMenuListener, OnKeyListener, OnAccountsUpdateListener
{
	private static final boolean PATCH_BUG_V14=true;
	
	private static final boolean HONEYCOMB=Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB;
	public static final boolean withFlurry=(!EMULATOR && !DEBUG);
	private static final String FlurryError="error";
	
	private static final String TAG = "VolatileContacts";

	private Handler _handler=new Handler();

	private static final int DISPLAY_NUMBER_OF_CONTACTS = 1 << 1;

	private static final int DISPLAY_PHOTO = 1 << 2;

	private static final int DISPLAY_HEADER = 1 << 3;

	private static final int DISPLAY_CALLBUTTON = 1 << 4;

	private static final int DISPLAY_DATA = 1 << 5;

	private static final int USE_QUICK_CONTACT = 1 << 6;

	/** Extension is present ? */
	private boolean _isExtension;
	
	/** Check double import at the same time */
	private boolean _asynImport;
	
	/** Account is present ? */
	private static boolean _isDone;

	private int _show;

	private int _mode;
	
	private boolean _showHeader;

	private int _scrollState;

	private ContactsAdapter _adapter;

	private QueryHandler _queryHandler;
	
	private ImportAllHandler _importAllHandler;

	private String _queryMode = Provider.QUERY_MODE_ALL;

	private SearchRecentSuggestions _suggestions;

	/** Query for current list */
	private String _lastQuery;

	private TextView _errorsText;
	
	private TextView _emptyText;

	private TextView _totalContacts;
	
	private MenuItem _searchMenu;
	private SearchView _searchView;

	private static final int MODE_UNKNOWN = -1;

	private static final int MODE_NORMAL = 0;
	private static final int MODE_PICK_CONTACT = 1 << 1;
	private static final int MODE_PICK_PHONE = 1 << 2;
	private static final int MODE_PICK_POSTAL = 1 << 3;
	private static final int MODE_SEARCH = 1 << 4;
	//private static final int MODE_MASK_PICKER = MODE_PICK_CONTACT | MODE_PICK_PHONE | MODE_PICK_POSTAL;

	private static final int SHOW_NORMAL        = DISPLAY_NUMBER_OF_CONTACTS | DISPLAY_PHOTO | DISPLAY_HEADER | DISPLAY_CALLBUTTON | DISPLAY_DATA | USE_QUICK_CONTACT  ;
	private static final int SHOW_LIST          = DISPLAY_NUMBER_OF_CONTACTS | USE_QUICK_CONTACT | DISPLAY_HEADER | DISPLAY_DATA;
	private static final int SHOW_LIST_CONTACTS = SHOW_NORMAL;

	private static final int SHOW_PICK_CONTACT = DISPLAY_PHOTO | DISPLAY_HEADER | DISPLAY_DATA;

	private static final int SHOW_PICK_PHONE = DISPLAY_HEADER | DISPLAY_DATA;

	private static final int SHOW_PICK_POSTAL = DISPLAY_HEADER | DISPLAY_DATA;


	// static arrays for optimize the code
	private static final String[] colDataId = new String[]
	{ BaseColumns._ID };

	private static final String[] colsNormal = new String[]
	{ 
		BaseColumns._ID, Contacts.DISPLAY_NAME, 
		RawContacts.ACCOUNT_TYPE,RawContacts.ACCOUNT_NAME,VolatileRawContact.LOOKUP, 
		Phone.TYPE, Phone.NUMBER 
	};

	private static final String[] colsPickContact = new String[]
	{ 
		BaseColumns._ID, Contacts.DISPLAY_NAME,
		RawContacts.ACCOUNT_TYPE,RawContacts.ACCOUNT_NAME,VolatileRawContact.LOOKUP, 
	};

	private static final String[] colsPickPhone = new String[]
	{ 
		BaseColumns._ID, Contacts.DISPLAY_NAME, 
		RawContacts.ACCOUNT_TYPE,RawContacts.ACCOUNT_NAME,VolatileRawContact.LOOKUP, 
		Phone.RAW_CONTACT_ID, Phone.TYPE, Phone.LABEL, Phone.NUMBER };

	private static final String[] colsPickPostal = new String[]
	{ 
		BaseColumns._ID, Contacts.DISPLAY_NAME, 
		RawContacts.ACCOUNT_TYPE,RawContacts.ACCOUNT_NAME,VolatileRawContact.LOOKUP, StructuredPostal.RAW_CONTACT_ID, 
		StructuredPostal.LABEL, StructuredPostal.TYPE, StructuredPostal.FORMATTED_ADDRESS
	};

	private static final String[] colAuthority = new String[]
	{ ContactsContract.AUTHORITY };

	private static final String[] colForCallOrSms = new String[]
	{ BaseColumns._ID, Phone.NUMBER, Phone.IS_SUPER_PRIMARY, Phone.TYPE };

	public VolatileContactsListActivity()
	{
		if (V) Log.v("LIFE", "Constructor me=" + hashCode());
	}
	class AsyncImport extends AsyncTask<Object,Void,Uri>
	{
		@Override
		protected void onPreExecute()
		{
			incProgressBar();
		}
		@Override
		protected Uri doInBackground(Object... params)
		{
			if (_asynImport)
				return null;
			_asynImport=true;
			return importMemoryContact((Long)params[0], (Boolean)params[1]);
		}
		@Override
		protected void onPostExecute(Uri result)
		{
			super.onPostExecute(result);
			_asynImport=false;
			decProgressBar();				
		}
	};

	static class ImportAllHandler extends AsyncTask<Cursor, Void, Void>
	{
		private VolatileContactsListActivity _activity;
		@Override
		protected Void doInBackground(Cursor... paramArrayOfParams)
		{
			final Cursor cursor=paramArrayOfParams[0];
			long id;
			if (cursor.moveToFirst())
			{
				do
				{
					id=cursor.getLong(0 /* BaseColumns._ID */);
					ProvidersManager.init();
					Uri uri=ProvidersManager.importVolatileContactToAndroid(id, false, Application.context); // $codepro.audit.disable variableDeclaredInLoop
					long rawId=Long.parseLong(uri.getLastPathSegment()); // $codepro.audit.disable variableDeclaredInLoop
					ProvidersManager.fixeSyncContactInAndroid(Application.context.getResources(),
							_activity.getContentResolver(), 
							rawId);
				} while (cursor.moveToNext());
			}
			return null;
		}
		@Override
		protected void onPostExecute(Void result)
		{
			super.onPostExecute(result);
			_activity.removeDialog(DIALOG_IMPORT_ALL_RESULT);
			_activity._importAllHandler=null;
		}
	}
	/**
	 * Use with RetainNonConfigurationInstance
	 * @version 1.0
	 * @since 1.0
	 * @author Philippe PRADOS
	 */
	static final class Retain
	{
		public QueryHandler _queryhandler;
		public Cursor _cursor;
		public boolean _onSearchRequest;
		public ImportAllHandler _importAllHandler;
	}
	
	private int _cntProgress;
	void incProgressBar()
	{
		if (++_cntProgress>0)
		{
			if (_queryHandler._progressDialog==null)
				setProgressBarIndeterminateVisibility(true);
		}
	}
	void decProgressBar()
	{
		if (E && _cntProgress<=0)
			Log.e(TAG,"dec progress bar with "+_cntProgress);
		if (--_cntProgress<=0)
		{
			setProgressBarIndeterminateVisibility(false);
			if (!D) _cntProgress=0;
		}
		else
			setProgressBarIndeterminateVisibility(true);
	}
	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
		if (withFlurry)
		{
			FlurryAgent.onStartSession(this, "YU1VATY1MRJ5XWV9RTJ4");
			//FlurryAgent.setLogEnabled(false);
		}

		if (V) Log.v("LIFE", "onCreate");
		if (!checkContact())
			return;
		if (!CheckContext.isAndroidMinVersion(this,7))
			return;
		Update.showUdpate(this,Application.VERSION,R.raw.update);
		Eula.showEula(this,R.raw.eula);
		HelpActivity.showIntro(this);
		
		AccountManager.get(this).addOnAccountsUpdatedListener(this, null, false);

		setContentView(R.layout.contacts_list_content);
		setProgressBarIndeterminateVisibility(true);
		setProgressBarIndeterminate(false);

		_errorsText=(TextView) findViewById(R.id.errorsText);
		_emptyText = (TextView) findViewById(R.id.emptyText);
		_emptyText.setMovementMethod(LinkMovementMethod.getInstance());
		
		final AbsListView list = getListView();
		list.setFocusable(true);
		list.setOnCreateContextMenuListener(this);
		if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB)
			list.setFastScrollAlwaysVisible(true);

		// We manually save/restore the listview state
		list.setSaveEnabled(false);

		_suggestions = new SearchRecentSuggestions(this, 
				VolatileContactsRecentSuggestionsProvider.AUTHORITY, 
				VolatileContactsRecentSuggestionsProvider.MODE);
		_queryHandler = new QueryHandler(this);
		handleIntent(getIntent());
		_adapter = new ContactsAdapter();
		list.setOnItemClickListener(_adapter);

		if ((_show & DISPLAY_NUMBER_OF_CONTACTS) != 0)
		{
			if (!HONEYCOMB)
			{
				final ArrayList<ListView.FixedViewInfo> headers = new ArrayList<ListView.FixedViewInfo>(3);
				final ListView.FixedViewInfo header = ((ListView)getListView()).new FixedViewInfo();
				header.isSelectable = true;
				final LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(
					R.layout.total_contacts, null, false);
				_totalContacts = (TextView) layout.findViewById(R.id.totalContactsText);
				if (Build.VERSION.SDK_INT < 8) _totalContacts.setGravity(Gravity.CENTER);
				header.view = layout;
				header.isSelectable = false;
				headers.add(header);
				setListAdapter(new HeaderViewListAdapter(headers, new ArrayList<ListView.FixedViewInfo>(3), _adapter));
			}
			else
				setListAdapter(_adapter);
		}
		else
		{
			setListAdapter(_adapter);
		}

		final Intent intent = getIntent();
		final String title = intent.getStringExtra(UI.TITLE_EXTRA_KEY);
		if (title != null)
			setTitle(title);

		// Auto open search, but not for main action.
//		final String action = intent.getAction();
		// Wait provider initialization before get accounts.
		findProviders(false);
	}

	@Override
	public void onAccountsUpdated(Account[] accounts)
	{
		if (D) Log.d(TAG,"Detect accounts updated");
		findProviders(true);
	}
	void findProviders(final boolean newProvider)
	{
		_isDone=_isExtension=false;
		if (_adapter._cursor==null || _adapter._cursor.getCount()==0)
			_emptyText.setText("");
		
		new AsyncTask<Void, Void, Void>()
		{
			protected void onPreExecute() 
			{
				incProgressBar();
			}
			@Override
			protected Void doInBackground(Void... params)
			{
				if (newProvider) ProvidersManager.reset();
				ProvidersManager.init();
				ProvidersManager.waitInit();
				return null;
			}
			@Override
			public void onPostExecute(Void r)
			{
				decProgressBar();
				_isExtension=!ProvidersManager.isEmpty();
				if (!_isExtension)
				{
					_emptyText.setText(R.string.help_need_provider);
					setSearchVisible(false);
					return;
				}
				final Account[] accounts = ProvidersManager.getAccounts();
				_isDone = (accounts != null) && (accounts.length > 0);
				if (_searchMenu!=null)
				{
					setSearchVisible(_isDone);
				}
				if (!_isDone)
				{
					_emptyText.setText(R.string.help_need_account);
				}
				else
				{
					if ((_adapter._cursor!=null) && (_adapter._cursor.getCount()==0))
					{
						_emptyText.setText(R.string.noMatchingContacts);				
						// Bug with version 14
						if (!PATCH_BUG_V14 || Build.VERSION.SDK_INT!=Build.VERSION_CODES.ICE_CREAM_SANDWICH)
							onSearchRequested();
					}
					else if (!_queryHandler._pending)
					{
						if ((_mode == MODE_SEARCH) || (_mode == MODE_NORMAL))
						{
							_emptyText.setText(R.string.help_first_time);
							if ((_adapter._cursor==null) || (_adapter._cursor.getCount()==0))
							{
								// Bug with version 14
								if (!PATCH_BUG_V14 || Build.VERSION.SDK_INT!=Build.VERSION_CODES.ICE_CREAM_SANDWICH)
									onSearchRequested();
							}
						}
						else
						{
							_emptyText.setText(R.string.help_with_search);
						}
						if (!Intent.ACTION_SEARCH.equals(getIntent().getAction()))
						{
							if (_searchMenu!=null)
							{
								setSearchVisible(_isDone);
							}
							else
								if (E) Log.e(TAG,"Menu not created");
						}
					}
					else
					{
						_emptyText.setText(R.string.currentQuery);
					}
					if (_mode==MODE_SEARCH)
					{
						if ((_lastQuery != null) && (_adapter._cursor == null))
						{
							if (D) Log.d(TAG,"Start query for onPostExec, MODE_SEARCH");
							startQuery();
						}
					}
				}
			}
			
		}.execute();
		
	}
	
	private void handleIntent(final Intent intent)
	{
		final String action = intent.getAction();
		_mode = MODE_UNKNOWN;
		if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action))
		{
			final String type = intent.resolveType(this);
			if (Contacts.CONTENT_ITEM_TYPE.equals(type) || Contacts.CONTENT_TYPE.equals(type))
			{
				_mode = MODE_PICK_CONTACT;
				_show = SHOW_PICK_CONTACT;
			}
			else if (Phone.CONTENT_ITEM_TYPE.equals(type) || Phone.CONTENT_TYPE.equals(type))
			{
				_mode = MODE_PICK_PHONE;
				_show = SHOW_PICK_PHONE;
				_queryMode = Provider.QUERY_MODE_ALL_WITH_PHONE;
			}
			else if (StructuredPostal.CONTENT_ITEM_TYPE.equals(type) || StructuredPostal.CONTENT_TYPE.equals(type))
			{
				_mode = MODE_PICK_POSTAL;
				_show = SHOW_PICK_POSTAL;
				_queryMode = Provider.QUERY_MODE_ALL_WITH_ADDRESS;
			}
		}
		else if (UI.LIST_CONTACTS_WITH_PHONES_ACTION.equals(action))
		{
			_mode = MODE_NORMAL;
			_show = SHOW_LIST;
			_queryMode = Provider.QUERY_MODE_ALL_WITH_PHONE;
		}
		else if (UI.LIST_ALL_CONTACTS_ACTION.equals(action))
		{
			_mode = MODE_NORMAL;
			_show = SHOW_LIST;
		}
		else if (Intent.ACTION_VIEW.equals(action) || Intent.ACTION_MAIN.equals(action))
		{
			_mode = MODE_NORMAL;
			_show = SHOW_NORMAL;
		}
		else if (UI.LIST_DEFAULT.equals(action))
		{
			_mode = MODE_NORMAL;
			_show = SHOW_LIST_CONTACTS;
		}
		else if ("com.android.contacts.action.LIST_CONTACTS".equals(action))
		{
			_mode = MODE_NORMAL;
			_show = SHOW_LIST_CONTACTS;
		}
		else if (Intent.ACTION_SEARCH.equals(action))
		{
			_mode = MODE_SEARCH;
			_show = SHOW_NORMAL;
			String queryData = null;

			if (intent.hasExtra(Insert.EMAIL))
			{
				_queryMode = Provider.QUERY_MODE_MAILTO;
				queryData = intent.getStringExtra(Insert.EMAIL);
				_show = SHOW_LIST_CONTACTS;
			}
			else if (intent.hasExtra(Insert.PHONE))
			{
				_queryMode = Provider.QUERY_MODE_TEL;
				queryData = intent.getStringExtra(Insert.PHONE);
			}
			else if ("call".equals(intent.getStringExtra(SearchManager.ACTION_MSG)))
			{
				_queryMode = Provider.QUERY_MODE_TEL;
				queryData = intent.getStringExtra(SearchManager.QUERY);
			}
			else
			{
				// Otherwise handle the more normal search case
				_queryMode = Provider.QUERY_MODE_ALL;
				queryData = getIntent().getStringExtra(SearchManager.QUERY);
				_show = SHOW_LIST_CONTACTS;
			}
			_suggestions.saveRecentQuery(queryData, null);
			_lastQuery = queryData;
		}
		if (_mode == MODE_UNKNOWN)
			LogMarket.wtf(TAG, "Unknown mode "+_mode);
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY) && !EMULATOR )
			_show &= ~DISPLAY_CALLBUTTON;
	}

	private boolean restoreRetainNonConfigurationInstance()
	{
		if (D) Log.d("LIFE", "...getLastNonConfigurationInstance");
		final Retain retain=(Retain)getLastNonConfigurationInstance();
		if (D) Log.d("LIFE", "...getLastNonConfigurationInstance return "+retain);
		if (retain!=null)
		{
			_queryHandler=retain._queryhandler;
			_queryHandler._activity=new WeakReference<VolatileContactsListActivity>(this);
			if (retain._cursor!=null)
			{
				_adapter.changeCursor(retain._cursor);
			}
			_importAllHandler=retain._importAllHandler;
			if (_importAllHandler!=null)
			{
				_importAllHandler._activity=this;
				showDialog(DIALOG_IMPORT_ALL_RESULT);
			}
			else
			{
				removeDialog(DIALOG_IMPORT_ALL_RESULT);
			}
			getListView().requestFocus();
			return true;
		}
		return false;
	}

	@Override
	protected void onRestart()
	{
		super.onRestart();
		if (V) Log.v("LIFE", "onRestart");

		// The cursor was killed off in onStop(), so we need to get a new one here
		// We do not perform the query if a filter is set on the list because the
		// filter will cause the query to happen anyway
		// if (TextUtils.isEmpty(getListView().getTextFilter()))
		if (REQUERY_AFTER_KILL)
		{
			if ((_lastQuery != null) && (_adapter._cursor == null))
			{
				if (D) Log.d(TAG,"Restart previous request "+_lastQuery);
				startQuery();
			}
		}
		else
		{
			_lastQuery=null;
			_adapter._cursor=null;
		}

	}

	@Override
	protected void onRestoreInstanceState(final Bundle state)
	{
		super.onRestoreInstanceState(state);
		if (V) Log.v("LIFE", "onRestoreInstanceState");
		if (!restoreRetainNonConfigurationInstance())
		{
			_restoreListState = state.getParcelable(STATE_LIST_STATE);
			_queryMode = state.getString(STATE_QUERY_MODE);
			_lastQuery = state.getString(STATE_LAST_REQUEST);
			if (D) Log.d(TAG, "restore lastQuery="+_lastQuery);
			if (REQUERY_AFTER_KILL)
			{
				if (!_queryHandler._pending && (_lastQuery != null) && (_adapter._cursor == null))
				{
					if (D) Log.d(TAG,"start query for on restore");
					new AsyncTask<Void,Void,Void>()
					{
						@Override
						protected Void doInBackground(Void... params)
						{
							ProvidersManager.waitInit();
							return null;
						}
						protected void onPostExecute(Void result) 
						{
							startQuery();
						}
					}.execute();
				}
			}
			if (_restoreListHasFocus = state.getBoolean(STATE_FOCUS))
			{
				getListView().requestFocus();
			}
		}
		_adapter.setTotalContactCountView();
		_queryHandler.showError(this);
	}
	@Override
	protected void onResume()
	{
		super.onResume();
		if (V) Log.v("LIFE", "onResume");
		_scrollState = OnScrollListener.SCROLL_STATE_IDLE;
		if (_searchMenu!=null)
		{
			setSearchVisible(_isDone);
			if (ProvidersManager.isAccountEmpty())
			{
				_lastQuery=null;
				_adapter._cursor=null;
			}
		}
		TypedArray a=obtainStyledAttributes(R.style.Theme,new int[]{R.attr.showHeader});
		_showHeader=a.getBoolean(0, true);
	}

	private void setSearchVisible(boolean visible)
	{
		if (_searchMenu==null) return;
		if (!visible && HONEYCOMB)
		{
			_searchMenu.collapseActionView();
		}
		_searchMenu.setVisible(visible);
	}
	 @Override
	 protected void onSaveInstanceState(final Bundle state)
	 {
		 super.onSaveInstanceState(state);
		 if (V) Log.v("LIFE", "onSaveInstanceState");
		 state.putParcelable(STATE_LIST_STATE,
		 getListView().onSaveInstanceState());
		 state.putBoolean(STATE_FOCUS, getListView().hasFocus());
		 state.putString(STATE_LAST_REQUEST, _lastQuery);
		 state.putString(STATE_QUERY_MODE, _queryMode);
		 if (D) Log.d(TAG,"Save lastquery="+_lastQuery);
		 AbsListView listView=getListView();
		 // Hack to manage the getCheckedItemPosition() not present in AbsListView
		 if (listView instanceof ListView)
			 ((ListView)listView).getCheckedItemPosition();
		 else
			 listView.getCheckedItemPosition();
	 }

//	@Override
//	protected void onPause()
//	{
//		super.onPause();
//		Log.d("LIFE", "onPause");
//	}

	@Override
	protected void onStop()
	{
		super.onStop();
		if (V) Log.v("LIFE", "onStop");
		
		if (withFlurry)
			FlurryAgent.onEndSession(this);
		
		// We don't want the list to display the empty state, since when we
		// resume it will still
		// be there and show up while the new query is happening. After the
		// async query finished
		// in response to onRestart() setLoading(false) will be called.
		if (_adapter!=null)
		{
			_adapter.setLoading(true);
			_adapter.clearImageFetching();
		}

		// Make sure the search box is closed
		final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		searchManager.stopSearch();
	}

	@Override
	public Object onRetainNonConfigurationInstance()
	{
		if (V) Log.v("LIFE", "onRetainNonConfigurationInstance " + _adapter._cursor);
		final Retain retain=new Retain();
		retain._queryhandler=_queryHandler;
		// Keep cursor when configuration change
		retain._cursor=_adapter._cursor;
		retain._importAllHandler=_importAllHandler;
		return retain;
	}

	@Override
	protected void onNewIntent(final Intent intent)
	{
		if (V) Log.v("LIFE", "onNewIntent()");
		setIntent(intent);
		if (Intent.ACTION_SEARCH.equals(intent.getAction()))
		{
			final String query = intent.getStringExtra(SearchManager.QUERY);
			_suggestions.saveRecentQuery(query, null);
			_lastQuery = query;
			if (D) Log.d(TAG,"Start query for new intent");
			hideKeyboard();
			getListView().requestFocus();
			startQuery();
		}
		else
			LogMarket.wtf(TAG, "onNewIntent without query");
	}

	private void hideKeyboard()
	{
		((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(
				getListView().getWindowToken(), 0);
	}

	public void hideSearchbar()
	{
		((SearchManager)getSystemService(Context.SEARCH_SERVICE)).stopSearch();
		if (_searchView!=null)
		{
			_searchView.setIconified(true);
		}
	}
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		if (V) Log.v("LIFE", "onDestroy");
		if (_queryHandler!=null)
		{
			_queryHandler.stop();
			AccountManager.get(this).removeOnAccountsUpdatedListener(this);
		}
	}

	@Override
	public void onBackPressed()
	{
		super.onBackPressed();
		ProvidersManager.wakedown();
	}
	
	@Override
	public void onLowMemory()
	{
		ProvidersManager.Cache.onLowMemory();
	}

	private static final int DIALOG_SEARCH=1;
	private static final int DIALOG_ADDSTAR=2;
	private static final int DIALOG_IMPORT=3;
	private static final int DIALOG_IMPORT_ALL_RESULT=4;
	@Override
	public Dialog onCreateDialog(int id)
	{
		ProgressDialog progressDialog;
		switch (id)
		{
			case DIALOG_SEARCH:
				progressDialog = ProgressDialog.show(
						this, getString(R.string.search_title), getString(R.string.search_wait), true,true,
						new DialogInterface.OnCancelListener()
						{
							@Override
							public void onCancel(DialogInterface dialog)
							{
								_queryHandler.cancel();
								_emptyText.setText(R.string.help_first_time);
								removeDialog(DIALOG_SEARCH);
							}
						});
				progressDialog.setCanceledOnTouchOutside(false);
				_queryHandler._progressDialog=progressDialog;
				
//				_queryHandler._progressDialog=ProgressDialog.show(
//					this, getString(R.string.search_title), getString(R.string.search_wait), true,true,
//					new DialogInterface.OnCancelListener()
//					{
//						@Override
//						public void onCancel(DialogInterface dialog)
//						{
//							_queryHandler.cancel();
//							_emptyText.setText(R.string.help_first_time);
//							setProgressBarIndeterminateVisibility(false);
//							removeDialog(DIALOG_SEARCH);
//						}
//					});
				return _queryHandler._progressDialog;
			case DIALOG_ADDSTAR:
				progressDialog = ProgressDialog.show(this, getString(R.string.importing_title), getString(R.string.importing_wait), true);
				progressDialog.setCanceledOnTouchOutside(true);
//				return ProgressDialog.show(this, getString(R.string.importing_title), getString(R.string.importing_wait), true);
				return progressDialog;
			case DIALOG_IMPORT:
				progressDialog = ProgressDialog.show(this, getString(R.string.importing_title), getString(R.string.importing_wait), true);
				progressDialog.setCanceledOnTouchOutside(true);
//				return ProgressDialog.show(this, getString(R.string.importing_title), getString(R.string.importing_wait), true);
				return progressDialog;
			case DIALOG_IMPORT_ALL_RESULT:
				progressDialog = ProgressDialog.show(this, getString(R.string.importing_title), getString(R.string.importing_wait), true);
				progressDialog.setCanceledOnTouchOutside(true);
//				return ProgressDialog.show(this, getString(R.string.importing_title), getString(R.string.importing_wait), true);
				return progressDialog;
			default:
				LogMarket.wtf(TAG, "Invalide dialog id "+id);
				return null;
		}
	}
	// ------------- Manage query
	public final static class QueryHandler implements Provider.OnQuery
	{
		private ProgressDialog _progressDialog;
		private WeakReference<VolatileContactsListActivity> _activity;
		private volatile boolean _pending;
		private StringBuilder _error=new StringBuilder(30);
		private boolean _warning;
		
		private QueryHandler(final VolatileContactsListActivity context)
		{
			_activity = new WeakReference<VolatileContactsListActivity>(context);
		}
		public void startQuery(final String selection, final String[] selectionArgs, boolean cont)
		{
			cancel();
			_pending=true;
			final VolatileContactsListActivity activity = _activity.get();
			ProvidersManager.Mode mode=null;
			String[] projection=null;
			switch (activity._mode)
			{
				case MODE_NORMAL:
				case MODE_SEARCH:
					mode=ProvidersManager.Mode.CONTACT;
					projection=colsNormal;
					break;
				case MODE_PICK_CONTACT:
					mode=ProvidersManager.Mode.CONTACT;
					projection=colsPickContact;
					break;
				case MODE_PICK_PHONE:
					mode=ProvidersManager.Mode.CONTACT_JOIN_PHONE;
					projection=colsPickPhone;
					break;
				case MODE_PICK_POSTAL:
					mode=ProvidersManager.Mode.CONTACT_JOIN_POSTAL;
					projection=colsPickPostal;
					break;
				default:
					LogMarket.wtf(TAG, "Unknown defaut mode");
			}
			if (!ProvidersManager.isAccountEmpty())
			{
				activity.showDialog(DIALOG_SEARCH);
				for (int i=ProvidersManager.getAccounts().length-1; i>=0;--i)
					activity.incProgressBar();
				
				ProvidersManager.query(mode, this, projection, selection, selectionArgs);
			}
			else
			{
				activity.setSearchVisible(false);
			}
		}
		public void cancel()
		{
			final VolatileContactsListActivity activity = _activity.get();
			clearError(activity);
			_pending=false;
			_warning=false;
			for (int i=ProvidersManager.cancelQuery()-1;i>=0;--i)
			{
				activity.decProgressBar();
			}
		}
		public void stop()
		{
			cancel();
		}

		private void showError(VolatileContactsListActivity activity)
		{
			if (_error.length()!=0)
			{
				if (withFlurry)
					FlurryAgent.onError(FlurryError, _error.toString(), "");
				activity._errorsText.setText(_error);
				activity._errorsText.setVisibility(View.VISIBLE);
			}
			else
				activity._errorsText.setVisibility(View.GONE);
				
		}
		private void showWarning(VolatileContactsListActivity activity,CharSequence msg)
		{
			Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
		}
		private void clearError(VolatileContactsListActivity activity)
		{
			_error.setLength(0);
			activity._errorsText.setText("");
			activity._errorsText.setVisibility(View.GONE);
		}

		@Override
		public synchronized void onQueryComplete(ResultsAndExceptions result,boolean finish)
		{
			final VolatileContactsListActivity activity = _activity.get();
			if (activity==null || activity.isFinishing()) 
				return;
			final Cursor cursor=result.cursor;
			// Manage all errors
			//for (Exception exception:result.exceptions)
			activity.hideSearchbar();
			final StringBuilder error=new StringBuilder(30);
			synchronized (result)
			{
				for (QueryException exception:result.exceptions)
				{
					if (exception instanceof QueryWarning)
					{
						_warning=true;
					}
					else if (exception instanceof QueryError)
					{
						QueryError w=(QueryError)exception;
						String accountName=w.getAccountName();
						if (accountName!=null)
							error.append(accountName).append(':');
						error.append(w.getMessage())
							.append('\n');
					}
					else
					{
						String msg=exception.getLocalizedMessage();
						if (msg==null) msg=exception.getMessage();
						if (msg==null) msg=exception.toString();
						error.append(msg).append('\n');
					}
				}
			}
			if (finish)
			{
				_pending=false;
				if (cursor==null || cursor.getCount()==0)
				{
					activity._emptyText.setText(R.string.noMatchingContacts);
				}
				if (_warning)
				{
					showWarning(activity,activity.getString(R.string.err_truncated));
				}
				if (error.length()!=0) 
				{
					error.trimToSize();
					_error=error;
					showError(activity);
				}
			}
			
			if (cursor!=null && activity != null && !activity.isFinishing())
			{
				activity._adapter.setLoading(false);
				activity.getListView().clearTextFilter(); // TODO : No keyboard filter at this time
				activity._adapter.changeCursor(cursor);
				// Now that the cursor is populated again, it's possible to
				// restore the list state
				if (activity._restoreListState != null)
				{
					activity.getListView().onRestoreInstanceState(activity._restoreListState);
					if (activity._restoreListHasFocus)
					{
						activity.getListView().requestFocus();
					}
					activity._restoreListHasFocus = false;
					activity._restoreListState = null;
				}
				if ((cursor.getCount()!=0) || finish)
				{
					if (_progressDialog != null)
					{
						activity.removeDialog(DIALOG_SEARCH);
						_progressDialog = null;
					}
				}
				activity._adapter.setTotalContactCountView();
			}
			else
			{
				if (_progressDialog != null)
				{
					activity.removeDialog(DIALOG_SEARCH);
					_progressDialog = null;
				}
			}
			
			if ((activity._mode==MODE_SEARCH) 
					&&  ("call".equals(activity.getIntent().getStringExtra(SearchManager.ACTION_MSG)))
					&& (cursor.getCount()!=0)
				)
			{	
				final Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts(
						"tel", activity._lastQuery, null));
				activity.startActivity(intent);
				activity.finish();
			}
			if (error.length()==0)
				QueryMarket.checkRate(activity);
			activity.decProgressBar();
		}
	}

	// ------------------ Save / Restore state
	private static final String STATE_LIST_STATE = "liststate";

	private static final String STATE_FOCUS = "focused";

	private static final String STATE_LAST_REQUEST = "request";

	private static final String STATE_QUERY_MODE = "queryMode";

	private Parcelable _restoreListState = null;

	private boolean _restoreListHasFocus;

	// ------------------- Manage list view
	/** View index cached in tag. */
	static private final class Cache
	{
		private View _header;

		private TextView _headerText;

		private View _rightSide;

		// private ImageView _presence;

		// private View _callview;

		private ImageView _callbutton;

		private ExQuickContactBadge _photoView;

		private QuickContactBadge _nonQuickContactPhotoView;

		private TextView _label;

		private TextView _data;

		private TextView _name;

		// private View _divider;
	}

	final class ContactsAdapter extends CursorAdapter implements SectionIndexer, ListAdapter, OnItemClickListener, OnClickListener
	{
		private SectionIndexer _indexer;

		private int[] _sectionPositions;

		static final int SUMMARY_NAME_COLUMN_INDEX = 1;

		private final String _alphabet;

		private boolean _loading;

		private Cursor _cursor;

		private static final int FETCH_IMAGE_MSG = 1;

		public ContactsAdapter()
		{
			super(VolatileContactsListActivity.this, null, false); // no autoRequery

			_alphabet = getString(R.string.fast_scroll_alphabet);
			if ((_show & DISPLAY_PHOTO) != 0)
			{
				_handler = new ImageFetchHandler();
				_bitmapCache = new HashMap<ContactId, SoftReference<Bitmap>>();
				_itemsMissingImages = new HashSet<QuickContactBadge>();
			}
		}

		public void setLoading(final boolean loading)
		{
			_loading = loading;
		}

		/** {@inheritDoc} */
		@Override
		public boolean isEmpty()
		{
			return (_loading) ? false : super.isEmpty();
		}

		/** {@inheritDoc} */
		@Override
		public void changeCursor(final Cursor cursor)
		{
			super.changeCursor(_cursor = cursor);
			// Update the indexer for the fast scroll widget
			updateIndexer(cursor);
		}

		/** {@inheritDoc} */
		@Override
		public void bindView(final View view, final Context context, final Cursor cursor)
		{
			if (_cursor == null)
				return;
			final Cache cache = (Cache) view.getTag();
			final int position = _cursor.getPosition();
			if (!cursor.moveToPosition(position))
				throw new IllegalStateException("couldn't move cursor to position " + position);
			bindSectionHeader(view, position);
			bindPhoto(cursor, cache);
			bindData(cursor, cache);
		}

		private void bindData(final Cursor cursor, final Cache cache)
		{
			cache._name.setText(cursor.getString(1 /* Contacts.DISPLAY_NAME */));
			// index
			if ((_show & DISPLAY_DATA) != 0)
			{
				int type;
				String label;
				switch (_mode)
				{
					case MODE_NORMAL:
					case MODE_SEARCH:
					case MODE_PICK_CONTACT:
						if (DEBUG)
						{
							if (cursor.getColumnIndex(RawContacts.ACCOUNT_NAME) != 3)
								LogMarket.wtf(TAG, "column index error");
						}
						cache._label.setText(R.string.from_provider);
						cache._data.setText(cursor.getString(3/*RawContacts.ACCOUNT_NAME*/));
						break;
						
					case MODE_PICK_PHONE:
						if (DEBUG)
						{
							if (cursor.getColumnIndex(Phone.TYPE) != 6)
								LogMarket.wtf(TAG, "column index error");
							if (cursor.getColumnIndex(Phone.LABEL) != 7)
								LogMarket.wtf(TAG, "column index error");
							if (cursor.getColumnIndex(Phone.NUMBER) != 8)
								LogMarket.wtf(TAG, "column index error");
						}
						type = cursor.getInt(6 /* Phone.TYPE */);
						label = cursor.getString(7 /* Phone.LABEL */);
						cache._label.setText(Phone.getTypeLabel(getResources(), type, label));
						cache._data.setText(cursor.getString(8 /* Phone.NUMBER */));
						break;

					case MODE_PICK_POSTAL:
						if (DEBUG)
						{
							if (cursor.getColumnIndex(StructuredPostal.LABEL) != 6)
								LogMarket.wtf(TAG, "column index error");
							if (cursor.getColumnIndex(StructuredPostal.TYPE) != 7)
								LogMarket.wtf(TAG, "column index error");
							if (cursor.getColumnIndex(StructuredPostal.FORMATTED_ADDRESS) != 8)
								LogMarket.wtf(TAG, "column index error");
						}
						label = cursor.getString(6 /* StructuredPostal.LABEL */);
						type = cursor.getInt(7 /* StructuredPostal.TYPE */);
						cache._label.setText(StructuredPostal.getTypeLabel(getResources(), type, label));
						cache._data.setText(cursor.getString(8 /* StructuredPostal.FORMATTED_ADDRESS */));
						break;
					default:
						LogMarket.wtf(TAG, "Unknow mode "+_mode+" in bindData.");

				}
			}
			if ((_show & DISPLAY_PHOTO) != 0 && cache._photoView!=null)
				cache._photoView.setItemId(cursor.getLong(0 /* BaseColumns._ID */));
			if ((_show & DISPLAY_CALLBUTTON) != 0 && cache._callbutton!=null)
				cache._callbutton.setTag(cursor.getLong(0 /* BaseColumns._ID */));

			bindVisibility(cursor, cache);
		}

		private void bindVisibility(final Cursor cursor, final Cache cache)
		{
			int visibility;
			visibility = ((_show & DISPLAY_DATA) != 0) ? View.VISIBLE : View.GONE;
			cache._label.setVisibility(visibility);
			cache._data.setVisibility(visibility);
			boolean withPhone = false;
			final int col = cursor.getColumnIndex(Phone.NUMBER);
			if (col != -1)
				withPhone = !cursor.isNull(col);
			visibility = (((_show & DISPLAY_CALLBUTTON) != 0) && withPhone) ? View.VISIBLE : View.GONE;
			if (cache._rightSide!=null)
				cache._rightSide.setVisibility(visibility);
		}

		/** {@inheritDoc} */
		@Override
		public View newView(final Context context, final Cursor cursor, final ViewGroup parent)
		{
			final View rc;
			final Cache cache;
			final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			rc = inflater.inflate(R.layout.contacts_list_item_photo, parent, false);
			cache = new Cache();
			rc.setTag(cache);
			rc.setOnKeyListener(VolatileContactsListActivity.this);
			cache._header = rc.findViewById(R.id.header);
			cache._headerText = (TextView) rc.findViewById(R.id.header_text);
			if ((_show & DISPLAY_CALLBUTTON)!=0 && cache._callbutton!=null)
			{
				cache._callbutton = (ImageView) rc.findViewById(R.id.call_button);
			}
			if ((_show & DISPLAY_PHOTO) == 0)
			{
				rc.findViewById(R.id.photo).setVisibility(View.GONE);
				rc.findViewById(R.id.noQuickContactPhoto).setVisibility(View.GONE);
			}
			else
			{
				cache._photoView = (ExQuickContactBadge) rc.findViewById(R.id.photo);
				if (cache._photoView!=null)
					cache._photoView.setId(R.id.photo);
			}

			cache._nonQuickContactPhotoView = (QuickContactBadge) rc.findViewById(R.id.noQuickContactPhoto);
			cache._label = (TextView) rc.findViewById(R.id.label);
			cache._data = (TextView) rc.findViewById(R.id.data);
			cache._name = (TextView) rc.findViewById(R.id.name);

			cache._rightSide = rc.findViewById(R.id.right_side);
			if (0 != (_show & DISPLAY_CALLBUTTON))
			{
				if (cache._callbutton!=null)
					cache._callbutton.setOnClickListener(this);
			}
			return rc;
		}

		// ----------------- Manage index
		private void bindSectionHeader(final View view, final int position)
		{
			final Cache cache = (Cache) view.getTag();
			if (((_show & DISPLAY_HEADER) == 0) || !_showHeader)
			{
				cache._header.setVisibility(View.GONE);
			}
			else
			{
				final int section = getSectionForPosition(position);
				if (getPositionForSection(section) == position)
				{
					final String title = _indexer.getSections()[section].toString().trim();
					if (!TextUtils.isEmpty(title))
					{
						cache._headerText.setText(title);
						cache._header.setVisibility(View.VISIBLE);
					}
					else
					{
						cache._header.setVisibility(View.GONE);
					}
				}
				else
				{
					cache._header.setVisibility(View.GONE);
				}
			}
		}

		private void updateIndexer(final Cursor cursor)
		{
			if (_indexer == null)
			{
				_indexer = getNewIndexer(cursor);
			}
			else
			{
				if (Locale.getDefault().equals(
					Locale.JAPAN))
				{
					// if (mIndexer instanceof JapaneseContactListIndexer)
					// ((JapaneseContactListIndexer)
					// mIndexer).setCursor(cursor);
					// else
					_indexer = getNewIndexer(cursor);
				}
				else
				{
					if (_indexer instanceof AlphabetIndexer)
					{
						((AlphabetIndexer) _indexer).setCursor(cursor);
					}
					else
					{
						_indexer = getNewIndexer(cursor);
					}
				}
			}

			final int sectionCount = _indexer.getSections().length;
			if (_sectionPositions == null || _sectionPositions.length != sectionCount)
			{
				_sectionPositions = new int[sectionCount];
			}
			for (int i = 0; i < sectionCount; i++)
			{
				_sectionPositions[i] = AdapterView.INVALID_POSITION;
			}
		}

		@Override
		public Object[] getSections()
		{
			return _indexer.getSections();
		}

		@Override
		public int getPositionForSection(final int sectionIndex)
		{

			if (sectionIndex < 0 || sectionIndex >= _sectionPositions.length)
			{
				return -1;
			}

			if (_indexer == null)
			{
				final Cursor cursor = getCursor();
				if (cursor == null)
				{
					// No cursor, the section doesn't exist so just return 0
					return 0;
				}
				_indexer = getNewIndexer(cursor);
			}

			int position = _sectionPositions[sectionIndex];
			if (position == AdapterView.INVALID_POSITION)
			{
				position = _sectionPositions[sectionIndex] = _indexer.getPositionForSection(sectionIndex);
			}

			return position;
		}

		@Override
		public int getSectionForPosition(final int position)
		{
			// The current implementations of SectionIndexers (specifically the
			// Japanese indexer)
			// only work in one direction: given a section they can calculate
			// the position.
			// Here we are using that existing functionality to do the reverse
			// mapping. We are
			// performing binary search in the mSectionPositions array, which
			// itself is populated
			// lazily using the "forward" mapping supported by the indexer.

			int start = 0;
			int end = _sectionPositions.length;
			while (start != end)
			{

				// We are making the binary search slightly asymmetrical,
				// because the
				// user is more likely to be scrolling the list from the top
				// down.
				final int pivot = start + (end - start) / 4; // $codepro.audit.disable variableDeclaredInLoop

				final int value = getPositionForSection(pivot); // $codepro.audit.disable variableDeclaredInLoop
				if (value <= position)
				{
					start = pivot + 1;
				}
				else
				{
					end = pivot;
				}
			}

			// The variable "start" cannot be 0, as long as the indexer is
			// implemented properly
			// and actually maps position = 0 to section = 0
			return start - 1;
		}

		private SectionIndexer getNewIndexer(final Cursor cursor)
		{
			/*
			 * if
			 * (Locale.getDefault().getLanguage().equals(Locale.JAPAN.getLanguage
			 * ())) { return new JapaneseContactListIndexer(cursor,
			 * SORT_STRING_INDEX); } else {
			 */
			return new AlphabetIndexer(cursor, SUMMARY_NAME_COLUMN_INDEX, _alphabet);
			/* } */
		}

		// ----------------- Manage header line ----------------------------
		private void setTotalContactCountView()
		{
			if (_totalContacts == null)
				return;

			String text = null;
			final int count = getCount();

			switch (_mode)
			{
				case MODE_NORMAL:
				case MODE_SEARCH:
				case MODE_PICK_CONTACT:
					text = getQuantityText(
						count, R.string.listFoundAllContactsZero, R.plurals.listFoundAllContacts);
					break;
				case MODE_PICK_PHONE:
					text = getQuantityText(
						count, R.string.listTotalPhoneContactsZero, R.plurals.listTotalPhoneContacts);
					break;
				case MODE_PICK_POSTAL:
					text = getQuantityText(
						count, R.string.listTotalAllContactsZero, R.plurals.listTotalAllContacts);
					break;
				default:
					LogMarket.wtf(TAG, "Unknown mode "+_mode);
			}
			assert (text != null);
			_totalContacts.setText(text);
		}

		private String getQuantityText(final int count, final int zeroResourceId, final int pluralResourceId)
		{
			if (count == 0)
			{
				return getString(zeroResourceId);
			}
			else
			{
				final String format = getResources().getQuantityText(
					pluralResourceId, count).toString();
				return String.format(format, count);
			}
		}

		// ---------------------------- Manage user interactions
		/** {@inheritDoc} */
		@Override
		public void onItemClick(final AdapterView<?> adapter, final View view, final int position, final long id)
		{
			final Intent intent = new Intent();
			switch (_mode)
			{
				case MODE_PICK_CONTACT:
					new AsyncImport()
					{
						protected void onPostExecute(Uri result) 
						{
							super.onPostExecute(result);
							setResult(RESULT_OK, intent.setData(result));
							finish();
						}
					}.execute(id,true);
					break;
				// case MODE_LEGACY_PICK_PERSON:
				// uri=importMemoryContact(id, true);
				// BUG car impossible de convertir uri to legacy uri
				// setResult(RESULT_OK,
				// intent.setData(Uri.withAppendedPath(People.CONTENT_URI,uri.getLastPathSegment())));
				// break;
				case MODE_PICK_PHONE:
				// case MODE_LEGACY_PICK_PHONE:
				case MODE_PICK_POSTAL:
				// case MODE_LEGACY_PICK_POSTAL:
					final Cursor cursor = getCursor();
					cursor.moveToPosition(position);
					if (DEBUG && cursor.getColumnIndex(Data.RAW_CONTACT_ID) != 5)
						LogMarket.wtf(TAG, "col position error");
					final long rawid = cursor.getLong(5 /* Data.RAW_CONTACT_ID */);
					cursor.close();
					new AsyncImport()
					{
						protected void onPostExecute(Uri uri) 
						{
							super.onPostExecute(uri);
							assert(uri!=null);
							final String filter = 
								(_mode == MODE_PICK_PHONE	// || _mode==MODE_LEGACY_PICK_PHONE	
								) ? Phone.CONTENT_ITEM_TYPE : StructuredPostal.CONTENT_ITEM_TYPE;
							try
							{
								final String value = ((Cache) view.getTag())._data.getText().toString();
								final Cursor cursor = getContentResolver().query(
									Uri.withAppendedPath(
										uri, Contacts.Data.CONTENT_DIRECTORY), colDataId, 
										Data.MIMETYPE + "=? and " + Data.DATA1 + "=?", 
										new String[] { filter, value }, null);
								if (cursor.moveToFirst())
								{
									final long iddata = cursor.getLong(0 /* Data._ID */);
									Uri result = null;
									switch (_mode)
									{
										case MODE_PICK_PHONE:
										case MODE_PICK_POSTAL:
											result = ContentUris.withAppendedId(Data.CONTENT_URI, iddata);
											break;
										// case MODE_LEGACY_PICK_PHONE:
										// result=ContentUris.withAppendedId(Phones.CONTENT_URI,iddata);
										// break;
										// case MODE_LEGACY_PICK_POSTAL:
										// result=ContentUris.withAppendedId(ContactMethods.CONTENT_URI,iddata);
										// break;
										default:
											LogMarket.wtf(TAG, "Invalide mode "+_mode+" in onItemClick.");
									}
									setResult(RESULT_OK, intent.setData(result));
								}
								else
								{
									setResult(RESULT_CANCELED);
								}
								finish();
							}
							finally
							{
								if (cursor != null)
								{
									cursor.close();
								}
							}
						}
					}.execute(rawid,true);
					break;
				case MODE_SEARCH:
				case MODE_NORMAL:
					showVolatileContact(id);
					break;
				default:
					LogMarket.wtf(TAG, "Unknown mode "+_mode);
			}
		}

		@Override
		public void onClick(final View v)
		{
			final long position = ((Long) v.getTag()).longValue();
			new AsyncImport()
			{
				protected void onPostExecute(Uri result) 
				{
					super.onPostExecute(result);
					callOrSmsContact(result, false);
				}
			}.execute(position,true);
		}

		// ----------------- Manage photos ---------------------------------

		private HashMap<ContactId, SoftReference<Bitmap>> _bitmapCache; // Cache des photos, via account:lookup
		private HashSet<QuickContactBadge> _itemsMissingImages; // List de items sans photo (pour le moment)
		private ImageDbFetcher _imageFetcher;

		private ImageFetchHandler _handler;
		private class ImageFetchHandler extends Handler
		{
			@Override
			public void handleMessage(final Message message)
			{
				if (VolatileContactsListActivity.this.isFinishing())
				{
					return;
				}
				switch (message.what)
				{
					case FETCH_IMAGE_MSG:
					{
						final ImageView imageView = (ImageView) message.obj;
						if (imageView == null)
							break;

						final PhotoCache info = (PhotoCache) imageView.getTag();
						if (info == null)
							break;

						final ContactId photoId = info._photoId;
						if (photoId == null)
							break;

						final SoftReference<Bitmap> photoRef = _bitmapCache.get(photoId);
						if (photoRef == null)
						{
							break;
						}
						final Bitmap photo = photoRef.get();
						if (photo == null)
						{
							_bitmapCache.remove(photoId);
							break;
						}

						// Make sure the photoId on this image view has not
						// changed
						// while we were loading the image.
						synchronized (imageView)
						{
							final PhotoCache updatedInfo = (PhotoCache) imageView.getTag();
							final ContactId currentPhotoId = updatedInfo._photoId;
							if (currentPhotoId.equals(photoId))
							{
								imageView.setImageBitmap(photo);
								_itemsMissingImages.remove(imageView);
							}
						}
						break;
					}
					default:
						LogMarket.wtf(TAG, "Unknown message");
				}
			}

			public void clearImageFecthing()
			{
				removeMessages(FETCH_IMAGE_MSG);
			}
		}

		private class ImageDbFetcher implements Runnable
		{
			private final ContactId _photoId;

			private final ImageView _imageView;

			public ImageDbFetcher(final ContactId photoId, final ImageView imageView)
			{
				_photoId = photoId;
				_imageView = imageView;
			}

			public void run()
			{
				if (VolatileContactsListActivity.this.isFinishing())
				{
					return;
				}

				if (Thread.interrupted())
				{
					return;
				}
				Bitmap photo = null;
				try
				{
					photo = ProvidersManager.getPhoto(_photoId);
				}
				catch (final OutOfMemoryError e)
				{
					// Not enough memory for the photo, do nothing.
					if (W) Log.w(TAG,"ImageFetcher",e);
				}
				if (photo == null)
				{
					return;
				}

				_bitmapCache.put(_photoId, new SoftReference<Bitmap>(photo));

				if (Thread.interrupted())
				{
					return;
				}

				// Update must happen on UI thread
				final Message msg = new Message();
				msg.what = FETCH_IMAGE_MSG;
				msg.obj = _imageView;
				_handler.sendMessage(msg);
			}
		}
		
		@TargetApi(11)
		private void bindPhoto(final Cursor cursor, final Cache cache)
		{
			if (cache._photoView==null)
				return;
			final int SUMMARY_LOOKUP_KEY = 0;
			// Set the photo, if requested
			if ((_show & DISPLAY_PHOTO) != 0)
			{
				final ContactId photoId=new ContactId(
					cursor.getString(2/*RawContacts.ACCOUNT_TYPE*/), 
					cursor.getString(3/*RawContacts.ACCOUNT_NAME*/), 
					cursor.getString(4/*VolatileRawContact.LOOKUP*/));

				final boolean useQuickContact = ((_show & USE_QUICK_CONTACT) != 0) && cache._photoView!=null;
				QuickContactBadge viewToUse;
				if (useQuickContact)
				{
					viewToUse = cache._photoView;
					// Build soft lookup reference
					final long contactId = cursor.getLong(0 /* BaseColumns._ID */);
					final String lookupKey = cursor.getString(SUMMARY_LOOKUP_KEY);
					cache._photoView.assignContactUri(Contacts.getLookupUri(contactId, lookupKey));
					cache._photoView.setVisibility(View.VISIBLE);
					if (cache._nonQuickContactPhotoView!=null)
						cache._nonQuickContactPhotoView.setVisibility(View.INVISIBLE);
				}
				else
				{
					viewToUse = cache._nonQuickContactPhotoView;
					if (cache._photoView!=null)
					{
						cache._photoView.setVisibility(View.INVISIBLE);
						if (cache._nonQuickContactPhotoView!=null)
							cache._nonQuickContactPhotoView.setVisibility(View.VISIBLE);
					}
				}

				final int position = cursor.getPosition();
				viewToUse.setTag(new PhotoCache(position, photoId));

				Bitmap photo = null;

				// Look for the cached bitmap
				final SoftReference<Bitmap> ref = _bitmapCache.get(photoId);
				if (ref != null)
				{
					photo = ref.get();
					if (photo == null) // Lose weak reference
						_bitmapCache.remove(photoId);
				}

				// Bind the photo, or use the fallback no photo resource
				if (photo != null)
				{
					viewToUse.setImageBitmap(photo);
				}
				else
				{
					// Cache miss
					if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB)
					{
						viewToUse.setImageToDefault();
					}
					else
					{
						viewToUse.setImageResource(R.drawable.ic_contact_list_picture);
					}

					// Add it to a set of images that are populated asynchronously.
					_itemsMissingImages.add(viewToUse);

					if (_scrollState != OnScrollListener.SCROLL_STATE_FLING)
					{
						// Scrolling is idle or slow, go get the image right now.
						sendFetchImageMessage(viewToUse);
					}
				}
			}

		}

		public void onScrollStateChanged(final AbsListView view, final int scrollState)
		{
			_scrollState = scrollState;
			if (scrollState == OnScrollListener.SCROLL_STATE_FLING)
			{
				clearImageFetching(); // If we are in a fling, stop loading images.
			}
			else if ((_mode & DISPLAY_PHOTO) != 0)
			{
				processMissingImageItems(view);
			}
		}

		private void processMissingImageItems(final AbsListView view)
		{
			for (final ImageView iv : _itemsMissingImages)
			{
				sendFetchImageMessage(iv);
			}
		}
		// Start background load image
		private void sendFetchImageMessage(final ImageView view)
		{
			final PhotoCache info = (PhotoCache) view.getTag();
			if (info == null)
			{
				return;
			}
			final ContactId photoId = info._photoId;
			if (photoId == null)
				return;
			
			_imageFetcher = new ImageDbFetcher(photoId, view);
			synchronized (VolatileContactsListActivity.this)
			{
				// can't sync on sImageFetchThreadPool.
				if (_imageFetchThreadPool == null)
				{
					// Don't use more than 3 threads at a time to update. The thread pool will be
					// shared by all contact items.
					_imageFetchThreadPool = Executors.newFixedThreadPool(IMAGE_POOL_SIZE,new ThreadFactory()
					{
						
						@Override
						public Thread newThread(Runnable r)
						{
							Thread thread=new Thread(r,"Image pool");
							thread.setPriority(IMAGE_POOL_PRIORITY);
							return thread;
						}
					});
				}
				_imageFetchThreadPool.execute(_imageFetcher);
			}
		}

		/**
		 * Stop the image fetching for ALL contacts, if one is in progress we'll
		 * not query the database.
		 * 
		 */
		public void clearImageFetching()
		{
			synchronized (VolatileContactsListActivity.this)
			{
				if (_imageFetchThreadPool != null)
				{
					_imageFetchThreadPool.shutdownNow();
					_imageFetchThreadPool = null;
				}
			}

			if (_handler != null)
			{
				_handler.clearImageFecthing();
			}
		}

	}
	/** Executor for photos. */
	private static ExecutorService _imageFetchThreadPool;

	// In photo tag
	final static class PhotoCache
	{
		public int _position;
		public ContactId _photoId;

		public PhotoCache(final int position, ContactId id)
		{
			_position = position;
			_photoId=id;
		}
	}

	// --------------- Manage menu
	@TargetApi(11)
	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		if (V) Log.v("LIFE", "onCreateOptionsMenu");
//		if ((_mode & MODE_MASK_PICKER) != 0)
//		{
//			return false;
//		}

		getMenuInflater().inflate(R.menu.list, menu);
		_searchMenu=menu.findItem(R.id.menu_search);
		if (HONEYCOMB)
		{
//			_searchMenu.expandActionView();
			// Get the SearchView and set the searchable configuration

			final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
			_searchView = (SearchView) _searchMenu.getActionView();
			_searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
			_searchView.setIconifiedByDefault(true);
		}
		setSearchVisible(_isDone);
	    return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		if (!HONEYCOMB)
		{
			menu.findItem(R.id.menu_accounts).setVisible(_isExtension);
			menu.findItem(R.id.menu_search).setVisible(_isDone);
			//menu.findItem(R.id.menu_import).setVisible(_isDone);
			//menu.findItem(R.id.menu_import_result).setVisible(_adapter._cursor!=null && _adapter._cursor.getCount()!=0);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onSearchRequested()
	{
		if (V) Log.v("LIFE", "onSearchRequested");
		if (!_isDone)
			return false;
		if (HONEYCOMB)
		{
			if (_searchMenu!=null)
			{
				_searchMenu.expandActionView();
			}
			return true;
		}
		else
			return super.onSearchRequested();
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item)
	{
		final Intent intent;
		switch (item.getItemId())
		{
			case R.id.menu_search:
				if (!ProvidersManager.isEmpty())
					onSearchRequested();
				return true;

//			case R.id.menu_std_contacts:
//				intent = _intentViewContacts;
//				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//				startActivity(intent);
//				return true;

			case R.id.menu_accounts:
				intent = new Intent(Settings.ACTION_SYNC_SETTINGS);
				intent.putExtra("authorities", colAuthority);
				startActivity(intent);
				return true;

			case R.id.menu_extensions:
				final SpannedString seq=(SpannedString)getText(R.string.help_need_provider);
				String url=seq.getSpans(0, seq.length(), URLSpan.class)[0].getURL();
				url=url.substring("market://search?q=".length());
				QueryMarket.startSearchMarket(this,url,R.string.extension_plugins,false);
				return true;
				
			case R.id.menu_help:
				startActivity(new Intent(this,HelpActivity.class));
				return true;

			case R.id.menu_import:
				// TODO: Import dans les contacts présent
				break;
				
//			case R.id.menu_import_result:
//				showDialog(DIALOG_IMPORT_ALL_RESULT);
//				_importAllHandler=new ImportAllHandler();
//				_importAllHandler._activity=this;
//				_importAllHandler.execute(_adapter._cursor);
//				break;
				
//			case R.id.menu_market_star:
//				QueryMarket.startSearchMarket(this, "fr.prados.contacts",R.string.market_rate_body);
//				break;

			default:
				LogMarket.wtf(TAG, "Unknown option item selected " + item.getItemId());
		}
		return super.onOptionsItemSelected(item);
	}

	// --------------- Manage context menu
	@Override
	public void onCreateContextMenu(final ContextMenu menu, final View view, final ContextMenuInfo menuInfo)
	{
		// The user guide line remove old the context menu.
		if (HONEYCOMB) return;
		final Cursor cursor = _adapter._cursor;
		if ((cursor == null) || cursor.getCount() == 0)
		{
			return;
		}
		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		if (!cursor.moveToPosition(info.position-1)) return;

		getMenuInflater().inflate(R.menu.context, menu);

		// Setup the menu header
		if (cursor.isAfterLast())
			return;
		if (DEBUG && cursor.getColumnIndex(Contacts.DISPLAY_NAME) != 1)
		{
			LogMarket.wtf(TAG, "error cols");
		}
		menu.setHeaderTitle(cursor.getString(1 /* Contacts.DISPLAY_NAME */));

		// View contact details
		if ((_show & DISPLAY_CALLBUTTON) != 0)
		{
			if (DEBUG && cursor.getColumnIndex(Phone.NUMBER) != 6)
				LogMarket.wtf(TAG, "error cols");
			final String number = cursor.getString(6 /* Phone.NUMBER */);
			if (number != null)
			{
				menu.findItem(R.id.menu_call).setVisible(true);
				menu.findItem(R.id.menu_send_sms).setVisible(true);
			}
		}
	}

	@Override
	public boolean onContextItemSelected(final MenuItem item)
	{
		final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		final long id = info.id;

		switch (item.getItemId())
		{
			case R.id.menu_view_contact:
				showVolatileContact(id);
				break;

			case R.id.menu_call:
				new AsyncImport()
				{
					protected void onPostExecute(Uri result) 
					{
						super.onPostExecute(result);
						callOrSmsContact(result, false);
					}
				}.execute(id,true);
				break;

			case R.id.menu_send_sms:
				new AsyncImport()
				{
					protected void onPostExecute(Uri result) 
					{
						super.onPostExecute(result);
						callOrSmsContact(result, true);
					}
				}.execute(id,true);
				break;

			case R.id.menu_import:
				showDialog(DIALOG_IMPORT);
				new AsyncImport()
				{
					protected void onPostExecute(Uri contactUri) 
					{
						super.onPostExecute(contactUri);
						contactUri = importMemoryContact(id, false);
						if (contactUri != null)
							startActivity(new Intent(Intent.ACTION_VIEW, contactUri));
						removeDialog(DIALOG_IMPORT);
					}
				}.execute(id,false);
				break;

			case R.id.menu_add_star:
				showDialog(DIALOG_ADDSTAR);
				starMemoryContact(id);
				removeDialog(DIALOG_ADDSTAR);
				break;

			default:
				return super.onContextItemSelected(item);
		}
		return true;
	}

	@Override
	public boolean onKey(final View v, final int keyCode, final KeyEvent event)
	{
		if (event.getMetaState() == 0)
		{
			if (keyCode==KeyEvent.KEYCODE_CALL)
			{
				final AbsListView list = getListView();
				final int position = list.getCheckedItemPosition();
				list.getItemIdAtPosition(position);
				if (position != AdapterView.INVALID_POSITION)
				{
					new AsyncImport()
					{
						protected void onPostExecute(Uri result) 
						{
							super.onPostExecute(result);
							callOrSmsContact(result, false);
						}
					}.execute(position,true);
					return true;
				}
			}
		}
		return false;
	}

	@SuppressLint("NewApi")
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		if (HONEYCOMB)
		{
			if (keyCode==KeyEvent.KEYCODE_SEARCH)
			{
				if (_searchMenu!=null)
				{
					_searchMenu.expandActionView();
					return true;
				}
			}
		}
		if (keyCode==KeyEvent.KEYCODE_I)
		{
			View v=getListView().getSelectedView();
			if (v!=null)
			{
				Cache cache=(Cache)v.getTag();
				cache._photoView.onClick(v);
				return true;
			}
		}
		return super.onKeyUp(keyCode, event);
	}
	// -------------------- Commands
	private void startQuery()
	{
		hideSearchbar();
		ProvidersManager.waitInit();
		if (D) Log.d(TAG, "startQuery(\""+_lastQuery+"\")");
		getListView().requestFocus();
		getListView().setSelection(0);
		hideKeyboard();
		// Hide search UI
		hideSearchbar();

		_emptyText.setText(R.string.currentQuery);

		if (_lastQuery!=null && _lastQuery.length() != 0)
		{
			// Delay for clean windows before
			_queryHandler._pending=true; // Because onResume() is call just after onRestoreInstance and the message must be empty
			_handler.post(new Runnable()
			{
				@Override
				public void run()
				{
					_queryHandler.startQuery( _queryMode, new String[] { _lastQuery }, false);
				}
			});
		}
	}
	
	// TODO : Invoke clear suggestions history in options menu ?
//	private void clearHistory()
//	{
//		_suggestions.clearHistory();
//	}
	
	private boolean checkContact()
	{
		final Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse("content://contacts/people/1"));
		final List<ResolveInfo> resolve=getPackageManager().queryIntentActivities(intent, 0);
	    if (resolve.size()==0)
	    {
	    	final AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.help_need_account_app_title);
			builder.setCancelable(true);
			builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
					finish();
				}
			});
			builder.setMessage(R.string.help_need_account_app);
			final AlertDialog dialog = builder.create();
			dialog.setCanceledOnTouchOutside(false);
			dialog.show();
			return false;
	    }
	    return true;
	}
	private void showVolatileContact(final long id)
	{
		new AsyncImport()
		{
			@Override
			protected void onPostExecute(Uri contactUri)
			{
				super.onPostExecute(contactUri);				
				if (contactUri != null)
				{
					startActivity(new Intent(Intent.ACTION_VIEW, contactUri));
				}
			}
		}.execute(id,true);
	}

	private void starMemoryContact(final long id)
	{
		new AsyncImport()
		{
			@Override
			protected void onPostExecute(Uri contactUri)
			{
				super.onPostExecute(contactUri);				
				if (contactUri != null)
				{
					final ContentValues values = new ContentValues(1);
					values.put(Contacts.STARRED, 1);
					getContentResolver().update(contactUri, values, null, null);
					startActivity(new Intent(Intent.ACTION_VIEW, contactUri));
				}
			}
		}.execute(id,false);
	}

	private Uri importMemoryContact(final long id, final boolean deleted)
	{
		ProvidersManager.init();
		final Uri uri=ProvidersManager.importVolatileContactToAndroid(id, deleted, Application.context);
//		if (!deleted)
//		{
//			long rawId=Long.parseLong(uri.getLastPathSegment());
//			return ProvidersManager.fixeSyncContactInAndroid(getResources(),
//					getContentResolver(), 
//					rawId);
//		}
		return uri;
	}

	/**
	 * Calls the contact which the cursor is point to.
	 * 
	 * @return true if the call was initiated, false otherwise
	 */
	private void callOrSmsContact(final Uri contactUri, final boolean sendSms)
	{
		if (contactUri == null)
			return;
		String phoneNumber = null;
		final Cursor phonesCursor = Application.context.getContentResolver().query(
			Uri.withAppendedPath(
				contactUri, Contacts.Data.CONTENT_DIRECTORY), colForCallOrSms, 
				Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE+"'", null, null);
		if (phonesCursor != null && phonesCursor.moveToFirst())
		{
			if (phonesCursor.getCount() == 1)
			{
				// only one number, call it.
				phoneNumber = phonesCursor.getString(1); // Phone.NUMBER
			}
			else
			{
				phonesCursor.moveToPosition(-1);
				int cntMobile = 0;
				while (phonesCursor.moveToNext())
				{
					if (phonesCursor.getInt(2/* Phone.IS_SUPER_PRIMARY */) != 0)
					{
						// Found super primary, call it.
						phoneNumber = phonesCursor.getString(1/* Phone.NUMBER */);
						break;
					}
					if (sendSms && phonesCursor.getInt(3/* Phone.TYPE */) == Phone.TYPE_MOBILE)
					{
						++cntMobile;
						phoneNumber = phonesCursor.getString(1/* Phone.NUMBER */);
					}
				}
				if (cntMobile > 1)
				{
					phoneNumber = null;
				}
			}

			if (phoneNumber == null)
			{
				// Display dialog to choose a number to call.
				final PhoneDisambigDialog phoneDialog = new PhoneDisambigDialog(this, phonesCursor, sendSms);
				phoneDialog.show();
			}
			else
			{
				if (sendSms)
				{
					final Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
						"sms", phoneNumber.toString(), null));
					startActivity(intent);
				}
				else
				{
					final Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts(
						"tel", phoneNumber.toString(), null));
					startActivity(intent);
				}
			}
		}
		else
		{
			LogMarket.wtf(TAG, "unknown status");
		}
	}
}
