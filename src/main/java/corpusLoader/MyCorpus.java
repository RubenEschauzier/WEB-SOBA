/**
 * 
 */
package corpusLoader;

import java.io.File;
import java.io.FileInputStream;   
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonStreamParser;

import edu.eur.absa.Framework;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;




//change so that you can read in hashmap with noun/verb/etc DONE
public class MyCorpus{

	private String filelocation_review;
	private String filelocation_business;
	private List<String> restaurants = new ArrayList<String>();
	private Map<String, String> allTerms = new HashMap<String, String>();
	
	
	public MyCorpus(String filelocation_review, String filelocation_business) {
		this.filelocation_review = filelocation_review;
		this.filelocation_business = filelocation_business;
		
	}
	
	public List<String> business_identifier() throws FileNotFoundException, UnsupportedEncodingException {
	   InputStream is_b = new FileInputStream(filelocation_business);
	   Reader r_b = new InputStreamReader(is_b, "UTF-8");
	   Gson gson_b = new GsonBuilder().create();
	   JsonStreamParser p = new JsonStreamParser(r_b);
	   while (p.hasNext()) {
	      JsonElement e = p.next();
	      if (e.isJsonObject()) {
	          business_identifier identifier = gson_b.fromJson(e, business_identifier.class);     
	          boolean isRestaurant = identifier.contains_key("RestaurantsPriceRange2");
	          if (isRestaurant == true) {
	        	  restaurants.add(identifier.get_id());
	          } 	          
	      }
	   }
	   return restaurants;

	}
		
	
	public void review_loader() throws FileNotFoundException, UnsupportedEncodingException {
		int counter = 0;
	    String pattern ="[\\p{Punct}&&[^@',&]]";
		Properties props = new Properties();
	    // set the list of annotators to run
	    props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
	    // set a property for an annotator, in this case the coref annotator is being
	    // build pipeline
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		//MaxentTagger tagger = new MaxentTagger(filelocation_pos);
		InputStream is_r = new FileInputStream(filelocation_review);
		Reader r_r = new InputStreamReader(is_r, "UTF-8");
		Gson gson_r = new GsonBuilder().create();
		JsonStreamParser p = new JsonStreamParser(r_r);
		while (p.hasNext()) {
			counter += 1;
			JsonElement e = p.next();
			if (e.isJsonObject()) {
				Review review = gson_r.fromJson(e, Review.class);
				if (restaurants.contains(review.get_id())) {
					Map<String, String> review_terms = review.stanford_pipeline_tagger(pipeline, pattern);
					allTerms.putAll(review_terms);
					System.out.println("size:" + allTerms.size() + "reviews processed: " + counter);				
				}
				}
			}
		}
	
		/*
		 * public void review_loader_own() throws FileNotFoundException,
		 * UnsupportedEncodingException { String[] needed_tags = {"VB","VBD", "VBG",
		 * "VBN","VBP","VBZ","VH","VHD","VHG","VHN","VHP","VHZ","VV","VVD","VVG","VVN",
		 * "VVP","VVZ","JJ","JJR","JJS","NN","NNS","NP","NPS" }; ArrayList<String>
		 * needed_tags_l = new ArrayList<String>(Arrays.asList(needed_tags)); int
		 * counter = 0; MaxentTagger tagger = new MaxentTagger(filelocation_pos);
		 * InputStream is_r = new FileInputStream(filelocation_review); Reader r_r = new
		 * InputStreamReader(is_r, "UTF-8"); Gson gson_r = new GsonBuilder().create();
		 * JsonStreamParser p = new JsonStreamParser(r_r); while (p.hasNext()) { counter
		 * += 1; JsonElement e = p.next(); if (e.isJsonObject()) { Review review =
		 * gson_r.fromJson(e, Review.class); if (restaurants.contains(review.get_id()))
		 * { review.get_pos_tags(tagger); HashSet<String> terms_in_review =
		 * review.get_adj_noun_verb_new(needed_tags_l);
		 * allTerms.addAll(terms_in_review); System.out.println("size:" +
		 * allTerms.size() + "reviews processed: " + counter); //
		 * System.out.println(verbs); } }
		 * 
		 * } }
		 */

	
	public void write_to_file() {
		System.out.println("saving file..");
	    try {
	        File fileOne=new File("E:\\OutputTerms\\Output_stanford_hashmap");
	        FileOutputStream fos=new FileOutputStream(fileOne);
	        ObjectOutputStream oos=new ObjectOutputStream(fos);

	        oos.writeObject(allTerms);
	        oos.flush();
	        oos.close();
	        fos.close();
	    } catch(Exception e) {}
	}
		
	
	public static void main(String args[]) throws IOException {
		// WHEN YOU RUN THE FILE you need to add review.json and business.json to the external data directory!
		Framework framework = new Framework();
		MyCorpus yelp_dataset = new MyCorpus("E:\\review.json", "E:\\business.json");
		List<String> restaurants = yelp_dataset.business_identifier();
		yelp_dataset.review_loader();
		yelp_dataset.write_to_file();
		System.out.println(yelp_dataset.allTerms);
			}
	
}
