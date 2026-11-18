package io.github.mzmine.modules.io.export_features_mztabm;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MergedMassSpectrum;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.projectload.ProjectLoaderParameters;
import io.github.mzmine.modules.io.projectload.ProjectOpeningTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.lifstools.mztab2.io.MzTabFileParser;
import org.lifstools.mztab2.model.Assay;
import org.lifstools.mztab2.model.MzTab;
import org.lifstools.mztab2.model.SmallMoleculeEvidence;
import org.lifstools.mztab2.model.SmallMoleculeFeature;
import org.lifstools.mztab2.model.SmallMoleculeSummary;
import org.lifstools.mztab2.model.SpectraRef;
import org.lifstools.mztab2.model.StudyVariable;
import uk.ac.ebi.pride.jmztab2.utils.errors.MZTabErrorType;

/**
 * Exports the bundled measured LC-MS workshop project through the real project loader.
 * Requires a normally configured signed-in mzmine user; run with
 * {@code -Dmzmine.test.workshopExport=true} to enable this account-dependent integration test.
 */
@EnabledIfSystemProperty(named = "mzmine.test.workshopExport", matches = "true")
class MZTabmWorkshopExportTest {

  private static final String PROJECT_RESOURCE =
      "/rawdatafiles/integration_tests/workshop_dataset/project.mzmine";

  @TempDir
  Path tempDir;

  /**
   * Starts headless mzmine with its configured user for authorized project loading.
   */
  @BeforeAll
  static void initializeMzmine() {
    MZmineCore.main(new String[]{"-r", "-m", "all"});
  }

  /**
   * Releases the raw data files opened by the integration fixture.
   */
  @AfterEach
  void closeWorkshopProject() {
    final MZmineProject loadedProject = ProjectService.getProject();
    // decision: Changing the current project closes its raw files before discarding loaded rows.
    ProjectService.getProjectManager().setCurrentProject(new MZmineProjectImpl());
    loadedProject.clearFeatureLists();
    loadedProject.clearSpectralLibrary();
  }

