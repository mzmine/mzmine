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
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleFeatureIdentity;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RawDataFileUtils;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.ac.ebi.pride.jmztab2.utils.errors.MZTabErrorList;
import uk.ac.ebi.pride.jmztab2.utils.errors.MZTabErrorType;

public class MzTabmImportTask extends AbstractTask {

  // parameter values
  private MZmineProject project;
  private final ParameterSet parameters;
  private File inputFile;
  private boolean importRawFiles;
  private double finishedPercentage = 0.0;

  // underlying tasks for importing raw data
  private final List<Task> underlyingTasks = new ArrayList<Task>();

  MzTabmImportTask(MZmineProject project, ParameterSet parameters, File inputFile,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);
    this.project = project;
    this.parameters = parameters;
    this.inputFile = inputFile;
    this.importRawFiles = parameters.getParameter(MzTabmImportParameters.importRawFiles).getValue();
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
      mzTabmFileParser​.parse(System.err, MZTabErrorType.Level.Info, 500);

      // inspect the output of the parse and errors
      MZTabErrorList errors = mzTabmFileParser​.getErrorList();

      // converting the MZTabErrorList into a list of ValidationMessage
      List<ValidationMessage> messages = errors.convertToValidationMessages();

      if (!errors.isEmpty()) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Error processing" + inputFile + ":\n"
            + mzTabmFileParser​.getErrorList().toString() + "\n" + messages.toString());
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
      RawDataFile rawDataArray[] = rawDataFiles.toArray(new RawDataFile[0]);
      ModularFeatureList newFeatureList = new ModularFeatureList(featureListName,
          getMemoryMapStorage(), rawDataArray);

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
      return;
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
          if ((task.getStatus() == TaskStatus.WAITING)
              || (task.getStatus() == TaskStatus.PROCESSING)) {
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
    UserParameter<?, ?> newUserParameter =
        new StringParameter(inputFile.getName() + " study variable", "");
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
    double mzExp = 0, abundance = 0, feature_mz = 0;
    float feature_rt = 0, feature_height = 0, rtValue = 0;
    int charge = 0;
    int rowCounter = 0;
    List<SmallMoleculeFeature> smfList = mzTabmFile.getSmallMoleculeFeature();
    List<SmallMoleculeEvidence> smeList = mzTabmFile.getSmallMoleculeEvidence();
    
    MzTabAccess mzTabAccess = new MzTabAccess(mzTabmFile);

    for (SmallMoleculeFeature smf : smfList) {
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
      formula = sml.getChemicalFormula().get(0);
      description = sml.getChemicalName().get(0);
      charge = smf.getCharge();
      // sm. (smile ->getSmiles(), inchikey -> getInchi(), database ->getDatabase(), reliability
      // ->getReliability)
      if (sml.getUri().size() != 0) {
        url = sml.getUri().get(0);
      }
      // Average Retention Time, convert to minutes for MZmine
      rtValue = smf.getRetentionTimeInSeconds().floatValue()/60.0f;
      // Get corresponding SME objects from SMF
      List<SmallMoleculeEvidence> corrSMEList = mzTabAccess.getEvidences(smf);
      // Identification Method
      method = corrSMEList.get(0).getIdentificationMethod().getName();
      // Identifier
      String identifier = sml.getDatabaseIdentifier().get(0);
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
      // Add shared info to peakListRow
      FeatureListRow newRow = new ModularFeatureListRow(newFeatureList, rowCounter);
      newRow.setAverageMZ(mzExp);
      newRow.setAverageRT(rtValue);
      if (description != null) {
        SimpleFeatureIdentity newIdentity =
            new SimpleFeatureIdentity(description, formula, method, identifier, url);
        newRow.addFeatureIdentity(newIdentity, false);
      }

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
            if (!optAssay.isEmpty()) {
              if (dataFileAssay.getName().equals(optAssay.get().getName())
                  && optCol.getIdentifier().contains("peak_mz")) {
                feature_mz = Double.parseDouble(optCol.getValue());
              } else if (dataFileAssay.getName().equals(optAssay.get().getName())
                  && optCol.getIdentifier().contains("peak_rt")) {
                feature_rt = (float) Double.parseDouble(optCol.getValue());
              } else if (dataFileAssay.getName().equals(optAssay.get().getName())
                  && optCol.getIdentifier().contains("peak_height")) {
                feature_height = (float) Double.parseDouble(optCol.getValue());
              }
            }
          }
        }
        Scan scans[] = {rawData.getScanNumberAtRT(rtValue)};
        DataPoint finalDataPoint[] = new DataPoint[1];
        finalDataPoint[0] = new SimpleDataPoint(feature_mz, feature_height);
        Scan representativeScan = null;
        List<Scan> allFragmentScans = List.of();

        Range<Float> finalRTRange = Range.singleton(feature_rt);
        Range<Double> finalMZRange = Range.singleton(feature_mz);
        Range<Float> finalIntensityRange = Range.singleton(feature_height);
        FeatureStatus status = FeatureStatus.DETECTED;
        if (abundance == 0) {
          status = FeatureStatus.UNKNOWN;
        }

        Feature feature = new ModularFeature(newFeatureList, rawData, feature_mz, feature_rt,
            feature_height, (float) abundance, scans, finalDataPoint, status, representativeScan,
            allFragmentScans, finalRTRange, finalMZRange, finalIntensityRange);

        feature.setCharge(charge);
        newRow.addFeature(rawData, feature);
      }

      // Add row to feature list
      newFeatureList.addRow(newRow);
    }
  }
}
