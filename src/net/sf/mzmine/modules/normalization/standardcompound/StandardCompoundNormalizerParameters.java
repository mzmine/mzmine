/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.normalization.standardcompound;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.data.impl.SimplePeakListRow;

/**
 * 
 */
public class StandardCompoundNormalizerParameters extends SimpleParameterSet {

    public static final String standardUsageTypeNearest = "Nearest standard";
    public static final String standardUsageTypeWeighted = "Weighted contribution of all standards";

    public static final Object[] standardUsageTypePossibleValues = {
            standardUsageTypeNearest, standardUsageTypeWeighted };

    public static final String peakMeasurementTypeHeight = "Peak height";
    public static final String peakMeasurementTypeArea = "Peak area";

    public static final Object[] peakMeasurementTypePossibleValues = {
            peakMeasurementTypeHeight, peakMeasurementTypeArea };

    
    public static final Parameter suffix = new SimpleParameter(
            ParameterType.STRING, "Name suffix",
            "Suffix to be added to peak list name", null, "normalized", null);
    
    public static final Parameter standardUsageType = new SimpleParameter(
            ParameterType.STRING, "Normalization type",
            "Normalize intensities using ", standardUsageTypeNearest,
            standardUsageTypePossibleValues);

    public static final Parameter peakMeasurementType = new SimpleParameter(
            ParameterType.STRING, "Peak measurement type",
            "Measure peaks using ", peakMeasurementTypeHeight,
            peakMeasurementTypePossibleValues);

    public static final Parameter MZvsRTBalance = new SimpleParameter(
            ParameterType.DOUBLE, "M/Z vs RT balance",
            "Used in distance measuring as multiplier of M/Z difference", "",
            new Double(10.0), new Double(0.0), null);

    public static final Parameter autoRemove = new SimpleParameter(
            ParameterType.BOOLEAN,
            "Remove original peak list",
            "If checked, original peak list will be removed and only normalized version remains",
            new Boolean(false));
    
	public static final Parameter standardCompounds = new SimpleParameter(
			ParameterType.MULTIPLE_SELECTION,
			"Standard compounds",
			"List of peaks, where is possible to choose one or more peaks as standard for normalization",
			null, new PeakListRow[]{new SimplePeakListRow(0)});

    public StandardCompoundNormalizerParameters() {
        super(new Parameter[] { suffix, standardUsageType, peakMeasurementType,
                MZvsRTBalance, autoRemove, standardCompounds });

    }

}
