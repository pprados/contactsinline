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
package fr.prados.contacts.tools;

import static fr.prados.contacts.Constants.I;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;
public class TOAContentProvider extends ContentProvider
{
    private static final int DATABASE_VERSION = 1;
    
	private static final String TAG="TOAContent";
	private I18nPhonesHelper _helper;
	
	@Override
	public boolean onCreate()
	{
		_helper=new I18nPhonesHelper(getContext());
		return false;
	}

	@Override
	public String getType(Uri uri)
	{
		return null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder)
	{
		final SQLiteDatabase database=_helper.getReadableDatabase();
		return database.query(I18nPhonesHelper.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs)
	{
		throw new IllegalArgumentException("delete not implemented");
	}

	@Override
	public Uri insert(Uri uri, ContentValues values)
	{
		throw new IllegalArgumentException("insert not implemented");
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs)
	{
		throw new IllegalArgumentException("update not implemented");
	}

	/**
	 * Extract local phone number format info from MCC.
	 * @version 1.0
	 * @since 1.0
	 * @author Philippe PRADOS
	 */
	// http://www.reigndesign.com/blog/using-your-own-sqlite-database-in-android-applications/
	// create index idx_code on i18nphones (code);
	public static class I18nPhonesHelper extends SQLiteOpenHelper 
	{

	    private static final String NAME = "i18n_phone_number.db";
	    private static final String TABLE_NAME = "i18nphones";
	    
	    private static final String SQL_TABLE_CREATE =
	                "CREATE TABLE " + TABLE_NAME + " (" +
	                TOAPhoneNumberFormats.COL_MCC + " INTEGER PRIMARY KEY, " +
	                TOAPhoneNumberFormats.COL_COUNTRY + " TEXT, " +
	                TOAPhoneNumberFormats.COL_ISO639 + " TEXT, " +
	                TOAPhoneNumberFormats.COL_INTPREFIX + " TEXT, " +
	                TOAPhoneNumberFormats.COL_EXIT + " TEXT, "+
	                TOAPhoneNumberFormats.COL_FORMAT + " TEXT, "+
	    			TOAPhoneNumberFormats.COL_TRUNK + " TEXT);";
	    private static final String SQL_TABLE_DROP =
	    	"DROP TABLE IF EXISTS "+TABLE_NAME;

		private AssetManager _assetManager;
		I18nPhonesHelper(Context context) 
		{
	        super(context, NAME, null, DATABASE_VERSION);
	        _assetManager=context.getAssets();
	    }

		private static final String MATCH_QUOTE="(\"[^\"]*\"|[^,]*)";
	    @Override
	    public void onCreate(SQLiteDatabase db) 
	    {
			if (I) Log.i(TAG,"Create international phone database");
	        db.execSQL(SQL_TABLE_CREATE);
	        try
			{
	        	
	        	final Pattern pattern=Pattern.compile(MATCH_QUOTE+","+MATCH_QUOTE+","+MATCH_QUOTE+","+MATCH_QUOTE+","+MATCH_QUOTE+","+MATCH_QUOTE+","+MATCH_QUOTE+","+MATCH_QUOTE);
				final InputStream in=new GZIPInputStream(_assetManager.open("phones.csv", AssetManager.ACCESS_STREAMING));
				final BufferedReader reader=new BufferedReader(new InputStreamReader(in,"UTF-8"));
				final ContentValues values=new ContentValues();
				boolean first=true;
				String line;
				while ((line=reader.readLine())!=null)
				{
					if (first)
					{
						first=false;
						continue;
					}

					final Matcher matcher=pattern.matcher(line); // $codepro.audit.disable variableDeclaredInLoop
					matcher.find();
					values.clear();
					final String mcc=trimQuote(matcher.group(1)); // $codepro.audit.disable variableDeclaredInLoop
					if (mcc.length()!=0)
					{
			        	values.put(TOAPhoneNumberFormats.COL_MCC,Integer.parseInt(mcc));
			        	values.put(TOAPhoneNumberFormats.COL_COUNTRY,trimQuote(matcher.group(2)));
			        	values.put(TOAPhoneNumberFormats.COL_ISO639, trimQuote(matcher.group(3)));
			        	values.put(TOAPhoneNumberFormats.COL_INTPREFIX,trimQuote(matcher.group(4)));
			        	values.put(TOAPhoneNumberFormats.COL_EXIT,trimQuote(matcher.group(5)));
			        	values.put(TOAPhoneNumberFormats.COL_TRUNK,trimQuote(matcher.group(6)));
			        	String format=trimQuote(matcher.group(7));
			        	if (format.length()==0)
			        	{
			        		format="+i n";
			        	}
			        	values.put(TOAPhoneNumberFormats.COL_FORMAT,format);
			        	// values.put(COL_MOBILE, ... Mobile
			        	db.insert(TABLE_NAME, "", values);
					}
				}
				reader.close();
				in.close();
			}
			catch (IOException e)
			{
				LogMarket.wtf(TAG,"Init I18N Phones fails",e);
			}
	        
	    }
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			db.execSQL(SQL_TABLE_DROP);
			onCreate(db);
		}
	    private static String trimQuote(String str)
	    {
	    	if (str.length()==0) return str;
	    	return (str.charAt(0)=='"') 
	    		? str.substring(1,str.length()-1)
	    		: str;
	    	
	    }
	}
	
}
