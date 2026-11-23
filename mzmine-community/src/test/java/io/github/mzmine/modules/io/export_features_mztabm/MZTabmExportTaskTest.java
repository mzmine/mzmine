package io.github.mzmine.modules.io.export_features_mztabm;

import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.MergedMassSpectrum;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.impl.SimpleFeatureIdentity;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javafx.collections.FXCollections;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.lifstools.mztab2.io.MzTabFileParser;
import org.lifstools.mztab2.model.Assay;
import org.lifstools.mztab2.model.MzTab;
import org.lifstools.mztab2.model.MzTabProfile;
import org.lifstools.mztab2.model.SmallMoleculeEvidence;
import org.lifstools.mztab2.model.SmallMoleculeFeature;
import org.lifstools.mztab2.model.SmallMoleculeSummary;
import org.lifstools.mztab2.model.StudyVariable;
import org.mockito.Mockito;
import uk.ac.ebi.pride.jmztab2.utils.errors.MZTabErrorType;

/**
 * Exercises mzTab-M export through the task and the real jmzTab-m writer.
 */
class MZTabmExportTaskTest {

  @TempDir
  Path tempDir;

  /**
   * A finished export must produce a nonempty file, rather than merely complete the task.
   *
   * @throws IOException if the exported file cannot be inspected
   */
  @Test
  void exportsIdentifiedFeatureToFile() throws IOException {
    final ModularFeatureList featureList = createFeatureList("Features", "sample.mzML",
        createIdentity("https://example.org/compound/1"));
    final Path output = tempDir.resolve("features.mzTab");
    final MzTab exported = runExport(output, featureList);

    Assertions.assertEquals("2.0.0-M", exported.getMetadata().getMzTabVersion());
    Assertions.assertEquals(MzTabProfile.M_S_F_E, exported.getMetadata().getMzTabProfile());
    Assertions.assertEquals("Features", exported.getMetadata().getDescription());
    Assertions.assertEquals(1, exported.getSmallMoleculeSummary().size());
    Assertions.assertEquals(1, exported.getSmallMoleculeFeature().size());
    Assertions.assertEquals(1, exported.getSmallMoleculeEvidence().size());

    final SmallMoleculeSummary summary = exported.getSmallMoleculeSummary().getFirst();
    final SmallMoleculeFeature feature = exported.getSmallMoleculeFeature().getFirst();
    final SmallMoleculeEvidence evidence = exported.getSmallMoleculeEvidence().getFirst();
    Assertions.assertEquals(List.of(feature.getSmfId()), summary.getSmfIdRefs());
    Assertions.assertEquals(List.of(evidence.getSmeId()), feature.getSmeIdRefs());
    Assertions.assertEquals(List.of("Caffeine"), summary.getChemicalName());
    Assertions.assertEquals(List.of("C8H10N4O2"), summary.getChemicalFormula());
    Assertions.assertEquals(List.of("SimpleFeatureIdentity_1:TEST:1"), summary.getDatabaseIdentifier());
    Assertions.assertEquals(List.of(URI.create("https://example.org/compound/1")), summary.getUri());
    Assertions.assertEquals("Caffeine", evidence.getChemicalName());
    Assertions.assertEquals("C8H10N4O2", evidence.getChemicalFormula());
    Assertions.assertEquals("SimpleFeatureIdentity_1:TEST:1", evidence.getDatabaseIdentifier());
    Assertions.assertEquals(URI.create("https://example.org/compound/1"), evidence.getUri());
    Assertions.assertEquals(195.08765, feature.getExpMassToCharge(), 1e-8);
    Assertions.assertEquals(90d, feature.getRetentionTimeInSeconds(), 1e-8);
    Assertions.assertEquals(195.08765, evidence.getExpMassToCharge(), 1e-8);
    Assertions.assertEquals(1, feature.getCharge());
    Assertions.assertEquals(List.of(1250d), summary.getAbundanceAssay());
    Assertions.assertEquals(List.of(1250d), feature.getAbundanceAssay());
    Assertions.assertEquals("index=0", evidence.getSpectraRef().getFirst().getReference());
    Assertions.assertEquals(1, evidence.getSpectraRef().getFirst().getMsRun().getId());

    final StudyVariable studyVariable = exported.getMetadata().getStudyVariable().getFirst();
    Assertions.assertEquals(1, exported.getMetadata().getStudyVariable().size());
    Assertions.assertEquals("undefined", studyVariable.getName());
    Assertions.assertEquals(List.of(1), studyVariable.getAssayRefs().stream().map(Assay::getId).toList());
    Assertions.assertEquals(List.of(1250d), summary.getAbundanceStudyVariable());
    Assertions.assertEquals(List.of(0d), summary.getAbundanceVariationStudyVariable());
  }

