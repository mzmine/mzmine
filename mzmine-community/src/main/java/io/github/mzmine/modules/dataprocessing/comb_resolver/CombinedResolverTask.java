package io.github.mzmine.modules.dataprocessing.comb_resolver;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.MemoryMapStorage;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.time.Instant;

import org.jetbrains.annotations.NotNull;

public class CombinedResolverTask extends AbstractFeatureListTask {
    private static Logger logger = Logger.getLogger(CombinedResolverTask.class.getName());

    public CombinedResolverTask(MZmineProject project, CombinedResolverParameters parameters, @NotNull MemoryMapStorage storage, @NotNull Instant moduleCallDate){
        super(storage, moduleCallDate);
    }

    protected List<FeatureList> getProcessedFeatureLists() {
        return new ArrayList<>();
    }

    protected List<RawDataFile> getProcessedDataFiles() {
        return new ArrayList<>();
    }

    public String getTaskDescription() {
        return "";
    }

    protected void process() {
    }
}
