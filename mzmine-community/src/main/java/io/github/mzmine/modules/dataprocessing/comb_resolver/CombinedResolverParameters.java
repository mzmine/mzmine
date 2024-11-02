package io.github.mzmine.modules.dataprocessing.comb_resolver;

import java.util.ArrayList;
import java.util.List;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.GeneralResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.Resolver;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;

public class CombinedResolverParameters extends SimpleParameterSet {
    public static CombinedResolverEnum[] selectedEnums = { CombinedResolverEnum.LOCALMIN,
            CombinedResolverEnum.MLRESOLVER };
    public static final ModuleOptionsEnumComboParameter<CombinedResolverEnum> selectedResolver = new ModuleOptionsEnumComboParameter<>(
            "Combined Resolver", "Select the resolvers you want to combined.", new CombinedResolverEnum[] {
                    CombinedResolverEnum.MLRESOLVER, CombinedResolverEnum.LOCALMIN },
            CombinedResolverEnum.LOCALMIN);

    public static final FeatureListsParameter flists = new FeatureListsParameter();

    public static final StringParameter suffix = new StringParameter("Suffix",
            "Suffix for the new feature list.", "comb");

    public static final OriginalFeatureListHandlingParameter handleOriginal = new OriginalFeatureListHandlingParameter(
            false);

    public CombinedResolverParameters() {
        super(flists, suffix, selectedResolver, handleOriginal);
    }

    public List<Resolver> getResolvers(ParameterSet comboParameters, ModularFeatureList originalFeatureList) {
        List<Resolver> resolvers = new ArrayList<>();
        for (CombinedResolverEnum currentEnum : CombinedResolverParameters.selectedEnums) {
            ParameterSet currentParam = CombinedResolverParameters.selectedResolver.getEmbeddedParameters(currentEnum);
            Resolver resolver = ((GeneralResolverParameters) currentParam).getResolver(currentParam,
                    originalFeatureList);
            resolvers.add(resolver);
        }
        return resolvers;
    }

}
