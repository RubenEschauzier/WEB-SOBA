package corpusLoader;

import java.io.Reader;
import java.io.StringReader;
import java.text.BreakIterator; 
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.simple.Token;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;


// https://codereview.stackexchange.com/questions/239716/extracting-all-nouns-verbs-and-adjectives-from-a-large-text-dataset


public class Review {
	private  String text;
	private String business_id;
	private int stars;
	private ArrayList<String> listOfSentences = new ArrayList<String>();
	private ArrayList<String> pos_tags = new ArrayList<String>();
	private Map<String, String> all_terms = new HashMap<String, String>(); // change to hashmap
	private ArrayList<TaggedWord> tagged = new ArrayList<TaggedWord>();
	// private ArrayList<ArrayList<String>> listOfSentences = new ArrayList<ArrayList<String>>();
	
	public Review() {
	}
	

	/*
	 * public void get_pos_tags(MaxentTagger tagger) { Reader reader = new
	 * StringReader(text); DocumentPreprocessor dp = new
	 * DocumentPreprocessor(reader); for (List<HasWord> sentence : dp) {
	 * tagged.addAll(tagger.tagSentence(sentence)); } }
	 * 
	 * 
	 * public HashSet<String >get_adj_noun_verb_new(ArrayList<String> needed_tags){
	 * for(int i = 0; i < tagged.size(); i ++) { if
	 * (needed_tags.contains(tagged.get(i).tag())) {
	 * all_terms.add(tagged.get(i).word().toLowerCase());
	 * 
	 * } } return all_terms; }
	 */
	
	
	public Map<String, String> stanford_pipeline_tagger(StanfordCoreNLP pipeline, String pattern) {
    CoreDocument doc = new CoreDocument(text);
    pipeline.annotate(doc);
    for(int f = 0; f <doc.sentences().size(); f++) {
    	for (int d = 0; d < doc.sentences().get(f).tokens().size(); d++) {
    		String tag = doc.sentences().get(f).posTags().get(d);
    		CoreLabel word = doc.sentences().get(f).tokens().get(d);
        	if (tag.contains("VB") == true|| tag.contains("JJ") == true || tag.contains("NN") == true){
        	    // Create a Pattern object
        	    Pattern r = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        	    // Now create matcher object.
        	    Matcher m = r.matcher(word.lemma());
        	    if (!m.find() && !word.originalText().equals("")) {
         	       all_terms.put(word.lemma().toLowerCase(), tag);
        	    }
        	}
    		
    	}
    }
    return all_terms;

    
	}
	public String get_text() {
		return this.text;
	}
	
	public String get_id() {
		return this.business_id;
	}
	
	public int get_stars() {
		return this.stars;
	}
	
	
	/*
	 * 
	 * public ArrayList<String> get_sentences(){ BreakIterator iterator =
	 * BreakIterator.getSentenceInstance(Locale.US); iterator.setText(text); int
	 * start = iterator.first(); for (int end = iterator.next(); end !=
	 * BreakIterator.DONE; start = end, end = iterator.next()) {
	 * listOfSentences.add(text.substring(start,end).toLowerCase().replace("\r\n",
	 * " ").replace("\n", " ").replaceAll("\\p{Punct}", "")); } return
	 * listOfSentences; }
	 */
	
	
	/*
	 * public ArrayList<String> pos_tagger(MaxentTagger tagger){ // MaxentTagger
	 * tagger = new MaxentTagger(
	 * "C:\\Users\\Ruben\\git\\Heracles\\stanford-postagger-2018-10-16\\models\\english-bidirectional-distsim.tagger"
	 * ); for (int i = 0; i <listOfSentences.size(); i ++) { String tagged =
	 * tagger.tagString(listOfSentences.get(i)); pos_tags.add(tagged); } return
	 * pos_tags; }
	 */
	
	/*
	 * public HashSet<String> get_adj_verb_noun() { for( int j = 0;
	 * j<pos_tags.size(); j++) { Pattern regex = Pattern.compile(
	 * "\\b(\\w+(?:_VB|_VBD|_VBG|_VBN|_VBP|_VBZ|_VH|_VHD|_VHG|_VHN|_VHP|_VHZ|_VV|_VVD|_VVG|_VVN|_VVP|_VVZ|_JJ|_JJR|_JJS|_NN|_NNS|_NP|_NPS|_))\\b"
	 * ); Matcher regexMatcher = regex.matcher(pos_tags.get(j)); while
	 * (regexMatcher.find()) { String lemma = new
	 * Sentence(regexMatcher.group(1).split("_")[0]).lemma(0); //
	 * System.out.println(regexMatcher.group(1).split("_")[0]);
	 * all_terms.add(lemma); // match end: regexMatcher.end() } } return all_terms;
	 * }
	 */
}
