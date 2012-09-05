package prefwork.rating.method;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.PropertyConfigurator;

import prefwork.core.DataSource;
import prefwork.core.UserEval;
import prefwork.core.Utils;
import prefwork.rating.Rating;
import prefwork.rating.datasource.ContentDataSource;
import utanm.Alternative;
import utanm.BadInputException;
import utanm.UTAADJ;
import utanm.UTATask;
import weka.core.Instances;

public class UtaBridge extends ContentBased {

	protected String pathExec = "C:\\data\\progs\\eclipseProjects2\\PrefWork2.0\\";
	protected Instances attributes;
	protected int countTrain = 0;
	protected double maxClass = 0;
	protected double max = 0.0;
	protected double[][] minmax;
	protected Map<Double, List<Integer>> ratings = new HashMap<Double, List<Integer>>();
	protected UTATask solution;
	protected int segments = 4;
	protected Integer userId;
	protected List<Rating> testSet=Utils.getList();
	protected List<Rating> testSetNoPredict=Utils.getList();
	protected boolean preparedForTesting = false;
	protected double[] classes;
	double mean = 0;
	protected int maxNonmonotonicityDegree = 2;
	protected String directory = "vw";
	public String toString() {
		return "UtaBridgeNoClass"+maxNonmonotonicityDegree;
	}
	
	protected void getMinMax(DataSource trainingDataset){

		Rating rec;
		minmax = new double[attributes.numAttributes()][];
		for (int i = 0; i < attributes.numAttributes(); i++) {
			minmax[i]=new double[2];
			minmax[i][0]=Double.MAX_VALUE;
			minmax[i][1]=Double.MIN_VALUE;
		}			
			while ((rec = (Rating)trainingDataset.next()) != null) {
				for (int i = 0; i < attributes.numAttributes(); i++) {
					if(attributes.attribute(i).isNumeric()){
						double d = Utils.objectToDouble(rec.get(i));
						if(d>minmax[i][1]){
							minmax[i][1]=d;
						}
						if(d<minmax[i][0]){
							minmax[i][0]=d;
						}
					}
				}			
			}
	}

