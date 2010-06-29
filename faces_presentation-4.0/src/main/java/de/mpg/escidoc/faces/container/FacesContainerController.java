package de.mpg.escidoc.faces.container;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import de.escidoc.www.services.om.ContainerHandler;
import de.mpg.escidoc.faces.album.ExportManager;
import de.mpg.escidoc.faces.container.collection.CollectionVO;
import de.mpg.escidoc.services.common.XmlTransforming;
import de.mpg.escidoc.services.common.valueobjects.ContainerVO;
import de.mpg.escidoc.services.common.valueobjects.MetadataSetVO;
import de.mpg.escidoc.services.common.valueobjects.PidTaskParamVO;
import de.mpg.escidoc.services.common.valueobjects.ContainerVO.State;
import de.mpg.escidoc.services.common.valueobjects.publication.MdsPublicationVO;
import de.mpg.escidoc.services.common.xmltransforming.JiBXHelper;
import de.mpg.escidoc.services.framework.PropertyReader;
import de.mpg.escidoc.services.framework.ServiceLocator;

/**
 * Controller for {@link FacesContainerVO} 
 * @author saquet
 *
 */
public class FacesContainerController 
{
	/**
     * The facesContainer controlled.
     */
    private FacesContainerVO facesContainer;
    
    private XmlTransforming xmlTransforming = null;
    private String APPLICATION_URL = null;
    
