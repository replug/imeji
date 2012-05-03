/*
*
* CDDL HEADER START
*
* The contents of this file are subject to the terms of the
* Common Development and Distribution License, Version 1.0 only
* (the "License"). You may not use this file except in compliance
* with the License.
*
* You can obtain a copy of the license at license/ESCIDOC.LICENSE
* or http://www.escidoc.de/license.
* See the License for the specific language governing permissions
* and limitations under the License.
*
* When distributing Covered Code, include this CDDL HEADER in each
* file and include the License file at license/ESCIDOC.LICENSE.
* If applicable, add the following below this CDDL HEADER, with the
* fields enclosed by brackets "[]" replaced with your own identifying
* information: Portions Copyright [yyyy] [name of copyright owner]
*
* CDDL HEADER END
*/

/*
* Copyright 2006-2007 Fachinformationszentrum Karlsruhe Gesellschaft
* für wissenschaftlich-technische Information mbH and Max-Planck-
* Gesellschaft zur Förderung der Wissenschaft e.V.
* All rights reserved. Use is subject to license terms.
*/ 

package de.mpg.imeji.ingest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;

import thewebsemantic.LocalizedString;

import de.mpg.jena.util.MetadataFactory;
import de.mpg.jena.vo.ComplexType.ComplexTypes;
import de.mpg.jena.vo.Image;
import de.mpg.jena.vo.Image.Visibility;
import de.mpg.jena.vo.ImageMetadata;
import de.mpg.jena.vo.MetadataSet;
import de.mpg.jena.vo.Organization;
import de.mpg.jena.vo.Person;
import de.mpg.jena.vo.Properties;
import de.mpg.jena.vo.Properties.Status;
import de.mpg.jena.vo.Statement;
import de.mpg.jena.vo.complextypes.ConePerson;
import de.mpg.jena.vo.complextypes.Text;

/**
 * Class helper to realize the ingest procedures
 * @author hnguyen
 *
 */
public class IngestHelper
{
	/**
	 * XML handling object.
	 */
	private SAXBuilder builder;
	
	/**
	 * Meta data objects in an array list
	 */
	private ArrayList<XmlMDBean> mdObjs;
	
	/**
	 * Hash table of profile names.
	 */
	private Hashtable<String,Integer> profileNames;
	
	/**
	 * Standard constructor
	 */
	public IngestHelper() {
		this.builder = new SAXBuilder();
		this.mdObjs = new ArrayList<XmlMDBean>();
		this.profileNames = new Hashtable<String,Integer>();
	}
	
	
	/**
	 * Returns array list of xml meta data objects.
	 * @return the mdObjs
	 */
	public ArrayList<XmlMDBean> getMdObjs() {
		return mdObjs;
	}

	/**
	 * Returns a hashmap specified various profiles 
	 * position in array list of the meta data object.
	 * @return the profileNames
	 */
	public Hashtable<String, Integer> getProfileNames() {
		return profileNames;
	}
	