  /**
   * Preserves measured rows, assay areas and modern annotations from the workshop.
   *
   * @throws URISyntaxException if the bundled project resource has an invalid location
   * @throws InterruptedException if project loading is interrupted
   * @throws IOException if the exported mzTab-M file cannot be read
   */
  @Test
  void exportsWorkshopProjectWithModernAnnotations()
      throws URISyntaxException, InterruptedException, IOException {
    final MZmineProject project = loadWorkshopProject();
    final ModularFeatureList featureList = finalWorkshopFeatureList(project);

    Assertions.assertEquals(2, project.getNumberOfDataFiles());
    Assertions.assertEquals(120, featureList.getNumberOfRows());
    Assertions.assertEquals(2, featureList.getNumberOfRawDataFiles());
    Assertions.assertEquals(8,
        featureList.getRows().stream().filter(row -> row.getPreferredAnnotation() != null).count());
    Assertions.assertTrue(featureList.getRows().stream()
        .allMatch(row -> row.getPreferredFeatureIdentity() == null));

    final ParameterSet parameters = new MZTabmExportParameters().cloneParameterSet();
    final Path output = tempDir.resolve("workshop.mzTab");
    parameters.setParameter(MZTabmExportParameters.featureLists,
        new FeatureListsSelection(featureList));
    parameters.setParameter(MZTabmExportParameters.filename, output.toFile());
    parameters.setParameter(MZTabmExportParameters.exportAll, true);
    final MZTabmExportTask exportTask = new MZTabmExportTask(project, parameters, Instant.EPOCH);

    exportTask.run();

    Assertions.assertEquals(TaskStatus.FINISHED, exportTask.getStatus(), exportTask.getErrorMessage());
    final MzTab exported = parseExport(output);
    Assertions.assertEquals(120, exported.getSmallMoleculeSummary().size());
    Assertions.assertEquals(120, exported.getSmallMoleculeFeature().size());
    Assertions.assertEquals(8, exported.getSmallMoleculeEvidence().size());
    Assertions.assertEquals(2, exported.getMetadata().getAssay().size());
    Assertions.assertEquals("MZmine annotation status",
        exported.getMetadata().getSmallMoleculeIdentificationReliability().getName());
    final List<RawDataFile> rawFiles = featureList.getRawDataFiles();
    final Map<Integer, RawDataFile> filesByRun = new HashMap<>();
    for (int fileIndex = 0; fileIndex < rawFiles.size(); fileIndex++) {
      final var run = exported.getMetadata().getMsRun().get(fileIndex);
      final RawDataFile rawFile = rawFiles.get(fileIndex);
      Assertions.assertEquals("//" + rawFile.getName(), run.getLocation().getSchemeSpecificPart());
      filesByRun.put(run.getId(), rawFile);
    }
    assertDefaultStudyVariable(featureList, exported);
    final Map<Integer, SmallMoleculeEvidence> evidenceById = exported.getSmallMoleculeEvidence()
        .stream().collect(Collectors.toMap(SmallMoleculeEvidence::getSmeId, Function.identity()));

    for (int rowIndex = 0; rowIndex < featureList.getNumberOfRows(); rowIndex++) {
      final FeatureListRow sourceRow = featureList.getRow(rowIndex);
      final SmallMoleculeFeature feature = exported.getSmallMoleculeFeature().get(rowIndex);
      final SmallMoleculeSummary summary = exported.getSmallMoleculeSummary().get(rowIndex);
      Assertions.assertEquals(sourceRow.getAverageMZ(), feature.getExpMassToCharge(), 1e-8);
      Assertions.assertEquals(sourceRow.getAverageRT().doubleValue() * 60,
          feature.getRetentionTimeInSeconds(), 1e-4);
      Assertions.assertEquals(List.of(feature.getSmfId()), summary.getSmfIdRefs());
      Assertions.assertEquals(rawFiles.size(), feature.getAbundanceAssay().size());
      Assertions.assertEquals(rawFiles.size(), summary.getAbundanceAssay().size());
      for (int assayIndex = 0; assayIndex < rawFiles.size(); assayIndex++) {
        final Feature sourceFeature = sourceRow.getFeature(rawFiles.get(assayIndex));
        final Float sourceArea = sourceFeature == null ? null : sourceFeature.getArea();
        final Double expectedArea = sourceArea == null ? null : sourceArea.doubleValue();
        Assertions.assertEquals(expectedArea, feature.getAbundanceAssay().get(assayIndex));
        Assertions.assertEquals(expectedArea, summary.getAbundanceAssay().get(assayIndex));
      }

      final FeatureAnnotation annotation = sourceRow.getPreferredAnnotation();
      Assertions.assertEquals(annotation == null ? "unannotated" : "annotated",
          summary.getReliability());
      if (annotation == null) {
        Assertions.assertTrue(feature.getSmeIdRefs().stream().allMatch(id -> id == null));
        Assertions.assertTrue(summary.getChemicalName().stream()
            .allMatch(MZTabmWorkshopExportTest::isMissingMzTabText));
      } else {
        Assertions.assertEquals(1, feature.getSmeIdRefs().size());
        final SmallMoleculeEvidence evidence = evidenceById.get(feature.getSmeIdRefs().getFirst());
        Assertions.assertNotNull(evidence);
        Assertions.assertEquals(annotation.getCompoundName(), evidence.getChemicalName());
        Assertions.assertEquals(List.of(annotation.getCompoundName()), summary.getChemicalName());
        // assumption: This workshop ion formula is the protonated form of C21H42N2O3.
        final String formula = "[C21H43N2O3]+".equals(annotation.getFormula()) ? "C21H42N2O3"
            : annotation.getFormula();
        if (formula == null || formula.isBlank()) {
          Assertions.assertTrue(isMissingMzTabText(evidence.getChemicalFormula()));
          Assertions.assertTrue(summary.getChemicalFormula().stream()
              .allMatch(MZTabmWorkshopExportTest::isMissingMzTabText));
        } else {
          Assertions.assertEquals(formula, evidence.getChemicalFormula());
          Assertions.assertEquals(List.of(formula), summary.getChemicalFormula());
        }

        final Double expectedScore;
        if (annotation instanceof SpectralDBAnnotation spectral) {
          expectedScore = spectral.getSimilarity().getScore();
        } else {
          final Float score = annotation.getScore();
          expectedScore = score == null ? null : score.doubleValue();
        }
        final String scoreAccession = annotation instanceof SpectralDBAnnotation ? "MS:1003303"
            : "MS:1001153";
        final var scoreMeasure = exported.getMetadata().getIdConfidenceMeasure().stream()
            .filter(measure -> scoreAccession.equals(measure.getCvAccession())).findFirst()
            .orElseThrow();
        final int scoreIndex = exported.getMetadata().getIdConfidenceMeasure().indexOf(scoreMeasure);
        Assertions.assertEquals(exported.getMetadata().getIdConfidenceMeasure().size(),
            evidence.getIdConfidenceMeasure().size());
        if (expectedScore == null) {
          Assertions.assertNull(evidence.getIdConfidenceMeasure().get(scoreIndex));
        } else {
          Assertions.assertEquals(expectedScore, evidence.getIdConfidenceMeasure().get(scoreIndex),
              1e-12);
          Assertions.assertEquals(scoreAccession,
              summary.getBestIdConfidenceMeasure().getCvAccession());
          Assertions.assertEquals(expectedScore, summary.getBestIdConfidenceValue(), 1e-12);
        }
        for (int confidenceIndex = 0;
            confidenceIndex < evidence.getIdConfidenceMeasure().size(); confidenceIndex++) {
          if (confidenceIndex != scoreIndex) {
            Assertions.assertNull(evidence.getIdConfidenceMeasure().get(confidenceIndex));
          }
        }
        if (annotation instanceof SpectralDBAnnotation spectral) {
          Assertions.assertEquals(spectral.getSimilarity().getFunctionName(), evidence.getOpt()
              .stream().filter(column ->
                  "global_annotation_score_name".equals(column.getIdentifier()))
              .findFirst().orElseThrow().getValue());
        }
        if (annotation.getPrecursorMZ() == null) {
          Assertions.assertNull(evidence.getTheoreticalMassToCharge());
        } else {
          Assertions.assertEquals(annotation.getPrecursorMZ(), evidence.getTheoreticalMassToCharge(),
              1e-8);
        }

        final List<Scan> measuredScans = referencedScans(evidence, filesByRun);
        if (annotation instanceof SpectralDBAnnotation spectral && spectral.getQueryScan() != null) {
          final Scan query = spectral.getQueryScan();
          final Set<Scan> expectedScans = query instanceof MergedMassSpectrum merged
              ? merged.getSourceSpectra().stream().map(Scan.class::cast).collect(Collectors.toSet())
              : Set.of(query);
          Assertions.assertEquals(expectedScans, Set.copyOf(measuredScans));
        }
      }
    }
  }

