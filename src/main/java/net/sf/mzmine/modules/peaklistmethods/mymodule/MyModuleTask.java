package net.sf.mzmine.modules.peaklistmethods.mymodule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.openscience.cdk.formula.MolecularFormulaGenerator;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IElement;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.Element;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.config.Isotopes;

import com.google.common.collect.Range;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.ResultFormula;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.restrictions.elements.ElementalHeuristicChecker;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.restrictions.rdbe.RDBERestrictionChecker;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreCalculator;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreParameters;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.modules.peaklistmethods.msms.msmsscore.MSMSScore;
import net.sf.mzmine.modules.peaklistmethods.msms.msmsscore.MSMSScoreCalculator;
import net.sf.mzmine.modules.peaklistmethods.msms.msmsscore.MSMSScoreParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.FormulaUtils;

public class MyModuleTask extends AbstractTask {

    private List<ResultFormula> ResultingFormulas;
    private Logger logger = Logger.getLogger(this.getClass().getName());
    private Range<Double> massRange;
    private Range<Double> rtRange;
    private MolecularFormulaRange elementCounts;
    private MolecularFormulaGenerator generator;
    private IonizationType ionType;
    private double searchedMass;
    private int charge;
    private PeakList peakList;
    private boolean checkIsotopes, checkMSMS, checkRatios, checkRDBE;
    private ParameterSet isotopeParameters, msmsParameters, ratiosParameters,
            rdbeParameters;
    private MZTolerance mzTolerance;
    private RTTolerance rtTolerance;
    private String message;
    private int totalRows, finishedRows;

    /**
     *
     * @param parameters
     * @param peakList
     * @param peakListRow
     * @param peak
     */
    MyModuleTask(PeakList peakList, ParameterSet parameters) {

        /*
         * searchedMass = parameters.getParameter(
         * MyModuleParameters.neutralMass).getValue();
         */
        this.peakList = peakList;
        charge = parameters
                .getParameter(MyModuleParameters.charge)
                .getValue();
        ionType = (IonizationType) parameters
                .getParameter(MyModuleParameters.ionization)
                .getValue();
        mzTolerance = parameters
                .getParameter(MyModuleParameters.mzTolerance)
                .getValue();
        rtTolerance = parameters
                .getParameter(MyModuleParameters.rtTolerance)
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
                .getEmbeddedParameters();

        message = "TODO"; //TODO
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
		Isotopes ifac = Isotopes.getInstance();
		IIsotope[] el = ifac.getIsotopes("Gd");
		int size = el.length;
		
		/*Element element = new Element("C");
		IsotopeFactory if = IsotopeFactory.getInstance(element.getNewBuilder());
		if.configure(element);*/
		

		ArrayList<Double> diff = new ArrayList<Double>(size);
		
		//calc differences in isotope masses, i'm assuming [0] has lowest and [n] highest mass
		for (int i = 0; i < el.length; i++)
			diff.add(i, el[i].getExactMass() - el[0].getExactMass());
		
		
		for(int i = 0; i < peakList.getNumberOfRows(); i++)
		{
			if(peakList.getRow(i).getPeakIdentities().length > 0)
				continue;
			
			massRange = mzTolerance.getToleranceRange(peakList.getRow(i).getAverageMZ());
			//rtRange = rtTolerance.getToleranceRange(peakList.getRow(i).getAverageRT());
			rtRange = rtTolerance.getToleranceRange(peakList.getRow(i).getBestPeak().getRT());
			
			//now get all peaks that lie within RT and maxIsotopeMassRange
			ArrayList<PeakListRow> rtGroup = groupPeaksByRT(peakList, i, rtRange);
			ArrayList<PeakListRow> mzGroup = groupPeaksByMZ(peakList, i, diff.get(size));

			//now get peaks within both groups
			ArrayList<PeakListRow> overlapGroup = getGroupOverlap(mzGroup, rtGroup);

			//TODO: compare with isotope pattern
			for(int j = 0; j < overlapGroup.size(); j++)
			{
				
			}
			
			if(isCanceled())
				return;			
			
			finishedRows++;
		}	
	}
	
	/**
	 * 
	 * @param pL PeakList
	 * @param index
	 * @param range
	 * @return
	 */
	private ArrayList<PeakListRow> groupPeaksByRT(PeakList pL, int index, Range<Double> range)
	{
		//TODO: creating a new PeakList would be more elegant, but how do you create an empty one?
		ArrayList<PeakListRow> buf = new ArrayList<PeakListRow>(); 
		
		for(int i = 0; i < pL.getNumberOfRows(); i++)
		{
			//TODO: maybe using highest peaks RT is better than avg RT?
			
			/*if(range.lowerEndpoint() >= pL.getRow(i).getAverageRT()
					&& range.upperEndpoint() <= (pL.getRow(i).getAverageRT()))
			{
				buf.add(pL.getRow(i));
			}*/

			if(range.lowerEndpoint() >= pL.getRow(i).getBestPeak().getRT()
					&& range.upperEndpoint() <= (pL.getRow(i).getBestPeak().getRT()))
			{
				buf.add(pL.getRow(i));
			}
		}
		
		return buf;
	}
	
	/**
	 * 
	 * @param pL PeakList to be searched
	 * @param index index of feature to be compared with
	 * @param maxMass maximum mass added by isotope abundance
	 * @return will return an ArrayList of PeakListRow whose mass is bigger or equal to mass in row index
	 * and smaller than mass of index + maxMass
	 * result will contain row(index)
	 */
	private ArrayList<PeakListRow> groupPeaksByMZ(PeakList pL, int index, double maxMass)
	{
		//TODO: creating a new PeakList would be more elegant, but how do you create an empty one?
		ArrayList<PeakListRow> buf = new ArrayList<PeakListRow>();
		
		double mz = pL.getRow(index).getAverageMZ();
		
		for(int i = 0; i < pL.getNumberOfRows(); i++)
		{
			if(mz >= (pL.getRow(i).getAverageMZ()) //this means the result will contain row(index) itself
					&& mz <= (pL.getRow(i).getAverageMZ() + maxMass))
			{
				buf.add(pL.getRow(i));
			}
		}
		
		return buf;
	}
	
	/**
	 * 
	 * @param mzGroup
	 * @param rtGroup
	 * @return ArrayList<PeakListRow> of Rows that are inside both groups
	 */
	private ArrayList<PeakListRow> getGroupOverlap(ArrayList<PeakListRow> mzGroup, ArrayList<PeakListRow> rtGroup)
	{
		ArrayList<PeakListRow> buf = new ArrayList<PeakListRow>();
		
		for(int i = 0; i < mzGroup.size(); i++)
		{
			for(int j = 0; j < rtGroup.size(); j++)
			{
				if(mzGroup.get(i).getID() == rtGroup.get(j).getID())
				{
					buf.add(mzGroup.get(i));
					/*TODO: does this work?
					 * since it will only add from index i and from mzGroup there should
					 * be no duplicates, right?
					 */
				}
			}
		}
		
		return buf;
	}
}


















