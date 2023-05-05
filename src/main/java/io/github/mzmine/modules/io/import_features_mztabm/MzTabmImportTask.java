/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.io.import_features_mztabm;

import com.google.common.collect.Range;
import de.isas.mztab2.io.MzTabFileParser;
import de.isas.mztab2.model.Assay;
import de.isas.mztab2.model.MsRun;
import de.isas.mztab2.model.MzTab;
import de.isas.mztab2.model.MzTabAccess;
import de.isas.mztab2.model.OptColumnMapping;
import de.isas.mztab2.model.SmallMoleculeEvidence;
import de.isas.mztab2.model.SmallMoleculeFeature;
import de.isas.mztab2.model.SmallMoleculeSummary;
import de.isas.mztab2.model.StudyVariable;
import de.isas.mztab2.model.ValidationMessage;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.CompoundDatabaseMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.InChIKeyStructureType;
import io.github.mzmine.datamodel.features.types.annotations.InChIStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.numbers.BestScanNumberType;
import io.github.mzmine.datamodel.features.types.numbers.MzPpmDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.RTRangeType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.features.types.numbers.scores.CompoundAnnotationScoreType;
import io.github.mzmine.datamodel.identities.iontype.IonTypeParser;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.MolecularSpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.MatchedLipid;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RawDataFileUtils;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import io.github.mzmine.util.spectraldb.entry.SpectralDBEntry;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.ac.ebi.pride.jmztab2.utils.errors.MZTabErrorList;
import uk.ac.ebi.pride.jmztab2.utils.errors.MZTabErrorType.Level;

public class MzTabmImportTask extends AbstractTask {

  // parameter values
  private final MZmineProject project;
  private final ParameterSet parameters;
  private final File inputFile;
  private final boolean importRawFiles;
  private @NotNull
  final Map<String, IonizationType> lipidIonTypeMap;
  private double finishedPercentage = 0.0;

  // underlying tasks for importing raw data
  private final List<Task> underlyingTasks = new ArrayList<>();

  public MzTabmImportTask(MZmineProject project, ParameterSet parameters, File inputFile,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);
    this.project = project;
    this.parameters = parameters;
    this.inputFile = inputFile;
    this.importRawFiles = parameters.getParameter(MzTabmImportParameters.importRawFiles).getValue();

