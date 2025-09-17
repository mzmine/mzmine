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
import com.opencsv.ICSVWriter;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.PseudoSpectrum;
import io.github.mzmine.datamodel.PseudoSpectrumType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.numbers.RTRangeType;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.io.export_features_sirius.SiriusExportTask;
import io.github.mzmine.util.CSVParsingUtils;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.exceptions.MissingMassListException;
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

  /**
   * @param row
   * @return Converted feature or null, e.g. if not enough information is present or the row is a
   * GC-EI row.
   */
  @Nullable
  public static FeatureImport feature(@NotNull FeatureListRow row) {
    if (!row.hasMs2Fragmentation() || !row.hasIsotopePattern() || (
        row.getMostIntenseFragmentScan() instanceof PseudoSpectrum ps
            && ps.getPseudoSpectrumType() == PseudoSpectrumType.GC_EI)) {
      return null;
    }
    final int charge = FeatureUtils.extractBestSignedChargeState(row,
        row.getMostIntenseFragmentScan()).orElse(1);
    if (Math.abs(charge) > 1) {
      // no support for multi charge species yet
      return null;
    }

    final FeatureImport f = new FeatureImport();
    f.setExternalFeatureId(String.valueOf(row.getID()));
    f.setName(String.valueOf(row.getID()));
    f.setIonMass(row.getAverageMZ());
    f.setCharge(charge);

    final IonIdentity adduct = row.getBestIonIdentity();
    if (adduct != null) {
      f.setDetectedAdducts(Set.of(adduct.toString()));
      f.setCharge(adduct.getIonType().getCharge());
    }

    f.setMs2Spectra(
        row.getAllFragmentScans().stream().map(MzmineToSirius::spectrum).filter(Objects::nonNull)
            .toList());
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
        || scan.getMassList().getBasePeakIntensity() == null || (scan instanceof PseudoSpectrum ps
        && ps.getPseudoSpectrumType() == PseudoSpectrumType.GC_EI)) {
      if (scan != null && scan.getMassList() == null) {
        throw new MissingMassListException(scan);
      }
      return null;
    }

    final MassList ml = scan.getMassList();
    BasicSpectrum spectrum = new BasicSpectrum();
    addPeaksToSiriusSpectrum(spectrum, ml);

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

    BasicSpectrum spectrum = new BasicSpectrum();
    addPeaksToSiriusSpectrum(spectrum, correlated);

    spectrum.setMsLevel(1);
    spectrum.setName("Correlated MS1 spectrum");
    return spectrum;
  }

  private static BasicSpectrum addPeaksToSiriusSpectrum(BasicSpectrum spectrum,
      MassSpectrum correlated) {
    final Double bpi = correlated.getBasePeakIntensity();
    spectrum.absIntensityFactor(bpi);

    for (int i = 0; i < correlated.getNumberOfDataPoints(); i++) {
      final SimplePeak p = new SimplePeak();
      p.setIntensity(correlated.getIntensityValue(i) / bpi);
      p.setMz(correlated.getMzValue(i));
      spectrum.addPeaksItem(p);
    }
    return spectrum;
  }

  public static SearchableDatabase toCustomDatabase(
      final @NotNull List<CompoundDBAnnotation> compounds, @NotNull Sirius sirius) {

    final Map<String, CompoundDBAnnotation> uniqueCompounds = compounds.stream()
        .filter(a -> a.getStructure() != null && a.getStructure().isomericSmiles() != null)
        .collect(Collectors.toMap(a -> a.getStructure().isomericSmiles(), a -> a, (a, _) -> a));
    final File dbFile = writeCompoundsToFile(uniqueCompounds);

    final SearchableDatabasesApi databases = sirius.api().databases();

    final SearchableDatabase database = databases.getCustomDatabases(false, false).stream()
        .filter(db -> db.getDatabaseId().equals(Sirius.mzmineCustomDbId)).findFirst()
        .orElseGet(() -> {
          SearchableDatabaseParameters dbParam = new SearchableDatabaseParameters();
          dbParam.setDisplayName(Sirius.mzmineCustomDbId);
          // .mzmine/external_resources/sirius/
          final File databaseFile = FileAndPathUtil.resolveInDownloadResourcesDir(
              "sirius/" + Sirius.mzmineCustomDbId);
          dbParam.setLocation(databaseFile.getAbsolutePath());

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

    try (final ICSVWriter writer = CSVParsingUtils.createDefaultWriter(file, '\t',
        WriterOptions.REPLACE);) {
      for (Entry<String, CompoundDBAnnotation> entry : db.entrySet()) {
        final String smiles = entry.getKey();
        final CompoundDBAnnotation annotation = entry.getValue();
        final String inChIKey = annotation.getInChIKey();
        final String name = annotation.getCompoundName();
        writer.writeNext(new String[]{smiles, Objects.requireNonNullElse(inChIKey, ""), name},
            false);
      }
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
   * {@link AlignedFeature#getAlignedFeatureId()}. Only contains the rows that were given to this
   * method.
   */
  public static Map<Integer, String> exportToSiriusUnique(@NotNull Sirius sirius,
      @NotNull List<? extends @NotNull FeatureListRow> rows) {
    sirius.checkLogin();

    final Map<Integer, String> alreadyImportedIds = SiriusToMzmine.getAllAlignedFeatureIds(sirius);

    // only send the features that are not already imported
    final List<? extends FeatureListRow> notImportedRows = rows.stream()
        .filter(r -> alreadyImportedIds.get(r.getID()) == null).toList();
    final List<FeatureImport> featureImports = notImportedRows.stream().map(MzmineToSirius::feature)
        .filter(Objects::nonNull).toList();
    final List<AlignedFeature> siriusFeatures = sirius.api().features()
        .addAlignedFeatures(sirius.getProject().getProjectId(), featureImports,
            InstrumentProfile.QTOF, List.of(AlignedFeatureOptField.NONE));

    final Map<Integer, @NotNull String> newlyImportedIds = siriusFeatures.stream()
        .filter(af -> af.getAlignedFeatureId() != null && af.getExternalFeatureId() != null)
        .collect(Collectors.toMap(af -> Integer.valueOf(af.getExternalFeatureId()),
            AlignedFeature::getAlignedFeatureId));

    final Map<Integer, String> idMap = new HashMap<>(newlyImportedIds);
    for (FeatureListRow row : rows) {
      final String siriusId = alreadyImportedIds.get(row.getID());
      if (siriusId != null) {
        idMap.put(row.getID(), siriusId);
      }
    }

    logger.info(() -> "Added features " + idMap.entrySet().stream()
        .map(e -> "%d->%s".formatted(e.getKey(), e.getValue())).collect(Collectors.joining("; ")));
    return idMap;
  }

}
