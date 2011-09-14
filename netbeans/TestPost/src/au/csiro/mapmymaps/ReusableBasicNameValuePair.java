/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.csiro.mapmymaps;

import org.apache.http.NameValuePair;

/**
 *
 * @author abd01c
 */
public class ReusableBasicNameValuePair implements NameValuePair {
	
	private final String name;
	private String value;

	public ReusableBasicNameValuePair(String name, String value) {
		this.name = name;
		this.value = value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
}