    lipidIonTypeMap = Arrays.stream(IonizationType.values())
        .collect(Collectors.toMap(IonizationType::getAdductName, ion -> ion));
  }

  @Override
  public String getTaskDescription() {
    return "Loading feature list from mzTab-m file " + inputFile;
  }

  @Override
  public void cancel() {
    super.cancel();
    // Cancel all the data import tasks
    for (Task t : underlyingTasks) {
      if ((t.getStatus() == TaskStatus.WAITING) || (t.getStatus() == TaskStatus.PROCESSING)) {
        t.cancel();
      }
    }
  }

  @Override
  public double getFinishedPercentage() {
    if (importRawFiles && (getStatus() == TaskStatus.PROCESSING) && (!underlyingTasks.isEmpty())) {
      double newPercentage = 0.0;
      synchronized (underlyingTasks) {
        for (Task t : underlyingTasks) {
          newPercentage += t.getFinishedPercentage();
        }
        newPercentage /= underlyingTasks.size();
      }
      // Let's say that raw data import takes 80% of the time
      finishedPercentage = 0.1 + newPercentage * 0.8;
    }
    return finishedPercentage;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    try {

      // Load mzTab file
      MzTabFileParser mzTabmFileParser​ = new MzTabFileParser(inputFile);
      mzTabmFileParser​.parse(System.err, Level.Info, 500);

      // inspect the output of the parse and errors
      MZTabErrorList errors = mzTabmFileParser​.getErrorList();

      // converting the MZTabErrorList into a list of ValidationMessage
      List<ValidationMessage> messages = errors.convertToValidationMessages();

      if (!errors.isEmpty()) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage(
            "Error processing" + inputFile + ":\n" + mzTabmFileParser​.getErrorList().toString()
                + "\n" + messages.toString());
        return;
      }
      MzTab mzTabFile = mzTabmFileParser​.getMZTabFile();

      // Let's say initial parsing took 10% of the time
      finishedPercentage = 0.1;

      // Import raw data files
      List<RawDataFile> rawDataFiles = importRawDataFiles(mzTabFile);

      // Check if not canceled
      if (isCanceled()) {
        return;
      }

      // Create new feature list
      String featureListName = inputFile.getName().replace(".mzTab", "");
      ModularFeatureList newFeatureList = new ModularFeatureList(featureListName,
          getMemoryMapStorage(), rawDataFiles);

      // Check if not canceled
      if (isCanceled()) {
        return;
      }

      // Import variables
      importVariables(mzTabFile, rawDataFiles);

      // Check if not canceled
      if (isCanceled()) {
        return;
      }
      // import small molecules feature (=feature list rows)
      importTablesData(newFeatureList, mzTabFile, rawDataFiles);

      // Check if not canceled
      if (isCanceled()) {
        return;
      }

      // Add the new feature list to the project
      project.addFeatureList(newFeatureList);

      // Finish
      setStatus(TaskStatus.FINISHED);
      finishedPercentage = 1.0;

    } catch (Exception e) {
      e.printStackTrace();
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not import data from " + inputFile + ": " + e.getMessage());
    }
  }

  private List<RawDataFile> importRawDataFiles(MzTab mzTabmFile) throws Exception {
    List<MsRun> msrun = mzTabmFile.getMetadata().getMsRun();
    List<RawDataFile> rawDataFiles = new ArrayList<>();

    // if Import option is selected in parameters window
    if (importRawFiles) {
      List<File> filesToImport = new ArrayList<>();
      for (MsRun singleRun : msrun) {
        File fileToImport = new File(singleRun.getLocation());
        if (fileToImport.exists() && fileToImport.canRead()) {
          filesToImport.add(fileToImport);
        } else {
          // Check if the raw file exists in the same folder as the
          // mzTab file
          File checkFile = new File(inputFile.getParentFile(), fileToImport.getName());
          if (checkFile.exists() && checkFile.canRead()) {
            filesToImport.add(checkFile);
          } else {
            // Append .gz & check again if file exists as a
            // workaround to .gz not getting preserved
            // when .mzML.gz importing
            checkFile = new File(inputFile.getParentFile(), fileToImport.getName() + ".gz");
            if (checkFile.exists() && checkFile.canRead()) {
              filesToImport.add(checkFile);
            } else {
              // One more level of checking, appending .zip &
              // checking as a workaround
              checkFile = new File(inputFile.getParentFile(), fileToImport.getName() + ".zip");
              if (checkFile.exists() && checkFile.canRead()) {
                filesToImport.add(checkFile);
              }
            }

          }
        }
      }

      // import files
      RawDataFileUtils.createRawDataImportTasks(MZmineCore.getProjectManager().getCurrentProject(),
          underlyingTasks, MZTabmImportModule.class, parameters, moduleCallDate,
          filesToImport.toArray(new File[0]));
      if (underlyingTasks.size() > 0) {
        MZmineCore.getTaskController().addTasks(underlyingTasks.toArray(new Task[0]));
      }

      // Wait until all raw data file imports have completed
      while (true) {
        if (isCanceled()) {
          return null;
        }
        boolean tasksFinished = true;
        for (Task task : underlyingTasks) {
          if ((task.getStatus() == TaskStatus.WAITING) || (task.getStatus()
              == TaskStatus.PROCESSING)) {
            tasksFinished = false;
          }
        }
        if (tasksFinished) {
          break;
        }
        Thread.sleep(1000);
      }
    } else {
      finishedPercentage = 0.5;
    }

    // one storage for all files imported in the same task as they are typically analyzed together
    final MemoryMapStorage storage = MemoryMapStorage.forRawDataFile();

    // Find a matching RawDataFile for each msRun object
    for (MsRun singleRun : msrun) {
      String rawFileName = new File(singleRun.getLocation()).getName();
      RawDataFile rawDataFile = null;
      // check whether we already have raw data file of that name
      for (RawDataFile f : project.getDataFiles()) {
        if (f.getName().equals(rawFileName)) {
          rawDataFile = f;
          break;
        }
      }

      // if no data file of that name exist, create a new one
      if (rawDataFile == null) {
        rawDataFile = MZmineCore.createNewFile(rawFileName, null, storage);
        project.addFile(rawDataFile);
      }

      // Save a reference to the new raw data file
      rawDataFiles.add(rawDataFile);
    }
    return rawDataFiles;
  }

  private void importVariables(MzTab mzTabmFile, List<RawDataFile> rawDataFiles) {
    // Add sample parameters if available in mzTab-m file
    List<StudyVariable> variableMap = mzTabmFile.getMetadata().getStudyVariable();
    if (variableMap.isEmpty()) {
      return;
    }
    UserParameter<?, ?> newUserParameter = new StringParameter(
        inputFile.getName() + " study variable", "");
    project.addParameter(newUserParameter);
    for (StudyVariable studyVariable : variableMap) {
      // Stop the process if cancel() was called
      if (isCanceled()) {
        return;
      }

      List<Assay> assayList = studyVariable.getAssayRefs();
      for (int i = 0; i < assayList.size(); i++) {
        Assay dataFileAssay = assayList.get(i);
        if (dataFileAssay != null) {
          int indexOfAssay = mzTabmFile.getMetadata().getAssay().indexOf(dataFileAssay);
          project.setParameterValue(newUserParameter, rawDataFiles.get(indexOfAssay),
              studyVariable.getDescription());
        }
      }
    }
  }

  private void importTablesData(ModularFeatureList newFeatureList, MzTab mzTabmFile,
      List<RawDataFile> rawDataFiles) {

    List<Assay> assayList = mzTabmFile.getMetadata().getAssay();
    List<SmallMoleculeSummary> smallMoleculeSummaryList = mzTabmFile.getSmallMoleculeSummary();

    // Loop through SMF data
    String formula, description, method, url = "";
    double mzExp, abundance = 0, feature_mz;
    float feature_rt, feature_height = 0, rtValue;
    int charge, rowCounter = 0;
    List<SmallMoleculeFeature> smfList = mzTabmFile.getSmallMoleculeFeature();
    List<SmallMoleculeEvidence> smeList = mzTabmFile.getSmallMoleculeEvidence();

    MzTabAccess mzTabAccess = new MzTabAccess(mzTabmFile);

    for (int j = 0; j < smfList.size(); j++) {
      SmallMoleculeFeature smf = smfList.get(j);
      // Stop the process if cancel() is called
      if (isCanceled()) {
        return;
      }
      rowCounter++;
      // getSML object corresponding to the SMF object
      SmallMoleculeSummary sml = new SmallMoleculeSummary();
      for (SmallMoleculeSummary sm : smallMoleculeSummaryList) {
        if (sm.getSmfIdRefs().contains(smf.getSmfId())) {
          sml = sm;
          break;
        }
      }

      formula = smeList.get(rowCounter - 1).getChemicalFormula();

      description = sml.getChemicalName().toString();
      charge = smf.getCharge();

      // sm. (smile ->getSmiles(), inchikey -> getInchi(), database ->getDatabase(), reliability
      // ->getReliability)
      if (sml.getUri().size() != 0) {
        url = sml.getUri().get(0);
      }

      // Get corresponding SME objects from SMF
      List<SmallMoleculeEvidence> corrSMEList = mzTabAccess.getEvidences(smf);

      // Average Retention Time, convert to minutes for MZmine
      rtValue = smf.getRetentionTimeInSeconds().floatValue() / 60.0f;

      // Identification Method
      try {
        method = corrSMEList.get(0).getIdentificationMethod().getName();
      } catch (Exception e) {
        method = null;
      }
      // Identifier
      String identifier = sml.getDatabaseIdentifier().toString(); //.get(0);
      if ((url != null) && (url.equals("null"))) {
        url = null;
      }
      if (identifier != null && identifier.equals("null")) {
        identifier = null;
      }
      if (description == null && identifier != null) {
        description = identifier;
      }
      // m/z value
      mzExp = smf.getExpMassToCharge();

      // Add shared info to featureListRow
      FeatureListRow featureListRow = new ModularFeatureListRow(newFeatureList, rowCounter);
      featureListRow.setAverageMZ(mzExp);
      featureListRow.setAverageRT(rtValue);
      if (sml.getTheoreticalNeutralMass().size() != 0) {
        featureListRow.set(NeutralMassType.class, sml.getTheoreticalNeutralMass().get(0));
      }

      if (description != null) {
        if (method != null) {
          featureListRow.set(FormulaType.class, formula);
        }
      }
      //===============================================

      // Add raw data file entries to row
      for (int i = 0; i < rawDataFiles.size(); i++) {
        Assay dataFileAssay = assayList.get(i);
        RawDataFile rawData = rawDataFiles.get(i);

        if (smf.getAbundanceAssay().get(i) != null) {
          abundance = smf.getAbundanceAssay().get(i);
        }

        List<OptColumnMapping> optColList = sml.getOpt();
        // Use average values if optional data for each msrun is not provided
        feature_mz = mzExp;
        // MzMine expects minutes, mzTab-M uses seconds
        feature_rt = rtValue;

        if (optColList != null) {
          for (OptColumnMapping optCol : optColList) {
            Optional<Assay> optAssay = mzTabAccess.getAssayFor(optCol, mzTabmFile.getMetadata());
            //if there is no assays
            if (optAssay.isEmpty() || dataFileAssay.getName() == null) {
              if (optCol.getIdentifier().contains("peak_mz") || optCol.getIdentifier()
                  .contains("feature_mz")) {
                feature_mz = Double.parseDouble(optCol.getValue());
              }
              if (optCol.getIdentifier().contains("peak_rt") || optCol.getIdentifier()
                  .contains("feature_rt")) {
                feature_rt = Float.parseFloat(optCol.getValue()) / 60f;
              }
              if (optCol.getIdentifier().contains("peak_height") || optCol.getIdentifier()
                  .contains("feature_height")) {
                feature_height = Float.parseFloat(optCol.getValue());
              }
            } else {
              if (dataFileAssay.getName().equals(optAssay.get().getName()) && (
                  optCol.getIdentifier().contains("peak_mz") || optCol.getIdentifier()
                      .contains("feature_mz"))) {
                feature_mz = Double.parseDouble(optCol.getValue());
              } else if (dataFileAssay.getName().equals(optAssay.get().getName()) && (
                  optCol.getIdentifier().contains("peak_rt") || optCol.getIdentifier()
                      .contains("feature_rt"))) {
                feature_rt = (float) Double.parseDouble(optCol.getValue());
              } else if (dataFileAssay.getName().equals(optAssay.get().getName()) && (
                  optCol.getIdentifier().contains("peak_height") || optCol.getIdentifier()
                      .contains("feature_height"))) {
                feature_height = (float) Double.parseDouble(optCol.getValue()) / 60f;
              }
            }
          }
        }

        //final DataPoint
        //DataPoint[] finalDataPoint = new DataPoint[1];
        //finalDataPoint[0] = new SimpleDataPoint(feature_mz, feature_height);
        //List<Scan> allFragmentScans = List.of();

        //import rt ranges
        if (smf.getRetentionTimeInSecondsStart().floatValue() != 0f
            && smf.getRetentionTimeInSecondsEnd().floatValue() != 0f) {
          Range<Float> rtRange = Range.closed(
              smf.getRetentionTimeInSecondsStart().floatValue() / 60f,
              smf.getRetentionTimeInSecondsEnd().floatValue() / 60f);
          featureListRow.set(RTRangeType.class, rtRange);
        } else {
          featureListRow.set(RTRangeType.class, Range.singleton(feature_rt));
        }

        Range<Double> finalMZRange = Range.singleton(feature_mz);
        Range<Float> finalIntensityRange = Range.singleton(feature_height);

        FeatureStatus status;
        if (abundance == 0) {
          status = FeatureStatus.UNKNOWN;
        } else {
          status = FeatureStatus.DETECTED;
        }

        ModularFeature feature = new ModularFeature(newFeatureList, rawData, status);

        feature.setMZ(feature_mz);
        feature.setRT(feature_rt);
        feature.setArea((float) abundance);

        //obtain representative scan from raw data
        Scan[] closestScans = {rawData.binarySearchClosestScan(rtValue)};
        Scan representativeScan = closestScans[0];
        feature.setRepresentativeScan(representativeScan);
        feature.set(BestScanNumberType.class, (feature.getRepresentativeScan()));

        //todo fix shape rendering and create FeatureData
        //scan indices can be extracted from spectra ref SME section
        //double[] mzs = representativeScan.getMzValues(
        //    new double[representativeScan.getNumberOfDataPoints()]);
        //double[] intensities = representativeScan.getIntensityValues(
        //    new double[representativeScan.getNumberOfDataPoints()]);
        //List<Scan> scanList = null;
        //create scan list here using getSpectraRef().
        //smeList.get(j).getSpectraRef().stream().map(SpectraRef::getReference).toArray();

        // FetureShapeChart requires FeatureData
        // SimpleIonTimeSeries featureData = new SimpleIonTimeSeries(getMemoryMapStorage(), mzs, intensities, scanList);
        // feature.set(FeatureDataType.class, featureData);

        feature.setHeight(feature_height);
        feature.setCharge(charge);
        //feature.setAllMS2FragmentScans(allFragmentScans);

        feature.setRawDataPointsMZRange(finalMZRange);
        feature.setRawDataPointsIntensityRange(finalIntensityRange);

        //import annotations
        featureListRow.set(CompoundNameType.class, smeList.get(j).getChemicalName());
        featureListRow.set(FormulaType.class, smeList.get(j).getChemicalFormula());
        featureListRow.set(InChIKeyStructureType.class, smeList.get(j).getInchi());
        featureListRow.set(SmilesStructureType.class, smeList.get(j).getSmiles());

        List<OptColumnMapping> optSMEColList = smeList.get(j).getOpt();
        if (optColList != null) {
          List<CompoundDBAnnotation> compoundAnnotations = new ArrayList<>();
          CompoundDBAnnotation compoundDBIdentity = new SimpleCompoundDBAnnotation();

          List<SpectralDBAnnotation> matches = new ArrayList<>();

          //if the representative scan is null, the arbitrary m/z and intensities arrays are used here
          SpectralDBEntry spectralDBEntry = null;
          if (representativeScan != null) {
            spectralDBEntry = new SpectralDBEntry(null, representativeScan.getMzValues(
                new double[representativeScan.getNumberOfDataPoints()]),
                representativeScan.getIntensityValues(
                    new double[representativeScan.getNumberOfDataPoints()]));
          } else {
            spectralDBEntry = new SpectralDBEntry(null, new double[1000], new double[1000]);
          }
          Double similarityScore = null;
          Integer numMatchedSignals = null;
          SpectralDBAnnotation spectralDBAnnotation;
          Map<DBEntryField, Object> map = new HashMap<>();

          MolecularSpeciesLevelAnnotation molecularSpeciesLevelAnnotation = new MolecularSpeciesLevelAnnotation(
              null, null, null, null);
          MatchedLipid matchedLipid = new MatchedLipid(null, null, null, null, null);

          for (OptColumnMapping optCol : optSMEColList) {
            if (!optCol.getValue().equals("null")) {
              if (optCol.getIdentifier().contains("compound_db_identity")) {
                extractCompoundDBAnnotations(compoundDBIdentity, optCol);
              }
              if (optCol.getIdentifier().contains("spectral_db_matches")) {
                extractSpectralDBAnnotations(map, optCol);
                if (optCol.getIdentifier().contains("cosine_score")) {
                  similarityScore = Double.parseDouble(optCol.getValue());
                }
                if (optCol.getIdentifier().contains("n_matching_signals")) {
                  map.put(DBEntryField.OTHER_MATCHED_COMPOUNDS_N,
                      Integer.parseInt(optCol.getValue()));
                  numMatchedSignals = Integer.parseInt(optCol.getValue());
                }
              }
              if (optCol.getIdentifier().contains("lipid_annotations")) {
                extractLipidAnnotations(molecularSpeciesLevelAnnotation, matchedLipid, optCol);
              }
            }
          }
          matchedLipid.setLipidAnnotation(molecularSpeciesLevelAnnotation);
          featureListRow.addLipidAnnotation(matchedLipid);
          compoundAnnotations.add(compoundDBIdentity);
          featureListRow.set(CompoundDatabaseMatchesType.class, compoundAnnotations);

          spectralDBEntry.putAll(map);
          if (similarityScore != null) {
            SpectralSimilarity similarity = new SpectralSimilarity("Cosine similarity",
                similarityScore, numMatchedSignals, Double.NaN);
            spectralDBAnnotation = new SpectralDBAnnotation(spectralDBEntry, similarity, null,
                null);
            matches.add(spectralDBAnnotation);
          }
          featureListRow.addSpectralLibraryMatches(matches);
        }
        featureListRow.addFeature(rawData, feature);
      }
      // Add row to feature list
      newFeatureList.addRow(featureListRow);
    }
  }

  private void extractLipidAnnotations(
      MolecularSpeciesLevelAnnotation molecularSpeciesLevelAnnotation, MatchedLipid matchedLipid,
      OptColumnMapping optCol) {
    Double mzDiffPpm = null;
    Double msmsScore = null;
    //todo matched signals
    //Set<LipidFragment> matchedSignals = null;

    if (optCol.getIdentifier().contains("lipid_annotations_lipid_annotations")) {
      molecularSpeciesLevelAnnotation.setAnnotation(optCol.getValue());
    }
    if (optCol.getIdentifier().contains("ion_adduct")) {
      matchedLipid.setIonizationType(lipidIonTypeMap.getOrDefault(optCol.getValue(), null));
    }
    if (optCol.getIdentifier().contains("mol_formula")) {
      molecularSpeciesLevelAnnotation.setMolecularFormula(
          FormulaUtils.createMajorIsotopeMolFormula(optCol.getValue()));
    }
    if (optCol.getIdentifier().contains("mz_diff_ppm")) {
      mzDiffPpm = Double.parseDouble(optCol.getValue());
    }
    if (optCol.getIdentifier().contains("msms_score")) {
      msmsScore = Double.parseDouble(optCol.getValue());
    }
    //if (optCol.getIdentifier().contains("matched_signals")) {
    //todo add matched signals
    //}

    if (mzDiffPpm != null) {
      matchedLipid.setAccurateMz(mzDiffPpm);
    }
    if (msmsScore != null) {
      matchedLipid.setMsMsScore(msmsScore);
    }

    //todo add matched signals
    //matchedLipid.setMatchedFragments(matchedSignals);
  }

  private static void extractSpectralDBAnnotations(Map<DBEntryField, Object> map,
      OptColumnMapping optCol) {
    if (optCol.getIdentifier().contains("compound_name")) {
      map.put(DBEntryField.NAME, optCol.getValue());
    }
    if (optCol.getIdentifier().contains("ion_adduct")) {
      map.put(DBEntryField.ION_TYPE, IonTypeParser.parse(optCol.getValue()));
    }
    if (optCol.getIdentifier().contains("mol_formula")) {
      map.put(DBEntryField.FORMULA, optCol.getValue());
    }
    if (optCol.getIdentifier().contains("smiles")) {
      map.put(DBEntryField.SMILES, optCol.getValue());
    }
    if (optCol.getIdentifier().contains("inchi")) {
      map.put(DBEntryField.INCHI, optCol.getValue());
    }
    if (optCol.getIdentifier().contains("precursor_mz")) {
      map.put(DBEntryField.PRECURSOR_MZ, Double.parseDouble(optCol.getValue()));
    }
    if (optCol.getIdentifier().contains("neutral_mass")) {
      map.put(DBEntryField.EXACT_MASS, Double.parseDouble(optCol.getValue()));
    }
    if (optCol.getIdentifier().contains("precursor_mz")) {
      map.put(DBEntryField.PRECURSOR_MZ, Double.parseDouble(optCol.getValue()));
    }
    if (optCol.getIdentifier().contains("ccs")) {
      map.put(DBEntryField.CCS, Float.parseFloat(optCol.getValue()));
    }
    if (optCol.getIdentifier().contains("ccs_percent_error")) {
      //todo add ccs percent error
    }
  }

  private static void extractCompoundDBAnnotations(CompoundDBAnnotation newIdentity,
      OptColumnMapping optCol) {
    if (optCol.getIdentifier().contains("compound_name")) {
      newIdentity.put(CompoundNameType.class, optCol.getValue());
    }
    if (optCol.getIdentifier().contains("annotation_score")) {
      newIdentity.put(CompoundAnnotationScoreType.class, Float.parseFloat(optCol.getValue()));
    }
    if (optCol.getIdentifier().contains("mol_formula")) {
      newIdentity.put(FormulaType.class, optCol.getValue());
    }
    if (optCol.getIdentifier().contains("adduct")) {
      newIdentity.put(IonTypeType.class, IonTypeParser.parse(optCol.getValue()));
    }
    if (optCol.getIdentifier().contains("precursor_mz")) {
      newIdentity.put(PrecursorMZType.class, Double.parseDouble(optCol.getValue()));
    }
    if (optCol.getIdentifier().contains("smiles")) {
      newIdentity.put(SmilesStructureType.class, optCol.getValue());
    }
    if (optCol.getIdentifier().contains("inchi")) {
      newIdentity.put(InChIStructureType.class, optCol.getValue());
    }
    if (optCol.getIdentifier().contains("mz_diff_ppm")) {
      newIdentity.putIfNotNull(MzPpmDifferenceType.class, Float.parseFloat(optCol.getValue()));
    }
    if (optCol.getIdentifier().contains("neutral_mass")) {
      newIdentity.putIfNotNull(NeutralMassType.class, Double.parseDouble(optCol.getValue()));
    }
    if (optCol.getIdentifier().contains("rt")) {
      newIdentity.putIfNotNull(RTType.class, Float.parseFloat(optCol.getValue()));
    }
    if (optCol.getIdentifier().contains("ccs")) {
      newIdentity.putIfNotNull(NeutralMassType.class, Double.parseDouble(optCol.getValue()));
    }
  }
}
