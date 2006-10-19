package net.sf.mzmine.data.impl;

import net.sf.mzmine.data.ParameterValue;

public class SimpleParameterValue implements ParameterValue {

	private Object value;
	
	public SimpleParameterValue(Object value) {
		this.value = value;
	}
	
	public Object getValue() {
		return value;
	}

}
