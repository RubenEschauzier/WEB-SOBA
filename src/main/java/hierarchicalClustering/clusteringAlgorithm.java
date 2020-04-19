/**
/*
 * Programmed by Shephalika Shekhar
 * Modified by Joanne Tjan
 * Class for Kmeans Clustering implementation
 * 
 **/

package hierarchicalClustering;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeMap;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;

import com.apporiented.algorithm.clustering.AverageLinkageStrategy;
import com.apporiented.algorithm.clustering.Cluster;
import com.apporiented.algorithm.clustering.ClusteringAlgorithm;
import com.apporiented.algorithm.clustering.DefaultClusteringAlgorithm;

import edu.eur.absa.Framework;

public class clusteringAlgorithm{
	private Map<String, String> MentionsWords = new HashMap<String, String>();
	private Map<String, double[]> term_wordvector = new HashMap<>(); //map word with corresponding wordvector
	private Map<double[], String> wordvector_term = new HashMap<>();

	private Map<String, double[]> class_wordvector = new HashMap<>();
	private Map<double[], String> wordvector_class = new HashMap<>();

	private List<double[]> wordvectors = new ArrayList<>(); //list with only the word vectors
	private List<String> terms = new ArrayList<>();

	//Initialize variables for the kmeans approach
	private Map<Integer, double[]> centroids = new HashMap<>(); 	//Hashmap to store centroids with index
	private Map<double[], Integer> clusters = new HashMap<>(); 	//Hashmap for finding cluster indexes
	private Map<Integer, String[]> finalclusters = new HashMap<>(); //Hashmap containing terms per cluster

	//Initialize variables for the similarities approach
	private Map<String, double[]> centroids2 = new HashMap<>();
	private Map<double[], String> clusters2 = new HashMap<>();
	private Map<String, String[]> finalclusters2 = new HashMap<>(); 

	private final int numberClusters; 
	private final int maxIterations; 
	private final String filename;

	private int[] termspercluster;
	private final String[] classes;
	private final String method;

	public clusteringAlgorithm(String nameoffile, int k, int x, String[] classes, String method) {
		this.filename = nameoffile;
		this.numberClusters = k;
		this.termspercluster = new int[k];
		this.maxIterations = x;
		this.classes = classes;
		this.method = method;
		//initialize these parameters String textfile, int numberMentionClasses, int maxiterations
	}
	
	/**
	 * 
	 * @return overview of the final clusters
	 */
	public Map<String, String[]> getFinalClusters() {
		return finalclusters2;
	}
	
	public Map<String, double[]> getAspectWordVectors() {
		return term_wordvector;
	}
	
	/**
	 * Read file containing the words
	 * @param textFile: file with words
	 * @return 
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public void readFile(String filename) throws ClassNotFoundException, IOException { //change to more input dependent
		File textFile = new File(Framework.OUTPUT_PATH + filename); // in constructor
		FileInputStream fis = new FileInputStream(textFile);
		ObjectInputStream ois = new ObjectInputStream(fis);
		MentionsWords = (Map<String, String>) ois.readObject();

		ois.close();
		fis.close();
	}
	
	/**
	 * Initialize the required variables
	 */
	public void initialization() {
		//Second step
		File Model = new File(Framework.EXTERNALDATA_PATH+ "w2v_yelp.bin");// if error occurs here, change w2v_yelp.bin to filename of w2c model
		org.deeplearning4j.models.word2vec.Word2Vec word2vec = WordVectorSerializer.readWord2VecModel(Model);

		for (Map.Entry<String, String> entry : MentionsWords.entrySet()) { // Per aspectmention, find the closest subcluster (not sure it is already stored)
			double[] tempwordvector = word2vec.getWordVector(entry.getKey());
			//			System.out.println("Aspectmention word: "+entry.getKey());
			//			System.out.println("With similarity : " + Arrays.toString(tempwordvector));

			term_wordvector.put(entry.getKey(), tempwordvector);
			wordvector_term.put(tempwordvector, entry.getKey());

			wordvectors.add(tempwordvector);
			terms.add(entry.getKey());
		}

		for (String mentionclasses : classes ) {
			double[] wordvector = word2vec.getWordVector(mentionclasses);
			class_wordvector.put(mentionclasses, wordvector);
			wordvector_class.put(wordvector, mentionclasses);
		}
	}

