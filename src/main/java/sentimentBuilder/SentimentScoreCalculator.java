package sentimentBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;

public class SentimentScoreCalculator {
	private Map<String, double[]> word_vec_yelp = new HashMap<String, double[]>();
	private Map<String, double[]> word_vec_google = new HashMap<String, double[]>();
	
	public SentimentScoreCalculator(String fileloc_google, String fileloc_yelp) throws ClassNotFoundException, IOException{
		read_file("google", fileloc_google);
		read_file("yelp", fileloc_yelp);
		
		
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
	        ois_yelp.close();
	        fis_yelp.close();	
		}
	}
	
	public double get_cosine_similarity(double[] vec1, double[] vec2) {	
		return (dotProduct(vec1, vec2)/(getMagnitude(vec2) * getMagnitude(vec1)));
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
