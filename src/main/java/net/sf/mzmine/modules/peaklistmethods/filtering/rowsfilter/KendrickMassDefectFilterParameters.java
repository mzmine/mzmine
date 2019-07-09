package net.sf.mzmine.modules.peaklistmethods.filtering.rowsfilter;

import com.google.common.collect.Range;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;

public class KendrickMassDefectFilterParameters extends SimpleParameterSet {

  public static final DoubleRangeParameter kendrickMassDefectRange = new DoubleRangeParameter(
      "Kendrick mass defect", "Permissible range of Kendrick mass defect per row",
      MZmineCore.getConfiguration().getRTFormat(), Range.closed(0.0, 1.0));

  public static final StringParameter kendrickMassBase = new StringParameter("Kendrick mass base",
      "Enter a sum formula for a Kendrick mass base, e.g. \"CH2\"");

  public static final IntegerParameter charge =
      new IntegerParameter("Charge", "Enter a charge for charge dependent KMD filtering", 1);

  public static final IntegerParameter divisor = new IntegerParameter("Divisor",
      "Enter a divisor for fractional base unit dependent KMD filtering", 1);

  public KendrickMassDefectFilterParameters() {
    super(new Parameter[] {kendrickMassDefectRange, kendrickMassBase, charge, divisor});
  }
}
