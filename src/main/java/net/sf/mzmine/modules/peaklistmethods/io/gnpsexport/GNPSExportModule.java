/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang 
 * at the Dorrestein Lab (University of California, San Diego). 
 * 
 * It is freely available under the GNU GPL licence of MZmine2.
 * 
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 * 
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package net.sf.mzmine.modules.peaklistmethods.io.gnpsexport;

import java.util.Collection;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

public class GNPSExportModule implements MZmineProcessingModule {
	private static final String MODULE_NAME = "Export for GNPS";
    private static final String MODULE_DESCRIPTION = "Export MGF file for GNPS (only for MS/MS)";
    
	@Override
	public String getDescription() {
		return MODULE_DESCRIPTION;
	}

	@Override
    @Nonnull
    public ExitCode runModule(MZmineProject project, ParameterSet parameters, Collection<Task> tasks) {
    	GNPSExportTask task = new GNPSExportTask(parameters);
	tasks.add(task);
	return ExitCode.OK;

    }

	@Override
	public MZmineModuleCategory getModuleCategory() {
		return MZmineModuleCategory.PEAKLISTEXPORT;
	}

	@Override
	public String getName() {
		return MODULE_NAME;
	}

	@Override
	public Class<? extends ParameterSet> getParameterSetClass() {
		return GNPSExportParameters.class;
	}

}
