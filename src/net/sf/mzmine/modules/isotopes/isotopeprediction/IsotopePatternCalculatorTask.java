package net.sf.mzmine.modules.isotopes.isotopeprediction;

import net.sf.mzmine.data.IsotopePattern;
import net.sf.mzmine.taskcontrol.Task;

public class IsotopePatternCalculatorTask implements Task {
	
	//private Logger logger = Logger.getLogger(this.getClass().getName());
	
	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage, description, formula;
	private float minAbundance, isotopeHeight;
	//private int processedAtoms, totalNumberOfAtoms;
	private IsotopePattern isotopePattern;
	private boolean autoHeight = false;
	
	public IsotopePatternCalculatorTask (IsotopePatternCalculatorParameters parameters){
		
		formula = (String) parameters
			.getParameterValue(IsotopePatternCalculatorParameters.formula);
		minAbundance = (Float) parameters
			.getParameterValue(IsotopePatternCalculatorParameters.minimalAbundance);
		isotopeHeight = (Float) parameters
			.getParameterValue(IsotopePatternCalculatorParameters.isotopeHeight);
		autoHeight = (Boolean) parameters
			.getParameterValue(IsotopePatternCalculatorParameters.autoHeight);
		
		description = "Isotope pattern calculation of " + formula;
		
	}

	public void cancel() {
		status = TaskStatus.CANCELED;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public float getFinishedPercentage() {
		return 1;//processedAtoms/totalNumberOfAtoms;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public String getTaskDescription() {
		return description;
	}

	public void run() {
		status = TaskStatus.PROCESSING;
		
		FormulaAnalyzer analyzer = new FormulaAnalyzer();
		
		if (status == TaskStatus.CANCELED)
			return;
		try{
		isotopePattern = analyzer.getIsotopePattern(formula, minAbundance, isotopeHeight, autoHeight);
		}
		catch (Exception e){
			e.printStackTrace();
		}
		
		status = TaskStatus.FINISHED;

	}
	
	public String getFormula(){
		return formula;
	}
	
	public IsotopePattern getIsotopePattern(){
		return isotopePattern;
	}

}
