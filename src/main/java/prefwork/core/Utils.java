package prefwork.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.management.RuntimeErrorException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.rating.test.TestInterpreter;
import weka.core.Attribute;
import weka.core.Instance;


public abstract class Utils {

	public static void addStringValue(String value, double[] vals, Attribute attr){
		if(attr.indexOfValue(value)!=-1){
			vals[attr.index()] = attr.indexOfValue(value);
		}
		else
	     vals[attr.index()] = attr.addStringValue(value);
	}
	public static void addStringValue(String value, Instance l, Attribute attr){
		//value = value.trim().replaceAll("\"", "");
		if(attr.indexOfValue(value)==-1){
			attr.addStringValue(value);
		}
		l.setValue(attr.index(), attr.indexOfValue(value));
	}
	public static List<Object> getListFromString(String s){
			String[]array = s.split(",");
			List<Object> val = Utils.getList(array.length);
			for(String st : array){
				val.add(st);
			}
			return val;
	}
	
	public static List<Object> transformLists(List<Object> l){
		if(l== null)
			return null;
		for (int i = 16; i < l.size(); i++) {			
			l.set(i, getListFromString(l.get(i).toString()));
		}
		return l;
	}
	
	/**
	 * Closes given statement.
	 * @param stmt
	 * @param valuesSet
	 */
	public static void closeStatement(PreparedStatement stmt,
			ResultSet valuesSet) {
		try {
			if (valuesSet != null)
				valuesSet.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			if (stmt != null)
				stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static int objectToInteger(Object number) {
		if (number instanceof Number)
			((Number) number).intValue();

		if (number instanceof BigDecimal)
			return ((BigDecimal) number).intValueExact();
		if (number instanceof Double)
			return ((Double) number).intValue();
		if (number instanceof Integer)
			return (Integer) number;
		try{
			String s=number.toString();
			if(s.length()==0)
				return 0;
			int d = Integer.parseInt(s);
			return d;
		}catch(Exception e){}
		throw new RuntimeErrorException(null, "Unable to process "
				+ number.getClass().getCanonicalName()+". Value "+number);

	}

	public static long objectToLong(Object number) {
		if (number instanceof Number)
			((Number) number).longValue();

		if (number instanceof BigDecimal)
			return ((BigDecimal) number).longValueExact();
		if (number instanceof Long)
			return (Long) number;
		
		try{
			String s=number.toString();
			long d = Long.parseLong(s);
			return d;
		}catch(Exception e){}
		throw new RuntimeErrorException(null, "Unable to process "+number.getClass().getCanonicalName()+". Value "+number);

	}


	public static int[] stringListToIntArray(List<String> list) {
		int[] array = new int[list.size()];
		for (int i = 0; i < list.size(); i++) {
			array[i] = Integer.parseInt(list.get(i));
		}
		return array;
	}

	@SuppressWarnings("rawtypes")
	public static TestInterpreter getTestInterpreter(XMLConfiguration config,
			String section) throws SecurityException, ClassNotFoundException,
			IllegalArgumentException, InstantiationException,
			IllegalAccessException, InvocationTargetException {
		String testResultsInterpreterClass = config.getString(section
				+ ".testInterpreter.class");
		Constructor[] a = Class.forName(testResultsInterpreterClass)
				.getConstructors();
		TestInterpreter tri = (TestInterpreter) a[0].newInstance();
		tri.configTestInterpreter(config, section + ".testInterpreter");
		return tri;
	}
	
	/**
	 * Returns object of given type. Constructor should be parameter-less.
	 * @param className
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static Object getInstance(String className) {
		try {
			Class c = Class.forName(className);
			for (Constructor cons : c.getConstructors()) {
				if (cons.getParameterTypes() == null
						|| cons.getParameterTypes().length == 0) {
					return cons.newInstance();
				}
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Transforms given object to Double
	 * @param number
	 * @return
	 */
	public static double objectToDouble(Object number) {
		if (number instanceof Double)
			return (Double) number;
		if (number instanceof Number)
			((Number) number).doubleValue();
		if (number instanceof BigDecimal)
			return ((BigDecimal) number).doubleValue();
		if (number instanceof Integer)
			return (Integer) number;
		if (number instanceof Float)
			return (Float) number;
		try{
			String s=number.toString();
			double d = Double.parseDouble(s.replace(',', '.'));
			return d;
		}catch(Exception e){
			try{
				String s=number.toString();
				double d = Double.parseDouble(s);
				return d;
			}catch(Exception e1){				
			}
		}
		throw new RuntimeErrorException(null, "Unable to process "+number.getClass().getCanonicalName()+". Value "+number);
	}

	public static double roundToDecimals(double d, int decimalPlace){
		    // see the Javadoc about why we use a String in the constructor
		    // http://java.sun.com/j2se/1.5.0/docs/api/java/math/BigDecimal.html#BigDecimal(double)
		    try {
		    	BigDecimal bd = new BigDecimal(Double.toString(d));
			    bd = bd.setScale(decimalPlace,BigDecimal.ROUND_HALF_UP);
			    return bd.doubleValue();
				
			} catch (Exception e) {
				//System.err.println("d "+d+",place "+decimalPlace);
				//e.printStackTrace();
			}
		    
		    return 0;
		    
	}


	
	public static String getFromConfIfNotNull(Configuration dbConf, String key, String replacement){

		if(dbConf.containsKey(key))
			return dbConf.getString(key);
		else 
			return replacement;
	}
	

	public static int getIntFromConfIfNotNull(Configuration dbConf, String key, int replacement){

		if(dbConf.containsKey(key))
			return dbConf.getInt(key);
		else 
			return replacement;
	}

	public static boolean getBooleanFromConfIfNotNull(Configuration dbConf, String key, boolean replacement){
		if(dbConf.containsKey(key))
			return dbConf.getBoolean(key);
		else 
			return replacement;
	}
	
	

	/**
	 * Load data serialized in a file.
	 * @param fileName
	 * @return
	 */
	public static Object loadDataFromFile(String fileName){
		java.io.File f = new java.io.File(fileName);
		if(f.exists()){
			InputStream file;
			try {
				file = new FileInputStream(fileName);
				InputStream buffer = new BufferedInputStream(file,1000000);
				ObjectInputStream in = new ObjectInputStream(buffer);
				Object o= in.readObject();
				in.close();
				return o;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * Serializes data into a file.
	 * @param fileName
	 * @param o
	 */
	public static void writeDataToFile(String fileName, Object o) {
		try {
			OutputStream file = new FileOutputStream(fileName);
			OutputStream buffer = new BufferedOutputStream(file);
			ObjectOutputStream out = new ObjectOutputStream(buffer);

			out.writeObject(o);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static double getDoubleFromConfIfNotNull(Configuration dbConf, String key, double replacement){

		if(dbConf.containsKey(key))
			return dbConf.getDouble(key);
		else 
			return replacement;
	}
	
	public static <T> List<T> getList(int capacity){
		return new ArrayList<T>(capacity);
	}
	public static <T> List<T> getList(){
		return new ArrayList<T>();
	}
	public static <T> List<List<T>> getListList(){
		return new ArrayList<List<T>>();
	}
}
