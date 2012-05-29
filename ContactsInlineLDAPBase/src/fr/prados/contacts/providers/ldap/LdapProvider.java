package fr.prados.contacts.providers.ldap;

import static fr.prados.contacts.Constants.D;
import static fr.prados.contacts.Constants.DNTOADDR;
import static fr.prados.contacts.Constants.DNTONAME;
import static fr.prados.contacts.Constants.E;
import static fr.prados.contacts.Constants.FAKE_ERROR;
import static fr.prados.contacts.Constants.FAKE_ERROR_PROBABILITY;
import static fr.prados.contacts.Constants.I;
import static fr.prados.contacts.Constants.PHOTO;
import static fr.prados.contacts.Constants.RECORD_LIMIT;
import static fr.prados.contacts.Constants.SYNC_SEARCH;
import static fr.prados.contacts.Constants.USE_PRE_CONNECTION;
import static fr.prados.contacts.Constants.V;
import static fr.prados.contacts.Constants.W;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javax.net.SocketFactory;

import org.xmlpull.v1.XmlPullParserException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;

import com.unboundid.ldap.sdk.AsyncRequestID;
import com.unboundid.ldap.sdk.AsyncSearchResultListener;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.DereferencePolicy;
import com.unboundid.ldap.sdk.ExtendedResult;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchResultReference;
import com.unboundid.ldap.sdk.extensions.StartTLSExtendedRequest;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;

import fr.prados.contacts.Application;
import fr.prados.contacts.ContactId;
import fr.prados.contacts.VolatileContact;
import fr.prados.contacts.providers.AbstractSimpleAuthenticator;
import fr.prados.contacts.providers.AuthQueryException;
import fr.prados.contacts.providers.Provider;
import fr.prados.contacts.providers.QueryError;
import fr.prados.contacts.providers.QueryException;
import fr.prados.contacts.providers.QueryWarning;
import fr.prados.contacts.providers.ResultsAndExceptions;
import fr.prados.contacts.providers.ldap.Mapping.PostJob;
import fr.prados.contacts.tools.LogMarket;
import fr.prados.contacts.tools.Photos;
import fr.prados.contacts.tools.TOAPhoneNumberFormats;
import fr.prados.provider.contacts.ldap.R;

/** 
 * @version 1.0
 * @since 1.0
 * @author Philippe PRADOS
 */
// Voir gestion en boucle d'erreur de LDAP
// A regarder pour l'implementation des services http://developer.android.com/reference/android/provider/ContactsContract.Contacts.html

public final class LdapProvider extends Provider 
{

	private static HashMap<CharSequence,ParamLdapDriver> _paramsPool=new HashMap<CharSequence,ParamLdapDriver>();
	
	private static class Filters
	{
		List<Filter> filtersNormal=new ArrayList<Filter>(3);
		List<Filter> filtersSingleEqual=new ArrayList<Filter>(3);
		
	}
	private static class ParamLdapDriver
	{
		private FutureTask<Object> 	_future;
		String 			_basedn;
		Mapping			_mapping;
		int				_mcc;