  /**
   * All assays must belong to the default study variable and contribute their measured areas.
   *
   * @throws IOException if the exported file cannot be inspected
   */
  @Test
  void exportsMultipleAssaysAndTheirStudyVariableAbundance() throws IOException {
    final FeatureIdentity identity = createIdentity("https://example.org/compound/1");
    final ModularFeatureList featureList = createFeatureList("Features", "first.mzML", identity);
    final ModularFeatureList secondList = createFeatureList("Second", "second.mzML", identity);
    final RawDataFile firstFile = featureList.getRawDataFiles().getFirst();
    final RawDataFile secondFile = secondList.getRawDataFiles().getFirst();
    final Feature secondFeature = secondList.getRows().getFirst().getFeature(secondFile);
    Mockito.when(secondFeature.getArea()).thenReturn(3750f);
    Mockito.when(featureList.getRows().getFirst().getFeature(secondFile)).thenReturn(secondFeature);
    Mockito.when(featureList.getRawDataFiles()).thenReturn(List.of(firstFile, secondFile));
    Mockito.when(featureList.getRows().getFirst().getRawDataFiles())
        .thenReturn(List.of(firstFile, secondFile));

    final MzTab exported = runExport(tempDir.resolve("assays.mzTab"), featureList);

    final StudyVariable studyVariable = exported.getMetadata().getStudyVariable().getFirst();
    final SmallMoleculeSummary summary = exported.getSmallMoleculeSummary().getFirst();
    Assertions.assertEquals(2, exported.getMetadata().getAssay().size());
    Assertions.assertEquals(List.of(1, 2),
        studyVariable.getAssayRefs().stream().map(Assay::getId).toList());
    Assertions.assertEquals(List.of(1250d, 3750d), summary.getAbundanceAssay());
    Assertions.assertEquals(List.of(1250d, 3750d),
        exported.getSmallMoleculeFeature().getFirst().getAbundanceAssay());
    Assertions.assertEquals(List.of(2500d), summary.getAbundanceStudyVariable());
    Assertions.assertEquals(0.70710678, summary.getAbundanceVariationStudyVariable().getFirst(), 1e-8);
    Assertions.assertEquals("MS:1002962", studyVariable.getAverageFunction().getCvAccession());
    Assertions.assertEquals("MS:1002963", studyVariable.getVariationFunction().getCvAccession());
    Assertions.assertEquals(List.of(1, 2), exported.getSmallMoleculeEvidence().getFirst()
        .getSpectraRef().stream().map(reference -> reference.getMsRun().getId()).toList());
  }

  /**
   * Unknown rows keep measured values without invented identification evidence or zero imputation.
   *
   * @throws IOException if the exported file cannot be inspected
   */
  @Test
  void exportsMixedAnnotationsAndMissingAssayMeasurements() throws IOException {
    final MzTab exported = runExport(tempDir.resolve("mixed.mzTab"), createMixedFeatureList());

    Assertions.assertEquals(2, exported.getSmallMoleculeSummary().size());
    Assertions.assertEquals(2, exported.getSmallMoleculeFeature().size());
    Assertions.assertEquals(1, exported.getSmallMoleculeEvidence().size());
    Assertions.assertEquals("MZmine annotation status",
        exported.getMetadata().getSmallMoleculeIdentificationReliability().getName());
    final SmallMoleculeSummary identified = exported.getSmallMoleculeSummary().getFirst();
    final SmallMoleculeSummary unknown = exported.getSmallMoleculeSummary().get(1);
    Assertions.assertEquals("annotated", identified.getReliability());
    Assertions.assertEquals("unannotated", unknown.getReliability());
    Assertions.assertEquals(Arrays.asList(1250d, null), identified.getAbundanceAssay());
    Assertions.assertEquals(Arrays.asList(null, 1250d), unknown.getAbundanceAssay());
    Assertions.assertEquals(List.of(1250d), identified.getAbundanceStudyVariable());
    Assertions.assertEquals(List.of(1250d), unknown.getAbundanceStudyVariable());
    Assertions.assertTrue(unknown.getChemicalName().stream()
        .allMatch(name -> name == null || "null".equals(name)));
    Assertions.assertTrue(exported.getSmallMoleculeFeature().get(1).getSmeIdRefs().stream()
        .allMatch(id -> id == null));
  }