	/**
	 * Implementation of Kmeans
	 */
	public void implementKmeans() {
		// third step
		for (int i = 0; i < numberClusters; i++) { //check whether same numbers will not be chosen;
			//			int number = (int) Math.floor(terms.size()/numberClusters);
			//			System.out.println(terms.get(number));
			String chosenterm = terms.get(i*19);
			double[] chosenwordvector = term_wordvector.get(chosenterm);
			centroids.put(i,chosenwordvector);
		}

		clusters = kmeans(wordvectors, centroids);
		double[] dummy = new double[wordvectors.get(0).length];

		for (int x = 0; x < maxIterations; x++) {
			for (int j = 0; j < numberClusters; j++) {
				List<double[]> list = new ArrayList<>();
				for (double[] key : clusters.keySet()) {
					if (clusters.get(key)==j) {
						list.add(key);

					}
				}
				dummy = centroidCalculator(list);
				centroids.put(j, dummy);
			}

			clusters.clear();
			clusters = kmeans(wordvectors, centroids);
		}
	}
	
	/**
	 * Implementation of assigning terms to the mentionclass with the highest similarity
	 */
	public void implementSimilarities() {
		//Alternative method instead of kmeans
		for (String mentionclasses : classes) {
			centroids2.put(mentionclasses, class_wordvector.get(mentionclasses));
		}

		for (String term: terms) {
			TreeMap<Double, String> exampleranking = getRankinglist(term, term_wordvector);
			String finalclass = exampleranking.get(exampleranking.lastKey());
			clusters2.put(term_wordvector.get(term), finalclass);
		}
	}
	
	/**
	 * Return an overview of the clusters
	 */
	public void showfinalclusters() {
		//fourth step
		if (method == "kmeans") {
			termspercluster = getTermsPerCluster(clusters, clusters2);
			finalclusters = getClusters1(wordvector_term,clusters);
			for (Map.Entry<Integer,String[]> entry : finalclusters.entrySet()) {
				System.out.println("Cluster: "+entry.getKey()+" with terms: "+ Arrays.toString(entry.getValue())+". Total of "+termspercluster[entry.getKey()]+" terms");
			}
		}

		if (method == "similarities") {
			termspercluster = getTermsPerCluster(clusters, clusters2);
			finalclusters2 = getClusters2(wordvector_term,clusters2);
			for (Map.Entry<String, String[]> entry: finalclusters2.entrySet()) {
				System.out.println("MentionClass: "+entry.getKey()+" with terms: "+ Arrays.toString(entry.getValue()));
			}
		}
	}
	
