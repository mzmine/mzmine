/*
 * Copyright 2006-2011 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.sf.mzmine.modules.peaklistmethods.alignment.graph;

import java.text.NumberFormat;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.SimpleParameterSet;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.MZToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.NumberParameter;
import net.sf.mzmine.parameters.parametertypes.RTToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;

public class GraphAlignerParameters extends SimpleParameterSet {

        public static final NumberFormat percentFormat = NumberFormat.getPercentInstance();
        public static final StringParameter peakListName = new StringParameter(
                "Peak list name",
                "Peak list name");
        public static final MZToleranceParameter MZTolerance = new MZToleranceParameter(
                "m/z tolerance",
                "Maximum allowed M/Z difference");
        public static final NumberParameter RTToleranceValueAbs = new NumberParameter(
                "RT tolerance after correction",
                "Maximum allowed absolute RT difference after the algorithm correction for the retention time",
                MZmineCore.getRTFormat());
        public static final RTToleranceParameter RTTolerance = new RTToleranceParameter();
        public static final NumberParameter minPeaksInTheGraph = new NumberParameter(
                "Peaks in the graph",
                "Minimun number of peaks inside the RT-MZ window.",
                NumberFormat.getIntegerInstance(), 200);
        public static final NumberParameter regressionWindow = new NumberParameter(
                "RT size for linear regression",
                "Retention time window where there is not change in the shift of the retention time with respect to the other samples.",
                MZmineCore.getRTFormat(), 100.0);
        public static final BooleanParameter SameChargeRequired = new BooleanParameter(
                "Require same charge state",
                "If checked, only rows having same charge state can be aligned");

        public GraphAlignerParameters() {
                super(new UserParameter[]{peakListName, MZTolerance,
                                RTTolerance, RTToleranceValueAbs, minPeaksInTheGraph, regressionWindow, SameChargeRequired});
        }
}
