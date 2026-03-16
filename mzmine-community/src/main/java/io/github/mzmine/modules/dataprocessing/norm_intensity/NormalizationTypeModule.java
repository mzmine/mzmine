/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTableUtils.InterpolationWeights;
import io.github.mzmine.parameters.ParameterSet;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Creates one normalization function per reference file.
 */
public interface NormalizationTypeModule extends MZmineModule {

  @NotNull Map<@NotNull RawDataFile, @NotNull NormalizationFunction> createReferenceFunctions(
      @NotNull List<@NotNull RawDataFile> referenceFiles, @NotNull ModularFeatureList featureList,
      @NotNull MetadataTable metadata, @NotNull ParameterSet mainParameters,
      @NotNull ParameterSet moduleSpecificParameters);

  @NotNull NormalizationFunction createInterpolatedFunction(@NotNull RawDataFile fileToInterpolate,
      @NotNull NormalizationFunction previousRunCalibration,
      @NotNull NormalizationFunction nextRunCalibration,
      @NotNull InterpolationWeights interpolationWeights, @NotNull MetadataTable metadata,
      @NotNull ParameterSet parameters, @NotNull ParameterSet normalizerParameters);

  @NotNull List<RawDataFile> getReferenceSamples(@NotNull final FeatureList flist,
      @NotNull final ParameterSet normalizationModuleParameters);
}