	/**
	 * This method gets the meta data from the xml file, 
	 * converted to xml meta data object (XmlMDOject).
	 * @param xmlFilename
	 * @return List of xml meta data objects 
	 */
	public ArrayList<XmlMDBean> extractXmlMDObjects(File xmlFile) {
    	try{
        	
			Document document = (Document) this.builder.build(xmlFile);			
			
			/*
			 * XML conform format for imeji
			 * <CollectionName>
			 * 	<Object-Profile1>
			 * 		<Metadata1>
			 * 			value1 ...
			 * 		</Metadata1>
			 * 		<Metadata2>
			 * 			value2 ...
			 * 		</Metadata2>
			 * 		...
			 * 	</Object-Profile1>
			 * 	<Object-Profile1>
			 * 		<Metadata1>
			 * 			value1 ...
			 * 		</Metadata1>
			 * 		<Metadata2>
			 * 			value2 ...
			 * 		</Metadata2>
			 * 		...
			 * 	</Object-Profile1>
			 *  <Object-Profile2>
			 * 		<Metadata1>
			 * 			value1 ...
			 * 		</Metadata1>
			 * 		<Metadata2>
			 * 			value2 ...
			 * 		</Metadata2>
			 * 		...
			 * 	</Object-Profile2>
			 * 	<Object-Profile2>
			 * 		<Metadata1>
			 * 			value1 ...
			 * 		</Metadata1>
			 * 		<Metadata2>
			 * 			value2 ...
			 * 		</Metadata2>
			 * 		...
			 * 	</Object-Profile2>
			 * 	...
			 * </CollectionName>
			 */
			
			// gets the collection name
			Element rootNode = document.getRootElement();
			
			// gets meta data structure
			@SuppressWarnings("unchecked")
			List<Element> list = rootNode.getChildren();	
			
			// traverses the xml tree (using algorithm deepth first search starting on one site
			for (int i=0; i< list.size(); i++) {
				// gets the meta data
				Element elem = list.get(i);
				XmlMDBean mdo = new XmlMDBean(rootNode.getName(),elem.getName());				
				if(elem.getChildren().isEmpty()) {
					mdo.add(elem.getName(),elem.getValue());
				} else {
					dfs(elem,mdo,"");
				}
				
				// add multiple profile if file contains more than one profile
				if(!this.profileNames.containsKey(elem.getName())) {
					this.profileNames.put(elem.getName(), new Integer(i));
				}
				this.mdObjs.add(mdo);
           }
 
			
    	 }catch(IOException io){
    		System.out.println(io.getMessage());
    	 }catch(JDOMException jdomex){
    		System.out.println(jdomex.getMessage());
    	}
    	 
    	 return this.mdObjs;
	}
	
	/**
	 * This method gets the meta data from the xml file stream, 
	 * converted to xml meta data object (XmlMDOject).
	 * @param xmlFilename
	 * @return List of xml meta data objects 
	 */
	public ArrayList<XmlMDBean> extractXmlMDObjects4Zuse(InputStream xmlFileStream) {
    	try{
        	
			Document document = (Document) this.builder.build(xmlFileStream);			
			
			/*
			 * XML conform format for imeji
			 * <CollectionName>
			 * 	<Object-Profile1>
			 * 		<Metadata1>
			 * 			value1 ...
			 * 		</Metadata1>
			 * 		<Metadata2>
			 * 			value2 ...
			 * 		</Metadata2>
			 * 		...
			 * 	</Object-Profile1>
			 * 	<Object-Profile1>
			 * 		<Metadata1>
			 * 			value1 ...
			 * 		</Metadata1>
			 * 		<Metadata2>
			 * 			value2 ...
			 * 		</Metadata2>
			 * 		...
			 * 	</Object-Profile1>
			 *  <Object-Profile2>
			 * 		<Metadata1>
			 * 			value1 ...
			 * 		</Metadata1>
			 * 		<Metadata2>
			 * 			value2 ...
			 * 		</Metadata2>
			 * 		...
			 * 	</Object-Profile2>
			 * 	<Object-Profile2>
			 * 		<Metadata1>
			 * 			value1 ...
			 * 		</Metadata1>
			 * 		<Metadata2>
			 * 			value2 ...
			 * 		</Metadata2>
			 * 		...
			 * 	</Object-Profile2>
			 * 	...
			 * </CollectionName>
			 */
			
			// gets the collection name
			Element rootNode = document.getRootElement();
			
			// gets meta data structure
			@SuppressWarnings("unchecked")
			List<Element> list = rootNode.getChildren();	
			
			// traverses the xml tree (using algorithm deepth first search starting on one site
			for (int i=0; i< list.size(); i++) {
				// gets the meta data
				Element elem = list.get(i);
				XmlMDBean mdo = new XmlMDBean(rootNode.getName(),elem.getName());				
				if(elem.getChildren().isEmpty()) {
					if(elem.getName().equalsIgnoreCase("Signatur")) {						
						mdo.add(elem.getName(),elem.getValue().replace("/", "_"));
					} else {
						mdo.add(elem.getName(),elem.getValue());
					}
				} else {
					dfs4Zuse(elem,mdo,"");
				}
				
				// add multiple profile if file contains more than one profile
				if(!this.profileNames.containsKey(elem.getName())) {
					this.profileNames.put(elem.getName(), new Integer(i));
				}
				this.mdObjs.add(mdo);
           }
 
			
    	 }catch(IOException io){
    		System.out.println(io.getMessage());
    	 }catch(JDOMException jdomex){
    		System.out.println(jdomex.getMessage());
    	}
    	 
    	 return this.mdObjs;
	}
	
