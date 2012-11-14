package prefwork.rating.method;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.core.DataSource;
import prefwork.core.Method;
import prefwork.core.PrefWork;
import prefwork.core.UserEval;

public class BehaviourAndContentMethod implements Method {
	Method behaviour = null;	
	Method content = null;
	boolean useBehaviour = true;
	
	public String toString(){
		if(behaviour == null)
			return "null+"+content.toString();
		return behaviour.toString()+"+"+content.toString();
	}
	@Override
	public int buildModel(DataSource trainingDataset, int user) {

		if(useBehaviour)
			return behaviour.buildModel(trainingDataset, user);
		else
			return content.buildModel(trainingDataset, user);
	}
	@Override
	public Object classifyRecord(UserEval record) {

		if(useBehaviour)
			return behaviour.classifyRecord(record);
		else
			return content.classifyRecord(record);
	}

	@Override
	public void configClassifier(XMLConfiguration config, String section) {
		try {
			Configuration methodConf = config.configurationAt(section);
			if(methodConf.containsKey("behaviour.name"))
				behaviour = PrefWork.getMethod(methodConf.getString("behaviour.name"));
			if(methodConf.containsKey("content.name"))		
				content = PrefWork.getMethod(methodConf.getString("content.name"));
		} catch (Exception e) {
			e.printStackTrace();
		} 
		if(behaviour != null)
			behaviour.configClassifier(config, section+".behaviour");
		if(content != null)
			content.configClassifier(config, section+".content");
		
	}
	public Method getBehaviour() {
		return behaviour;
	}
	public void setBehaviour(ContentBased behaviour) {
		this.behaviour = behaviour;
	}
	public Method getContent() {
		return content;
	}
	public void setContent(ContentBased content) {
		this.content = content;
	}
	public boolean isUseBehaviour() {
		return useBehaviour;
	}

	public void useBehaviour() {
		this.useBehaviour = true;
	}
	public void useContent() {
		this.useBehaviour = false;
	}
}
