package termSelector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import org.deeplearning4j.models.embeddings.WeightLookupTable;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.*;
import org.nd4j.linalg.api.ndarray.INDArray;


//https://stackoverflow.com/questions/12747946/how-to-write-and-read-a-file-with-a-hashmap/12748356

public class Word2Vec {
	private Map<String, String> AllTerms= new HashMap<String, String>();
	private Map<String, double[]> word_vec_yelp = new HashMap<String, double[]>();
	private Map<String, double[]> word_vec_google = new HashMap<String, double[]>();
	private File textFile;
	private File word2vecFile;
	private File word2vecGoogle;
	private Scanner input;
	private org.deeplearning4j.models.word2vec.Word2Vec w2vModel_yelp;
	private org.deeplearning4j.models.word2vec.Word2Vec w2vModel_google;
	
	
	public Word2Vec(String file, String w2v_loc, String google_loc) throws ClassNotFoundException, IOException {
		textFile = new File(file);
		word2vecFile = new File(w2v_loc);
		word2vecGoogle = new File(google_loc);
		readFile(textFile);
		read_word2vec(word2vecFile);
		//read_word2vec_google(word2vecGoogle);
		//create_wordvec_yelp();
		//create_wordvec_google();
		
	}
	// convert to hashmap reader DONE
	public void readFile(File textFile) throws ClassNotFoundException, IOException {
	    FileInputStream fis=new FileInputStream(textFile);
	    ObjectInputStream ois=new ObjectInputStream(fis);
        AllTerms =(Map<String, String>) ois.readObject();

        ois.close();
        fis.close();	

	}
	
	public void read_word2vec(File word2vecFile) throws FileNotFoundException{
		System.out.println("reading yelp word2vec..");

		w2vModel_yelp = WordVectorSerializer.readWord2VecModel(word2vecFile);
		System.out.println(w2vModel_yelp.similarity("food", "cuisine"));

	}
	
	public void read_word2vec_google(File word2vecGoogle) {
		System.out.println("Reading google word2vec..");

		w2vModel_google  = WordVectorSerializer.readWord2VecModel(word2vecGoogle);
		System.out.println(w2vModel_google.similarity("food", "cuisine"));
		
	}
	
	// convert to hashset iterator DONE (TEST IT THOUGH)
	public void create_wordvec_google() {
		System.out.println("Creating google word2vec..");
		for (Map.Entry<String, String> entry : AllTerms.entrySet()) {
		    if (w2vModel_google.getWordVector(entry.getKey()) != null){
		    	word_vec_google.put(entry.getKey(), w2vModel_google.getWordVector(entry.getKey()));
		    }
		}
		/*
			 * for( String word2 : AllTerms) { if (w2vModel_google.getWordVector(word2) !=
			 * null) { word_vec_google.put(word2, w2vModel_google.getWordVector(word2)); } }
			 */
	}
	// conver to hashset iterator DONE (TEST IT )
	public void create_wordvec_yelp() {		
		System.out.println("Creating yelp word2vec..");
		for (Map.Entry<String, String> entry : AllTerms.entrySet()) {
		    if (w2vModel_yelp.getWordVector(entry.getKey()) != null){
		    	word_vec_yelp.put(entry.getKey(), w2vModel_yelp.getWordVector(entry.getKey()));
		    }
		}
		
		/*
		 * for(String word : AllTerms) { //double[] wordVector =
		 * w2vModel_yelp.getWordVector("myword"); //System.out.println(wordVector); if
		 * (w2vModel_yelp.getWordVector(word) != null){ word_vec_yelp.put(word,
		 * w2vModel_yelp.getWordVector(word)); } }
		 */
	}

	
	public void save_to_file(Map<String,double[]> word_vec, String filelocation) {
		System.out.println("saving file..");
	    try {
	        File fileOne=new File(filelocation);
	        FileOutputStream fos=new FileOutputStream(fileOne);
	        ObjectOutputStream oos=new ObjectOutputStream(fos);

	        oos.writeObject(word_vec);
	        oos.flush();
	        oos.close();
	        fos.close();
	    } catch(Exception e) {}
	}
	
	public Map<String,double[]> get_vec_google() {
		return word_vec_google;
	}
	
	public Map<String,double[]> get_vec_yelp() {
		return word_vec_yelp;
	}
	
	
	// replace filelocation when done running
	public static void main(String args[]) throws IOException, ClassNotFoundException{
		Word2Vec w2v_model = new Word2Vec("E:\\OutputTerms\\Output_stanford_hashmap", "C:\\Users\\Ruben\\PycharmProjects\\Word2Vec(2.0)\\w2v_yelp.bin", "C:\\Users\\Ruben\\gensim-data\\word2vec-google-news-300\\word2vec-google-news-300.gz");
		//w2v_model.save_to_file(w2v_model.get_vec_yelp(), "E:\\yelp_wordvec");
		//w2v_model.save_to_file(w2v_model.get_vec_google(), "E:\\google_wordvec");
		Path path = Paths.get("/OntologyBuilding/src/main/resources/data");
		System.out.println(path);
		
	
	}
	
}