	/**
	 * This method gets the meta data from the xml file stream, 
	 * converted to xml meta data object (XmlMDOject).
	 * @param xmlFilename
	 * @return List of xml meta data objects 
	 */
	public ArrayList<XmlMDBean> extractXmlMDObjects(InputStream xmlFileStream) {
    	try{
        	
			Document document = (Document) this.builder.build(xmlFileStream);			
			
			/*
			 * XML conform format for imeji
			 * <CollectionName>
			 * 	<Object-Profile1>
			 * 		<Metadata1>
			 * 			value1 ...
			 * 		</Metadata1>
			 * 		<Metadata2>
			 * 			value2 ...
			 * 		</Metadata2>
			 * 		...
			 * 	</Object-Profile1>
			 * 	<Object-Profile1>
			 * 		<Metadata1>
			 * 			value1 ...
			 * 		</Metadata1>
			 * 		<Metadata2>
			 * 			value2 ...
			 * 		</Metadata2>
			 * 		...
			 * 	</Object-Profile1>
			 *  <Object-Profile2>
			 * 		<Metadata1>
			 * 			value1 ...
			 * 		</Metadata1>
			 * 		<Metadata2>
			 * 			value2 ...
			 * 		</Metadata2>
			 * 		...
			 * 	</Object-Profile2>
			 * 	<Object-Profile2>
			 * 		<Metadata1>
			 * 			value1 ...
			 * 		</Metadata1>
			 * 		<Metadata2>
			 * 			value2 ...
			 * 		</Metadata2>
			 * 		...
			 * 	</Object-Profile2>
			 * 	...
			 * </CollectionName>
			 */
			
			// gets the collection name
			Element rootNode = document.getRootElement();
			
			// gets meta data structure
			@SuppressWarnings("unchecked")
			List<Element> list = rootNode.getChildren();	
			
			// traverses the xml tree (using algorithm deepth first search starting on one site
			for (int i=0; i< list.size(); i++) {
				// gets the meta data
				Element elem = list.get(i);
				XmlMDBean mdo = new XmlMDBean(rootNode.getName(),elem.getName());				
				if(elem.getChildren().isEmpty()) {
					mdo.add(elem.getName(),elem.getValue());
				} else {
					dfs(elem,mdo,"");
				}
				
				// add multiple profile if file contains more than one profile
				if(!this.profileNames.containsKey(elem.getName())) {
					this.profileNames.put(elem.getName(), new Integer(i));
				}
				this.mdObjs.add(mdo);
           }
 
			
    	 }catch(IOException io){
    		System.out.println(io.getMessage());
    	 }catch(JDOMException jdomex){
    		System.out.println(jdomex.getMessage());
    	}
    	 
    	 return this.mdObjs;
	}

	/**
	 * Using depth first search algorithm starting on the left side of the tree
	 */
	private void dfs(Element elem, XmlMDBean metadatas, String prefix) {
		@SuppressWarnings("unchecked")
		List<Element> list = (List<Element>) elem.getChildren();
		Element subelem;
		String tag, value;
		for (int i = 0; i < list.size(); i++) {
			subelem = list.get(i);
			tag = subelem.getName();			
			if(subelem.getChildren().isEmpty()) {
				value = subelem.getValue();
				if(prefix.isEmpty()) {
					// add meta data
					metadatas.add(tag,value);					
				} else {
					// adds sub meta data, %20 for space key
					metadatas.add(prefix + " - " + tag,value);
				}
			} else {
				// traverses the xml tree and adds sub meta data
				dfs(subelem,metadatas,tag);
			}
		}		
	}
	
