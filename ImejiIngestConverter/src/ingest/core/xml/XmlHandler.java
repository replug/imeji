/**
 * 
 */
package ingest.core.xml;

import ingest.core.beans.metadata.MetaDataEntity;
import ingest.core.beans.metadata.MetadataProfile;
import ingest.core.beans.metadata.ProjectObject;
import ingest.core.beans.metadata.terms.Terms;
import ingest.core.helper.sorter.ElementSorter;
import ingest.core.zuse.metadata.terms.ZuseDCTerms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.Comment;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;



/**
 * @author hnguyen
 *
 */
public class XmlHandler {
	
	/**
	 * 
	 * @param filename
	 * @return
	 * @throws IOException 
	 * @throws JDOMException 
	 */
	public static ProjectObject getProjectObject(String filename) throws JDOMException, IOException {
		File file = new File(filename);
		return getProjectObject(file);
	}

	
	public static ProjectObject getProjectObject(File file) throws JDOMException, IOException {
		FileInputStream fis = new FileInputStream(file);
		return getProjectObject(fis);
	}
	
	/**
	 * 
	 * @param file
	 * @return
	 * @throws IOException 
	 * @throws JDOMException 
	 */
	public static ProjectObject getProjectObject(FileInputStream fileInputStream) throws JDOMException, IOException {
		ProjectObject po = null;
		SAXBuilder sax = new SAXBuilder();
		
		Document doc = sax.build(fileInputStream);
		
		Element rootNode = doc.getRootElement();
		rootNode.getContent();
		po = new ProjectObject(rootNode.getName());
		po.setMetaDataEntities(getMetadataEntities(rootNode));
			

		return po;
	}
	
	/**
	 * 
	 * @param node
	 * @return
	 */
	private static ArrayList<MetaDataEntity> getMetadataEntities(Element node) {
		
		List<Element> elements = node.getChildren();
		
		ArrayList<MetaDataEntity> metaDataEntities = new ArrayList<MetaDataEntity>(elements.size());
		
		for (Element element : elements) {
			metaDataEntities.add(getMetaDataEntity(element));
		}
		
		return metaDataEntities;
	}

	/**
	 * 
	 * @param profileName
	 * @param profile
	 * @return
	 */
	private static MetaDataEntity getMetaDataEntity(Element element) {
		
		MetadataProfile mdp = new MetadataProfile(element.getName());
		List<Element> datas = element.getChildren();
		Hashtable<String, Element> metadatas = new Hashtable<String, Element>(datas.size());
		String labelName = "";
		for (Element metadata : datas) {
			Element mde = (Element) metadata;
			
			List<Attribute> attrs = mde.getAttributes();
			
			labelName = "";
			
			for (Attribute attribute : attrs) {
				if(attribute.getName().equalsIgnoreCase(MetaDataEntity.LABEL)) {
					labelName = attribute.getValue();
				}
			}
			
			metadatas.put(metadata.getName()+labelName,mde);
		}
		
		mdp.setMetaDatas(metadatas);
		MetaDataEntity metaDataEntity = new MetaDataEntity(mdp);		
		return metaDataEntity;
	}
	
	/**
	 * 
	 * @param fileName
	 * @throws IOException 
	 * @throws JDOMException 
	 */
	public static void createMetadataXmlFile(String fileName, ProjectObject po) throws JDOMException, IOException {
		createMetadataXmlFile(new File(fileName), po);		
	}
	
	/**
	 * 
	 * @param fileName
	 * @param projectName
	 * @param metadataProfile
	 * @param namespaces
	 * @throws JDOMException
	 * @throws IOException
	 */
	public static void createMetadataProfileXmlFile(String fileName, String projectName, MetadataProfile metadataProfile, ArrayList<Namespace> namespaces) throws JDOMException, IOException {
		createMetadataProfileXmlFile(new File(fileName), projectName, metadataProfile, namespaces);		
	}
	