	/**
	 * User input to check the quality of clustering
	 */
	public void userinput() {
		// fifth step
		Scanner sc = new Scanner(System.in);
		int[] userinput = new int[numberClusters];
		for (int i = 0; i < numberClusters; i++) {
			userinput[i] = 1;
		}
		int[] correctinput = new int[numberClusters];

		if (method == "kmeans"){
			while( Arrays.equals(correctinput,userinput) == false) {
				// put updated clusters here
				termspercluster = getTermsPerCluster(clusters, clusters2);
				finalclusters = getClusters1(wordvector_term,clusters);

				for (int i = 0; i < numberClusters; i++) {
					String[] selected_terms = finalclusters.get(i);
					System.out.println("Cluster "+i+": "+Arrays.toString(selected_terms));
					System.out.println("Does cluster "+i+" contain the right terms? Press (y) for yes and (n) for no");
					String answer_rightcluster = sc.next();
					if (answer_rightcluster.equals("y")) {
						userinput[i] = 0;
					}
					else if (answer_rightcluster.equals("n")) {
						for (String word : selected_terms) {
							System.out.println("Does "+word+" belong in cluster "+i+"? Press (y) for yes and (n) for no");
							String answer_rightword = sc.next();
							if (answer_rightword.equals("n")) {
								for (int j = 0; j < numberClusters; j++) {
									String[] other_terms = finalclusters.get(j);
									if( j != i) {
										System.out.println("Cluster "+j+": "+Arrays.toString(other_terms));
										System.out.println("Does "+word+ " belong in cluster "+j+"? Terms in cluster "+j+ " are given above");
										System.out.println("Press (y) for yes and (n) for no");
										String answer_moveterm = sc.next();
										if (answer_moveterm.equals("y")) {
											clusters.replace(term_wordvector.get(word),i,j);
											System.out.println(word+ " has successfully been removed from cluster "+i+" and moved to cluster "+j);
											break;
										}
										else if (answer_moveterm.equals("n")){
											continue;
										}
										else { //give error
											System.out.println("Invalid input");
										}
									}
									else {
										continue;
									}
								}
							}
							else if (answer_rightword.equals("y")){
								continue;
							}
							else { //give error
								System.out.println("Invalid input");
							}
						}	
					}

					else {
						System.out.println("Invalid input");
					}
				}
			}
		}

		if (method == "similarities") {
			while( Arrays.equals(correctinput,userinput) == false) {
				termspercluster = getTermsPerCluster(clusters, clusters2);
				finalclusters2 = getClusters2(wordvector_term,clusters2);


				for (int i = 0; i < classes.length; i++) {
					String[] selected_terms = finalclusters2.get(classes[i]);
					System.out.println("MentionClass "+classes[i]+": "+Arrays.toString(selected_terms));
					System.out.println("Does MentionClass "+classes[i]+" contain the right terms? Press (y) for yes and (n) for no");
					String answer_rightcluster = sc.next();
					if (answer_rightcluster.equals("y")) {
						userinput[i] = 0;
					}

					else if (answer_rightcluster.equals("n")) {
						for (String word : selected_terms) {
							System.out.println("Does "+word+" belong in Mentionclass "+classes[i]+"? Press (y) for yes and (n) for no");
							String answer_rightword = sc.next();
							if (answer_rightword.equals("n")) {
								for (int j = 0; j < classes.length; j++) {
									String[] other_terms = finalclusters2.get(classes[j]);
									if( j!=i) {
										System.out.println("Mentionclass "+classes[j]+": "+Arrays.toString(other_terms));
										System.out.println("Does "+word+ " belong in Mentionclass "+classes[j]+"? Terms in Mentionclass "+classes[j]+ " are given above");
										System.out.println("Press (y) for yes and (n) for no");
										String answer_moveterm = sc.next();
										if (answer_moveterm.equals("y")) {
											clusters2.replace(term_wordvector.get(word),classes[i],classes[j]);
											System.out.println(word+ " has successfully been removed from Mentionclass "+classes[i]+" and moved to Mentionclass "+classes[j]);
											break;
										}
										else if (answer_moveterm.equals("n")){
											continue;
										}
										else { //give error
											System.out.println("Invalid input");
										}
									}
									else {
										continue;
									}
								}
							}
							else if (answer_rightword.equals("y")){
								continue;
							}
							else { //give error
								System.out.println("Invalid input");
							}
						}	
					}

					else {
						System.out.println("Invalid input");
					}
				}
			}
		}

		System.out.println("End results of clustering after user input: ");
		showfinalclusters();
	}
	
	/**
	 * Order of clustering using kmeans approach
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public void clusteringKMeans() throws ClassNotFoundException, IOException {
		readFile(filename);
		initialization();
		implementKmeans();
		showfinalclusters();
		userinput();
	}
	
	/**
	 * Order of clustering using similarities approach
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public void clusteringsimilarities() throws ClassNotFoundException, IOException {
		readFile(filename);
		initialization();
		implementSimilarities();
		showfinalclusters();
		userinput();
	}
	
	/**
	 * Calculate distance using the Euclidean distance given two words
	 * @param term1: wordvector of first term
	 * @param term2: wordvector of second term
	 * @return distance between two terms
	 */
	public double getEuclideanDistance(double[] term1, double[] term2) {
		double distance = 0;
		for (int i = 0; i< term1.length; i++){
			distance += Math.pow(term1[i]-term2[i],2);
		}
		return Math.sqrt(distance);
	}
	
	/**
	 * Calculate the cosine similarity given two words
	 * @param term1
	 * @param term2
	 * @return
	 */
	public double getCosineSimilarity(double[] term1, double[] term2) {
		double sum = 0;
		double sqrt1 = 0;
		double sqrt2 = 0;
		for (int i = 0; i < term1.length; i++) {
			sum += term1[i]*term2[i];
			sqrt1 += term1[i] * term1[i];
			sqrt2 += term2[i] * term2[i];
		}
		return sum / (Math.sqrt(sqrt1) * Math.sqrt(sqrt2));
	}
	
