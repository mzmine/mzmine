package net.sf.mzmine.modules.identification.qbixlipiddb;

import java.util.ArrayList;

class QBIXLipidDBQuery {

	
	private String name;
	private	float minSearchMZ;
	private float maxSearchMZ;
	private float tolerance;
	private String adduct;
	private float add;
	private float resolution;
	private String expected;
	private float originalMZ;
	private float originalRT;
	
	
	QBIXLipidDBQuery(	String name,
				float minSearchMZ,
				float maxSearchMZ,
				float tolerance,
				String adduct,
				float add,
				float resolution,
				String expected,
				float originalMZ,
				float originalRT ) {

		this.name = name;
		this.minSearchMZ = minSearchMZ;
		this.maxSearchMZ = maxSearchMZ;
		this.tolerance = tolerance;
		this.adduct = adduct;
		this.add = add;
		this.resolution = resolution;
		this.expected = expected;
		this.originalMZ = originalMZ; 
		this.originalRT = originalRT;		
		
	}

	
}
