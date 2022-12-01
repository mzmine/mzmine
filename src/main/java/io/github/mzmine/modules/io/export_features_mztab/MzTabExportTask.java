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

package io.github.mzmine.modules.io.export_features_mztab;

import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Pattern;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import org.jetbrains.annotations.NotNull;
import uk.ac.ebi.pride.jmztab.model.Assay;
import uk.ac.ebi.pride.jmztab.model.CVParam;
import uk.ac.ebi.pride.jmztab.model.MZTabColumnFactory;
import uk.ac.ebi.pride.jmztab.model.MZTabDescription;
import uk.ac.ebi.pride.jmztab.model.Metadata;
import uk.ac.ebi.pride.jmztab.model.MsRun;
import uk.ac.ebi.pride.jmztab.model.Section;
import uk.ac.ebi.pride.jmztab.model.SmallMolecule;
import uk.ac.ebi.pride.jmztab.model.SmallMoleculeColumn;
import uk.ac.ebi.pride.jmztab.model.StudyVariable;

class MzTabExportTask extends AbstractTask {

  private int processedRows = 0, totalRows = 0;
  String newLine = System.lineSeparator();

  // parameter values
  private final MZmineProject project;
  private final File fileName;
  private String plNamePattern = "{}";
  private FeatureList[] featureLists;
  private final boolean exportall;

