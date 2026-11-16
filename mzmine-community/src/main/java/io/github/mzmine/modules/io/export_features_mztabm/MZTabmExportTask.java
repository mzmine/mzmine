/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.io.export_features_mztabm;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MergedMassSpectrum;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.io.SemverVersionReader;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lifstools.mztab2.io.MzTabValidatingWriter;
import org.lifstools.mztab2.model.Assay;
import org.lifstools.mztab2.model.CV;
import org.lifstools.mztab2.model.Database;
import org.lifstools.mztab2.model.Metadata;
import org.lifstools.mztab2.model.MsRun;
import org.lifstools.mztab2.model.MzTab;
import org.lifstools.mztab2.model.MzTabProfile;
import org.lifstools.mztab2.model.OptColumnMapping;
import org.lifstools.mztab2.model.Parameter;
import org.lifstools.mztab2.model.SmallMoleculeEvidence;
import org.lifstools.mztab2.model.SmallMoleculeFeature;
import org.lifstools.mztab2.model.SmallMoleculeSummary;
import org.lifstools.mztab2.model.Software;
import org.lifstools.mztab2.model.SpectraRef;
import org.lifstools.mztab2.model.StudyVariable;
import org.lifstools.mztab2.model.ValidationMessage;
import uk.ac.ebi.pride.jmztab2.model.OptColumnMappingBuilder;

/**
 * Exports measured features and their selected modern or legacy annotations to mzTab-M.
 */
public class MZTabmExportTask extends AbstractTask {

  private int processedRows;
  private int totalRows;
  private final File fileName;
  private final FeatureList[] featureLists;
  private final boolean exportAll;
  private final MetadataTable projectMetadata;
  private final @Nullable MetadataColumn<?> studyVariableColumn;

  /**
   * Captures the selected feature lists and export settings.
   *
   * @param project project owning the selected feature lists
   * @param parameters export settings
   * @param moduleCallDate module invocation time
   * @param studyVariableColumn metadata column selected in the MS data files Group by selector,
   *                            or null when no metadata grouping is selected
   */
  MZTabmExportTask(@NotNull final MZmineProject project, @NotNull final ParameterSet parameters,
      @NotNull final Instant moduleCallDate,
      @Nullable final MetadataColumn<?> studyVariableColumn) {
    super(null, moduleCallDate);
    projectMetadata = project.getProjectMetadata();
    this.studyVariableColumn = studyVariableColumn;
    featureLists = parameters.getParameter(MZTabmExportParameters.featureLists).getValue()
        .getMatchingFeatureLists();
    fileName = parameters.getParameter(MZTabmExportParameters.filename).getValue();
    exportAll = parameters.getParameter(MZTabmExportParameters.exportAll).getValue();
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows == 0 ? 0 : (double) processedRows / totalRows;
  }

  @Override
  public @NotNull String getTaskDescription() {
    return "Exporting feature list(s) " + Arrays.toString(featureLists) + " to mzTab-M file(s)";
  }

