/*
 * Copyright 2006-2019 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.peaklistmethods.filtering.rowsfilter;

import java.text.DecimalFormat;
import com.google.common.collect.Range;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;

public class KendrickMassDefectFilterParameters extends SimpleParameterSet {

  public static final DoubleRangeParameter kendrickMassDefectRange = new DoubleRangeParameter(
      "Kendrick mass defect", "Permissible range of Kendrick mass defect per row",
      MZmineCore.getConfiguration().getRTFormat(), Range.closed(0.0, 1.0));

  public static final StringParameter kendrickMassBase = new StringParameter("Kendrick mass base",
      "Enter a sum formula for a Kendrick mass base, e.g. \"CH2\"");

  public static final DoubleParameter shift = new DoubleParameter("Shift",
      "Enter a shift for shift dependent KMD filtering", new DecimalFormat("0.##"), 0.00);

  public static final IntegerParameter charge =
      new IntegerParameter("Charge", "Enter a charge for charge dependent KMD filtering", 1);

  public static final IntegerParameter divisor = new IntegerParameter("Divisor",
      "Enter a divisor for fractional base unit dependent KMD filtering", 1);

  public static final BooleanParameter useRemainderOfKendrickMass =
      new BooleanParameter("Use Remainder of Kendrick mass",
          "Use Remainder of Kendrick mass (RKM) instead of Kendrick mass defect (KMD)", false);

  public KendrickMassDefectFilterParameters() {
    super(new Parameter[] {kendrickMassDefectRange, kendrickMassBase, shift, charge, divisor,
        useRemainderOfKendrickMass});
  }

}