  MzTabExportTask(MZmineProject project, ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.project = project;
    this.featureLists =
        parameters.getParameter(MzTabExportParameters.featureLists).getValue().getMatchingFeatureLists();
    this.fileName = parameters.getParameter(MzTabExportParameters.filename).getValue();
    this.exportall = parameters.getParameter(MzTabExportParameters.exportall).getValue();
  }

  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0;
    }
    return (double) processedRows / (double) totalRows;
  }

  @Override
  public String getTaskDescription() {
    return "Exporting feature list(s) " + Arrays.toString(featureLists) + " to MzTab file(s)";
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    // Shall export several files?
    boolean substitute = fileName.getPath().contains(plNamePattern);

    // Total number of rows
    for (FeatureList peakList : featureLists) {
      totalRows += peakList.getNumberOfRows();
    }

    // Process feature lists
    for (FeatureList peakList : featureLists) {

      File curFile = fileName;
      try {

        // Filename
        if (substitute) {
          // Cleanup from illegal filename characters
          String cleanPlName = peakList.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
          // Substitute
          String newFilename =
              fileName.getPath().replaceAll(Pattern.quote(plNamePattern), cleanPlName);
          curFile = new File(newFilename);
        }

        // Open file
        FileWriter writer;
        try {
          writer = new FileWriter(curFile);
        } catch (Exception e) {
          setStatus(TaskStatus.ERROR);
          setErrorMessage("Could not open file " + curFile + " for writing.");
          return;
        }

        // Metadata
        Metadata mtd = new Metadata();
        mtd.setMZTabMode(MZTabDescription.Mode.Summary);
        mtd.setMZTabType(MZTabDescription.Type.Quantification);
        mtd.setDescription(peakList.getName());
        mtd.addSoftwareParam(1,
            new CVParam("MS", "MS:1002342", "MZmine", String.valueOf(MZmineCore.getMZmineVersion())));
        mtd.setSmallMoleculeQuantificationUnit(
            new CVParam("PRIDE", "PRIDE:0000330", "Arbitrary quantification unit", null));
        mtd.addSmallMoleculeSearchEngineScoreParam(1,
            new CVParam("MS", "MS:1001153", "search engine specific score", null));
        mtd.addFixedModParam(1,
            new CVParam("MS", "MS:1002453", "No fixed modifications searched", null));
        mtd.addVariableModParam(1,
            new CVParam("MS", "MS:1002454", "No variable modifications searched", null));

        // Create stable columns
        MZTabColumnFactory factory = MZTabColumnFactory.getInstance(Section.Small_Molecule);
        factory.addDefaultStableColumns();

        // Add optional columns which have stable order
        factory.addURIOptionalColumn();
        factory.addBestSearchEngineScoreOptionalColumn(SmallMoleculeColumn.BEST_SEARCH_ENGINE_SCORE,
            1);

        final RawDataFile rawDataFiles[] = peakList.getRawDataFiles().toArray(RawDataFile[]::new);
        int fileCounter = 0;
        for (RawDataFile file : rawDataFiles) {
          fileCounter++;

          /**
           * TO DO: Add path to original imported raw file to MZmine and write it out here instead
           */
          // MS run location
          MsRun msRun = new MsRun(fileCounter);
          msRun.setLocation(new URL("file:///" + file.getName()));
          mtd.addMsRun(msRun);
          mtd.addAssayMsRun(fileCounter, msRun);

          // Add samples to study variable assay
          for (UserParameter<?, ?> p : project.getParameters()) {
            Assay assay = mtd.getAssayMap().get(fileCounter);
            for (StudyVariable studyVariable : mtd.getStudyVariableMap().values()) {
              if (studyVariable.getDescription().equals(
                  String.valueOf(p) + ": " + String.valueOf(project.getParameterValue(p, file)))) {
                mtd.addStudyVariableAssay(studyVariable.getId(), assay);
              }
            }
          }

          // Additional columns
          factory.addAbundanceOptionalColumn(new Assay(fileCounter));
          factory.addOptionalColumn(new Assay(fileCounter), "peak_mz", String.class);
          factory.addOptionalColumn(new Assay(fileCounter), "peak_rt", String.class);
          factory.addOptionalColumn(new Assay(fileCounter), "peak_height", String.class);
        }

        // Variable descriptions
        int parameterCounter = 0;
        for (UserParameter<?, ?> p : project.getParameters()) {
          for (Object e : ((ComboParameter<?>) p).getChoices()) {
            parameterCounter++;
            mtd.addStudyVariableDescription(parameterCounter,
                String.valueOf(p) + ": " + String.valueOf(e));
            StudyVariable studyVariable = new StudyVariable(parameterCounter);
            factory.addAbundanceOptionalColumn(studyVariable);
          }
        }

        // Write to file
        BufferedWriter out = new BufferedWriter(writer);
        out.write(mtd.toString());
        out.write(newLine);
        out.write(factory.toString());
        out.write(newLine);

        // Write data rows
        for (FeatureListRow peakListRow : peakList.getRows()) {

          // Cancel?
          if (isCanceled()) {
            return;
          }

          FeatureIdentity featureIdentity = peakListRow.getPreferredFeatureIdentity();
          if (exportall || featureIdentity != null) {
            SmallMolecule sm = new SmallMolecule(factory, mtd);
            if (featureIdentity != null) {
              // Identity information
              String identifier = escapeString(featureIdentity.getPropertyValue("ID"));
              String database = featureIdentity.getPropertyValue("Identification method");
              String formula = featureIdentity.getPropertyValue("Molecular formula");
              String description = escapeString(featureIdentity.getPropertyValue("Name"));
              String url = featureIdentity.getPropertyValue("URL");

              if (identifier != null) {
                sm.setIdentifier(identifier);
              }
              if (database != null) {
                sm.setDatabase(database);
              }
              if (formula != null) {
                sm.setChemicalFormula(formula);
              }
              if (description != null) {
                sm.setDescription(description);
              }
              if (url != null) {
                sm.setURI(url);
              }
            }

            Double rowMZ = peakListRow.getAverageMZ();
            int rowCharge = peakListRow.getRowCharge();
            String rowRT = String.valueOf(peakListRow.getAverageRT());

            if (rowMZ != null) {
              sm.setExpMassToCharge(rowMZ);
            }
            if (rowCharge > 0) {
              sm.setCharge(rowCharge);
            }
            if (rowRT != null) {
              sm.setRetentionTime(rowRT);
            }

            int dataFileCount = 0;
            for (RawDataFile dataFile : rawDataFiles) {
              dataFileCount++;
              Feature peak = peakListRow.getFeature(dataFile);
              if (peak != null) {
                String peakMZ = String.valueOf(peak.getMZ());
                String peakRT = String.valueOf(String.valueOf(peak.getRT()));
                String peakHeight = String.valueOf(peak.getHeight());
                double peakArea = peak.getArea();

                sm.setOptionColumnValue(new Assay(dataFileCount), "peak_mz", peakMZ);
                sm.setOptionColumnValue(new Assay(dataFileCount), "peak_rt", peakRT);
                sm.setOptionColumnValue(new Assay(dataFileCount), "peak_height", peakHeight);
                sm.setAbundanceColumnValue(new Assay(dataFileCount), peakArea);
              }
            }

            out.write(sm.toString());
            out.write(newLine);
          }

        }

        out.flush();
        out.close();
        writer.close();

      } catch (Exception e) {
        e.printStackTrace();
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Could not export feature list to file " + curFile + ": " + e.getMessage());
        return;
      }
    }

    if (getStatus() == TaskStatus.PROCESSING)
      setStatus(TaskStatus.FINISHED);

  }

  private String escapeString(final String inputString) {

    if (inputString == null)
      return "";

    // Remove all special characters e.g. \n \t
    return inputString.replaceAll("[\\p{Cntrl}]", " ");
  }

}
