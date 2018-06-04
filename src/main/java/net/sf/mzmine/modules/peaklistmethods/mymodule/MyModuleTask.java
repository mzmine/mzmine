package net.sf.mzmine.modules.peaklistmethods.mymodule;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.interfaces.IIsotope;

import com.google.common.collect.Range;

import dulab.adap.datamodel.Project;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimpleIsotopePattern;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.ResultFormula;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class MyModuleTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    private ParameterSet parameters;
    private Range<Double> massRange;
    private Range<Double> rtRange;
    private boolean checkIntensity;
    private double intensityDeviation;
    private double minAbundance;
    private double minRating;
    private double minHeight;
    private String element, suffix;
    private MZTolerance mzTolerance;
    private RTTolerance rtTolerance;
    private String message;
    private int totalRows, finishedRows;
    private PeakList resultPeakList;
    private MZmineProject project;
    private PeakList peakList;
    private boolean checkRT;
    private IsotopePattern pattern;
    private PolarityType polarityType;
    private int charge;
    
    private enum ScanType {singleAtom, neutralLoss, pattern};
    ScanType scanType;
    private double dMassLoss;
    IIsotope[] el;
    
    //private MolecularFormulaRange elementCounts;
    //private MolecularFormulaGenerator generator;
    //private IonizationType ionType;
    //private double searchedMass;
    //private int charge;
    //private boolean checkIsotopes, checkMSMS, checkRatios, checkRDBE;
    //private ParameterSet isotopeParameters, msmsParameters, ratiosParameters,
    //        rdbeParameters;
    

    /**
     *
     * @param parameters
     * @param peakList
     * @param peakListRow
     * @param peak
     */
    MyModuleTask(MZmineProject project, PeakList peakList, ParameterSet parameters) {
    	this.parameters = parameters;
    	this.project = project;
        this.peakList = peakList;
        
        mzTolerance = parameters
                .getParameter(MyModuleParameters.mzTolerance)
                .getValue();
        rtTolerance = parameters
                .getParameter(MyModuleParameters.rtTolerance)
                .getValue();
        checkIntensity = parameters.getParameter(MyModuleParameters.checkIntensity).getValue();
        minAbundance = parameters.getParameter(MyModuleParameters.minAbundance).getValue();
        intensityDeviation = parameters.getParameter(MyModuleParameters.intensityDeviation).getValue() / 100;
        element = parameters.getParameter(MyModuleParameters.element).getValue();
        minRating = parameters.getParameter(MyModuleParameters.minRating).getValue();
        suffix = parameters.getParameter(MyModuleParameters.suffix).getValue();
        checkRT = parameters.getParameter(MyModuleParameters.checkRT).getValue();
        minHeight = parameters.getParameter(MyModuleParameters.minHeight).getValue();
        dMassLoss = parameters.getParameter(MyModuleParameters.neutralLoss).getValue();
        charge = parameters.getParameter(MyModuleParameters.charge).getValue();
        
        polarityType = (charge > 0) ? PolarityType.POSITIVE : PolarityType.NEGATIVE;
        charge = (charge < 0) ? charge*-1 : charge;
        
        if(suffix.equals("auto"))
        	suffix = "_-El/Pat_" + element + "-chRT_" + checkRT + 
        			"-checkInt_"+ checkIntensity + "_results";

        
        if(dMassLoss != 0.0)
        	scanType = ScanType.neutralLoss;

        if(element.length() > 2)
        	scanType = ScanType.pattern;
        else
        	scanType = ScanType.singleAtom;
        
        message = "Got paramenters..."; //TODO
        
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
        if (totalRows == 0)
            return 0.0;
        return (double) finishedRows / (double) totalRows;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return message;
    }

	@Override
	public void run() {
		setStatus(TaskStatus.PROCESSING);
		
		totalRows = peakList.getNumberOfRows();
		
		ArrayList<Double> diff = setUpDiff(scanType);
		if(diff == null)
		{
			message = "ERROR: could not set up diff.";
			return;
		}
		
	    // get all rows and sort by m/z
	    PeakListRow[] rows = peakList.getRows();
	    Arrays.sort(rows, new PeakListRowSorter(SortingProperty.MZ, SortingDirection.Ascending));
	    //totalRows = rows.length;
	    
	    resultPeakList = new SimplePeakList(peakList.getName() + suffix, peakList.getRawDataFiles());
	    PeakListHandler resultMap = new PeakListHandler(); 
	    
	    for(int i = 0; i < totalRows; i++) 
	    {
	    // i will represent the index of the row in peakList
			if(peakList.getRow(i).getPeakIdentities().length > 0)
				continue;
			
			message = "Row " + i + "/" + totalRows;
			massRange = mzTolerance.getToleranceRange(peakList.getRow(i).getAverageMZ());
			rtRange = rtTolerance.getToleranceRange(peakList.getRow(i).getAverageRT());
			
			//now get all peaks that lie within RT and maxIsotopeMassRange: pL[index].mz -> pL[index].mz+maxMass
			ArrayList<PeakListRow> groupedPeaks = groupPeaks(rows, i, diff.get(diff.size()-1).doubleValue());
			
			//logger.info("Row: " + i + "\tgroupedPeaks.size(): " + groupedPeaks.size());
			
			if(groupedPeaks.size() < 2)
				continue;
			//else
			//	logger.info("groupedPeaks.size > 2 in row: " + i + " size: " + groupedPeaks.size());

			ResultBuffer[] resultBuffer = new ResultBuffer[diff.size()];//this will store row indexes of all features with fitting rt and mz		
			for(int a = 0; a < diff.size(); a++)							//resultBuffer[i] index will represent Isotope[i] (if numAtoms = 0)
				resultBuffer[a] = new ResultBuffer();					//[0] will be the isotope with lowest mass#
			
			int resultCounter = 0; 	// not sure if we need this yet

			for(int j = 0; j < groupedPeaks.size(); j++)	// go through all possible peaks
			{
				for(int k = 0; k < diff.size(); k ++)		// check for each peak if it is a possible feature for every diff[](isotope)
				{											// this is necessary bc there might be more than one possible feature
			// j represents the row index in groupedPeaks
			// k represents the isotope number the peak will be a candidate for
					if(mzTolerance.checkWithinTolerance(groupedPeaks.get(0).getAverageMZ() + diff.get(k), groupedPeaks.get(j).getAverageMZ()))
					{
						if(scanType == ScanType.singleAtom)
						{
							// this will automatically add groupedPeaks[0] to the list -> isotope with lowest mass
							//logger.info("Main peak (m/z)" +						"\tElement" + 		 "\tIsotope num" + "\tIsotope mass" + 				"\tPeak mass" + 					"\tAbbrevieation(m/z)");
							//logger.info(groupedPeaks.get(0).getAverageMZ() + "\t" + el[k].getSymbol() +"\t"+ k  +"\t\t"+ el[k].getExactMass()	+	"\t" + groupedPeaks.get(j).getAverageMZ() +"\t" + (groupedPeaks.get(j).getAverageMZ()-groupedPeaks.get(0).getAverageMZ()));

							resultBuffer[k].addFound(); //+1 result for isotope k
							resultBuffer[k].addRow(j);  //row in groupedPeaks[]
							resultBuffer[k].addID(groupedPeaks.get(j).getID());
							resultCounter++;
						}
						else if(scanType == ScanType.pattern)
						{
							// this will automatically add groupedPeaks[0] to the list -> isotope with lowest mass
							//logger.info("Main peak (m/z)" +						"\tPattern" + 		 "\tPeak num" +		"\tPeak mass" + 					"\tAbbrevieation(m/z)");
							//logger.info(groupedPeaks.get(0).getAverageMZ() + "\t" + element + "\t"+ k  +"\t\t"+ 	"\t" + groupedPeaks.get(j).getAverageMZ() +"\t" + (groupedPeaks.get(j).getAverageMZ()-groupedPeaks.get(0).getAverageMZ()));

							resultBuffer[k].addFound(); //+1 result for isotope k
							resultBuffer[k].addRow(j);  //row in groupedPeaks[]
							resultBuffer[k].addID(groupedPeaks.get(j).getID());
							resultCounter++;
						}
						else if(scanType == ScanType.neutralLoss)
						{
							//logger.info("Main peak (m/z)" +											"\tMass loss(found)"					 + 		"\tMass loss(input)");
							//logger.info(groupedPeaks.get(0).getAverageMZ() + "\t" + (groupedPeaks.get(j).getAverageMZ()-groupedPeaks.get(0).getAverageMZ()) + dMassLoss);
							
							resultBuffer[k].addFound(); //+1 result for isotope k
							resultBuffer[k].addRow(j);  //row in groupedPeaks[]
							resultBuffer[k].addID(groupedPeaks.get(j).getID());
							resultCounter++;
						}
					}
				}
			}
			
			if(!checkIfAllTrue(resultBuffer))	// this means that for every isotope we expected to find, we found one or more possible features
			{
				//logger.info("Not enough possible features were added to resultBuffer.");
				continue;
			}
			
			message = "Found enough possible features.";
			Candidate[] candidates = new Candidate[diff.size()];
			for(int a = 0; a < diff.size(); a++)							//resultBuffer[i] index will represent Isotope[i] (if numAtoms = 0)
				candidates[a] = new Candidate();
			
			for(int k = 0; k < resultBuffer.length; k++) // reminder: resultBuffer.length = diff.size()
			{
				for(int l = 0; l < resultBuffer[k].getFoundCount(); l++)
				{
		// k represents index resultBuffer[k] and thereby the isotope number
		// l represents the number of results in resultBuffer[k]
					if(scanType == ScanType.singleAtom)
					{
						if(candidates[k].checkForBetterRating(groupedPeaks, 0, resultBuffer[k].getRow(l), el, k, intensityDeviation, minRating, checkIntensity))
						{
						//	logger.info("New best rating for parent m/z: " + groupedPeaks.get(0).getAverageMZ() + "\t->\t" + 
						//		groupedPeaks.get(resultBuffer[k].getRow(l)).getAverageMZ() + "\tRating: " + candidates[k].getRating() + 
						//		"\tDeviation: " + (groupedPeaks.get(0).getAverageMZ() + diff.get(k) - groupedPeaks.get(candidates[k].getRow()).getAverageMZ()));
							
						}
					}
					else if(scanType == ScanType.pattern)
					{
						if(candidates[k].checkForBetterRating(groupedPeaks, 0, resultBuffer[k].getRow(l), pattern, k, intensityDeviation, minRating, checkIntensity))
						{
						//	logger.info("New best rating for parent m/z: " + groupedPeaks.get(0).getAverageMZ() + "\t->\t" + 
						//			groupedPeaks.get(resultBuffer[k].getRow(l)).getAverageMZ() + "\tRating: " + candidates[k].getRating() + 
						//			"\tDeviation: " + (groupedPeaks.get(0).getAverageMZ() + diff.get(k) - groupedPeaks.get(candidates[k].getRow()).getAverageMZ()));
						}
					}
					else if(scanType == ScanType.neutralLoss)
					{
						if(candidates[k].checkForBetterRating(groupedPeaks, 0, resultBuffer[k].getRow(l), diff.get(k), minRating))
						{
						//	logger.info("New best rating for parent m/z: " + groupedPeaks.get(0).getAverageMZ() + "\t->\t" + 
						//			groupedPeaks.get(resultBuffer[k].getRow(l)).getAverageMZ() + "\tRating: " + candidates[k].getRating() + 
						//			"\tDeviation: " + (groupedPeaks.get(0).getAverageMZ() + diff.get(k) - groupedPeaks.get(candidates[k].getRow()).getAverageMZ()));
						}
					}
				}
			}
			
			if(!checkIfAllTrue(candidates))
			{ 
				//logger.info("Not enough valid candidates for parent feature " + groupedPeaks.get(0).getAverageMZ() + "\talthough enough peaks were found.") ;
				continue;	// jump to next i
			}

			
			String comParent = "", comChild = "";
			PeakListRow parent = copyPeakRow(peakList.getRow(i));
			//resultPeakList.addRow(parent);
			resultMap.addRow(parent);	//add results to resultPeakList
			//int parentIndex = resultPeakList.getNumberOfRows() - 1;
			
			comParent = "-IS PARENT-ID: " + parent.getID();
			addComment(parent, comParent);
			int parentID = parent.getID(); // = groupedPeaks.get(0).getID();
			
			DataPoint[] dp = new DataPoint[candidates.length];
			dp[0] = new SimpleDataPoint(parent.getAverageMZ(), parent.getAverageHeight());
			
			for(int k = 1; k < candidates.length; k++) //we skip k=0 because == groupedPeaks[0] which we added before
			{
				PeakListRow child = copyPeakRow(groupedPeaks.get((candidates[k].getRow())));

				dp[k] = new SimpleDataPoint(child.getAverageMZ(), child.getAverageHeight());
				if(scanType == ScanType.singleAtom)
				{
					addComment(parent, "Intensity ratios: " + el[0].getExactMass()/el[0].getExactMass() + ":" + round(el[k].getExactMass()/el[0].getExactMass(), 2));
					comChild = (/*child.getComment() +*/ 
							" ParentID:" + parentID
							+ " Diff. (m/z): " +  round(child.getAverageMZ()-parent.getAverageMZ(), 5)
							+ " Diff. (isot)" +  round((candidates[k].getIsotope().getExactMass() - el[0].getExactMass()), 5)
							+ " A(c)/A(p): " +  round(child.getAverageArea()/parent.getAverageArea(), 2)
							+ " Rating: " +  round(candidates[k].getRating(), 7));
					
					//child.setComment(comChild);
					addComment(child, comChild);
				}
				else if(scanType == ScanType.pattern)
				{
					//parent.setComment(parent.getComment() + " Intensity: " + getIntensityRatios(pattern));
					addComment(parent, "Intensity ratios: " + getIntensityRatios(pattern));
					comChild = ("Parent ID: " + parent.getID() + " Diff. (m/z): " + round(child.getAverageMZ() - parent.getAverageMZ(), 5)
							+ " Diff. (pattern): " +  round(diff.get(k), 5) + " A(c)/A(p): " +  round(child.getAverageArea()/parent.getAverageArea(),2) + " Rating: " +  round(candidates[k].getRating(), 7));
					//child.setComment(comChild);
					addComment(child, comChild);
				}
				else if(scanType == ScanType.neutralLoss)
				{
					comChild = ("Parent ID: " + parent.getID() + " Diff(m/z): " + round(child.getAverageMZ() - parent.getAverageMZ(), 5)
					+ "Diff (calc): " + round(diff.get(k), 5) + " Rating: " + round(candidates[k].getRating(), 7));
					addComment(child, comChild);
				}
				resultMap.addRow(child);
				//resultPeakList.addRow(child);
			}
			
			IsotopePattern resultPattern = new SimpleIsotopePattern(dp, IsotopePatternStatus.DETECTED, "Monoisotopic mass: " + parent.getAverageMZ());
			
			parent.getBestPeak().setIsotopePattern(resultPattern);
			for(int j = 1; j < candidates.length; j++)
				resultMap.getRowByID(candidates[j].getCandID()).getBestPeak().setIsotopePattern(resultPattern);
			
			if(isCanceled())
				return;			
			
			finishedRows++;
		}

	    ArrayList<Integer> keys = resultMap.getAllKeys();
	    for(int j = 0; j < keys.size(); j++)
	    	resultPeakList.addRow(resultMap.getRowByID(keys.get(j)));
	    
	    
	    if(resultPeakList.getNumberOfRows() > 1)
	    	addResultToProject(/*resultPeakList*/);
	    else
	    	message = "Element not found.";
	    setStatus(TaskStatus.FINISHED);
	}
	
	/**
	 * 
	 * @param b
	 * @return true if every
	 */
	private boolean checkIfAllTrue(ResultBuffer[] b)
	{
		for(int i = 0; i < b.length; i++)
			if(b[i].getFoundCount() == 0)
				return false;
		return true;
	}
	
	private boolean checkIfAllTrue (Candidate[] cs)
	{
		for(Candidate c : cs)
			if(c.getRating() == 0)
				return false;
		return true;
	}

	private ArrayList<Double> setUpDiff(ScanType scanType)
	{
		ArrayList<Double> diff = new ArrayList<Double>(2);
		
		switch(scanType)
		{
		case singleAtom:			
			el = getIsotopes(element);
			if(el == null)
			{
				logger.info("Error setting up isotope information. el == null.");
				return null;
			}
		
			//calc differences in isotope masses, i'm assuming [0] has lowest and [n] highest mass
			for (int i = 0; i < el.length; i++)
				diff.add(i, el[i].getExactMass() - el[0].getExactMass());	//diff[0] will be 0
			break;
			
		case pattern:
			pattern = IsotopePatternCalculator.calculateIsotopePattern(element, minAbundance, charge, polarityType);
			pattern = IsotopePatternCalculator.mergeIsotopes(pattern, 0.0003);
			DataPoint[] points = pattern.getDataPoints();
			logger.info("DataPoints in Pattern: " + points.length);
			for(int i = 0; i < pattern.getNumberOfDataPoints(); i++)
			{
				diff.add(i, points[i].getMZ() - points[0].getMZ());
			}
			/*
			 * (a+b)^n ; n=2; => a^2 + 2ab + b^2 => 35Cl+35Cl, 2 * (35Cl+37Cl), 37Cl+37Cl
			 * possibility: a^2 = 0.7577^2; 2ab = 2*(0.7577*0.2423); b^2 = 0.2423^2
			 * a = 35Cl, b = 37Cl, n = numAtoms
			 */
			break;
		case neutralLoss:
			diff.add(0.0);
			diff.add(dMassLoss);
			break;
		}
		
		return diff;
	}
	
	private IIsotope[] getIsotopes(String isotope)
	{
		message = "Getting isotope information for element " + element;
		Isotopes ifac;// = Isotopes.getInstance();
		
		IIsotope[] el;
		try {
			ifac = Isotopes.getInstance();
			el = ifac.getIsotopes(element);
			el = (IIsotope[]) Arrays.stream(el).filter(i -> i.getNaturalAbundance()>minAbundance).toArray(IIsotope[]::new);
			int size = el.length;
			System.out.println(size);
			for(IIsotope i : el)
				System.out.println("mass "+ i.getExactMass() + "   abundance "+i.getNaturalAbundance());
			
			logger.info(size + " isotopes for " + element);
			for(IIsotope i : el)
				logger.info("mass: "+ i.getExactMass() + "\tabundance: "+i.getNaturalAbundance());
			
			return el;

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * 
	 * @param pL
	 * @param parentIndex index of possible parent peak
	 * @param maxMass
	 * @return will return ArrayList<PeakListRow> of all peaks within the range of pL[parentIndex].mz -> pL[parentIndex].mz+maxMass 
	 */
	private ArrayList<PeakListRow> groupPeaks(PeakListRow[] pL, int parentIndex, double maxDiff)
	{
		ArrayList<PeakListRow> buf = new ArrayList<PeakListRow>();
		
		buf.add(pL[parentIndex]); //this means the result will contain row(parentIndex) itself

		double mz = pL[parentIndex].getAverageMZ();
		double rt = pL[parentIndex].getAverageRT();
		
		for(int i = parentIndex + 1; i < pL.length; i++) // will not add the parent peak itself
		{
			PeakListRow r = pL[i];
			// check for rt
			
			if(r.getAverageHeight() < minHeight)
				continue;
			
			if(!rtTolerance.checkWithinTolerance(rt, r.getAverageRT()) && checkRT)
				continue;
			else
				logger.info("within RT tolerance: parentRow: " + parentIndex + " row: " + i);
			
			//if(mzTolerance.checkWithinTolerance(mz + maxDiff, pL[i].getAverageMZ()))
			if(pL[i].getAverageMZ() > mz && pL[i].getAverageMZ() <= (mz + maxDiff + mzTolerance.getMzTolerance()))
			{
				logger.info("within MZ tolerance - parentRow: " + parentIndex + " row: " + i);
					buf.add(pL[i]);
			}
			
			if(pL[i].getAverageMZ() > (mz + maxDiff))	// since pL is sorted by ascending mass, we can stop now
				return buf;
		}
		return buf;
	}
	
	/**
	   * Create a copy of a peak list row.
	   *
	   * @param row the row to copy.
	   * @return the newly created copy.
	   */
	private static PeakListRow copyPeakRow(final PeakListRow row)
	{
		// Copy the peak list row.
	    final PeakListRow newRow = new SimplePeakListRow(row.getID());
	    PeakUtils.copyPeakListRowProperties(row, newRow);

	    // Copy the peaks.
	    for (final Feature peak : row.getPeaks()) {
	      final Feature newPeak = new SimpleFeature(peak);
	      PeakUtils.copyPeakProperties(peak, newPeak);
	      newRow.addPeak(peak.getDataFile(), newPeak);
	    }

	    return newRow;
	}
	
	private static String getIntensityRatios(IsotopePattern pattern)
	{
		DataPoint[] dp = pattern.getDataPoints();
		String ratios = "";
		for(int i = 0; i < dp.length; i++)
			ratios += round((dp[i].getIntensity()/dp[0].getIntensity()),2) + ":";
		ratios = (ratios.length() > 0) ? ratios.substring(0, ratios.length()-1) : ratios;
		return 	ratios;
	}
	
	public static double round(double value, int places) { // https://stackoverflow.com/questions/2808535/round-a-double-to-2-decimal-places
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
	
	public static void addComment(PeakListRow row, String str)
	{
		String current = row.getComment();
		if(current == null)
			row.setComment(str);
		else if(current.contains(str))
			return;
		else
			row.setComment(current + " " + str);
	}
	/**
	   * Add peaklist to project, delete old if requested, add description to result
	   */
	public void addResultToProject() 
	{
	    // Add new peakList to the project
	    project.addPeakList(resultPeakList);

	    // Load previous applied methods
	    for (PeakListAppliedMethod proc : peakList.getAppliedMethods()) {
	      resultPeakList.addDescriptionOfAppliedTask(proc);
	    }

	    // Add task description to peakList
	    resultPeakList
	        .addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod("MyModule", parameters));
	  }
}
















