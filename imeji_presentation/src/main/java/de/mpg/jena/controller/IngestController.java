/**
 * 
 */
package de.mpg.jena.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import de.mpg.imeji.ingest.IngestBean;

import de.mpg.jena.vo.CollectionImeji;
import de.mpg.jena.vo.MetadataProfile;
import de.mpg.jena.vo.User;

/**
 * @author hnguyen
 *
 */
public class IngestController {

	private IngestBean ingestBean;
	
	
	/**
	 * The standard constructor
	 */
	public IngestController() {
		this.ingestBean = new IngestBean();		
	}
	
	public IngestController(CollectionImeji collection, User user) {
		this.ingestBean = new IngestBean(collection,user);
	}

	/**
	 * Creates all meta data profiles from the input file
	 * @param file
	 * @return  a list of all meta data profiles
	 * @deprecated
	 */
	public ArrayList<MetadataProfile> createMetadataProfiles(File file) {
		return this.ingestBean.createMetadataProfiles(file);		
	}
	
	/**
	 * Creates all meta data profiles from the input stream
	 * @param stream, input stream of uploaded file
	 * @return a list of all meta data profiles
	 */
	public ArrayList<MetadataProfile> loadMetadataProfiles(InputStream stream) {
		return this.ingestBean.loadMetadataProfiles(stream);
	}

	/**
	 * Gets all available profile names
	 * @return List of profile names
	 */
	public ArrayList<String> getProfileNames() {
		return this.ingestBean.getProfileNames();
	}
	
	/**
	 * 
	 * @return a list of all meta data profiles which could not created.
	 */
	public ArrayList<String> getFailUPProfiles() {
		return this.ingestBean.getProfileNamesFailUP();
	}
	
	/**
	 * 
	 * @return a list of all meta data profiles which could created.
	 */
	public ArrayList<String> getSuccUPProfiles() {
		return this.ingestBean.getProfileNamesSuccUP();
	}

	public void setProfilesMenu(ArrayList<MetadataProfile> mdProfiles) {
		this.ingestBean.setProfilesMenu(mdProfiles);
		
	}

	public void loadMDProfile() throws IOException {		
		this.ingestBean.load();
	}

	public ArrayList<String> ingestMetaData() {
		return this.ingestBean.ingest();
	}
}