		public String toString()
		{
			return "basedn="+_basedn+" mcc="+_mcc+" mapping=...";
		}
		LDAPConnection getConnection() throws InterruptedException, ExecutionException,LDAPException,GeneralSecurityException
		{
				Object rc=_future.get();
				if (rc instanceof LDAPConnection)
				{
					LDAPConnection conn=(LDAPConnection)rc;
					if (!conn.isConnected())
					{
						if (I) Log.i(TAG,"Reconnect to LDAP server");
						conn.reconnect();
						if (I) Log.i(TAG,"Reconnected");
					}
					return conn;
				}
				if (rc instanceof LDAPException)
				{
					throw (LDAPException)rc;
				}
				if (rc instanceof GeneralSecurityException)
					throw (GeneralSecurityException)rc;
				throw new IllegalArgumentException((Throwable)rc);
		}
		Filters toFilter(String selection,String selectionArg) throws LDAPException
		{
			int idx=selectionArg.indexOf(',');
			if (idx!=-1)
				selectionArg=selectionArg.substring(0,idx-1);
			Filters filters=new Filters();
			final List<Filter> filtersNormal=filters.filtersNormal=new ArrayList<Filter>(3);
			final List<Filter> filtersSingleEqual=filters.filtersSingleEqual=new ArrayList<Filter>(3);
			if (selection.equals(QUERY_MODE_ALL)
				|| selection.equals(QUERY_MODE_ALL_WITH_ADDRESS)
				|| selection.equals(QUERY_MODE_ALL_WITH_PHONE)
				)
			{
				selection=analyseModeQuery(selectionArg);
			}

			if (QUERY_MODE_NAME.equals(selection))
			{
				final List<CharSequence>[] reqs=generateLdapNameRequest(_mapping, selectionArg);
				for (CharSequence seq:reqs[0])
					filtersNormal.add(Filter.create(seq.toString()));
				reqs[0].clear();
				
				if (reqs[1]!=null)
				{
					for (CharSequence seq:reqs[1])
						filtersSingleEqual.add(Filter.create(seq.toString()));
					reqs[1].clear();
				}
				return filters;
			}
			if (QUERY_MODE_TEL.equals(selection))
			{
				final List<CharSequence>[] reqs=generateLdapPhoneRequest(_mapping.phonesAttr, selectionArg);
				for (CharSequence seq:reqs[0])
					filtersNormal.add(Filter.create(seq.toString()));
				reqs[0].clear();
				if (reqs[1]!=null)
				{
					for (CharSequence seq:reqs[1])
						filtersSingleEqual.add(Filter.create(seq.toString()));
					reqs[1].clear();
				}
				return filters;
			}
			if (QUERY_MODE_MAILTO.equals(selection))
			{
				final List<CharSequence>[] reqs=generateLdapMailRequest(_mapping.emailsAttr,selectionArg);
				for (CharSequence seq:reqs[0])
					filtersNormal.add(Filter.create(seq.toString()));
				reqs[0].clear();
				if (reqs[1]!=null)
				{
					for (CharSequence seq:reqs[1])
						filtersSingleEqual.add(Filter.create(seq.toString()));
					reqs[1].clear();
				}
				return filters;
			}
			LogMarket.wtf(TAG, "Invalide filter "+selection+" "+selectionArg);
			return null;
		}
		private static String analyseModeQuery(String selectionArg)
		{
			if (selectionArg.indexOf('@')!=-1)
				return QUERY_MODE_MAILTO;
			int number=0;
			for (int i=0;i<selectionArg.length();++i)
			{
				final char c=selectionArg.charAt(i);
				if ((c>='0') && (c<='9'))
					++number;
			}
			if ((number!=0) && (selectionArg.length()*100/number)>55)
				return QUERY_MODE_TEL;
			return QUERY_MODE_NAME;
		}
//		private static String mergeArgs(String req,String...selectionArgs)
//		{
//			StringBuilder builder=new StringBuilder();
//			int start=0;
//			int cur;
//			int idx=0;
//			while ((cur=req.indexOf('?',start))!=-1)
//			{
//				builder.append(req.substring(start,cur))
//					.append(escapeLDAPSearchFilter(selectionArgs[idx++]));
//				start=cur+1;
//			}
//			builder.append(req.substring(start));
//			if (idx!=selectionArgs.length)
//				throw new IllegalArgumentException();
//			builder.trimToSize();
//			return builder.toString();
//		}

