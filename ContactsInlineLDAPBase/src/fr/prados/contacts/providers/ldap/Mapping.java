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

import static fr.prados.contacts.Constants.D;
import static fr.prados.contacts.Constants.DNTOADDR;
import static fr.prados.contacts.Constants.DNTONAME;
import static fr.prados.contacts.Constants.E;
import static fr.prados.contacts.Constants.I;
import static fr.prados.contacts.Constants.NB_RETRY_DNTO;
import static fr.prados.contacts.Constants.V;
import static fr.prados.contacts.Constants.W;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.WeakHashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Environment;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;

import fr.prados.contacts.Application;
import fr.prados.contacts.VolatileContact;
import fr.prados.contacts.VolatileContact.Import;
import fr.prados.contacts.VolatileData;
import fr.prados.contacts.VolatileRawContact;
import fr.prados.contacts.lib.R;
import fr.prados.contacts.tools.LogMarket;
import fr.prados.contacts.tools.Photos;
import fr.prados.contacts.tools.TOAPhoneNumberFormats;

public final class Mapping implements Cloneable
{
	private static final String TAG="Mapping";
	
	private static final String MAP_STANDARD="standard.xml";
	private static final String MAP_NO_NORMALIZE="no_normalize.xml";
	
	int timeout; // Global timeout for all request
	int connectionTimeout=5; // Connexion timeout
	int requesttimeout=5; // Request timeout
	int sizelimit=50000;
	int maxrecord=20;
	SearchScope scope=SearchScope.SUB;
	int _mcc=0;
	String classFilter="";
	String[] namesAttr=emptyArray;
	Phonetic phonetic;
	String[] phonesAttr=emptyArray;
	String[] emailsAttr=emptyArray;
	String[] attrsList=emptyArray;
	String[] photoAttrsList=emptyArray;
	
	private static final String[] emptyArray=new String[0];
	private HashMap<String,Map> types=new HashMap<String,Map>();
	private HashMap<String,Clazz> classes=new HashMap<String,Clazz>();

	@SuppressWarnings("unchecked")
	@Override
	public Object clone()
	{
		try
		{
			super.clone();
			Mapping other= (Mapping)super.clone();
			other.types=(HashMap<String,Map>)types.clone();
			other.classes=new HashMap<String,Clazz>(classes.size());
			for (String key:classes.keySet())
			{
				other.classes.put(key, (Clazz)classes.get(key).clone());
			}
			return other;
		}
		catch (CloneNotSupportedException e) // $codepro.audit.disable logExceptions
		{
			LogMarket.wtf(TAG, "Clone error");
			return null;
		}
	}
	static private class Map
	{
		Class<?> _type;
		String _mimeType;
		String _col;
		Class<?> _extraType;
		String _extraCol;
		Object _extraValue;
		Conv _conv;
	};
	
