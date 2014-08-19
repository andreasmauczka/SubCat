/* Context.java
 *
 * Copyright (C) 2014  Brosch Florian
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
 * 	Florian Brosch <flo.brosch@gmail.com>
 */

package at.ac.tuwien.inso.hurrier.bugzilla;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
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

public class Context {
	private XmlRpcClientConfigImpl config;
	private XmlRpcClient client;
	
	public Context (String bugzilla) throws MalformedURLException {
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
	private void printValue (String key, Object obj, String offset) {
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

	private void print (Map<?,?> map) {
		print (map, "");
	}

	private void print (Map<?,?> map, String offset) {
		System.out.println ("----");
		for (Object foo : map.keySet ()) {
			Object val = map.get (foo);

			printValue ((String) foo, val, offset);
		}
	}
	*/
	
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
			throw new BugzillaException ("Unregistered key `" + key + "' in Result Map");
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

	public Product[] getProducts (Integer... ids) throws BugzillaException {
		// Params:
		Map<String, Object[]> params = new HashMap<String, Object[]> ();
		params.put ("ids", ids);

		// Call:
		return getProductsImpl (params);
	}

	public Product[] getProductsImpl (Map<String, Object[]> params) throws BugzillaException {
		assert (params != null);

		Object resObj = execute ("Product.get", new Object[] {params});
		assertType (resObj, Map.class);

		Map<?,?> map = (Map<?,?>) resObj;
		Object[] objArr = getArrayFromResultMap (map, "products");
		assertArrayType (objArr, Map.class);

		Product[] products = new Product[objArr.length];
		for (int i = 0; i < objArr.length; i++) {
			assertType (objArr[i], Map.class);
			Map<?,?> productMap = (Map<?,?>) objArr[i];
			
			int id = getIntFromResultMap (productMap, "id");
			String name = getStringFromResultMap (productMap, "name");
			String desc = getStringFromResultMap (productMap, "description");
			Object[] compObjList = getArrayFromResultMap (productMap, "components");
			assertArrayType (compObjList, Map.class);

			/* Components are not available for older versions
			Component[] components = new Component[compObjList.length];
			for (int c = 0; c < compObjList.length; c++) {
				Map<?,?> componentMap = (Map<?,?>) compObjList[i];

				int cid = getIntFromResultMap (componentMap, "id");
				String cname = getStringFromResultMap (componentMap, "name");
				String cdesc = getStringFromResultMap (componentMap, "description");
				int ckey = getIntFromResultMap (componentMap, "sort_key");
				
				components[i] = new Component (cid, cname, cdesc, ckey);
			} */

			products[i] = new Product (id, name, desc);
		}

		return products;
	}

	
	//
	// Bug API:
	//

	public Bug[] getBugs (Integer... ids) throws BugzillaException {
		// Get the resutl map:
		Map<String, Object[]> params = new HashMap<String, Object[]> ();
		params.put ("ids", ids);		

		Object resObj = execute ("Bug.get", new Object[] {params});
		assertType (resObj, Map.class);


		// Convert resuls:
		Map<?,?> map = (Map<?,?>) resObj;
		return processBugHash (map);
	}
	
	public Bug[] getBugs (String product, int page, int pageSize) throws BugzillaException {
		assert (product != null);

		// Get the resutl map:
		Map<String, Object> params = new HashMap<String, Object> ();
		params.put ("product", product);
		params.put ("limit", pageSize);		
		params.put ("offset", page);		

		Object resObj = execute ("Bug.search", new Object[] {params});
		assertType (resObj, Map.class);

		
		// Convert resuls:
		Map<?,?> map = (Map<?,?>) resObj;
		return processBugHash (map);
	}

	private Bug[] processBugHash (Map<?,?> map) throws BugzillaException {
		assert (map != null);

		// Convert resuls:
		Object[] objArr = getArrayFromResultMap (map, "bugs");
		assertArrayType (objArr, Map.class);

		Bug[] bugs = new Bug[objArr.length];
		for (int i = 0; i < objArr.length; i++) {
			assertType (objArr[i], Map.class);
			Map<?,?> bugMap = (Map<?,?>) objArr[i];
			
			Integer id = getIntFromResultMap (bugMap, "id");
			String alias = getStringFromResultMap (bugMap, "alias");
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

			bugs[i] = new Bug (id, alias, assignedTo,
					ccs, component,
					creationTime,
					dups, isOpen,
					lastChangeTime, opSys,
					priority, product,
					resolution, severity, status, summary,
					version);
		}

		return bugs;
	}
	
	public Map<Integer, Comment[]> getComments (Integer... ids) throws BugzillaException {
		// Get the resutl map:
		Map<String, Object[]> params = new HashMap<String, Object[]> ();
		params.put ("ids", ids);		

		Object resObj = execute ("Bug.comments", new Object[] {params});
		assertType (resObj, Map.class);

		
		// Convert resuls:
		Map<?,?> map = (Map<?,?>) resObj;
		Map<?,?> objMap = getMapFromResultMap (map, "bugs");
		assertType (objMap, Map.class);

		HashMap<Integer, Comment[]> res = new HashMap<Integer, Comment[]> ();
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


			Comment[] comments = processCommentHash ((Map<?,?>) value);
			res.put (bugId, comments);
		}
		
		return res;
	}

	private Comment[] processCommentHash (Map<?,?> map) throws BugzillaException {
		assert (map != null);

		Object[] objArr = getArrayFromResultMap (map, "comments");
		assertArrayType (objArr, Map.class);

		Comment[] bugs = new Comment[objArr.length];
		for (int i = 0; i < objArr.length ; i++) {
			assertType (objArr[i], Map.class);
			Map<?,?> cmntMap = (Map<?,?>) objArr[i];

			int id = getIntFromResultMap (cmntMap, "id");
			int bugId = getIntFromResultMap (cmntMap, "bug_id");
			Integer attachmentId = getIntFromResultMap (cmntMap, "attachment_id", true);
			String text = getStringFromResultMap (cmntMap, "text");
			String creator = getStringFromResultMap (cmntMap, "author", true); // Docu: creator
			Date time = getDateFromResultMap (cmntMap, "time");
			
			bugs[i] = new Comment(id, bugId, attachmentId, text,
				creator, time);
		}

		return bugs;
	}

	public Map<Integer, History[]> getHistory (Integer... ids) throws BugzillaException {
		// Get the resutl map:
		Map<String, Object[]> params = new HashMap<String, Object[]> ();
		params.put ("ids", ids);

		Object resObj = execute ("Bug.history", new Object[] {params});
		assertType (resObj, Map.class);

		
		// Convert resuls:
		Map<?,?> map = (Map<?,?>) resObj;
		Object[] objArr = getArrayFromResultMap (map, "bugs");
		assertArrayType (objArr, Map.class);

		HashMap<Integer, History[]> history = new HashMap<Integer, History[]> ();
		for (int i = 0; i < objArr.length; i++) {
			assertType (objArr[i], Map.class);
			Map<?,?> bugMap = (Map<?,?>) objArr[i];

			int id = getIntFromResultMap (bugMap, "id");
			Map<?,?>[] historiesMap = getMapArrayFromResultMap (bugMap, "history");

			History[] histories = new History[historiesMap.length];
			for (int h = 0; h < histories.length; h++) {
				Map<?,?> historyMap = historiesMap[h];

				Date when = getDateFromResultMap (historyMap, "when");
				String who = getStringFromResultMap (historyMap, "who");
				Map<?, ?>[] changesArr = getMapArrayFromResultMap (historyMap, "changes");
				
				Change[] changes = new Change[changesArr.length];
				for (int c = 0; c < changesArr.length ; c++) {
					Map<?,?> changeMap = changesArr[c];

					String fieldName = getStringFromResultMap (changeMap, "field_name");
					String removed = getStringFromResultMap (changeMap, "removed");
					String added = getStringFromResultMap (changeMap, "added");
					Integer attachmentId = getIntFromResultMap (changeMap, "attachment_id", true);

					changes[c] = new Change (fieldName, removed, added, attachmentId);
				}
				
				histories[h] = new History (when, who, changes);
			}

			history.put (id, histories);
		}

		return history;
	}
	
	/* Not provided by our bugzilla version
	public Map<Integer, Attachment[]> getAttachments (Integer... bugIds) throws BugzillaException {
		// Get the resutl map:
		Map<String, Object[]> params = new HashMap<String, Object[]> ();
		params.put ("ids", bugIds);		

		Object resObj = execute ("Bug.attachments", new Object[] {params});
		assertType (resObj, Map.class);

		// Convert resuls:
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

		// Get the resutl map:
		Map<String, Object> params = new HashMap<String, Object> ();
		params.put ("login", userName);
		params.put ("password", passwd);

		Object resObj = execute ("User.login", new Object[] {params});
		assertType (resObj, Map.class);


		// Convert resuls:
		Map<?,?> map = (Map<?,?>) resObj;
		return map.containsKey ("id");
	}

	public void logout () throws BugzillaException {
		// Get the resutl map:
		execute ("User.logout", new Object[] {});
	}	

	public User[] getUsers (String... names) throws BugzillaException {
		// Get the resutl map:
		Map<String, Object[]> params = new HashMap<String, Object[]> ();
		params.put ("names", names);

		Object resObj = execute ("User.get", new Object[] {params});
		assertType (resObj, Map.class);

		// Process results:
		Map<?,?> map = (Map<?,?>) resObj;
		Object[] objArr = getArrayFromResultMap (map, "users");
		assertArrayType (objArr, Map.class);

		User[] users = new User[objArr.length];
		for (int i = 0; i < objArr.length; i++) {
			assertType (objArr[i], Map.class);
			Map<?,?> userMap = (Map<?,?>) objArr[i];

			int id = getIntFromResultMap (userMap, "id");
			String realName = getStringFromResultMap (userMap, "real_name");
			String email = getStringFromResultMap (userMap, "email", true);
			String name = getStringFromResultMap (userMap, "name");

			users[i] = new User(id, realName, email, name);
		}

		return users;
	}

	
	
	//
	// Test Main:
	//
	
	/*
	public static void main (String[] args) {
		try {
			Context context = new Context ("https://bugzilla.gnome.org");
			context.enableUntrustedCertificates ();

			Bug[] bugs = context.getBugs (688732);
			for (Bug bug : bugs) {
				System.out.println(bug);
			}
	
			Map<Integer, History[]> histories = context.getHistory (688732);
			for (History c : histories.get (688732)) {
				System.out.println (c);
				for (Change v : c.getChanges ()) {
					System.out.println ("  " + v);
				}
			}

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
		}
	}
	*/
}