		/**
		 * @param attr
		 * @param words
		 * @return first:multi equal sequence, 
		 * 	next: single equal sequence with different patterns
		 */
		private List<CharSequence>[] generateLdapNameRequest(Mapping mapping,String words)
		{
			String[] attr=mapping.namesAttr;
			@SuppressWarnings("unchecked")
			List<CharSequence>[] rc=new List[2];
			List<CharSequence> strategy=new ArrayList<CharSequence>(3);
			String[] slip=words.split(" ");
			if (mapping.phonetic!=null)
			{
				for (int i=slip.length-1;i>=0;--i)
				{
					slip[i]=mapping.phonetic.phonetic(slip[i]);
				}
			}
			final CharSequence[] escapeSlip=new CharSequence[slip.length];
			for (int i=slip.length-1;i>=0;--i)
			{
				escapeSlip[i]=escapeLDAPSearchFilter(slip[i]);
			}
			slip=null;
			
			StringBuilder builder;
			
			// 1. Use one attribute at a time with & model
			for (String att:attr)
			{
				builder=new StringBuilder();
				if ((escapeSlip.length>1) || (_mapping.classFilter!=null)) 
					builder.append("(&");
				if (_mapping.classFilter!=null)
					builder.append(_mapping.classFilter);
				for (CharSequence w:escapeSlip)
				{
					builder.append('(')
						.append(att)
						.append("=*")
						.append(w)
						.append("*)");
				}
				if ((escapeSlip.length>1) || (_mapping.classFilter!=null)) 
					builder.append(')');
				strategy.add(builder);
				if (D) Log.d(TAG,builder.toString());
			}
			rc[0]=strategy;
			if (D) Log.d(TAG,"---");
			
			// 2. Use one equal sequence for specific LDAP server with error 'single equality filter only'
			// Rotate the fields to check different positions
			strategy=new ArrayList<CharSequence>(3);
			for (String att:attr)
			{
				for (int i=0;i<escapeSlip.length;++i)
				{
					builder=new StringBuilder();
//					if (_mapping.classFilter!=null)
//						builder.append("(&").append(_mapping.classFilter);
					builder.append('(')
						.append(att)
						.append("=*");
					for (int j=i;j<escapeSlip.length+i;++j)
					{
						final CharSequence seq=escapeSlip[j % escapeSlip.length];
						builder.append(seq).append('*');
					}
					builder.append(')');
//					if (_mapping.classFilter!=null)
//						builder.append(")");
					builder.trimToSize();
					strategy.add(builder);
					if (D) Log.d(TAG,builder.toString());
				}
			}
			rc[1]=strategy;
			return rc;
		}
		private List<CharSequence>[] generateLdapPhoneRequest(String[] attrs,CharSequence phonenumber)
		{
			@SuppressWarnings("unchecked")
			List<CharSequence>[] rc=new List[2];
			// 1. Use 
			ArrayList<CharSequence> strategy=new ArrayList<CharSequence>();
			for (String attr:attrs)
			{
				final CharSequence subNumber=TOAPhoneNumberFormats.extractSubscriberNumber(phonenumber);
				final StringBuilder req=new StringBuilder();
				if (_mapping.classFilter!=null)
					req.append("(&")
					.append(_mapping.classFilter);
				req.append('(')
					.append(attr).append("=*")
					.append(escapeLDAPSearchFilter(subNumber))
					.append("*)");
				if (_mapping.classFilter!=null)
					req.append(')');
					// Search only local number
				strategy.add(req);
			}
			rc[0]=strategy;
			return rc;
		}

		private List<CharSequence>[] generateLdapMailRequest(String[] attrs,CharSequence mail)
		{
			@SuppressWarnings("unchecked")
			List<CharSequence>[] rc=new List[2];
			ArrayList<CharSequence> strategy=new ArrayList<CharSequence>();
			for (String attr:attrs)
			{
				final StringBuilder req=new StringBuilder();
				if (_mapping.classFilter!=null)
					req.append("(&")
					.append(_mapping.classFilter);
				req.append('(')
					.append(attr).append('=')
					.append(escapeLDAPSearchFilter(mail))
					.append(')');
				if (_mapping.classFilter!=null)
					req.append(')');
				strategy.add(req);
			}
			rc[0]=strategy;
			return rc;
		}
		
		private static final CharSequence escapeLDAPSearchFilter(final CharSequence filter)
		{
			final StringBuilder sb = new StringBuilder();
			for (int i = 0; i < filter.length(); i++)
			{
				final char curChar = filter.charAt(i);
				switch (curChar)
				{
					case '\\':
						sb.append("\\5c");
						break;
					case '*':
						sb.append("\\2a");
						break;
					case '(':
						sb.append("\\28");
						break;
					case ')':
						sb.append("\\29");
						break;
					case '\u0000':
						sb.append("\\00");
						break;
					default:
						sb.append(curChar);
				}
			}
			return sb;
		}
	}

