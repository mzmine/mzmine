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

package io.github.mzmine.modules.dataanalysis.rtmzplots.cvplot;

import java.awt.Color;
import java.util.Collection;

import javax.annotation.Nonnull;

import org.jfree.data.xy.AbstractXYZDataset;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.dataanalysis.rtmzplots.RTMZAnalyzerWindow;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.interpolatinglookuppaintscale.InterpolatingLookupPaintScale;

public class CVPlotModule implements MZmineRunnableModule {

    private static final String MODULE_NAME = "CV plot";
    private static final String MODULE_DESCRIPTION = "Coefficient of variation plot.";

    @Override
    public @Nonnull String getName() {
        return MODULE_NAME;
    }

    @Override
    public @Nonnull String getDescription() {
        return MODULE_DESCRIPTION;
    }

    @Override
    @Nonnull
    public ExitCode runModule(@Nonnull MZmineProject project,
            @Nonnull ParameterSet parameters, @Nonnull Collection<Task> tasks) {

        PeakList peakLists[] = parameters.getParameter(CVParameters.peakLists)
                .getValue().getMatchingPeakLists();

        for (PeakList pl : peakLists) {

            // Create dataset & paint scale
            AbstractXYZDataset dataset = new CVDataset(pl, parameters);
            InterpolatingLookupPaintScale paintScale = new InterpolatingLookupPaintScale();

            paintScale.add(0.00, new Color(0, 0, 0));
            paintScale.add(0.15, new Color(102, 255, 102));
            paintScale.add(0.30, new Color(51, 102, 255));
            paintScale.add(0.45, new Color(255, 0, 0));

            // Create & show window
            RTMZAnalyzerWindow window = new RTMZAnalyzerWindow(dataset, pl,
                    paintScale);

            window.setVisible(true);

        }

        return ExitCode.OK;

    }

    @Override
    public @Nonnull MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.DATAANALYSIS;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return CVParameters.class;
    }

}
