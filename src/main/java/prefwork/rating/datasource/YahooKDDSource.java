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
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import au.com.bytecode.opencsv.CSVReader;

import prefwork.core.DataSource;
import prefwork.core.Utils;
import prefwork.rating.Rating;
import prefwork.rating.datasource.YahooKDDSource.Album;
import prefwork.rating.datasource.YahooKDDSource.Artist;
import prefwork.rating.datasource.YahooKDDSource.Genre;
import prefwork.rating.datasource.YahooKDDSource.Track;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

/**
 * Datasource that computes ratings and adds them to the objects obtained by inner datasource.
 * The method of computing ratings is in the configuration confDataSources.xml
 * @author Alan
 *
 */
public class YahooKDDSource extends ContentDataSource {
	int ratingsCount = 200;
	int usersToLoad = 100;
	private static Logger log = Logger.getLogger(YahooKDDSource.class);
	String basePath = "C:\\tmp\\";
	String lastFile = null;
	Attribute[] YahooKDDattributes = null;
	protected Instances innerAttributes;
	Map<Integer, Artist> artists = new HashMap<Integer, Artist>(); 
	Map<Integer, Album> albums = new HashMap<Integer, Album>(); 
	Map<Integer, Genre> genres = new HashMap<Integer, Genre>(); 
	Map<Integer, Track> tracks = new HashMap<Integer, Track>(); 
	

	public String getName(){
		return "YahooKDDSource"+usersToLoad+ratingsCount;
	}

	
	public void configDataSource(XMLConfiguration config, String section, String dataSourceName) {		
		//super.configDataSource(config, section);
		Configuration dbConf = config.configurationAt( section);
		ratingsCount = Utils.getIntFromConfIfNotNull(dbConf, "ratingsCount", ratingsCount);
		basePath = Utils.getFromConfIfNotNull(dbConf, "basePath", basePath);
		usersToLoad = Utils.getIntFromConfIfNotNull(dbConf, "usersToLoad", usersToLoad);

		
		//String dbClass = dbConf.getString("innerclass");

		if(lastFile == null || !lastFile.equals("YahooKDDSource"+getName())){
			loadData();
			lastFile = "YahooKDDSource"+getName();
		}
	}
	

	public Attribute[] getAttributes(){
		
		if(YahooKDDattributes != null)
			return YahooKDDattributes;
		//  Rating, AlbumId, ArtistId, Genres 
		int size = 5;
		Attribute[] attrs = new Attribute[size];
		attrs[0] = new Attribute("Rating", 0);
		attrs[1] = new Attribute("TrackId", (java.util.ArrayList<String>)null, 1);
		attrs[2] = new Attribute("AlbumId", (java.util.ArrayList<String>)null, 2);
		attrs[3] = new Attribute("ArtistId", (java.util.ArrayList<String>)null, 3);
		ArrayList<Attribute> listAttr = new ArrayList<Attribute>();
		listAttr.add(new Attribute("Genres",(java.util.ArrayList<String>)null));
		attrs[4]= new Attribute("Genres", new Instances("Genres", listAttr,10), 4);	
			

		YahooKDDattributes = attrs;
		ArrayList<Attribute> list = new ArrayList<Attribute>();
		for (int i = 0; i < attrs.length; i++) {
			list.add(attrs[i]);
		}
		instances = new Instances("name", list,10);
		instances.setClassIndex(0);
		return attrs;
	}
	
