package net.sf.mzmine.modules.peaklistmethods.IsotopePeakScanner;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import org.apache.poi.ss.formula.functions.Rows;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.interfaces.IIsotope;

import com.google.common.collect.Range;

import io.github.msdk.MSDKRuntimeException;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimpleIsotopePattern;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.modules.peaklistmethods.IsotopePeakScanner.tests.ExtendedIsotopePattern;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class IsotopePeakScannerTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    private ParameterSet parameters;
    private Range<Double> massRange;
    private Range<Double> rtRange;
    private boolean checkIntensity;
//    private double intensityDeviation;
    private double minAbundance;
    private double minRating;
    private double minHeight;
    private String element, suffix;
    private MZTolerance mzTolerance;
    private RTTolerance rtTolerance;
    private double minPatternIntensity;
    private double mergeFWHM;
    private String message;
    private int totalRows, finishedRows;
    private PeakList resultPeakList;
    private MZmineProject project;
    private PeakList peakList;
    private boolean checkRT;
    private ExtendedIsotopePattern pattern;
    private PolarityType polarityType;
    private int charge;
    private boolean avgIntensity;
    private String massListName;
    
    private enum ScanType {neutralLoss, pattern};
    ScanType scanType;
    private double dMassLoss;
    IIsotope[] el;    

    /**
     *
     * @param parameters
     * @param peakList
     * @param peakListRow
     * @param peak
     */
    IsotopePeakScannerTask(MZmineProject project, PeakList peakList, ParameterSet parameters) {
    	this.parameters = parameters;
    	this.project = project;
        this.peakList = peakList;
        
        mzTolerance = parameters
                .getParameter(IsotopePeakScannerParameters.mzTolerance)
                .getValue();
        rtTolerance = parameters
                .getParameter(IsotopePeakScannerParameters.rtTolerance)
                .getValue();
        checkIntensity = parameters.getParameter(IsotopePeakScannerParameters.checkIntensity).getValue();
        minAbundance = parameters.getParameter(IsotopePeakScannerParameters.minAbundance).getValue();
//        intensityDeviation = parameters.getParameter(IsotopePeakScannerParameters.intensityDeviation).getValue() / 100;
        mergeFWHM = parameters.getParameter(IsotopePeakScannerParameters.mergeFWHM).getValue();
        minPatternIntensity = parameters.getParameter(IsotopePeakScannerParameters.minPatternIntensity).getValue();
        element = parameters.getParameter(IsotopePeakScannerParameters.element).getValue();
        minRating = parameters.getParameter(IsotopePeakScannerParameters.minRating).getValue();
        suffix = parameters.getParameter(IsotopePeakScannerParameters.suffix).getValue();
        checkRT = parameters.getParameter(IsotopePeakScannerParameters.checkRT).getValue();
        minHeight = parameters.getParameter(IsotopePeakScannerParameters.minHeight).getValue();
        dMassLoss = parameters.getParameter(IsotopePeakScannerParameters.neutralLoss).getValue();
        charge = parameters.getParameter(IsotopePeakScannerParameters.charge).getValue();
        avgIntensity = parameters.getParameter(IsotopePeakScannerParameters.massList).getValue();
        massListName = parameters.getParameter(IsotopePeakScannerParameters.massList).getEmbeddedParameter().getValue();
        
        
       /* RawDataFile[] raws = peakList.getRawDataFiles();
        Scan scan = raws[0].getScan(1);
        MassList[] lists = scan.getMassLists();
        for(MassList list : lists)
        	System.out.println("MassList: " + list.getName());
        if(!peakList.getRawDataFiles()[0].getScan(0).getMassLists().toString().contains(massListName))
        	throw new MSDKRuntimeException("massList " + massListName + " not within: " + peakList.getRawDataFiles()[0].getScan(0).getMassLists().toString());*/
        
        if(avgIntensity == true && checkIntensity == false)
        {
        	
        	avgIntensity = false;
        }
        
        polarityType = (charge > 0) ? PolarityType.POSITIVE : PolarityType.NEGATIVE;
        charge = (charge < 0) ? charge*-1 : charge;
        
        if(getPeakListPolarity(peakList) != polarityType)
        	logger.warning("PeakList.polarityType does not match selected polarity. " + getPeakListPolarity(peakList).toString() + "!=" + polarityType.toString());
        
        if(suffix.equals("auto"))
        	suffix = "_-Pat_" + element + "-RT" + checkRT + 
        			"-INT"+ checkIntensity + "-R" + minRating + "_results";

        if(dMassLoss != 0.0)
        	scanType = ScanType.neutralLoss;
        else
        	scanType = ScanType.pattern;

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
	    
	    PeakListHandler plh = new PeakListHandler();
	    plh.setUp(peakList);
	    //totalRows = rows.length;
	    
	    resultPeakList = new SimplePeakList(peakList.getName() + suffix, peakList.getRawDataFiles());
	    PeakListHandler resultMap = new PeakListHandler();
	    
	    for(int i = 0; i < totalRows; i++) 
	    {
	    // i will represent the index of the row in peakList
			if(peakList.getRow(i).getPeakIdentities().length > 0
					/*|| peakList.getRow(i).getRowCharge() != this.charge*/)
			{
				//logger.info("Charge of row " + i + " is not " + charge + ". Charge of row " + i + " is " + peakList.getRow(i).getRowCharge());
				finishedRows++;
				continue;			
			}
			
			message = "Row " + i + "/" + totalRows;
			massRange = mzTolerance.getToleranceRange(peakList.getRow(i).getAverageMZ());
			rtRange = rtTolerance.getToleranceRange(peakList.getRow(i).getAverageRT());
			
			//now get all peaks that lie within RT and maxIsotopeMassRange: pL[index].mz -> pL[index].mz+maxMass
			ArrayList<PeakListRow> groupedPeaks = groupPeaks(rows, i, diff.get(diff.size()-1).doubleValue());
			
			//logger.info("Row: " + i + "\tgroupedPeaks.size(): " + groupedPeaks.size());
			
			if(groupedPeaks.size() < 2)
			{
				finishedRows++;
				continue;
			}
			//else
			//	logger.info("groupedPeaks.size > 2 in row: " + i + " size: " + groupedPeaks.size());

			ResultBuffer[] resultBuffer = new ResultBuffer[diff.size()];	//this will store row indexes of all features with fitting rt and mz		
			for(int a = 0; a < diff.size(); a++)							//resultBuffer[i] index will represent Isotope[i] (if numAtoms = 0)
				resultBuffer[a] = new ResultBuffer();						//[0] will be the isotope with lowest mass#

			for(int j = 0; j < groupedPeaks.size(); j++)	// go through all possible peaks
			{
				for(int k = 0; k < diff.size(); k ++)		// check for each peak if it is a possible feature for every diff[](isotope)
				{											// this is necessary bc there might be more than one possible feature
			// j represents the row index in groupedPeaks
			// k represents the isotope number the peak will be a candidate for
					if(mzTolerance.checkWithinTolerance(groupedPeaks.get(0).getAverageMZ() + diff.get(k), groupedPeaks.get(j).getAverageMZ()))
					{
						if(scanType == ScanType.pattern)
						{
							// this will automatically add groupedPeaks[0] to the list -> isotope with lowest mass
							//logger.info("Main peak (m/z)" +						"\tPattern" + 		 "\tPeak num" +		"\tPeak mass" + 					"\tAbbrevieation(m/z)");
							//logger.info(groupedPeaks.get(0).getAverageMZ() + "\t" + element + "\t"+ k  +"\t\t"+ 	"\t" + groupedPeaks.get(j).getAverageMZ() +"\t" + (groupedPeaks.get(j).getAverageMZ()-groupedPeaks.get(0).getAverageMZ()));

							resultBuffer[k].addFound(); //+1 result for isotope k
							resultBuffer[k].addRow(j);  //row in groupedPeaks[]
							resultBuffer[k].addID(groupedPeaks.get(j).getID());
						}
						else if(scanType == ScanType.neutralLoss)
						{
							//logger.info("Main peak (m/z)" +											"\tMass loss(found)"					 + 		"\tMass loss(input)");
							//logger.info(groupedPeaks.get(0).getAverageMZ() + "\t" + (groupedPeaks.get(j).getAverageMZ()-groupedPeaks.get(0).getAverageMZ()) + dMassLoss);
							
							resultBuffer[k].addFound(); //+1 result for isotope k
							resultBuffer[k].addRow(j);  //row in groupedPeaks[]
							resultBuffer[k].addID(groupedPeaks.get(j).getID());

						}
					}
				}
			}
			
			if(!checkIfAllTrue(resultBuffer))	// this means that for every isotope we expected to find, we found one or more possible features
			{
				finishedRows++;
				//logger.info("Not enough possible features were added to resultBuffer.");
				continue;
			}
			
//			message = "Found enough possible features.";
			
			Candidates candidates = new Candidates(diff.size(), minHeight, mzTolerance, pattern, massListName);
			
			//Candidate[] candidates = new Candidate[diff.size()];
//			for(int a = 0; a < diff.size(); a++)							//resultBuffer[i] index will represent Isotope[i] (if numAtoms = 0)
//				candidates[a] = new Candidate();
			
			for(int k = 0; k < resultBuffer.length; k++) // reminder: resultBuffer.length = diff.size()
			{
				for(int l = 0; l < resultBuffer[k].getFoundCount(); l++)
				{
		// k represents index resultBuffer[k] and thereby the isotope number
		// l represents the number of results in resultBuffer[k]
					if(scanType == ScanType.pattern)
					{
						if(candidates.get(k).checkForBetterRating(groupedPeaks, 0, resultBuffer[k].getRow(l), pattern, k, minRating, checkIntensity))
						{
						//	logger.info("New best rating for parent m/z: " + groupedPeaks.get(0).getAverageMZ() + "\t->\t" + 
						//			groupedPeaks.get(resultBuffer[k].getRow(l)).getAverageMZ() + "\tRating: " + candidates[k].getRating() + 
						//			"\tDeviation: " + (groupedPeaks.get(0).getAverageMZ() + diff.get(k) - groupedPeaks.get(candidates[k].getRow()).getAverageMZ()));
						}
					}
					else if(scanType == ScanType.neutralLoss)
					{
						if(candidates.get(k).checkForBetterRating(groupedPeaks, 0, resultBuffer[k].getRow(l), diff.get(k), minRating))
						{
						//	logger.info("New best rating for parent m/z: " + groupedPeaks.get(0).getAverageMZ() + "\t->\t" + 
						//			groupedPeaks.get(resultBuffer[k].getRow(l)).getAverageMZ() + "\tRating: " + candidates[k].getRating() + 
						//			"\tDeviation: " + (groupedPeaks.get(0).getAverageMZ() + diff.get(k) - groupedPeaks.get(candidates[k].getRow()).getAverageMZ()));
						}
					}
				}
			}
			
			if(!checkIfAllTrue(candidates.getCandidates()))
			{ 
				finishedRows++;
				//logger.info("Not enough valid candidates for parent feature " + groupedPeaks.get(0).getAverageMZ() + "\talthough enough peaks were found.") ;
				continue;	// jump to next i
			}

			
			String comParent = "", comChild = "";
			PeakListRow parent = copyPeakRow(peakList.getRow(i));
			resultMap.addRow(parent);	//add results to resultPeakList
			
			comParent =  parent.getID() + "--IS PARENT--";
			addComment(parent, comParent);
			
			DataPoint[] dp = new DataPoint[candidates.size()];	// we need this to add the IsotopePattern later on
			dp[0] = new SimpleDataPoint(parent.getAverageMZ(), parent.getAverageHeight());
			
			double[] avgIntens = null;
			if(avgIntensity) 
			{
				/*int[] ids = new int[diff.size()];
				ids[0] = parent.getID();
				for(int k= 1; k < diff.size(); k++)
					ids[k] = candidates.get(k).getCandID();
				avgIntens = getAvgPeakHeights(plh, ids, minHeight);*/
				
				candidates.calcAvgRating(plh);
			}
			
			for(int k = 1; k < candidates.size(); k++) //we skip k=0 because == groupedPeaks[0] which we added before
			{
				PeakListRow child = copyPeakRow(groupedPeaks.get((candidates.get(k).getRow())));
				dp[k] = new SimpleDataPoint(child.getAverageMZ(), child.getAverageHeight());
				
				if(scanType == ScanType.pattern)
				{
					String average = "";
					if(avgIntensity)
					{
						average = " AvgRating: " + candidates.getAvgRating(k);
					}
					
					//parent.setComment(parent.getComment() + " Intensity: " + getIntensityRatios(pattern));
					addComment(parent, "Intensity ratios: " + getIntensityRatios(pattern) + " Identity: " + pattern.getDetailedPeakDescription(0));
					comChild = (parent.getID() + "-Parent ID" + " m/z-shift(ppm): " + round(((child.getAverageMZ() - parent.getAverageMZ()) 
							- diff.get(k))*1E6/child.getAverageMZ(), 4) + " A(c)/A(p): " +  round(child.getAverageHeight()/parent.getAverageHeight(),2)
							+ " Identity: " + pattern.getDetailedPeakDescription(k)
							+ " Rating: " +  round(candidates.get(k).getRating(), 7)
							+ average);
					//child.setComment(comChild);
					addComment(child, comChild);
				}
				else if(scanType == ScanType.neutralLoss)
				{
					comChild = ("Parent ID: " + parent.getID() + " Diff(m/z): " + round(child.getAverageMZ() - parent.getAverageMZ(), 5)
					+ "Diff (calc): " + round(diff.get(k), 5) + " Rating: " + round(candidates.get(k).getRating(), 7));
					addComment(child, comChild);
				}
				resultMap.addRow(child);
				//resultPeakList.addRow(child);

			}
			
			IsotopePattern resultPattern = new SimpleIsotopePattern(dp, IsotopePatternStatus.DETECTED, "Monoisotopic mass: " + parent.getAverageMZ());
			
			parent.getBestPeak().setIsotopePattern(resultPattern);
			for(int j = 1; j < candidates.size(); j++)
				resultMap.getRowByID(candidates.get(j).getCandID()).getBestPeak().setIsotopePattern(resultPattern);
			
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
		case pattern:

			pattern = new ExtendedIsotopePattern();
			pattern.setUpFromFormula(element, minAbundance, mergeFWHM,minPatternIntensity);
			//pattern.mergePeaks(mergeFWHM);
			pattern.normalizePatternToPeak(0);
			pattern.print();
			pattern.applyCharge(charge, polarityType);
			
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
//			else
//				logger.info("within RT tolerance: parentRow: " + parentIndex + " row: " + i);
			
			//if(mzTolerance.checkWithinTolerance(mz + maxDiff, pL[i].getAverageMZ()))
			if(pL[i].getAverageMZ() > mz && pL[i].getAverageMZ() <= (mz + maxDiff + mzTolerance.getMzTolerance()))
			{
//				logger.info("within MZ tolerance - parentRow: " + parentIndex + " row: " + i);
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
	        .addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod("IsotopePeakScanner", parameters));
	  }
	
	private PolarityType getPeakListPolarity(PeakList peakList)
	{
		int[] scans = peakList.getRow(0).getPeaks()[0].getScanNumbers();
		RawDataFile raw = peakList.getRow(0).getPeaks()[0].getDataFile();
		return raw.getScan(scans[0]).getPolarity();
	}
}
















