package termSelector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import it.unimi.dsi.fastutil.Arrays;

import java.util.Set;
import java.util.TreeMap;

public class TermSelectionAlgo {
	private Map<String, double[]> word_vec_yelp = new HashMap<String, double[]>();
	private Map<String, double[]> word_vec_google = new HashMap<String, double[]>();
	private Map<String, double[]> allTerms = new HashMap<String, double[]>();
	private Set<double[]> mention_classes_vec = new HashSet<double[]>();
	private Map<String,double[]> word_term_score = new HashMap<String, double[]>();
	private List<Double> term_scores= new ArrayList<Double>();
	private Map<Double, String> term_scores_test = new TreeMap<Double, String>(new DescOrder());
	private double max_score_noun;
	private double max_score_adj;
	private double max_score_verb;
	
	public TermSelectionAlgo(String filelocation_google, String filelocation_yelp, String filelocation_terms) throws ClassNotFoundException, IOException {
		read_file("google", filelocation_google);
		read_file("yelp", filelocation_yelp);
		read_file("allTerms", filelocation_terms);
		get_vectors_mention_classes();
		
	}
	
	public void read_file(String dataset, String filelocation) throws IOException, ClassNotFoundException {
		if (dataset == "google") {
			File toRead=new File(filelocation);
		    FileInputStream fis=new FileInputStream(toRead);
		    ObjectInputStream ois=new ObjectInputStream(fis);
	        word_vec_google =(HashMap<String, double[]>) ois.readObject();

	        ois.close();
	        fis.close();	
		}
		if (dataset == "yelp") {
			File toRead_yelp=new File(filelocation);
		    FileInputStream fis_yelp=new FileInputStream(toRead_yelp);
		    ObjectInputStream ois_yelp =new ObjectInputStream(fis_yelp);
	        word_vec_yelp =(HashMap<String,double[]>)ois_yelp.readObject();
	        System.out.println(word_vec_yelp.get("great"));
	        ois_yelp.close();
	        fis_yelp.close();	
		}
		if (dataset == "allTerms") {
			File toRead_terms=new File(filelocation);
		    FileInputStream fis_terms=new FileInputStream(toRead_terms);
		    ObjectInputStream ois_terms =new ObjectInputStream(fis_terms);
	        allTerms =(HashMap<String,double[]>)ois_terms.readObject();

	        ois_terms.close();
	        fis_terms.close();	
		}
	}
	
	
	public void get_vectors_mention_classes() {
		String[] mention_words = {"ambience", "drinks","food","service","price", "restaurant","location","quality", "style", "options"};
		for (String mention: mention_words) {
			mention_classes_vec.add(word_vec_yelp.get(mention));
		}
	}
	
	
	public double get_domain_similarity(double[] general_vec, double[] domain_vec) {	
		return (dotProduct(general_vec, domain_vec)/(getMagnitude(domain_vec) * getMagnitude(general_vec)));
	}
	
	// Mention classes are: Ambience, Drinks, Food, Service, Price, Restaurant, Location, Quality, Style, Options
	public double get_mention_class_similarity(double[] domain_vec) {
		double max = -1;
		for(double []vector : mention_classes_vec) {
			double similarity = (dotProduct(vector, domain_vec))/(getMagnitude(domain_vec) * getMagnitude(vector));
			if (similarity > max) {
				max = similarity;
			}
		}
		return max;
	}
	
	
	public double get_term_score(double[] general_vec, double[] domain_vec) {
		return 2 / (get_domain_similarity(general_vec, domain_vec))+(1/get_mention_class_similarity(domain_vec));
	}
	
	
	public void create_word_term_score() {
		for (Map.Entry<String, double[]> entry : word_vec_yelp.entrySet()) {
			if(word_vec_google.containsKey(entry.getKey())) {
				double term_score = get_term_score(word_vec_google.get(entry.getKey()), entry.getValue());
				if (term_scores.contains(term_score)) {
					
				}
				term_scores.add(term_score);
				term_scores_test.put(term_score, entry.getKey());
		    	//word_vec_yelp.put(entry.getKey(), w2vModel_yelp.getWordVector(entry.getKey()));
		    }
		}
		Collections.sort(term_scores, Collections.reverseOrder());
		System.out.println(term_scores_test);
	}
	public static void main(String args[]) throws ClassNotFoundException, IOException {
		TermSelectionAlgo term_select = new TermSelectionAlgo( "E://google_wordvec", "E://yelp_wordvec", "E:\\OutputTerms\\Output_stanford_hashmap");
		term_select.create_word_term_score();
	}
	
	static class DescOrder implements Comparator<Double>{
		@Override
		public int compare(Double o1, Double o2) {
			return o2.compareTo(o1);
		}
	}
	
	public static double dotProduct(double[] a, double[] b) {
		double sum = 0;
		for (int i = 0; i < a.length; i++) {
			sum += a[i] * b[i];
		}
		return sum;
	}
	public static double getMagnitude(double[] vec_1) {
		double magnitude = 0;
		for (int j = 0; j < vec_1.length; j++) {
			magnitude += Math.pow(vec_1[j],2);
		}
		return Math.sqrt(magnitude);
	}
}

