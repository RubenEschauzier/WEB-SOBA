package seminarOntologyBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import org.json.JSONException;
import edu.eur.absa.Framework;
//import edu.eur.absa.model.exceptions.IllegalSpanException;

//our own classes
import seminarOntologyBuilder.MainOntologyBuilder;
import termSelector.TermSelectionAlgo;

/**
 * The main for the semi-automatic ontology builder.
 * 
 * @author Karoliina Ranta
 * Adapted by Suzanne Veltman
 * Adapted by Lisa Zhuang
 * Adapted by Fenna ten Haaf  
 */
public class MainOntologyBuilder {
	
	public static void main(String[] args) throws ClassNotFoundException, JSONException, IOException, Exception { //IllegalSpanException

		/* RESTAURANT DOMAIN */

		/* Start with the skeletal ontology. */
		SkeletalOntology base = new SkeletalOntology(Framework.EXTERNALDATA_PATH + "RestaurantOntologySeminar6Base_version2_2020.owl"); // Onze is: RestaurantOntologySeminar6Base2020.owl	
		HashMap<String, HashSet<String>> aspectCategories = new HashMap<String, HashSet<String>>();
		
		/*create HashMap aspectCategories that maps each aspect to its relevant categories */
		HashSet<String> restaurant = new HashSet<String>();
		restaurant.add("general");
		restaurant.add("prices");
		restaurant.add("miscellaneous");
		aspectCategories.put("restaurant", restaurant);

		HashSet<String> ambience = new HashSet<String>();  
		ambience.add("general");
		aspectCategories.put("ambience", ambience);

		HashSet<String> service = new HashSet<String>();
		service.add("general");
		aspectCategories.put("service", service);

		HashSet<String> location = new HashSet<String>();
		location.add("general");
		aspectCategories.put("location", location);

		HashSet<String> sustenance = new HashSet<String>();
		sustenance.add("prices");
		sustenance.add("quality");
		sustenance.add("style&options");
		aspectCategories.put("sustenance", sustenance);

		/* Set the domain. */
		String domain = "restaurant";

		/* Initialise the semi-automatic ontology builder. *///
		OntologyBuilder build = new OntologyBuilder(base, aspectCategories, domain);

		build.save("TestSkeletalOntology2020.owl");
		
		// Perform the termselection
		build.getTerms(); 
		
		//Now add the sentiment words in a kind of hierarchy
		build.addSentimentWords(); 
		build.save("ontoWithSentimentWords.owl");
		
		//Next is to add the hierarchy
		build.getHierarchicalClusters();
		build.save("finalOntology.owl");
		 
		}
	
}
	
