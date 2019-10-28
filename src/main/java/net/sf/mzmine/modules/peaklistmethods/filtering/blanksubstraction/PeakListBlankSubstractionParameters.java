package net.sf.mzmine.modules.peaklistmethods.filtering.blanksubstraction;

import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public class PeakListBlankSubstractionParameters extends SimpleParameterSet {

  public static PeakListsParameter blankPeakLists = new PeakListsParameter("Blank peak list(s)", 1, 10);
  
  public static PeakListsParameter peakLists = new PeakListsParameter("Target peak lists", 1, 30);
  
  public static MZToleranceParameter mzTolerance = new MZToleranceParameter("m/z tolerance", "m/z tolerance allowed to be considered the same peak.");
  
  public static RTToleranceParameter rtTolerance = new RTToleranceParameter("Rt tolerance", "Rt tolerance allowed to be considered the same compound");
  
  public static BooleanParameter createBlankList = new BooleanParameter("Show combined blank", "If checked, a new peak list, containing all peaks found within the blank measurements will be created");

  public static final String[] substractionType = {"All", "Aligned"};
  
  public static ComboParameter<String> blankType = new ComboParameter<String>("Blank substraction type", "Defines if all peaks detected in any blank should be sustracted, or if only peaks found in all blanks will be substracted.", substractionTypes);
}