	public static void createMetadataProfileXmlFile(File file, String projectName, MetadataProfile metadataProfile, ArrayList<Namespace> namespaces) throws JDOMException, IOException {
		
		Element root = null;
		if(namespaces.size() > 0) {
			root = new Element(projectName,namespaces.get(0));
			if(namespaces.size() > 1) {
				for (int i = 1; i < namespaces.size(); i++) {
					root.addNamespaceDeclaration(namespaces.get(i));
				}
			}
		} else {
			root = new Element(projectName);
		}

		Element mdProfileElem = new Element(metadataProfile.getMetadataProfileName());
	
		Hashtable<String, Element> metadatas = metadataProfile.getMetaDatas();
		Enumeration<String> mdKeys = metadatas.keys();
		while(mdKeys.hasMoreElements()) {
			String mdKey = mdKeys.nextElement();			
			mdProfileElem.addContent(metadatas.get(mdKey).clone());
			
		}
		root.addContent(mdProfileElem);

		Document doc = new Document(root);
				
		XMLOutputter serializer = new XMLOutputter(Format.getPrettyFormat());		
		FileOutputStream fos = new FileOutputStream(file);
		serializer.output(doc, fos);
		fos.close();
		
	}
	
	public static void createMetadataProfileXmlFile(File file, ArrayList<MetadataProfile> metadataProfiles, String projectName,
			ArrayList<Namespace> namespaces) throws IOException {
		createMetadataProfileXmlFileSorted(new FileOutputStream(file), metadataProfiles, projectName, namespaces);
	}
	
	public static void createMetadataProfileXmlFileNormal(FileOutputStream fileOutputStream, ArrayList<MetadataProfile> metadataProfiles, String projectName,
			ArrayList<Namespace> namespaces) throws IOException {
		Element root = null;
		if(namespaces.size() > 0) {
			root = new Element(projectName,namespaces.get(0));
			if(namespaces.size() > 1) {
				for (int i = 1; i < namespaces.size(); i++) {
					root.addNamespaceDeclaration(namespaces.get(i));
				}
			}
		} else {
			root = new Element(projectName);
		}
		
		

		for (MetadataProfile mdProfile : metadataProfiles) {
			Element mdProfileElem = new Element(mdProfile.getMetadataProfileName());
			
			Hashtable<String, Element> metadatas = mdProfile.getMetaDatas();
			Enumeration<String> mdKeys = metadatas.keys();
			while(mdKeys.hasMoreElements()) {
				String mdKey = mdKeys.nextElement();				
				mdProfileElem.addContent(metadatas.get(mdKey).clone());
			}
			root.addContent(mdProfileElem);
		}
		
		DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy - HH:mm:ss");
		Date date = new Date();
		
		Comment comment = new Comment("Exported from database: "+projectName+" on: "+dateFormat.format(date)+" // (c) Software-Engineering, FU Berlin");

		Document doc = new Document();		
		doc.addContent(comment);
		doc.addContent(root);

		
		XMLOutputter serializer = new XMLOutputter(Format.getPrettyFormat());		
		serializer.output(doc, fileOutputStream);
		fileOutputStream.close();
		
	}

	public static void createMetadataProfileXmlFileSorted(FileOutputStream fileOutputStream, ArrayList<MetadataProfile> metadataProfiles, String projectName,
			ArrayList<Namespace> namespaces) throws IOException {
		Element root = null;
		if(namespaces.size() > 0) {
			root = new Element(projectName,namespaces.get(0));
			if(namespaces.size() > 1) {
				for (int i = 1; i < namespaces.size(); i++) {
					root.addNamespaceDeclaration(namespaces.get(i));
				}
			}
		} else {
			root = new Element(projectName);
		}
		
		

		for (MetadataProfile mdProfile : metadataProfiles) {
			Element mdProfileElem = new Element(mdProfile.getMetadataProfileName());
		
			
			
			Hashtable<String, Element> metadatas = mdProfile.getMetaDatas();
			
			Enumeration<String> mdKeys = metadatas.keys();
			
			ArrayList<Element> elements = new ArrayList<Element>(metadatas.size());
			
			while(mdKeys.hasMoreElements()) {
				String mdKey = mdKeys.nextElement();
				elements.add(metadatas.get(mdKey).clone());
//				mdProfileElem.addContent(metadatas.get(mdKey).clone());
			}
			
			ElementSorter comparator = new ElementSorter();
			java.util.Collections.sort( elements, comparator );
			
			mdProfileElem.addContent(elements);			
			
			root.addContent(mdProfileElem);
			
		}
		
		DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy - HH:mm:ss");
		Date date = new Date();
		
		Comment comment = new Comment("Exported from database: "+projectName+" on: "+dateFormat.format(date)+" // (c) Software-Engineering, FU Berlin");

		Document doc = new Document();		
		doc.addContent(comment);
		doc.addContent(root);

		
		XMLOutputter serializer = new XMLOutputter(Format.getPrettyFormat());		
		serializer.output(doc, fileOutputStream);
		fileOutputStream.close();
		
	}
	