	@Override
	public void onStart()
	{
		super.onStart();
		if (V)
		{
			System.setProperty("com.unboundid.ldap.sdk.debug.enabled", "true");
		}
		if (!USE_PRE_CONNECTION) return;
		for (final Account account:
			AccountManager.get(Application.context)
				.getAccountsByType(LdapAuthenticationService.ACCOUNT_TYPE))
		{
			Application._executor.submit(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						final ParamLdapDriver provider=getParams(account.name,true);
						provider.getConnection(); // Initialize connection
					}
					catch (Exception e)
					{
						// Ignore
						if (W) Log.w(TAG,"onStart",e);
					}
				}
			});
		}
	}

	final List<AsyncRequestID> _asyncjobs=Collections.synchronizedList(new ArrayList<AsyncRequestID>());

	@Override
	public void signalCanceled(final String accountName)
	{
		if (D) Log.d(TAG,"LDAP Canceled");
		super.signalCanceled(accountName);
		new Thread()
		{
			public void run() 
			{
				synchronized (_asyncjobs)
				{
					for (AsyncRequestID id:_asyncjobs)
					{
						try
						{
							getParams(accountName,false).getConnection().abandon(id);
						}
						catch (Exception e)
						{
							// Ignore
							if (I) Log.i(TAG,"cancel ldap",e);
						}
						_asyncjobs.notify();
					}
				}
			}
		}.start();
	}
	public static abstract class AsyncSearch implements AsyncSearchResultListener
	{
		private static final long	serialVersionUID	= 1L;
		protected volatile AsyncRequestID _id; 
	};
	@Override
	public ResultsAndExceptions queryContact(final String accountName,final String selection,final String selectionArg)
	{
		final Context context=Application.context;
		checkPermission();
		final ResultsAndExceptions result=new ResultsAndExceptions();

		if (FAKE_ERROR!=null && (FAKE_ERROR.nextInt(FAKE_ERROR_PROBABILITY)==0))
		{
			result.exceptions.add(new QueryError(null, "Fake error"));
			return result;
		}
		// ---
		
		final HashSet<String> ldapRef=new HashSet<String>();
		resetCanceled();
		ParamLdapDriver par=null;
		try
		{
			final ParamLdapDriver params=par=getParams(accountName,true);
			result.contacts=new ArrayList<VolatileContact>(3);
			if (params==null)
				return result; // Invalide authentification
			
			// Try with differents forms
			long start=0;

			Filters filters=params.toFilter(selection,selectionArg);
			if (filters==null)
			{
				LogMarket.wtf(TAG,"Invalide selection "+selection);
				throw new IllegalArgumentException("Invalide selection "+selection);
			}
			List<Filter> currentStrategy=filters.filtersNormal;
			final Stack<PostJob> jobs=new Stack<PostJob>();
			final LDAPConnection conn=params.getConnection();
			for (Filter filter:currentStrategy)
			{
				if (start==0)
					start=System.currentTimeMillis();
				if (conn.isConnected())
				{
					final AsyncSearch as=new AsyncSearch()
					{
						private static final long	serialVersionUID	= 1L;

						boolean mCanceded;
						private void notifyFinish()
						{
							synchronized (_asyncjobs)
							{
								_asyncjobs.remove(_id);
								_asyncjobs.notify();
							}
						}

						private void cancelJob() throws CancellationException
						{
							mCanceded=true;
							try
							{
								if (_id!=null)
									conn.abandon(_id);
							}
							catch (LDAPException e)
							{
								if (E) Log.d(TAG,"Error when cancel job",e);
							}							
							notifyFinish();
							throw new CancellationException();
						}
						@Override
						public void searchReferenceReturned(SearchResultReference searchReference)
						{
						}
						
						@Override
						public void searchEntryReturned(SearchResultEntry e)
						{
							if (mCanceded) return;
							if (V) Log.v(TAG,accountName+"->"+e.getDN());
							if (RECORD_LIMIT && result.contacts.size()>=params._mapping.maxrecord)
							{
								result.exceptions.add(new QueryWarning(accountName,context.getString(R.string.err_maxitem_exceeded)));
								cancelJob();
							}
							if (isCanceled())
							{
								cancelJob();
							}
							if (ldapRef.contains(e.getDN())) // Remove double if multiple request
								return;
							VolatileContact contact=
								params._mapping.convertLdapRecordToVolatileContact(jobs,accountName,params._mcc,e);
							if (contact==null)
								return;
							if ((QUERY_MODE_ALL_WITH_ADDRESS == selection)
							   && !contact.hasAddress())
							{
								return;
							}
							if ((QUERY_MODE_ALL_WITH_PHONE == selection)
									   && !contact.hasPhoneNumber())
							{
								return;
							}
							ldapRef.add(e.getDN());
							result.contacts.add(contact);
						}
						
						@Override
						public void searchResultReceived(AsyncRequestID requestID, SearchResult searchResult)
						{
							notifyFinish();
						}
					};
					SearchRequest se=new SearchRequest(as, null, params._basedn,params._mapping.scope,DereferencePolicy.ALWAYS,params._mapping.sizelimit,params._mapping.requesttimeout,
							false,filter,params._mapping.attrsList);
					if (SYNC_SEARCH)
					{
						try
						{
							conn.search(se);
						} 
						catch (LDAPException e)
						{
							// PB : if exception, return nothing !
							if (e!=null)
							{
								if (e.getResultCode().intValue()==ResultCode.SIZE_LIMIT_EXCEEDED_INT_VALUE)
									result.exceptions.add(new QueryWarning(accountName,context.getString(R.string.err_maxitem_exceeded)));
								else
									result.exceptions.add(new QueryError(accountName,context.getString(R.string.err_connection_exception)));
							}
						}
						catch (CancellationException e) // $codepro.audit.disable emptyCatchClause, logExceptions
						{
							// Ignore
						}
					}
					else
					{
						final AsyncRequestID requestID=conn.asyncSearch(se);
						as._id=requestID;
						if (D) Log.d(TAG,as._id+" job "+se);
						_asyncjobs.add(requestID);
					}
				}
			}
			filters.filtersNormal.clear(); // Help GC
			filters.filtersSingleEqual.clear(); // Help GC
			
			// Wait all jobs
			if (!SYNC_SEARCH)
			{
				synchronized (_asyncjobs)
				{
					while (_asyncjobs.size()!=0)
					{
						_asyncjobs.wait(500);
						if (params.getConnection().getActiveOperationCount()==0)
							break;
					}
				}
				_asyncjobs.clear();
				if (DNTONAME | DNTOADDR)
				{
					signalCanceled(accountName);
				}
					
			}
			else
				signalCanceled(accountName);
			if (conn.isConnected())
			{
				synchronized (_asyncjobs)
				{
					for (PostJob job:jobs)
					{
						try
						{
							job.run(conn);
						}
						catch (Exception e)
						{
							// Ignore
							if (W) Log.w(TAG,"queryContact",e);
						}
					}
					jobs.clear(); // Help GC
				}
			}
		}
		catch (IOException e)
		{
			if (W) Log.w(TAG,e);
			if( e.getMessage().contains("Unable to establish"))
			{
				result.exceptions.add(new QueryError(accountName,context.getString(R.string.err_connection_exception,e)));
			}
			else
				result.exceptions.add(new QueryError(accountName,context.getString(R.string.err_io_exception,e)));
			removeParams(accountName);
		}
		catch (LDAPException e)
		{ 
			if (W) Log.w(TAG,"Exception :"+e);
			QueryException ee=convLDAPException(e,accountName);
			if (ee!=null)
			{
				if ((ee instanceof AuthQueryException) && (par!=null))
				{
					_paramsPool.remove(par);
				}
				result.exceptions.add(ee);
			}
			removeParams(accountName);
		}
		catch (InterruptedException e)
		{
			LogMarket.wtf(TAG,e);
			result.exceptions.add(new QueryError(accountName,
					Application.context.getString(R.string.ldap_err_unknown_exception,e)));
			removeParams(accountName);
		}
		catch (ExecutionException e)
		{
			LogMarket.wtf(TAG,e);
			
			result.exceptions.add(new QueryError(accountName,
					Application.context.getString(R.string.ldap_err_unknown_exception,e)));
			removeParams(accountName);
		}
		catch (GeneralSecurityException e)
		{
			LogMarket.wtf(TAG,e);
			result.exceptions.add(new QueryError(accountName,
					Application.context.getString(R.string.err_authent,e)));
			removeParams(accountName);
		}
//		catch (RuntimeException e)
//		{
//			LogMarket.wtf(TAG, e);
//			throw e;
//		}
		
		return result;
	}

	@Override
	public VolatileContact getVolatileContact(ContactId id) //throws QueryException
	{
		try
		{
			return getContact(id.accountName.toString(),id.lookupKey.toString());
		}
		catch (QueryException e)
		{
			if (W) Log.w(TAG,"getVolatileContact",e);
			return null;
		}
	}
	
	public VolatileContact getContact(String accountName,String lookup) throws QueryException
	{
		final Context context=Application.context;
		ParamLdapDriver params=null;
		Stack<PostJob> jobs=new Stack<PostJob>();
		try
		{
			params=getParams(accountName,true);
			final String[] fullAttr=new String[params._mapping.attrsList.length+params._mapping.photoAttrsList.length];
			System.arraycopy(params._mapping.attrsList, 0, fullAttr, 0, params._mapping.attrsList.length);
			System.arraycopy(params._mapping.photoAttrsList, 0, fullAttr, params._mapping.attrsList.length, params._mapping.photoAttrsList.length);
			SearchResultEntry result=params.getConnection().getEntry(lookup,fullAttr);
			VolatileContact contact=params._mapping.convertLdapRecordToVolatileContact(jobs,accountName,params._mcc,result);

			LDAPConnection conn=params.getConnection();
			if (conn.isConnected())
			{
				for (PostJob job:jobs)
				{
					try
					{
						job.run(params.getConnection());
					}
					catch (Exception e)
					{
						// Ignore
						if (W) Log.w(TAG,"getRawContact",e);
					}
				}
				jobs.clear();
			}			
			return contact;
		}
		catch (LDAPException e)
		{
			switch (e.getResultCode().intValue())
			{
				case ResultCode.AUTH_METHOD_NOT_SUPPORTED_INT_VALUE:
				case ResultCode.AUTH_UNKNOWN_INT_VALUE:
				case ResultCode.AUTHORIZATION_DENIED_INT_VALUE:
					 throw new AuthQueryException(accountName,context.getString(R.string.err_server_down),e);
				default:
					throw new QueryError(accountName,context.getString(R.string.err_server_down),e);
			}
			
		}
		catch (IOException e)
		{
			throw new QueryError(accountName,e);
		}
		catch (InterruptedException e)
		{
			throw new QueryError(accountName,e);
		}
		catch (ExecutionException e)
		{
			throw new QueryError(accountName,e);
		}
		catch (GeneralSecurityException e)
		{
			throw new QueryError(accountName,e);
		}
	}

	// ------------------------------------------
	private void removeParams(CharSequence accountName)
	{
		_paramsPool.remove(accountName);
	}
	private ParamLdapDriver getParams(final CharSequence accountName,boolean notification)
    throws LDAPException,GeneralSecurityException,IOException
    {
		try
		{
			ParamLdapDriver lparams=_paramsPool.get(accountName);
			if (lparams!=null)
			{
				return lparams;
			}
			lparams=new ParamLdapDriver();
			final ParamLdapDriver params=lparams;
			  
			final AccountManager accountManager = AccountManager.get(Application.context);
			for (final Account account:accountManager.getAccountsByType(LdapAuthenticationService.ACCOUNT_TYPE))
			{
				if (accountName.equals(account.name))
				{
	        		try
					{
						AccountManagerFuture<Bundle> futur = accountManager.getAuthToken(account, AbstractSimpleAuthenticator.AUTHTOKEN_TYPE, notification,null,null);

						final String mappingfile=accountManager.getUserData(account,LdapAuthenticationService.KEY_MAPPING);
						params._mapping=Mapping.parse(mappingfile);
						final String crypt=accountManager.getUserData(account,LdapAuthenticationService.KEY_CRYPT);
		            	final String host=accountManager.getUserData(account,LdapAuthenticationService.KEY_HOST);
		            	final int port=Integer.parseInt(accountManager.getUserData(account,LdapAuthenticationService.KEY_PORT));
		        		final String basedn=accountManager.getUserData(account,LdapAuthenticationService.KEY_BASEDN);
		            	final String username=accountManager.getUserData(account,LdapAuthenticationService.KEY_USERNAME);
		        		int mcc=Integer.parseInt(accountManager.getUserData(account,LdapAuthenticationService.KEY_MCC));
		        		if (mcc==0)
		        			mcc=TOAPhoneNumberFormats.getServerMCC(host);
		        		Bundle result=futur.getResult();
		        		final String password=result.getString(AccountManager.KEY_AUTHTOKEN); // Generate exception if error
		        		if (password==null)
		        			throw new AuthenticatorException();
		        		params._basedn=basedn;
		        		params._mcc=mcc;
		        		params._future=new FutureTask<Object>(new Callable<Object>() 
		        			{
		        				public Object call() 
		        				{
		    		        		try
		    		        		{
		    		        			if (D) Log.d(TAG,"getConnection "+host+"...");
		    							final LDAPConnection conn=getConnection(crypt,host, port, username, password,
		    								params._mapping.connectionTimeout,params._mapping.sizelimit);
		    		        			if (D) Log.d(TAG,"getConnection "+host+" done.");
		    		        			if (E && !conn.isConnected())
		    		        				Log.e(TAG,"Get connection no connected !");
		    							return conn;
		    		        		}
		    		        		catch (LDAPException e)
		    		        		{
		    		        			switch (e.getResultCode().intValue())
		    		        			{
		    		        				case ResultCode.AUTH_METHOD_NOT_SUPPORTED_INT_VALUE:
		    		        				case ResultCode.AUTH_UNKNOWN_INT_VALUE:
		    		        				case ResultCode.AUTHORIZATION_DENIED_INT_VALUE:
		    		        				case ResultCode.INVALID_CREDENTIALS_INT_VALUE:
		    		        					AbstractSimpleAuthenticator.invalideCredential(account,password);
		    		        					break;
		    		        				default:
		    		        					/* Nothing*/
		    		        					break;
		    		        			}
		    		        			return e.fillInStackTrace();
		    		        		}
									catch (GeneralSecurityException e)
									{
										_paramsPool.remove(accountName);
										AbstractSimpleAuthenticator.invalideCredential(account, password);
										if (E) Log.e(TAG,"Error when get connection ("+e.getMessage()+")");
										return e;
									}
		        				}
		        			});
		        		Application._executor.execute(params._future);
		        		_paramsPool.put(accountName, params);
		        		return params;
	        		}
					catch (XmlPullParserException e)
					{
		    			if (E) Log.e(TAG,"Error when get params",e);
						throw new LDAPException(ResultCode.PARAM_ERROR,e);
					}
					catch (ClassNotFoundException e)
					{
						if (D) Log.d(TAG,"Error when get params",e);
						throw new LDAPException(ResultCode.PARAM_ERROR,e);
					}
					catch (AuthenticatorException e)
					{
						throw new GeneralSecurityException(e);
					}
				}
			}
			return null;
		  }
		catch (OperationCanceledException e)
		{
			if (D) Log.d(TAG,"Error when get params",e);
			return null;
		}
	}
	@Override
	public byte[] getAccountPhoto(ContactId id)
	{
		ParamLdapDriver params;
		try
		{
			if (PHOTO)
			{
				params = getParams(id.accountName,false);
				if (params==null)
					return null; // Invalide authentification
				final LDAPConnection connection=params.getConnection();
				SearchResultEntry entry=connection.getEntry(id.lookupKey.toString(),params._mapping.photoAttrsList);
				for (Attribute attr:entry.getAttributes())
				{
					// extract bitmap
					Bitmap resizedBitmap = Photos.extractFace(attr.getValueByteArray());
					
					// Convert to byte array
					ByteArrayOutputStream os = 
						new ByteArrayOutputStream(resizedBitmap.getWidth()*resizedBitmap.getHeight()*3);
					resizedBitmap.compress(Bitmap.CompressFormat.PNG, 0, os);
					resizedBitmap.recycle(); // Help GC
					byte[] jpeg=os.toByteArray();
					os.close();
					os=null;
					return jpeg;
				}
			}
			return null;
		}
		catch (OutOfMemoryError e)
		{
			if (W) Log.w(TAG,e);
			return null;
		}
		catch (Exception e)
		{
			// Ignore
			if (W) Log.w(TAG,e);
			return null;
		}
	}
