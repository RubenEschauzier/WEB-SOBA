package seminarOntologyBuilder;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.TreeSet;
import javafx. util. Pair;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.json.JSONException;
import org.json.JSONObject;

import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.WordNetDatabase;
import edu.smu.tspell.wordnet.impl.file.Morphology;
import edu.eur.absa.Framework;
import edu.eur.absa.data.DatasetJSONReader;
import edu.eur.absa.model.Dataset;
import edu.eur.absa.model.Relation;
import edu.eur.absa.model.Span;
import edu.eur.absa.model.Word;
import edu.eur.absa.model.exceptions.IllegalSpanException;
import seminarOntologyBuilder.Synonyms;
import seminarOntologyBuilder.SkeletalOntology;
import seminarOntologyBuilder.readJSON;
import edu.eur.absa.nlp.*;
import edu.eur.absa.Framework;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.WordNetDatabase.*;

//our own classes
import sentimentBuilder.SentimentWordProcessor;
import termSelector.TermSelectionAlgo;
import hierarchicalClustering.clusteringAlgorithm;
import hierarchicalClustering.HierarichalClusterAlgorithm;

//for the clustering things
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.apporiented.algorithm.clustering.AverageLinkageStrategy;
import com.apporiented.algorithm.clustering.Cluster;
import com.apporiented.algorithm.clustering.ClusteringAlgorithm;
import com.apporiented.algorithm.clustering.DefaultClusteringAlgorithm;
import com.apporiented.algorithm.clustering.ElbowPlotter;
import com.apporiented.algorithm.clustering.visualization.DendrogramFrame;
import com.apporiented.algorithm.clustering.visualization.DendrogramPanel;

// import edu.cmu.lti.ws4j.*;

/**
 * A method that builds an ontology semi-automatically.
 * 
 * @author Karoliina Ranta
 * Adapted by Fenna ten Haaf
 * 
 */
public class OntologyBuilder {

	public final String NS = "http://www.semanticweb.org/bsc.seminar/ontologies/2020/5/RestaurantOntologyBase";
	/* The base ontology. */
	private SkeletalOntology base;
	private HashMap<String, HashSet<String>> aspectCategories;
	private String domain;
	private Dataset reviewData;
	private Dataset reviewData1;
	private JSONObject wordFrequencyReview;
	private JSONObject wordFrequencyDocument;
	private HashMap<String, HashMap<String, Integer>> contrastData;
	private int numRejectTerms;   //only words
	private int numAcceptTerms;
	private int numRejectOverall;  //words + parent-relations
	private int numAcceptOverall;
	private int numRev;
	private HashSet<String> remove;
	private double threshold;
	private double invThreshold;
	private double[] fraction;
	private HashMap<String, HashSet<String>> relatedNouns;
	private boolean relations;
	private HashMap<String, HashSet<String>> nounsWithSynset;
	private HashSet<String> synonymsAccepted;
	public HashSet<String> allAcceptedTerms; 
	public HashSet<String> acceptedSoFar; 
	public HashMap<String,String> allTermsWithPOS;

	private boolean synonymsInitialised;
	private TermSelectionAlgo synonym_select;