	/**
	 * 
	 * @param file
	 * @param projectName
	 * @param metadataEntities
	 * @param namespaces
	 * @throws JDOMException
	 * @throws IOException
	 */
	public static void createMetadataProfileXmlFile(File file, String projectName, ArrayList<MetaDataEntity> metadataEntities, ArrayList<Namespace> namespaces) throws JDOMException, IOException {
		
		Element root = null;
		if(namespaces.size() > 0) {
			root = new Element(projectName,namespaces.get(0));
			if(namespaces.size() > 1) {
				for (int i = 1; i < namespaces.size(); i++) {
					root.addNamespaceDeclaration(namespaces.get(i));
				}
			}
		} else {
			root = new Element(projectName);
		}

		for (MetaDataEntity mdEntity : metadataEntities) {
			Element mdProfileElem = new Element(mdEntity.getMetaDataProfile().getMetadataProfileName());
			
			Hashtable<String, Element> metadatas = mdEntity.getMetaDataProfile().getMetaDatas();
			Enumeration<String> mdKeys = metadatas.keys();
			while(mdKeys.hasMoreElements()) {
				String mdKey = mdKeys.nextElement();			
				mdProfileElem.addContent(metadatas.get(mdKey).clone());
				
			}
			root.addContent(mdProfileElem);
		}

		Document doc = new Document(root);
				
		XMLOutputter serializer = new XMLOutputter(Format.getPrettyFormat());		
		FileOutputStream fos = new FileOutputStream(file);
		serializer.output(doc, fos);
		fos.close();
		
	}
	
	/**
	 * 
	 * @param file
	 * @throws IOException 
	 * @throws JDOMException 
	 */
	public static void createMetadataXmlFile(File file, ProjectObject po) throws JDOMException, IOException {

		Element root = new Element(po.getProjectName(),Terms.RDF_NAMESPACE);	
		root.addNamespaceDeclaration(ZuseDCTerms.DCTERMS_NAMESPACE);
		root.addNamespaceDeclaration(Terms.XSI_NAMESPACE);

		
		ArrayList<MetaDataEntity> metadataEbtries = po.getMetaDataEntities();
		
		for (MetaDataEntity metaDataEntity : metadataEbtries) {
				
			MetadataProfile profile = metaDataEntity.getMetaDataProfile();
			Element mdProfileElem = new Element(profile.getMetadataProfileName());
		
			Hashtable<String, Element> metadatas = profile.getMetaDatas();
			Enumeration<String> mdKeys = metadatas.keys();
			while(mdKeys.hasMoreElements()) {
				String mdKey = mdKeys.nextElement();
				Element metadata = metadatas.get(mdKey);
				if(metadata.getNamespace() == null || metadata.getNamespace().getURI().isEmpty()) {
					mdProfileElem.addContent(metadata.clone());
					
				} else {
					Element md = metadata.clone();
					md.setName(metadata.getName());
					md.setNamespace(ZuseDCTerms.DCTERMS_NAMESPACE);					
					mdProfileElem.addContent(md);
				}
//				mdProfileElem.addContent(metadata.clone());
				
			}
			root.addContent(mdProfileElem);
		}
		
		Document doc = new Document(root);
				
		XMLOutputter serializer = new XMLOutputter(Format.getPrettyFormat());		
		FileOutputStream fos = new FileOutputStream(file);
		serializer.output(doc, fos);
		fos.close();
		
	}


}
