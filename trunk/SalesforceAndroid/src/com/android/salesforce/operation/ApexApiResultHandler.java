/**
 * Copyright (C) 2008 Dai Odahara.
 */

package com.android.salesforce.operation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import android.content.ContentValues;
import android.util.Log;

import com.android.salesforce.frame.FieldHolder;
import com.android.salesforce.frame.SectionHolder;
import com.android.salesforce.util.SObjectDB;
import com.android.salesforce.util.SObjectFactory;
import com.android.salesforce.util.StaticInformation;

public class ApexApiResultHandler {
	private static final String TAG = "ApexApiResultHandler";

	public ApexApiResultHandler() {
	}

	/** extracts session id of salesforce force.com api */
	public void extractSessionId(Object result) {
		Log.v(TAG, "Start Extracting Session Id");
		String res = result.toString();
		String si = "sessionId=";
		int ss = res.indexOf(si);
		int es = res.indexOf(' ', ss);
		StaticInformation.SESSION_ID = res.substring(ss + si.length(), es - 1);
		Log.v(TAG, "Session Id : " + StaticInformation.SESSION_ID);
	}

	/** extracts API server URL of salesforce force.com api */
	public void extractAPIServerURL(Object result) {
		String res = result.toString();
		String si = "serverUrl=";
		int ss = res.indexOf(si);
		int es = res.indexOf(' ', ss);
		Log.v(TAG, "\tAPI Server URL : "
				+ res.substring(ss + si.length(), es - 1));
		StaticInformation.API_SERVER_URL = res.substring(ss + si.length(),
				es - 1);
	}

	/** extracts record item name and value from soap */
	public HashMap<String, String> extractNameAndValue(String records) {
		HashMap<String, String> ret = new HashMap<String, String>();
		String[] elements = records.split("; ");
		int size = elements.length;
		for (int i = 0; i < size; i++) {
			String[] vals = elements[i].split("=");
			ret.put(vals[0], vals[1]);
		}
		return ret;
	}

	/** extracts query result size */
	public int getQueryResultSize(String res) {
		Log.v(TAG, "Result : " + res);
		String si = "size=";
		int ss = res.indexOf(si);
		int es = res.indexOf(';', ss + 1);
		Log.v(TAG, "Result size : "
				+ Integer.valueOf(res.substring(ss + si.length(), es)));
		return Integer.valueOf(res.substring(ss + si.length(), es));
	}

	/** save data into db (at present, hashmap object */
	public ArrayList<ContentValues> saveData(String[] records, String sobject) {
		int ss = 0, es = 0;
		/** for cache */
		HashMap<String, HashMap> iv = new HashMap<String, HashMap>();
		ArrayList<String> where = new ArrayList<String>();
		
		/** for local db */
		ArrayList<ContentValues> insertData  = new ArrayList<ContentValues>();
		
		for(String s : records) {
			HashMap nav = new HashMap();
			ContentValues cv = new ContentValues();
			
			nav.put("SObjectType", sobject);
			ss = s.indexOf("Id=");
			es = s.indexOf("; ", ss);
			
			String Id = s.substring(ss + "Id=".length(), es);
			iv.put(Id, nav);
			cv.put("Id", Id);
			
			//Log.v(TAG, "\tId=" + s.substring(ss + "Id=".length(), es));
		
			String[] rr = s.split("; ");
			//Log.v(TAG, "\t\t" + "SObjectType=" + nav.get("SObjectType"));
			
			for(String r : rr) {
				String[] tt = r.split("=");
				Log.v(TAG, "\t\t" + tt[0] + "=" + tt[1]);

				/** for where query */
				if(tt[0].equals("AccountId"))where.add(tt[1]);

				nav.put(tt[0], tt[1]);
				cv.put(tt[0], tt[1]);
			}
			insertData.add(cv);
			
		}
		SObjectDB.SOBJECT_DB.put(sobject, iv);
		
		/** for opportunity query */
		SObjectDB.WHERE_HOLDER.put(sobject, where);
		/**
		for(String s : where) {
			Log.v(TAG, "Added Id :" + s);
		}
		*/
		return insertData;

	}
	
