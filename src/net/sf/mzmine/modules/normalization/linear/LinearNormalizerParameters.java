/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.normalization.linear;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;

/**
 * 
 */
public class LinearNormalizerParameters extends SimpleParameterSet {

    public static final String NormalizationTypeAverageIntensity = "Average intensity";
    public static final String NormalizationTypeAverageSquaredIntensity = "Average squared intensity";
    public static final String NormalizationTypeMaximumPeakHeight = "Maximum peak intensity";
    public static final String NormalizationTypeTotalRawSignal = "Total raw signal";

    public static final Object[] normalizationTypePossibleValues = {
            NormalizationTypeAverageIntensity,
            NormalizationTypeAverageSquaredIntensity,
            NormalizationTypeMaximumPeakHeight, NormalizationTypeTotalRawSignal };

    public static final Parameter suffix = new SimpleParameter(
            ParameterType.STRING, "Name suffix",
            "Suffix to be added to peak list name", null, "normalized", null);

    public static final Parameter normalizationType = new SimpleParameter(
            ParameterType.STRING, "Normalization type",
            "Normalize intensities by...", NormalizationTypeAverageIntensity,
            normalizationTypePossibleValues);

    public static final String PeakMeasurementTypeHeight = "Peak height";
    public static final String PeakMeasurementTypeArea = "Peak area";

    public static final Object[] PeakMeasurementTypePossibleValues = {
            PeakMeasurementTypeHeight, PeakMeasurementTypeArea };

    public static final Parameter peakMeasurementType = new SimpleParameter(
            ParameterType.STRING, "Peak measurement type",
            "Measure peaks using", PeakMeasurementTypeHeight,
            PeakMeasurementTypePossibleValues);

    public static final Parameter autoRemove = new SimpleParameter(
            ParameterType.BOOLEAN,
            "Remove original peak list",
            "If checked, original peak list will be removed and only normalized version remains",
            new Boolean(true));

    public LinearNormalizerParameters() {
        super(new Parameter[] { suffix, normalizationType, peakMeasurementType,
                autoRemove });
    }

}
