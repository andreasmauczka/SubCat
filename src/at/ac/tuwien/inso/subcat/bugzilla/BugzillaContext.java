/* Context.java
 *
 * Copyright (C) 2014 Florian Brosch
 *
 * Based on work from Andreas Mauczka
 *
 * This program is developed as part of the research project
 * "Lexical Repository Analyis" which is part of the PhD thesis
 * "Design and evaluation for identification, mapping and profiling
 * of medium sized software chunks" by Andreas Mauczka at
 * INSO - University of Technology Vienna. For questions in regard
 * to the research project contact andreas.mauczka(at)inso.tuwien.ac.at
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2.0
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 * Author:
 *       Florian Brosch <flo.brosch@gmail.com>
 */

package at.ac.tuwien.inso.subcat.bugzilla;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;


//
// See:
//  http://www.bugzilla.org/docs/4.2/en/html/api/index.html

public class BugzillaContext {
	private XmlRpcClientConfigImpl config;
	private XmlRpcClient client;
	
	public BugzillaContext (String bugzilla) throws MalformedURLException {
		assert (bugzilla != null);

		// Endpoint:
		URL url = new URL (bugzilla);
		String host = url.getProtocol () + "://" + url.getHost () + "/xmlrpc.cgi";


		// RPC Setup:
		this.config = new XmlRpcClientConfigImpl ();
		config.setServerURL (new URL (host));

		this.client = new XmlRpcClient ();
		this.client.setConfig (this.config);
	}

