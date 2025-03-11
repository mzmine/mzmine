/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.datamodel.features.types.annotations.iin;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.IntegerType;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.modules.visualization.chromatogram.TICVisualizerTab;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The ID from Ion Identity Networking (IIN), which searches for different ions describing the same
 * molecule. See {@link IonIdentity} and {@link IonNetwork}
 */
public class IonNetworkIDType extends IntegerType {

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "iin_id";
  }

  @NotNull
  @Override
  public String getHeaderString() {
    return "IIN ID";
  }

  @Nullable
  @Override
  public Runnable getDoubleClickAction(final @Nullable FeatureTableFX table, @NotNull ModularFeatureListRow row,
      @NotNull List<RawDataFile> file, DataType<?> superType, @Nullable final Object value) {

    var ionIdentity = row.getBestIonIdentity();
    if (ionIdentity == null) {
      return null;
    }

    return () -> {
      final IonNetwork network = ionIdentity.getNetwork();
      ModularFeature bestFeature = row.getBestFeature();
      final RawDataFile bestRaw = bestFeature.getRawDataFile();

      Map<Feature, String> labels = new HashMap<>();
      List<Feature> features = network.keySet().stream().filter(r -> r.getFeature(bestRaw) != null)
          .<Feature>mapMulti((r, c) -> {
            Feature feature = r.getFeature(bestRaw);
            labels.put(feature, network.get(r).getAdduct());
            c.accept(feature);
          }).toList();

      TICVisualizerTab tab = new TICVisualizerTab(new RawDataFile[]{bestRaw}, TICPlotType.BASEPEAK,
          new ScanSelection(1), bestFeature.getRawDataPointsMZRange(), features, labels, null);
      MZmineCore.getDesktop().addTab(tab);
    };
  }

  @Override
  public boolean getDefaultVisibility() {
    return true;
  }
}