//----------------------------
	/*package*/ static LDAPConnection getConnection(String crypt,String host,int port,String username,String password,int connectionTimeout,int maxMessageSize) 
	throws LDAPException, GeneralSecurityException
	{
		boolean useSSL=false;
		boolean useStartTLS=false;
		if ("ssl".equals(crypt))
		{
			useSSL=true;
		}
		else if ("tls".equals(crypt))
		{
			useSSL=useStartTLS=true;
		}
		SocketFactory socketFactory = null;
		if (useSSL)
		{
			final SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
			socketFactory = sslUtil.createSSLSocketFactory();
		}

		final LDAPConnectionOptions options = new LDAPConnectionOptions();
		options.setAutoReconnect(true);
		options.setConnectTimeoutMillis(connectionTimeout*1000);
		options.setFollowReferrals(false);
		options.setMaxMessageSize(maxMessageSize);
		

		final LDAPConnection conn = new LDAPConnection(socketFactory, options,host, port);

		if (useStartTLS)
		{
			final SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
			try
			{
				if (conn.isConnected())
				{
					final ExtendedResult r = conn.processExtendedOperation(new StartTLSExtendedRequest(sslUtil.createSSLContext()));
					if (r.getResultCode() != ResultCode.SUCCESS)
					{
						throw new LDAPException(r);
					}
				}
			}
			catch (LDAPException le)
			{
				conn.close();
				throw (LDAPException)le.fillInStackTrace();
			}
		}

		if ((username != null) && (password != null))
		{
			try
			{
				conn.bind(username, password);
			}
			catch (LDAPException le)
			{
				conn.close();
				throw new GeneralSecurityException("auth error",le);
			}
		}
		return conn;
	}

	public static QueryException convLDAPException(LDAPException e,String accountName)
	{
		if (e==null)
			return new QueryError(accountName,Application.context.getString(R.string.err_unknown,e));
		if (I) Log.i(TAG,accountName+" "+e.getMessage());
		switch (e.getResultCode().intValue())
		{
			case ResultCode.SIZE_LIMIT_EXCEEDED_INT_VALUE:
				return new QueryWarning(accountName,Application.context.getString(R.string.err_maxitem_exceeded,e));
			case ResultCode.TIME_LIMIT_EXCEEDED_INT_VALUE:
				return new QueryWarning(accountName,Application.context.getString(R.string.err_time_limit_exceeded,e));
			case ResultCode.CONNECT_ERROR_INT_VALUE:
			case ResultCode.SERVER_DOWN_INT_VALUE:
				return new QueryError(accountName,Application.context.getString(R.string.err_server_down,e));
			case ResultCode.NO_SUCH_OBJECT_INT_VALUE:
				return null;
			case ResultCode.ADMIN_LIMIT_EXCEEDED_INT_VALUE:
				return new QueryWarning(accountName,Application.context.getString(R.string.err_admin_limit_exceeded,e));
			case ResultCode.INSUFFICIENT_ACCESS_RIGHTS_INT_VALUE:
				return new AuthQueryException(accountName,Application.context.getString(R.string.err_access_right,e));
			case ResultCode.INVALID_CREDENTIALS_INT_VALUE:
			case ResultCode.AUTHORIZATION_DENIED_INT_VALUE:
				return new AuthQueryException(accountName,Application.context.getString(R.string.err_authent,e));
			default:
				LogMarket.wtf(TAG, e);
				return new QueryError(accountName,e);
		}
	}

}