  /**
   * Writes each selected list, reporting validation and filesystem failures through task status.
   */
  @Override
  public void run() {
    if (isCanceled()) {
      return;
    }
    setStatus(TaskStatus.PROCESSING);
    totalRows = Arrays.stream(featureLists).mapToInt(FeatureList::getNumberOfRows).sum();
    for (final FeatureList featureList : featureLists) {
      if (isCanceled()) {
        return;
      }
      final String cleanName = featureList.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
      final File output = new File(fileName.getPath().replace("{}", cleanName));
      for (final RawDataFile file : featureList.getRawDataFiles()) {
        for (final PolarityType polarity : file.getDataPolarity()) {
          if (polarity.getSign() == 0) {
            error("Invalid scan polarity 0 encountered for file " + file.getName() + ".", null);
            return;
          }
        }
      }
      try {
        exportFeatureList(featureList, output);
      } catch (Exception e) {
        error("Could not export feature list to file " + output + ": " + e.getMessage(), e);
        return;
      }
      if (isCanceled() || getStatus() == TaskStatus.ERROR) {
        return;
      }
    }
    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Builds annotation evidence only for identified rows and validates the complete output.
   *
   * @param featureList input measurements and annotations
   * @param output destination file
   * @throws IOException if writing or validation fails
   * @throws URISyntaxException if a source filename cannot be represented as a URI
   */
  private void exportFeatureList(@NotNull final FeatureList featureList, @NotNull final File output)
      throws IOException, URISyntaxException {
    final Map<FeatureListRow, @Nullable MzTabmAnnotation> rows = new LinkedHashMap<>();
    for (final FeatureListRow row : featureList.getRows()) {
      final MzTabmAnnotation annotation = MzTabmAnnotation.fromRow(row);
      if (exportAll || annotation != null) {
        rows.put(row, annotation);
      } else {
        processedRows++;
      }
    }
    final boolean hasAnnotations = rows.values().stream().anyMatch(annotation -> annotation != null);
    final Metadata metadata = createMetadata(featureList, hasAnnotations);
    final Map<String, Parameter> scoreMeasures = createScoreMeasures(rows, metadata);
    final Map<RawDataFile, Assay> assays = createAssays(featureList, metadata);
    final List<StudyVariable> studyVariables = MzTabmStudyVariables.create(projectMetadata, assays,
        studyVariableColumn);
    studyVariables.forEach(metadata::addStudyVariableItem);
    final Map<String, Database> databases = new LinkedHashMap<>();
    final MzTab mzTab = new MzTab().metadata(metadata);
    int rowId = 0;
    for (final Map.Entry<FeatureListRow, @Nullable MzTabmAnnotation> entry : rows.entrySet()) {
      if (isCanceled()) {
        return;
      }
      final FeatureListRow row = entry.getKey();
      final MzTabmAnnotation annotation = entry.getValue();
      final SmallMoleculeFeature feature = new SmallMoleculeFeature().smfId(++rowId)
          .expMassToCharge(row.getAverageMZ()).charge(resolveCharge(row));
      if (row.getAverageRT() != null) {
        feature.setRetentionTimeInSeconds(row.getAverageRT().doubleValue() * 60d);
      }
      if (annotation != null) {
        feature.setAdductIon(annotation.adduct());
      }
      mzTab.addSmallMoleculeFeatureItem(feature);

      // decision: Unannotated-only data use M+F; mixed files retain unknown summaries without evidence.
      final SmallMoleculeSummary summary = hasAnnotations ? new SmallMoleculeSummary().smlId(rowId)
          .addSmfIdRefsItem(rowId).reliability(annotation == null ? "unannotated" : "annotated") : null;
      addAbundances(row, feature, summary, assays, studyVariables);
      if (summary != null) {
        mzTab.addSmallMoleculeSummaryItem(summary);
      }
      if (annotation != null) {
        final boolean legacy = row.getPreferredAnnotation() == null;
        final Database database = registerDatabase(annotation, legacy, databases);
        final String identifier = database.getPrefix() + ":"
            + (annotation.databaseIdentifier() == null ? "null" : annotation.databaseIdentifier());
        addSummaryAnnotation(summary, annotation, identifier);
        final Parameter scoreMeasure = scoreMeasures.get(scoreAccession(annotation));
        final List<Double> scores = new ArrayList<>(Collections.nCopies(scoreMeasures.size(), null));
        scores.set(scoreMeasure.getId() - 1, annotation.score());
        if (annotation.score() != null) {
          summary.setBestIdConfidenceMeasure(scoreMeasure);
          summary.setBestIdConfidenceValue(annotation.score());
        }
        final SmallMoleculeEvidence evidence = new SmallMoleculeEvidence().smeId(rowId)
            .evidenceInputId(String.valueOf(row.getID())).databaseIdentifier(identifier)
            .chemicalName(annotation.compoundName()).chemicalFormula(annotation.formula())
            .smiles(annotation.smiles()).inchi(annotation.inchi()).uri(annotation.uri())
            .adductIon(annotation.adduct()).expMassToCharge(row.getAverageMZ())
            .charge(feature.getCharge()).identificationMethod(new Parameter().name(annotation.method()))
            .idConfidenceMeasure(scores);
        // decision: Retain the legacy fallback; modern annotations use their actual precursor m/z.
        evidence.setTheoreticalMassToCharge(annotation.precursorMz() == null && legacy
            ? row.getAverageMZ() : annotation.precursorMz());
        final List<Scan> scans = evidenceScans(row, annotation.queryScan());
        final int msLevel = annotation.queryScan() != null ? annotation.queryScan().getMSLevel()
            : scans.isEmpty() ? 1 : scans.getFirst().getMSLevel();
        evidence.setMsLevel(new Parameter().cvLabel("MS").cvAccession("MS:1000511")
            .name("ms level").value(String.valueOf(msLevel)));
        evidence.setSpectraRef(spectraReferences(scans, assays));
        evidence.addOptItem(new OptColumnMapping().identifier("opt_global_annotation_score_name")
            .value(annotation.scoreName()));
        evidence.addOptItem(new OptColumnMapping().identifier("opt_global_database_accession")
            .value(annotation.databaseIdentifier()));
        feature.addSmeIdRefsItem(rowId);
        mzTab.addSmallMoleculeEvidenceItem(evidence);
      }
      processedRows++;
    }
    for (final Database database : databases.values()) {
      metadata.addDatabaseItem(database);
    }
    final List<ValidationMessage> messages = new MzTabValidatingWriter().write(output.toPath(), mzTab)
        .orElse(List.of());
    // decision: The default writer suppresses output for any validation message, including info.
    if (!messages.isEmpty()) {
      throw new IOException("mzTab-M validation failed:\n" + messages);
    }
  }

  /**
   * Creates metadata appropriate to annotated or entirely unannotated measurements.
   *
   * @param featureList source list
   * @param hasAnnotations whether identification evidence will be exported
   * @return metadata with the declared annotation-status scheme
   */
  private static @NotNull Metadata createMetadata(@NotNull final FeatureList featureList,
      final boolean hasAnnotations) {
    final Metadata metadata = new Metadata().mzTabVersion("2.0.0-M").mzTabID("1")
        .description(featureList.getName())
        .mzTabProfile(hasAnnotations ? MzTabProfile.M_S_F_E : MzTabProfile.M_F);
    metadata.addSoftwareItem(new Software().id(1).parameter(new Parameter().cvLabel("MS")
        .cvAccession("MS:1002342").name("mzmine")
        .value(String.valueOf(SemverVersionReader.getMZmineVersion()))));
    metadata.setSmallMoleculeFeatureQuantificationUnit(new Parameter().cvLabel("PRIDE")
        .cvAccession("PRIDE:0000330").name("Arbitrary quantification unit"));
    metadata.setQuantificationMethod(new Parameter().cvLabel("MS").cvAccession("MS:1001834")
        .name("LC-MS label-free quantitation analysis"));
    metadata.addCvItem(new CV().id(1).label("MS").fullName("PSI-MS controlled vocabulary")
        .version("4.2.17").uri(URI.create("https://raw.githubusercontent.com/HUPO-PSI/psi-ms-CV/"
            + "4d2a2346f7dd20d2da5eadfe93c0a897efe47a6b/psi-ms.obo")));
    metadata.addCvItem(new CV().id(2).label("PRIDE").fullName("PRIDE controlled vocabulary")
        .version("releases/2026-11-11").uri(URI.create(
            "https://raw.githubusercontent.com/PRIDE-Utilities/pride-ontology/"
                + "44aa3a3f72f0bacc4ee22e913b3d318c5624253e/pride_cv.obo")));
    if (hasAnnotations) {
      metadata.setSmallMoleculeQuantificationUnit(new Parameter().cvLabel("PRIDE")
          .cvAccession("PRIDE:0000330").name("Arbitrary quantification unit"));
      metadata.setSmallMoleculeIdentificationReliability(
          new Parameter().name("MZmine annotation status"));
    }
    return metadata;
  }

  /**
   * Declares separate CV-defined columns for spectral similarity and other matching scores.
   *
   * @param rows selected row annotations
   * @param metadata destination score definitions
   * @return confidence measure by CV accession, in column order
   */
  private static @NotNull Map<String, Parameter> createScoreMeasures(
      @NotNull final Map<FeatureListRow, @Nullable MzTabmAnnotation> rows,
      @NotNull final Metadata metadata) {
    final Map<String, Parameter> measures = new LinkedHashMap<>();
    for (final MzTabmAnnotation annotation : rows.values()) {
      if (annotation != null) {
        final String accession = scoreAccession(annotation);
        if (!measures.containsKey(accession)) {
          final Parameter measure = new Parameter().id(measures.size() + 1).cvLabel("MS")
              .cvAccession(accession).name(annotation.spectralMatch()
                  ? "spectral similarity" : "search engine specific score");
          measures.put(accession, measure);
          metadata.addIdConfidenceMeasureItem(measure);
        }
      }
    }
    return measures;
  }

  /**
   * Selects a score category without assigning an unsupported algorithm-specific CV accession.
   *
   * @param annotation annotation supplying the original score
   * @return supported confidence-measure CV accession
   */
  private static @NotNull String scoreAccession(@NotNull final MzTabmAnnotation annotation) {
    return annotation.spectralMatch() ? "MS:1003303" : "MS:1001153";
  }

  /**
   * Preserves sample order and filename-only run locations.
   *
   * @param featureList source files
   * @param metadata metadata to populate
   * @return assay corresponding to each raw file
   * @throws URISyntaxException if a raw filename cannot be represented as a URI
   */
  private static @NotNull Map<RawDataFile, Assay> createAssays(
      @NotNull final FeatureList featureList, @NotNull final Metadata metadata)
      throws URISyntaxException {
    final Map<RawDataFile, Assay> assays = new LinkedHashMap<>();
    for (final RawDataFile file : featureList.getRawDataFiles()) {
      final int id = assays.size() + 1;
      final int dot = file.getName().lastIndexOf('.');
      final String format = dot < 0 ? "" : file.getName().substring(dot + 1);
      final Parameter fileFormat = format.equalsIgnoreCase("mzML")
          ? new Parameter().cvLabel("MS").cvAccession("MS:1000584").name("mzML format")
          : new Parameter().name(format + " file");
      final MsRun run = new MsRun().id(id).location(createRawDataFileLocation(file.getName()))
          .format(fileFormat)
          .idFormat(new Parameter().cvLabel("MS").cvAccession("MS:1000774")
              .name("multiple peak list nativeID format"));
      for (final PolarityType polarity : file.getDataPolarity()) {
        run.addScanPolarityItem(new Parameter().cvLabel("MS")
            .cvAccession(polarity.getSign() > 0 ? "MS:1000130" : "MS:1000129")
            .name(polarity.getSign() > 0 ? "positive scan" : "negative scan"));
      }
      metadata.addMsRunItem(run);
      final Assay assay = new Assay().id(id).addMsRunRefItem(run);
      metadata.addAssayItem(assay);
      assays.put(file, assay);
    }
    return assays;
  }

  /**
   * Preserves assay measurements and summarizes each metadata-defined study variable independently.
   *
   * @param row source measurements
   * @param feature mzTab feature row
   * @param summary optional summary row
   * @param assays ordered assays
   * @param studyVariables factor groups in metadata and abundance-column order
   */
  private static void addAbundances(@NotNull final FeatureListRow row,
      @NotNull final SmallMoleculeFeature feature, @Nullable final SmallMoleculeSummary summary,
      @NotNull final Map<RawDataFile, Assay> assays,
      @NotNull final List<StudyVariable> studyVariables) {
    final Map<Integer, @Nullable Double> areasByAssay = new LinkedHashMap<>();
    for (final Map.Entry<RawDataFile, Assay> entry : assays.entrySet()) {
      final Feature measurement = row.getFeature(entry.getKey());
      final Double area = measurement == null || measurement.getArea() == null ? null
          : measurement.getArea().doubleValue();
      feature.addAbundanceAssayItem(area);
      areasByAssay.put(entry.getValue().getId(), area);
      if (summary != null) {
        summary.addAbundanceAssayItem(area);
        summary.addOptItem(OptColumnMappingBuilder.forIndexedElement(entry.getValue())
            .withName("peak_mz").build(measurement == null ? null : String.valueOf(measurement.getMZ())));
        summary.addOptItem(OptColumnMappingBuilder.forIndexedElement(entry.getValue())
            .withName("peak_rt").build(measurement == null ? null : String.valueOf(measurement.getRT())));
        summary.addOptItem(OptColumnMappingBuilder.forIndexedElement(entry.getValue())
            .withName("peak_height").build(measurement == null ? null : String.valueOf(measurement.getHeight())));
      }
    }
    if (summary != null) {
      final List<Double> means = new ArrayList<>();
      final List<Double> variations = new ArrayList<>();
      for (final StudyVariable studyVariable : studyVariables) {
        final List<Double> measured = new ArrayList<>();
        for (final Assay assay : studyVariable.getAssayRefs()) {
          final Double area = areasByAssay.get(assay.getId());
          if (area != null) {
            measured.add(area);
          }
        }
        final Double mean = measured.isEmpty() ? null
            : measured.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        final Double variation;
        if (mean == null) {
          variation = null;
        } else {
          final double squaredDifferences = measured.stream()
              .mapToDouble(value -> (value - mean) * (value - mean)).sum();
          variation = measured.size() < 2 || mean == 0d ? 0d
              : Math.sqrt(squaredDifferences / (measured.size() - 1)) / mean;
        }
        means.add(mean);
        variations.add(variation);
      }
      summary.setAbundanceStudyVariable(means);
      summary.setAbundanceVariationStudyVariable(variations);
    }
  }

  /**
   * Registers a database source without treating absent compound accessions as real identifiers.
   *
   * @param annotation selected annotation
   * @param legacy whether legacy source-URI behavior must be retained
   * @param databases sources already registered
   * @return matching database metadata
   * @throws URISyntaxException if a local provenance URI cannot be represented
   */
  private static @NotNull Database registerDatabase(@NotNull final MzTabmAnnotation annotation,
      final boolean legacy, @NotNull final Map<String, Database> databases) throws URISyntaxException {
    final String name = annotation.databaseName() == null ? annotation.method() : annotation.databaseName();
    final String key = annotation.sourceKey() + "\n" + name;
    final Database existing = databases.get(key);
    if (existing != null) {
      return existing;
    }
    final int id = databases.size() + 1;
    final URI sourceUri = legacy ? annotation.uri() == null
        ? URI.create("mzmine://" + annotation.sourceKey()) : annotation.uri()
        : new URI("mzmine", "annotation-source/" + annotation.sourceKey() + "/" + name, null);
    // decision: A local source URI identifies provenance, not a fabricated external compound link.
    final Database database = new Database().id(id).param(new Parameter().name(name))
        .prefix(annotation.sourceKey() + "_" + id).uri(sourceUri);
    databases.put(key, database);
    return database;
  }

  /**
   * Copies optional compound fields while preserving the standard missing-accession placeholder.
   *
   * @param summary destination summary
   * @param annotation selected annotation
   * @param identifier actual accession or database-prefix:null placeholder
   */
  private static void addSummaryAnnotation(@NotNull final SmallMoleculeSummary summary,
      @NotNull final MzTabmAnnotation annotation, @NotNull final String identifier) {
    summary.addDatabaseIdentifierItem(identifier);
    summary.setChemicalName(Collections.singletonList(annotation.compoundName()));
    summary.setChemicalFormula(Collections.singletonList(annotation.formula()));
    summary.setSmiles(Collections.singletonList(annotation.smiles()));
    summary.setInchi(Collections.singletonList(annotation.inchi()));
    summary.setUri(Collections.singletonList(annotation.uri()));
    if (annotation.adduct() != null) {
      summary.addAdductIonsItem(annotation.adduct());
    }
  }

  /**
   * Uses the row-level charge first, retaining a known charge if a per-file feature lacks it.
   *
   * @param row source row
   * @return absolute charge, or null when unknown
   */
  private static @Nullable Integer resolveCharge(@NotNull final FeatureListRow row) {
    final Integer charge = row.getRowCharge();
    if (charge != null && charge != 0) {
      return Math.abs(charge);
    }
    for (final RawDataFile file : row.getRawDataFiles()) {
      final Feature feature = row.getFeature(file);
      if (feature != null && feature.getCharge() != null && feature.getCharge() != 0) {
        return Math.abs(feature.getCharge());
      }
    }
    return null;
  }

  /**
   * Selects actual query spectra, expanding merged matches to their measured source scans.
   *
   * @param row source feature row
   * @param queryScan spectral-match query spectrum, when available
   * @return measured scans suitable for evidence references
   */
  private static @NotNull List<Scan> evidenceScans(@NotNull final FeatureListRow row,
      @Nullable final Scan queryScan) {
    final List<Scan> scans = new ArrayList<>();
    if (queryScan != null) {
      addSourceScans(queryScan, scans);
    } else {
      for (final RawDataFile file : row.getRawDataFiles()) {
        final Feature feature = row.getFeature(file);
        if (feature != null) {
          for (final Scan scan : feature.getScanNumbers()) {
            addSourceScans(scan, scans);
          }
        }
      }
    }
    return scans;
  }

  /**
   * Recursively resolves merged spectra to scans present in the raw files.
   *
   * @param spectrum original or merged spectrum
   * @param scans measured scans to collect
   */
  private static void addSourceScans(@NotNull final MassSpectrum spectrum,
      @NotNull final List<Scan> scans) {
    if (spectrum instanceof MergedMassSpectrum merged) {
      for (final MassSpectrum source : merged.getSourceSpectra()) {
        addSourceScans(source, scans);
      }
    } else if (spectrum instanceof Scan scan) {
      scans.add(scan);
    }
  }

  /**
   * Refers to zero-based file positions, which need not equal native scan numbers.
   *
   * @param scans measured source scans
   * @param assays raw-file to assay mapping
   * @return unique spectrum references in source order
   */
  private static @NotNull List<SpectraRef> spectraReferences(@NotNull final List<Scan> scans,
      @NotNull final Map<RawDataFile, Assay> assays) {
    final Map<String, SpectraRef> references = new LinkedHashMap<>();
    for (final Scan scan : scans) {
      final Assay assay = assays.get(scan.getDataFile());
      if (assay == null) {
        continue;
      }
      final int index = scan.getDataFile().getScans().indexOf(scan);
      if (index >= 0) {
        references.putIfAbsent(assay.getId() + ":" + index, new SpectraRef()
            .msRun(assay.getMsRunRef().getFirst()).reference("index=" + index));
      }
    }
    return new ArrayList<>(references.values());
  }

  /**
   * Creates a file URI containing the escaped filename without its source directory.
   *
   * @param fileName raw data filename
   * @return filename-only run location
   * @throws URISyntaxException if the filename cannot be represented as a URI
   */
  private static @NotNull URI createRawDataFileLocation(@NotNull final String fileName)
      throws URISyntaxException {
    final String encoded = new URI(null, null, "/" + fileName, null).getRawPath().substring(1);
    return new URI("file://" + encoded);
  }
}
