package io.github.mzmine.modules.dataprocessing.comb_resolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.featdet_ML.MLFeatureResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.GeneralResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.Resolver;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;

public class CombinedResolverParameters extends GeneralResolverParameters {
    public static final ModuleOptionsEnumComboParameter<CombinedResolverEnum> firstResolverParameters = new ModuleOptionsEnumComboParameter<>(
            "First Resolver", "SElect the first resolver you want to combine.", new CombinedResolverEnum[] {
                    CombinedResolverEnum.ML_RESOLVER, CombinedResolverEnum.LOCAL_MIN },
            CombinedResolverEnum.LOCAL_MIN);

    public static final ModuleOptionsEnumComboParameter<CombinedResolverEnum> secondResolverParameters = new ModuleOptionsEnumComboParameter<>(
        "Second Resolver", "Select the second resolver you want to combine.", new CombinedResolverEnum[] { CombinedResolverEnum.LOCAL_MIN, CombinedResolverEnum.ML_RESOLVER },
        CombinedResolverEnum.ML_RESOLVER);

    public CombinedResolverParameters() {
        super(new Parameter[] {GeneralResolverParameters.PEAK_LISTS, GeneralResolverParameters.SUFFIX, firstResolverParameters, secondResolverParameters, handleOriginal});
    }

    public List<Resolver> getEmbeddedResolvers(ParameterSet comboParameters, ModularFeatureList originalFeatureList) {
        List<Resolver> resolvers = new ArrayList<>();
        ParameterSet firstEmbeddedParameters = firstResolverParameters.getEmbeddedParameters();
        ParameterSet secondEmbeddedParameters = secondResolverParameters.getEmbeddedParameters();
        switch(firstResolverParameters.getValue()){
            case CombinedResolverEnum.LOCAL_MIN -> resolvers.add(((MinimumSearchFeatureResolverParameters) firstEmbeddedParameters).getResolver(firstEmbeddedParameters,originalFeatureList));
            case CombinedResolverEnum.ML_RESOLVER -> resolvers.add(((MLFeatureResolverParameters) firstEmbeddedParameters).getResolver(firstEmbeddedParameters, originalFeatureList));
        }
        switch(secondResolverParameters.getValue()){
            case CombinedResolverEnum.LOCAL_MIN -> resolvers.add(((MinimumSearchFeatureResolverParameters) secondEmbeddedParameters).getResolver(secondEmbeddedParameters,originalFeatureList));
            case CombinedResolverEnum.ML_RESOLVER -> resolvers.add(((MLFeatureResolverParameters) secondEmbeddedParameters).getResolver(secondEmbeddedParameters, originalFeatureList));
        }
        return resolvers;
    }

    public Resolver getResolver(ParameterSet comboParameters, ModularFeatureList originalFeatureList) {
        return new CombinedResolver(comboParameters, originalFeatureList);
    }

    @Override
    public boolean checkParameterValues(Collection<String> errorMessages,
            boolean skipRawDataAndFeatureListParameters) {
        boolean value = super.checkParameterValues(errorMessages, skipRawDataAndFeatureListParameters);

        // final @NotNull ModularFeatureList[] flists = getValue(
        //         CombinedResolverParameters.flists).getMatchingFeatureLists();
        // final String error = Arrays.stream(flists).filter(flist -> flist.getNumberOfRawDataFiles() > 1)
        //         .map(ModularFeatureList::getName).collect(Collectors.joining(", "));

        // if (error != null && !error.isBlank()) {
        //     errorMessages.add("Feature lists " + error
        //             + " contain more than one raw file. This module is intended to be used directly after chromatogram detection, not after alignment.");
        // }

        if (!value || !errorMessages.isEmpty()) {
            return false;
        }
        return true;
    }

}