  /**
   * Checks the single default study variable against both assays' measured values.
   *
   * @param featureList measured workshop feature list
   * @param exported parsed export containing the default study variable
   */
  private static void assertDefaultStudyVariable(@NotNull final ModularFeatureList featureList,
      @NotNull final MzTab exported) {
    final List<RawDataFile> rawFiles = featureList.getRawDataFiles();
    Assertions.assertEquals(2, rawFiles.size());
    Assertions.assertEquals(120, exported.getSmallMoleculeSummary().size());
    Assertions.assertEquals(120, exported.getSmallMoleculeFeature().size());
    Assertions.assertEquals(8, exported.getSmallMoleculeEvidence().size());
    Assertions.assertEquals(2, exported.getMetadata().getAssay().size());
    Assertions.assertEquals(1, exported.getMetadata().getStudyVariable().size());
    final StudyVariable group = exported.getMetadata().getStudyVariable().getFirst();
    Assertions.assertEquals("undefined", group.getName());
    Assertions.assertTrue(group.getFactors().isEmpty());
    Assertions.assertEquals("MS:1002962", group.getAverageFunction().getCvAccession());
    Assertions.assertEquals("MS:1002963", group.getVariationFunction().getCvAccession());
    Assertions.assertEquals(2, group.getAssayRefs().size());
    Assertions.assertEquals(exported.getMetadata().getAssay().stream().map(Assay::getId)
            .collect(Collectors.toSet()),
        group.getAssayRefs().stream().map(Assay::getId).collect(Collectors.toSet()));

    for (int rowIndex = 0; rowIndex < featureList.getNumberOfRows(); rowIndex++) {
      final FeatureListRow sourceRow = featureList.getRow(rowIndex);
      final Double firstArea = measuredArea(sourceRow, rawFiles.get(0));
      final Double secondArea = measuredArea(sourceRow, rawFiles.get(1));
      final Double expectedMean;
      final Double expectedVariation;
      if (firstArea == null) {
        expectedMean = secondArea;
        expectedVariation = secondArea == null ? null : Double.valueOf(0d);
      } else if (secondArea == null) {
        expectedMean = firstArea;
        expectedVariation = 0d;
      } else {
        expectedMean = (firstArea + secondArea) / 2d;
        // decision: For two measurements the sample CV has this independent closed form.
        expectedVariation = expectedMean == 0d ? 0d
            : Math.abs(firstArea - secondArea) / (Math.sqrt(2d) * expectedMean);
      }
      final SmallMoleculeSummary summary = exported.getSmallMoleculeSummary().get(rowIndex);
      Assertions.assertEquals(1, summary.getAbundanceStudyVariable().size());
      Assertions.assertEquals(1, summary.getAbundanceVariationStudyVariable().size());
      Assertions.assertEquals(expectedMean, summary.getAbundanceStudyVariable().getFirst());
      if (expectedVariation == null) {
        Assertions.assertNull(summary.getAbundanceVariationStudyVariable().getFirst());
      } else {
        Assertions.assertEquals(expectedVariation,
            summary.getAbundanceVariationStudyVariable().getFirst(), 1e-12);
      }
    }
  }