	/**
	 * A constructor for the OntologyBuilder class.
	 * @param baseOnt, the base ontology from which the final ontology is further constructed
	 * @param aspectCat, the aspect categories of the domain
	 * @param dom, the domain name
	 * @param thres, the threshold to use for the subsumption method
	 * @param invThres, the second threshold for the subsumption method
	 * @param frac, the top fraction of terms to suggest
	 */
	public OntologyBuilder(SkeletalOntology baseOnt, HashMap<String, HashSet<String>> aspectCat, String dom, boolean r) throws Exception {

		/* Initialise the base ontology, aspect categories, and domain name. */
		base = baseOnt;
		aspectCategories = aspectCat;
		domain = dom;
		numRejectTerms = 0;
		numAcceptTerms = 0;
		numRejectOverall = 0;
		numAcceptOverall = 0;
		relations = r;
		synonymsInitialised = false;

		remove = new HashSet<String>();
		remove.add("http://www.w3.org/2000/01/rdf-schema#Resource");
		remove.add("http://www.w3.org/2002/07/owl#Thing");
		remove.add(base.URI_Mention);
		remove.add(base.URI_Sentiment);
		remove.add(base.NS + "#" + domain.substring(0, 1).toUpperCase() + domain.substring(1).toLowerCase() + "Mention");

		HashMap<String, HashSet<String>> aspectTypes = groupAspects();

		synonymsAccepted = new HashSet<String>();

		HashSet<String> doneAspects = new HashSet<String>();
		//HashSet<String> 
		allAcceptedTerms = new HashSet<String>();

		/**
		We want to add synonyms of particular words to the Generic Positive and Negative classes:
		Generic Positive verbs in Kim's ontology: love, enjoy, respect, recommend
		Generic positive adjectives: decent, enjoyable, excellent, exceptional, 
		   	fabulous, fantastic, favourite, fine, fun, good, great, happy,
		   	impressive, interesting, nice, unique, wonderful, pleasant, 
		   	Authentic, Awesome, Beautiful, Best, Charming, Creative
		Generic Negative verbs: Disappoint, lack, limit, rush, skip, spill
		Generic negative adjectives: Hideous, Horrible, inexpertly, mediocre, overhyped, overrated,
			poor, uncomfortable, awful, bad, bland, difficult

		goal: to Select a few (like 4) words for each category, and use word embeddings to find the x closest words. 
		these words will also be added to the ontology as separate classes, and then for each of those we can suggest synonyms?? Or 
		use wordnet to determine which of them are synonyms and which should be added as separate classes
		 */

		TermSelectionAlgo synonym_select = new TermSelectionAlgo(Framework.LARGEDATA_PATH +"yelp_wordvec", Framework.OUTPUT_PATH+"Output_stanford_hashmap");//initialise synonyms

		String negativePropertyURI1 = base.addClass("bad#ajective#1", "Bad", true, "bad", new HashSet<String>(), base.URI_GenericNegativeProperty);
		allAcceptedTerms = this.getSynonymsWithEmbeddings(allAcceptedTerms,"bad",10, synonym_select, negativePropertyURI1);

		String negativeActionURI2 = base.addClass("hate#verb#1", "Hate", true, "hate", new HashSet<String>(), base.URI_GenericNegativeAction);
		allAcceptedTerms = this.getSynonymsWithEmbeddings(allAcceptedTerms,"hate",10, synonym_select,negativeActionURI2);

		String positivePropertyURI1 = base.addClass("good#adjective#1", "Good", true, "good", new HashSet<String>(), base.URI_GenericPositiveProperty);
		allAcceptedTerms = this.getSynonymsWithEmbeddings(allAcceptedTerms,"good", 10 , synonym_select, positivePropertyURI1);

		String positiveActionURI1 = base.addClass("enjoy#verb#1", "Enjoy", true, "enjoy", new HashSet<String>(), base.URI_GenericPositiveAction);
		allAcceptedTerms = this.getSynonymsWithEmbeddings(allAcceptedTerms,"enjoy",10, synonym_select,  positiveActionURI1);


		/* Loop over the aspect category entities. */

		//create a hashmap with synsets as value of the entities (key), and add as synset property during loop
		HashMap<String, String> entitySynsets = new HashMap<String, String>();
		//add for aspects
		entitySynsets.put("ambience", "ambience#noun#1");
		entitySynsets.put("service", "service#noun#1");
		entitySynsets.put("restaurant", "restaurant#noun#1");
		entitySynsets.put("location", "location#noun#1");
		entitySynsets.put("sustenance", "sustenance#noun#1"); //add drinks and food to sustenance

		for (String entity : aspectCat.keySet()) { 									//for each aspect
			HashSet<String> aspectSet = aspectCat.get(entity); 						//retrieve aspect's categories
			/* Each entity should have its own AspectMention class. */
			HashSet<String> aspects = new HashSet<String>();						// create 'aspect' HashSet, to contain all ASPECT#CATEGORY combinations per aspect
			String synset = entitySynsets.get(entity);								// retrieve synset of aspect
			for (String aspect : aspectSet) {										//per category of an aspect

				/* Don't add miscellaneous to the ontology. */
				if (!aspect.equals("miscellaneous")) {
					aspects.add(entity.toUpperCase() + "#" + aspect.toUpperCase());	// all ASPECT#CATEGORY added to 'aspects'
				}
			}
			String newClassURI = base.addClass(synset, entity.substring(0, 1).toUpperCase() + entity.substring(1).toLowerCase() + "Mention", true, entity, aspects, base.URI_EntityMention);

			/* The domain entity doesn't get sentiment classes. */
			if (!entity.equals(domain)) {

				/* Create the SentimentMention classes (positive and negative) related to the entity. */
				String aspectPropertyClassURI = base.addClass(synset, entity.substring(0, 1).toUpperCase() + entity.substring(1).toLowerCase() + "PropertyMention", true, entity.toLowerCase(), new HashSet<String>(), newClassURI, base.URI_PropertyMention);
				String aspectActionClassURI =  base.addClass(synset, entity.substring(0, 1).toUpperCase() + entity.substring(1).toLowerCase() + "ActionMention", true, entity.toLowerCase(), new HashSet<String>(), newClassURI, base.URI_ActionMention);
				String positivePropertyClassURI = base.addClass(synset, entity.substring(0, 1).toUpperCase() + entity.substring(1).toLowerCase() + "PositiveProperty", false, entity.toLowerCase(), new HashSet<String>(), aspectPropertyClassURI, base.URI_Positive);
				String negativePropertyClassURI = base.addClass(synset, entity.substring(0, 1).toUpperCase() + entity.substring(1).toLowerCase() + "NegativeProperty", false, entity.toLowerCase(), new HashSet<String>(), aspectPropertyClassURI,  base.URI_Negative);
				String positiveActionClassURI = base.addClass(synset, entity.substring(0, 1).toUpperCase() + entity.substring(1).toLowerCase() + "PositiveAction", false, entity.toLowerCase(), new HashSet<String>(), aspectActionClassURI, base.URI_Positive);
				String negativeActionClassURI = base.addClass(synset, entity.substring(0, 1).toUpperCase() + entity.substring(1).toLowerCase() + "NegativeAction", false, entity.toLowerCase(), new HashSet<String>(), aspectActionClassURI, base.URI_Negative);
				String positiveEntityClassURI = base.addClass(synset, entity.substring(0, 1).toUpperCase() + entity.substring(1).toLowerCase() + "PositiveEntity", false, entity.toLowerCase(), new HashSet<String>(), newClassURI, base.URI_Positive);
				String negativeEntityClassURI = base.addClass(synset, entity.substring(0, 1).toUpperCase() + entity.substring(1).toLowerCase() + "NegativeEntity", false, entity.toLowerCase(), new HashSet<String>(), newClassURI, base.URI_Negative);
	
			} 

			/* Create AspectMention and SentimentMention subclasses for all aspects except for general and miscellaneous. */
			for (String aspectName : aspectTypes.keySet()) { // for each category
				if (!aspectName.equals("general") && !aspectName.equals("miscellaneous") && !doneAspects.contains(aspectName)) {
					doneAspects.add(aspectName);

					/* Create the AspectMention class. */
					HashSet<String> aspectsAsp = new HashSet<String>();
					for (String entityName : aspectTypes.get(aspectName)) { // retrieve all aspects per category
						aspectsAsp.add(entityName.toUpperCase() + "#" + aspectName.toUpperCase()); //ASPECT#CATEGORY added to aspectAsp, to be added to class as aspect-property
					}
					//add CategoryMention class
					String newClassURIAspect = base.addClass(aspectName.substring(0, 1).toUpperCase() + aspectName.substring(1).toLowerCase() + "Mention", true, aspectName, aspectsAsp, base.URI_EntityMention);

					/* Create the SentimentMention classes. */
					String aspectPropertyClassURI = base.addClass(aspectName.substring(0, 1).toUpperCase() + aspectName.substring(1).toLowerCase() + "PropertyMention", true, entity.toLowerCase(), new HashSet<String>(), newClassURIAspect, base.URI_PropertyMention);
					String aspectActionClassURI =  base.addClass(aspectName.substring(0, 1).toUpperCase() + aspectName.substring(1).toLowerCase() + "ActionMention", true, entity.toLowerCase(), new HashSet<String>(), newClassURIAspect, base.URI_ActionMention);
					String positivePropertyURI = base.addClass(aspectName.substring(0, 1).toUpperCase() + aspectName.substring(1).toLowerCase() + "PositiveProperty", false, aspectName.toLowerCase(), new HashSet<String>(), aspectPropertyClassURI, base.URI_Positive);
					String negativePropertyURI = base.addClass(aspectName.substring(0, 1).toUpperCase() + aspectName.substring(1).toLowerCase() + "NegativeProperty", false, aspectName.toLowerCase(), new HashSet<String>(), aspectPropertyClassURI, base.URI_Negative);
					String positiveActionURI = base.addClass(aspectName.substring(0, 1).toUpperCase() + aspectName.substring(1).toLowerCase() + "PositiveAction", false, aspectName.toLowerCase(), new HashSet<String>(), aspectActionClassURI, base.URI_Positive);
					String negativeActionURI = base.addClass(aspectName.substring(0, 1).toUpperCase() + aspectName.substring(1).toLowerCase() + "NegativeAction", false, aspectName.toLowerCase(), new HashSet<String>(), aspectActionClassURI, base.URI_Negative);
					String positiveEntityURI = base.addClass(aspectName.substring(0, 1).toUpperCase() + aspectName.substring(1).toLowerCase() + "PositiveEntity", false, aspectName.toLowerCase(), new HashSet<String>(), newClassURIAspect, base.URI_Positive);
					String negativeEntityURI = base.addClass(aspectName.substring(0, 1).toUpperCase() + aspectName.substring(1).toLowerCase() + "NegativeEntity", false, aspectName.toLowerCase(), new HashSet<String>(), newClassURIAspect, base.URI_Negative);					

					if (aspectName.contains("&")) {
						HashSet<String> lexs = new HashSet<String>();
						String[] parts = aspectName.split("&");
						lexs.add(parts[0]);
						lexs.add(parts[1]);
						base.addLexicalizations(newClassURIAspect, lexs);
						base.addLexicalizations(aspectPropertyClassURI, lexs);
						base.addLexicalizations(aspectActionClassURI, lexs);
					}
					if (aspectName.contains("_")) { 
						HashSet<String> lexs = new HashSet<String>();
						String[] parts = aspectName.split("_");
						lexs.add(parts[0]);
						lexs.add(parts[1]);
						base.addLexicalizations(newClassURIAspect, lexs);
						base.addLexicalizations(aspectPropertyClassURI, lexs);
						base.addLexicalizations(aspectActionClassURI, lexs);
					}
				}
			}			
		}

		//add Food and DrinksMention to Sustenance Class

		String FoodMentionClassURI = base.addClass("food#noun#1", "FoodMention",true, "food", aspectCat.get("sustenance"), base.NS + "#SustenanceMention");
		String FoodMentionActionClassURI = base.addClass("food#noun#1", "FoodActionMention",true, "food", aspectCat.get("sustenance"), base.NS + "#SustenanceActionMention");
		String FoodMentionPropertyClassURI = base.addClass("food#noun#1",  "FoodPropertyMention", true, "food", aspectCat.get("sustenance"), base.NS + "#SustenancePropertyMention");
		
		//add a few food adjectives
		String positiveFoodURI1 = base.addClass("tasty#adjective#1", "Tasty", true, "tasty", new HashSet<String>(), NS + "#FoodPositiveProperty");
		allAcceptedTerms = this.getSynonymsWithEmbeddings(allAcceptedTerms, "tasty",10, synonym_select,  positiveActionURI1);
		// could also add some extra verbs and stuff for everything, still
		
		String DrinksMentionClassURI = base.addClass("drinks#noun#1", "DrinksMention", true, "drinks", aspectCat.get("sustenance"), base.NS + "#SustenanceMention");
		String DrinksMentionActionClassURI = base.addClass("drinks#noun#1", "DrinksActionMention", true, "drinks", aspectCat.get("sustenance"), base.NS + "#SustenanceActionMention");
		String DrinksMentionPropertyClassURI = base.addClass("drinks#noun#1", "DrinksPropertyMention", true, "drinks", aspectCat.get("sustenance"), base.NS + "#SustenancePropertyMention");

		//add a few extra EntityMention classes
		//ExperienceMention
		HashSet<String> experienceAspects = new HashSet<String>();
		experienceAspects.add("RESTAURANT#MISCELLANEOUS");
		String ExperienceMentionClassURI = base.addClass("experience#noun#3", "Experience" + "Mention", true, "experience", experienceAspects, base.URI_EntityMention);
		String ExperienceMentionActionClassURI = base.addClass("experience#noun#3", "Experience" + "ActionMention", true, "experience", experienceAspects, base.URI_ActionMention);
		String ExperienceMentionPropertyClassURI = base.addClass("experience#noun#3", "Experience" + "PropertyMention", true, "experience", experienceAspects, base.URI_PropertyMention);
		//this.suggestSynonyms("experience", ExperienceMentionClassURI, ExperienceMentionActionClassURI, ExperienceMentionPropertyClassURI);

		/**
		//PersonMention
		HashSet<String> personAspects = new HashSet<String>();
		String PersonMentionClassURI = base.addClass("person#noun#1", "Person" + "Mention", true, "person", personAspects, base.URI_EntityMention);
		String PersonMentionActionClassURI = base.addClass("person#noun#1", "Person" + "ActionMention", true, "person", personAspects, base.URI_ActionMention);
		String PersonMentionPropertyClassURI = base.addClass("person#noun#1", "Person" + "PropertyMention", true, "person", personAspects, base.URI_PropertyMention);
		//this.suggestSynonyms("person", PersonMentionClassURI, PersonMentionActionClassURI, PersonMentionPropertyClassURI);

		//TimeMention
		HashSet<String> timeAspects = new HashSet<String>();
		String TimeMentionClassURI = base.addClass("time#noun#2", "Time" + "Mention", true, "time", timeAspects, base.URI_EntityMention);
		String TimeMentionActionClassURI = base.addClass("time#noun#2", "Time" + "ActionMention", true, "time", timeAspects, base.URI_ActionMention);
		String TimeMentionPropertyClassURI = base.addClass("time#noun#2", "Time" + "PropertyMention", true, "time", timeAspects, base.URI_PropertyMention);
		//this.suggestSynonyms("time", TimeMentionClassURI, TimeMentionActionClassURI, TimeMentionPropertyClassURI);
		 */
	}


