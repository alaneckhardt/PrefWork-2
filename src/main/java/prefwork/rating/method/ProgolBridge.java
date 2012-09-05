package prefwork.rating.method;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.PropertyConfigurator;

import prefwork.core.Utils;
import prefwork.core.DataSource;
import prefwork.core.UserEval;
import prefwork.rating.Rating;
import prefwork.rating.datasource.ContentDataSource;
import weka.core.Attribute;
import weka.core.Instances;

public class ProgolBridge extends ContentBased {

	String pathExec = "C:\\data\\progs\\ilp\\";
	String binaryName = "progol4.2.exe";
	Process p;
	BufferedReader stdOut;
	BufferedReader stdErr;
	PrintStream stdIn;
	List<Rule> rules = Utils.getList();
	Instances attributes;
	int countTrain = 0;
	int noise = 60;
	boolean monotonize = false;
	double mean;
	String ratingPredicate;
	int classIndex;
	
	public String toString() {
		return "Progol2.0Bridge"+noise+monotonize;
	}

	@SuppressWarnings("unchecked")
	private void getAttributes(DataSource trainingDataset, Integer user,
			BufferedWriter out) {
		Rating rec;
		try {
			double[] classes = ((ContentDataSource) trainingDataset).getClasses();
			Arrays.sort(classes);
			countTrain = 0;
			while ((rec = (Rating) trainingDataset.next()) != null) {
				mean+=Utils.objectToDouble(rec.getRating());
				out.write("object(" + rec.getObjectId() + ").\n");

				// Write out the rating of the object
				if(monotonize){
					for (int i = 0; i < classes.length; i++) {
						if(rec.getRating()<classes[i])
							out.write(":- "+ratingPredicate+"(" + Integer.toString(rec.getObjectId()).toLowerCase() + ", '" + classes[i]
									+ "').\n");
						else
							out.write(ratingPredicate+"(" + Integer.toString(rec.getObjectId()).toLowerCase() + ", '" + classes[i] + "').\n");
						
					}	
				}
				else {
					for (int i = 0; i < classes.length; i++) {
						if(rec.getRating() != classes[i])
							out.write(":- "+ratingPredicate+"(" + Integer.toString(rec.getObjectId()).toLowerCase() + ", '" + classes[i]
									+ "').\n");
						else
							out.write(ratingPredicate+"(" + Integer.toString(rec.getObjectId()).toLowerCase() + ", '" + classes[i] + "').\n");
						
					}	
				}
				
				
				/*for (int i = 1; i <= 5; i++) {
					if (((Number) rec.getRating()).intValue() == i ||
							// If we monotonize, we add all values of ratings below the current as true.
							(monotonize && i <= ((Number) rec.getRating()).intValue())	) {
						out.write(ratingPredicate+"(" + Integer.toString(rec.getObjectId()).toLowerCase() + ", '" + i + "').\n");
					} 
					else {
						out.write(":- "+ratingPredicate+"(" + Integer.toString(rec.getObjectId()).toLowerCase() + ", '" + i
								+ "').\n");
					}

				}*/
				// Write out the attributes of the object
				for (int i = 0; i < rec.getDataset().numAttributes(); i++) {	
					//Do not write class predicate to body predicates
					if(i == classIndex)
						continue;
					String attrName = attributes.attribute(i).name().toLowerCase();					
					attrName = transform(attrName);
					
					if( attributes.attribute(i).isNumeric()){
						out.write( attrName + "(" + Integer.toString(rec.getObjectId())
								+ ", '" + Double.toString(rec.get(i)) + "').\n");
						out.write("const" + i + "('" + transform(Double.toString(rec.get(i))) + "').\n");
					}
					else if (attributes.attribute(i).isString()) {
						out.write( attrName + "(" + transform(Integer.toString(rec.getObjectId()))
								+ ", '" + transform(rec.getRecord().stringValue(i)) + "').\n");
						out.write("const" + i + "('" + transform(rec.getRecord().stringValue(i)) + "').\n");
					} else if (attributes.attribute(i).isRelationValued()) {
						out.write( attrName + "(" + transform(Integer.toString(rec.getObjectId()))
								+ ", '" + transform(rec.getRecord().stringValue(i)) + "').\n");
						out.write("const" + i + "('" + transform(rec.getRecord().stringValue(i)) + "').\n");
						/*List<Object> l = (List<Object>) rec.get(i);
						for (Object o : l) {
							if(o == null)
								continue;
							out.write(attrName + "("
									+ Integer.toString(rec.getObjectId()).toLowerCase() + ", '" + transform(o.toString()) + "').\n");
							out.write("const" + i + "('" + transform(o.toString()) + "').\n");
						}*/
					}
				}
				countTrain++;
			}
			mean/=countTrain;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected static String transform(String in){
		String tmp = in.trim();
		if(tmp.equals("name")){
			tmp = "myname";
		}
		tmp = tmp.replaceAll("'", "").toLowerCase();

		tmp = tmp.replaceAll(" ", "");
		tmp = tmp.replaceAll(".csv", "");
		tmp = tmp.replaceAll("-", "");
		tmp = tmp.replaceAll("\\.", "");
		tmp = tmp.replaceAll("\\\"", "");
		tmp = tmp.replaceAll("\\\\", "");		
		if(tmp.length() > 100)
			tmp = tmp.substring(0,100);
		return tmp;
	}

	
	protected void writeHeader(DataSource trainingDataset, Integer user) {
		BufferedWriter out;
		ContentDataSource dataset = (ContentDataSource)trainingDataset;
		try {
			classIndex = attributes.classIndex();
			ratingPredicate = transform(attributes.attribute(classIndex).name());
			
			out = new BufferedWriter(new FileWriter(pathExec + getFileName(dataset, user) + ".pl"));
			out.write("");

			out.write(":- set(c,5), set(i,5), set(h,50), set(nodes,80000)?\n");
			out.write(":- set(verbose,0)?\n");
			out.write(":- set(noise,"+noise+")?\n");
			/*if(!monotonize)
				out.write(":- set(posonly,1)?\n");*/
			


			// out.write(":- set(posonly)?\n");
			out.write(":- modeh(1,"+ratingPredicate+"(+object,#const2))?\n");
			

			String[] attributeNames = dataset.getAttributesNames();
			//Writing body predicates
			for (int i = 0; i < attributeNames.length; i++) {
				//Do not write class predicate to body predicates
				if(i == classIndex)
					continue;
				String attrName = transform(attributeNames[i]);
				
				if (attributes.attribute(i).isString() || attributes.attribute(i).isNumeric()) {
					out.write(":- modeb(1," + attrName.toLowerCase() + "(+object,#const" + i
							+ "))?\n");
				}else if (attributes.attribute(i).isRelationValued()) {
					out.write(":- modeb(*," + attrName.toLowerCase() + "(+object,#const" + i
							+ "))?\n");
				}
			}
			// Write possible classes
			double[] classes = dataset.getClasses();
			for (int i = 0; i < classes.length; i++) {
				out.write(":- "+ratingPredicate+"('"+ classes[i] + "').\n");
			}
			out.write("\n");
			getAttributes(dataset, user, out);

			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public int buildModel(DataSource trainingDataset, int user) {
		rules = Utils.getList();
		mean = 0;
		attributes = ((ContentDataSource)trainingDataset).getInstances();
		trainingDataset.setFixedUserId(user);
		trainingDataset.restart();
		writeHeader(trainingDataset, user);

		loadILP(trainingDataset, user);
		return countTrain;
	}

	@SuppressWarnings("unused")
	private void writeRules(String inpath){
		BufferedWriter out;
		try {
			out = new BufferedWriter(new FileWriter(inpath + ".rules", true));
			for(Rule r : rules){
				out.write(r.head+" "+Arrays.deepToString(r.body)+"\n");
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private void parseResults(String inpath) {
		BufferedReader in;
		try {
			in = new BufferedReader(new FileReader(inpath));
			String line;
			while ((line = in.readLine()) != null) {
				if (!line.startsWith(ratingPredicate+"("))
					continue;
				Rule rule = new Rule();
				rule.head = line.substring(0, line.indexOf(")") + 1);
				if (rule.head.length() + 4 < line.length()) {
					String body = line.substring(rule.head.length() + 4);
					if (!body.endsWith(".")) {
						line = in.readLine();
						body += line.substring(1);
					}
					body = body.substring(0, body.length() - 1);
					rule.body = body.split(", ");
					rules.add(rule);
				}
			}
 			in.close();
			//writeRules(inpath);
			/*File f = new File(inpath);
			f.delete();*/
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getFileName(DataSource trainingDataset, Integer userId){
		return  transform(trainingDataset.getName()+this.toString()+userId);
	}
	private void loadILP(DataSource trainingDataset, Integer userId) {

		/*
		 * read("CProgol");
		 * 
		 * stdIn.write(("["+trainingDataset.getName()+"]? \n").getBytes());
		 * stdIn.flush(); read("[:- ["+trainingDataset.getName()+"]? - Time
		 * taken"); stdIn.write(("generalise(rating/2)? \n").getBytes());
		 * stdIn.flush(); read("[:- generalise(rating/2)? - Time taken");
		 */
		runProcess(trainingDataset, userId);
		parseResults(pathExec + getFileName(trainingDataset, userId)+".txt");

	}
	private void runProcess(DataSource trainingDataset, Integer userId) {
		try {
			p = Runtime.getRuntime().exec(
					"cmd " 
					, null, new File(pathExec));
			p.getOutputStream().write((binaryName +" "+ getFileName(trainingDataset, userId)
							+ " > "+getFileName(trainingDataset, userId)+".txt 2>>pp.txt\n").getBytes());
			p.getOutputStream().flush();
			p.getOutputStream().write("exit\n".getBytes());
			p.getOutputStream().flush();
			
			p.waitFor();
			stdOut = new BufferedReader(new InputStreamReader(p
					.getInputStream()));
			stdErr = new BufferedReader(new InputStreamReader(p
					.getErrorStream()));
			stdIn = new PrintStream(new BufferedOutputStream(p
					.getOutputStream()), true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected static String getValue(String body, int index) {
		String parameters = body.substring(body.indexOf('('));
		if (index == 0)
			parameters = parameters.substring(0, parameters.indexOf(','));
		else if (index == 1)
			parameters = parameters.substring(parameters.indexOf(',') + 1,
					parameters.length() - 1);
		if (parameters.startsWith("\'"))
			parameters = parameters.substring(1);
		if (parameters.endsWith("\'"))
			parameters = parameters.substring(0, parameters.length() - 1);
		return parameters;
	}

	@SuppressWarnings("unchecked")
	protected static boolean match(Rule r, Rating record, Instances attributes) {
		if (r.body == null || r.body.length == 0) {
			// TODO Find if the id in the rule corresponds with id in record.
			return false;
		}

		for (int i = 0; i < r.body.length; i++) {
			boolean found = false;
			for (int j = 0; j < attributes.numAttributes(); j++) {
				String attrName = transform(attributes.attribute(j).name());
				if (r.body[i].startsWith(attrName)) {

					if( attributes.attribute(j).isNumeric() ){
						if ( Double.toString(record.get(j)).equals(
								getValue(r.body[i], 1))) {
							found = true;
							break;
						}
					}
					else if (attributes.attribute(j).isString()) {
						if (record.getRecord().stringValue(j)!=null && transform(record.getRecord().stringValue(j)).equals(
								getValue(r.body[i], 1))) {
							found = true;
							break;
						}
					}else if (attributes.attribute(j).isRelationValued()) {
							/*List<Object> l = (List<Object>) record.get(j);
							if(l == null)
								continue;
							for (Object o : l) {
								if(o == null)
									continue;
								if (transform(o.toString()).equals(
										getValue(r.body[i], 1))) {
									found = true;
									break;
								}
							}*/
						if (record.getRecord().stringValue(j)!=null && transform(record.getRecord().stringValue(j)).equals(
								getValue(r.body[i], 1))) {
							found = true;
							break;
						}
					}
				}
			}
			// We didn't find the corresponding value for this body element
			if (!found)
				return false;
		}
		return true;
	}


	public Double classifyRecord(UserEval record) {
		double res = 0, max = -1;//
		@SuppressWarnings("unused")
		int count = 0;
		for (Rule r : rules) {
			if (match(r, (Rating)record, attributes)) {
				res =Utils.objectToDouble(getValue(r.head, 1));
				if(!monotonize)
					return  res;
				
				if(res>max)
					max = res;
				//count++;
			}
		}
		if(max!=-1)
			return max;
		return null;
	}	


	public void configClassifier(XMLConfiguration config, String section) {
		Configuration methodConf = config.configurationAt(section);
		if(methodConf.containsKey("noise"))
			noise = methodConf.getInt("noise");
		else
			noise = 0;
		if(methodConf.containsKey("pathToProgol"))
			pathExec = methodConf.getString("pathToProgol");
		if(methodConf.containsKey("monotonize"))
			monotonize = methodConf.getBoolean("monotonize");
	}

	public static void main(String[] args) {
		// BasicConfigurator replaced with PropertyConfigurator.
		PropertyConfigurator.configure("log4j.properties");
		ProgolBridge b = new ProgolBridge();
		//b.classifyRecord(null, 2);

	}
}

class Rule {
	String head;
	String[] body;
}
/*
 * Runtime now = Runtime.getRuntime(); // try { try { String line; /*
 * 
 * p = Runtime.getRuntime().exec("cmd", null, new File(pathExec));
 * BufferedReader stdOut = new BufferedReader(new InputStreamReader(p
 * .getInputStream())); BufferedReader stdErr = new BufferedReader(new
 * InputStreamReader(p .getErrorStream())); PrintStream stdIn = new
 * PrintStream(new BufferedOutputStream(p .getOutputStream()), true); read(
 * "Microsoft Windows"); stdIn.write("C:\\data\\progs\\ilp\\progol4.5.exe >p.txt
 * 2> pp.txt\n".getBytes()); stdIn.flush();
 * 
 * read( "|-"); stdIn.write("[animals]?\n".getBytes()); stdIn.flush(); read(
 * "generalise(class/1?"); stdIn.write("./progol \n".getBytes()); stdIn.flush();
 * read( "|-"); stdIn.write("consult('c:/install/ilp/aleph.pl').\n".getBytes());
 * stdIn.flush(); read( "true."); stdIn.write("read_all(train).\n".getBytes());
 * stdIn.flush(); read( "true"); stdIn.write(".\n".getBytes()); stdIn.flush();
 * read( ""); stdIn.write("induce.\n".getBytes()); stdIn.flush(); read(
 * "true."); stdIn.write("eastbound(west7).\n".getBytes()); stdIn.flush();
 * String res = getResult(stdOut, stdErr); res = "";
 * 
 * /*Process p = Runtime.getRuntime().exec(pathExec + "Progol421.exe", null, new
 * File(pathExec)); BufferedReader stdOut = new BufferedReader(new
 * InputStreamReader(p .getInputStream())); BufferedReader stdErr = new
 * BufferedReader(new InputStreamReader(p .getErrorStream())); PrintStream stdIn =
 * new PrintStream(new BufferedOutputStream(p .getOutputStream()), true);
 * stdIn.write("consult('c:/install/ilp/aleph.pl').\n".getBytes());
 * stdIn.flush(); read( "true."); stdIn.write("read_all(train).\n".getBytes());
 * stdIn.flush(); read( "true"); stdIn.write(".\n".getBytes()); stdIn.flush();
 * read( ""); stdIn.write("induce.\n".getBytes()); stdIn.flush(); read(
 * "true."); stdIn.write("eastbound(west7).\n".getBytes()); stdIn.flush();
 * String res = getResult(stdOut, stdErr); res = "";
 * 
 * 
 * 
 * 
 * Process pp = now .exec("c:\\F\\devel\\ilp\\SWI\\bin\\plcon.exe");
 */
/*
 * Process pp = now .exec("c:\\F\\devel\\ilp\\SWI\\bin\\plcon.exe",
 * "c:\\install\\ilp\\aleph.pl"}); // consult('c:/install/ilp/aleph.pl'). }
 * catch (Exception e) { e.printStackTrace(); } return 0.0D;
 */