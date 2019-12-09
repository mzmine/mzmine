/*
 * Copyright 2006-2020 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_manual;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;

public class ManualPickerParameters extends SimpleParameterSet {

    public static final DoubleRangeParameter retentionTimeRange = new DoubleRangeParameter(
            "Retention time", "Retention time range",
            MZmineCore.getConfiguration().getRTFormat());

    public static final DoubleRangeParameter mzRange = new DoubleRangeParameter(
            "m/z range", "m/z range",
            MZmineCore.getConfiguration().getMZFormat());

    public ManualPickerParameters() {
        super(new UserParameter[] { retentionTimeRange, mzRange });
    }

}