	/**
	 * A method to perform the termselection
	 */
	public void getTerms() throws Exception 
	{
		TermSelectionAlgo term_select = new TermSelectionAlgo(Framework.LARGEDATA_PATH+"google_wordvec", Framework.LARGEDATA_PATH +"yelp_wordvec", Framework.OUTPUT_PATH+"Output_stanford_hashmap");
		term_select.create_word_term_score();
		System.out.println("doing thresholds");
		//double threshold_noun = term_select.create_threshold(100, "NN");
		//double threshold_verb = term_select.create_threshold(15, "VB");
		//double threshold_adj = term_select.create_threshold(80, "JJ");
		//term_select.create_term_list(0.84, threshold_verb, threshold_adj, 100, 80, 80);

		// Eigenlijk zouden we het zo moeten maken dat de thresholds als input voor de constructor gemaakt worden
		allAcceptedTerms =  term_select.create_term_list(allAcceptedTerms, 0.84, 0.8, 0.915, 100, 20, 80); 
		term_select.save_outputs(term_select);
	}


	/**
	 * Method to create a hierarchy within the sentiment words, and add them to the ontology
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void addSentimentWords() throws IOException, ClassNotFoundException
	{

		//First, we get the hashMap that will give us whether we have a noun or a verb (from termSelectionALgo method)
		File toRead_terms=new File(Framework.OUTPUT_PATH+ "Output_stanford_hashmap");
		FileInputStream fis_terms=new FileInputStream(toRead_terms);
		ObjectInputStream ois_terms =new ObjectInputStream(fis_terms);
		allTermsWithPOS =(HashMap<String,String>)ois_terms.readObject();
		ois_terms.close();
		fis_terms.close();	

		HashSet<String> set;

		//Get the clustered sentiment words
		Map<String, Map<String,String>> clustered_sentiment = new HashMap<String,Map<String,String>>();
		SentimentWordProcessor sent_calc = new SentimentWordProcessor(Framework.LARGEDATA_PATH + "yelp_wordvec", Framework.OUTPUT_PATH + "sentiment_mentions");
		clustered_sentiment = sent_calc.create_sentiment_links();

		for (Map.Entry<String,Map<String,String>> entry : clustered_sentiment.entrySet()) 
		{
			//mogelijke keys zijn 'generic'of een van de mention_words:
			//{"ambience", "drinks","food","service","price","location","quality", "style", "options", "experience", "restaurant"};

			String mentionClass = entry.getKey(); 
			Map<String,String> sentPolarities = entry.getValue();

			for (Map.Entry<String,String> entry2 : sentPolarities.entrySet()) 
			{
				String sentWord = entry2.getKey(); // the word to add to our ontology
				String pol = entry2.getValue(); // the polarity class
				String pos = ""; 

				//Define the part-of-speech
				if (allTermsWithPOS.get(sentWord).contains("NN"))
				{
					pos = "noun";

					set = base.getSubclasses(base.URI_EntityMention); 
					set.remove(base.URI_EntityMention);
					HashSet<String> temp = base.getSubclasses(base.URI_ActionMention);
					temp.addAll(base.getSubclasses(base.URI_PropertyMention));
					temp.addAll(base.getSubclasses(base.URI_Sentiment));
					set.removeAll(temp);
				}
				else if (allTermsWithPOS.get(sentWord).contains("VB"))
				{
					pos = "verb"; 

					set = base.getSubclasses(base.URI_ActionMention); 
					set.remove(base.URI_ActionMention);
					HashSet<String> temp = base.getSubclasses(base.URI_Sentiment);
					set.removeAll(temp);
				}	
				else
				{
					pos = "adjective"; 	

					set = base.getSubclasses(base.URI_PropertyMention); 
					set.remove(base.URI_PropertyMention);
					HashSet<String> temp = base.getSubclasses(base.URI_Sentiment);
					set.removeAll(temp);
				}

				//HashSet<String> parent = base.getLexicalizations(parentURI);

				String parentClassURI = "";

				if(pol.equals("positive")) //positive polarity
				{ 
					if (mentionClass.equals("generic")) { // add to type 1 positive entity, action or property mention
						if (pos.equals("noun"))
						{
							parentClassURI = base.URI_GenericPositiveEntity;
						}
						else if (pos.equals("verb"))
						{			
							parentClassURI = base.URI_GenericPositiveAction;
						}	
						else if (pos.equals("adjective"))
						{
							parentClassURI = base.URI_GenericPositiveProperty;
						}
					}
					else // add to other type of negative entity, action or property mention
					{
						if (pos.equals("noun"))
						{
							parentClassURI = NS + mentionClass+ "PositiveMention";
						}
						else if (pos.equals("verb"))
						{			
							parentClassURI = NS + mentionClass+ "PositiveAction";
						}	
						else if (pos.equals("adjective"))
						{
							parentClassURI = NS + mentionClass+ "PositiveProperty";
						}
					}
				}
				else // negative polarity
				{  
					if (mentionClass.equals("generic")) { // add to type 1 positive entity, action or property mention
						if (pos.equals("noun"))
						{
							parentClassURI = base.URI_GenericNegativeEntity;
						}
						else if (pos.equals("verb"))
						{			
							parentClassURI = base.URI_GenericNegativeAction;
						}	
						else if (pos.equals("adjective"))
						{
							parentClassURI = base.URI_GenericNegativeProperty;
						}
					}
					else // add to other type of negative entity, action or property mention
					{
						if (pos.equals("noun"))
						{
							parentClassURI = NS + "#"+ mentionClass.substring(0, 1).toUpperCase() + mentionClass.substring(1).toLowerCase()+ "NegativeMention";
						}
						else if (pos.equals("verb"))
						{			
							parentClassURI = NS + mentionClass.substring(0, 1).toUpperCase() + mentionClass.substring(1).toLowerCase()+ "NegativeAction";
						}	
						else if (pos.equals("adjective"))
						{
							parentClassURI = NS + mentionClass.substring(0, 1).toUpperCase() + mentionClass.substring(1).toLowerCase()+ "NegativeProperty";
						}
					}

				}

				//Now we add the new parent URI thingy to the ontology
				String newConcept = base.addClass(pos, sentWord.substring(0, 1).toUpperCase() + sentWord.substring(1).toLowerCase(), true, sentWord, new HashSet<String>(), parentClassURI);
			} 

		}

	}

	/**
	 * 
	 * @throws Exception
	 */
	public void getHierarchicalClusters() throws Exception {

		//First get POS data
		File toRead_terms=new File(Framework.OUTPUT_PATH+ "Output_stanford_hashmap");
		FileInputStream fis_terms=new FileInputStream(toRead_terms);
		ObjectInputStream ois_terms =new ObjectInputStream(fis_terms);
		allTermsWithPOS =(HashMap<String,String>)ois_terms.readObject();
		ois_terms.close();
		fis_terms.close();	

		//Now start the method
		Scanner scanner = new Scanner(System.in);

		String[] mentionclasses = {"restaurant","ambience","service","Location","food","drinks","price","quality","style","options","experience"};
		int numberofclusters = mentionclasses.length;
		int iterations = 100;
		String name = "aspect_mentions";
		String approach = "similarities";

		clusteringAlgorithm HC = new clusteringAlgorithm(name, numberofclusters, iterations, mentionclasses, approach);
		List<Pair<String,Map<String,List<String>>>> clusterRepPairList = HC.getHierarchicalClusters();


		for (Pair<String,Map<String,List<String>>> clusterRepPair : clusterRepPairList)
		{

			String mentClass = clusterRepPair.getKey();
			Map<String,List<String>> clusterRepresentation = clusterRepPair.getValue();

			for (Map.Entry<String, List<String>> entry2 : clusterRepresentation.entrySet()) {
				// parent-child relation
				String parent = entry2.getKey();
				List<String> children = entry2.getValue();

				if (!children.isEmpty()) {
					// Now we add the parent-child relation to the skeletal ontology
					for (String child : children) {
						
						String checkChild ="";
						String checkParent ="";
						
						if(child.length()>5)
						{
							checkChild = child.substring(0,5);
						}
						else
						{
							checkChild = "it's fine!"; // if it is shorter, then it can't be clstr# something
								
						}
						
						if(parent.length()>5)
						{
							checkParent = parent.substring(0,5);
						}
						else
						{
							checkParent = "it's fine!"; // if it is shorter, then it can't be clstr# something
								
						}
						
						String parentClassURI="";
						
						if(!(checkChild.equals("clstr")) && !(checkParent.equals("clstr")))
						{
							System.out.println("Parent: "+parent+" and child: "+child+ " in the MentionClass: "+mentClass);
							
							//First we get the parts of speech
							String pos ="";
							//String test = allTermsWithPOS.get(child); 

							pos = "noun"; // testing if it works like this 
							/*
							if (allTermsWithPOS.get(child).contains("NN"))
							{
								pos = "noun";
							}
							else if (allTermsWithPOS.get(child).contains("VB"))
							{
								pos = "verb"; 
							}	
							else
							{
								pos = "adjective"; 	
							}
							*/

							if (pos.equals("noun"))
							{
								if (parent.equals(child)) { // in this case, we are talking about a direct subclass of mentionClass
									parentClassURI = NS + "#"+ mentClass.substring(0, 1).toUpperCase() + mentClass.substring(1).toLowerCase()+ "Mention";
								}
								else {
									parentClassURI = NS + "#"+ parent.substring(0, 1).toUpperCase() + parent.substring(1).toLowerCase()+ "Mention";
								}
							}
							else if (pos.equals("verb"))
							{			
								if (parent.equals(child)) { // in this case, we are talking about a direct subclass of mentionClass
									parentClassURI = NS + "#"+ mentClass.substring(0, 1).toUpperCase() + mentClass.substring(1).toLowerCase()+ "ActionMention";
								}
								else {
									parentClassURI = NS + "#"+ parent.substring(0, 1).toUpperCase() + parent.substring(1).toLowerCase()+ "ActionMention";
								}
							}	
							else if (pos.equals("adjective"))
							{
								if (parent.equals(child)) { // in this case, we are talking about a direct subclass of mentionClass
									parentClassURI = NS + "#"+ mentClass.substring(0, 1).toUpperCase() + mentClass.substring(1).toLowerCase()+ "PropertyMention";
								}
								else {
									parentClassURI = NS + "#"+ parent.substring(0, 1).toUpperCase() + parent.substring(1).toLowerCase()+ "PropertyMention";
								}
							}

							String newConcept = base.addClass(pos, child.substring(0, 1).toUpperCase() + child.substring(1).toLowerCase(), true, child, new HashSet<String>(), parentClassURI);
						}
						
						if((checkParent.equals("clstr"))) 
						{ //just add the child as a direct subclass
							String pos = "noun"; // to test
							
							parentClassURI = NS + "#"+ mentClass.substring(0, 1).toUpperCase() + mentClass.substring(1).toLowerCase()+ "Mention";
							String newConcept = base.addClass(pos, child.substring(0, 1).toUpperCase() + child.substring(1).toLowerCase(), true, child, new HashSet<String>(), parentClassURI);
						}
					}
				}
			}
		}
	}