  /**
   * Reads an assay's measured area while preserving an absent measurement as missing.
   *
   * @param row measured workshop feature row
   * @param rawFile measured assay file
   * @return area, or null when unavailable
   */
  private static @Nullable Double measuredArea(@NotNull final FeatureListRow row,
      @NotNull final RawDataFile rawFile) {
    final Feature feature = row.getFeature(rawFile);
    final Float area = feature == null ? null : feature.getArea();
    return area == null ? null : area.doubleValue();
  }

  /**
   * Resolves every exported spectrum reference to an existing measured scan in its original file.
   *
   * @param evidence exported annotation evidence
   * @param filesByRun original measured files indexed by exported run identifier
   * @return referenced measured scans
   */
  private static @NotNull List<Scan> referencedScans(@NotNull final SmallMoleculeEvidence evidence,
      @NotNull final Map<Integer, RawDataFile> filesByRun) {
    final List<Scan> measuredScans = new ArrayList<>();
    for (final SpectraRef reference : evidence.getSpectraRef()) {
      final RawDataFile rawFile = filesByRun.get(reference.getMsRun().getId());
      Assertions.assertNotNull(rawFile, "Evidence must reference a measured raw file");
      Assertions.assertTrue(reference.getReference().startsWith("index="));
      final int index = Integer.parseInt(reference.getReference().substring("index=".length()));
      Assertions.assertTrue(index >= 0 && index < rawFile.getScans().size(),
          "Spectrum index must refer to an existing measured scan");
      final Scan measuredScan = rawFile.getScans().get(index);
      Assertions.assertSame(rawFile, measuredScan.getDataFile());
      measuredScans.add(measuredScan);
    }
    Assertions.assertFalse(measuredScans.isEmpty(), "Evidence must reference measured spectra");
    return measuredScans;
  }

  /**
   * Recognizes the parser's nullable text representation for an absent mzTab cell.
   *
   * @param value parsed optional text
   * @return whether the cell contains no value
   */
  private static boolean isMissingMzTabText(@Nullable final String value) {
    return value == null || value.equals("null");
  }

  /**
   * Parses the retained export at the most verbose validation level.
   *
   * @param output exported workshop file
   * @return validated mzTab-M contents
   * @throws IOException if the export is missing, empty or unreadable
   */
  private static @NotNull MzTab parseExport(@NotNull final Path output) throws IOException {
    Assertions.assertTrue(Files.isRegularFile(output), "The workshop export must create a file");
    Assertions.assertTrue(Files.size(output) > 0, "The workshop export must contain data");
    final MzTabFileParser parser = new MzTabFileParser(output.toFile());
    final ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
    parser.parse(diagnostics, MZTabErrorType.Level.Info, 500);
    Assertions.assertTrue(parser.getErrorList().isEmpty(),
        diagnostics.toString(StandardCharsets.UTF_8));
    final MzTab exported = parser.getMZTabFile();
    Assertions.assertNotNull(exported);
    return exported;
  }

  /**
   * Loads the embedded raw data and feature lists without applying the project's application settings.
   *
   * @return the project loaded by the production project-opening task
   * @throws URISyntaxException if the bundled project resource has an invalid location
   * @throws InterruptedException if project loading is interrupted
   */
  private static @NotNull MZmineProject loadWorkshopProject()
      throws URISyntaxException, InterruptedException {
    final URL resource = MZTabmWorkshopExportTest.class.getResource(PROJECT_RESOURCE);
    Assertions.assertNotNull(resource, "The bundled workshop project must be available");
    ProjectService.getProjectManager().setCurrentProject(new MZmineProjectImpl());
    final ParameterSet parameters = new ProjectLoaderParameters().cloneParameterSet();
    parameters.setParameter(ProjectLoaderParameters.projectFile, Path.of(resource.toURI()).toFile());
    parameters.setParameter(ProjectLoaderParameters.loadProjectConfiguration, false);
    final ProjectOpeningTask task = new ProjectOpeningTask(parameters, Instant.EPOCH);
    final Thread loader = Thread.ofPlatform().daemon().name("workshop-project-load").start(task);
    loader.join(TimeUnit.SECONDS.toMillis(45));
    if (loader.isAlive()) {
      task.cancel();
      loader.join(TimeUnit.SECONDS.toMillis(5));
      Assertions.fail("Workshop project loading did not complete within 45 seconds: "
          + task.getErrorMessage());
    }
    Assertions.assertEquals(TaskStatus.FINISHED, task.getStatus(), task.getErrorMessage());
    return ProjectService.getProject();
  }

  /**
   * Selects the final annotated and normalized list retained by the workshop project.
   *
   * @param project loaded workshop project
   * @return final processing result
   */
  private static @NotNull ModularFeatureList finalWorkshopFeatureList(
      @NotNull final MZmineProject project) {
    return project.getCurrentFeatureLists().stream().map(ModularFeatureList.class::cast)
        .max(Comparator.comparingInt(featureList -> featureList.getName().length())).orElseThrow();
  }
}