  /**
   * Annotated-only selection must exclude unknown rows before constructing output records.
   *
   * @throws IOException if the exported file cannot be inspected
   */
  @Test
  void filtersUnidentifiedRowsWhenIncludeAllIsDisabled() throws IOException {
    final Path output = tempDir.resolve("identified-only.mzTab");
    final MZTabmExportTask task = createTask(output, false, createMixedFeatureList());

    task.run();

    Assertions.assertEquals(TaskStatus.FINISHED, task.getStatus(), task.getErrorMessage());
    final MzTab exported = parseExport(output);
    Assertions.assertEquals(1, exported.getSmallMoleculeSummary().size());
    Assertions.assertEquals(1, exported.getSmallMoleculeFeature().size());
    Assertions.assertEquals(1, exported.getSmallMoleculeEvidence().size());
    Assertions.assertEquals(List.of("Caffeine"),
        exported.getSmallMoleculeSummary().getFirst().getChemicalName());
    Assertions.assertEquals(1d, task.getFinishedPercentage());
  }

  /**
   * An unidentified-only list uses the feature-only profile without claiming compound evidence.
   *
   * @throws IOException if the exported file cannot be inspected
   */
  @Test
  void exportsUnidentifiedOnlyFeatureLists() throws IOException {
    final ModularFeatureList featureList = createFeatureList("Unknown features", "sample.mzML", null);

    final MzTab exported = runExport(tempDir.resolve("unknown.mzTab"), featureList);

    Assertions.assertEquals(MzTabProfile.M_F, exported.getMetadata().getMzTabProfile());
    Assertions.assertEquals(1, exported.getSmallMoleculeFeature().size());
    Assertions.assertEquals(List.of(1250d), exported.getSmallMoleculeFeature().getFirst().getAbundanceAssay());
    Assertions.assertTrue(exported.getSmallMoleculeSummary().isEmpty());
    Assertions.assertTrue(exported.getSmallMoleculeEvidence().isEmpty());
    Assertions.assertTrue(exported.getMetadata().getDatabase().isEmpty());
  }

  /**
   * Missing per-sample charges must not discard the charge retained by the aligned row.
   *
   * @throws IOException if the exported file cannot be inspected
   */
  @Test
  void preservesRowChargeWhenSampleChargeIsMissing() throws IOException {
    final ModularFeatureList featureList = createFeatureList("Features", "sample.mzML",
        createIdentity("https://example.org/compound/1"));
    final FeatureListRow row = featureList.getRows().getFirst();
    Mockito.when(row.getRowCharge()).thenReturn(1);
    Mockito.when(row.getFeature(featureList.getRawDataFiles().getFirst()).getCharge()).thenReturn(0);

    final MzTab exported = runExport(tempDir.resolve("charge.mzTab"), featureList);

    Assertions.assertEquals(1, exported.getSmallMoleculeFeature().getFirst().getCharge());
    Assertions.assertEquals(1, exported.getSmallMoleculeEvidence().getFirst().getCharge());
  }

