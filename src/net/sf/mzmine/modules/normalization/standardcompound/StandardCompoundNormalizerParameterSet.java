/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.normalization.standardcompound;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;

/**
 * 
 */
public class StandardCompoundNormalizerParameterSet extends SimpleParameterSet {

    public static final String StandardUsageTypeNearest = "Nearest standard";
    public static final String StandardUsageTypeWeighted = "Weighted contribution of all standards";

    public static final Object[] StandardUsageTypePossibleValues = {
            StandardUsageTypeNearest, StandardUsageTypeWeighted };

    public static final Parameter StandardUsageType = new SimpleParameter(
            ParameterType.STRING, "Normalization type",
            "Normalize intensities using ", StandardUsageTypeNearest,
            StandardUsageTypePossibleValues);

    public static final String PeakMeasurementTypeHeight = "Peak height";
    public static final String PeakMeasurementTypeArea = "Peak area";

    public static final Object[] PeakMeasurementTypePossibleValues = {
            PeakMeasurementTypeHeight, PeakMeasurementTypeArea };

    public static final Parameter PeakMeasurementType = new SimpleParameter(
            ParameterType.STRING, "Peak measurement type",
            "Measure peaks using ", PeakMeasurementTypeHeight,
            PeakMeasurementTypePossibleValues);

    public static final Parameter MZvsRTBalance = new SimpleParameter(
            ParameterType.FLOAT, "M/Z vs RT balance",
            "Used in distance measuring as multiplier of M/Z difference", "",
            new Float(10.0), new Float(0.0), null);

    public static final Parameter autoRemove = new SimpleParameter(
            ParameterType.BOOLEAN,
            "Remove original peak list",
            "If checked, original peak list will be removed and only normalized version remains",
            new Boolean(true));

    private PeakListRow[] selectedPeaks;

    public StandardCompoundNormalizerParameterSet() {
        super(new Parameter[] { StandardUsageType, PeakMeasurementType,
                MZvsRTBalance, autoRemove });

    }

    public StandardCompoundNormalizerParameterSet clone() {

        StandardCompoundNormalizerParameterSet clone = (StandardCompoundNormalizerParameterSet) super.clone();

        if (selectedPeaks != null) {
            clone.setSelectedStandardPeakListRows(selectedPeaks);
        }

        return clone;

    }

    public void setSelectedStandardPeakListRows(PeakListRow[] selectedPeaks) {
        this.selectedPeaks = selectedPeaks;
    }

    public PeakListRow[] getSelectedStandardPeakListRows() {
        return selectedPeaks;
    }

}
