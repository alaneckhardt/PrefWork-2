package prefwork.rating.datasource;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import prefwork.core.Utils;
import prefwork.rating.Rating;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Contains data from IMDb in form of maps. The attributes of the film are
 * returned as Relation attributes, e.g. there is one attribute Director with a
 * list of string values.
 * 
 * @author Alan Eckhardt
 * 
 */
public class IMDbRelation {

	Instances instances;
	Long IMDb = 0L;
	Attribute[] IMDBAttributes = null;
	private static Logger log = Logger.getLogger(IMDbRelation.class);
	String basePath = "D:\\data2\\datasets\\imdb2008mapped\\";
	protected static HashMap<Integer, String> IMDBPlotMovies;
	protected static HashMap<Integer, Integer> IMDBMapFromFlix;
	protected static List<HashMap<Integer, List<Integer>>> IMDBMaps = Utils.getList();
	protected static HashMap<Integer, LaserDisc> IMDBLaserDiscs;

	protected String[] names;
	boolean getPlot = false;
	boolean getLaserDisc = false;
	/*protected boolean[] loadFile = {
			true,//"actorsRel.csv",
			true,//"certificatesRel.csv",
			false,//"IMDBCinematographMovies.csv",
			true,//"color-infoRel.csv",
			false,//"IMDBComposerMovies.csv",
			false,//"IMDBCountryMovies.csv",
			true,//"countriesRel.csv",
			true,//"directorsRel.csv",
			false,//"IMDBDistributorMovies.csv",
			true,//"editorsRel.csv",
			true,//"genresRel.csv",
			true,//"keywordsRel.csv",
			false,//"IMDBLanguageMovies.csv",
			false,//"IMDBLaserDiscMovies.csv",
			false,//"IMDBLocationMovies.csv",
			true,//"producersRel.csv",
			false,//"IMDBProductionDesignerMovies.csv",
			false,//"IMDBSoundMovies.csv",
			false,//"IMDBWriterMovies.csv",
			true //"plotRel.csv"
	};*/
	protected boolean[] loadFile = {
			false,//"actorsRel.csv",
			false,//"certificatesRel.csv",
			false,//"IMDBCinematographMovies.csv",
			false,//"color-infoRel.csv",
			false,//"IMDBComposerMovies.csv",
			false,//"IMDBCountryMovies.csv",
			false,//"countriesRel.csv",
			false,//"directorsRel.csv",
			false,//"IMDBDistributorMovies.csv",
			false,//"editorsRel.csv",
			false,//"genresRel.csv",
			false,//"keywordsRel.csv",
			false,//"IMDBLanguageMovies.csv",
			false,//"IMDBLaserDiscMovies.csv",
			false,//"IMDBLocationMovies.csv",
			false,//"producersRel.csv",
			false,//"IMDBProductionDesignerMovies.csv",
			false,//"IMDBSoundMovies.csv",
			false,//"IMDBWriterMovies.csv",
			false //"plotRel.csv"
	};
	protected String[] files = {
			"actorsRel.csv",
			"certificatesRel.csv",
			"IMDBCinematographMovies.csv",
			"color-infoRel.csv",
			"IMDBComposerMovies.csv",
			"IMDBCountryMovies.csv",
			"countriesRel.csv",
			"directorsRel.csv",
			"IMDBDistributorMovies.csv",
			"editorsRel.csv",
			"genresRel.csv",
			"keywordsRel.csv",
			"IMDBLanguageMovies.csv",
			"IMDBLaserDiscMovies.csv",
			"IMDBLocationMovies.csv",
			"producersRel.csv",
			"IMDBProductionDesignerMovies.csv",
			"IMDBSoundMovies.csv",
			"IMDBWriterMovies.csv",
			"plotRel.csv"};

	public String getName() {
		return "IMDBRel"+IMDBMaps.size()+(getPlot?"Plot":"")+(getLaserDisc?"Laser":"");
	}
	
