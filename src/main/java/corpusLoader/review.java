package corpusLoader;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class review {
	private  String text;
	private String business_id;
	private int stars;
	private ArrayList<String> listOfSentences = new ArrayList<String>();
	private ArrayList<String> pos_tags = new ArrayList<String>();
	private ArrayList<String> all_terms = new ArrayList<String>();
	// private ArrayList<ArrayList<String>> listOfSentences = new ArrayList<ArrayList<String>>();
	
	public review() {
	}
	
	public ArrayList<String> get_sentences(){
		BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.US);
		iterator.setText(text);
		int start = iterator.first();
		for (int end = iterator.next();
		    end != BreakIterator.DONE;
		    start = end, end = iterator.next()) {
		  listOfSentences.add(text.substring(start,end).toLowerCase().replace("\r\n", " ").replace("\n", " ").replaceAll("\\p{Punct}", ""));
		}
		
		return listOfSentences;
	}
	
	public ArrayList<String> pos_tagger(MaxentTagger tagger){
		// MaxentTagger tagger = new MaxentTagger("C:\\Users\\Ruben\\git\\Heracles\\stanford-postagger-2018-10-16\\models\\english-bidirectional-distsim.tagger");
		for (int i = 0; i <listOfSentences.size(); i ++) {
			String tagged = tagger.tagString(listOfSentences.get(i));
			pos_tags.add(tagged);
		}
		return pos_tags;	
	}
	
	public ArrayList<String> get_adj_verb_noun() {
		for( int j = 0; j<pos_tags.size(); j++) {
				Pattern regex = Pattern.compile("\\b(\\w+(?:_VB|_VBD|_VBG|_VBN|_VBP|_VBZ|_VH|_VHD|_VHG|_VHN|_VHP|_VHZ|_VV|_VVD|_VVG|_VVN|_VVP|_VVZ|_JJ|_JJR|_JJS|_NN|_NNS|_NP|_NPS|_))\\b");
			    Matcher regexMatcher = regex.matcher(pos_tags.get(j));
			    while (regexMatcher.find()) {
			        all_terms.add(regexMatcher.group(1).split("_")[0]);
			        // match end: regexMatcher.end()	
			    }
		    } 
		// System.out.println(all_terms);
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
}
