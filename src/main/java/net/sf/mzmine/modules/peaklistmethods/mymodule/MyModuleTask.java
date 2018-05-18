package net.sf.mzmine.modules.peaklistmethods.mymodule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.formula.MolecularFormulaGenerator;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IIsotope;

import com.google.common.collect.Range;

import dulab.adap.datamodel.Project;
import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.ResultFormula;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class MyModuleTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    private Range<Double> massRange;
    private Range<Double> rtRange;
    private boolean checkIntensity;
    private double intensityDeviation;
    private double minAbundance;
    private double minRating;
    private String element, suffix;
    private MZTolerance mzTolerance;
    private RTTolerance rtTolerance;
    private String message;
    private int totalRows, finishedRows;
    private PeakList resultPeakList;
    private MZmineProject project;
    private PeakList peakList;
    
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
        
        message = "Got paramenters..."; //TODO

        /*charge = parameters
        	.getParameter(MyModuleParameters.charge)
        	.getValue();
		ionType = (IonizationType) parameters
        	.getParameter(MyModuleParameters.ionization)
        	.getValue();
        elementCounts = parameters
                .getParameter(MyModuleParameters.elements)
                .getValue(); 
        checkIsotopes = parameters
                .getParameter(MyModuleParameters.isotopeFilter)
                .getValue();
        isotopeParameters = parameters
                .getParameter(MyModuleParameters.isotopeFilter)
                .getEmbeddedParameters();

        checkMSMS = parameters
                .getParameter(MyModuleParameters.msmsFilter)
                .getValue();
        msmsParameters = parameters
                .getParameter(MyModuleParameters.msmsFilter)
                .getEmbeddedParameters();

        checkRDBE = parameters
                .getParameter(
                        MyModuleParameters.rdbeRestrictions)
                .getValue();
        rdbeParameters = parameters
                .getParameter(
                        MyModuleParameters.rdbeRestrictions)
                .getEmbeddedParameters();

        checkRatios = parameters
                .getParameter(
                        MyModuleParameters.elementalRatios)
                .getValue();
        ratiosParameters = parameters
                .getParameter(
                        MyModuleParameters.elementalRatios)
                .getEmbeddedParameters();*/
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
		
		//get isotope information, idk if it works
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
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		int size = el.length;
		
		logger.info(size + " isotopes for " + element);
		for(IIsotope i : el)
			logger.info("mass: "+ i.getExactMass() + "\tabundance: "+i.getNaturalAbundance());
		
		ArrayList<Double> diff = new ArrayList<Double>(size);
		
		//calc differences in isotope masses, i'm assuming [0] has lowest and [n] highest mass
		for (int i = 0; i < el.length; i++)
			diff.add(i, el[i].getExactMass() - el[0].getExactMass());	//diff[0] will be 0
		
	    // get all rows and sort by m/z
	    PeakListRow[] rows = peakList.getRows();
	    
	    Arrays.sort(rows, new PeakListRowSorter(SortingProperty.MZ, SortingDirection.Ascending));
	    totalRows = rows.length;
	    
	    resultPeakList = new SimplePeakList(peakList.getName() + suffix, peakList.getRawDataFiles());
	    
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
			
			logger.info("Row: " + i + "\tgroupedPeaks.size(): " + groupedPeaks.size());
			if(groupedPeaks.size() < 2)
				continue;
			else
				logger.info("groupedPeaks.size > 2 in row: " + i + " size: " + groupedPeaks.size());

			ResultBuffer[] resultBuffer = new ResultBuffer[el.length]; 	//this will store row indexes of all features with fitting rt and mz		
			for(int a = 0; a < el.length; a++)							//resultBuffer[i] index will represent Isotope[i]
				resultBuffer[a] = new ResultBuffer();					//[0] will be the isotope with lowest mass#
			
			int resultCounter = 0; 	// not sure if we need this yet

			for(int j = 0; j < groupedPeaks.size(); j++)	// go through all possible peaks
			{
				for(int k = 0; k < el.length; k ++)		// check for each peak if it is a possible feature for every diff[](isotope)
				{											// this is necessary bc there might be more than one possible feature
			// j represents the row index in groupedPeaks
			// k represents the isotope number the peak will be a candidate for
					if(mzTolerance.checkWithinTolerance(groupedPeaks.get(0).getAverageMZ() + diff.get(k), groupedPeaks.get(j).getAverageMZ()))
					{
					// this will automatically add groupedPeaks[0] to the list -> isotope with lowest mass
						logger.info("Main peak (m/z)" +						"\tElement" + 		 "\tIsotope num" + "\tIsotope mass" + 				"\tPeak mass" + 					"\tAbbrevieation(m/z)");
						logger.info(groupedPeaks.get(0).getAverageMZ() + "\t" + el[k].getSymbol() +"\t"+ k  +"\t\t"+ el[k].getExactMass()	+	"\t" + groupedPeaks.get(j).getAverageMZ() +"\t" + (groupedPeaks.get(j).getAverageMZ()-groupedPeaks.get(0).getAverageMZ()));

						resultBuffer[k].addFound(); //+1 result for isotope k
						resultBuffer[k].addRow(j);  //row in groupedPeaks[]

						resultCounter++;
					}
				}
			}
			
			if(!checkIfAllTrue(resultBuffer))	// this means that for every isotope we expected to find, we found one or more possible features
			{
				logger.info("Not enough possible features were added to resultBUffer.");
				continue;
			}
			
			message = "Found enough possible features.";
			Candidate[] candidates = new Candidate[el.length];
			for(int a = 0; a < el.length; a++)							//resultBuffer[i] index will represent Isotope[i]
				candidates[a] = new Candidate();
			
			for(int k = 0; k < resultBuffer.length; k++) // reminder: resultBuffer.length = el.length
			{
				for(int l = 0; l < resultBuffer[k].getFoundCount(); l++)
				{
		// k represents index resultBuffer[k] and thereby the isotope number
		// l represents the number of results in resultBuffer[k]

					if(candidates[k].checkForBetterRating(groupedPeaks, 0, resultBuffer[k].getRow(l), el, k, intensityDeviation, minRating, checkIntensity))
					{
						logger.info("New best rating for parent m/z: " + groupedPeaks.get(0).getAverageMZ() + "\t->\t" + 
								groupedPeaks.get(resultBuffer[k].getRow(l)).getAverageMZ() + "\tRating: " + candidates[k].getRating() + 
								"\tDeviation: " + (groupedPeaks.get(0).getAverageMZ() + diff.get(k) - groupedPeaks.get(candidates[k].getRow()).getAverageMZ()));
						
					}
				}
			}
			
			if(!checkIfAllTrue(candidates))
			{
				logger.info("Not enough valid candidates for parent feature " + groupedPeaks.get(0).getAverageMZ() + "\talthough enough peaks were found.") ;
				continue;	// jump to next i
			}

			
			resultPeakList.addRow(groupedPeaks.get(0));		//add results to resultPeakList
			int parentIndex = resultPeakList.getNumberOfRows() - 1;
			resultPeakList.getRow(parentIndex).setComment("-Parent- ID: " + resultPeakList.getRow(parentIndex).getID());
			int parentID = peakList.getRow(i).getID();
			for(int k = 0; k < candidates.length; k++)
			{
				resultPeakList.addRow(groupedPeaks.get(candidates[k].getRow()));
				/*resultPeakList.getRow(k).setComment("Parentmz: " + resultPeakList.getRow(parentIndex).getAverageMZ()
						+ "\nAbbrv. (m/z): " + (resultPeakList.getRow(parentIndex + k).getAverageMZ()-resultPeakList.getRow(parentIndex).getAverageMZ())
						+ "\nIsotope num.: " + k + "\nIsotope mass: " + el[k].getExactMass() + "\nRating: " + candidates[k].getRating());*/
				resultPeakList.getRow(k).setComment("ParentID:" + parentID + "Parentmz: " + peakList.getRow(i).getAverageMZ()
						+ "\nAbbrv. (m/z): " + (resultPeakList.getRow(parentIndex + k).getAverageMZ()-peakList.getRow(i).getAverageMZ())
						+ "\nIsotope num.: " + k + "\nIsotope mass: " + el[k].getExactMass() + "\nRating: " + candidates[k].getRating());
			}
			
			if(isCanceled())
				return;			
			
			finishedRows++;
		}
	    if(resultPeakList.getNumberOfRows() > 1)
	    	project.addPeakList(resultPeakList);
	    else
	    	message = "Element not found.";
	}
	
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

	/**
	 * 
	 * @param pL
	 * @param parentIndex index of possible parent peak
	 * @param maxMass
	 * @return will return ArrayList<PeakListRow> of all peaks within the range of pL[parentIndex].mz -> pL[parentIndex].mz+maxMass 
	 */
	private ArrayList<PeakListRow> groupPeaks(PeakListRow[] pL, int parentIndex, double maxDiff)
	{
		//TODO: creating a new PeakList would be more elegant, but how do you create an empty one?
		ArrayList<PeakListRow> buf = new ArrayList<PeakListRow>();
		
		buf.add(pL[parentIndex]); //this means the result will contain row(parentIndex) itself

		//double mz = pL.getRow(parentIndex).getAverageMZ();
		//double rt = pL.getRow(parentIndex).getAverageRT();
		double mz = pL[parentIndex].getAverageMZ();
		double rt = pL[parentIndex].getAverageRT();
		
		for(int i = parentIndex + 1; i < pL.length; i++) // will not add the parent peak itself
		{
			PeakListRow r = pL[i];
			// check for rt
			if(rtTolerance.checkWithinTolerance(rt, r.getAverageRT()))
			{
				logger.info("within RT tolerance: parentRow: " + parentIndex + " row: " + i);
				//if(mzTolerance.checkWithinTolerance(mz + maxDiff, pL[i].getAverageMZ()))
				if(pL[i].getAverageMZ() > mz && pL[i].getAverageMZ() <= (mz + maxDiff + mzTolerance.getMzTolerance()))
				{
					logger.info("within MZ tolerance - parentRow: " + parentIndex + " row: " + i);
						buf.add(pL[i]);
				}
			}
			
			if(pL[i].getAverageMZ() > (mz + maxDiff))	// since pL is sorted by ascending mass, we can stop now
				return buf;
		}
		return buf;
	}
}
















