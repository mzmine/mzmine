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

package net.sf.mzmine.modules.normalization.rtnormalizer;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;

/**
 * 
 */
public class RTNormalizerParameters extends SimpleParameterSet {

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

    public static final Parameter MZTolerance = new SimpleParameter(
            ParameterType.DOUBLE, "M/Z tolerance",
            "Maximum allowed M/Z difference", "m/z", new Double(0.2), new Double(
                    0.0), null, MZmineCore.getMZFormat());

    public static final Parameter RTTolerance = new SimpleParameter(
            ParameterType.DOUBLE, "Retention time tolerance",
            "Maximum allowed retention time difference", null, new Double(60.0),
            new Double(0.0), null, MZmineCore.getRTFormat());

    public static final Parameter minHeight = new SimpleParameter(
            ParameterType.DOUBLE,
            "Minimum standard intensity",
            "Minimum height of a peak to be selected as normalization standard",
            null, new Double(1000.0), new Double(0.0), null,
            MZmineCore.getIntensityFormat());

    public static final Parameter autoRemove = new SimpleParameter(
            ParameterType.BOOLEAN,
            "Remove original peak list",
            "If checked, original peak list will be removed and only normalized version remains",
            new Boolean(true));

    public RTNormalizerParameters() {
        super(new Parameter[] { suffix, MZTolerance, RTTolerance, minHeight,
                autoRemove });
    }

}
