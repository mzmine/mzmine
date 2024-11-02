package io.github.mzmine.modules.dataprocessing.comb_resolver;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.Range;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.AbstractResolver;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.Resolver;
import io.github.mzmine.parameters.ParameterSet;

public class CombinedResolver extends AbstractResolver {

    CombinedResolverParameters parameters;

    protected CombinedResolver(@NotNull ParameterSet parameters, @NotNull ModularFeatureList flist) throws Exception {
        super(parameters, flist);
        if (!(parameters instanceof CombinedResolverParameters)) {
            // place error message here
        }
        this.parameters = (CombinedResolverParameters) parameters;
    }

    @Override
    public @NotNull List<Range<Double>> resolve(double[] x, double[] y) {
        List<Resolver> resolvers = ((CombinedResolverParameters) this.parameters).getResolvers(this.parameters,this.flist);
        if (resolvers.size() == 0) {
            // add logging message
        }
        List<Range<Double>> resolvedRanges = new ArrayList<>();
        for (Resolver resolver : resolvers) {
            resolvedRanges.addAll(resolver.resolve(x, y));
        }

        return resolvedRanges;
    }

    @Override
    public @NotNull Class<? extends MZmineModule> getModuleClass() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getModuleClass'");
    }
}