	/**
	 * A method to get a number of similar words using word embeddings
	 * @param word
	 * @throws Exception
	 */
	public HashSet<String> getSynonymsWithEmbeddings(HashSet<String>acceptedSoFar, String word, int synonymNum, TermSelectionAlgo synsel, String... classURI) throws Exception{

		HashSet<String> accepted = new HashSet<String>();
		HashSet<String> rejected = new HashSet<String>();
		HashSet<String> acceptSoFar = acceptedSoFar;
		//accepted.add("test");
		//rejected.add("test"); 
		acceptSoFar.add(word);

		Integer numAccepted = 0; 
		Map<String, double[]> word_vec_yelp = new HashMap<String, double[]>();
		final int SYNONYM_NUM = synonymNum; 
		TermSelectionAlgo synonym_select = synsel;

		// Add the word that we want synonyms of to the accepted terms list as well

		//TermSelectionAlgo constructor initialiseren
		//if (!synonymsInitialised) {
		//TermSelectionAlgo synonym_select = new TermSelectionAlgo(Framework.LARGEDATA_PATH +"yelp_wordvec", Framework.OUTPUT_PATH+"Output_stanford_hashmap");
		// synonymsInitialised = True;
		//}

		Collection<String> similar_words_list = synonym_select.getNearestWords(word,SYNONYM_NUM); 

		System.out.println("Enter 'a' to accept and 'r' to reject the synonym: ");
		Scanner input = new Scanner(System.in);

		int i = 0;
		for (String nearTerm : similar_words_list) {
			i++;

			if (nearTerm.equals(word) || accepted.contains(nearTerm) || rejected.contains(nearTerm) || acceptSoFar.contains(nearTerm))  {
				continue; //in this case, we have already suggested the term. we won't suggest it again.
			}

			while(true) {
				System.out.println("synonym: " + word + " --> " + nearTerm);
				String userInput = input.next();
				if (userInput.equals("a")) {
					numAccepted++;
					numAcceptOverall++;
					accepted.add(nearTerm);
					synonymsAccepted.add(nearTerm);
					acceptSoFar.add(nearTerm);
					break;

				} 
				else if (userInput.equals("r")) {
					rejected.add(nearTerm);
					numRejectOverall++;
					break; 
				}
				else {
					System.out.print("Please type either a or r."+'\n');
				}
			}
		}
		for (String URI : classURI) {
			base.addLexicalizations(URI, accepted);
		}
		return acceptSoFar;
	}



