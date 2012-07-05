/**
 * 
 */
package ingest.core.beans.metadata;

import java.util.ArrayList;

/**
 * @author hnguyen
 *
 */
public class ProjectObject {
	private final String projectName;
	private ArrayList<MetaDataEntity> metaDataEntities;
	
	public ProjectObject(String projectName) {
		this.projectName = projectName;
	}
	
	/**
	 * @return the projectName
	 */
	public String getProjectName() {
		return projectName;
	}

	/**
	 * @param metaDataEntities the metaDataEntities to set
	 */
	public void setMetaDataEntities(ArrayList<MetaDataEntity> metaDataEntities) {
		this.metaDataEntities = metaDataEntities;
	}

	/**
	 * @return the metaDataEntities
	 */
	public ArrayList<MetaDataEntity> getMetaDataEntities() {
		return metaDataEntities;
	}
}