    /**
     * Default Constructor
     */
    public FacesContainerController()
    {
        try
        {
            // Class initialization
            InitialContext context = new InitialContext();
            xmlTransforming = (XmlTransforming) context.lookup(XmlTransforming.SERVICE_NAME);
            
            facesContainer = new FacesContainerVO();
        }
        catch (Exception e) 
        {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Copy Constructor
     * @param facesContainer
     */
    public FacesContainerController(FacesContainerVO facesContainer)
    {
        try
        {
            // Class initialization
            InitialContext context = new InitialContext();
            xmlTransforming = (XmlTransforming) context.lookup(XmlTransforming.SERVICE_NAME);
            
            // Variable initialization
            this.facesContainer = facesContainer;
            APPLICATION_URL = PropertyReader.getProperty("escidoc.faces.instance.url");
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Retrieve an facesContainer.
     * @param id
     * @param userHandle
     * @return
     * @throws Exception
     */
    public FacesContainerVO retrieve(String id, String userHandle) throws Exception
    {       
           ContainerHandler handler = ServiceLocator.getContainerHandler();
            
            if (userHandle != null)
            {
                handler = ServiceLocator.getContainerHandler(userHandle);
            }
            
            String facesContainerXml= handler.retrieve(id);
            
            ContainerVO containerVo = xmlTransforming.transformToContainer(facesContainerXml);
            
            facesContainer = new FacesContainerVO(containerVo);

        return facesContainer;
    }
    
    /**
     * Release a pending facesContainer.
     * <br> - Step1: assign object pid.
     * <br> - Step2: assign version pid.
     * <br> - Step3: submit facesContainer.
     * <br> - Step4: release facesContainer.
     * @param facesContainer
     * @param userHandle
     * @return
     * @throws Exception
     */
    public FacesContainerVO release(FacesContainerVO facesContainer, String userHandle) throws Exception
    {
        String modificationDate = JiBXHelper.serializeDate(facesContainer.getModificationDate());
        String paramXml = null;
        
        // Assign object pid
        PidTaskParamVO paramAssignation = 
                new PidTaskParamVO(facesContainer.getLatestVersion().getModificationDate()
                                        , ServiceLocator.getFrameworkUrl() 
                                            + "/ir/container/" 
                                            + facesContainer.getLatestVersion().getObjectId());
        
        paramXml = xmlTransforming.transformToPidTaskParam(paramAssignation);
        
        modificationDate = ServiceLocator
            .getContainerHandler(userHandle)
                    .assignObjectPid(facesContainer.getLatestVersion().getObjectId(), paramXml);
        
        facesContainer = retrieve(facesContainer.getVersion().getObjectId(), userHandle);
        
        modificationDate = JiBXHelper.serializeDate(facesContainer.getModificationDate());
        
        // Assign version pid
        PidTaskParamVO paramVersion = 
            new PidTaskParamVO(JiBXHelper.deserializeDate(modificationDate)
                                    , APPLICATION_URL + "facesContainer/" + facesContainer.getVersion().getObjectIdAndVersion());
        
        paramXml = xmlTransforming.transformToPidTaskParam(paramVersion);
        
        modificationDate = ServiceLocator
            .getContainerHandler(userHandle)
                .assignVersionPid( facesContainer.getLatestVersion().getObjectId(), paramXml);
        
        facesContainer = retrieve(facesContainer.getVersion().getObjectId(), userHandle);
        
        modificationDate = JiBXHelper.serializeDate(facesContainer.getModificationDate());
        
        // Submit facesContainer
        paramXml = "<param last-modification-date=\""
            + JiBXHelper.serializeDate(facesContainer.getModificationDate())
            + "\"><comment>submit to publish</comment></param>";

            modificationDate = ServiceLocator.getContainerHandler(userHandle).submit(facesContainer.getVersion().getObjectId(), paramXml);
        
        facesContainer = retrieve(facesContainer.getVersion().getObjectId(), userHandle);
        modificationDate = JiBXHelper.serializeDate(facesContainer.getModificationDate());
        
        // Release the FacesContainer
        paramXml = "<param last-modification-date=\""
                    + modificationDate
                    + "\"><comment>Publication of the facesContainer</comment></param>";
  
        ServiceLocator.getContainerHandler(userHandle).release(
        		facesContainer.getVersion().getObjectId(), paramXml);
        
        return retrieve(facesContainer.getVersion().getObjectId(), userHandle);
    }
    
    /**
     * Create an facesContainer on the FW.
     * @param facesContainer
     * @param userHandle
     * @throws Exception
     */
    public FacesContainerVO create(FacesContainerVO facesContainer, String userHandle) throws Exception
    {
    	String facesContainerXml = xmlTransforming.transformToContainer(transformToContainerVO(facesContainer));
        
        facesContainerXml = ServiceLocator.getContainerHandler(userHandle).create(facesContainerXml);
        
        ContainerVO containerVO = xmlTransforming.transformToContainer(facesContainerXml);
        
        return  new FacesContainerVO(containerVO);
    }
    
    
    /**
     * Edit an facesContainer
     * @param facesContainer : facesContainer to update
     * @param userHandle
     */
    public void edit(FacesContainerVO facesContainer, String userHandle) throws Exception
    {        
        String facesContainerXml = xmlTransforming.transformToContainer(transformToContainerVO(facesContainer));
        
        ServiceLocator
        	.getContainerHandler(userHandle)
        		.update(facesContainer.getLatestVersion().getObjectId()
        				, facesContainerXml);
    }
    
    /**
     * Delete an facesContainer if status is not released or withdraw.
     * @param facesContainer
     * @param userHandle
     * @return true if facesContainer has been deleted
     * @throws Exception 
     */
    public boolean delete(FacesContainerVO facesContainer, String userHandle) throws Exception
    {
        if (State.PENDING.equals(facesContainer.getVersion().getState())
                || State.SUBMITTED.equals(facesContainer.getVersion().getState()))
        {
            removeAllImages(facesContainer, userHandle);
            
            ServiceLocator
                .getContainerHandler(userHandle)
                    .delete(facesContainer.getLatestVersion().getObjectId());
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Withdraw an an facesContainer if its status is released.
     * @param facesContainer
     * @param userHandle
     * @return true if facesContainer has been withdrawn
     */
    public boolean withdraw(FacesContainerVO facesContainer, String message, String userHandle) throws Exception
    {
        if (State.RELEASED.equals(facesContainer.getVersion().getState()))
        {
            String param = "<param last-modification-date=\""
                + JiBXHelper.serializeDate(facesContainer.getLatestVersion().getModificationDate())
                + "\"><comment>" 
                + message
                + "</comment></param>";
        
            ServiceLocator
                .getContainerHandler(userHandle)
                    .withdraw(facesContainer.getLatestVersion().getObjectId(), param);

            return true;
        }
        
        return false;
    }
    
    /**
     *  Submit an facesContainer.
     * The facesContainer must be in state pending.
     * The facesContainer must have PID's, please call the method assignPIDs before.
     */
    public void submit(String userHandle)
    {
        String param = "<param last-modification-date=\""
            + JiBXHelper.serializeDate(facesContainer.getModificationDate())
            + "\"><comment>submit to publish</comment></param>";
        
        try
        {
            ServiceLocator.getContainerHandler(userHandle).submit(
                facesContainer.getVersion().getObjectId(), param);
        }
        catch (Exception e) 
        {
            throw new RuntimeException("Error submitting facesContainer", e);
        }
    }
    
    /**
     * Call Export
     * @param facesContainer
     * @param userHandle
     */
    public void export(FacesContainerVO facesContainer, ExportManager export)
    {
    	try 
    	{
			//export.doExport(facesContainer);
		} 
    	catch (Exception e) 
		{
			throw new RuntimeException(e);
		}
    }
    
    
    /**
     * Adds an item as a member to an facesContainer if this item is not already a member of the facesContainer.
     * <br> The method is synchronized to avoid optimistic lock exception from FW.
     * @param ItemVO 
     * @throws NamingException 
     * @throws IOException 
     */
    public FacesContainerVO addMember(FacesContainerVO facesContainer, String item, String userHandle) throws Exception
    {
        List<String> itemList = Arrays.asList(item);
        return this.addMembers(facesContainer, itemList, userHandle);
    }
    
    /**
     * Adds items as members to an facesContainer if this item is not already a member of the facesContainer.
     * <br> The method is synchronized to avoid optimistic lock exception from FW.
     * @param itemList 
     * @throws IOException 
     */
    public FacesContainerVO addMembers(FacesContainerVO facesContainer, List<String> itemList, String userHandle) throws Exception
    {     
        String param ="<param last-modification-date=\"";
        param += JiBXHelper.serializeDate(facesContainer.getVersion().getModificationDate());
        param += "\">";
        for (int i = 0; i < itemList.size(); i++)
        {
            if (facesContainer.getMembersId()
                    .indexOf(itemList.get(i)) == -1)
            {
                // Create the list of items to be added
                param += "<id>";
                param += itemList.get(i);
                param += "</id>";
            }
        }
        param += "</param>";
        
        // Add the pictures
        ServiceLocator
            .getContainerHandler(userHandle)
                .addMembers(facesContainer.getVersion().getObjectId(), param);
        
        return retrieve(facesContainer.getVersion().getObjectId(), userHandle);
    }
    
    /**
     * Removes an item from an facesContainer
     * <b> The method is synchronized to avoid optimistic lock exception from FW.
     * @param ItemVO 
     * @throws NamingException 
     * @throws IOException 
     */
    public synchronized FacesContainerVO removeMember(FacesContainerVO facesContainer, String item, String userHandle) throws Exception
    {
        List<String> itemList = Arrays.asList(item);
        return removeMembers(facesContainer, itemList, userHandle);
    }
    
    /**
     * Removes a list of items from an facesContainer
     * <br> The method is synchronized to avoid optimistic lock exception from FW.
     * @param List<ItemVO>
     * @throws IOException 
     */
    public synchronized FacesContainerVO removeMembers(FacesContainerVO facesContainer, List<String> itemList, String userHandle) throws Exception
    {
        String param ="<param last-modification-date=\"";
        param += JiBXHelper.serializeDate(facesContainer.getVersion().getModificationDate());
        param += "\">";
        
        for (int i = 0; i < itemList.size(); i++)
        {
            if (facesContainer.getMembersId()
                    .indexOf(itemList.get(i)) != -1)
            {
                param += "<id>";
                param += itemList.get(i);
                param += "</id>";
            }
        }
        param += "</param>";
        
        try
        {
            ServiceLocator
                .getContainerHandler(userHandle)
                     .removeMembers(facesContainer.getLatestVersion().getObjectId(), param);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        
        return retrieve(facesContainer.getVersion().getObjectId(), userHandle);
    }
    
    /**
     * Delete all members of an facesContainer.
     * @param facesContainer
     * @param userHandle
     * @throws Exception
     */
    public void removeAllImages(FacesContainerVO facesContainer, String userHandle) throws Exception
    {
        // Set delete method parameter
        String paramXml = 
                "<param last-modification-date=\"" +
                    JiBXHelper.serializeDate(facesContainer.getLatestVersion().getModificationDate())
                + "\">" +
                "<filter name=\"http://purl.org/dc/elements/1.1/identifier\">";

        for (int i = 0; i <  facesContainer.getMembers().size(); i++)
        {
            paramXml += "<id>" +  facesContainer.getMembersId().get(i) + "</id>";
        }
        
        paramXml += "</filter></param>";
        
        // Remove all the members of the facesContainer
        ServiceLocator
            .getContainerHandler(userHandle)
                .removeMembers(facesContainer.getLatestVersion().getObjectId(), paramXml);
    }
    
    /**
     * Retrieves a list of all images identifiers of the facesContainer.
     * @return List<String>
     */
    public List<String> getImages()
    {
        List<String> images = new ArrayList<String>();
        for (int i = 0; i < this.facesContainer.getMembers().size(); i++)
        {
            images.add(this.facesContainer.getMembers().get(i).getObjectId());
        }        
        return images;
    }
    
    /**
     * Returns the number of images in an facesContainer.
     * @return number of images as int
     */
    public int getFacesContainerSize()
    {
        return this.facesContainer.getMembers().size();
    }
    
    /**
     * Transform a {@link FacesContainerVO} to {@link ContainerVO}
     * <br> Used to instantiate eSciDoc Handlers (which only works with {@link ContainerVO})
     * @param facesContainerVO
     * @return
     */
    public ContainerVO transformToContainerVO(FacesContainerVO facesContainerVO)
    {
    	ContainerVO ct = new ContainerVO(facesContainer);
    	
    	if (ct.getMetadataSets().size() == 0) 
    	{
			ct.getMetadataSets().add(new MetadataSetVO());
		}
    	
    	ct.getMetadataSets().set(0, new MdsPublicationVO(facesContainer.getMdRecord()));
    	
    	return ct;
    }
}

