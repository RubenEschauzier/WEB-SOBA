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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Random;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;

import com.tdunning.math.stats.Centroid;

import org.apache.commons.math3.ml.clustering.Clusterer;
import org.deeplearning4j.clustering.algorithm.BaseClusteringAlgorithm;
import org.deeplearning4j.clustering.algorithm.Distance;
import org.deeplearning4j.clustering.cluster.Point;
import org.deeplearning4j.clustering.kmeans.KMeansClustering;

import java.util.function.ToDoubleBiFunction;

public class clusteringAlgorithm{
	private static Map<String, String> aspectMentions = new HashMap<String, String>();
	private static Map<String, String> sentimentMentions = new HashMap<String, String>();
	private static Map<String, double[]> aspect_wordvector = new HashMap<>(); //map word with corresponding wordvector
	private static List<double[]> wordvectors = new ArrayList<>(); //list with only the word vectors
	private static List<String> aspects = new ArrayList<>();
	private static Map<double[], String> wordvector_aspect = new HashMap<>();
	

	public clusteringAlgorithm() {
	}
	/**
	 * Read file containing the aspectmention words
	 * @param textFile: file with aspectmention words
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public void readFile(File textFile) throws ClassNotFoundException, IOException {
		
		
		FileInputStream fis = new FileInputStream(textFile);
		ObjectInputStream ois = new ObjectInputStream(fis);
		aspectMentions = (Map<String, String>) ois.readObject();

		ois.close();
		fis.close();
	} 
	
	
	/**
	 * Calculate distance using the Euclidean distance given two words
	 * @param term1: wordvector of first term
	 * @param term2: wordvector of second term
	 * @return distance between two terms
	 */
	public static double getDistance(double[] term1, double[] term2) {
		double distance = 0;
		for (int i = 0; i< term1.length; i++){
			distance += Math.pow(term1[i]-term2[i],2);
		}
		return Math.sqrt(distance);
	}