  /**
   * Modern compound annotations take precedence and retain their actual precursor and score.
   *
   * @throws IOException if the exported file cannot be inspected
   */
  @Test
  void exportsPreferredModernCompoundAnnotation() throws IOException {
    final ModularFeatureList featureList = createFeatureList("Features", "sample.mzML",
        createIdentity("https://example.org/legacy/1"));
    final SimpleCompoundDBAnnotation annotation = Mockito.mock(SimpleCompoundDBAnnotation.class);
    Mockito.when(annotation.getCompoundName()).thenReturn("Modern caffeine");
    Mockito.when(annotation.getFormula()).thenReturn("C8H10N4O2");
    Mockito.when(annotation.getDatabase()).thenReturn("Local compound database");
    Mockito.when(annotation.getAnnotationMethodName()).thenReturn("Compound database matching");
    Mockito.when(annotation.getPrecursorMZ()).thenReturn(195.0878);
    Mockito.when(annotation.getScore()).thenReturn(0.87f);
    Mockito.when(featureList.getRows().getFirst().getPreferredAnnotation()).thenReturn(annotation);

    final MzTab exported = runExport(tempDir.resolve("modern.mzTab"), featureList);

    final SmallMoleculeEvidence evidence = exported.getSmallMoleculeEvidence().getFirst();
    Assertions.assertEquals("Modern caffeine", evidence.getChemicalName());
    Assertions.assertEquals(195.0878, evidence.getTheoreticalMassToCharge(), 1e-8);
    Assertions.assertEquals(0.87, evidence.getIdConfidenceMeasure().getFirst(), 1e-6);
    Assertions.assertEquals("Compound database matching", evidence.getIdentificationMethod().getName());
    Assertions.assertEquals("SimpleCompoundDBAnnotation_1:null", evidence.getDatabaseIdentifier());
    Assertions.assertNull(evidence.getUri());
    Assertions.assertEquals("mzmine", exported.getMetadata().getDatabase().getFirst().getUri().getScheme());
    Assertions.assertEquals("annotated", exported.getSmallMoleculeSummary().getFirst().getReliability());
  }

  /**
   * Merged spectral evidence references measured source scans by file position, not native number.
   *
   * @throws IOException if the exported file cannot be inspected
   */
  @Test
  void exportsMergedSpectralEvidenceUsingMeasuredScanIndices() throws IOException {
    final ModularFeatureList featureList = createFeatureList("Features", "sample.mzML", null);
    final RawDataFile rawFile = featureList.getRawDataFiles().getFirst();
    final Scan firstScan = rawFile.getScans().getFirst();
    final Scan source = Mockito.mock(Scan.class);
    Mockito.when(source.getDataFile()).thenReturn(rawFile);
    Mockito.when(source.getScanNumber()).thenReturn(101);
    Mockito.when(source.getMSLevel()).thenReturn(2);
    Mockito.when(rawFile.getScans()).thenReturn(FXCollections.observableArrayList(firstScan, source));
    final MergedMassSpectrum nested = Mockito.mock(MergedMassSpectrum.class);
    Mockito.when(nested.getSourceSpectra()).thenReturn(List.of(source));
    final MergedMassSpectrum query = Mockito.mock(MergedMassSpectrum.class);
    Mockito.when(query.getSourceSpectra()).thenReturn(List.of(nested, nested));
    Mockito.when(query.getMSLevel()).thenReturn(2);
    final SpectralLibraryEntry entry = Mockito.mock(SpectralLibraryEntry.class);
    Mockito.when(entry.getField(DBEntryField.ENTRY_ID)).thenReturn(Optional.of("LIB:42"));
    final SpectralDBAnnotation annotation = Mockito.mock(SpectralDBAnnotation.class);
    Mockito.when(annotation.getEntry()).thenReturn(entry);
    Mockito.when(annotation.getSimilarity()).thenReturn(new SpectralSimilarity("Cosine", 0.91, 5, 0.8));
    Mockito.when(annotation.getQueryScan()).thenReturn(query);
    Mockito.when(annotation.getCompoundName()).thenReturn("Caffeine");
    Mockito.when(annotation.getFormula()).thenReturn("C8H10N4O2");
    Mockito.when(annotation.getPrecursorMZ()).thenReturn(195.0878);
    Mockito.when(annotation.getAnnotationMethodName()).thenReturn("Spectral library matching");
    Mockito.when(featureList.getRows().getFirst().getPreferredAnnotation()).thenReturn(annotation);

    final MzTab exported = runExport(tempDir.resolve("spectral.mzTab"), featureList);

    final SmallMoleculeEvidence evidence = exported.getSmallMoleculeEvidence().getFirst();
    Assertions.assertEquals("SpectralDBAnnotation_1:LIB:42", evidence.getDatabaseIdentifier());
    Assertions.assertEquals(0.91, evidence.getIdConfidenceMeasure().getFirst(), 1e-12);
    Assertions.assertEquals("2", evidence.getMsLevel().getValue());
    Assertions.assertEquals(1, evidence.getSpectraRef().size());
    Assertions.assertEquals("index=1", evidence.getSpectraRef().getFirst().getReference());
    Assertions.assertEquals("Cosine", evidence.getOpt().getFirst().getValue());
  }

