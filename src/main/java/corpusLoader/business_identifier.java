package corpusLoader;

import java.util.Map;

public class business_identifier {
	private Map attributes = null;
	private String business_id = null;
	
	public business_identifier() {
		
	}
	
	public boolean contains_key(Object key) {	
		if (attributes != null) {
			return attributes.containsKey(key);
		} else {
			return false;
		}
	}
	public Map get_attributes() {
		return this.attributes;
	}
	
	public String get_id() {
		return this.business_id;
	}
}
