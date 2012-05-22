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
import static fr.prados.contacts.Constants.*;
import static fr.prados.contacts.Constants.V;
import static fr.prados.contacts.Constants.W;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.SparseArray;
import fr.prados.contacts.Application;
public class TOAPhoneNumberFormats
{
	private static final String TAG="Phone";
	public static final String AUTHORITY="fr.prados.contacts.tools.TOA";
    public static final String COL_MCC="mcc";
    public static final String COL_COUNTRY="country";
    public static final String COL_INTPREFIX="intprefix";
    public static final String COL_ISO639="iso639";
    public static final String COL_EXIT="exit";
    public static final String COL_TRUNK="trunk";
    public static final String COL_FORMAT="format";
    public static final Uri CONTENT_URI =Uri.parse("content://"+AUTHORITY);

	private static final String[] COLS=new String[]{COL_ISO639,COL_INTPREFIX,COL_EXIT,COL_TRUNK,COL_FORMAT};
	private static final String[] COLS_MCC=new String[]{COL_MCC};
	private static final int POS_ISO639=0;
	private static final int POS_INTPREFIX=1;
	private static final int POS_EXIT=2;
	private static final int POS_THRUNK=3;
	private static final int POS_FORMAT=4;
    private static final String WHERE_MCC=COL_MCC+"=";
    private static final String WHERE_ISO639=COL_ISO639+"=";
    private static final String WHERE_INTPREFIX=COL_INTPREFIX+"=";

	
	public static TOAPhoneNumberFormats _phoneTOAContext;
	private static final SparseArray<TOAPhoneNumberFormats> _cacheMCC=new SparseArray<TOAPhoneNumberFormats>();
	private static final SparseArray<TOAPhoneNumberFormats> _cacheIntPrefix=new SparseArray<TOAPhoneNumberFormats>();
//	private static final HashMap<String,TOAPhoneNumberFormats> _cacheIso639=new HashMap<String,TOAPhoneNumberFormats>();
//	private static final HashMap<Integer, Integer> _cacheIntPrefix=new HashMap<Integer,Integer>();
	private static final ContentResolver _resolver=Application.context.getContentResolver();
	static
	{
		startup(Application.context);
	}
	private static void startup(Context context)
	{
		int mcc=getPhoneMCC();
		if (mcc==0)
			mcc=310; // Default US
		if (EMULATOR)
			mcc=208; // Force France local in debug mode
		_phoneTOAContext=getTOAContextFromMCC(mcc);
	}
	
	public String _country;
	public String _iso639;
	public int 	  _intPrefix;
	public Pattern _trunk;
	public String _exit;
	public String _local;
	public String _format;
	public String _mobile;

	public TOAPhoneNumberFormats(String iso639,int intPrefix,String trunk,String exit,String local,String format)
	{
		_iso639=iso639;
		_intPrefix=intPrefix;
		_trunk=Pattern.compile("^("+trunk+")(.*)");
		if (trunk!=null && trunk.length()==0) _trunk=null;
		_local=local;
		if (local!=null && local.length()==0) _local=null;
		_exit=exit;
		if (exit!=null && exit.length()==0) _exit=null;
		_format=format;
		if (format==null)
			format="+i n";
	}
	private static final String _i18nPhoneRoute=
		"1(?:24[26]|26[48]|284|34[05]|441|473|649|664|67[01]|684|721|758|767|78[47]|8[02]9|86[89]|876|939)?|"+
		"2(?:0|7|[1-69]\\d)|"+
		"3(?:[0-469]|[578]\\d)|"+
		"4(?:[013-9]|2\\d)|"+
		"5(?:[1-8]|[09]\\d)|"+
		"6(?:[0-6]|[7-9]\\d)|"+
		"7|"+
		"8(?:[1246]|[578]\\d)|"+
		"9(?:[0-58]|[679]\\d)";
	private static final Pattern _toaPhonePattern=
		Pattern.compile("^[^0-9*#+(]*(?:(?:\\+|0(?:0|11))("+
			  _i18nPhoneRoute+")|166)?(?:[^(]*\\((\\d+)\\))?[^0-9*#+]*([0-9][-0-9*#+. ]*)(?:[^(]*\\([^)]*\\))?$");
	private static final Pattern _intPhonePattern=
		Pattern.compile("^("+_i18nPhoneRoute+")");
	/**
	 * Split phone number.
	 * Note that this function does not strictly care the country calling code with
     * 3 length (like Morocco: +212)
	 * @param phone
	 * @return array with parts. 
	 * [0]= international prefix or null
	 * [1]= Trunk, city code or null
	 * [2]= subscriber number
	 */
	private static String[] splitPhoneNumber(CharSequence phone) throws IllegalArgumentException
	{
		final String[] rc=new String[3];
		final Matcher matcher=_toaPhonePattern.matcher(phone);
		if (!matcher.matches())
		{
			final String msg=phone+" is not a phone number";
			if (W) Log.w(TAG,msg);
			throw new IllegalArgumentException(msg);
		}
		rc[0]=matcher.group(1)!=null?PhoneNumberUtils.stripSeparators(matcher.group(1)):null;
		rc[1]=matcher.group(2)!=null?PhoneNumberUtils.stripSeparators(matcher.group(2)):null;
		rc[2]=matcher.group(3)!=null?PhoneNumberUtils.stripSeparators(matcher.group(3)):null;
		return rc;
	}
	