  /**
   * Raw data references keep only the filename and preserve characters requiring URI escaping.
   *
   * @param rawFileName raw filename to round-trip through the real writer and parser
   * @throws IOException if the exported file cannot be inspected
   */
  @ParameterizedTest
  @ValueSource(strings = {"sample.mzML", "sample one.mzML", "sample#1.mzML", "sample%1.mzML",
      "sample?1.mzML", "[sample].mzML", "試料.mzML"})
  void preservesFilenameOnlyRawDataReferences(@NotNull final String rawFileName) throws IOException {
    final ModularFeatureList featureList = createFeatureList("Features", rawFileName,
        createIdentity("https://example.org/compound/1"));

    final MzTab exported = runExport(tempDir.resolve("filename.mzTab"), featureList);

    final URI location = exported.getMetadata().getMsRun().getFirst().getLocation();
    Assertions.assertEquals("file", location.getScheme());
    Assertions.assertEquals("//" + rawFileName, location.getSchemeSpecificPart());
    Assertions.assertNull(location.getQuery());
    Assertions.assertNull(location.getFragment());
  }

  /**
   * Missing annotation URLs remain valid missing URI values after the jmzTab-m API migration.
   *
   * @param url null or empty annotation URL
   * @throws IOException if the exported file cannot be inspected
   */
  @ParameterizedTest
  @NullAndEmptySource
  void exportsAnnotationsWithoutUrls(@Nullable final String url) throws IOException {
    final ModularFeatureList featureList = createFeatureList("Features", "sample.mzML",
        createIdentity(url));

    final MzTab exported = runExport(tempDir.resolve("missing-url.mzTab"), featureList);

    Assertions.assertNull(exported.getSmallMoleculeEvidence().getFirst().getUri());
    Assertions.assertTrue(
        exported.getSmallMoleculeSummary().getFirst().getUri().stream().allMatch(uri -> uri == null));
    Assertions.assertEquals(URI.create("mzmine://SimpleFeatureIdentity"),
        exported.getMetadata().getDatabase().getFirst().getUri());
  }

  /**
   * Each selected list receives its own output when a filename pattern is supplied.
   *
   * @throws IOException if an exported file cannot be inspected
   */
  @Test
  void substitutesAndSanitizesFeatureListNamesForMultipleExports() throws IOException {
    final FeatureIdentity identity = createIdentity("https://example.org/compound/1");
    final ModularFeatureList first = createFeatureList("Sample A", "first.mzML", identity);
    final ModularFeatureList second = createFeatureList("Sample/B", "second.mzML", identity);
    final Path pattern = tempDir.resolve("export_{}.mzTab");
    final MZTabmExportTask task = createTask(pattern, true, first, second);

    task.run();

    Assertions.assertEquals(TaskStatus.FINISHED, task.getStatus(), task.getErrorMessage());
    Assertions.assertEquals("Sample A",
        parseExport(tempDir.resolve("export_Sample_A.mzTab")).getMetadata().getDescription());
    Assertions.assertEquals("Sample/B",
        parseExport(tempDir.resolve("export_Sample_B.mzTab")).getMetadata().getDescription());
    Assertions.assertFalse(Files.exists(pattern));
  }

  /**
   * A writer validation failure must be reported, even if an older output file already exists.
   *
   * @param existingOutput whether an earlier export is already present
   * @throws IOException if the existing output cannot be created or inspected
   */
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void reportsWriterValidationFailure(final boolean existingOutput) throws IOException {
    final ModularFeatureList featureList = createFeatureList("Features", "sample.mzML",
        createIdentity("https://example.org/compound/1"));
    Mockito.when(featureList.getRows().getFirst().getAverageMZ()).thenReturn(null);
    final Path output = tempDir.resolve("invalid.mzTab");
    if (existingOutput) {
      Files.writeString(output, "previous export");
    }
    final MZTabmExportTask task = createTask(output, true, featureList);

    task.run();

    Assertions.assertEquals(TaskStatus.ERROR, task.getStatus());
    Assertions.assertNotNull(task.getErrorMessage());
    Assertions.assertTrue(task.getErrorMessage().contains("exp_mass_to_charge"),
        task.getErrorMessage());
    Assertions.assertTrue(task.getErrorMessage().contains(output.toString()), task.getErrorMessage());
    if (existingOutput) {
      Assertions.assertEquals("previous export", Files.readString(output));
    } else {
      Assertions.assertFalse(Files.exists(output));
    }
  }