	protected void writeRecord(BufferedWriter out, Rating rec) throws IOException{
		out.write("    <Alternative>\n"+
        "      <Name>"+rec.getObjectId()+"</Name>\n"+
        "      <CriteriaValues>\n");
		Double r = Utils.objectToDouble(rec.getRating());
		if(maxClass<r)
			maxClass = r;
		if(!ratings.containsKey(r)){
			ratings.put(r, new ArrayList<Integer>());
		}
		ratings.get(r).add(rec.getObjectId());
		for (int i = 0; i < attributes.numAttributes(); i++) {//TODO
			if(!processAttribute(i))
				continue;
			String attrName = transform(attributes.attribute(i).name());
			//Numeric
			if(attributes.attribute(i).isNumeric())
				out.write("        <Criterion name=\""+attrName+"\">"+transform(Double.toString(rec.get(i)))+"</Criterion>\n");
			//Nominal
			else	
				out.write("        <Criterion name=\""+attrName+"\">"+transform(rec.getRecord().stringValue(i))+"</Criterion>\n");
			
		}
        out.write("      </CriteriaValues>\n"+
        "    </Alternative>\n");
	}
	private void getAttributes(DataSource trainingDataset, Integer user,
			BufferedWriter out) {
		Rating rec;
		maxClass = 0;
		ratings = new HashMap<Double, List<Integer>>();
		try {
			countTrain = 0;
			out.write("  <Alternatives>\n");
			while ((rec = (Rating)trainingDataset.next()) != null) {
				writeRecord(out, rec);
				countTrain++;
			}
			out.write("  </Alternatives>\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	protected void getPreferences(BufferedWriter out) {
		try {
			double rating = 0;
			out.write("  <StatedPreferences>\n");
			List<Entry> l = new ArrayList<Entry>(ratings.entrySet());
			Collections.sort(l, new EntryComparator());
			for(Entry e : l){
				Double r = Utils.objectToDouble(e.getKey());
				rating+=r;
				int classIndex = 0;
				for (; classIndex < classes.length; classIndex++) {
					if(classes[classIndex]==r){
						break;
					}						
				}
				out.write("  <Rank order=\""+(classes.length-classIndex+1)+"\">\n");
				for(Integer i : ((List<Integer>)e.getValue())){
					out.write("    <Alternative>"+i+"</Alternative>\n");
							
				}
				out.write("  </Rank>\n");
			}
			mean = rating/l.size();
			out.write("  </StatedPreferences>\n");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	protected String transform(String in){
		String tmp = in;
		if(tmp.equals("name")){
			tmp = "myname";
		}
		tmp = tmp.replaceAll("'", "").toLowerCase();

		tmp = tmp.replaceAll(".csv", "");
		//tmp = tmp.replaceAll("-", "");
		//tmp = tmp.replaceAll("\\.", "");
		tmp = tmp.replaceAll("\\\"", "");
		tmp = tmp.replaceAll("Ã©", "");
		tmp = tmp.replaceAll("ã½", "");
		tmp = tmp.replaceAll("\\\\", "");		
		if(tmp.length() > 100)
			tmp = tmp.substring(0,100);
		return tmp;
	}

	
	protected void writeHeader(BufferedWriter out) throws IOException {		
			out.write("");
			out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"+
			    "<UTATASK name=\"vw\">\n"+
			    "<TaskSettings>\n"+
			    "<!-- Used algorithm is not specified here-->\n"+
			    "    <Sigma>0.001</Sigma>\n"+
			    "    <BIGM>1000</BIGM>\n"+
			    "    <INEQ>0.001</INEQ>\n"+
			    "    <AlgorithmSetting type=\"UTAADJ\">\n"+
			    "      <MaxNonmonotonicityDegree>"+/*(attributes.numAttributes()-3)*/maxNonmonotonicityDegree+"</MaxNonmonotonicityDegree>\n"+
			    "      <ObjectiveThreshold>0.02</ObjectiveThreshold>\n"+
			    "      <MinimumImprovementOfObjectiveByAdditionalDegree>0.00</MinimumImprovementOfObjectiveByAdditionalDegree>\n"+
			    "      <MissingValueTreatmentForCardinalAndNominalCriteria>assumeAverageValue</MissingValueTreatmentForCardinalAndNominalCriteria>"+
			    "    </AlgorithmSetting>\n"+
			    "    <!-- Include two forward slashes in the end-->\n"+
			    "    <OutputFolder>"+directory+"//</OutputFolder>\n"+
			    "</TaskSettings>\n"); 
	}
	
	protected int writeCriteria(BufferedWriter out, String[] attributeNames) throws IOException{
		out.write("<Criteria>\n");
		int count = 0;
		for (int i = 0; i < attributeNames.length; i++) {
			String attrName = transform(attributeNames[i]);
			if(attributes.classIndex() == i)
				continue;
			if (attributes.attribute(i).isNumeric()) {	
				if(minmax[i][0] == minmax[i][1])
					continue;
				out.write("  <CardinalCriterion>\n"+
						  "    <Name>"+attrName+"</Name>\n"+					
				          "    <NumberOfSegments>"+Math.max(attributes.attribute(i).numValues()/3,segments)+"</NumberOfSegments>\n"+
				          "    <Shape>Gain</Shape>\n"+
						  "    <Min>"+minmax[i][0]+"</Min> \n"+	
						  "    <Max>"+minmax[i][1]+"</Max> \n"+	
						  "  </CardinalCriterion>\n");
				count++;
			}
	        
			if (attributes.attribute(i).isString() ) {	
				if (attributes.attribute(i).numValues()==1)
					continue;
				out.write("  <NominalCriterion>\n"+
						  "    <Name>"+attrName+"</Name>\n"+
		            	  "    <Values>\n");
				Enumeration e = attributes.attribute(i).enumerateValues();
				while(e.hasMoreElements()){
					Object o = e.nextElement();
					out.write("      <Value>"+transform(o.toString())+"</Value>\n");
				}	
		        out.write("    </Values>\n"+	
					      "  </NominalCriterion>\n");
		        count++;
			}
		}

		out.write("</Criteria>\n");
		return count;
	}
	
	protected String getFileName(String trainingDataset, Integer user){
		return pathExec
		+ transform( trainingDataset+ user +this.hashCode()+".xml");
	}
	
	private void writeTest(){
		BufferedWriter out;
		try {
			out = new BufferedWriter(new FileWriter("C:\\testset"+userId+this.hashCode()+".xml"));
			out.write("<Alternatives>\n");
			for(Rating rec : testSet){
				out.write("    <Alternative>\n"+
	            "      <Name>"+rec.getObjectId()+"</Name>\n"+
	            "      <CriteriaValues>\n");
				Double r = Utils.objectToDouble(rec.get(2));
				if(maxClass<r)
					maxClass = r;
				if(!ratings.containsKey(r)){
					ratings.put(r, new ArrayList<Integer>());
				}
				ratings.get(r).add(Utils.objectToInteger(rec.getObjectId()));
				for (int i = 0; i < attributes.numAttributes(); i++) {
					if(!processAttribute(i))
						continue;
					String attrName = transform(attributes.attribute(i).name());
					out.write("        <Criterion name=\""+attrName+"\">"+transform(Double.toString(rec.get(i)))+"</Criterion>\n");					
				}
	            out.write("      </CriteriaValues>\n"+
	            "    </Alternative>\n");
			}
			out.write("</Alternatives>");
			out.flush();
			out.close();
			
			
			out = new BufferedWriter(new FileWriter("C:\\testsetNoPredict"+userId+".xml"));
			out.write("<Alternatives>\n");
			for(Rating rec : testSetNoPredict){
				out.write("    <Alternative>\n"+
	            "      <Name>"+rec.getObjectId()+"</Name>\n"+
	            "      <CriteriaValues>\n");
				Double r = Utils.objectToDouble(rec.get(2));
				if(maxClass<r)
					maxClass = r;
				if(!ratings.containsKey(r)){
					ratings.put(r, new ArrayList<Integer>());
				}
				ratings.get(r).add(rec.getObjectId());
				for (int i = 0; i < attributes.numAttributes(); i++) {
					if(!processAttribute(i))
						continue;
					String attrName = transform(attributes.attribute(i).name());
					out.write("        <Criterion name=\""+attrName+"\">"+transform(Double.toString(rec.get(i)))+"</Criterion>\n");					
				}
	            out.write("      </CriteriaValues>\n"+
	            "    </Alternative>\n");
			}
			out.write("</Alternatives>");
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void clear(){
		//writeTest();
		solution = null;
		attributes= null;
		maxClass = 0;
		minmax = null;
		ratings = null;
		testSet = Utils.getList();
		testSetNoPredict = Utils.getList();
		preparedForTesting = false;
		mean = 0;
	}
	protected void cleanUp(){
		/*int max = Factory.getMaxTaskID();
	    for (int i = 0; i <= max; i++) {
	        Factory.removeTask(0);			
		}*/
		if(solution == null)
			return ;
	    File folder = new File(directory+"//");

	    for(File f : folder.listFiles())
	    	f.delete();
	    folder.delete();
	    //folder.mkdir();
	}

	public int buildModel(DataSource trainingDataset, int user) {
		//writeTest();
		clear();
		userId = user;
		attributes = ((ContentDataSource)trainingDataset).getInstances();
		classes = ((ContentDataSource)trainingDataset).getClasses();
		Arrays.sort(classes);
		/*System.out.print("build");
	    Attribute[] dAttr = trainingDataset.getAttributes();
		attributes = new Attribute[dAttr.length];
		for (int i = 0; i < attributes.numAttributes(); i++) {
			attributes[i]=dAttr[i].clone();
		}*/
		directory = pathExec+"/vw"+""+this.hashCode();

	    File folder = new File(/*".//"+*/directory+"//");
	    folder.mkdir();
		trainingDataset.setFixedUserId(user);
		trainingDataset.restart();
		BufferedWriter out;
		try {
			out = new BufferedWriter(new FileWriter(getFileName(trainingDataset.getName(), user)));
			writeHeader(out);
			String[] attributeNames = ((ContentDataSource)trainingDataset).getAttributesNames();
			getMinMax(trainingDataset);
			int count = writeCriteria(out, attributeNames);
			//We do not have enough data for all the attributes
			//We need at least two distinct value of preferences.
			if(count == 0){
				out.flush();
				out.close();
				solution = null;
		        System.out.print("end build "+countTrain+" \n");
				return countTrain;				
			}
			trainingDataset.restart();
			getAttributes(trainingDataset, user, out);
			getPreferences(out);

			out.write("</UTATASK>");
		    
		
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	
	
	
        UTAADJ task  = new UTAADJ();
        try {
			solution =task.findBestFittingUTATask(getFileName(trainingDataset.getName(), user));
		} catch (BadInputException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    File f = new File(getFileName(trainingDataset.getName(), user));
	    f.delete();
        cleanUp();
        System.out.print("end build "+countTrain+" \n");
		return countTrain;
	}

	protected boolean processAttribute(int i){
		if(attributes.classIndex() == i)
			return false;
		if (attributes.attribute(i).isNumeric() && minmax[i][0] == minmax[i][1])
				return false;
		if (attributes.attribute(i).isString() && attributes.attribute(i).numValues()==1)
			return false;
		if (!attributes.attribute(i).isNumeric() && !attributes.attribute(i).isString()) {
			return false;
		}
		return true;
	}

	public Double classifyRecord(UserEval record) {
		if(solution == null)
			return mean;
		if(!(record instanceof Rating))
			return mean;
		Rating rec = (Rating)record;
		// create a test alternative
		testSet.add(rec);
        List<String> l = Utils.getList(rec.getRecord().numAttributes());
        for (int i = 0; i < rec.getRecord().numAttributes(); i++) {
        	if(!processAttribute(i))
        		continue;
			if (attributes.attribute(i).isNumeric() ) {
				l.add(transform(Double.toString(rec.get(i))));
			}
			else{
				l.add(transform(rec.getRecord().stringValue(i)));				
			}
			if(Double.isNaN(rec.get(i)))
				return null;
		}
        String criterionValues[] = new String[l.size()];
        criterionValues = l.toArray(criterionValues);

        String testTaskName = Double.toString(rec.get(1));
        if(!preparedForTesting){
        	solution.prepareForTesting();
        	preparedForTesting = true;
        }
        //append it to the model
        Alternative a = null;
        try {
        		a = solution.addTestAlternative(testTaskName, criterionValues);
		} catch (Exception e) {
			e.printStackTrace();
		}
        
        // get alternative utility
		double r = a.getTotalUtility();
		if(Double.isInfinite(r) || Double.isNaN(r)){
			testSetNoPredict.add(rec);
		}
		if(max<r)
			max = r;
        return r*maxClass;
	}



	public static void main(String[] args) {
		// BasicConfigurator replaced with PropertyConfigurator.
		PropertyConfigurator.configure("log4j.properties");
		UtaBridge b = new UtaBridge();
		b.classifyRecord(null);
	}

	@Override
	public void configClassifier(XMLConfiguration config, String section) {
		Configuration methodConf = config.configurationAt(section);
		maxNonmonotonicityDegree = Utils.getIntFromConfIfNotNull(methodConf, "maxNonmonotonicityDegree", maxNonmonotonicityDegree);
	}
}
@SuppressWarnings("unchecked")
class EntryComparator<HashMap$Entry> implements Comparator<Entry>{

	@Override
	public int compare(Entry arg0, Entry arg1) {
		Entry o1=(Entry)arg0;
		Entry o2=(Entry)arg1;
		return -((Double)o1.getKey()).compareTo(((Double)o2.getKey()));
	}
	
}