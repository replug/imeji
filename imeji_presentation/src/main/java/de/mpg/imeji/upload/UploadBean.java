/**
 * License: src/main/resources/license/escidoc.license
 */

package de.mpg.imeji.upload;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;

import de.escidoc.core.resources.om.item.Item;
import de.mpg.imeji.beans.SessionBean;
import de.mpg.imeji.collection.CollectionBean;
import de.mpg.imeji.collection.CollectionSessionBean;
import de.mpg.imeji.collection.ViewCollectionBean;
import de.mpg.imeji.escidoc.EscidocHelper;
import de.mpg.imeji.upload.deposit.DepositController;
import de.mpg.imeji.util.BeanHelper;
import de.mpg.imeji.util.LoginHelper;
import de.mpg.imeji.util.PropertyReader;
import de.mpg.imeji.util.UrlHelper;
import de.mpg.jena.controller.UserController;
import de.mpg.jena.vo.CollectionImeji;
import de.mpg.jena.vo.MetadataProfile;
import de.mpg.jena.vo.User;

public class UploadBean
{
	
	private static Logger logger = Logger.getLogger(CollectionBean.class);
	
	private CollectionImeji collection;
	private SessionBean sessionBean;
	private String id;
	private String escidocContext;
	private String escidocUserHandle;
	private User user;
	private String title;
	private String format;
	private String mimetype;
	private String description;

	private String totalNum ;
	private int sNum;
	private int mdsNum;
	private int mdfNum;
	private int fNum;
	private List<String> sFiles;
	private List<String> mdsFiles;
	private List<String> mdfFiles;
	private List<String> fFiles;
	
	private ArrayList<String> ingestedEntries;
	
	private ArrayList<MetadataProfile> mdProfiles;
	private List<SelectItem> profilesMenu;
	
	private String template;
	private CollectionSessionBean collectionSession;
	private DepositController controller;

	private String ingestMessage;
	

