package prefwork.rating.method;

import org.apache.commons.configuration.XMLConfiguration;

import prefwork.core.DataSource;
import prefwork.core.Method;
import prefwork.core.UserEval;

public class BehaviourAndContentMethod implements Method {
	ContentBased behaviour;	
	public ContentBased getBehaviour() {
		return behaviour;
	}
	public void setBehaviour(ContentBased behaviour) {
		this.behaviour = behaviour;
	}
	public ContentBased getContent() {
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
	ContentBased content;
	boolean useBehaviour = true;
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
			behaviour.configClassifier(config, section+".behaviour");
			content.configClassifier(config, section+".content");
		
	}
}
