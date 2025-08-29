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

import com.google.common.collect.Range;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.numbers.RTRangeType;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.io.export_features_sirius.SiriusExportTask;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.WriterOptions;
import io.github.mzmine.util.scans.SpectraMerging;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntryFactory;
import io.sirius.ms.sdk.api.SearchableDatabasesApi;
import io.sirius.ms.sdk.model.AlignedFeature;
import io.sirius.ms.sdk.model.AlignedFeatureOptField;
import io.sirius.ms.sdk.model.BasicSpectrum;
import io.sirius.ms.sdk.model.FeatureImport;
import io.sirius.ms.sdk.model.InstrumentProfile;
import io.sirius.ms.sdk.model.SearchableDatabase;
import io.sirius.ms.sdk.model.SearchableDatabaseParameters;
import io.sirius.ms.sdk.model.SimplePeak;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MzmineToSirius {

  private static final Logger logger = Logger.getLogger(MzmineToSirius.class.getName());

  @Nullable
  public static FeatureImport feature(@NotNull FeatureListRow row) {
    if (!row.hasMs2Fragmentation() || !row.hasIsotopePattern()) {
      return null;
    }

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

    if (Math.abs(f.getCharge()) > 1) {
      // no support for multi charge species yet
      return null;
    }

    f.setMs2Spectra(row.getAllFragmentScans().stream().map(MzmineToSirius::spectrum).toList());
    f.setMs1Spectra(List.of(generateCorrelationSpectrum(row)));

    final Range<Float> rtRange = row.get(RTRangeType.class);
    if (rtRange != null) {
      f.setRtStartSeconds(rtRange.lowerEndpoint() * 60d);
      f.setRtEndSeconds(rtRange.upperEndpoint() * 60d);
    }
    f.setRtApexSeconds(row.getAverageRT() * 60d);

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
    final File dbFile = writeCompoundsToFile(uniqueCompounds);

    final SearchableDatabasesApi databases = sirius.api().databases();
    databases.getCustomDatabases(false, false);

    final SearchableDatabase database = databases.getCustomDatabases(false, false).stream()
        .filter(db -> db.getDatabaseId().equals(Sirius.mzmineCustomDbId)).findFirst()
        .orElseGet(() -> {
          SearchableDatabaseParameters dbParam = new SearchableDatabaseParameters();
          dbParam.setDisplayName(Sirius.mzmineCustomDbId);
          dbParam.setLocation(new File(new File(FileAndPathUtil.getMzmineDir(), "sirius_databases"),
              Sirius.mzmineCustomDbId).getAbsolutePath());
          return databases.createDatabase(Sirius.mzmineCustomDbId, dbParam);
        });

    databases.importIntoDatabase(database.getDatabaseId(), List.of(dbFile), 1000, null);
    return database;
  }

  private static File writeCompoundsToFile(final Map<String, CompoundDBAnnotation> db) {
    final File file = new File(new File(FileAndPathUtil.getTempDir(), "sirius_databases"),
        "custom_%s.tsv".formatted(Sirius.getDefaultSessionId()));
    file.deleteOnExit();

    if (!file.getParentFile().exists()) {
      file.getParentFile().mkdirs();
    }

    try (var bufferedWriter = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8,
        WriterOptions.REPLACE.toOpenOption())) {
      CSVWriterBuilder builder = new CSVWriterBuilder(bufferedWriter).withSeparator('\t');
      final ICSVWriter writer = builder.build();

      for (Entry<String, CompoundDBAnnotation> entry : db.entrySet()) {
        final String smiles = entry.getKey();
        final CompoundDBAnnotation annotation = entry.getValue();
        final String inChIKey = annotation.getInChIKey();
        final String name = annotation.getCompoundName();
        writer.writeNext(new String[]{smiles, Objects.requireNonNullElse(inChIKey, ""), name},
            false);
      }
      bufferedWriter.flush();
    } catch (IOException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      throw new RuntimeException(e);
    }
    return file;
  }

  /**
   * Exports rows to sirius. checks the current project. Only exports rows that are not already
   * contained in the sirius project.
   *
   * @param sirius The sirius session.
   * @param rows   The rows to export
   * @return A mapping of mzmine feature id {@link FeatureListRow#getID()} to sirius unique id
   * {@link AlignedFeature#getAlignedFeatureId()}.
   */
  public static Map<Integer, String> exportToSiriusUnique(@NotNull Sirius sirius,
      @NotNull List<? extends @NotNull FeatureListRow> rows) {
    sirius.checkLogin();
    final List<FeatureImport> features = rows.stream().map(MzmineToSirius::feature)
        .filter(Objects::nonNull).toList();

    final Map<Integer, String> mzmineIdToSiriusId = sirius.api().features()
        .getAlignedFeatures(sirius.getProject().getProjectId(), null,
            List.of(AlignedFeatureOptField.NONE)).stream().collect(Collectors.toMap(
            alignedFeature -> Integer.valueOf(alignedFeature.getExternalFeatureId()),
            AlignedFeature::getAlignedFeatureId));

    final List<FeatureImport> notImportedFeatures = features.stream()
        .filter(f -> mzmineIdToSiriusId.get(Integer.valueOf(f.getExternalFeatureId())) == null)
        .toList();

    var imported = sirius.api().features()
        .addAlignedFeatures(sirius.getProject().getProjectId(), notImportedFeatures,
            InstrumentProfile.QTOF, List.of(AlignedFeatureOptField.NONE));
    var mzmineIdToSiriusId2 = imported.stream().collect(
        Collectors.toMap(AlignedFeature::getExternalFeatureId,
            AlignedFeature::getAlignedFeatureId));

    final HashMap<Integer, String> idMap = new HashMap<>(mzmineIdToSiriusId);
    mzmineIdToSiriusId2.forEach(
        (mzmineId, siriusId) -> idMap.put(Integer.valueOf(mzmineId), siriusId));

    logger.info(() -> "Added features " + idMap.entrySet().stream()
        .map(e -> "%d->%s".formatted(e.getKey(), e.getValue())).collect(Collectors.joining("; ")));
    return idMap;
  }
}
