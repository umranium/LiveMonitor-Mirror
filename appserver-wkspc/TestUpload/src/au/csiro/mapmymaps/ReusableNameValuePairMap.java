/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.csiro.mapmymaps;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author abd01c
 */
public class ReusableNameValuePairMap {
	
	private final Comparator<String> NAME_COMPARATOR = new Comparator<String>() {
		@Override
		public int compare(String o1, String o2) {
			return o1.compareToIgnoreCase(o2);
		}
	};
	private List<ReusableBasicNameValuePair> nameValuePairs;
	private Map<String, ReusableBasicNameValuePair> map;

	public ReusableNameValuePairMap() {
		this.nameValuePairs = new ArrayList<ReusableBasicNameValuePair>();
		this.map = new TreeMap<String, ReusableBasicNameValuePair>(NAME_COMPARATOR);
	}

	public void add(String name, String value) {
		ReusableBasicNameValuePair pair = new ReusableBasicNameValuePair(name, value);
		nameValuePairs.add(pair);
		map.put(name, pair);
	}

	public void update(String name, String value) {
		ReusableBasicNameValuePair pair = map.get(name);
		if (pair == null) {
			throw new RuntimeException("Unknown parameter name '" + name + "'");
		}
		pair.setValue(value);
	}

	public List<ReusableBasicNameValuePair> getPairs() {
		return nameValuePairs;
	}
	
}
