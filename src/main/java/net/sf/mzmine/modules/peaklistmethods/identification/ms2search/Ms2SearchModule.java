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

package net.sf.mzmine.modules.peaklistmethods.identification.ms2search;

import java.util.Collection;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

public class Ms2SearchModule implements MZmineProcessingModule {

    private static final String MODULE_NAME = "MS2 similarity search";
    private static final String MODULE_DESCRIPTION = "This method searches for similar MS2 fragmentation spectra between two peaklists";

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

        PeakList peakLists1[] = parameters
                .getParameter(Ms2SearchParameters.peakLists1).getValue()
                .getMatchingPeakLists();
        
        PeakList peakLists2[] = parameters
                .getParameter(Ms2SearchParameters.peakLists2).getValue()
                .getMatchingPeakLists();

        
        //Setup new peaklists for concatenation
        PeakList concatenatedPeakList1 = new SimplePeakList(peakLists1[0].getRawDataFiles()[0].getName(), 
                peakLists1[0].getRawDataFiles()[0]);
        PeakList concatenatedPeakList2 = new SimplePeakList(peakLists2[0].getRawDataFiles()[0].getName(), 
                peakLists2[0].getRawDataFiles()[0]);
        
        //Concatenate peaklists
        for (PeakList pl : peakLists1)
        {
            for (PeakListRow plrow : pl.getRows())
            {
                concatenatedPeakList1.addRow(plrow);
            }
        }
        
        for (PeakList pl : peakLists2)
        {
            for (PeakListRow plrow : pl.getRows())
            {
                concatenatedPeakList2.addRow(plrow);
            }
        }
        
        //PeakList peakList1 = peakLists1[0];
        //PeakList peakList2 = peakLists2[0];
        PeakList peakList1 = concatenatedPeakList1;
        PeakList peakList2 = concatenatedPeakList2;
        

        //Previously iterated over all the peaklists & did a separate task for each.
        //Now a single task.
        Task newTask = new Ms2SearchTask(parameters, peakList1, peakList2);
        tasks.add(newTask);


        return ExitCode.OK;
    }

    @Override
    public @Nonnull MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.IDENTIFICATION;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return Ms2SearchParameters.class;
    }

}
