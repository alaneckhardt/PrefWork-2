package prefwork.core;

import org.apache.commons.configuration.XMLConfiguration;

public interface Test {

	void test(Method method, DataSource trainDataSource);

	void configTest(XMLConfiguration confRuns, String string);

	Object getResultsInterpreter();

}