	public UploadBean()
	{
		sessionBean = (SessionBean)BeanHelper.getSessionBean(SessionBean.class);
		collectionSession = (CollectionSessionBean)BeanHelper.getSessionBean(CollectionSessionBean.class);
		
		try 
		{
			escidocContext = PropertyReader.getProperty("escidoc.imeji.context.id");
			logInEscidoc();
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}    

	public void status()
	{
		if(UrlHelper.getParameterBoolean("init"))
		{
			loadCollection();
			totalNum = "";
			ingestMessage = "";
			sNum = 0;
			mdsNum = 0;
			mdfNum = 0;
			fNum = 0;
			
			ingestedEntries = new ArrayList<String>();
			sFiles = new ArrayList<String>();
			mdsFiles= new ArrayList<String>();
			mdfFiles= new ArrayList<String>();
			fFiles= new ArrayList<String>();
			
			profilesMenu = new ArrayList<SelectItem>();
			profilesMenu.add(new SelectItem(null,"Select Template"));

		}
		else if (UrlHelper.getParameterBoolean("start"))
		{
			try 
			{
				upload();
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
		else if (UrlHelper.getParameterBoolean("xml"))
		{
			try 
			{
				uploadXML();
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
		else if(UrlHelper.getParameterBoolean("done"))
		{
			try 
			{
				totalNum = UrlHelper.getParameterValue("totalNum");
				report();
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
	}

	public void uploadXML() throws IOException, FileUploadException
	{
		HttpServletRequest req = (HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
		boolean isMultipart = ServletFileUpload.isMultipartContent(req);
		if(isMultipart)
		{  
			ServletFileUpload upload = new ServletFileUpload();
			// Parse the request
			FileItemIterator iter = upload.getItemIterator(req);
			while (iter.hasNext()) 
			{
				FileItemStream item = iter.next();
				
				InputStream stream = item.openStream();
				if (!item.isFormField()) 
				{
					this.title = item.getName();
					StringTokenizer st = new StringTokenizer(this.title, ".");
					
					while (st.hasMoreTokens()) {
						this.format = st.nextToken();
					}
					
					this.mimetype = "application/" + this.format;

					// TODO remove static image description
					this.description = "";
					try
					{
						UserController uc = new UserController(null);
						User user = uc.retrieve(getUser().getEmail());

						try{							
							this.mdProfiles = new ArrayList<MetadataProfile>();
							this.controller = new DepositController();
							
							// uses directly the stream

							ArrayList<ArrayList<String>> mdpl = controller.loadMetadataProfilesUpList(this.collection, user, stream, this.mdProfiles);							
							// if want to save stream as file and handle it as file format
//							File file = controller.createFile(stream, title, mimetype, format);
//							controller.createMetadataProfile(this.collection, user, file);
							

							sNum += 1;
							sFiles.add(title);
							
							mdsNum += mdpl.get(0).size(); // success added meta data profiles
							mdsFiles.addAll(mdpl.get(0));
							for (MetadataProfile mdp : this.mdProfiles) {
								this.profilesMenu.add(new SelectItem(mdp.getId().toString(),mdp.getTitle()));
							}
							
							mdfNum += mdpl.get(1).size(); // fail added meta data profiles
							mdfFiles.addAll(mdpl.get(1));
							
						} catch (Exception e) {
							fNum += 1;
							fFiles.add(title);							
							throw new RuntimeException(e);
						}
					} catch (Exception e)	{
						throw new RuntimeException(e);
					}
				} 
			}
		}
	}

	public void loadProfile() {
		try {			
			
			this.controller.loadMDProfile(this.collectionSession.getActive(), this.getUser());
			logger.info("Profile "+ this.template +"loaded ...");
		} catch (NullPointerException npe) {
			logger.info("Cannot load profile!");
		} catch (IOException e) {			
			e.printStackTrace();
		}
	}
	
	/**
	 * Process to ingest the meta data
	 */
	public void ingest() {
		this.setIngestedEntries(this.controller.ingestMetaData(this.collectionSession.getActive(), this.getUser()));
		
		if(ingestedEntries.isEmpty())
			this.setIngestMessage("Cannot ingest "+this.template+"! Maybe no images or meta data profiles available?");
		else 
			this.setIngestMessage("Profile "+this.template+" ingested ...");
		
		logger.info(getIngestMessage());
	}
		
	/**
	 * @return the ingestMessage
	 */
	public String getIngestMessage() {
		return ingestMessage;
	}

	/**
	 * @param ingestMessage the ingestMessage to set
	 */
	public void setIngestMessage(String ingestMessage) {
		this.ingestMessage = ingestMessage;
	}

	/**
	 * @return the ingestedEntriesSize
	 */
	public int getIngestedEntriesSize() {
		return this.ingestedEntries.size();
	}
	
	/**
	 * @return the ingestedEntries
	 */
	public ArrayList<String> getIngestedEntries() {
		return ingestedEntries;
	}

	/**
	 * @param ingestedEntries the ingestedEntries to set
	 */
	public void setIngestedEntries(ArrayList<String> ingestedEntries) {
		this.ingestedEntries = ingestedEntries;
	}

	/**
	 * Change the template
	 * @return pretty
	 */
	public String changeTemplate() 
	{		
		// To delete 
        return "pretty:";
	}
	
	/**
	 * Event listen to change the template
	 * @param event
	 * @throws Exception
	 */
	public void templateListener(ValueChangeEvent event) throws Exception
    {
        if (event != null && event.getNewValue() != event.getOldValue())
        {
            this.template = event.getNewValue().toString();
            
            for (MetadataProfile mdp : mdProfiles) 
            {
            	if(template.contains(mdp.getId().toString()))
            	{
            		collectionSession.setProfile(mdp);            		
            		break;
            	}
			}
        }
    }	

	/**
	 * @return the template
	 */
	public String getTemplate() {
		return template;
	}

	/**
	 * @param template the template to set
	 */
	public void setTemplate(String template) {
		this.template = template;
	}

	/**
	 * @return the profilesMenu
	 */
	public List<SelectItem> getProfilesMenu() {
		return profilesMenu;
	}
	
	public void upload() throws IOException, FileUploadException
	{
		HttpServletRequest req = (HttpServletRequest)FacesContext.getCurrentInstance().getExternalContext().getRequest();
		boolean isMultipart = ServletFileUpload.isMultipartContent(req);
		if(isMultipart)
		{  
			ServletFileUpload upload = new ServletFileUpload();
			// Parse the request
			FileItemIterator iter = upload.getItemIterator(req);
			while (iter.hasNext()) 
			{
				FileItemStream item = iter.next();
				
				InputStream stream = item.openStream();
				if (!item.isFormField()) 
				{
					title =item.getName();
					StringTokenizer st = new StringTokenizer(title, ".");
					while (st.hasMoreTokens())
					{
						format = st.nextToken();
					}
					mimetype = "image/" + format;

					// TODO remove static image description
					description = "";
					try
					{
						UserController uc = new UserController(null);
						User user = uc.retrieve(getUser().getEmail());
						try
						{
							DepositController controller = new DepositController();
							Item escidocItem = controller.createEscidocItem(stream, title, mimetype, format);
							controller.createImejiImage(collection, user, escidocItem.getOriginObjid(), title
									, URI.create(EscidocHelper.getOriginalResolution(escidocItem))
									, URI.create(EscidocHelper.getThumbnailUrl(escidocItem))
									, URI.create(EscidocHelper.getWebResolutionUrl(escidocItem)));
							sNum += 1;
							sFiles.add(title);
						} 
						catch (Exception e)
						{
							fNum += 1;
							fFiles.add(title);
							throw new RuntimeException(e);
						}
					}
					catch (Exception e)
					{
						throw new RuntimeException(e);
					}
				} 
			}
		}
	}

	public String report() throws Exception{
		setTotalNum(totalNum);
		setsNum(sNum);
		setsFiles(sFiles);
		setfNum(fNum);
		setfFiles(fFiles);
		return "";
	}

	public String getTotalNum() {
		System.err.println("totalNum = " +totalNum);
		return totalNum;
	}

	public void setTotalNum(String totalNum) {
		this.totalNum = totalNum;
	}

	public int getsNum() {
		return sNum;
	}

	public void setsNum(int sNum) {
		this.sNum = sNum;
	}

	public int getfNum() {
		return fNum;
	}

	public void setfNum(int fNum) {
		this.fNum = fNum;
	}

	public List<String> getsFiles() {
		return sFiles;
	}

	public void setsFiles(List<String> sFiles) {
		this.sFiles = sFiles;
	}

	public List<String> getfFiles() {
		return fFiles;
	}

	public void setfFiles(List<String> fFiles) {
		this.fFiles = fFiles;
	}
	
	
	/**
	 * @return the mdsNum
	 */
	public int getMdsNum() {
		return mdsNum;
	}

	/**
	 * @param mdsNum the mdsNum to set
	 */
	public void setMdsNum(int mdsNum) {
		this.mdsNum = mdsNum;
	}

	/**
	 * @return the mdsFiles
	 */
	public List<String> getMdsFiles() {
		return mdsFiles;
	}

	/**
	 * @param mdsFiles the mdsFiles to set
	 */
	public void setMdsFiles(List<String> mdsFiles) {
		this.mdsFiles = mdsFiles;
	}
	
	/**
	 * @return the mdfNum
	 */
	public int getMdfNum() {
		return mdfNum;
	}

	/**
	 * @param mdfNum the mdfNum to set
	 */
	public void setMdfNum(int mdfNum) {
		this.mdfNum = mdfNum;
	}

	/**
	 * @return the mdfFiles
	 */
	public List<String> getMdfFiles() {
		return mdfFiles;
	}

	/**
	 * @param mdfFiles the mdfFiles to set
	 */
	public void setMdfFiles(List<String> mdfFiles) {
		this.mdfFiles = mdfFiles;
	}

	public void loadCollection()
	{
		if (id != null)
		{
			((ViewCollectionBean)BeanHelper.getSessionBean(ViewCollectionBean.class)).setId(id);
			((ViewCollectionBean)BeanHelper.getSessionBean(ViewCollectionBean.class)).init();
			//collection = ObjectLoader.loadCollection(ObjectHelper.getURI(CollectionImeji.class,id), sessionBean.getUser());
			collection = ((ViewCollectionBean)BeanHelper.getSessionBean(ViewCollectionBean.class)).getCollection();
		}
		else
		{
			BeanHelper.error(sessionBean.getLabel("error") + "No ID in URL");
		}
	}

	public void logInEscidoc() throws Exception
	{
		String userName = PropertyReader.getProperty("imeji.escidoc.user");
		String password = PropertyReader.getProperty("imeji.escidoc.password");
		escidocUserHandle = LoginHelper.login(userName, password);
	}

	public CollectionImeji getCollection()
	{
		return collection;
	}

	public void setCollection(CollectionImeji collection)
	{
		this.collection = collection;
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public String getEscidocContext()
	{
		return escidocContext;
	}

	public void setEscidocContext(String escidocContext)
	{
		this.escidocContext = escidocContext;
	}

	public String getEscidocUserHandle()
	{
		return escidocUserHandle;
	}

	public void setEscidocUserHandle(String escidocUserHandle)
	{
		this.escidocUserHandle = escidocUserHandle;
	}

	public User getUser()
	{
		return sessionBean.getUser();
	}

	public void setUser(User user)
	{
		this.user = user;
	}
}
