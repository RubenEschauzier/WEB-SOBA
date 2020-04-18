package com.apporiented.algorithm.clustering;

import java.awt.Frame;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.apporiented.algorithm.clustering.visualization.DendrogramFrame;
import com.apporiented.algorithm.clustering.visualization.DendrogramPanel;

public class ClusterFile {
	private Map<String, Integer> aspect_mentions = new HashMap<String, Integer>();
	private List<double[]> wordvectors = new ArrayList<double[]>(); //list with only the word vectors
	private List<String> aspects = new ArrayList<String>();
	private Map<double[], String> wordvector_aspect = new HashMap<double[], String>();
	private Map<String, double[]> aspect_wordvector = new HashMap<String, double[]>();
	private List<double[]> vector_list = new ArrayList<double[]>();
	private Map<Integer,Double> elbow_data = new HashMap<Integer, Double>();
	private Map<String,List<String>> cluster_representation = new HashMap<String,List<String>>();
	
	private List<String> terms = new ArrayList<String>();
	private Map<String, double[]> word_vec_yelp = new HashMap<String, double[]>();
	
	
	public void read_file_wordvec(String dataset, String filelocation) throws IOException, ClassNotFoundException {
		if (dataset == "yelp") {
			File toRead_yelp=new File(filelocation);
		    FileInputStream fis_yelp=new FileInputStream(toRead_yelp);
		    ObjectInputStream ois_yelp =new ObjectInputStream(fis_yelp);
	        word_vec_yelp =(HashMap<String,double[]>)ois_yelp.readObject();
	        ois_yelp.close();
	        fis_yelp.close();	
		}
	}
	

	public void read_file(String filelocation) throws IOException, ClassNotFoundException {		
		File toRead_sent=new File(filelocation);
	    FileInputStream fis_sent=new FileInputStream(toRead_sent);
	    ObjectInputStream ois_sent =new ObjectInputStream(fis_sent);
	    aspect_mentions =(HashMap<String,Integer>)ois_sent.readObject();
	    ois_sent.close();
	    fis_sent.close();	
	}
	

	public void get_terms(Map<String,double[]>aspect_wordvector) {
		for (Map.Entry<String, double[]> entry : aspect_wordvector.entrySet()) { 
			terms.add(entry.getKey());
			
		}			
	}
	
	public void create_aspect_wordvec() {
		for (Map.Entry<String, Integer> entry: aspect_mentions.entrySet()) {
			if (word_vec_yelp.get(entry.getKey()) != null) {
				aspect_wordvector.put(entry.getKey(), word_vec_yelp.get(entry.getKey()));
			}
		}
	}
	
	public void create_wordvec_aspect() {
		for (Map.Entry<String, Integer> entry: aspect_mentions.entrySet()) {
			if (word_vec_yelp.get(entry.getKey()) != null) {
				wordvector_aspect.put(word_vec_yelp.get(entry.getKey()), entry.getKey());
			}
		}
	}

	
	public double get_cosine_similarity(double[] vec1, double[] vec2) {	
		return (dotProduct(vec1, vec2)/(getMagnitude(vec2) * getMagnitude(vec1)));
	}
	
	
	public int recursion_depth(Cluster cluster) {
		if (!cluster.isLeaf()){
			List<Cluster> child_clusters = cluster.getChildren();
			if (child_clusters.size() == 1) {
				return recursion_depth(cluster) + 1;
			}
			if (child_clusters.size() == 2) {
				return Math.max(recursion_depth(child_clusters.get(0)), recursion_depth(child_clusters.get(1))) + 1;
			}
		}
		
		return 0;
	}
	
