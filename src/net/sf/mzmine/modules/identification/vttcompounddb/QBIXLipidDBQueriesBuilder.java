package net.sf.mzmine.modules.identification.qbixlipiddb;

import java.util.ArrayList;

import net.sf.mzmine.data.PeakListRow;

class QBIXLipidDBQueriesBuilder {

	/*
	 * Table format: min m/z, max m/z, min rt, max rt, "name", add,
	 * "adduct", "expected"
	 */
	
	private final int COL_MINMZ = 0;
	private final int COL_MAXMZ = 1;
	private final int COL_MINRT = 2;
	private final int COL_MAXRT = 3;
	private final int COL_NAME =  4;
	private final int COL_ADD = 5;
	private final int COL_ADDUCT = 6;
	private final int COL_EXPECTED = 7;
	
	private final Object[][] lipidClassLookup = {			
{0.0f,		650.0f,				0.0f,		300.0f, 			"Glycerophospholipi*",	1.007825f,				"[M+H]",		"LPC/LPE/LPA/LSer"},
{0.0f,		500.0f, 			0.0f, 		300.0f,				"Glycerolipids", 		-17.0027f,				"[(M+H)-18]",	"MAG"},
{550.0f,	Float.MAX_VALUE,	300.0f,		420.0f, 			"Glycerophospholipi*", 	1.007825f,				"[M+H]",		"GPCho/GPEtn/GPIns/GPSer"},
{340.0f,	Float.MAX_VALUE,	0.0f,		430.0f,				"Sphingolipids", 		-17.0027f,				"[(M+H)-18]",	"Cer"}, 
{0.0f,		Float.MAX_VALUE, 	330.0f, 	420.0f,				"Sphingolipids", 		1.007825f,				"[M+H]",		"SM"},  
{550.0f, 	Float.MAX_VALUE, 	0.0f, 		410.0f,				"Glycerophospholipi*", 	-96.9691f,				"[M-97]",		"GPA"},
{550.0f, 	Float.MAX_VALUE, 	0.0f,		410.0f,				"Glycerophospholipi*",	-171.0059f,				"[M-171]",		"GPGro"},		
{350.0f,	Float.MAX_VALUE, 	0.0f,		410.0f,				"Glycerolipids",		18.0344f,				"[M+18]",		"DAG"},
{0.0f,		Float.MAX_VALUE, 	410.0f,		Float.MAX_VALUE,	"Glycerolipids",		18.0344f,				"[M+18]",		"TAG"},
{550.0f,	Float.MAX_VALUE, 	350.0f,		Float.MAX_VALUE,	"Sterollipids",			18.0344f,				"[M+18]",		"ChoE"}, 
{1000.0f,	Float.MAX_VALUE, 	410.0f,		Float.MAX_VALUE,	"Glycerophospholipi*",	3*22.98977f-2*1.007825f,	"[M-2H+3Na]+",	"CL"},
{1000.0f,	Float.MAX_VALUE, 	410.0f,		Float.MAX_VALUE,	"Glycerophospholipi*",	2*22.98977f-1.007825f,	"[M-H+2Na]+",	"CL"}, 
{1000.0f,	Float.MAX_VALUE, 	410.0f,		Float.MAX_VALUE,	"Glycerophospholipi*",	22.98977f,				"[M+Na]+",		"CL"}
};

	private static Object[] NoMatchQuery = 
{0.0f,		0.0f,				0.0f,		0.0f,				"(.*)", 				1.007825,				"AUTO",			"NOIDEA"};

	
	private QBIXLipidDBSearchParameters parameters;
	
	
	QBIXLipidDBQueriesBuilder(QBIXLipidDBSearchParameters parameters) {
		this.parameters = parameters;
	}
	
	
	protected QBIXLipidDBQuery[] createQueries(PeakListRow peakListRow) {
		
		float mz = peakListRow.getAverageMZ(); 
		float rt = peakListRow.getAverageRT();
		
		ArrayList<QBIXLipidDBQuery> queries = new ArrayList<QBIXLipidDBQuery>();
			
		if (mz<=0) return new QBIXLipidDBQuery[0];
		if (rt<0) return new QBIXLipidDBQuery[0];
		
		float toleranceMZ = mz * (Float)parameters.getParameterValue(parameters.MZTolerance) / 1000000.0f;
		
		float resolution = (Float)parameters.getParameterValue(parameters.MassResolution);
		
		for (int queryNumber = 0; queryNumber<lipidClassLookup.length; queryNumber++) {
			if ( 	(mz>(Float)lipidClassLookup[queryNumber][COL_MINMZ]) &&
					(mz<(Float)lipidClassLookup[queryNumber][COL_MAXMZ]) &&
					(rt>(Float)lipidClassLookup[queryNumber][COL_MINRT]) &&
					(rt<(Float)lipidClassLookup[queryNumber][COL_MAXRT]) ) {


				float minMZ = mz - toleranceMZ - (Float)lipidClassLookup[queryNumber][COL_ADD];
				float maxMZ = mz + toleranceMZ - (Float)lipidClassLookup[queryNumber][COL_ADD];
				
				QBIXLipidDBQuery newQueryData = new QBIXLipidDBQuery(
						(String)lipidClassLookup[queryNumber][COL_NAME],
						minMZ,
						maxMZ,
						toleranceMZ,
						(String)lipidClassLookup[queryNumber][COL_ADDUCT],
						(Float)lipidClassLookup[queryNumber][COL_ADD],
						resolution,
						(String)lipidClassLookup[queryNumber][COL_EXPECTED],
						mz,
						rt);
				
				queries.add(newQueryData);
				
			}
			
		}

		return queries.toArray(new QBIXLipidDBQuery[0]);
		
	}

	
}
