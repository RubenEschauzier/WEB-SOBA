/**
 * 
 */
package corpusLoader;

import java.io.FileInputStream;  
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonStreamParser;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

import edu.stanford.nlp.simple.Sentence;





public class MyCorpus{

	private String filelocation_review;
	private String filelocation_business;
	private ArrayList<String> restaurants = new ArrayList<String>();
	private Set<String> allTerms = new HashSet<String>();
	
	
	public MyCorpus(String filelocation_review, String filelocation_business) {
		this.filelocation_review = filelocation_review;
		this.filelocation_business = filelocation_business;
		
	}
	

	public ArrayList<String> business_identifier() throws FileNotFoundException, UnsupportedEncodingException {
	   //int counter = 0;	
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
	          //if (counter == 10) {
	            // break;  
	          //}
	          //counter += 1;
	          
	      }
	   }
	   return restaurants;

	}
		
	
	public void review_loader() throws FileNotFoundException, UnsupportedEncodingException {
		MaxentTagger tagger = new MaxentTagger("C:\\Users\\Ruben\\git\\Heracles\\stanford-postagger-2018-10-16\\models\\english-bidirectional-distsim.tagger");
		InputStream is_r = new FileInputStream(filelocation_review);
		Reader r_r = new InputStreamReader(is_r, "UTF-8");
		Gson gson_r = new GsonBuilder().create();
		JsonStreamParser p = new JsonStreamParser(r_r);
		while (p.hasNext()) {
			JsonElement e = p.next();
			if (e.isJsonObject()) {
				review review = gson_r.fromJson(e, review.class);
				if (restaurants.contains(review.get_id())) {
					review.get_sentences();
					review.pos_tagger(tagger);
					ArrayList<String> terms_in_review = review.get_adj_verb_noun();
					allTerms.addAll(terms_in_review);
					// System.out.println(sentences_list);
					// System.out.println(verbs);					
				}
				else
				{
					System.out.println("A non restaurant found!");
				}
				}

			}
		}
		
		
	
	public static void main(String args[]) throws FileNotFoundException, UnsupportedEncodingException {
		MyCorpus yelp_dataset = new MyCorpus("E:\\review.json", "E:\\business.json");
		ArrayList<String> restaurants = yelp_dataset.business_identifier();
		// for(int i =0; i < rest.size(); i++) {
		//	  if (rest.lastIndexOf(rest.get(i)) != i)  {
		// 	     System.out.println(rest.get(i)+" is duplicated");
	    //      }
		//   }
		yelp_dataset.review_loader();
		System.out.println(yelp_dataset.allTerms);
			}
	
}
