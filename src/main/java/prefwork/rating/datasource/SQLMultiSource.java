package prefwork.rating.datasource;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import prefwork.core.Utils;
import prefwork.rating.Rating;

import weka.core.Attribute;


/**
 * SQLDataSource provides connection to arbitrary SQL database. It contains
 * methods for querying for records and users. Every subclass must instantiate
 * provider in its constructor with a database provider.
 * 
 * @author Alan
 * 
 */
public abstract class SQLMultiSource extends SQLDataSource {
	protected static HashMap<Integer, List<Object>>[] maps;
	protected static String section;
	protected String selects[];
	public void configDataSource(XMLConfiguration config, String section, String dataSourceName) {
		super.configDataSource(config, section, dataSourceName);
		Configuration dsConf = config.configurationAt(section+".datasources." + dataSourceName);
		selects = new String[attributes.length];
		/*if (SQLMultiSource.section == null
				|| !section.equals(SQLMultiSource.section)) {
			SQLMultiSource.maps = new HashMap[attributes.length];
			for (int i = 0; i < SQLMultiSource.maps.length; i++)
				SQLMultiSource.maps[i] = new HashMap<Integer, List<Object>>();
		}*/

		if (dsConf.containsKey("attributes")) {
			for (int i = 0; i < attributes.length; i++) {
				Attribute attr = attributes[i];
				if (attr.isRelationValued())
					continue;
				String select = dsConf.getString("attributes.attribute("
						+ attr.index() + ").select");
				if (dsConf.getString("attributes.attribute(" + attr.index()
						+ ").selectAll") != null) {
					String query = Arrays.deepToString(dsConf
							.getStringArray("attributes.attribute("
									+ attr.index() + ").selectAll"));
					// Getting rid of [ and ]
					query = query.substring(1, query.length() - 1);
					initMap(attr, query);
				}
				selects[i] = select;
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void initMap(Attribute attr, String query) {
		PreparedStatement stmt = null;
		ResultSet valuesSet = null;
		if (SQLMultiSource.maps == null)
			SQLMultiSource.maps = new HashMap[attributes.length];

		if (SQLMultiSource.maps[attr.index()] == null){
			SQLMultiSource.maps[attr.index()] = new HashMap<Integer, List<Object>>();
			HashMap<Integer, List<Object>> map = SQLMultiSource.maps[attr.index()];
			try {
				stmt = provider.getConn().prepareStatement(query);
				valuesSet = stmt.executeQuery();
				while (valuesSet.next()) {
					Integer id = Utils
							.objectToInteger(valuesSet.getObject(1));
					if (map.get(id) == null)
						map.put(id, Utils.getList());
					map.get(id).add(valuesSet.getObject(2));
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				Utils.closeStatement(stmt, valuesSet);
			}
		}
	}

	public Rating getRecord() {
		if (!hasNext())
			return null;

		Rating l = new Rating(instances);
		try {
			for (int i = 1; i <= attributes.length; i++) {
				if (attributes[i - 1].isRelationValued()) {/*
					l.add(getAttribute(i - 1, Integer.parseInt(records
							.getObject(2).toString())));*/
					//TODO l.add(Utils.getListFromString(records.getObject(i).toString()));	
					;
				} else
					//TODO l.add(records.getObject(i));
					;
			}
			records.next();
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
		return l;
	}

	public void restart() {
		try {
			clearRecords();

			recordsSelect = "SELECT ";
			for (Attribute attr : attributes) {
				/*if (attr.getType() == Attribute.LIST || attr.getName().isEmpty())
					continue;*/
				recordsSelect += attr.name() + " ,";
			}
			recordsSelect = recordsSelect.substring(0,
					recordsSelect.length() - 1);

			recordsSelect += " FROM " + recordsTable;
			if (betweenCondition != null) {
				recordsSelect += " WHERE " + betweenCondition;
			}
			if (userID != null) {
				// We add where clause, if it is not present
				if (betweenCondition == null
						|| !recordsSelect.endsWith(betweenCondition))
					recordsSelect += " WHERE ";
				// AND if where is already there
				else
					recordsSelect += " AND ";

				recordsSelect += userColumn + " = " + userID;
			}
			recordsStatement = provider.getConn().prepareStatement(
					recordsSelect);
			records = recordsStatement.executeQuery();
			records.next();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public List<Object> getAttribute(Integer index, Integer objectId) {
		PreparedStatement stmt = null;
		ResultSet valuesSet = null;
		if (SQLMultiSource.maps[index] != null && SQLMultiSource.maps[index].containsKey(objectId)) {
			return SQLMultiSource.maps[index].get(objectId);
		}
		else if (SQLMultiSource.maps[index] != null)
			return null;
		try {
			stmt = provider.getConn().prepareStatement(
					selects[index].replace("#ObjectId#",
							objectId.toString()));
			valuesSet = stmt.executeQuery();
			List<Object> values = Utils.getList();
			while (valuesSet.next()) {
				values.add(valuesSet.getObject(1));
			}
			//SQLMultiSource.maps[index].put(objectId, values);
			return values;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			Utils.closeStatement(stmt, valuesSet);
		}
	}

}