	public List<double[]> get_vector_list(Cluster cluster, List<double[]> vector_list){
		if (cluster.isLeaf()){
			vector_list.add(aspect_wordvector.get(cluster.getName()));		
		}
		else {	
			List<Cluster> child_clusters = cluster.getChildren();
			if (child_clusters.size() == 1) {
				get_vector_list(child_clusters.get(0), vector_list);
			}
			if (child_clusters.size() == 2) {
				get_vector_list(child_clusters.get(0), vector_list);
				get_vector_list(child_clusters.get(1), vector_list);
			}
		}
		return vector_list;
		
	}

	
	public double calculate_WSS(Cluster cluster) { 
		List<double[]> vector_list = new ArrayList<double[]>();
		List<double[]> vectors_list = get_vector_list(cluster, vector_list);
		double [] average_vector = new double[vectors_list.get(0).length];
		
		for( int i = 0; i<vectors_list.size(); i++) {
			for (int j = 0; j < vectors_list.get(0).length; j ++) {
				average_vector[j] += vectors_list.get(i)[j];
				if (i == vectors_list.size() - 1) {
					average_vector[j] = average_vector[j] / (double)vectors_list.size();
				}
			}
		}
		double WSS = 0;
		for (int x = 0; x < vectors_list.size(); x++) {
			for (int z = 0; z < vectors_list.get(x).length; z ++) {
				WSS += Math.pow(vectors_list.get(x)[z]-average_vector[z], 2);
			}
		}
		
	  
	return WSS;
	}
	 
	
	public double get_WSS(int total_depth, int max_depth, Cluster cluster) {
		if (total_depth < max_depth) {
			List<Cluster> child_clusters = cluster.getChildren();
			if (child_clusters.size() == 1) {
				total_depth += 1;
				return get_WSS(total_depth, max_depth, child_clusters.get(0));
			}
			if (child_clusters.size() == 2) {
				total_depth += 1;
				return get_WSS(total_depth, max_depth, child_clusters.get(0)) + get_WSS(total_depth, max_depth, child_clusters.get(1));	}

		}

		return calculate_WSS(cluster);
	}
	
	public void elbow_method(int total_depth, Cluster cluster) {
		for (int i = 0; i < total_depth; i++) {
			double WSS = get_WSS(0,i, cluster);
			elbow_data.put(i, WSS);
			
			
		}
	}
	

	public void make_plot() {
		
	    final ElbowPlotter demo = new ElbowPlotter("Elbow method", elbow_data );
	    demo.pack();
	    demo.setVisible(true);
	
	}
	
	public String get_maximum_average_similarity(List<double[]> vector_list) {
		double[] max_vector = vector_list.get(0);
		double max_average_similarity = 0;
		for (double[] base_vector: vector_list) {
			double average_similarity = 0;
			int count = 0;
			for (double[] to_compare: vector_list) {
				if (!base_vector.equals(to_compare)){
					count += 1;
					average_similarity += get_cosine_similarity(base_vector, to_compare);

				}
			}
			average_similarity = average_similarity / count;
			if(average_similarity > max_average_similarity) {
				max_average_similarity = average_similarity;
				max_vector = base_vector;
			}
		}
		
		
		return wordvector_aspect.get(max_vector);
	}
	
	
	public void rename_subclusters(int cut_off_depth, int current_depth, Cluster cluster) {
		if (!cluster.isLeaf()) {
			List<double[]> vector_list = new ArrayList<double[]>();
			List<double[]> vectors_list = get_vector_list(cluster, vector_list);
			String new_name = get_maximum_average_similarity(vectors_list);
			cluster.setName(new_name);
		
			if (current_depth < cut_off_depth) {
				List<Cluster> child_clusters = cluster.getChildren();
				if (child_clusters.size() == 1) {
					rename_subclusters(cut_off_depth, current_depth + 1, child_clusters.get(0));
				}
				if (child_clusters.size() == 2) {
					rename_subclusters(cut_off_depth, current_depth + 1, child_clusters.get(0));
					rename_subclusters(cut_off_depth, current_depth + 1, child_clusters.get(1));
				}
			}
		}	
	}
	
	
	public void addToMap(String mapKey, String word_to_add) {
	      List<String> itemsList = cluster_representation.get(mapKey);
		  
		  // if list does not exist create it
	      if(itemsList == null)  {
	    	  itemsList = new ArrayList<String>(); 
	      itemsList.add(word_to_add);
	      cluster_representation.put(mapKey, itemsList); 
	      }     
	      else 
	      { // add if item is not already in list
		  if(!itemsList.contains(word_to_add)) 
			  itemsList.add(word_to_add); 
		  } 
	}
	
	
	public void leaf_node_handler(Cluster original_cluster, Cluster cluster) {
		if (!cluster.isLeaf()) {
			List<Cluster> child_clusters = cluster.getChildren();
			if (child_clusters.size() == 1) {
				leaf_node_handler(original_cluster, child_clusters.get(0));
			}
			if (child_clusters.size() == 2) {
				leaf_node_handler(original_cluster, child_clusters.get(0));
				leaf_node_handler(original_cluster, child_clusters.get(1));
			}
			
		}
		else {
			addToMap(original_cluster.getName(), cluster.getName());
		}
		
	}
	
