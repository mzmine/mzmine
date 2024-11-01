package io.github.mzmine.modules.dataprocessing.comb_resolver;

import java.util.List;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.Resolver;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;

public class CombinedResolverParameters extends SimpleParameterSet{
    public static final ModuleOptionsEnumComboParameter<CombinedResolverEnum> selectedResolver = new ModuleOptionsEnumComboParameter<>(
            "Baseline corrector", "Select the baseline correction algorithm.", new CombinedResolverEnum[] {
                    CombinedResolverEnum.MLRESOLVER, CombinedResolverEnum.LOCALMIN }, CombinedResolverEnum.LOCALMIN);


  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final StringParameter suffix = new StringParameter("Suffix",
      "Suffix for the new feature list.", "comb");

  public static final OriginalFeatureListHandlingParameter handleOriginal = new OriginalFeatureListHandlingParameter(
      false);

    public CombinedResolverParameters() {
        super(flists, suffix, selectedResolver, handleOriginal);
    }

	public List<Resolver> getResolvers(ParameterSet parameters, ModularFeatureList originalFeatureList) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getResolver'");
	}

}
