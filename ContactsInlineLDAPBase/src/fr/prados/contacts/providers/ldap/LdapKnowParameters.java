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

import static fr.prados.contacts.Constants.EMULATOR;
import static fr.prados.contacts.Constants.D;
import static fr.prados.contacts.Constants.W;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import fr.prados.contacts.Application;
import fr.prados.contacts.tools.LogMarket;
public class LdapKnowParameters implements Parcelable
{
	private static final String TAG="LDAP";

	private static final String GOOGLEAPPS=EMULATOR ? "10.0.2.2:8888" : "contactsinline.appspot.com";
	//private static final String GOOGLEAPPS="contacts.prados.fr";
	public static final String USER_TAG="<username>";
	
	public CharSequence _host;
    public CharSequence _crypt;
	public CharSequence _usernamePattern;
	public CharSequence _basedn;
	public CharSequence _mappingname;

	public String toString()
	{
		return "host="+_host+", crypt="+_crypt+", usernamepattern="+_usernamePattern+", basedn="+_basedn+", mappingname="+_mappingname;
	}
	// -- Parcel managment
	@Override
	public void writeToParcel(Parcel dest, int flags)
	{
		dest.writeString(_host.toString());
		dest.writeString(_crypt.toString());
		dest.writeString(_usernamePattern.toString());
		dest.writeString(_basedn!=null?_basedn.toString():null);
		dest.writeString(_mappingname!=null?_mappingname.toString():null);
	}
	
	@Override
	public int describeContents()
	{
		return 0;
	}

	public static final Parcelable.Creator<LdapKnowParameters> CREATOR = new Parcelable.Creator<LdapKnowParameters>() 
	{
		@Override
        public LdapKnowParameters createFromParcel(Parcel in) 
        {
			LdapKnowParameters know=new LdapKnowParameters();
			know._host=in.readString();
			know._crypt=in.readString();
			know._usernamePattern=in.readString();
			know._basedn=in.readString();
			know._mappingname=in.readString();
			return know;
        }

		@Override
        public LdapKnowParameters[] newArray(int size) 
        {
            return new LdapKnowParameters[size];
        }
    };

    // -----------------
    static private DocumentBuilder domBuilder;
    static
    {
    	try
		{
			domBuilder=DocumentBuilderFactory.newInstance().newDocumentBuilder();
		}
		catch (ParserConfigurationException e)
		{
			LogMarket.wtf(TAG,"Init dom parser",e);
			throw new ExceptionInInitializerError();
		}
    }
	public static LdapKnowParameters getParameters(String host) throws IOException
	{
		if (D) Log.d(TAG,"get parameters...");
		host=host.toLowerCase();
		try
		{
	
			URL url=new URL("http://"+GOOGLEAPPS+"/ldap/"+URLEncoder.encode(host,"UTF-8"));
			HttpURLConnection con=(HttpURLConnection)url.openConnection();
			if (con.getResponseCode()==200)
			{
				Reader reader=new InputStreamReader(con.getInputStream(),"UTF-8");
			    return parseKnowParameters(host, reader);
			}
		}
		catch (Exception e)
		{
			// Ignore
			if (W) Log.w(TAG,e);
		}
		return null;
	}
	public static LdapKnowParameters parseKnowParameters(String host, Reader reader)
			throws SAXException, IOException, FileNotFoundException
	{
		LdapKnowParameters rc=new LdapKnowParameters();
		Document doc = domBuilder.parse(new InputSource(reader));
		NodeList nodes = doc.getElementsByTagName("params");
		Element params=(Element)nodes.item(0);
		rc._host=host;
		rc._crypt=params.getAttribute("crypt");
		rc._usernamePattern=params.getAttribute("username");
		if (rc._usernamePattern!=null && rc._usernamePattern.length()==0) rc._usernamePattern=null;
		rc._basedn=params.getAttribute("basedn");
		rc._mappingname=params.getAttribute("mappingname");
		NodeList listes=params.getElementsByTagName("mapping");
		if (listes.getLength()>0)
		{
		    Element map=(Element)listes.item(0);

		    String xml=XMLUtils.elementToString(map);

		    //create string from xml tree
		    rc._mappingname=host+".xml";
		    Writer o=new OutputStreamWriter(Application.context.openFileOutput(rc._mappingname.toString(), Context.MODE_PRIVATE),
		    			Charset.forName("UTF-8"));
		    o.write(xml);
		    o.close();
		}
		return rc;
	}


	public static void postParameters(LdapKnowParameters params)
	{
		if (D) Log.d(TAG,"post parameters...");
		try
		{
			URL url=new URL("http://"+GOOGLEAPPS+"/ldap/"+URLEncoder.encode(params._host.toString(),"UTF-8"));

            Document doc = domBuilder.newDocument();
            Element root = doc.createElement("params");
            doc.appendChild(root);
            if (params._crypt!=null) root.setAttribute("crypt", params._crypt.toString());
            if (params._usernamePattern!=null) root.setAttribute("username", params._usernamePattern.toString());
            if (params._basedn!=null) root.setAttribute("basedn", params._basedn.toString());

            boolean localMapping=(params._mappingname.charAt(0)=='/');
            if (!localMapping)
            {
            	root.setAttribute("mappingname", params._mappingname.toString());
            }
            String xmlString = XMLUtils.elementToString(root);
            if (localMapping)
            {
            	StringBuilder buf=new StringBuilder();
				int idx=xmlString.indexOf("/>");
				buf.append(xmlString.substring(0,idx)).append(">\n");
            	BufferedReader r=new BufferedReader(Mapping.getMappingFile(params._mappingname.toString()));
            	for (;;)
            	{
            		String line=r.readLine();
            		if (line==null) break;
            		if (!line.startsWith("<?xml"))
            			buf.append(line).append('\n');
            	}
            	r.close();
				buf.append("\n</params>");
				xmlString=buf.toString();
            }
            // Send data
			HttpURLConnection con=(HttpURLConnection) url.openConnection();
		    con.setDoOutput(true);
		    OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
		    wr.write(URLEncoder.encode("xml", "UTF-8") + "=" + URLEncoder.encode(xmlString, "UTF-8"));
		    wr.flush();
		    int rc=con.getResponseCode();
		    if (D) Log.d(TAG,"rc="+rc);
		}
		catch (Exception e)
		{
			// Ignore
			if (W) Log.w(TAG,"post",e);
		}

	}
}