	public static TOAPhoneNumberFormats getTOAContextFromMCC(int mobileCountryCode)
	{
		TOAPhoneNumberFormats rc=_cacheMCC.get(mobileCountryCode);
		if (rc!=null) return rc;
		Cursor cursor=null;
		try
		{
			cursor=_resolver.query(CONTENT_URI, COLS, WHERE_MCC+mobileCountryCode, null,null);
			if (cursor==null) return null;
			if (!cursor.moveToFirst()) return null;
	 		rc=
	 			new TOAPhoneNumberFormats(
	 				cursor.getString(POS_ISO639),
	 				cursor.getInt(POS_INTPREFIX),
	 				cursor.getString(POS_THRUNK),
	 				null,
	 				cursor.getString(POS_EXIT),
	 				cursor.getString(POS_FORMAT));
	 		_cacheMCC.put(mobileCountryCode, rc);
	 		_cacheIntPrefix.put(rc._intPrefix, rc);
	 		return rc;
		}
		finally
		{
			if (cursor!=null) cursor.close();
		}
	}
	
	public static TOAPhoneNumberFormats getTOAContextFromIntPrefix(int intprefix)
	{
		TOAPhoneNumberFormats rc=_cacheIntPrefix.get(intprefix);
		if (rc!=null) return rc;
		Cursor cursor=null;
		try
		{
			cursor=_resolver.query(CONTENT_URI, COLS, WHERE_INTPREFIX+intprefix, null,null);
			if (!cursor.moveToFirst()) return null;
	 		rc=
	 			new TOAPhoneNumberFormats(
		 				cursor.getString(POS_ISO639),
		 				cursor.getInt(POS_INTPREFIX),
		 				cursor.getString(POS_THRUNK),
		 				null,
		 				cursor.getString(POS_EXIT),
		 				cursor.getString(POS_FORMAT));
	 		_cacheIntPrefix.put(rc._intPrefix, rc);
	 		return rc;
		}
		finally
		{
			if (cursor!=null) cursor.close();
		}
	}
	
	private static int getMCCFromISO639(String iso639)
	{
		if (iso639==null || iso639.length()==0) return 0;
		Cursor cursor=null;
		try
		{
			cursor=_resolver.query(CONTENT_URI, COLS_MCC, 
					WHERE_ISO639+DatabaseUtils.sqlEscapeString(iso639.toLowerCase()), null,null);
			if (!cursor.moveToFirst()) 
			{
				LogMarket.wtf(TAG, "Not found ISO639 code "+iso639);
				return 0;
			}
			return cursor.getInt(0/*MCC*/);
		}
		finally
		{
			if (cursor!=null) cursor.close();
		}
	}
	// subscriber number
	// http://en.wikipedia.org/wiki/Local_conventions_for_writing_telephone_numbers
	// http://www.wtng.info/wtng-33-fr.html
	// http://www.itu.int/oth/T0202.aspx?parent=T0202
	// MCC and MNC (mobile country code and mobile network code)
	// http://www.howtocallabroad.com/results.php?callfrom=france&callto=thuraya
	public static CharSequence toTOA(CharSequence phone,TOAPhoneNumberFormats toaContext)
	{
		try
		{
			// Split country, optional number and subscriber number. (Optional must be trunk or local code)
			// Merge optional and subscriber number
			// Remove country is is not in good place
			// Remove trunk
			// Rebuilde the TOA number
			StringBuilder toa=new StringBuilder();
			String[] split=splitPhoneNumber(phone);
			String countryCode=split[0];
			CharSequence optional=split[1];
			String subscriberNumber=split[2];
			
			boolean allNul=true;
			for (int i=0;i<subscriberNumber.length();++i)
				if (subscriberNumber.charAt(i)!='0')
				{
					allNul=false;
					break;
				}
			if (allNul)
				return null;
			// If local or trunk is in parentheses,
			// merge optional number and subscriber number
			if (optional!=null)
			{
				subscriberNumber=optional+subscriberNumber;
			}
			
			// If country code is with prefix, but in subscriber number
			// split countrycode and subscribernumber
			if ((countryCode==null || countryCode.length()==0) && subscriberNumber.length()>10)
			{
				Matcher matcher=_intPhonePattern.matcher(subscriberNumber);
				if (matcher.find())
				{
					countryCode=matcher.group(0);
					subscriberNumber=subscriberNumber.substring(countryCode.length());
				}
			}
			TOAPhoneNumberFormats context;
			int iCountryCode=0;
			if (countryCode==null)
			{
				
				context=toaContext;
				iCountryCode=toaContext._intPrefix;
			}
			else
			{
				context=getTOAContextFromIntPrefix(iCountryCode=Integer.parseInt(countryCode));
			}
			// Add local ?
			if ((subscriberNumber.length()<7) && toaContext._local!=null)
				subscriberNumber=toaContext._local+subscriberNumber;
			
			// Remove trunk ?
			if (context._trunk!=null)
			{
				Matcher matcher=context._trunk.matcher(subscriberNumber);
				if (matcher.find()) 
				{
					subscriberNumber=matcher.group(2);
				}
			}

			toa.append('+').append(iCountryCode).append(subscriberNumber);
			
			if (V) Log.v(TAG,phone+" => TOA: "+toa);
			return toa;
		}
		catch (Exception e)
		{
			LogMarket.wtf(TAG, "TOAPhoneNumber "+phone,e);
			return phone; // No update
		}
	}
	public static CharSequence toTOA(CharSequence phone)
	{
		return toTOA(phone,_phoneTOAContext);
	}
	
