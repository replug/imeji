package ingest.core.beans.metadata.terms;

import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.Namespace;


public class DCTerms extends Terms {
	
	public static String DCTERMS = "dcterms";
	public static String DCMITYPE = "dcmitype";
	
	public static Namespace DCTERMS_NAMESPACE = Namespace.getNamespace("dcterms", "http://purl.org/dc/terms");
	
	/**
	 * 
	 * @param termLabel
	 * @param termURI
	 * @param attributes
	 */
	public DCTerms(String termLabel, String termURI, List<Attribute> attributes) {
		super(termLabel, Namespace.getNamespace(DCTerms.DCTERMS, termURI), attributes);
	}

}
