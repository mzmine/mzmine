package io.github.mzmine.modules.dataprocessing.comb_resolver;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.FeatureDataAccess;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.GeneralResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.Resolver;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;

public class CombinedResolverTask extends AbstractFeatureListTask {

    private int processedRows;
    private int totalRows;

    /**
     * @param storage        The {@link MemoryMapStorage} used to store results of
     *                       this task (e.g.
     *                       RawDataFiles, MassLists, FeatureLists). May be null if
     *                       results shall be
     *                       stored in ram. For now, one storage should be created
     *                       per module call in
     * @param moduleCallDate the call date of module to order execution order
     * @param parameters
     * @param moduleClass
     */
    protected CombinedResolverTask(@Nullable MemoryMapStorage storage,
            @NotNull Instant moduleCallDate, @NotNull ParameterSet parameters,
            @NotNull Class<? extends MZmineModule> moduleClass) {
        super(storage, moduleCallDate, parameters, moduleClass);
        this.processedRows = 0;
        this.totalRows = 0;
    }

    @java.lang.Override
    protected void process() {

    }

    public void startResolvers(ModularFeatureList originalFeatureList) {
        // TODO implement method
        final List<Resolver> resolvers = ((CombinedResolverParameters) parameters).getResolvers(parameters,
                originalFeatureList);
        if (resolvers.isEmpty()) {
            setErrorMessage("No Resolver could be found.");
            setStatus(TaskStatus.ERROR);
            return;
        }

        final RawDataFile dataFile = originalFeatureList.getRawDataFile(0);
        final ModularFeatureList resolvedFeatureList = createNewFeatureList(originalFeatureList);

        final FeatureDataAccess access = EfficientDataAccess.of(originalFeatureList,
                EfficientDataAccess.FeatureDataType.INCLUDE_ZEROS, dataFile);

        processedRows = 0;
        totalRows = originalFeatureList.getNumberOfRows();
        while (access.hasNextFeature()) {
            final ModularFeature originalFeature = (ModularFeature) access.nextFeature();
            for (int i = 0; i < resolvers.size(); i++) {
                Resolver resolver = resolvers.get(i);
                final List<IonTimeSeries<? extends Scan>> resolvedSeries = resolver.resolve(access,
                        getMemoryMapStorage());
            }
            processedRows++;
        }
    }

    @java.lang.Override
    protected @NotNull List<FeatureList> getProcessedFeatureLists() {
        return null;
    }

    @java.lang.Override
    public String getTaskDescription() {
        return null;
    }
}