	/**
	 * A method that suggests the synonyms of a word and adds it as a lexicalization to the concepts, using wordNet.
	 * @param classURI, the concepts to which to add the lexicalizations
	 * @param word, the word of which to find synonyms
	 */
	public void suggestSynonyms(String word, String... classURI) {
		HashSet<String> accepted = new HashSet<String>();
		HashSet<String> rejected = new HashSet<String>();
		Integer numAccepted = 0; 
		Synonyms syn = new Synonyms(word);

		// Add the word that we want synonyms of to the accepted terms list as well
		allAcceptedTerms.add(word);

		System.out.println("Enter 'a' to accept and 'r' to reject the synonym: ");
		Scanner input = new Scanner(System.in);
		int i = 0;
		for (String synonym : syn.synonyms()) {
			i++;
			if (i > 20 || numAccepted>5) { //we stop if we have suggested more than this number of synonyms or if we already have 5 synonyms
				break; 
			}

			if (synonym.equals(word) || accepted.contains(synonym) || rejected.contains(synonym) || allAcceptedTerms.contains(synonym))  {
				continue; //in this case, we have already suggested the term. we won't suggest it again.
			}

			while(true) {
				System.out.println("synonym: " + word + " --> " + synonym);
				String userInput = input.next();
				if (userInput.equals("a")) {
					numAccepted++;
					numAcceptOverall++;
					accepted.add(synonym);
					synonymsAccepted.add(synonym);
					allAcceptedTerms.add(synonym);
					break;

				} 
				else if (userInput.equals("r")) {
					rejected.add(synonym);
					numRejectOverall++;
					break; 
				}
				else {
					System.out.print("Please type either a or r."+'\n');
				}
			}
		}
		for (String URI : classURI) {
			base.addLexicalizations(URI, accepted);
		}
	}