	/** extracts result records */	
	public String[] analyzeQueryResults(Object result, String sobject) {

		String res = result.toString();
		int sa = getQueryResultSize(res);
		String[] ret = new String[sa];

		String si = "records=" + sobject + "{";
		int ss = 0, es = 0;
		for (int i = 0; i < sa; i++) {
			ss = res.indexOf(si, es);
			es = res.indexOf('}', ss + 1);
			ret[i] = res.substring(ss + si.length(), es);
			//Log.v(TAG, "record : " + ret[i]);
		}
		return ret;
	}
	
	/** analyze apex describe api call result */	
	public StringBuffer analyzeDescribeSObjectResults(Object result, String sobject) {

		StringBuffer table = new StringBuffer("create table " + sobject + " (rowid integer primary key, ");
		
		String res = result.toString();
		// System.out.println( res.length() );

		String fn = "fields=anyType";
		int start = res.indexOf(fn);

		String kpn = "keyPrefix";
		int end = res.indexOf(kpn);

		String f = res.substring(start, end);
		// System.out.println( f );
		// System.out.println( res.substring(matcher.start(), matcher.start() +
		// start.length()) );

		String[] fs = f.split("fields=anyType\\{");
		int fSize = fs.length, s1 = 0, e1 = 0;

		String[] ps;
		int pSize = 0, s2 = 0, e2 = 0;

		for (int i = 1; i < fSize; i++) {
			s1 = fs[i].indexOf("autoNumber=");
			e1 = fs[i].indexOf(";", s1);

			//table.append("autoNumber integer, ");
			// System.out.print(i + ":autoNumber=" + fs[i].substring(s1 +
			// "autoNumber=".length(), e1));

			s1 = fs[i].indexOf("calculated=");
			e1 = fs[i].indexOf(";", s1);

			//table.append("calculated integer, ");

			// System.out.print(":calculated=" + fs[i].substring(s1 +
			// "calculated=".length(), e1));

			s1 = fs[i].indexOf("idLookup=");
			e1 = fs[i].indexOf(";", s1);

			//table.append("idLookup integer, ");

			// System.out.print(":idLookup=" + fs[i].substring(s1 +
			// "idLookup=".length(), e1));

			s1 = fs[i].indexOf("label=");
			e1 = fs[i].indexOf(";", s1);
			
			//table.append("label text, ");

			//String label = fs[i].substring(s1 + "label=".length(), e1);
			// SObjectDB.AccountLayoutLabel.add(fs[i].substring(s1 +
			// "label=".length(), e1));
			//Log.v(TAG, "label=" + fs[i].substring(s1 + "label=".length(), e1));
			// System.out.print(":label=" + fs[i].substring(s1 +
			// "label=".length(), e1));

			s1 = fs[i].indexOf("length=", e1);
			e1 = fs[i].indexOf(";", s1);

			//table.append("length integer, ");

			// System.out.print(":length=" + fs[i].substring(s1 +
			// "length=".length(), e1));

			s1 = fs[i].indexOf("name=", e1);
			e1 = fs[i].indexOf(";", s1);
			String name = fs[i].substring(s1 + "name=".length(), e1);
			
			s1 = fs[i].indexOf("soapType=xsd:", e1);
			e1 = fs[i].indexOf(";", s1);
			String type = fs[i].substring(s1 + "soapType=xsd:".length(), e1).equals("int") ? "int" : "text";
			Log.v(TAG, "soapType=xsd:" + type);

			
			table.append(name + " " + type + ", ");

			// SObjectDB.AccountLayout.append(fs[i].substring(s1 +
			// "name=".length(), e1)).append(",");
			ps = fs[i].split("picklistValues=anyType\\{");
			pSize = ps.length;
			s2 = 0;
			e2 = 0;
			for (int j = 1; j < pSize; j++) {
				s2 = ps[j].indexOf("label=");
				e2 = ps[j].indexOf(";", s2);
				// System.out.print("\tlabel=" + ps[j].substring(s2 +
				// "label=".length(), e2));

				s2 = ps[j].indexOf("value=", e2);
				e2 = ps[j].indexOf(";", s2);

				// System.out.println(":value=" + ps[j].substring(s2 +
				// "value=".length(), e2));
			}
		}
		fSize++;

		//int sa = getDescribeResultFieldSize(result);
		//String[] ret = new String[sa];

		table.delete(table.length()-2, table.length());
		table.append(");");
		return table;
	}