  /**
   * Unsupported raw data polarities fail before attempting to write an output file.
   *
   * @param polarity raw data polarity without a positive or negative sign
   */
  @ParameterizedTest
  @EnumSource(value = PolarityType.class, names = {"NEUTRAL", "ANY", "UNKNOWN"})
  void rejectsUndefinedScanPolarities(@NotNull final PolarityType polarity) {
    final ModularFeatureList featureList = createFeatureList("Features", "sample.mzML",
        createIdentity("https://example.org/compound/1"));
    Mockito.when(featureList.getRawDataFiles().getFirst().getDataPolarity())
        .thenReturn(List.of(polarity));
    final Path output = tempDir.resolve("polarity.mzTab");
    final MZTabmExportTask task = createTask(output, true, featureList);

    task.run();

    Assertions.assertEquals(TaskStatus.ERROR, task.getStatus());
    Assertions.assertEquals("Invalid scan polarity 0 encountered for file sample.mzML.",
        task.getErrorMessage());
    Assertions.assertFalse(Files.exists(output));
  }

  /**
   * Filesystem failures must identify the destination and leave the task in its error state.
   *
   * @throws IOException if the directory used to block the output cannot be created
   */
  @Test
  void reportsOutputWriteFailure() throws IOException {
    final ModularFeatureList featureList = createFeatureList("Features", "sample.mzML",
        createIdentity("https://example.org/compound/1"));
    final Path output = Files.createDirectory(tempDir.resolve("blocked.mzTab"));
    final MZTabmExportTask task = createTask(output, true, featureList);

    task.run();

    Assertions.assertEquals(TaskStatus.ERROR, task.getStatus());
    Assertions.assertNotNull(task.getErrorMessage());
    Assertions.assertTrue(task.getErrorMessage().contains(output.toString()), task.getErrorMessage());
    Assertions.assertTrue(Files.isDirectory(output));
  }

  /**
   * Runs the task without replacing the production writer and validates the resulting file.
   *
   * @param output destination file
   * @param featureLists selected feature lists
   * @return the parsed, validated export
   * @throws IOException if the exported file cannot be inspected
   */
  private static @NotNull MzTab runExport(@NotNull final Path output,
      @NotNull final ModularFeatureList... featureLists) throws IOException {
    final MZTabmExportTask task = createTask(output, true, featureLists);
    task.run();
    Assertions.assertEquals(TaskStatus.FINISHED, task.getStatus(), task.getErrorMessage());
    return parseExport(output);
  }

  /**
   * Rejects missing, empty or invalid files rather than relying only on the task's status.
   *
   * @param output exported file
   * @return parsed mzTab-M data with no validation messages
   * @throws IOException if the exported file cannot be inspected
   */
  private static @NotNull MzTab parseExport(@NotNull final Path output) throws IOException {
    Assertions.assertTrue(Files.isRegularFile(output), "A finished export must create its file");
    Assertions.assertTrue(Files.size(output) > 0, "The exported file must contain data");
    final MzTabFileParser parser = new MzTabFileParser(output.toFile());
    final ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
    parser.parse(diagnostics, MZTabErrorType.Level.Info, 500);
    Assertions.assertTrue(parser.getErrorList().isEmpty(),
        diagnostics.toString(StandardCharsets.UTF_8));
    final MzTab exported = parser.getMZTabFile();
    Assertions.assertNotNull(exported, "The export must be readable as an mzTab-M model");
    return exported;
  }

  /**
   * Creates a complete legacy annotation consumed by this exporter.
   *
   * @param url optional compound URL
   * @return annotation with formula, identifier and database method
   */
  private static @NotNull FeatureIdentity createIdentity(@Nullable final String url) {
    return new SimpleFeatureIdentity("Caffeine", "C8H10N4O2", "Test database", "TEST:1", url);
  }

