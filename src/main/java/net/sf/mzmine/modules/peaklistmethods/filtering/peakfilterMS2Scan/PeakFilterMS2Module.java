/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang 
 * at the Dorrestein Lab (University of California, San Diego). 
 * 
 * It is freely available under the GNU GPL licence of MZmine2.
 * 
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 */

package net.sf.mzmine.modules.peaklistmethods.filtering.peakfilterMS2Scan;

import java.util.Collection;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

/**
 * Implements a filter for alignment results. The filter removes rows that have
 * fewer than a defined number of peaks detected and other conditions.
 */
public class PeakFilterMS2Module implements MZmineProcessingModule {

    private static final String MODULE_NAME =  "Keep only features with MS/MS scan (GNPS)";
    private static final String MODULE_DESCRIPTION = "This method removes certain peak entries that do not have MS scans";

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

        final PeakList[] peakLists = parameters
                .getParameter(PeakFilterMS2Parameters.PEAK_LISTS).getValue()
                .getMatchingPeakLists();

        for (PeakList peakList : peakLists) {

            Task newTask = new PeakFilterMS2Task(project, peakList, parameters);
            tasks.add(newTask);

        }

        return ExitCode.OK;
    }

    @Override
    public @Nonnull MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.PEAKLISTFILTERING;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return PeakFilterMS2Parameters.class;
    }
}