	/** returns field count of Sobject */
	private int getDescribeResultFieldSize(Object result) {
		Pattern pattern = Pattern.compile("fields=anyType");
		Matcher matcher = pattern.matcher(result.toString());
		int size = 0;
		while (matcher.find()) {
			size++;
		}
		Log.v(TAG, "Fields size : " + size);
		return size;
	}
	
	/** create user data base */
	public void saveUserData(String[] records, String sobject) {
		Log.v(TAG, "\nAnalyzing User...");
		HashMap<String, String> hm = new HashMap<String, String>();
		int s1 = 0, e1 = 0;
		for(String s : records) {

			s1 = s.indexOf( "Id=" );
			e1 = s.indexOf( ";", s1 );	
			String id = s.substring(s1 + "Id=".length(), e1);	
			
			String[] rr = s.split("; ");
			//Log.v(TAG, "\t\t" + "SObjectType=" + nav.get("SObjectType"));
			
			for(String r : rr) {
				String[] tt = r.split("=");
				//Log.v(TAG, "User Info :" + tt[0] + "=" + tt[1]);
				hm.put(tt[0], tt[1]);
			}
			SObjectDB.SYSTEM_DB.put(id, hm);
		}
	}
	
	/** anylyze describeLayout result */
	public void analyzeDescribeLayoutResults(Object result, String sobject) {
		Log.v(TAG, "\nAnalyzing Layout...");
		
		SObjectFactory sf = new SObjectFactory();
		sf.sections = new ArrayList<SectionHolder>();
		String res = result.toString();
		
		//String fn = "layouts=anyType";
		String fn = "detailLayoutSections=anyType";
		int start = res.indexOf( fn );
		
		String kpn = "editLayoutSections=anyType";
		int end = res.indexOf( kpn );

		String f = res.substring( start + fn.length() + 1 , end );
		Log.v(TAG, "orignal :" +  f );
		
		/** analyze start form 'detailLayoutSections' */
		String[] dls = f.split("detailLayoutSections=anyType\\{");

		//System.out.println("0 : " + dls[0]);
		//System.out.println("1 : " + dls[1]);
		
		int index = 0, e1 = 0;
		for(String s : dls ){
			SectionHolder sh = new SectionHolder();
			sh.fields = new ArrayList<FieldHolder>();
			//System.out.println(s);
			
			//String heading = getHeading(s);
			getHeading(s, sh);
			
			//System.out.println("" + heading);
			
			//sh.name = heading;
			sh.sectionOrder = index;
			
			if(!sh.name.equals("Address Information"))getLayoutItems(s, sh);
			
			index++;
			sf.sections.add(sh);

			/*
			s1 = fs[i].indexOf( "calculated=" );
			e1 = fs[i].indexOf( ";", s1 );	
			
			
			System.out.print(":calculated=" + fs[i].substring(s1 + "calculated=".length(), e1));
				*/
		}
		HashMap<String,SObjectFactory> te = new HashMap<String,SObjectFactory>();
		te.put(sobject, sf);
		SObjectDB.SOBJECTS.put(sobject, sf);
	}
	