	/**
	 * Using depth first search algorithm starting on the left side of the tree
	 */
	private void dfs4Zuse(Element elem, XmlMDBean metadatas, String prefix) {
		@SuppressWarnings("unchecked")
		List<Element> list = (List<Element>) elem.getChildren();
		Element subelem;
		String tag, value;
		for (int i = 0; i < list.size(); i++) {
			subelem = list.get(i);
			tag = subelem.getName();			
			if(subelem.getChildren().isEmpty()) {
				value = subelem.getValue();
				if(prefix.isEmpty()) {
					// add meta data
					if(tag.equalsIgnoreCase("Signatur")) {
						value = value.replace("/", "_");
					}
					metadatas.add(tag,value);					
				} else {
					// adds sub meta data, %20 for space key
					metadatas.add(prefix + " - " + tag,value);
				}
			} else {
				// traverses the xml tree and adds sub meta data
				dfs(subelem,metadatas,tag);
			}
		}		
	}
	
	/**
	 * Specific method only use for the Zuse archive use case!
	 * @param mdbs
	 * @param filename
	 * @return
	 */
	public XmlMDBean getMDBeanObject(ArrayList<XmlMDBean> mdbs, String filename) {				
		String fn = new String("DMA_NL_207_00620.jpg");
		StringBuffer fnsb = new StringBuffer();
		
		for (XmlMDBean xb : mdbs) {
			fn = "DMA_" 
				+ xb.getValueOfTag("Bestand")
				+ "_"
				+ xb.getValueOfTag("Signatur")
				+ ".jpg";
			fn = fn.replace(" ", "_");
//			System.out.println("in: "+fn+" - out:"+filename);
			if(fn.equalsIgnoreCase(filename))
				return xb;
		}		
		return null;
	}
	
	/**
	 * Maps a meta data object entry to image meta data entry
	 * @param mdb
	 * @param statements
	 * @return an image meta data entry as a list of image meta data.
	 */
	public List<ImageMetadata> mappingFromXmlMDObjectToImageMD(XmlMDBean mdb, ArrayList<Statement> statements) {
		List<ImageMetadata> cimd = new ArrayList<ImageMetadata>();
		
		for (Statement statement : statements) {
			ImageMetadata imd = MetadataFactory.newMetadata(statement);
			ArrayList<LocalizedString> labels = (ArrayList<LocalizedString>) statement.getLabels();
			String value = null;
			for (LocalizedString label : labels) {
				value = mdb.getValueOfTag(label.toString());
			}			
						
			((Text)imd).setText(value);
			cimd.add(imd);
		}
		
		return (List<ImageMetadata>)cimd;
	}
	
	/**
	 * Replace the space key value to '%20' for the right URL
	 * @param string
	 * @return
	 */
	public String replaceSpace(String string) {		
		return string.replace(" ", "%20");
	}