	/**
	 * Calculating the centroids of a cluster
	 * @param cluster: list of the wordvectors of the terms in one cluster
	 * @return centroid of the given cluster
	 */
	public static double[] centroidCalculator(List<double[]> cluster) {
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
	public static Map<double[], Integer> kmeans(List<double[]> wordvectors, Map<Integer, double[]> centroids, int k) {
		Map<double[], Integer> cluster = new HashMap<>();
		int k1 = 0;
		double dist = 0.0;
		for(double[] x : wordvectors) {
			double minimum = 999999.0;
			for (int j = 0; j < k; j++) {
				dist = getDistance(centroids.get(j), x);

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
	 * Show the terms in clusters more clearly
	 * @param w2v_aspects: terms with corresponding wordvector
	 * @param finalcluster: list of terms and in which cluster it contains
	 * @param k: number of clusters
	 * @param terms: list of number of terms per cluster
	 * @return list of terms per cluster
	 */
	public static Map<Integer, String[]> Clustercleaning(Map<double[], String> w2v_aspects,Map<double[],Integer> finalcluster, int k, int[] terms) {
		Map<Integer, String[]> compactclusters = new HashMap<>();
		for (int x = 0; x < k; x++) {
			String[] temporary_words = new String[terms[x]];
			int count = 0;
			for (Map.Entry<double[], Integer > entry : finalcluster.entrySet()) {
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
	 * Calculate the distancematrix in preparation for hierarchical clustering
	 * @param terms: list of terms 
	 * @param aspect_wordvector: map of terms with corresponding wordvector
	 * @return distancematrix
	 */
	public static double[][] getDistanceMatrix(String[] terms, Map<String, double[]> aspect_wordvector) {
		int totalterms = terms.length;
		double[][] distancematrix = new double[totalterms][totalterms];
		for (int i = 0; i < totalterms; i++) {
			for (int j = 0; j < totalterms; j++) {
				double[] rowterm = aspect_wordvector.get(terms[i]);
				double[] columnterm = aspect_wordvector.get(terms[j]);
				double distance = getDistance(rowterm, columnterm);
				distancematrix[i][j] = distance;
			}
		}
		return distancematrix;
	}
	
	public static int[] getTermsPerCluster(Map<double[], Integer> cluster, int k) {
		int[] results = new int[k];
		for (int j = 0; j < k; j++) {
			for (Map.Entry<double[], Integer > entry : cluster.entrySet()) {
				if (entry.getValue() == j) {
					results[j]++;
				}
			}
		}
		return results;
	}

	public static void main(String[] args)throws Exception {
		Scanner sc = new Scanner(System.in);
		clusteringAlgorithm test = new clusteringAlgorithm();
		File textFile = new File("aspect_mentions");
		test.readFile(textFile);

		File Model = new File("w2v_yelp.bin");
		org.deeplearning4j.models.word2vec.Word2Vec word2vec = WordVectorSerializer.readWord2VecModel(Model);


		for (Map.Entry<String, String> entry : aspectMentions.entrySet()) { // Per aspectmention, find the closest subcluster (not sure it is already stored)
			double[] tempwordvector = word2vec.getWordVector(entry.getKey());
			//			System.out.println("Aspectmention word: "+entry.getKey());
			//			System.out.println("With similarity : " + Arrays.toString(tempwordvector));

			aspect_wordvector.put(entry.getKey(), tempwordvector);
			wordvector_aspect.put(tempwordvector, entry.getKey());

			wordvectors.add(tempwordvector);
			aspects.add(entry.getKey());
		}

		// START KMEANS 
		System.out.println("Start first phase of term clustering");
		int numberClusters = 8;
		int maxIterations = 10;

		//Hashmap to store centroids with index
		Map<Integer, double[]> centroids = new HashMap<>();
		//Hashmap for finding cluster indexes
		Map<double[], Integer> clusters = new HashMap<>();

		for (int i = 0; i < numberClusters; i++) { //check whether same numbers will not be chosen;
			String chosenterm = aspects.get(i*24);
			double[] chosenwordvector = aspect_wordvector.get(chosenterm);
			centroids.put(i,chosenwordvector);
		}

		clusters = kmeans(wordvectors, centroids, numberClusters);
		double[] dummy = new double[wordvectors.get(0).length];
		double previousWCSS = 99999999999.99;
		int stopcriteria = 0;
		int count = 0;

		for (int x = 0; x < maxIterations; x++) {
			count++;
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
			clusters = kmeans(wordvectors, centroids, numberClusters);

			double nextWCSS = 0.0;

			for(int i=0; i < numberClusters; i++){
				double sse=0;
				for (double[] key : clusters.keySet()) {
					if (clusters.get(key)==i) {
						sse+=getDistance(key, centroids.get(i));
					}
				}
				nextWCSS+=sse;
			}

			//			System.out.println("previous wcss: "+previousWCSS+" and next wcss: "+nextWCSS);
			//			previousWCSS = nextWCSS;
		}
		int[] termspercluster = getTermsPerCluster(clusters, numberClusters);
		Map<Integer, String[]> finalCluster = Clustercleaning(wordvector_aspect,clusters,numberClusters,termspercluster);

		for (Map.Entry<Integer,String[]> entry : finalCluster.entrySet()) {
			System.out.println("Cluster: "+entry.getKey()+" with terms: "+ Arrays.toString(entry.getValue())+". Total of "+termspercluster[entry.getKey()]+" terms");
		}

		System.out.println("User input now required for check");

		int[] userinput = new int[numberClusters];
		for (int i = 0; i < numberClusters; i++) {
			userinput[i] = 1;
		}
		int[] correctinput = new int[numberClusters];

		while( Arrays.equals(correctinput,userinput) == false) {
			// put updated clusters here
			for (int i = 0; i < numberClusters; i++) {
				String[] selected_terms = finalCluster.get(i);
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
								String[] other_terms = finalCluster.get(j);
								if( j != i) {
									System.out.println("Cluster "+j+": "+Arrays.toString(other_terms));
									System.out.println("Does "+word+ " belong in cluster "+j+"? Terms in cluster "+j+ " are given above");
									System.out.println("Press (y) for yes and (n) for no");
									String answer_moveterm = sc.next();
									if (answer_moveterm.equals("y")) {
										clusters.replace(aspect_wordvector.get(word),i,j);
										System.out.println(word+ " has successfully been removed from cluster "+i+" to cluster "+j);
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

				//					System.out.println(Arrays.toString(selected_terms));
				//					System.out.println("Does cluster "+i+" contain the right terms? Press (y) for yes and (n) for no");
				//					String answer_correction = sc.nextLine();
				//					if (answer_correction.equals("y")) {
				//						userinput[i] = 0;
				//					}
				//					else if(answer_correction.equals("n")){
				//						continue;
				//					}
				//					else {
				//						System.out.println("Invalid input");
				//					}

			}
		}
		int[] termspercluster2 =  getTermsPerCluster(clusters, numberClusters);
		Map<Integer, String[]> CorrectCluster = Clustercleaning(wordvector_aspect,clusters,numberClusters,termspercluster2);

		System.out.println("Before correction:");
		for (Map.Entry<Integer,String[]> entry : finalCluster.entrySet()) {
			System.out.println("Cluster: "+entry.getKey()+" with terms: "+ Arrays.toString(entry.getValue())+". Total of "+termspercluster[entry.getKey()]+" terms");
		}
		System.out.println("After correction:");
		for (Map.Entry<Integer,String[]> entry : CorrectCluster.entrySet()) {
			System.out.println("Cluster: "+entry.getKey()+" with terms: "+ Arrays.toString(entry.getValue())+". Total of "+termspercluster2[entry.getKey()]+" terms");
		}
		System.out.println("End of user input of the first phase");	
		// User input, check whether the clusters are in the correct cluster
		// Start hierarchical clustering per cluster
		System.out.println("Start second phase of building a hierarchy for each cluster");

		//		Tree<Integer, String> tree = new Tree<>(1, "Bob");
		//
		//		tree.addChild(1, 2, "John");
		//		tree.addChild(1, 3, "James");
		//		tree.addChild(2, 4, "David");
		//		tree.addChild(2, 5, "Alice");
		//
		//		System.out.println(tree.subtreeToString(1));
		//		System.out.println(tree.subtreeToString(2));

		//		List<Double> vals= Arrays.asList(1d,2d,10d); //your data
		//		Linkage la = new AverageLinkage<Double>((o, o2) -> Math.abs(o-o2));  //lamda distance function
		//		ClusterAlgorithm<Double> h = new ClusterAlgorithm<Double>(vals,la); 
		//		h.cluster(); //run 
		//		System.out.println(h.getFirstCluster());
		//		System.out.println("Top Cluster distance: "+h.getFirstCluster().distance);

		//example use cluster 4
		String[] names = finalCluster.get(4);
		double[][] dummymatrix = new double[names.length][names.length];
		dummymatrix = getDistanceMatrix(names, aspect_wordvector);

		//		int terms = names.length;
		//		DissimilarityMeasure dissimilarityMeasure = ;
		//		AgglomerationMethod agglomerationMethod = new AverageLinkage();
		//		DendrogramBuilder dendrogramBuilder = new DendrogramBuilder(terms);
		//		HierarchicalAgglomerativeClusterer clusterer = new HierarchicalAgglomerativeClusterer(terms, dissimilarityMeasure, agglomerationMethod);
		//		clusterer.cluster(dendrogramBuilder);
		//		Dendrogram dendrogram = dendrogramBuilder.getDendrogram();

		System.out.println("End of second phase");

	}
}