	@SuppressWarnings("unchecked")
	protected void loadDataFromCache(String fileName){
		Long start;
			java.io.File f = new java.io.File(basePath+fileName+".ser");
			if(f.exists()){
				start = System.currentTimeMillis();

				userRecords = (Rating[][]) Utils.loadDataFromFile(basePath+fileName+".ser");
				log.info("casFile" + (System.currentTimeMillis()-start));
			}
			else{

				start = System.currentTimeMillis();
				String path = "C:\\data\\datasets\\Yahoo\\track1\\";
				String albums = "albumData1.txt";
				String tracks = "trackData1.txt";
				String ratings = "trainIdx1.firstLines.txt";
				artists = loadArtists(path + "artistData1.txt");
				genres = loadGenres(path + "genreData1.txt");
				loadDataFromFiles(path, albums, tracks, ratings);
				log.info("casDb" + (System.currentTimeMillis()-start));
				//log.debug("Loaded file:"+fileName);

				start = System.currentTimeMillis();
				/*for (int i = 0; i < userRecords.length; i++) {
					for (int j = 0; j < userRecords[i].length; j++) {
						//userRecords[i][j].setRecord(null);
						userRecords[i][j].getDataset().compactify();
					}
				}*/
				Utils.writeDataToFile(basePath+fileName+".ser", userRecords);
				log.info("casWriteFile" + (System.currentTimeMillis()-start));				
			}
			userCount = userRecords.length;
	}
	private void loadData() {
		currentUser = 0;		
		// Configure the inner datasource
		//TODO instances = innerDataSource.getInstances();
	
		loadDataFromCache("YahooKDDSource"+getName());
		//loadDataFromDb();
	}
	/**
	 * Copies data from innerDataSource to this.records
	 */
	@SuppressWarnings("unchecked")
	private void loadDataFromFiles(String path, String fileAlbums,String fileTracks, String fileRatings) {		
		Map<Integer,Integer[]> albums = loadDataFromFile(path + fileAlbums);
		Map<Integer,Integer[]> tracks = loadDataFromFile(path + fileTracks);
		processAlbums(albums);
		processTracks(albums, tracks);
		Map<Integer,List<Rating>> ratings = loadRatings(path + fileRatings, tracks, albums);
		this.userRecords = new Rating[usersToLoad][];
		userCount = 0;
		
	}
	
	private void processAlbums(Map<Integer,Integer[]> albums){
		for(Integer id : albums.keySet()){
			Integer[] fields = albums.get(id);
			Album a = new Album();
			a.id = id;
			a.artist = artists.get(fields[0]);
			a.genres = new Genre[fields.length-1];
			for (int i = 1; i < fields.length; i++) {
				a.genres[i-1] = genres.get(fields[i]);
				if(a.genres[i-1] != null)
					a.genres[i-1].albums.put(id, a);
			}
			a.tracks = new HashMap<Integer, Track>();
			if(a.artist != null)
				a.artist.albums.put(id, a);
			this.albums.put(id, a);
		}
	}

	private void processTracks(Map<Integer,Integer[]> albums, Map<Integer,Integer[]> tracks){
		for(Integer id : tracks.keySet()){
			Integer[] fields = tracks.get(id);
			Track t = new Track();
			t.id = id;
			t.album = this.albums.get(fields[0]);
			t.artist = artists.get(fields[1]);
			t.genres = new Genre[fields.length-2];
			for (int i = 2; i < fields.length; i++) {
				t.genres[i-2] = genres.get(fields[i]);
				if(t.genres[i-2] != null)
					t.genres[i-2].tracks.put(id, t);
			}
			if(t.album != null)
				t.album.tracks.put(id, t);
			if(t.artist != null)
				t.artist.tracks.put(id, t);
			this.tracks.put(id, t);
		}
	}
	