	public ArrayList<Image> fetchGenericFromXmlToImageObjectList(String xmlFilename) {
		ArrayList<Image> zo = null;
		File currentDir = new File (".");
		String path = null;
	     
		SAXBuilder builder = null;
    	File xmlFile = null;	
    	    	
 
    	try{
        	path = new String(currentDir.getCanonicalPath());
        	path = path + "\\dump\\";
        	builder = new SAXBuilder();
        	xmlFile = new File(path + xmlFilename);
			Document document = (Document) builder.build(xmlFile);

			// gets the collection name
			Element rootNode = document.getRootElement();
			
			// gets meta data structure
			@SuppressWarnings("unchecked")
			List<Element> list = rootNode.getChildren();		
			
			HashMap<String, String>namespaces = new HashMap<String, String>();
			namespaces.put(rootNode.getNamespaceURI(), rootNode.getNamespacePrefix());
			@SuppressWarnings("unchecked")
			List<Namespace> listNamespace = rootNode.getAdditionalNamespaces();
			
			for (Namespace ns : listNamespace) {				
				namespaces.put(ns.getURI(),ns.getPrefix());
			}
 
			zo = new ArrayList<Image>();
			for (int i=0; i< list.size(); i++) {
				// gets the meta data
				Element elem = list.get(i);
				Image img = new Image();
				if(elem.getChildren().isEmpty()) {
					continue;
				} else {
					fetch(elem,img,"");
				}
				zo.add(img);
            }
			System.out.println("");
			
    	 }catch(IOException io){
    		System.out.println(io.getMessage());
    	 }catch(JDOMException jdomex){
    		System.out.println(jdomex.getMessage());
    	}
    	 
    	return zo;
	}
	
