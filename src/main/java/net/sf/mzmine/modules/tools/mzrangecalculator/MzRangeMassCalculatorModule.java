/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.tools.mzrangecalculator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Range;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.util.ExitCode;

/**
 * m/z range calculator module. Calculates m/z range from a given mass and m/z
 * tolerance.
 */
public class MzRangeMassCalculatorModule implements MZmineModule {

    private static final String MODULE_NAME = "m/z range calculator from formula";

    @Override
    public @Nonnull String getName() {
        return MODULE_NAME;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return MzRangeMassCalculatorParameters.class;
    }

    /**
     * Shows the calculation dialog and returns the calculated m/z range. May
     * return null in case user clicked Cancel.
     */
    @Nullable
    public static Range<Double> showRangeCalculationDialog() {

        ParameterSet myParameters = MZmineCore.getConfiguration()
                .getModuleParameters(MzRangeMassCalculatorModule.class);

        if (myParameters == null)
            return null;

        ExitCode exitCode = myParameters.showSetupDialog(null, true);
        if (exitCode != ExitCode.OK)
            return null;

        Double mz = myParameters
                .getParameter(MzRangeMassCalculatorParameters.mz).getValue();
        MZTolerance mzTolerance = myParameters
                .getParameter(MzRangeMassCalculatorParameters.mzTolerance)
                .getValue();

        if ((mz == null) || (mzTolerance == null))
            return null;

        Range<Double> mzRange = mzTolerance.getToleranceRange(mz);

        return mzRange;
    }

}