	static private class Clazz
	{
		@SuppressWarnings("unchecked")
		@Override
		public Object clone()
		{
			Clazz other=new Clazz();
			other._may=(HashMap<String,Map>)_may.clone();
			return other;
		}
		HashMap<String,Map> _may=new HashMap<String,Map>();
	}
	interface PostJob
	{
		public void run(LDAPConnection conn);
	}
	interface Conv
	{
		public void putForType(Stack<PostJob> jobs,VolatileData data,Class<?> type,int mcc,String name,Attribute attr,int pos);
	}
	static private final HashMap<String,Conv> _convs=new HashMap<String,Conv>();
	interface Phonetic
	{
		public String phonetic(String data);
	}
	static private final HashMap<String,Phonetic> _convsPhonetics=new HashMap<String,Phonetic>();
	static private final Map _emptyMap=new Map();
	static private final WeakHashMap<String,HashMap<String,String>> _cacheDn=new WeakHashMap<String,HashMap<String,String>>();
	static private final Conv _stdConv=new Conv()
	{

		@Override
		public void putForType(Stack<PostJob> jobs,VolatileData data,Class<?> type,int mcc,String name,Attribute attr,int pos)
		{
			if (type==null)
				return;
			final String value=attr.getValues()[pos];
			if (type==String.class)
				data.put(name,value);
			else if (type==Boolean.class)
				data.put(name, Integer.valueOf(value).intValue()!=0);
			else if (type==Byte.class)
				data.put(name, Byte.valueOf(value).byteValue());
			else if (type==byte[].class)
				data.put(name, attr.getValueByteArray());
			else if (type==Short.class)
				data.put(name, Short.valueOf(value).shortValue());
			else if (type==Integer.class)
				data.put(name, Integer.valueOf(value));
			else if (type==Long.class)
				data.put(name,Long.valueOf(value));
			else if (type==Float.class)
				data.put(name, Float.valueOf(value).floatValue());
			else if (type==Double.class)
				data.put(name, Double.valueOf(value).doubleValue());
			else if (type==Bitmap.class)
			{
				Bitmap bitmap=Photos.extractFace(attr.getValueByteArray());
				ByteArrayOutputStream os = new ByteArrayOutputStream(bitmap.getWidth()*bitmap.getHeight()*3);
				bitmap.compress(Bitmap.CompressFormat.PNG, 0, os); // FIXME: bug si isRecycled()
				data.put(name,os.toByteArray());
			}
			else
				LogMarket.wtf(TAG, "Unknown type "+type);
		}
		@Override
		public String toString()
		{
			return "type conv";
		}
	};
	static
	{
		_convs.put("dollars", new Conv()
		{

			@Override
			public void putForType(Stack<PostJob> jobs,VolatileData data,Class<?> type,int mcc,String name,Attribute attr,int pos)
			{
				assert(type==String.class);
				data.put(name, attr.getValues()[pos].replace('$', '\n'));
			}
			@Override
			public String toString()
			{
				return "dollars";
			}
			
		});
		_convs.put("firstCaps", new Conv()
		{
			@Override
			public void putForType(Stack<PostJob> jobs,VolatileData data,Class<?> type,int mcc,String name,Attribute attr,int pos)
			{
				assert(type==String.class);
				StringBuilder builder=new StringBuilder(attr.getValues()[pos].toLowerCase());
				builder.setCharAt(0, Character.toUpperCase(builder.charAt(0)));
				data.put(name, builder.toString());
			}
			
		});
		_convs.put("uppercase", new Conv()
		{
			@Override
			public void putForType(Stack<PostJob> jobs,VolatileData data,Class<?> type,int mcc,String name,Attribute attr,int pos)
			{
				assert(type==String.class);
				data.put(name, attr.getValues()[pos].toUpperCase());
			}
			
		});
		_convs.put("dnToName",new Conv()
		{

			@Override
			public void putForType(Stack<PostJob> jobs,final VolatileData data, final Class<?> type, int mcc,final String name, final Attribute attr,final int pos)
			{
				if (DNTONAME)
				{
					assert (attr!=null);
					jobs.push(new PostJob()
					{
						@Override
						public void run(LDAPConnection conn)
						{
							HashMap<String,String> cache=_cacheDn.get(conn.getConnectedAddress());
							if (cache==null)
							{
								cache=new HashMap<String,String>();
								_cacheDn.put(conn.getConnectedAddress(), cache);
							}
							for (int nbTry=0;nbTry<NB_RETRY_DNTO;++nbTry)
							{
								try
								{
									if (conn.isConnected())
									{
										String strDn=attr.getValues()[pos];
										DN dn=new DN(strDn);
										String val=cache.get(strDn);
										if (val==null)
										{
											if (D) Log.d(TAG,"Post: getName "+dn+" with "+conn.getConnectedAddress()+"...");
											SearchResultEntry entry=conn.getEntry(attr.getValues()[pos],"cn");
											if (D) Log.d(TAG,"Post: get "+dn+" done");
											val=entry.getAttributeValue("cn");
											cache.put(strDn, val);
										}
										else
										{
											if (D) Log.d(TAG,"Post: getName "+dn+" from cache= "+val);
										}
										if (val!=null)
										{
											data.put(name,val);
											
											return;
										}
										data.put(name,attr.getValue());
									}
								}
								catch (LDAPException e)
								{
									// Retry
									if (W && nbTry==2) Log.w(TAG,"try="+nbTry+" "+e.getDiagnosticMessage(),e);
								}
								catch (Exception e)
								{
									if (E) e.printStackTrace();
									break;
								}
							}
						}
						@Override
						public String toString()
						{
							return name+"="+attr.getValue();
						}
					});
				}
			}
			@Override
			public String toString()
			{
				return "dnToName";
			}
		});
		_convs.put("dnToAddr",new Conv()
		{

			@Override
			public void putForType(Stack<PostJob> jobs,final VolatileData data, final Class<?> type, int mcc,final String name, final Attribute attr,final int pos)
			{
				if (DNTOADDR)
				{
					jobs.push(new PostJob()
					{
						@Override
						public void run(LDAPConnection conn)
						{
							try
							{
								if (conn.isConnected())
								{
									if (D) Log.d(TAG,"Post: getAddress "+attr.getValues()[pos]+" with "+conn.getConnectedAddress()+"...");
									SearchResultEntry entry=conn.getEntry(attr.getValues()[pos],"name","postalAddress","postalCode","street","l","c");
									if (D) Log.d(TAG,"Post: getAddress "+attr.getValues()[pos]+" with "+conn.getConnectedAddress()+" done.");
									String val;
									data.put(StructuredPostal.TYPE,StructuredPostal.TYPE_WORK);
									val=entry.getAttributeValue("postalAddress");
									if (val!=null)	data.put(StructuredPostal.FORMATTED_ADDRESS,val.replace('$', '\n'));
									val=entry.getAttributeValue("postalCode");
									if (val!=null)	data.put(StructuredPostal.POSTCODE,val);
									val=entry.getAttributeValue("street");
									if (val!=null)	data.put(StructuredPostal.STREET,val);
									val=entry.getAttributeValue("l");
									if (val!=null)	data.put(StructuredPostal.CITY,val);
									val=entry.getAttributeValue("c");
									if (val!=null)	data.put(StructuredPostal.COUNTRY,val);
								}
							}
							catch (LDAPException e)
							{
								// Ignore
								if (W) Log.w(TAG,"dnToAttr",e);
							}
						}
						@Override
						public String toString()
						{
							return name+"="+attr.getValue();
						}
					});
				}
			}
			@Override
			public String toString()
			{
				return "dnToAddr";
			}
		});
		_convs.put("labeledUri",new Conv()
		{

			@Override
			public void putForType(Stack<PostJob> jobs,VolatileData data,Class<?> type,int mcc,String name,Attribute attr,int pos)
			{
				final String val=attr.getValues()[pos];
				final int idx=val.lastIndexOf(' ');
				final String url=(idx!=-1) ? new String(val.substring(0, idx-1)) : val; // Dup string to economize buffer
				
				data.put(Website.URL, url);
				CharSequence typeUrl=val.subSequence(idx+1,val.length());
				if ("homepage".equals(typeUrl))
				{
					data.put(Website.TYPE, Website.TYPE_HOMEPAGE);
				}
				else if ("blog".equals(typeUrl))
				{
					data.put(Website.TYPE, Website.TYPE_BLOG);
				}
				else if ("profile".equals(typeUrl))
				{
					data.put(Website.TYPE, Website.TYPE_PROFILE);
				}
				else if ("home".equals(typeUrl))
				{
					data.put(Website.TYPE, Website.TYPE_HOME);
				}
				else if ("work".equals(typeUrl))
				{
					data.put(Website.TYPE, Website.TYPE_WORK);
				}
				else if ("ftp".equals(typeUrl))
				{
					data.put(Website.TYPE, Website.TYPE_FTP);
				}
				else if ("other".equals(typeUrl))
				{
					data.put(Website.TYPE, Website.TYPE_OTHER);
				}
				else
				{
					data.put(Website.TYPE, Website.TYPE_CUSTOM);
					data.put(Website.LABEL,typeUrl.toString());
				}
			}
			@Override
			public String toString()
			{
				return "labelUri";
			}
		}
		);
		_convs.put("phone",new Conv()
		{

			@Override
			public void putForType(Stack<PostJob> jobs, VolatileData data, Class<?> type, int mcc,String name, Attribute attr,int pos)
			{
				try
				{
					String num=attr.getValues()[pos];
					if (mcc==-1) // No normalize phone number
					{
						boolean other=false;
						for (int i=num.length()-1;i>=0;--i)
						{
							char c=num.charAt(i);
							if (Character.isDigit(c) && c!='0')
							{
								other=true;
								break;
							}
						}
						if (other)
							data.put(name,attr.getValue());
					}
					else
					{
						TOAPhoneNumberFormats format=TOAPhoneNumberFormats.getTOAContextFromMCC(mcc);
						if (format==null) format=TOAPhoneNumberFormats._phoneTOAContext;
						CharSequence toa=TOAPhoneNumberFormats.toTOAFormat(num,format);
						if (toa==null) return;
						final String parts=toa.toString();
						data.put(name, parts);
					}
				}
				catch (IllegalArgumentException e)
				{
					data.put(name, attr.getValue());
				}
			}
			@Override
			public String toString()
			{
				return "phone";
			}
			
		});
//		_convsPhonetics.put("sfinxbis", new Phonetic()
//		{
//			
//			@Override
//			public String phonetic(String data)
//			{
//				return new SwamiSfinxBis().codeSfinx(data);
//			}
//		});
	}
	private static volatile Mapping _defaultMapping;
	public static Mapping getDefaultMapping()
	{
		if (_defaultMapping==null)
		{
			try
			{
				_defaultMapping=parse(MAP_STANDARD);
			}
			catch (Exception e) // $codepro.audit.disable logExceptions
			{
				LogMarket.wtf(TAG, "Error in default mapping");
			}
		}
		return _defaultMapping;
	}
	public static Mapping parse(String filename) throws XmlPullParserException, IOException, ClassNotFoundException
	{
		Mapping mapping=new Mapping();
		XmlPullParserFactory xmlFactory = XmlPullParserFactory.newInstance();
		xmlFactory.setNamespaceAware(false);
        XmlPullParser xpp = xmlFactory.newPullParser();
        Reader reader=getMappingFile(filename);
        try
        {
	        if (reader==null) return null;
	        xpp.setInput(reader);		
			
			Clazz clazz=null;
			Clazz sup=null;
			for (int eventType = xpp.getEventType();eventType != XmlPullParser.END_DOCUMENT;eventType = xpp.next())
			{
				if (eventType == XmlPullParser.START_TAG)
				{
					if ("type".equals(xpp.getName()))
					{
						Map map;
						if (xpp.getAttributeValue(null,"type")==null)
							map=_emptyMap;
						else
						{
							map=new Map();
							if (!parseMap(xpp, map))
								LogMarket.wtf(TAG,"Invalide xml file");
						}
						mapping.types.put(xpp.getAttributeValue(null,"id").toLowerCase().intern(),map);
					}
					else if ("class".equals(xpp.getName()))
					{
						clazz=new Clazz();
						mapping.classes.put(xpp.getAttributeValue(null,"id").toLowerCase().intern(),clazz);
						sup=mapping.classes.get(xpp.getAttributeValue(null,"sup"));
					}
					else if ("may".equals(xpp.getName()) || "must".equals(xpp.getName()))
					{
						final String ref=xpp.getAttributeValue(null,"ref").toLowerCase().intern();
						assert(ref!=null);
						Map map;
						if (xpp.getAttributeValue(null, "col")!=null)
						{
							if (xpp.getAttributeValue(null,"type")==null)
								map=_emptyMap;
							else
							{
								map=new Map();
								if (!parseMap(xpp, map))
									LogMarket.wtf(TAG,"Invalide xml file");
							}
						}
						else
							map=mapping.types.get(ref);
						if (map!=null)
							clazz._may.put(ref, map);
						else
							if (W) Log.w(TAG,"Reference "+ref+" unknown");
						
					}
					else if ("mapping".equals(xpp.getName()))
					{
						String extend=xpp.getAttributeValue(null, "extend");
						if (extend!=null)
						{
							mapping=(Mapping)parse(extend).clone();
						}
						
						String val;
						val=xpp.getAttributeValue(null, "filter");
						if (val!=null) 
						{
							mapping.classFilter=val;
							if (mapping.classFilter.length()==0) mapping.classFilter=null;
						}
						val=xpp.getAttributeValue(null, "names");
						if (val!=null) mapping.namesAttr=val.split("\\s+");
						val=xpp.getAttributeValue(null, "phonetic");
						if (val!=null) mapping.phonetic=_convsPhonetics.get(val.toLowerCase());
						val=xpp.getAttributeValue(null, "phones");
						if (val!=null) mapping.phonesAttr=val.split("\\s+");
						val=xpp.getAttributeValue(null, "mails");
						if (val!=null) mapping.emailsAttr=val.split("\\s+");
						val=xpp.getAttributeValue(null, "attrs");
						if (val!=null) mapping.attrsList=val.split("\\s+");
						val=xpp.getAttributeValue(null, "photos");
						if (val!=null) mapping.photoAttrsList=val.split("\\s+");
						val=xpp.getAttributeValue(null,"timeout");
						if (val!=null) mapping.timeout=Integer.parseInt(val);
						val=xpp.getAttributeValue(null,"requesttimeout");
						if (val!=null) mapping.requesttimeout=Integer.parseInt(val);
						val=xpp.getAttributeValue(null,"connectiontimeout");
						if (val!=null) mapping.connectionTimeout=Integer.parseInt(val);
						val=xpp.getAttributeValue(null,"mcc");
						if (val!=null) mapping._mcc=Integer.parseInt(val);
						if (mapping.connectionTimeout==-1) mapping.connectionTimeout=mapping.timeout;
						val=xpp.getAttributeValue(null,"maxrecord");
						if (val!=null) mapping.maxrecord=Integer.parseInt(val);
						val=xpp.getAttributeValue(null,"sizelimit");
						if (val!=null) mapping.sizelimit=Integer.parseInt(val); // 10Ko
						val=xpp.getAttributeValue(null,"scope");
						if ("one".equals(val)) mapping.scope=SearchScope.ONE;
						else if ("base".equals(val)) mapping.scope=SearchScope.BASE;
						else if ("sub".equals(val)) mapping.scope=SearchScope.SUB;
					}
	
				}
				else if (eventType == XmlPullParser.END_TAG)
				{
					if ("class".equals(xpp.getName()))
					{
						if (sup!=null)
							clazz._may.putAll(sup._may); // Pack all hierarchie in one level
						clazz=sup=null;
					}
				}
			}
			return mapping;
        }
        finally
        {
        	try
        	{
        		reader.close();
        	}
        	catch (IOException e) // $codepro.audit.disable logExceptions
        	{
        		if (I) Log.i(TAG,"Exception when close");
        		// Ignore
        	}
        }
	}
	/**
	 * 
	 * @param xpp
	 * @param type
	 * @return true if ok.
	 * @throws ClassNotFoundException
	 */
	private static boolean parseMap(XmlPullParser xpp, Map type)
			throws ClassNotFoundException
	{
		String ty=xpp.getAttributeValue(null,"type");
		if (ty==null) return false;
		type._type=Class.forName(ty);
		type._col=xpp.getAttributeValue(null,"col").intern();
		type._mimeType=xpp.getAttributeValue(null,"mime").intern();
		type._extraCol=xpp.getAttributeValue(null,"extraCol");
		if (type._extraCol!=null) 
		{
			type._extraCol=type._extraCol.intern();
			String val=xpp.getAttributeValue(null,"extraValue");
			Class<?> extype=type._extraType=Class.forName(xpp.getAttributeValue(null,"extraType"));
			if (extype==Integer.class)
			{
				type._extraValue=Integer.parseInt(val);
			}
			else if (extype==Long.class)
			{
				type._extraValue=Long.parseLong(val);
			}
			else if (extype==Float.class)
			{
				type._extraValue=Float.parseFloat(val);
			}
			else if (extype==Double.class)
			{
				type._extraValue=Double.parseDouble(val);
			}
			else if (extype==String.class)
			{
				type._extraValue=val;
			}
			else if (extype!=null)
				LogMarket.wtf(TAG, "Unknown type");
			
		}
		final String conv=xpp.getAttributeValue(null,"conv");
		type._conv=(conv==null) ? _stdConv : _convs.get(conv);

		return true;
	}
	