	public ArrayList<Image> fetchGenericFromXmlToImageObjectList(InputStream xmlFileStream) {
		ArrayList<Image> zo = null;
    	try{

			Document document = (Document) this.builder.build(xmlFileStream);
			// gets the collection name
			Element rootNode = document.getRootElement();
			
			// gets meta data structure
			@SuppressWarnings("unchecked")
			List<Element> list = rootNode.getChildren();		
			
			HashMap<String, String>namespaces = new HashMap<String, String>();
			namespaces.put(rootNode.getNamespaceURI(), rootNode.getNamespacePrefix());
			@SuppressWarnings("unchecked")
			List<Namespace> listNamespace = rootNode.getAdditionalNamespaces();
			
			for (Namespace ns : listNamespace) {				
				namespaces.put(ns.getURI(),ns.getPrefix());
			}
 
			zo = new ArrayList<Image>();
			for (int i=0; i< list.size(); i++) {
				// gets the meta data
				Element elem = list.get(i);
				Image img = new Image();
				if(elem.getChildren().isEmpty()) {
					continue;
				} else {
					fetch(elem,img,"");
				}
				zo.add(img);
            }
			System.out.println("");
			
    	 }catch(IOException io){
    		System.out.println(io.getMessage());
    	 }catch(JDOMException jdomex){
    		System.out.println(jdomex.getMessage());
    	}
    	 
    	return zo;
	}

	
	@SuppressWarnings({ "unused", "unchecked" })
    private void fetch(Element elem, Image image, String prefix) {
		// get the id

        List<Attribute> attrs = (List<Attribute>) elem.getAttributes();
		for (Attribute attribute : attrs) {
			String attrValue = attribute.getValue();
			try {
				image.setId(new URL(attrValue).toURI());
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
		
		Properties properties = new Properties();
		List<Element> list = (List<Element>) elem.getChildren();
		for (Element element : list) {
			String switchCase = element.getName();
			if(switchCase.equalsIgnoreCase("properties")){
				List<Element> listProperties = element.getChildren();
				fetchProperties(listProperties,image);
			} else if(switchCase.equalsIgnoreCase("collection")) {
				List<Attribute> attrsColllection = (List<Attribute>) element.getAttributes();
				for (Attribute attribute : attrsColllection) {
					String attrValue = attribute.getValue();
					try {
						image.setCollection(new URL(attrValue).toURI());
					} catch (MalformedURLException e) {
						e.printStackTrace();
					} catch (URISyntaxException e) {
						e.printStackTrace();
					}
				}
			} else if(switchCase.equalsIgnoreCase("escidocId")) {
				String escidocId = element.getValue();
				image.setEscidocId(escidocId);
			} else if(switchCase.equalsIgnoreCase("filename")) {
				String filename = element.getValue();
				image.setFilename(filename);
			} else if(switchCase.equalsIgnoreCase("fullImageUrl")) {
				List<Attribute> attrsFullImageUrl = (List<Attribute>) element.getAttributes();
				for (Attribute attribute : attrsFullImageUrl) {
					String attrValue = attribute.getValue();
					try {
						image.setFullImageUrl(new URL(attrValue).toURI());
					} catch (MalformedURLException e) {
						e.printStackTrace();
					} catch (URISyntaxException e) {
						e.printStackTrace();
					}
				}
			} else if(switchCase.equalsIgnoreCase("metadataSet")) {				
				List<Element> listMetadataSet = element.getChildren();
				fetchMetadataSet(listMetadataSet,image);
			} else if(switchCase.equalsIgnoreCase("thumbnailImageUrl")) {
				List<Attribute> attrsThumbnailImageUrl = (List<Attribute>) element.getAttributes();
				for (Attribute attribute : attrsThumbnailImageUrl) {
					String attrValue = attribute.getValue();
					try {
						image.setThumbnailImageUrl(new URL(attrValue).toURI());
					} catch (MalformedURLException e) {
						e.printStackTrace();
					} catch (URISyntaxException e) {
						e.printStackTrace();
					}
				}
			} else if(switchCase.equalsIgnoreCase("visibility")) {
				List<Attribute> attrsVisibility = (List<Attribute>) element.getAttributes();
				for (Attribute attribute : attrsVisibility) {
					String attrValue = attribute.getValue();
					if(attrValue.contains("visibility/PUBLIC")) {
						image.setVisibility(Visibility.PUBLIC);
					} else if(attrValue.contains("visibility/PRIVATE")) {
						image.setVisibility(Visibility.PRIVATE);
					} else {
						image.setVisibility(Visibility.PRIVATE);
					}
				}
			} else if(switchCase.equalsIgnoreCase("webImageUrl")) {
				List<Attribute> attrsWebImageUrl = (List<Attribute>) element.getAttributes();
				for (Attribute attribute : attrsWebImageUrl) {
					String attrValue = attribute.getValue();
					try {
						image.setWebImageUrl(new URL(attrValue).toURI());
					} catch (MalformedURLException e) {
						e.printStackTrace();
					} catch (URISyntaxException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
    private void fetchProperties(List<Element> listProperties, Image image) {
		Properties properties = new Properties();
		for (Element element : listProperties) {
			String switchCase = element.getName();
			if(switchCase.equalsIgnoreCase("createdBy")) {
                List<Attribute> attrsCreatedBy = (List<Attribute>) element.getAttributes();
				for (Attribute attribute : attrsCreatedBy) {
					String attrValue = attribute.getValue();
					try {
						properties.setCreatedBy(new URL(attrValue).toURI());
					} catch (MalformedURLException e) {
						e.printStackTrace();
					} catch (URISyntaxException e) {
						e.printStackTrace();
					}
				}
			} else if(switchCase.equalsIgnoreCase("creationDate")) {
				
				String dateString = element.getValue();
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'");
				Date creationDate = null;
                // See if we can parse the output of Date.toString()
                try {
                    creationDate = format.parse(dateString);
                }
                catch(ParseException pe) {
                    System.out.println("ERROR: Cannot parse \"" + dateString + "\"");
                }
				
				properties.setCreationDate(creationDate);
			}  else if(switchCase.equalsIgnoreCase("lastModificationDate")) {
                String dateString = element.getValue();
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'");
                Date lastModificationDate = null;
                // See if we can parse the output of Date.toString()
                try {
                    lastModificationDate = format.parse(dateString);
                }
                catch(ParseException pe) {
                    System.out.println("ERROR: Cannot parse \"" + dateString + "\"");
                }
                
                properties.setLastModificationDate(lastModificationDate);
            } else if(switchCase.equalsIgnoreCase("modifiedBy")) {

                List<Attribute> attrsModifiedBy = (List<Attribute>) element.getAttributes();
				for (Attribute attribute : attrsModifiedBy) {
					String attrValue = attribute.getValue();
					try {
						properties.setModifiedBy(new URL(attrValue).toURI());
					} catch (MalformedURLException e) {
						e.printStackTrace();
					} catch (URISyntaxException e) {
						e.printStackTrace();
					}
				}
			} else if(switchCase.equalsIgnoreCase("status")) {

                List<Attribute> attrsSstatus = (List<Attribute>) element.getAttributes();
                for (Attribute attribute : attrsSstatus) {
                    String attrValue = attribute.getValue();
                    if(attrValue.contains("status/PENDING")) {
                        properties.setStatus(Status.PENDING);
                    } else if(attrValue.contains("status/RELEASED")) {
                        properties.setStatus(Status.RELEASED);
                    } else if(attrValue.contains("status/WITHDRAWN")) {
                        properties.setStatus(Status.WITHDRAWN);
                    } else {
                        properties.setStatus(Status.PENDING);
                    }
                }
            } else if(switchCase.equalsIgnoreCase("version")) {
                String version = element.getValue();
                properties.setVersion(Integer.valueOf(version));
            }
			
		}	
		image.setProperties(properties);
	}

	@SuppressWarnings({ "unchecked" })
    private void fetchMetadataSet(List<Element> listMetadataSet, Image image) {
	    MetadataSet metadataSet = new MetadataSet();
	    Collection<ImageMetadata> metadatas = new ArrayList<ImageMetadata>();
	    
        for (Element element : listMetadataSet) {
            String elemValue = element.getName();
            if(elemValue.equalsIgnoreCase("profile")) {
                List<Attribute> attrsProfile = (List<Attribute>) element.getAttributes();
                
                for (Attribute attribute : attrsProfile) {
                    String attrValue = attribute.getValue();
                    try {
                        metadataSet.setProfile(new URL(attrValue).toURI());
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }   
            } else if(elemValue.equalsIgnoreCase("metadata")) {
                ImageMetadata imdt = new ImageMetadata();
                
                List<Element> attrsMetadata = (List<Element>) element.getChildren();                
                
                for (Element elem : attrsMetadata) {
                    String tagName = elem.getName();
                    
                    if(tagName.equalsIgnoreCase("text")) {
                        String textVal = elem.getValue();                        
                        Text text = new Text();
                        text.setText(textVal);
                        imdt = text;                        
                    } else if(tagName.equalsIgnoreCase("ns")) {
                        List<Attribute> attrsNs = (List<Attribute>) elem.getAttributes();
                        for (Attribute attribute : attrsNs) {
                            String attrValue = attribute.getValue();
                            try {
                                imdt.setNamespace((new URL(attrValue)).toURI());                                
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            } catch (URISyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                    } else if(tagName.equalsIgnoreCase("complexTypes")) {
                        List<Attribute> attrsComplexTypes = (List<Attribute>) elem.getAttributes();
                        for (Attribute attribute : attrsComplexTypes) {
                            String attrValue = attribute.getValue();
                            if(attrValue.contains("complexTypes/PERSON")) {
                                imdt.setType(ComplexTypes.PERSON);
                            } else if(attrValue.contains("complexTypes/TEXT")) {
                                imdt.setType(ComplexTypes.TEXT);
                            } else if(attrValue.contains("complexTypes/NUMBER")) {
                                imdt.setType(ComplexTypes.NUMBER);
                            } else if(attrValue.contains("complexTypes/DATE")) {
                                imdt.setType(ComplexTypes.DATE);
                            } else if(attrValue.contains("complexTypes/LICENSE")) {
                                imdt.setType(ComplexTypes.LICENSE);
                            } else if(attrValue.contains("complexTypes/GEOLOCATION")) {
                                imdt.setType(ComplexTypes.GEOLOCATION);
                            } else if(attrValue.contains("complexTypes/URI")) {
                                imdt.setType(ComplexTypes.URI);
                            } else if(attrValue.contains("complexTypes/PUBLICATION")) {
                                imdt.setType(ComplexTypes.PUBLICATION);
                            } else {
                                imdt.setType(ComplexTypes.TEXT);
                            }
                        }
                    } else {
                        List<Element> attrs = (List<Element>) elem.getChildren();
                        switch(imdt.getType()) {
                            case PERSON:
                                imdt = getImageMetadataAsPerson(attrs,imdt.getNamespace());
                                break;
                            case DATE:
                                imdt = getImageMetadataAsDate(attrs,imdt.getNamespace());
                                break;
                            case GEOLOCATION:
                                //TODO: need to implement                                
                                break;
                            case LICENSE:
                                //TODO: need to implement                                
                                break;
                            case NUMBER:
                                imdt = getImageMetadataAsNumber(attrs,imdt.getNamespace());
                                break;
                            case PUBLICATION:
                                //TODO: need to implement                                
                                break;
                            case TEXT:
                                imdt = getImageMetadataAsText(attrs,imdt.getNamespace());
                                break;
                            case URI:
                                //TODO: need to implement
                                break;
                            default:
                                //TODO: need to implement
                                break;
                        }
                    }

                }
                metadatas.add(imdt);
            }
        }
	    
        
        metadataSet.setMetadata(metadatas);
        image.setMetadataSet(metadataSet);
	}


	private ImageMetadata getImageMetadataAsNumber(List<Element> attrs, URI namespace)
    {
		de.mpg.jena.vo.complextypes.Number number = new de.mpg.jena.vo.complextypes.Number();
        number.setNamespace(namespace);
        for (Element attribute : attrs) {
            String numberValue = attribute.getValue();
            number.setNumber(Double.valueOf(numberValue));
        }
        return number;
    }

    private ImageMetadata getImageMetadataAsDate(List<Element> attrs, URI namespace)
    {
        de.mpg.jena.vo.complextypes.Date date = new de.mpg.jena.vo.complextypes.Date();
        date.setNamespace(namespace);
        for (Element attribute : attrs) {            
            date.setDate(attribute.getValue());
        }
        return date;
    }

    private ImageMetadata getImageMetadataAsText(List<Element> attrs, URI namespace)
    {
        Text text = new Text();
        text.setNamespace(namespace);
        return null;
    }

    @SuppressWarnings("unchecked")
    private ImageMetadata getImageMetadataAsPerson(List<Element> attrs, URI namespace)
    {
	    ConePerson cp = new ConePerson();
        Person p = new Person();
        Organization o = new Organization();        

        cp.setNamespace(namespace);        

        for (Element attribute : attrs) {
            String attrValue = attribute.getName();
            if(attrValue.equalsIgnoreCase("alternative-name")) {
                p.setAlternativeName(attribute.getValue());
            } else if(attrValue.equalsIgnoreCase("family-name")) {
                p.setFamilyName(attribute.getValue());
            } else if(attrValue.equalsIgnoreCase("given-name")) {
                p.setGivenName(attribute.getValue());
            } else if(attrValue.equalsIgnoreCase("identifier")) {
                p.setIdentifier(attribute.getValue());
            } else if(attrValue.equalsIgnoreCase("organizationalunit")) {
                List<Element> attrsOrganizationalunit = (List<Element>) attribute.getChildren();
                for (Element orgaElem : attrsOrganizationalunit)
                {
                    String attrOrga = orgaElem.getName();
                    if(attrOrga.equalsIgnoreCase("title")) {
                        o.setName(orgaElem.getValue());
                    } else if(attrOrga.equalsIgnoreCase("city")) {
                        o.setCity(orgaElem.getValue());
                    } else if(attrOrga.equalsIgnoreCase("country")) {
                        o.setCountry(orgaElem.getValue());
                    } else if(attrOrga.equalsIgnoreCase("description")) {
                        o.setDescription(orgaElem.getValue());
                    } else if(attrOrga.equalsIgnoreCase("identifier")) {
                        o.setIdentifier(orgaElem.getValue());
                    } else if(attrOrga.equalsIgnoreCase("pos")) {
                        o.setPos(Integer.valueOf(orgaElem.getValue()));
                    }
                }
                p.getOrganizations().add(o);
            }
        }

        cp.setPerson(p);
        return cp;
    }
}