	public void create_cluster_representation(Cluster cluster, int current_depth, int cut_off_depth) {
		if (!cluster.isLeaf()) {
				List<String> child_names = new ArrayList<String>();
				List<Cluster> child_clusters = cluster.getChildren();
				for (Cluster child: child_clusters) {
					child_names.add(child.getName());
				}
				cluster_representation.put(cluster.getName(), child_names);
				if (current_depth < cut_off_depth) {
					if (child_clusters.size() == 1) {
						create_cluster_representation(child_clusters.get(0), current_depth + 1, cut_off_depth);
					}
					if (child_clusters.size() == 2) {
						create_cluster_representation(child_clusters.get(0), current_depth + 1, cut_off_depth);
						create_cluster_representation(child_clusters.get(1), current_depth + 1, cut_off_depth);
					}
				}
				else {
					leaf_node_handler(cluster, cluster);
				}
		}
	}
	
	public static double getDistance(double[] term1, double[] term2) {
		double distance = 0;
		for (int i = 0; i< term1.length; i++){
			distance += Math.pow(term1[i]-term2[i],2);
		}
		return Math.sqrt(distance);
	}
	
	
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

	
	
	public static void main(String[] args) throws ClassNotFoundException, IOException {
		ClusterFile test = new ClusterFile();
		test.read_file_wordvec("yelp", "E:\\Projects\\Eclipse Workspaces\\OntologyBuilding\\src\\main\\resources\\data\\yelp_wordvec");
		test.read_file("E:\\Projects\\Eclipse Workspaces\\OntologyBuilding\\src\\main\\resources\\output\\aspect_mentions");
		test.create_aspect_wordvec();
		test.create_wordvec_aspect();
		test.get_terms(test.aspect_wordvector);
		
		
		ClusteringAlgorithm alg = new DefaultClusteringAlgorithm();
		String[] terms = test.terms.toArray(new String[test.terms.size()]);
		double[][] distances = getDistanceMatrix(test.terms.toArray(new String[test.terms.size()]), test.aspect_wordvector);
		String[] names = new String[terms.length];
		for(int i = 0; i < terms.length; i++) {
			names[i] = String.valueOf(i);
		}
		Cluster cluster = alg.performClustering(distances, terms, new AverageLinkageStrategy());
		int recursion = test.recursion_depth(cluster);
		test.rename_subclusters(14, 0, cluster);
		System.out.println(cluster.getName());
		test.create_cluster_representation(cluster, 0, 14);
		System.out.println(test.cluster_representation);
		test.elbow_method(recursion, cluster );
		test.make_plot();
        Frame f1 = new DendrogramFrame(cluster);
        f1.setSize(500, 400);
        f1.setLocation(100, 200);
        test.make_plot();

		// TODO Auto-generated method stub

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