	public VolatileContact convertLdapRecordToVolatileContact(Stack<PostJob> jobs,String accountName,int mcc,SearchResultEntry entry)
	{
		if (_mcc!=0) mcc=_mcc; // Priority for mapping
		boolean empty=true;
		VolatileContact contact=new VolatileContact();
		VolatileRawContact rawContact=contact.addNewRawContact();
		rawContact.setAttr(RawContacts.ACCOUNT_NAME, accountName);
		rawContact.setAttr(RawContacts.ACCOUNT_TYPE, LdapAuthenticationService.ACCOUNT_TYPE);
		rawContact.setAttr(VolatileRawContact.MUST_DELETED, 1);
		rawContact.setLookupKey(entry.getDN());
		rawContact.setAttr(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE_DISABLED);

		// Convert class names to Clazz array
		final String[] clazzName=entry.getObjectClassValues();
		
		HashMap<String,ArrayList<VolatileData> > datasByMime=rawContact._datas;
		for (Attribute attr:entry.getAttributes())
		{
			final String name=attr.getName().toLowerCase();
			// Search the mapping in all classes or in types
			Map mapping=null;
			if (clazzName!=null)
			{
				for (int i=clazzName.length-1;i>=0;--i)
				{
					final Clazz cl=classes.get(clazzName[i]);
					if (cl!=null)
					{
						mapping=cl._may.get(name);
						if (mapping!= null) 
							break;
					}
					else
					{
						classes.put(clazzName[i], new Clazz());
					}
				}
			}
			if (mapping==null)
				mapping=types.get(name);
			if (mapping==_emptyMap) 
			{
				continue;
			}
			if (mapping==null)
			{
				if (clazzName==null) continue;
				for (int i=clazzName.length-1;i>=0;--i)
				{
					classes.get(clazzName[i])._may.put(name,_emptyMap);
				}
				if (I) Log.i(TAG,"Ldap attribute "+name+" is unknown");
				continue; // Unknown mapping
			}
			
			// May have many data for the same mime type
			ArrayList<VolatileData> datas=datasByMime.get(mapping._mimeType);
			if (datas==null)
			{
				datas=new ArrayList<VolatileData>(3);
				datasByMime.put(mapping._mimeType,datas);
			}
			
			final int len=attr.getValues().length;
			for (int i=0;i<len;++i)
			{
				VolatileData data=null;
				for (VolatileData d:datas)
				{
					// Can I reuse the data record ?
					if (d.getAttr(mapping._col)==null)
					{
						data=d;
						break;
					}
				}
				if (data==null)	// No data to reused
				{
					data=new VolatileData();
					data.put(Data.MIMETYPE,mapping._mimeType);
					datas.add(data);
				}
				// If only structured name, the record is removed.
				if (!mapping._mimeType.equals(StructuredName.CONTENT_ITEM_TYPE) && 
					!mapping._mimeType.equals(Organization.CONTENT_ITEM_TYPE))
					empty=false;
				mapping._conv.putForType(jobs,data,mapping._type,mcc,mapping._col,attr,i);
				if (V) Log.v(TAG,"name="+name+" mime="+mapping._mimeType+" col="+mapping._col+" attr="+attr.getName()+" "+attr.getValues()[i]);
				if (mapping._extraCol!=null)
				{
					putExtra(data,mapping._extraType,mapping._extraCol,mapping._extraValue);
					//if (D) Log.d(TAG,"EXTRA "+mapping._extraType+" "+mapping._extraCol+" ="+mapping._extraValue);
				}
			}
		}

		// Add import button
		{
			VolatileData data=new VolatileData();
			data.put(Data.MIMETYPE, Import.CONTENT_ITEM_TYPE); 
			data.put(Import.SUMMARY_COLUMN, Application.context.getString(R.string.import_summary));
			data.put(Import.DETAIL_COLUMN,
				MessageFormat.format(
						Application.context.getString(R.string.import_detail),
						RawContacts.ACCOUNT_NAME));
			data.put(Import.LOOKUP_COLUMN,entry.getDN());
			rawContact.put(Import.CONTENT_ITEM_TYPE, data);
		}
		
		return (empty) ? null : contact;
	}
	
