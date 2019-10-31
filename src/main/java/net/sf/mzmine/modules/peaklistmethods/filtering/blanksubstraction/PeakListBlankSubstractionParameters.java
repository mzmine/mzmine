package net.sf.mzmine.modules.peaklistmethods.filtering.blanksubstraction;

import net.sf.mzmine.modules.peaklistmethods.filtering.blanksubstraction.PeakListBlankSubstractionTask.SubstractionType;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public class PeakListBlankSubstractionParameters extends SimpleParameterSet {

  // private SubstractionType types;

  public static PeakListsParameter blankPeakLists =
      new PeakListsParameter("Blank peak list(s)", 1, 100);

  public static PeakListsParameter peakLists = new PeakListsParameter("Target peak lists", 1, 1000);

  public static MZToleranceParameter mzTolerance = new MZToleranceParameter("m/z tolerance",
      "m/z tolerance allowed to be considered the same peak.");

  public static RTToleranceParameter rtTolerance = new RTToleranceParameter("Rt tolerance",
      "Rt tolerance allowed to be considered the same compound");

  public static BooleanParameter createBlankList = new BooleanParameter("Show combined blank",
      "If checked, a new peak list, containing all peaks found within the blank measurements will be created");

  public static ComboParameter<SubstractionType> substractionType =
      new ComboParameter<SubstractionType>("Blank substraction type",
          "Defines if all peaks detected in the combined blank should be sustracted, or if only peaks found"
              + " in all blanks will be substracted.",
          SubstractionType.values());

  public static IntegerParameter minBlanks = new IntegerParameter(
      "Minimum # of detection in blanks",
      "Specifies in how many of the blank files a peak has to be detected, if 'Combined' is selected "
      + "in the 'Blank substraction type' parameter.");

  public PeakListBlankSubstractionParameters() {
    super(new Parameter[] {blankPeakLists, peakLists, mzTolerance, rtTolerance, createBlankList,
        substractionType, minBlanks});
  };
}