	private void addGenres(SparseInstance d, Attribute[] attributes, Integer[] fields, int start){
		//Genres
		Instances relationHeader =  new Instances(attributes[4].relation(), fields.length-1);
		for (int k = start; k < fields.length; k++) {
			Instance inst = new weka.core.SparseInstance(1);
			inst.setDataset(relationHeader);
			Utils.addStringValue(fields[k].toString(), inst, relationHeader.attribute(0));
			relationHeader.add(inst);
		}		
		d.setValue(4, attributes[4].addRelation(relationHeader));
	}
	private Map<Integer,List<Rating>> loadRatings(String file, Map<Integer,Integer[]> tracks, Map<Integer,Integer[]> albums) {
		Map<Integer,List<Rating>> data = new HashMap<Integer, List<Rating>>();
		try {
			CSVReader reader = new CSVReader(new FileReader(file), '\t', '\"');			
			String[] nextLine;
			Integer userId = -1, numberOfRatings = -1;			
			while ((nextLine = reader.readNext()) != null) {
				if(nextLine.length==1){
					String[] userData = nextLine[0].split("\\|");
					userId =  Integer.parseInt(userData[0]);
					numberOfRatings =  Integer.parseInt(userData[1]);
					continue;
				}
				
				Integer id;
				Integer[] fields = new Integer[2];
				//ItemId
				if(nextLine[0].equals("None"))
					fields[0] = null;
				else
					fields[0] = Integer.parseInt(nextLine[0]);
				
				//Rating
				if(nextLine[1].equals("None"))
					fields[1]=null;
				else {
					fields[1]=Integer.parseInt(nextLine[1]);


					Rating l = new Rating(instances);				
					l.setUserId(Utils.objectToInteger(userId));
					l.setObjectId(Utils.objectToInteger(fields[0]));
					
					Attribute[] attributes = getAttributes();
					SparseInstance d = new SparseInstance(attributes.length);
					d.setDataset(instances);
					Integer[] trackFields = tracks.get(fields[0]);
					Integer[] albumFields = albums.get(fields[0]);
					if(trackFields == null && albumFields == null)
						continue;
					/*if(trackFields[0] == null || trackFields[1] == null)
						continue;*/
					//TrackId
					if(trackFields != null)
						Utils.addStringValue(fields[0].toString(), d, attributes[1]);
					
					
					//AlbumId
					if(trackFields != null && trackFields[0] != null)
						Utils.addStringValue(trackFields[0].toString(), d, attributes[2]);
					else if(albumFields != null && albumFields[0] != null){
						Utils.addStringValue(albumFields[0].toString(), d, attributes[2]);					
					}
					//ArtistId
					if(trackFields != null && trackFields[1] != null)
						Utils.addStringValue(trackFields[1].toString(), d, attributes[3]);
					else if(albumFields != null && albumFields.length>1 && albumFields[1] != null){
						Utils.addStringValue(albumFields[1].toString(), d, attributes[3]);					
					}
					
					if(albumFields != null)
						addGenres(d, attributes, albumFields, 2);
					else
						addGenres(d, attributes, trackFields, 3);
					l.setRecord(d);
					l.setDataset(instances);
					l.setRating(fields[1]);
					if(data.get(userId)==null)
						data.put(userId, new ArrayList<Rating>());
					data.get(userId).add(l);
				}
			}
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return data;
	}
	
	private Map<Integer,Integer[]> loadDataFromFile(String file) {
		Map<Integer,Integer[]> data = new HashMap<Integer, Integer[]>();
		try {
			CSVReader reader = new CSVReader(new FileReader(file), '|', '\"');			
			String[] nextLine;
			Integer key;
			Integer value;
			while ((nextLine = reader.readNext()) != null) {
				Integer id;
				Integer[] fields = new Integer[nextLine.length-1];
				if(nextLine[0].equals("None"))
					id = null;
				else
					id = Integer.parseInt(nextLine[0]);
				
				
				/*if(nextLine[1].equals("None"))
					fields[0]=null;
				else {
					fields[0]=Integer.parseInt(nextLine[1]);
				}
				if(nextLine[2].equals("None"))
					fields[1]=null;
				else {
					fields[1]=Integer.parseInt(nextLine[2]);
				}*/

				for (int i = 1; i < nextLine.length; i++) {
					if(nextLine[i].equals("None"))
						fields[i-1]=null;
					else {
						fields[i-1]=Integer.parseInt(nextLine[i]);
					}
					
					//fields[i-1]=Integer.parseInt(nextLine[i]);
				}
				data.put(id, fields);
			}
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return data;
	}

	private Map<Integer,Genre> loadGenres(String file) {
		Map<Integer,Genre> data = new HashMap<Integer, Genre>();
		try {
			CSVReader reader = new CSVReader(new FileReader(file), '|', '\"');			
			String[] nextLine;
			Integer id = null;
			while ((nextLine = reader.readNext()) != null) {
				id = Integer.parseInt(nextLine[0]);
				Genre g = new Genre();
				g.id = id;
				g.albums = new HashMap<Integer, Album>();
				g.tracks = new HashMap<Integer, Track>();
				g.artists = new HashMap<Integer, Artist>();	
				data.put(id, g);
			}
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return data;
	}
	
	private Map<Integer,Artist> loadArtists(String file) {
		Map<Integer,Artist> data = new HashMap<Integer, Artist>();
		try {
			CSVReader reader = new CSVReader(new FileReader(file), '|', '\"');			
			String[] nextLine;
			Integer id = null;
			while ((nextLine = reader.readNext()) != null) {
				id = Integer.parseInt(nextLine[0]);
				Artist a = new Artist();
				a.id = id;

				a.albums = new HashMap<Integer, Album>();
				a.tracks = new HashMap<Integer, Track>();
				data.put(id, a);
			}
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return data;
	}

	public class Track{
		Album album;
		Artist artist;
		Genre[] genres;
		int id;
	}

	public class Album{
		int id;
		Artist artist;
		Map<Integer, Track> tracks;
		Genre[] genres;
		
	}

	public class Artist{
		int id;
		Map<Integer, Album> albums;
		Map<Integer, Track> tracks;
	}

	public class Genre{
		int id;
		Map<Integer, Album> albums;
		Map<Integer, Track> tracks;
		Map<Integer, Artist> artists;		
	}
}