	/**
	 * Calculating the centroids of a cluster
	 * @param cluster: list of the wordvectors of the terms in one cluster
	 * @return centroid of the given cluster
	 */
	public double[] centroidCalculator(List<double[]> cluster) {
		int length = cluster.get(0).length;
		double[] Centroid = new double[length];
		double sum = 0.0;
		int count = 0;
		for (int i = 0; i < length;i++) {
			sum = 0.0;
			for (double[] j: cluster) {
				count++;
				sum+=j[i];
			}
			Centroid[i]=sum/count;
		}

		return Centroid;
	}
	
	/**
	 * Implementing kmeans algorithm
	 * @param wordvectors: list of the wordvectors of each term
	 * @param centroids: list of centroids of all clusters
	 * @param k: number of clusters
	 * @return list of wordvectors in corresponding cluster
	 */
	public Map<double[], Integer> kmeans(List<double[]> wordvectors, Map<Integer, double[]> centroidspoint) {
		Map<double[], Integer> cluster = new HashMap<>();
		int k1 = 0;
		double dist = 0.0;
		for(double[] x : wordvectors) {
			double minimum = 999999.0;
			for (int j = 0; j < numberClusters; j++) {
				dist = getEuclideanDistance(centroidspoint.get(j), x);

				if (dist < minimum) {
					minimum = dist;
					k1 = j;
				}		
			}
			cluster.put(x, k1);
		}
		return cluster;
	}
	
	/**
	 * Show the terms in clusters more clearly after clustering
	 * @param w2v_aspects: terms with corresponding wordvector
	 * @param finalcluster: list of terms and in which cluster it contains
	 * @param k: number of clusters
	 * @param terms: list of number of terms per cluster
	 * @return list of terms per cluster
	 */
	public Map<Integer, String[]> getClusters1(Map<double[], String> w2v_aspects,Map<double[],Integer> finalcluster1) {
		Map<Integer, String[]> compactclusters = new HashMap<>();
		for (int x = 0; x < numberClusters; x++) {
			String[] temporary_words = new String[termspercluster[x]];
			int count = 0;
			for (Map.Entry<double[], Integer > entry : finalcluster1.entrySet()) {
				if (entry.getValue() == x) {
					temporary_words[count] = w2v_aspects.get(entry.getKey());
					count++;
				}
				compactclusters.put(x, temporary_words);
			}
		}
		return compactclusters;
	}
	
	/**
	 * Show the terms in mentionclasses more clearly afterimplementation
	 * @param w2v_aspects
	 * @param finalclusters2
	 * @return
	 */
	public Map<String, String[]> getClusters2(Map<double[], String> w2v_aspects, Map<double[], String> finalclusters2) {
		Map<String, String[]> compactclusters = new HashMap<>();
		for (int x = 0; x < classes.length; x++) {
			String[] temporary_words = new String[termspercluster[x]];
			int count = 0;
			for (Map.Entry<double[], String > entry : finalclusters2.entrySet()) {
				if (entry.getValue() == classes[x]) {
					temporary_words[count] = w2v_aspects.get(entry.getKey());
					count++;
				}
				compactclusters.put(classes[x], temporary_words);
			}
		}
		return compactclusters;

	}

	/**
	 * Calculate the distancematrix in preparation for hierarchical clustering
	 * @param terms: list of terms 
	 * @param aspect_wordvector: map of terms with corresponding wordvector
	 * @return distancematrix
	 */
	public double[][] getDistanceMatrix(String[] terms, Map<String, double[]> aspect_wordvector) {
		int totalterms = terms.length;
		double[][] distancematrix = new double[totalterms][totalterms];
		for (int i = 0; i < totalterms; i++) {
			for (int j = 0; j < totalterms; j++) {
				double[] rowterm = aspect_wordvector.get(terms[i]);
				double[] columnterm = aspect_wordvector.get(terms[j]);
				double distance = getEuclideanDistance(rowterm, columnterm);
				distancematrix[i][j] = distance;
			}
		}
		return distancematrix;
	}
	
