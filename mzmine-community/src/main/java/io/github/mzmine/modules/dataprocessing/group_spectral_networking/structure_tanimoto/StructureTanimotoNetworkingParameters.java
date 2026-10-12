/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
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

package io.github.mzmine.modules.dataprocessing.group_spectral_networking.structure_tanimoto;

import io.github.mzmine.modules.tools.molecular_similarity.tanimoto.FingerprintType;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import org.jetbrains.annotations.NotNull;

/**
 * Structural (Tanimoto) similarity networking based on the molecular structures of the preferred
 * annotations of each {@link io.github.mzmine.datamodel.features.FeatureListRow}.
 */
public class StructureTanimotoNetworkingParameters extends SimpleParameterSet {

  public static final ComboParameter<FingerprintType> fingerprint = new ComboParameter<>(
      "Fingerprint",
      "The CDK molecular fingerprint preset used to compute the Tanimoto similarity. ECFP4/ECFP6 "
          + "are circular (Morgan) fingerprints and the common choice for structural similarity; "
          + "Daylight/Extended are path-based; MACCS and PubChem are fixed-length structural keys. "
          + "The bit length (1024/2048) trades collisions against speed/memory.",
      FingerprintType.values(), FingerprintType.ECFP4_2048);

  public static final PercentParameter minSimilarity = new PercentParameter("Min similarity",
      "Minimum Tanimoto similarity (0-1) between two rows to create a structure similarity edge.",
      0.7, 0.0, 1.0);

  public static final IntegerParameter maxStructuresPerRow = new IntegerParameter(
      "Max structures per row",
      "Maximum number of distinct structures (by InChIKey first block) considered per row. The "
          + "top annotations by confidence are used and the maximum similarity across all structure "
          + "pairs is reported.", 3, 1, null);

  public StructureTanimotoNetworkingParameters() {
    super(
        "https://mzmine.github.io/mzmine_documentation/module_docs/group_spectral_net/molecular_networking.html",
        fingerprint, minSimilarity, maxStructuresPerRow);
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
