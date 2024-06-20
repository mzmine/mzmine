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

package io.github.mzmine.modules.tools.siriusapi;

import de.unijena.bioinf.ms.nightsky.sdk.model.BasicSpectrum;
import de.unijena.bioinf.ms.nightsky.sdk.model.FeatureImport;
import de.unijena.bioinf.ms.nightsky.sdk.model.SimplePeak;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.io.export_features_sirius.SiriusExportTask;
import io.github.mzmine.util.scans.SpectraMerging;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MzmineToSirius {

  public static FeatureImport feature(FeatureListRow row) {
    final FeatureImport f = new FeatureImport();
    f.setFeatureId(String.valueOf(row.getID()));
    f.setName(String.valueOf(row.getID()));
    f.setIonMass(row.getAverageMZ());

    final IonIdentity adduct = row.getBestIonIdentity();
    if (adduct != null) {
      f.setAdduct(adduct.toString());
    }

    f.setMs2Spectra(row.getAllFragmentScans().stream().map(MzmineToSirius::spectrum).toList());
    f.setMs1Spectra(List.of(generateCorrelationSpectrum(row)));
    f.setRtStartSeconds(row.getAverageRT() * 60d);
    f.setRtEndSeconds(row.getAverageRT() * 60d);

    return f;
  }

  public static @Nullable BasicSpectrum spectrum(@Nullable Scan scan) {
    if (scan == null || scan.getMassList() == null
        || scan.getMassList().getNumberOfDataPoints() == 0
        || scan.getMassList().getBasePeakIntensity() == null) {
      return null;
    }

    final MassList ml = scan.getMassList();
    final BasicSpectrum spectrum = new BasicSpectrum();
    final Double bpi = ml.getBasePeakIntensity();
    spectrum.absIntensityFactor(bpi);

    for (int i = 0; i < ml.getNumberOfDataPoints(); i++) {
      final SimplePeak p = new SimplePeak();
      p.setIntensity(ml.getIntensityValue(i) / bpi);
      p.setMz(ml.getMzValue(i));
      spectrum.addPeaksItem(p);
    }

    final MsMsInfo msMsInfo = scan.getMsMsInfo();
    if (msMsInfo != null) {
      spectrum.setCollisionEnergy(String.valueOf(msMsInfo.getActivationEnergy()));
      spectrum.setPrecursorMz(scan.getPrecursorMz());
    }
    spectrum.setMsLevel(scan.getMSLevel());

    final NumberFormats formats = ConfigService.getExportFormats();
    spectrum.setName(
        "%s-Scan_%d-%smin".formatted(scan.getDataFile().getName(), scan.getScanNumber(),
            formats.rt(scan.getRetentionTime())));
    return spectrum;
  }

  private static BasicSpectrum generateCorrelationSpectrum(FeatureListRow row) {
    final MassSpectrum correlated = SiriusExportTask.generateCorrelationSpectrum(row, null,
        SpectraMerging.defaultMs1MergeTol);

    if (correlated == null) {
      return spectrum(row.getBestFeature().getRepresentativeScan());
    }

    final BasicSpectrum spectrum = new BasicSpectrum();
    final Double bpi = correlated.getBasePeakIntensity();
    spectrum.absIntensityFactor(bpi);

    for (int i = 0; i < correlated.getNumberOfDataPoints(); i++) {
      final SimplePeak p = new SimplePeak();
      p.setIntensity(correlated.getIntensityValue(i) / bpi);
      p.setMz(correlated.getMzValue(i));
      spectrum.addPeaksItem(p);
    }

    spectrum.setMsLevel(1);
    spectrum.setName("Correlated MS1 spectrum");
    return spectrum;
  }


  public static void toCustomDatabase(final @NotNull List<CompoundDBAnnotation> compounds) {
  }
}
