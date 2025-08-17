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

import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
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
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.scans.SpectraMerging;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntryFactory;
import io.sirius.ms.sdk.api.SearchableDatabasesApi;
import io.sirius.ms.sdk.model.BasicSpectrum;
import io.sirius.ms.sdk.model.BioTransformerParameters;
import io.sirius.ms.sdk.model.FeatureImport;
import io.sirius.ms.sdk.model.SearchableDatabase;
import io.sirius.ms.sdk.model.SimplePeak;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MzmineToSirius {

  private static final Logger logger = Logger.getLogger(MzmineToSirius.class.getName());

  public static FeatureImport feature(FeatureListRow row) {
    final FeatureImport f = new FeatureImport();
    f.setExternalFeatureId(String.valueOf(row.getID()));
    f.setName(String.valueOf(row.getID()));
    f.setIonMass(row.getAverageMZ());
    f.setCharge(
        FeatureUtils.extractBestSignedChargeState(row, row.getMostIntenseFragmentScan()).orElse(1));

    final IonIdentity adduct = row.getBestIonIdentity();
    if (adduct != null) {
      f.setDetectedAdducts(Set.of(adduct.toString()));
      f.setCharge(adduct.getIonType().getCharge());
    }

    f.setMs2Spectra(row.getAllFragmentScans().stream().map(MzmineToSirius::spectrum).toList());
    f.setMs1Spectra(List.of(generateCorrelationSpectrum(row)));
    f.setRtStartSeconds(row.getBestFeature().getRawDataPointsRTRange().lowerEndpoint() * 60d);
    f.setRtEndSeconds(row.getBestFeature().getRawDataPointsRTRange().upperEndpoint() * 60d);

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
    SpectralLibraryEntryFactory factory = new SpectralLibraryEntryFactory(true, false, false,
        false);
    final MassSpectrum correlated = SiriusExportTask.generateCorrelationSpectrum(factory,
        SpectraMerging.defaultMs1MergeTol, row, null, null);

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

  public static SearchableDatabase toCustomDatabase(
      final @NotNull List<CompoundDBAnnotation> compounds, @NotNull Sirius sirius) {

    final Map<String, CompoundDBAnnotation> uniqueCompounds = compounds.stream()
        .filter(a -> a.getSmiles() != null)
        .collect(Collectors.toMap(CompoundDBAnnotation::getSmiles, a -> a));
    final File dbFile = writeCustomDatabase(uniqueCompounds);

    final SearchableDatabasesApi databases = sirius.api().databases();
    final SearchableDatabase database = databases.importIntoDatabase(
        FileAndPathUtil.eraseFormat(dbFile).getName(), List.of(dbFile), 1000, null);
    database.customDb(true);
    return database;
  }

  private static File writeCustomDatabase(final Map<String, CompoundDBAnnotation> db) {
    final File file = new File(new File(FileAndPathUtil.getMzmineDir(), "sirius_databases"),
        "custom_%s".formatted(LocalDateTime.now().truncatedTo(ChronoUnit.DAYS).toString()));
    if (!file.getParentFile().exists()) {
      file.getParentFile().mkdirs();
    }

    try (var bufferedWriter = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8,
        StandardOpenOption.WRITE, StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {
      CSVWriterBuilder builder = new CSVWriterBuilder(bufferedWriter).withSeparator('\t');
      final ICSVWriter writer = builder.build();

      for (Entry<String, CompoundDBAnnotation> entry : db.entrySet()) {
        final String smiles = entry.getKey();
        final CompoundDBAnnotation annotation = entry.getValue();
        final String name = annotation.getCompoundName();
        writer.writeNext(new String[]{smiles, name}, false);
      }
      bufferedWriter.flush();
    } catch (IOException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      throw new RuntimeException(e);
    }
    return file;
  }
}
