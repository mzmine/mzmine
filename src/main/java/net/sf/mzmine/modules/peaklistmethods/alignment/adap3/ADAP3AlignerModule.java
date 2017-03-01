/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.mzmine.modules.peaklistmethods.alignment.adap3;

import java.util.Collection;
import javax.annotation.Nonnull;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

/**
 *
 * @author aleksandrsmirnov
 */
public class ADAP3AlignerModule implements MZmineProcessingModule {
    
    private static final String MODULE_NAME = "ADAP Aligner";
    private static final String MODULE_DESCRIPTION = "This module calculates "
            + "pairwise convolution integral for each pair of unaligned peaks "
            + "in order to find the best alignment";

    @Override
    public @Nonnull String getName() {
	return MODULE_NAME;
    }

    @Override
    public @Nonnull String getDescription() {
	return MODULE_DESCRIPTION;
    }
    
    @Override @Nonnull
    public MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.ALIGNMENT;
    }
    
    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
	return ADAP3AlignerParameters.class;
    }
    
    @Override @Nonnull
    public ExitCode runModule(@Nonnull MZmineProject project,
            @Nonnull ParameterSet parameters, @Nonnull Collection<Task> tasks) 
    {
        Task newTask = new ADAP3AlignerTask(project, parameters);
        tasks.add(newTask);
        return ExitCode.OK;
    }
}