	private void putExtra(VolatileData data,Class<?> type,String name,Object value)
	{
		if (type==String.class)
			data.put(name,(String)value);
		else if (type==Boolean.class)
			data.put(name, (Boolean)value);
		else if (type==Byte.class)
			data.put(name, (Byte)value);
		else if (type==byte[].class)
			data.put(name, (byte[])value);
		else if (type==Short.class)
			data.put(name, (Short)value);
		else if (type==Integer.class)
			data.put(name, (Integer)value);
		else if (type==Long.class)
			data.put(name,(Long)value);
		else if (type==Float.class)
			data.put(name, (Float)value);
		else if (type==Double.class)
			data.put(name, (Double)value);
		else if (type==Bitmap.class)
		{
			Bitmap bitmap=Photos.extractFace((byte[])value);
			ByteArrayOutputStream os = new ByteArrayOutputStream(bitmap.getWidth()*bitmap.getHeight()*3);
			bitmap.compress(Bitmap.CompressFormat.PNG, 0, os);
			data.put(name,os.toByteArray());
		}
		else
			LogMarket.wtf(TAG, "Unknown class type "+type+" in ldap mapping file.");
	}
	public static Reader getMappingFile(String name)
	{
		Context context=Application.context;
		try
		{
			if (name!=null)
			{
				if ((MAP_NO_NORMALIZE.equals(name) || (MAP_STANDARD.equals(name))))
				{
					AssetManager assets=context.getAssets();
					try
					{
						assets=context.getPackageManager().getResourcesForApplication("fr.prados.contacts").getAssets();
						return new InputStreamReader(assets.open(name),Charset.forName("UTF-8"));
					}
					catch (NameNotFoundException e)
					{
						if (E) Log.e(TAG,"Build error",e);
					}					
				}
				// Downloaded mapping is in application directory
				else if (name.charAt(0)!='/')
					return new InputStreamReader(context.openFileInput(name),Charset.forName("UTF-8"));
				else
				{
					String state = Environment.getExternalStorageState();
		
					if (Environment.MEDIA_MOUNTED.equals(state) ||
							(Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))) 
					{
						return new InputStreamReader(new FileInputStream(name),Charset.forName("UTF-8"));
					} 
				}
			}
		}
		catch (FileNotFoundException e)
		{
			// Ignore
			if (D) Log.d(TAG,"file",e);
		}
		catch (IOException e)
		{
			// Ignore
			if (D) Log.d(TAG,"file",e);
		}
		try
		{
			return new InputStreamReader(context.getAssets().open(MAP_STANDARD),Charset.forName("UTF-8"));
		}
		catch (IOException e)
		{
			LogMarket.wtf(TAG, "Impossible to read standard.xml",e);
			return null;
		}
	}
}
