package sentimentBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleLists;

public class SentimentScoreCalculator {
	private Map<String, double[]> word_vec_yelp = new HashMap<String, double[]>();
	private Map<String, double[]> word_vec_google = new HashMap<String, double[]>();
	private Map<String, Integer> sentiment_mentions = new HashMap<String, Integer>();
	private Map<String, double[]> word_vec_refined = new HashMap<String, double[]>();
	
	
	public SentimentScoreCalculator(String fileloc_yelp, String fileloc_sent) throws ClassNotFoundException, IOException{
		read_file("yelp", fileloc_yelp);
		read_file("sentiment", fileloc_sent);		
		
		
	}
	
	/*
	 * public void read_word2vec_file(int size_vector) throws IOException { int
	 * counter = 0; BufferedReader in = new BufferedReader(new FileReader(
	 * "C:\\Users\\Ruben\\PycharmProjects\\Word2Vec(2.0)\\w2v_yelp.bin.refine"));
	 * while (in.readLine() != null) { if
	 * (in.readLine().split(" ")[0].equals("bad")) {
	 * System.out.println("bad found: " + in.readLine()); } counter += 1; try {
	 * double[] vectors = new double[size_vector]; String line = in.readLine();
	 * String[] line_array = line.trim().split(" "); String word = line_array[0];
	 * for (int i = 1; i < size_vector + 1; i++) { vectors[i-1] =
	 * Double.parseDouble(line_array[i]); } word_vec_refined.put(word, vectors); }
	 * catch(NullPointerException e) { System.out.println("null pointer at " +
	 * counter); } } }
	 */
	public void read_word2vec_file() throws IOException {
		File test = new File("C:\\Users\\Ruben\\PycharmProjects\\Word2Vec(2.0)\\refined_model.json");
		HashMap<String,List> result = new ObjectMapper().readValue(test, HashMap.class);
		//double[] test2 = result.get("bad");
		List<Double> test2 = new ArrayList<Double>(result.get("good"));
		List<Double> test3 = new ArrayList<Double>(result.get("amazing"));
		for (Map.Entry<String, List> entry : result.entrySet()) {
			String word = entry.getKey();
			List<Double> vector_prem = new ArrayList<Double>(entry.getValue());
			Double[] array = vector_prem.toArray(new Double[vector_prem.size()]);
			double[] word_vector = ArrayUtils.toPrimitive(array);
			word_vec_refined.put(word, word_vector);
		}
		System.out.println(get_cosine_similarity(word_vec_refined.get("overpriced"), word_vec_refined.get("bad")));
		System.out.println(get_cosine_similarity(word_vec_refined.get("overpriced"), word_vec_refined.get("decent")));

	}
	
	public void read_file(String dataset, String filelocation) throws IOException, ClassNotFoundException {
		if (dataset == "yelp") {
			File toRead_yelp=new File(filelocation);
		    FileInputStream fis_yelp=new FileInputStream(toRead_yelp);
		    ObjectInputStream ois_yelp =new ObjectInputStream(fis_yelp);
	        word_vec_yelp =(HashMap<String,double[]>)ois_yelp.readObject();
	        ois_yelp.close();
	        fis_yelp.close();	
		}
		if (dataset == "sentiment") {
			File toRead_sent=new File(filelocation);
		    FileInputStream fis_sent=new FileInputStream(toRead_sent);
		    ObjectInputStream ois_sent =new ObjectInputStream(fis_sent);
	        sentiment_mentions =(HashMap<String,Integer>)ois_sent.readObject();
	        ois_sent.close();
	        fis_sent.close();	
		}
	}
	
	
	public double get_cosine_similarity(double[] vec1, double[] vec2) {	
		return (dotProduct(vec1, vec2)/(getMagnitude(vec2) * getMagnitude(vec1)));
	}
	
	public double generate_sentiment_scores() {
		String[] negative_sentiment_seeds = {"bad", "awful", "horrible", "terrible", "poor", "lousy", "shitty", "horrid"};
		String[] positive_sentiment_seeds = {"good", "decent", "great", "tasty", "fantastic", "solid", "yummy", "terrific"};
		for (Map.Entry<String, Integer> sentiment_word : sentiment_mentions.entrySet()) {
			double[] wordembedding = word_vec_refined.get(sentiment_word.getKey());
			double max_sim_pos = 0;
			double max_sim_neg = 0;
			for (String neg_seed: negative_sentiment_seeds) {
				double[] wordvec = word_vec_refined.get(neg_seed);
				double cosine_sim = get_cosine_similarity(wordvec, wordembedding);
				if (cosine_sim > max_sim_neg) {
					max_sim_neg = cosine_sim;
				}
			}
			for (String pos_seed: positive_sentiment_seeds) {
				double[] wordvec = word_vec_refined.get(pos_seed);
				double cosine_sim = get_cosine_similarity(wordvec, wordembedding);
				if (cosine_sim > max_sim_pos) {
					max_sim_pos = cosine_sim;
				}
			}
			if (max_sim_pos>max_sim_neg) {
				System.out.println(sentiment_word.getKey() + " is a positive word, pos similarity: " + max_sim_pos + " neg similarity: " + max_sim_neg);
			}
			else {
				System.out.println(sentiment_word.getKey() + " is a negative word, pos similarity: " + max_sim_pos + " neg similarity: " + max_sim_neg);

			}
		}
		
		return 0;
	}
	public static void main(String args[]) throws IOException, ClassNotFoundException {
	SentimentScoreCalculator sent_calc = new SentimentScoreCalculator("E:\\Projects\\Eclipse Workspaces\\OntologyBuilding\\src\\main\\resources\\data\\yelp_wordvec", "E:\\Projects\\Eclipse Workspaces\\OntologyBuilding\\src\\main\\resources\\output\\sentiment_mentions");
	sent_calc.read_word2vec_file();
	sent_calc.generate_sentiment_scores();
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