	public static CharSequence toTOAFormat(CharSequence phone,TOAPhoneNumberFormats toaContext)
	{
		if (toaContext==null)
			return phone;
		CharSequence toa=toTOA(phone,toaContext);
		if (toa==null) return null;
		final String[] split=splitPhoneNumber(toa);
		TOAPhoneNumberFormats numberFormat=getTOAContextFromIntPrefix(Integer.parseInt(split[0]));
		
		final String custnumber=(split[1]!=null) ? split[1]+split[2] : split[2];
		final String format=numberFormat._format;
		final int len=format.length();
		int j=0;
		StringBuilder builder=new StringBuilder();
		for (int i=0;i<len;++i)
		{
			char c=format.charAt(i);
			switch (c)
			{
				case 'i' :
					builder.append(split[0]);
					break;
				case 'n':
					if (j<custnumber.length())
					{
						builder.append(custnumber.charAt(j++));
					}
					break;
				default:
					builder.append(c);
					break;
			}
		}
		if (j<custnumber.length())
			builder.append(custnumber.substring(j));
		if (V) Log.v(TAG,phone+"=>"+builder+" ("+format+")");
		return builder;
	}
	
	/**
	 * Return subscribe number, after a TOA adjustement.
	 * @param phone
	 * @param defCountry
	 * @param defExit
	 * @return
	 */
	public static final CharSequence extractSubscriberNumber(CharSequence phone,TOAPhoneNumberFormats toaContext)
	{
		final CharSequence toa=toTOA(phone, toaContext);
		if (toa==null)
			return null;
		return splitPhoneNumber(toa)[2].toString();
	}
	public static final CharSequence extractSubscriberNumber(CharSequence phone)
	{
		return extractSubscriberNumber(phone,_phoneTOAContext);
	}

	public static final int getServerMCC(String host)
	{
		URL url=null;
		try
		{
			url=new URL("http://geoiptool.com/data.php?IP="+URLEncoder.encode(host));
			
			SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();

            XMLReader xr = sp.getXMLReader();
            class MyHandler extends DefaultHandler
            {
            	String iso639;
	        	@Override
	            public void startElement(String uri, String localName, String name, Attributes attributes)
                	throws SAXException 
                {
            		if ("marker".equals(name))
            		{
            			iso639=attributes.getValue("code");
            		}
                }
            }
            MyHandler handler=new MyHandler();
            xr.setContentHandler(handler);
            xr.parse(new InputSource(url.openStream()));
			int mcc=getMCCFromISO639(handler.iso639);
            if (I) Log.i(TAG,"Country code for host "+host+" is '"+handler.iso639+"' and MCC is "+mcc);
			return mcc;
		}		
		catch (UnknownHostException e) // $codepro.audit.disable logExceptions
		{
			LogMarket.wtf(TAG, "Unknown host "+host);
			return 0;
		}
		catch (MalformedURLException e)
		{
			LogMarket.wtf(TAG, "GeoIP URLException",e);
			return 0;
		}
		catch (IOException e)
		{
			LogMarket.wtf(TAG, "GeopIP IOException ",e);
			return 0;
		}
		catch (SAXException e)
		{
			LogMarket.wtf(TAG, "GeoIP SAXException ",e);
			return 0;
		}
		catch (ParserConfigurationException e)
		{
			LogMarket.wtf(TAG, "GeoIP",e);
			return 0;
		}
	}
	public static final int getPhoneMCC()
	{
		if (_tm==null)
		{
			_tm=(TelephonyManager)Application.context.getSystemService(Context.TELEPHONY_SERVICE);
		}
		if (_tm!=null)
		{
			String mccncc=_tm.getNetworkOperator();
			int mcc=0;
			if (mccncc.length()>3)
			{
				mcc=Integer.parseInt(_tm.getNetworkOperator().substring(0,3));
	 		}
			return mcc;
		}
		else
			return 0;
	}
	private static TelephonyManager _tm;
	
}
