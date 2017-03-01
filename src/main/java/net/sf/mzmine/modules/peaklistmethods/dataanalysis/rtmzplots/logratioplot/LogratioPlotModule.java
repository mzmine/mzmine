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

package net.sf.mzmine.modules.peaklistmethods.dataanalysis.rtmzplots.logratioplot;

import java.awt.Color;
import java.util.Collection;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineRunnableModule;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.rtmzplots.RTMZAnalyzerWindow;
import net.sf.mzmine.modules.peaklistmethods.dataanalysis.rtmzplots.cvplot.CVParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.interpolatinglookuppaintscale.InterpolatingLookupPaintScale;

import org.jfree.data.xy.AbstractXYZDataset;

public class LogratioPlotModule implements MZmineRunnableModule {

    private static final String MODULE_NAME = "Logratio analysis";
    private static final String MODULE_DESCRIPTION = "Logratio analysis"; // TODO

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
            AbstractXYZDataset dataset = new LogratioDataset(pl, parameters);
            InterpolatingLookupPaintScale paintScale = new InterpolatingLookupPaintScale();
            paintScale.add(-1.00, new Color(0, 255, 0));
            paintScale.add(0.00, new Color(0, 0, 0));
            paintScale.add(1.00, new Color(255, 0, 0));

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
        return LogratioParameters.class;
    }

}
