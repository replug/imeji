/**
 * License: src/main/resources/license/escidoc.license
 */

package de.mpg.imeji.upload.deposit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;

import de.escidoc.core.client.Authentication;
import de.escidoc.core.resources.om.item.Item;
import de.mpg.imeji.escidoc.EscidocHelper;
import de.mpg.imeji.util.PropertyReader;
import de.mpg.jena.controller.ImageController;
import de.mpg.jena.controller.IngestController;
import de.mpg.jena.vo.CollectionImeji;
import de.mpg.jena.vo.Image;
import de.mpg.jena.vo.Image.Visibility;
import de.mpg.jena.vo.MetadataProfile;
import de.mpg.jena.vo.Properties.Status;
import de.mpg.jena.vo.User;

/**
 * @author yu
 */
public class DepositController
{
	private IngestController ingestController;
    /**
     * Create the escidoc item with the images as components (for version from 1.3)
     * 
     * @param inputStream
     * @param title
     * @param mimetype
     * @param format
     * @return
     * @throws Exception
     */
    public Item createEscidocItem(InputStream inputStream, String title, String mimetype, String format) throws Exception
    {
    	EscidocHelper escidocHelper = new EscidocHelper();
    	Authentication auth = escidocHelper.login();
    	
    	Item item = escidocHelper.initNewItem(PropertyReader.getProperty("escidoc.imeji.content-model.id")
    			, PropertyReader.getProperty("escidoc.imeji.context.id"));
    	
    	item = escidocHelper.loadFiles(item, inputStream, title, mimetype, format, auth);
    	
    	return escidocHelper.createItem(item, auth);
    }
    
    /**
     * Create the {@link Image} in Jena.
     * 
     * @param collection
     * @param user
     * @param escidocId (if created in eSciDoc)
     * @param title
     * @param fullImageURL
     * @param thumbnailURL
     * @param webURL
     * @return
     * @throws Exception 
     */
    public Image createImejiImage(CollectionImeji collection, User user, String escidocId, String title, URI fullImageURI, URI thumbnailURI, URI webURI) throws Exception
    {
    	ImageController imageController = new ImageController(user);
        Image img = new Image();        
        img.setCollection(collection.getId());
        img.setFullImageUrl(fullImageURI);
        img.setThumbnailImageUrl(thumbnailURI);
        img.setWebImageUrl(webURI);
        img.setVisibility(Visibility.PUBLIC);
        img.setFilename(title);
        
        if (escidocId != null)
        {
        	  img.setEscidocId(escidocId);
        }
        if(collection.getProperties().getStatus() == Status.RELEASED)
        {
        	img.getProperties().setStatus(Status.RELEASED);
        }
        
        imageController.create(img, collection.getId());
        
        return img;
    }
    
    /**
     * Creates a file from input stream
     * @param inputStream
     * @param title
     * @param mimetype
     * @param format
     * @return a file
     */
    public File createFile(InputStream inputStream, String title, String mimetype, String format) {
    	File f = new File(title);
//		InputStream inputStream;
		OutputStream out;		
		byte buf[] = new byte[1024];
		int len;
				
		System.out.println(f.getAbsolutePath());
		try {
//			inputStream = new FileInputStream(title);
			out = new FileOutputStream(f);
			while((len = inputStream.read(buf)) > 0)
				out.write(buf,0,len);
			out.close();
			inputStream.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

//		System.out.println("\nFile is created ...");
		return f;	
    }
    
//    /**
//     * Creates meta data profile from a file
//     * @param collection
//     * @param user
//     * @param file
//     */
//    public void createMetadataProfilesVoid(CollectionImeji collection, User user, File file) {
//    	if(this.ingestController == null)
//    		this.setIngestController(new IngestController());
//    	this.getIngestController().createMetadataProfiles(file);    	
//    }
    
    /**
     * Creates meta data profile from stream
     * @param collection
     * @param user
     * @param input stream
     * @deprecated
     */
    public void loadMetadataProfilesVoid(CollectionImeji collection, User user, InputStream stream) {
    	if(this.ingestController == null)
    		this.setIngestController(new IngestController());
    	this.getIngestController().loadMetadataProfiles(stream);    	
    }
    
    /**
     * Creates meta data profile from stream
     * @param collection
     * @param user
     * @param input stream
     * @return a list of all meta data profile names
     */
    public ArrayList<String> loadMetadataProfilesList(CollectionImeji collection, User user, InputStream stream) {
    	if(this.ingestController == null)
    		this.setIngestController(new IngestController());    	
    	this.getIngestController().loadMetadataProfiles(stream);
    	return this.getIngestController().getProfileNames();
    }
    
    /**
     * Creates meta data profile from stream
     * @param collection
     * @param user
     * @param input stream
     * @param mdProfiles
     * @return a list of a list of all meta data profile names which are added successfully as well as failed
     */
    public ArrayList<ArrayList<String>> loadMetadataProfilesUpList(CollectionImeji collection, User user, InputStream stream, ArrayList<MetadataProfile> mdProfiles) {
    	if(this.ingestController == null)
    		this.setIngestController(new IngestController(collection,user));    	
    	mdProfiles.addAll(this.getIngestController().loadMetadataProfiles(stream));
    	ArrayList<ArrayList<String>> sf = new ArrayList<ArrayList<String>>();
    	sf.add(this.getIngestController().getSuccUPProfiles());
    	sf.add(this.getIngestController().getFailUPProfiles());
    	return sf;
    }

    
    public void loadMDProfile(CollectionImeji collection, User user) throws IOException {
    	if(this.ingestController == null)
    		this.setIngestController(new IngestController(collection,user));
    	this.getIngestController().loadMDProfile();
    }
    
	public void setProfilesMenu(CollectionImeji collection, User user, ArrayList<MetadataProfile> mdProfiles) {
		if(this.ingestController == null)
			this.setIngestController(new IngestController(collection,user));
		this.getIngestController().setProfilesMenu(mdProfiles);
	}

	public ArrayList<String> ingestMetaData(CollectionImeji collection, User user) {
		if(this.ingestController == null)
			this.setIngestController(new IngestController(collection,user));
    	return this.getIngestController().ingestMetaData();
	}

	/**
	 * @param ingestController the ingestController to set
	 */
	public void setIngestController(IngestController ingestController) {
		this.ingestController = ingestController;
	}

	/**
	 * @return the ingestController
	 */
	public IngestController getIngestController() {
		return ingestController;
	}
}
