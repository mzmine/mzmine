package net.sf.mzmine.data.impl;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakListAppliedMethod;

public class SimplePeakListAppliedMethod implements
		PeakListAppliedMethod {

	private String description;
	private ParameterSet parameters;

	public SimplePeakListAppliedMethod(String description,
			ParameterSet parameters) {
		this.description = description;
		this.parameters = parameters;
	}

	public SimplePeakListAppliedMethod(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public String toString() {
		return description;
	}

	public ParameterSet getParameterSet() {
		return parameters;
	}

}