	@SuppressWarnings("unchecked")
	private void load(String fileName, HashMap<Integer, List<Integer>> map){
		try {
			java.io.File f = new java.io.File(basePath + fileName + ".ser");
			if (f.exists()) {

				InputStream file = new FileInputStream(basePath + fileName
						+ ".ser");
				InputStream buffer = new BufferedInputStream(file);
				ObjectInputStream in = new ObjectInputStream(buffer);
				map.putAll((HashMap<Integer, List<Integer>>) in.readObject());
				in.close();
			} else {

				loadData(fileName, map);
				log.debug("Loaded file:" + fileName);

				try {
					OutputStream file = new FileOutputStream(basePath
							+ fileName + ".ser");
					OutputStream buffer = new BufferedOutputStream(file);
					ObjectOutputStream out = new ObjectOutputStream(buffer);
					out.writeObject(map);
					out.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void restart() {
		IMDb = 0L;		
	}
	
	public IMDbRelation() {

		// We have already loaded the data.
		if (IMDBMaps != null && IMDBMaps.size() > 0)
			return;

		log.info("Start of loading");		
		IMDBPlotMovies = new HashMap<Integer, String>();
		IMDBLaserDiscs = new HashMap<Integer, LaserDisc>();
		IMDBMapFromFlix = new HashMap<Integer, Integer>();

		loadLaserDiscs("laserFiltered.csv", IMDBLaserDiscs);

		for(int i = 0;i< loadFile.length;i++){
			if(loadFile[i] == false)
				continue;
			HashMap<Integer, List<Integer>> map = new HashMap<Integer, List<Integer>>(2000);
			load(files[i], map);
			IMDBMaps.add(map);
		}
		if(loadFile[loadFile.length -1] == true){
			loadPlotData("plotRel.csv","plotValues.csv", IMDBPlotMovies);
		}
		loadMapping();
		
		log.info("End of loading");
	}

	public SparseInstance getMovie(Integer flixMovieId) {
		int size = IMDBMaps.size();
		if(getLaserDisc)
			size += 29;
		if(getPlot)
			size += 1;
		SparseInstance l = new SparseInstance(size);
		l.setDataset(instances);
		Integer imdbId = IMDBMapFromFlix.get(flixMovieId);	
		// IMDB
		for (int i = 0; i < IMDBMaps.size(); i++) {
			if (IMDBAttributes[i].isRelationValued()) {
				List<Integer> list = getIMBDAttribute(i, imdbId);
				if(list == null){
					l.setMissing(i);
					continue;
				}
				Instances relationHeader =  new Instances(instances.attribute(i).relation(), 0);
				for (int k = 0; k < list.size(); k++) {
					Instance inst = new weka.core.SparseInstance(1);
					inst.setDataset(relationHeader);
					Utils.addStringValue(list.get(k).toString(), inst, relationHeader.attribute(0));
					relationHeader.add(inst);
				}		
				l.setValue(i, instances.attribute(i).addRelation(relationHeader));
				//l.relationalValue(i);
			}
			/*TODO - is there any nominal value?
			 * else if(IMDBattributes[i].isNumeric()){
					try {
						l.setValue(i, Utils.objectToDouble(rec[j]));
					} catch (Exception e) {
						r.getRecord().setValue(index, rec[j]);
					}
				}
				else{
					try {
						if(IMDBattributes[i].indexOfValue(rec[j])==-1){
							IMDBattributes[i].addStringValue(rec[j]);
						}
						r.getRecord().setValue(index, rec[j]);
					} catch (Exception e) {
						r.getRecord().setValue(index, rec[j]);
					}
					
				}*/
		}
		// LaserDisc
		if (getLaserDisc) {
			LaserDisc ld = IMDBLaserDiscs.get(imdbId);
			for (int i = 0; i < 29; i++) {
				if (ld == null)
					l.setMissing(i+IMDBMaps.size());
				else{
					Utils.addStringValue(ld.names[i], l, IMDBAttributes[i+IMDBMaps.size()]);
				}
			}
		}
		if (getPlot) {
			Utils.addStringValue(IMDBPlotMovies.get(imdbId), l, IMDBAttributes[IMDBAttributes.length-1]);
		}
		
		return l;
	}
	
	
	public String getPlot(Integer flixMovieId) {
		Integer movieId = IMDBMapFromFlix.get(flixMovieId);
		return IMDBPlotMovies.get(movieId);
		
	}
	public String[] getAttributesNames(){
		if(IMDBAttributes != null)
			getAttributes();
		if(names != null)
			return names;
		names = new String[IMDBAttributes.length];
		for (int i = 0; i < names.length; i++) {
			names[i]=IMDBAttributes[i].name();
		}
		return names;		
	}

	public Attribute[] getAttributes(){
		
		if(IMDBAttributes != null)
			return IMDBAttributes;
		int size = IMDBMaps.size();
		if(getLaserDisc)
			size += 29;
		if(getPlot)
			size += 1;
		Attribute[] attrs = new Attribute[size];
		//IMDB
		for (int i = 0; i < IMDBMaps.size(); i++) {
			FastVector list = new FastVector();
			list.addElement(new Attribute("list",(FastVector)null));
			attrs[i]= new Attribute(files[i], new Instances("list"+(i), list,10), i);	
		}
		//Adding LaserDisc data
		if(getLaserDisc){
			for (int i = 0; i < 29; i++) {
				attrs[IMDBMaps.size() + i] = new Attribute(LaserDisc.names[i], (FastVector)null, IMDBMaps.size() + i);
			}
		}

		//Adding the plot
		if(getPlot){
			attrs[attrs.length-1] = new Attribute("plotRel.csv", (FastVector)null, IMDBMaps.size());

		}
		IMDBAttributes = attrs;
		FastVector list = new FastVector();
		for (int i = 0; i < attrs.length; i++) {
			list.addElement(attrs[i]);
		}
		instances = new Instances("name", list,10);
		instances.setClassIndex(0);
		return attrs;
	}
	
	protected void loadMapping(){
		try {
			CSVReader reader = new CSVReader(
					new FileReader(basePath + "movie_titlesMappedNoNulls.csv"), ';', '\"');

			String[] nextLine;
			Integer key;
			Integer value;
			while ((nextLine = reader.readNext()) != null) {
				key = Integer.parseInt(nextLine[0]);
				value = Integer.parseInt(nextLine[3]);
				IMDBMapFromFlix.put(key, value);
			}
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void loadData(String fileName, HashMap<Integer, List<Integer>> map) {
		try {
			CSVReader reader = new CSVReader(
					new FileReader(basePath + fileName), ';', '\"');

			String[] nextLine;
			Integer key;
			Integer value;
			while ((nextLine = reader.readNext()) != null) {
				key = Integer.parseInt(nextLine[0]);
				value = Integer.parseInt(nextLine[1]);
				List<Integer> list = map.get(key);
				if (list == null) {
					list = Utils.getList();
					map.put(key, list);
				}
				list.add(value);
			}
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private void loadLaserDiscs(String fileName, HashMap<Integer, LaserDisc> map) {
		try {
			CSVReader reader = new CSVReader(
					new FileReader(basePath + fileName), ';', '\"');
			String[] nextLine;
			LaserDisc ld;
			while ((nextLine = reader.readNext()) != null) {
				ld = new LaserDisc();
				ld.load(nextLine);
				map.put(ld.MOVIEID, ld);
			}
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void loadPlotData(String fileName, String fileNamePlots, HashMap<Integer, String> map) {
		try {
			CSVReader reader = new CSVReader(
					new FileReader(basePath + fileNamePlots), ';', '\"');
			String[] nextLine;
			Integer key;
			Integer value;
			String plot;
			HashMap<Integer, String> mapValues = new HashMap<Integer, String>();
			while ((nextLine = reader.readNext()) != null) {
				key = Integer.parseInt(nextLine[0]);
				plot = nextLine[1];
				mapValues.put(key, plot);				
			}
			reader.close();
			reader = new CSVReader(
					new FileReader(basePath + fileName), ';', '\"');
			while ((nextLine = reader.readNext()) != null) {
				key = Integer.parseInt(nextLine[0]);
				value = Integer.parseInt(nextLine[1]);
				map.put(key, mapValues.get(value));
			}
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void configDataSource(XMLConfiguration config, String section) {
		//super.configDataSource(config, section);
		Configuration dsConf = config.configurationAt(section);
		
		if(dsConf.containsKey("getLaserDisc")){
			getLaserDisc = dsConf.getBoolean("getLaserDisc");
		}
		if(dsConf.containsKey("getPlot")){
			getPlot = dsConf.getBoolean("getPlot");
		}
		getAttributes();
	}
	
	public List<Integer> getIMBDAttribute(Integer attributeId, Integer movie) {
		if (IMDBMaps.get(attributeId) == null)
			return null;
		return IMDBMaps.get(attributeId).get(movie);
	}
}

class LaserDisc{
	int MOVIEID;
	String LN;
	String LB;
	String CN;
	String LT;
	String OT;
	String PC;
	int YR;
	String CF;
	String CA;
	String GR;
	String LA;
	String SU;
	int LE;
	String RD;
	String ST;
	String PR;
	String QP;
	String CC;
	String PF;
	String DF;
	int SI;
	String MF;
	String AR;
	String AL;
	String DS;
	String SE;
	String CO;
	String VS;
	String RC;
	String[] line;
	static String[] names = {
		"LN.csv",
		"LB.csv",
		"CN.csv",
		"LT.csv",
		"OT.csv",
		"PC.csv",
		"YR.csv",
		"CF.csv",
		"CA.csv",
		"GR.csv",
		"LA.csv",
		"SU.csv",
		"LE.csv",
		"RD.csv",
		"ST.csv",
		"PR.csv",
		"QP.csv",
		"CC.csv",
		"PF.csv",
		"DF.csv",
		"SI.csv",
		"MF.csv",
		"AR.csv",
		"AL.csv",
		"DS.csv",
		"SE.csv",
		"CO.csv",
		"VS.csv",
		"RC.csv"};
	public void load(String[] line){
		this.line = line; 
		 MOVIEID=Utils.objectToInteger(line[0]);
		 LN=line[1];
		 LB=line[2];
		 CN=line[3];
		 LT=line[4];
		 OT=line[5];
		 PC=line[6];
		 YR=Utils.objectToInteger(line[7]);
		 CF=line[8];
		 CA=line[9];
		 GR=line[10];
		 LA=line[11];
		 SU=line[12];
		 LE=Utils.objectToInteger(line[13]);
		 RD=line[14];
		 ST=line[15];
		 PR=line[16];
		 QP=line[17];
		 CC=line[18];
		 PF=line[19];
		 DF=line[20];
		 SI=Utils.objectToInteger(line[21]);
		 MF=line[22];
		 AR=line[23];
		 AL=line[24];
		 DS=line[25];
		 SE=line[26];
		 CO=line[27];
		 VS=line[28];
		 RC=line[29];
	}
}