	/**
	 * Calculate the number of terms per cluster
	 * @param cluster
	 * @return array of integers
	 */
	public int[] getTermsPerCluster(Map<double[], Integer> cluster1, Map<double[], String> cluster2) {
		int[] results = new int[numberClusters];

		if (method == "kmeans") {
			for (int j = 0; j < numberClusters; j++) {
				for (Map.Entry<double[], Integer > entry : cluster1.entrySet()) {
					if (entry.getValue() == j) {
						results[j]++;
					}
				}
			}
		}

		if (method == "similarities") {
			for (int j = 0; j < classes.length ; j++) {
				for (Map.Entry<double[], String> entry: cluster2.entrySet()) {
					if (entry.getValue() == classes[j]) {
						results[j]++;
					}
				}
			}
		}
		return results;
	}
	
	/**
	 * Construct a ranking list of the most compatible mentionclasses for one term
	 * @param term, selected term
	 * @param map, term with the corresponding wordvector
	 * @return ranking list
	 */
	public TreeMap<Double, String> getRankinglist(String term, Map<String,double[]> map) {// ranking for one term
		// term = woord, map = term with its corresponding wordvector
		TreeMap<Double, String> ranklist= new TreeMap<Double,String>();
		double[] termvector = map.get(term);
		for (String mentionclasses : classes) {
			double[] classvector = centroids2.get(mentionclasses);
			double distance = getCosineSimilarity(termvector,classvector);
			ranklist.put(distance, mentionclasses);
		}
		return ranklist;
	}

	public static void main(String[] args)throws Exception {
		Scanner scanner = new Scanner(System.in);

		String[] mentionclasses = {"restaurant","ambience","service","location","food","drinks","price","quality","style","options"};
		String[] sentimentclasses = {"positive","negative"};
		int numberofclusters1 = mentionclasses.length;
		int numberofclusters2 = sentimentclasses.length;
		int iterations = 100;
		String name = "aspect_mentions";
		String name2 = "sentiment_mentions";
		String approach1 = "similarities";
		String approach2 = "kmeans";

		clusteringAlgorithm test1 = new clusteringAlgorithm(name, numberofclusters1, iterations, mentionclasses, approach1);
		test1.clusteringsimilarities();

		Map<String, String[]> Clusters = test1.getFinalClusters();
		Map<String, double[]> aspectWordvector = test1.getAspectWordVectors();

		for (Map.Entry<String, String[]> entry : Clusters.entrySet()) {
			HierarichalClusterAlgorithm HCA = new HierarichalClusterAlgorithm(Framework.EXTERNALDATA_PATH + "yelp_wordvec",  Framework.OUTPUT_PATH + name); //if error occurs at this line, change pathfile to the wanted file (not sure which file needed)
			ClusteringAlgorithm clustering_algorithm = new DefaultClusteringAlgorithm();

			String[] terms = entry.getValue();
			double[][] distances = HCA.getDistanceMatrix(terms, aspectWordvector);
			Cluster cluster = clustering_algorithm.performClustering(distances, terms, new AverageLinkageStrategy());
			int recursion = HCA.recursion_depth(cluster);
			
			HCA.elbow_method(recursion, cluster);
			HCA.make_plot();

			System.out.print("Please enter the most optimal depth of MentionClass "+ entry.getKey()+" under "+ recursion);
			int depth = scanner.nextInt();

			System.out.println("Hierarchy of the MentionClass: "+entry.getKey());
			HCA.rename_subclusters(depth, 0, cluster);
			HCA.create_cluster_representation(cluster, 0, depth);
			Map<String,List<String>> clusterRepresentation = HCA.getClusterRepresentation();
			System.out.println(clusterRepresentation);

			for (Map.Entry<String, List<String>> entry2 : clusterRepresentation.entrySet()) {
				// parent-child relation
				String parent = entry2.getKey();
				List<String> children = entry2.getValue();
				for (String child : children) {
					System.out.println("Parent: "+parent+" and child: "+child+ " in the MentionClass: "+entry.getKey());
					// here add the parent-child relation to the skeletal ontology
				}
			}

			//			Frame f1 = new DendrogramFrame(cluster);
			//			f1.setSize(500, 400);
			//			f1.setLocation(100, 200);
			//			HCA.make_plot();

			// Missing how to add hierarchy to the skeleton/ontologybuilder

		}
		// K means implementation test 
		//		clusteringAlgorithm test2 = new clusteringAlgorithm(name2, numberofclusters2, iterations, sentimentclasses, approach);
		//		test2.clusteringKMeans();
	}
}