  /**
   * Combines measured rows with and without an annotation, each absent from the other assay.
   *
   * @return two-row, two-assay fixture with genuine missing values
   */
  private static @NotNull ModularFeatureList createMixedFeatureList() {
    final ModularFeatureList identified = createFeatureList("Mixed", "first.mzML",
        createIdentity("https://example.org/compound/1"));
    final ModularFeatureList unknown = createFeatureList("Unknown", "second.mzML", null);
    final FeatureListRow firstRow = identified.getRows().getFirst();
    final FeatureListRow secondRow = unknown.getRows().getFirst();
    final RawDataFile firstFile = identified.getRawDataFiles().getFirst();
    final RawDataFile secondFile = unknown.getRawDataFiles().getFirst();
    Mockito.when(secondRow.getID()).thenReturn(2);
    Mockito.when(identified.getRawDataFiles()).thenReturn(List.of(firstFile, secondFile));
    Mockito.when(identified.getNumberOfRows()).thenReturn(2);
    Mockito.when(identified.getRows()).thenReturn(FXCollections.observableArrayList(firstRow, secondRow));
    return identified;
  }

  /**
   * Builds an isolated parameter set without changing the module's shared parameter defaults.
   *
   * @param output output file or filename pattern
   * @param includeUnknown whether unidentified features are included
   * @param featureLists selected feature lists
   * @return task ready to run synchronously
   */
  private static @NotNull MZTabmExportTask createTask(@NotNull final Path output,
      final boolean includeUnknown, @NotNull final ModularFeatureList... featureLists) {
    final ParameterSet parameters = new MZTabmExportParameters().cloneParameterSet();
    parameters.setParameter(MZTabmExportParameters.featureLists,
        new FeatureListsSelection(featureLists));
    parameters.setParameter(MZTabmExportParameters.filename, output.toFile());
    parameters.setParameter(MZTabmExportParameters.exportAll, includeUnknown);
    return new MZTabmExportTask(new MZmineProjectImpl(), parameters, Instant.EPOCH);
  }

  /**
   * Supplies a single measured feature while keeping the exporter and writer real.
   *
   * @param name feature list name
   * @param rawFileName raw data filename
   * @param identity optional feature annotation
   * @return feature list containing one measured row
   */
  private static @NotNull ModularFeatureList createFeatureList(@NotNull final String name,
      @NotNull final String rawFileName, @Nullable final FeatureIdentity identity) {
    final RawDataFile rawFile = Mockito.mock(RawDataFile.class);
    Mockito.when(rawFile.getName()).thenReturn(rawFileName);
    Mockito.when(rawFile.getDataPolarity()).thenReturn(List.of(PolarityType.POSITIVE));

    final Scan scan = Mockito.mock(Scan.class);
    Mockito.when(scan.getScanNumber()).thenReturn(7);
    Mockito.when(scan.getDataFile()).thenReturn(rawFile);
    Mockito.when(scan.getMSLevel()).thenReturn(1);
    Mockito.when(rawFile.getScans()).thenReturn(FXCollections.observableArrayList(scan));
    final Feature feature = Mockito.mock(Feature.class);
    Mockito.when(feature.getRawDataFile()).thenReturn(rawFile);
    Mockito.when(feature.getScanNumbers()).thenReturn(List.of(scan));
    Mockito.when(feature.getMZ()).thenReturn(195.08765);
    Mockito.when(feature.getRT()).thenReturn(1.5f);
    Mockito.when(feature.getHeight()).thenReturn(250f);
    Mockito.when(feature.getArea()).thenReturn(1250f);
    Mockito.when(feature.getCharge()).thenReturn(1);

    final FeatureListRow row = Mockito.mock(FeatureListRow.class);
    Mockito.when(row.getID()).thenReturn(1);
    Mockito.when(row.getRawDataFiles()).thenReturn(List.of(rawFile));
    Mockito.when(row.getPreferredFeatureIdentity()).thenReturn(identity);
    Mockito.when(row.getAverageMZ()).thenReturn(195.08765);
    Mockito.when(row.getAverageRT()).thenReturn(1.5f);
    Mockito.when(row.getFeature(rawFile)).thenReturn(feature);

    final ModularFeatureList featureList = Mockito.mock(ModularFeatureList.class);
    Mockito.when(featureList.getName()).thenReturn(name);
    Mockito.when(featureList.getRawDataFiles()).thenReturn(List.of(rawFile));
    Mockito.when(featureList.getNumberOfRows()).thenReturn(1);
    Mockito.when(featureList.getRows()).thenReturn(FXCollections.observableArrayList(row));
    return featureList;
  }
}