	/**
	 * Creates an object that stores all the aspect types and for each aspect which entities have this aspect.
	 * @return The HashMap containing the aspects and corresponding entities.
	 */
	public HashMap<String, HashSet<String>> groupAspects() {
		HashMap<String, HashSet<String>> aspectTypes = new HashMap<String, HashSet<String>>();

		/* Loop over the entities. */
		for (String entity : aspectCategories.keySet()) {

			/* Loop over the aspects of the entity. */
			for (String aspect : aspectCategories.get(entity)) {
				HashSet<String> entities;

				/* Check if the set already contains the aspect. */
				if (aspectTypes.containsKey(aspect)) {
					entities = aspectTypes.get(aspect);
				} else {
					entities = new HashSet<String>();
				}
				entities.add(entity);
				aspectTypes.put(aspect, entities);
			}
		}
		return aspectTypes;
	}

	/**
	 * A method that returns the number of accepted and rejected terms.
	 * @return an array with first the number of accepted and second number of rejected terms
	 */
	public int[] getStats() {
		int[] stats = new int[3];
		stats[0] = numAcceptOverall; //change to numAcceptTerms if you only need words and no parent-relations
		stats[1] = numRejectOverall;
		return stats;
	}

	/**
	 * converts a verb to its most simple form.
	 * @param verb to be converted
	 * @return simple form of the verb
	 */
	public String verbConvertion(String verb) {

		System.setProperty("wordnet.database.dir", "C:\\Users\\HP\\Documents\\Advanced programming\\.metadata\\.plugins\\org.eclipse.ltk.core.refactoring\\.refactorings\\.workspace\\absa_software\\target\\classes\\externalData\\WordNet-3.0\\dict");
		WordNetDatabase database = WordNetDatabase.getFileInstance();

		Morphology id = Morphology.getInstance();

		String[] arr = id.getBaseFormCandidates(verb, SynsetType.VERB);
		if (arr.length>0) {
			return arr[0];
		}
		return verb;

	}

	/**
	 * Save the built ontology.
	 * @param file, the name of the file to which to save the ontology
	 */
	public void save(String file) {
		base.save(file);
	}
}