	private void getHeading(String s, SectionHolder sh) {		
		int s1 = s.indexOf( "heading=" );
		int e1 = s.indexOf( ";", s1 );	
		
		String ret = s.substring(s1 + "heading=".length(), e1);		
		Log.v(TAG, ret);
		sh.name = ret;
	}
	
	private void getLayoutItems(String s, SectionHolder sh){
		String l = "layoutItems=anyType";
		int sa = s.indexOf(l);
		String temp = s.substring(sa + l.length() + 1, s.length());
		//System.out.println(temp);
		String[] items = temp.split(l + "\\{");
		int s1 = 0;
		int e1 = 0;	
		for(String a : items) {
			FieldHolder fs = new FieldHolder();
			//System.out.println("aLine :" + a);
			s1 = a.indexOf( "editable=", e1 );
			e1 = a.indexOf( ";", s1 );	
			fs.editable = a.substring(s1 + "editable=".length(), e1).equals("true") ? true : false;
			//Log.v(TAG, "\teditable=" + fs.editable);
			
			s1 = a.indexOf( "label=", e1 );
			e1 = a.indexOf( ";", s1 );
			fs.label = a.substring(s1 + "label=".length(), e1);
			//Log.v(TAG, "\tlabel=" + fs.label);
			
			s1 = a.indexOf( "layoutComponents=anyType", e1 );
			if(-1 == s1) continue;
			else {
				e1 = a.indexOf( " };", s1 );
				String aa = a.substring(s1 , e1 + 1);
				//System.out.println("\tlayoutComponents=anyType" + aa);
				
				getLayoutComponents(aa, fs);
			}
			s1 = a.indexOf( "placeholder=", e1 );
			e1 = a.indexOf( ";", s1 );
			fs.placeholder = a.substring(s1 + "placeholder=".length(), e1).equals("true") ? true : false;;
			//Log.v(TAG, "\tplaceholder=" + fs.placeholder);
			
			s1 = a.indexOf( "required=", e1 );
			e1 = a.indexOf( ";", s1 );
			fs.required = a.substring(s1 + "required=".length(), e1).equals("true") ? true : false;;			
			//Log.v(TAG, "\trequired=" + fs.required);
			
			sh.fields.add(fs);
		}
		/** set sort */

		ArrayList<FieldHolder> ff = sh.fields;
		Collections.sort(ff, new Comparator(){
			public int compare(Object o1, Object o2) {
			    return ((FieldHolder)o1).tabOrder - ((FieldHolder)o2).tabOrder;
			}
		});
	}
	
	private void  getLayoutComponents(String s, FieldHolder fs) {
		String l = "layoutComponents=anyType";
		int sa = s.indexOf(l);
		
		String temp = s.substring(sa + l.length() + 1 , s.length()-1);
		//System.out.println(temp);
		String[] items = temp.split(l + "\\{");
		int s1 = 0;
		int e1 = 0;	

		for(String a : items) {
			//System.out.println("bLine :" + a);
			s1 = a.indexOf( "displayLines=" );
			e1 = a.indexOf( ";", s1 );
			fs.desplayLines = Integer.valueOf(a.substring(s1 + "displayLines=".length(), e1));
			//Log.v(TAG, "\t\tdisplayLines=" + fs.desplayLines);
			
			s1 = a.indexOf( "tabOrder=" );
			e1 = a.indexOf( ";", s1 );
			fs.tabOrder = Integer.valueOf(a.substring(s1 + "tabOrder=".length(), e1));
			//Log.v(TAG, "\t\ttabOrder=" + fs.tabOrder);
			
			s1 = a.indexOf( "type=" );
			e1 = a.indexOf( ";", s1 );
			fs.type = a.substring(s1 + "type=".length(), e1);			
			//Log.v(TAG, "\t\ttype=" + fs.type);
			
			s1 = a.indexOf( "value=" );
			e1 = a.indexOf( ";", s1 );
			fs.value = a.substring(s1 + "value=".length(), e1);						
			//Log.v(TAG, "\t\tvalue=" + fs.value);
			
		}
		//return "";
	}
}