	public void enableUntrustedCertificates () {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { 
		    new X509TrustManager () {     
		        public java.security.cert.X509Certificate[] getAcceptedIssuers () { 
		            return null;
		        }

		        public void checkClientTrusted( 
		            java.security.cert.X509Certificate[] certs, String authType) {
	            }

		        public void checkServerTrusted( 
		            java.security.cert.X509Certificate[] certs, String authType) {
		        }
		    } 
		}; 

		// Install the all-trusting trust manager
		try {
		    SSLContext sc = SSLContext.getInstance("SSL"); 
		    sc.init(null, trustAllCerts, new java.security.SecureRandom ()); 
		    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory ());
		} catch (GeneralSecurityException e) {
		}
	}

	//
	// Debug Helper:
	//

	/*
	private static void printValue (String key, Object obj, String offset) {
		if (obj instanceof Map) {
			print ((Map<?,?>) obj, offset + " " + key + ":");
			return ;
		}
		if (obj instanceof Object[]) {
			System.out.print (offset + key + "[");
			for (Object obj2 : (Object[]) obj) {
				System.out.print (obj2 + ",");
			}
			System.out.println ("]");
			return ;			
		}
		System.out.println (offset + key + ": " + obj);
	}

	private static void print (Map<?,?> map, String offset) {
		System.out.println ("----");
		for (Object foo : map.keySet ()) {
			Object val = map.get (foo);

			printValue ((String) foo, val, offset);
		}
	}
	/**/
	
	//
	// Result Map Helper:
	//
	
	private static void assertType (Object got, Class<?> expected) throws BugzillaException {
		assert (expected != null);

		if (got == null) {
			throw new BugzillaException ("Incompatible type in result, got `null' expected "
					+ expected.getName ());
		}
		if (!expected.isInstance (got)) {
			throw new BugzillaException ("Incompatible type in result, got `"
					+ got.getClass ().getName ()
					+ "' expected "
					+ expected.getName ());
		}
	}

	private static void assertArrayType (Object got, Class<?> expected) throws BugzillaException {
		assert (expected != null);

		if (got instanceof Object[] == false) {
			throw new BugzillaException ("Incompatible type in result, array expected");
		}
		
		if (!got.getClass ().getComponentType ().isInstance (expected)) {
			throw new BugzillaException ("Incompatible element type in result");
		}
	}

	private static Object getFromResultMap (Map<?,?> map, String key, boolean optional) throws BugzillaException {
		assert (map != null);
		assert (key != null);

		Object res = map.get (key);
		if (res == null && !optional) {
			StringBuilder keys = new StringBuilder ();
			boolean first = true;
			for (Object keyItem : map.keySet ()) {
				//printValue (keyItem.toString (), map, "MAP: " + keyItem + ": ");
				if (first == false) {
					keys.append (',');
				}
				keys.append (keyItem);
				first = false;
			}
			throw new BugzillaException ("Unregistered key `" + key + "' in Result Map. Got: [" + keys + "]");
		}
		return res;
	}

	private static Date getDateFromResultMap (Map<?,?> map, String key) throws BugzillaException {
		return getDateFromResultMap (map, key, false);
	}

	private static Date getDateFromResultMap (Map<?,?> map, String key, boolean optional) throws BugzillaException {
		Object entries = getFromResultMap (map, key, optional);
		if (entries == null) {
			return null;
		}

		assertType (entries, Date.class);
		return (Date) entries;
	}

	private static Boolean getBoolFromResultMap (Map<?,?> map, String key) throws BugzillaException {
		return getBoolFromResultMap (map, key, false);
	}

	private static Boolean getBoolFromResultMap (Map<?,?> map, String key, boolean optional) throws BugzillaException {
		Object entries = getFromResultMap (map, key, optional);
		if (entries == null) {
			return null;
		}

		assertType (entries, Boolean.class);
		return (Boolean) entries;
	}

	private static String getStringFromResultMap (Map<?,?> map, String key) throws BugzillaException {
		return getStringFromResultMap (map, key, false);
	}

	private static String getStringFromResultMap (Map<?,?> map, String key, boolean optional) throws BugzillaException {
		Object entries = getFromResultMap (map, key, optional);
		if (entries == null) {
			return null;
		}

		assertType (entries, String.class);
		return (String) entries;
	}

	private static int getIntFromResultMap (Map<?,?> map, String key) throws BugzillaException {
		return getIntFromResultMap (map, key, false);
	}

	private static Integer getIntFromResultMap (Map<?,?> map, String key, boolean optional) throws BugzillaException {
		Object entries = getFromResultMap (map, key, optional);
		if (entries == null) {
			return null;
		}

		assertType (entries, Integer.class);
		return (Integer) entries;
	}


	private static Map<?,?> getMapFromResultMap (Map<?,?> map, String key) throws BugzillaException {
		return getMapFromResultMap (map, key, false);
	}

	private static Map<?,?> getMapFromResultMap (Map<?,?> map, String key, boolean optional) throws BugzillaException {
		Object entries = getFromResultMap (map, key, optional);
		if (entries == null) {
			return null;
		}

		assertType (entries, Map.class);
		return (Map<?,?>) entries;
	}
	private static Object[] getArrayFromResultMap (Map<?,?> map, String key) throws BugzillaException {
		return getArrayFromResultMap (map, key, false);
	}

	private static Object[] getArrayFromResultMap (Map<?,?> map, String key, boolean optional) throws BugzillaException {
		Object entries = getFromResultMap (map, key, optional);
		if (entries == null) {
			return null;
		}

		assertArrayType (entries, Object.class);
		return (Object[]) entries;
	}

	private static Integer[] getIntegerArrayFromResultMap (Map<?,?> map, String key) throws BugzillaException {
		return getIntegerArrayFromResultMap (map, key, false);
	}

	private static Integer[] getIntegerArrayFromResultMap (Map<?,?> map, String key, boolean optional) throws BugzillaException {
		Object entry = getFromResultMap (map, key, optional);
		if (entry == null) {
			return null;
		}

		assertArrayType (entry, Integer.class);

		Object[] entries = (Object[]) entry;
		Integer[] ret = new Integer[entries.length];
		for (int i = 0; i < entries.length ; i++) {
			ret[i] = (Integer) entries[i];
		}
		return ret;
	}

	private static Map<?,?>[] getMapArrayFromResultMap (Map<?,?> map, String key) throws BugzillaException {
		return getMapArrayFromResultMap (map, key, false);
	}

	private static Map<?,?>[] getMapArrayFromResultMap (Map<?,?> map, String key, boolean optional) throws BugzillaException {
		Object entry = getFromResultMap (map, key, optional);
		if (entry == null) {
			return null;
		}

		assertArrayType (entry, Map.class);

		Object[] entries = (Object[]) entry;
		Map<?,?>[] ret = new Map<?,?>[entries.length];
		for (int i = 0; i < entries.length ; i++) {
			ret[i] = (Map<?,?>) entries[i];
		}
		return ret;
	}

	/*
	private static String[] getStringArrayFromResultMap (Map<?,?> map, String key) throws BugzillaException {
		return getStringArrayFromResultMap (map, key, false);
	}
	*/
		
	private static String[] getStringArrayFromResultMap (Map<?,?> map, String key, boolean optional) throws BugzillaException {
		Object entry = getFromResultMap (map, key, optional);
		if (entry == null) {
			return null;
		}

		assertArrayType (entry, Integer.class);

		Object[] entries = (Object[]) entry;
		String[] ret = new String[entries.length];
		for (int i = 0; i < entries.length ; i++) {
			ret[i] = (String) entries[i];
		}
		return ret;
	}

	private Object execute (String method, Object[] params) throws BugzillaException {
		assert (method != null);
		assert (params != null);

		try {
			Object resObj = client.execute (method, params);
			return resObj;
		} catch (XmlRpcException e) {
			throw new BugzillaException (e);
		}
	}

	private Object execute (String method) throws BugzillaException {
		return execute (method, new Object[0]);
	}


	
	//
	// Bugzilla API:
	//

	public Date getServerTime () throws BugzillaException {
		Object resObj = execute ("Bugzilla.time");
		assertType (resObj, Map.class);		

		Map<?,?> map = (Map<?,?>) resObj;
		return getDateFromResultMap (map, "db_time");
	}

	public String getBugzillaVersion () throws BugzillaException {
		Object resObj = execute ("Bugzilla.version");
		assertType (resObj, Map.class);		

		Map<?,?> map = (Map<?,?>) resObj;
		return getStringFromResultMap (map, "version");
	}


	//
	// Product API:
	//
	
	public Integer[] getSelectableProducts () throws BugzillaException {
		Object resObj = execute ("Product.get_selectable_products");
		assertType (resObj, Map.class);

		Map<?,?> map = (Map<?,?>) resObj;
		return getIntegerArrayFromResultMap (map, "ids");
	}

	public Integer[] getEnterableProducts () throws BugzillaException {
		Object resObj = execute ("Product.get_enterable_products");
		assertType (resObj, Map.class);

		Map<?,?> map = (Map<?,?>) resObj;
		return getIntegerArrayFromResultMap (map, "ids");
	}
	
	public Integer[] getAccessibleProducts () throws BugzillaException {
		Object resObj = execute ("Product.get_accessible_products");
		assertType (resObj, Map.class);
		
		Map<?,?> map = (Map<?,?>) resObj;
		return getIntegerArrayFromResultMap (map, "ids");
	}
	
	public BugzillaProduct[] getProducts (Integer... ids) throws BugzillaException {
		// Params:
		Map<String, Object[]> params = new HashMap<String, Object[]> ();
		params.put ("ids", ids);

		// Call:
		return getProductsImpl (params);
	}

	public BugzillaProduct getProduct (String productName) throws BugzillaException {
		// Params:
		Map<String, Object[]> params = new HashMap<String, Object[]> ();
		params.put ("names", new String[] {productName});

		// Call:
		BugzillaProduct[] products = getProductsImpl (params);
		if (products.length > 0) {
			return products[0];
		} else {
			return null;
		}
	}

	private BugzillaProduct[] getProductsImpl (Map<String, Object[]> params) throws BugzillaException {
		assert (params != null);

		Object resObj = execute ("Product.get", new Object[] {params});
		assertType (resObj, Map.class);

		Map<?,?> map = (Map<?,?>) resObj;
		Object[] objArr = getArrayFromResultMap (map, "products");
		assertArrayType (objArr, Map.class);

		BugzillaProduct[] products = new BugzillaProduct[objArr.length];
		for (int i = 0; i < objArr.length; i++) {
			assertType (objArr[i], Map.class);
			Map<?,?> productMap = (Map<?,?>) objArr[i];
			
			int id = getIntFromResultMap (productMap, "id");
			String name = getStringFromResultMap (productMap, "name");
			String desc = getStringFromResultMap (productMap, "description");

			/* Components are not available for older versions
			Object[] compObjList = getArrayFromResultMap (productMap, "components");
			assertArrayType (compObjList, Map.class);

			Component[] components = new Component[compObjList.length];
			for (int c = 0; c < compObjList.length; c++) {
				Map<?,?> componentMap = (Map<?,?>) compObjList[i];

				int cid = getIntFromResultMap (componentMap, "id");
				String cname = getStringFromResultMap (componentMap, "name");
				String cdesc = getStringFromResultMap (componentMap, "description");
				int ckey = getIntFromResultMap (componentMap, "sort_key");
				
				components[i] = new Component (cid, cname, cdesc, ckey);
			} */

			products[i] = new BugzillaProduct (id, name, desc);
		}

		return products;
	}

	
	//
	// Bug API:
	//

	public BugzillaBug[] getBugs (Integer... ids) throws BugzillaException {
		// Get the result map:
		Map<String, Object[]> params = new HashMap<String, Object[]> ();
		params.put ("ids", ids);		

		Object resObj = execute ("Bug.get", new Object[] {params});
		assertType (resObj, Map.class);


		// Convert results:
		Map<?,?> map = (Map<?,?>) resObj;
		return processBugHash (map);
	}
	
	public BugzillaBug[] getBugs (String product, int page, int pageSize) throws BugzillaException {
		return getBugs (null, product, page, pageSize);
	}
	
	public BugzillaBug[] getBugs (Date changed_since, String product, int page, int pageSize) throws BugzillaException {
		assert (product != null);
		assert (pageSize > 0);
		assert (page > 0);

		int offset = (page - 1) * pageSize;

		// Get the result map:
		Map<String, Object> params = new HashMap<String, Object> ();
		params.put ("product", product);
		params.put ("limit", pageSize);		
		params.put ("offset", offset);		
		if (changed_since != null) {
			params.put ("last_change_time", changed_since);
		}
		
		Object resObj = execute ("Bug.search", new Object[] {params});
		assertType (resObj, Map.class);

		
		// Convert results:
		Map<?,?> map = (Map<?,?>) resObj;
		return processBugHash (map);
	}

	
	
	private BugzillaBug[] processBugHash (Map<?,?> map) throws BugzillaException {
		assert (map != null);

		// Convert results:
		Object[] objArr = getArrayFromResultMap (map, "bugs");
		assertArrayType (objArr, Map.class);

		BugzillaBug[] bugs = new BugzillaBug[objArr.length];
		for (int i = 0; i < objArr.length; i++) {
			assertType (objArr[i], Map.class);
			Map<?,?> bugMap = (Map<?,?>) objArr[i];
			
			Integer id = getIntFromResultMap (bugMap, "id");
			String alias = getStringFromResultMap (bugMap, "alias", true);
			String assignedTo = getStringFromResultMap (bugMap, "assigned_to");			
			String[] ccs = getStringArrayFromResultMap (bugMap, "cc", true);
			String component = getStringFromResultMap (bugMap, "component");
			Date creationTime = getDateFromResultMap (bugMap, "creation_time");
			Integer dups = getIntFromResultMap (bugMap, "dupe_of", true);
			Boolean  isOpen = getBoolFromResultMap (bugMap, "is_open");
			Date lastChangeTime = getDateFromResultMap (bugMap, "last_change_time");
			String opSys = getStringFromResultMap (bugMap, "op_sys", true);
			String priority = getStringFromResultMap (bugMap, "priority");
			String product = getStringFromResultMap (bugMap, "product");
			String resolution = getStringFromResultMap (bugMap, "resolution");
			String severity = getStringFromResultMap (bugMap, "severity");
			String status = getStringFromResultMap (bugMap, "status");
			String summary = getStringFromResultMap (bugMap, "summary");
			String version = getStringFromResultMap (bugMap, "version", true);
			String platform = getStringFromResultMap (bugMap, "platform");
			String targetMilestone = getStringFromResultMap (bugMap, "target_milestone");
			Date deadline = getDateFromResultMap (bugMap, "deadline", true);
			String qaContact = getStringFromResultMap (bugMap, "qa_contact");
			Integer[] blocks = getIntegerArrayFromResultMap (bugMap, "blocks");
			Integer[] dependsOn = getIntegerArrayFromResultMap (bugMap, "depends_on");
			String[] keywords = getStringArrayFromResultMap (bugMap, "keywords", true);


			bugs[i] = new BugzillaBug (id, alias, assignedTo,
					ccs, component,
					creationTime,
					dups, isOpen,
					lastChangeTime, opSys,
					platform, priority, product,
					resolution, severity, status, summary,
					version, targetMilestone, deadline,
					qaContact, blocks, dependsOn, keywords);
		}

		return bugs;
	}
	
	public Map<Integer, BugzillaComment[]> getComments (Collection<Integer> ids) throws BugzillaException {
		Integer[] arr = new Integer[ids.size ()];
		ids.toArray (arr);

		return getComments (arr);
	}

	public Map<Integer, BugzillaComment[]> getComments (Integer... ids) throws BugzillaException {
		// Get the result map:
		Map<String, Object[]> params = new HashMap<String, Object[]> ();
		params.put ("ids", ids);		

		Object resObj = execute ("Bug.comments", new Object[] {params});
		assertType (resObj, Map.class);

		
		// Convert results:
		Map<?,?> map = (Map<?,?>) resObj;
		Map<?,?> objMap = getMapFromResultMap (map, "bugs");
		assertType (objMap, Map.class);

		HashMap<Integer, BugzillaComment[]> res = new HashMap<Integer, BugzillaComment[]> ();
		for(Entry<?, ?> entry : objMap.entrySet()) {
			// Key:
			Object key = entry.getKey();
			assertType (key, String.class);

			Integer bugId = null;
			try {
				bugId = Integer.parseInt ((String) key);
			} catch (NumberFormatException e) {
				throw new BugzillaException (e);
			}

			// Value:
			Object value = entry.getValue();
			assertType (value, Map.class);


			BugzillaComment[] comments = processCommentHash ((Map<?,?>) value);
			res.put (bugId, comments);
		}
		
		return res;
	}

	private BugzillaComment[] processCommentHash (Map<?,?> map) throws BugzillaException {
		assert (map != null);

		Object[] objArr = getArrayFromResultMap (map, "comments");
		assertArrayType (objArr, Map.class);

		BugzillaComment[] bugs = new BugzillaComment[objArr.length];
		for (int i = 0; i < objArr.length ; i++) {
			assertType (objArr[i], Map.class);
			Map<?,?> cmntMap = (Map<?,?>) objArr[i];

			int id = getIntFromResultMap (cmntMap, "id");
			int bugId = getIntFromResultMap (cmntMap, "bug_id");
			Integer attachmentId = getIntFromResultMap (cmntMap, "attachment_id", true);
			String text = getStringFromResultMap (cmntMap, "text");
			String creator = getStringFromResultMap (cmntMap, "author", true); // Docu: creator
			Date time = getDateFromResultMap (cmntMap, "time");
			
			bugs[i] = new BugzillaComment(id, bugId, attachmentId, text,
				creator, time);
		}

		return bugs;
	}

	public Map<Integer, BugzillaHistory[]> getHistory (Collection<Integer> ids) throws BugzillaException {
		Integer[] arr = new Integer[ids.size ()];
		ids.toArray (arr);

		return getHistory (arr);
	}

	public Map<Integer, BugzillaHistory[]> getHistory (Integer... ids) throws BugzillaException {
		// Get the result map:
		Map<String, Object[]> params = new HashMap<String, Object[]> ();
		params.put ("ids", ids);

		Object resObj = execute ("Bug.history", new Object[] {params});
		assertType (resObj, Map.class);

		
		// Convert results:
		Map<?,?> map = (Map<?,?>) resObj;
		Object[] objArr = getArrayFromResultMap (map, "bugs");
		assertArrayType (objArr, Map.class);

		HashMap<Integer, BugzillaHistory[]> history = new HashMap<Integer, BugzillaHistory[]> ();
		for (int i = 0; i < objArr.length; i++) {
			assertType (objArr[i], Map.class);
			Map<?,?> bugMap = (Map<?,?>) objArr[i];

			int id = getIntFromResultMap (bugMap, "id");
			Map<?,?>[] historiesMap = getMapArrayFromResultMap (bugMap, "history");

			BugzillaHistory[] histories = new BugzillaHistory[historiesMap.length];
			for (int h = 0; h < histories.length; h++) {
				Map<?,?> historyMap = historiesMap[h];

				Date when = getDateFromResultMap (historyMap, "when");
				String who = getStringFromResultMap (historyMap, "who");
				Map<?, ?>[] changesArr = getMapArrayFromResultMap (historyMap, "changes");
				
				BugzillaChange[] changes = new BugzillaChange[changesArr.length];
				for (int c = 0; c < changesArr.length ; c++) {
					Map<?,?> changeMap = changesArr[c];

					String fieldName = getStringFromResultMap (changeMap, "field_name");
					String removed = getStringFromResultMap (changeMap, "removed");
					String added = getStringFromResultMap (changeMap, "added");
					Integer attachmentId = getIntFromResultMap (changeMap, "attachment_id", true);

					changes[c] = new BugzillaChange (fieldName, removed, added, attachmentId);
				}
				
				histories[h] = new BugzillaHistory (when, who, changes);
			}

			history.put (id, histories);
		}

		return history;
	}
	
	/* Not provided by our bugzilla version
	public Map<Integer, Attachment[]> getAttachments (Integer... bugIds) throws BugzillaException {
		// Get the result map:
		Map<String, Object[]> params = new HashMap<String, Object[]> ();
		params.put ("ids", bugIds);		

		Object resObj = execute ("Bug.attachments", new Object[] {params});
		assertType (resObj, Map.class);

		// Convert results:
		Map<?,?> map = (Map<?,?>) resObj;
		Map<?,?> objMap = getMapFromResultMap (map, "bugs");
		assertType (objMap, Map.class);

		HashMap<Integer, Attachment[]> res = new HashMap<Integer, Attachment[]> ();
		for(Entry<?, ?> entry : objMap.entrySet()) {
			// Key:
			Object key = entry.getKey();
			assertType (key, String.class);

			Integer bugId = null;
			try {
				bugId = Integer.parseInt ((String) key);
			} catch (NumberFormatException e) {
				throw new BugzillaException (e);
			}

			// Value:
			Object value = entry.getValue();
			assertType (value, Map.class);


			Attachment[] comments = processAttachmentHash ((Map<?,?>) value);
			res.put (bugId, comments);
		}
		
		return res;
	}

	private Attachment[] processAttachmentHash (Map<?,?> map) throws BugzillaException {
		Object[] objArr = getArrayFromResultMap (map, "comments");
		assertArrayType (objArr, Map.class);

		Attachment[] attachments = new Attachment[objArr.length];
		for (int i = 0; i < objArr.length ; i++) {
			assertType (objArr[i], Map.class);
			Map<?,?> attachmentMap = (Map<?,?>) objArr[i];

			int id = getIntFromResultMap (attachmentMap, "id");
			int bugId = getIntFromResultMap (attachmentMap, "bug_id");
			Date creationTime = getDateFromResultMap (attachmentMap, "ceration_time");
			Date lastChangeTime = getDateFromResultMap (attachmentMap, "last_change_time");
			String fileName = getStringFromResultMap (attachmentMap, "file_name");
			String summary = getStringFromResultMap (attachmentMap, "summary");
			String creator = getStringFromResultMap (attachmentMap, "creator");
			Boolean isPrivate = getBoolFromResultMap (attachmentMap, "is_private");
			Boolean isObsolete = getBoolFromResultMap (attachmentMap, "is_obsolete");
			Boolean isPatch = getBoolFromResultMap (attachmentMap, "is_patch");
			String data = getStringFromResultMap (attachmentMap, "data");
			String contentType = getStringFromResultMap (attachmentMap, "content_type");

			attachments[i] = new Attachment(id, bugId, creationTime,
					lastChangeTime, fileName, contentType, summary,
					creator, isPrivate, isObsolete,
					isPatch, data);
		}

		return attachments;
	}*/



	//
	// User API:
	//

	public boolean login (String userName, String passwd) throws BugzillaException {
		assert (userName != null);
		assert (passwd != null);

		// Get the result map:
		Map<String, Object> params = new HashMap<String, Object> ();
		params.put ("login", userName);
		params.put ("password", passwd);

		Object resObj = execute ("User.login", new Object[] {params});
		assertType (resObj, Map.class);


		// Convert results:
		Map<?,?> map = (Map<?,?>) resObj;
		return map.containsKey ("id");
	}

	public void logout () throws BugzillaException {
		// Get the result map:
		execute ("User.logout", new Object[] {});
	}	

	public BugzillaUser[] getUsers (String... names) throws BugzillaException {
		// Get the result map:
		Map<String, Object[]> params = new HashMap<String, Object[]> ();
		params.put ("names", names);

		Object resObj = execute ("User.get", new Object[] {params});
		assertType (resObj, Map.class);

		// Process results:
		Map<?,?> map = (Map<?,?>) resObj;
		Object[] objArr = getArrayFromResultMap (map, "users");
		assertArrayType (objArr, Map.class);

		BugzillaUser[] users = new BugzillaUser[objArr.length];
		for (int i = 0; i < objArr.length; i++) {
			assertType (objArr[i], Map.class);
			Map<?,?> userMap = (Map<?,?>) objArr[i];

			int id = getIntFromResultMap (userMap, "id");
			String realName = getStringFromResultMap (userMap, "real_name");
			String email = getStringFromResultMap (userMap, "email", true);
			String name = getStringFromResultMap (userMap, "name");

			users[i] = new BugzillaUser (id, realName, email, name);
		}

		return users;
	}

	
	
	//
	// Test Main:
	//
	
	/*
	public static void main (String[] args) {
		try {
			BugzillaContext context = new BugzillaContext ("https://bugzilla.gnome.org");
			context.enableUntrustedCertificates ();

			BugzillaProduct product = context.getProduct ("valadoc");
			System.out.println ("pid: " + product);
			
			// Date since = new java.text.SimpleDateFormat ("dd/MM/yyyy/hh:mm:ss").parse ("01/01/2015/00:00:00");
			// System.out.println (since);
			// BugzillaBug[] changedBugs = context.getBugs (since, "valadoc", 1, 5);
			// for (BugzillaBug bug : changedBugs) {
			// 	System.out.println(bug);
			// }

			// String version = context.getBugzillaVersion ();
			// System.out.println (version);
			
			// Map<Integer, BugzillaHistory[]> histories = context.getHistory (559704);
			// for (BugzillaHistory c : histories.get (559704)) {
			// 	System.out.println (c);
			// 	for (BugzillaChange v : c.getChanges ()) {
			// 		System.out.println ("  " + v);
			// 	}
			// }

			// BugzillaProduct[] products = context.getProducts (148);
			// for (BugzillaProduct p : products) {
			//  	System.out.println(p);
			// }

			// Integer[] productIds = context.getAccessibleProducts ();
			// for (Integer p : productIds) {
			// 	System.out.println(p);
			// }

			// BugzillaBug[] bugs = context.getBugs ("valadoc", 1, 5);
			// for (BugzillaBug bug : bugs) {
			// 	System.out.println(bug);
			// }
	
			// histories = context.getHistory (703688, 692187);
			// for (BugzillaHistory c : histories.get (703688)) {
			// 	System.out.println (c);
			// 	for (BugzillaChange v : c.getChanges ()) {
			// 		System.out.println ("  " + v);
			// 	}
			// }

			// context.login ("MY LOGIN", "MY PW");
			// context.logout ();
			// 
			// 
			// User[] users = context.getUsers ("flo.brosch@gmail.com");
			// for (User obj : users) {
			// 	System.out.println (obj);
			// }
			// 
			// Bug[] bugs = context.getBugs ("valadoc", 1, 10);
			// for (Bug obj : bugs) {
			// 	System.out.println (obj);
			// }
			// 
			// Map<Integer, Comment[]> comments = context.getComments (725194);
			// for (Comment c : comments.get (725194)) {
			// 	System.out.println (c);
			// }
			// 
 			// Object[] map = context.getProducts (148);
			// // Object[] map = context.getSelectableProducts ( );
			// for (Object obj : map) {
			// 	System.out.println (obj);
			// }
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (BugzillaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		//} catch (java.text.ParseException e) {
		//	// TODO Auto-generated catch block
		//	e.printStackTrace();
		}
	}
	/**/
}
