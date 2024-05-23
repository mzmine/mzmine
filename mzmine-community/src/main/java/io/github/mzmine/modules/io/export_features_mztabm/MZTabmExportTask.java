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

package io.github.mzmine.modules.io.export_features_mztabm;

import com.google.common.collect.Range;
import de.isas.mztab2.io.MzTabValidatingWriter;
import de.isas.mztab2.model.Assay;
import de.isas.mztab2.model.CV;
import de.isas.mztab2.model.Database;
import de.isas.mztab2.model.Metadata;
import de.isas.mztab2.model.MsRun;
import de.isas.mztab2.model.MzTab;
import de.isas.mztab2.model.OptColumnMapping;
import de.isas.mztab2.model.Parameter;
import de.isas.mztab2.model.SmallMoleculeEvidence;
import de.isas.mztab2.model.SmallMoleculeFeature;
import de.isas.mztab2.model.SmallMoleculeSummary;
import de.isas.mztab2.model.Software;
import de.isas.mztab2.model.SpectraRef;
import de.isas.mztab2.model.StudyVariable;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.datamodel.features.types.modifiers.NoTextColumn;
import io.github.mzmine.datamodel.features.types.numbers.RTRangeType;
import io.github.mzmine.main.MZmineConfiguration;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.molecular_species.MolecularSpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import io.github.mzmine.util.io.SemverVersionReader;
import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.ac.ebi.pride.jmztab2.model.IOptColumnMappingBuilder;
import uk.ac.ebi.pride.jmztab2.model.OptColumnMappingBuilder;
import uk.ac.ebi.pride.jmztab2.model.OptColumnMappingBuilder.GlobalOptColumnMappingBuilder;

public class MZTabmExportTask extends AbstractTask {

  // parameter values
  private final MZmineProject project;
  private final File fileName;
  private final boolean exportAll;
  //atm default values are used in case charges, theoretical masses, and best_confidence_value
  //in case if value is null
  //modify in case mzTab-M specification is updated
  private final Integer DEFAULT_INTEGER_VALUE = 1000;
  private final Double DEFAULT_DOUBLE_VALUE = 1000.00;
  private int processedRows = 0, totalRows = 0;
  private String plNamePattern = "{}";
  private FeatureList featureList;
  private Metadata mtd;
  private SmallMoleculeSummary sm;
  private SmallMoleculeFeature smf;
  private List<GlobalOptColumnMappingBuilder> globalOptColumns;
  private int columnCount;


  MZTabmExportTask(MZmineProject project, FeatureList featureList, ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.project = project;
    this.featureList = featureList;
    this.fileName = parameters.getParameter(MZTabmExportParameters.filename).getValue();
    this.exportAll = parameters.getParameter(MZTabmExportParameters.exportAll).getValue();
  }

  @NotNull
  private static List<SpectraRef> generateSpectraRef(Feature feature,
      HashMap<RawDataFile, Assay> rawDataFileToAssay) {
    List<SpectraRef> sr = new ArrayList<>();
    for (Scan scan : feature.getScanNumbers()) {
      sr.add(new SpectraRef().msRun(
              rawDataFileToAssay.get(feature.getRawDataFile()).getMsRunRef().getFirst())
          .reference("index=" + scan.getScanNumber()));
    }
    if (sr.isEmpty()) {
      sr.add(new SpectraRef().msRun(
              rawDataFileToAssay.get(feature.getRawDataFile()).getMsRunRef().getFirst())
          .reference("index=0"));
    }
    return sr;
  }

  private static boolean isMatchesType(DataType dataType) {
    return (CompoundAnnotationUtils.isAnnotationOrMissingType(dataType));
  }

  private static List<? extends ListWithSubsType<?>> filterForTypesWithAnnotation(
      Collection<DataType> types) {
    return types.stream().filter(Objects::nonNull)
        .filter(CompoundAnnotationUtils::isAnnotationOrMissingType)
        .filter(t -> t instanceof ListWithSubsType).map(t -> (ListWithSubsType<?>) t).toList();
  }

  @NotNull
  private static Metadata generateMetadata(FeatureList featureList) {
    Metadata mtd = new Metadata();
    mtd.setMzTabVersion("2.0.0-M");
    mtd.setMzTabID("1");
    mtd.setDescription(featureList.getName());
    mtd.addSoftwareItem(new Software().id(1).parameter(
        new Parameter().cvLabel("MS").cvAccession("MS:1002342").name("MZmine")
            .value(String.valueOf(SemverVersionReader.getMZmineVersion()))));
    mtd.setSmallMoleculeQuantificationUnit(
        new Parameter().cvLabel("PRIDE").cvAccession("PRIDE:0000330")
            .name("Arbitrary quantification unit"));
    mtd.setSmallMoleculeFeatureQuantificationUnit(
        new Parameter().cvLabel("PRIDE").cvAccession("PRIDE:0000330")
            .name("Arbitrary quantification unit"));
    mtd.addIdConfidenceMeasureItem(
        new Parameter().id(1).cvLabel("MS").cvAccession("MS_1003303").name("spectral similarity"));
    mtd.setSmallMoleculeIdentificationReliability(
        new Parameter().cvLabel("MS").cvAccession("MS:1002896")
            .name("compound identification confidence level"));
    mtd.setQuantificationMethod(new Parameter().cvLabel("MS").cvAccession("MS:1001834")
        .name("LC-MS label-free quantification analysis"));
    mtd.addCvItem(
        new CV().id(1).label("MS").fullName("PSI-MS controlled vocabulary").version("4.1.108")
            .uri("https://raw.githubusercontent.com/HUPO-PSI/psi-ms-CV/master/psi-ms.obo"));
    mtd.addCvItem(new CV().id(2).label("MS")
        .fullName("PRIDE PRoteomics IDEntifications (PRIDE) database controlled vocabulary")
        .version("17.11.2022").uri("http://purl.obolibrary.org/obo/pride_cv.obo"));
    mtd.addDatabaseItem(
        new Database().id(1).prefix("mzmdb").version(SemverVersionReader.getMZmineVersion().toString())
            .uri("https://mzmine.github.io/").param(new Parameter().name("MZmine database")));
    return mtd;
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
    return "Exporting feature list(s) " + this.featureList.getName() + " to mzTab-m file(s)";
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    boolean substitute = fileName.getPath().contains(plNamePattern);

    //Optional Columns
    List<IOptColumnMappingBuilder> feature_mzList = new ArrayList<>();
    List<IOptColumnMappingBuilder> feature_rtList = new ArrayList<>();
    List<IOptColumnMappingBuilder> feature_heightList = new ArrayList<>();

    File curFile = fileName;

    try {
      //Filename cleanup
      if (substitute) {
        // Cleanup from illegal filename characters
        //not small alphabets, large alphabets, numbers, dots or dashes
        String newFilename = generateNewFilename(featureList);
        curFile = new File(newFilename);
      }

      MzTab mzTabFile = new MzTab();

      //Set up US decimal number formatting
      NumberFormat formatter;
      formatter = new DecimalFormat("#.######");

      //Metadata
      mtd = generateMetadata(featureList);

      final RawDataFile[] rawDataFiles = featureList.getRawDataFiles().toArray(RawDataFile[]::new);
      int fileCounter = 0;

      // Study Variable name and descriptions
      HashMap<String, List<RawDataFile>> svhash = new HashMap<>();
      HashMap<RawDataFile, Assay> rawDataFileToAssay = new HashMap<>();

      for (RawDataFile file : rawDataFiles) {
        fileCounter++;

        // MS run location
        MsRun msRun = getMsRunData(fileCounter, file);
        mtd.addMsRunItem(msRun);

        // Add Assay
        Assay assay = new Assay();
        rawDataFileToAssay.put(file, assay);
        assay.id(fileCounter);
        assay.addMsRunRefItem(msRun);

        mtd.addAssayItem(assay);

        feature_mzList.add(OptColumnMappingBuilder.forIndexedElement(assay).withName("feature_mz"));
        feature_rtList.add(OptColumnMappingBuilder.forIndexedElement(assay).withName("feature_rt"));
        feature_heightList.add(
            OptColumnMappingBuilder.forIndexedElement(assay).withName("feature_height"));

        createSVHash(svhash, file);
      }

      getStudyVariables(svhash, rawDataFileToAssay);

      //Write data rows
      Map<Parameter, Database> databases = new LinkedHashMap<>();

      Map<FeatureListRow, DataType<?>> annotationPriorityMap = CompoundAnnotationUtils.mapBestAnnotationTypesByPriority(
          featureList.getRows(), false);

      for (int i = 0; i < featureList.getRows().size(); ++i) {
        FeatureListRow featureListRow = featureList.getRows().get(i);

        DataType<?> annotationType = annotationPriorityMap.get(featureListRow);

        sm = new SmallMoleculeSummary();
        sm.setSmlId(i + 1);
        smf = new SmallMoleculeFeature();
        smf.setSmfId(i + 1);
        SmallMoleculeEvidence sme = new SmallMoleculeEvidence();
        sme.setSmeId(i + 1);

        sme.setMsLevel(new Parameter().cvLabel("MS").cvAccession("MS:1000511").name("ms level")
            .value(String.valueOf(
                featureListRow.getBestFeature().getRepresentativeScan().getMSLevel())));
        sme.setEvidenceInputId(String.valueOf(i + 1));
        sme.setRank(1);

        final Collection<DataType> dataTypes = featureListRow.getTypes();

        this.globalOptColumns = new ArrayList<>();

        //introduce column count for optional column placement
        this.columnCount = 0;

        //Get available annotations
        if (exportAll || !dataTypes.isEmpty()) {

          List<? extends ListWithSubsType<?>> listType = filterForTypesWithAnnotation(dataTypes);

          for (ListWithSubsType<?> type : listType) {

            // get the actual value of the ListWithSubsType stored in the feature list
            final List<?> featureAnnotationList = featureListRow.get(type);
            setDefaultConfidences(sme);

            if (featureAnnotationList == null || featureAnnotationList.isEmpty()) {
              sm.setReliability("4"); //unknown compound
              continue;
            } else {
              sm.setReliability("2"); //putatively annotated compound (2)
            }

            // export annotations
            exportAnnotations(type, featureAnnotationList, sme, annotationType);
          }

          Double rowMZ = featureListRow.getAverageMZ();
          Float rowRT = featureListRow.getAverageRT();
          Range<Float> rowRTRange = featureListRow.get(RTRangeType.class);
          Integer rowCharge = featureListRow.getRowCharge().intValue();

          if (rowMZ != null) {
            smf.setExpMassToCharge(rowMZ.doubleValue());
            sme.setExpMassToCharge(rowMZ.doubleValue());
          }
          if (rowRT != null) {
            smf.setRetentionTimeInSeconds(rowRT.doubleValue() * 60f);
          }
          if (rowRTRange != null) {
            smf.setRetentionTimeInSecondsStart(rowRTRange.lowerEndpoint().doubleValue() * 60f);
            smf.setRetentionTimeInSecondsEnd(rowRTRange.upperEndpoint().doubleValue() * 60f);
          }

          assignMissingMandatoryFields(sme);

          int dataFileCount = 0;
          HashMap<String, List<Double>> sampleVariableAbundancehash = new HashMap<>();
          for (RawDataFile dataFile : rawDataFiles) {
            dataFileCount++;
            Feature feature = featureListRow.getFeature(dataFile);
            if (feature != null) {
              //Spectra ref
              //modify if better alternative representation of spectra ref is suggested
              List<SpectraRef> sr = generateSpectraRef(feature, rawDataFileToAssay);
              sme.setSpectraRef(sr);

              String featureMZ = formatter.format(feature.getMZ());
              String featureRT = formatter.format(feature.getRT() * 60f);
              String featureHeight = formatter.format(feature.getHeight());
              Double featureArea = feature.getArea().doubleValue();

              sm.addOptItem(feature_mzList.get(dataFileCount - 1).build(featureMZ));
              sm.addOptItem(feature_rtList.get(dataFileCount - 1).build(featureRT));
              sm.addOptItem(feature_heightList.get(dataFileCount - 1)
                  .build(featureHeight.formatted(formatter)));

              Integer featureCharge = feature.getCharge();
              if (smf.getCharge() == null) {
                if (featureCharge != 0) {
                  smf.setCharge(featureCharge);
                } else {
                  smf.setCharge(DEFAULT_INTEGER_VALUE);
                  if (sme.getCharge() == null) {
                    sme.setCharge(DEFAULT_INTEGER_VALUE);
                  }
                }
              }

              sm.addAbundanceAssayItem(featureArea.doubleValue());
              smf.addAbundanceAssayItem(featureArea.doubleValue());
              for (String sampleVariable : svhash.keySet()) {
                if (svhash.get(sampleVariable).contains(dataFile)) {
                  if (sampleVariableAbundancehash.containsKey(sampleVariable)) {
                    sampleVariableAbundancehash.get(sampleVariable).add(featureArea);
                  } else {
                    List<Double> l = new ArrayList<>();
                    l.add(featureArea);
                    sampleVariableAbundancehash.put(sampleVariable, l);
                  }
                }
              }
            }
          }
          if (rowCharge > 0) {
            smf.setCharge(rowCharge);
            sme.setCharge(rowCharge);
          }
          if (!sampleVariableAbundancehash.keySet().isEmpty()) {
            for (String studyVariable : sampleVariableAbundancehash.keySet()) {
              addSVAbundance(sampleVariableAbundancehash, studyVariable);
            }
          } else {
            sm.addAbundanceStudyVariableItem(null);
            sm.addAbundanceVariationStudyVariableItem(null);
          }

          sm.addSmfIdRefsItem(smf.getSmfId());
          smf.addSmeIdRefsItem(sme.getSmeId());
          mzTabFile.addSmallMoleculeSummaryItem(sm);
          mzTabFile.addSmallMoleculeFeatureItem(smf);
          mzTabFile.addSmallMoleculeEvidenceItem(sme);
        }
      }

      //cv term
      int dbId = 1;

      //set ids sequentially, starting from 1
      for (Entry<Parameter, Database> entry : databases.entrySet()) {
        mtd.addDatabaseItem(entry.getValue().id(dbId++));
      }

      //add nulls in empty annotation cells
      ArrayList<String> optColNames = new ArrayList<>();
      for (SmallMoleculeEvidence sme : mzTabFile.getSmallMoleculeEvidence().stream().toList()) {
        if (sme != null & sme.getOpt() != null) {
          for (int i = 0; i < sme.getOpt().size(); i++) {
            OptColumnMapping entry = sme.getOpt().get(i);
            if (!optColNames.contains(entry.getIdentifier())) {
              optColNames.add(entry.getIdentifier());
            }
          }
        }
      }

      for (SmallMoleculeEvidence sme : mzTabFile.getSmallMoleculeEvidence().stream().toList()) {
        if (sme != null & sme.getOpt() != null) {
          List<String> existingCols = sme.getOpt().stream().map(OptColumnMapping::getIdentifier)
              .toList();
          for (String colName : optColNames) {
            if (!existingCols.contains(colName)) {
              sme.addOptItem(
                  OptColumnMappingBuilder.forGlobal().withName(colName.split("_global_")[1])
                      .build("null"));
            }
          }
        } else {
          for (String colName : optColNames) {
            sme.addOptItem(
                OptColumnMappingBuilder.forGlobal().withName(colName.split("_global_")[1])
                    .build("null"));
          }
        }
      }

      mzTabFile.metadata(mtd);
      MzTabValidatingWriter validatingWriter = new MzTabValidatingWriter();
      validatingWriter.write(curFile.toPath(), mzTabFile);
    } catch (Exception e) {
      e.printStackTrace();
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not export feature list to file " + curFile + ": " + e.getMessage());
      return;
    }
    if (getStatus() == TaskStatus.PROCESSING) {
      setStatus(TaskStatus.FINISHED);
    }
  }

  private void assignMissingMandatoryFields(SmallMoleculeEvidence sme) {
    if (sme.getIdentificationMethod() == null) {
      Parameter identificationMethod = new Parameter();
      identificationMethod.setName("Identification method");
      identificationMethod.setValue("Identification method");
      sme.setIdentificationMethod(identificationMethod);
    }

    if (sme.getTheoreticalMassToCharge() == null) {
      sme.setTheoreticalMassToCharge(DEFAULT_DOUBLE_VALUE);
    }
    //check if there is best id confidence measure present
    if (sm.getBestIdConfidenceMeasure() == null) {
      Parameter parameter = new Parameter();
      parameter.setName("NaN");
      sm.setBestIdConfidenceMeasure(parameter);
      sm.setBestIdConfidenceValue(DEFAULT_DOUBLE_VALUE);
    }
  }

  private void exportAnnotations(ListWithSubsType listWithSubsType, List annotationList,
      SmallMoleculeEvidence sme, DataType<?> annotationType) {

    //get the list of subTypes
    final List<DataType> subDataTypeList = listWithSubsType.getSubDataTypes();

    for (DataType subType : subDataTypeList) {
      final String uniqueID = subType.getUniqueID();

      for (int j = 0; j < annotationList.size(); j++) {
        if (subType instanceof NoTextColumn || uniqueID.equals("structure_2d")) {
          continue;
        }

        Object mappedVal = listWithSubsType.map(subType, annotationList.get(j));

        if (mappedVal != null) {

          modifyReliabilityForLipidMatch(mappedVal);
          String subtypeValue = subType.getFormattedExportString(mappedVal);

          String colName = STR."\{listWithSubsType.getUniqueID()}_\{uniqueID}_\{j}1";
          createSMEOptCols(sme, listWithSubsType, colName, subtypeValue);

          if (listWithSubsType.getUniqueID().equals(annotationType.getUniqueID())) {
            updateWithPreferredAnnotation(sme, annotationType.getUniqueID(), uniqueID,
                subtypeValue);
          }
          columnCount++;
        }
      }
    }
  }

  private void modifyReliabilityForLipidMatch(Object mappedVal) {
    if (mappedVal instanceof MatchedLipid) {
      MatchedLipid mappedLipid = (MatchedLipid) mappedVal;
      if (mappedLipid.getLipidAnnotation() instanceof MolecularSpeciesLevelAnnotation
          & mappedLipid.getLipidAnnotation().getLipidAnnotationLevel()
          .equals(LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL)) {
        sm.setReliability("3"); // only class identified
      }
    }
  }

  private void createSMEOptCols(SmallMoleculeEvidence sme, DataType type, String name,
      String value) {
    globalOptColumns.add(OptColumnMappingBuilder.forGlobal().withName(name));
    if (!value.equals("")) {
      sme.addOptItem(globalOptColumns.get(columnCount).build(value));
    } else {
      sme.addOptItem(globalOptColumns.get(columnCount).build("null"));
    }
  }

  private void updateWithPreferredAnnotation(SmallMoleculeEvidence sme, String preferredAnnotation,
      String uniqueID, String subtypeValue) {
    sme.setDatabaseIdentifier(preferredAnnotation);
    String returnValue;
    if (subtypeValue.equals("")) {
      returnValue = "null";
    } else {
      returnValue = subtypeValue;
    }
    switch(returnValue) {
      case "mol_formula":
        sme.setChemicalFormula(returnValue);
      case "adduct":
        sme.setAdductIon(returnValue);
        sme.setCharge(getChargeParameterFromAdduct(returnValue));
      case "compound_name":
        sme.setChemicalName(returnValue);
      case "smiles":
        sme.setSmiles(returnValue);
      case "precursor_mz":
        sme.setTheoreticalMassToCharge(DEFAULT_DOUBLE_VALUE);
    }

    if (uniqueID.contains("score") & !uniqueID.contains("isotope") & returnValue != null) {
      Parameter parameter = new Parameter();
      parameter.setName(uniqueID);
      sm.setBestIdConfidenceMeasure(parameter);
      sm.setBestIdConfidenceValue(Double.valueOf(returnValue));

      //at the moment, only a single confidence value is added
      //can be modified based on "matches" data types if needed
      List<Double> confidences = new ArrayList<>();
      confidences.add(Double.valueOf(returnValue));
      sme.setIdConfidenceMeasure(confidences);
    }
  }

  private void setDefaultConfidences(SmallMoleculeEvidence sme) {
    List<Double> confidences = new ArrayList<>();
    confidences.add(DEFAULT_DOUBLE_VALUE);
    setConfidences(sme, confidences);
  }

  private void setConfidences(SmallMoleculeEvidence sme, List<Double> confidences) {
    sme.setIdConfidenceMeasure(confidences);
  }

  private void addSVAbundance(HashMap<String, List<Double>> sampleVariableAbundancehash,
      String studyVariable) {
    //Using mean as average function for abundance of Study Variable
    Double sumSV = sampleVariableAbundancehash.get(studyVariable).stream()
        .collect(Collectors.summingDouble(Double::doubleValue));

    int totalSV = sampleVariableAbundancehash.get(studyVariable).size();
    final Double averageSV = totalSV == 0 ? 0.0 : sumSV / totalSV;

    //Coefficient of variation
    Double covSV = sampleVariableAbundancehash.get(studyVariable).stream()
        .map(d -> (d - averageSV) * (d - averageSV))
        .collect(Collectors.summingDouble(Double::doubleValue));

    covSV = (totalSV == 0 || totalSV == 1) ? 0.0 : Math.sqrt(covSV / (totalSV - 1));

    if (averageSV != 0.0) {
      covSV = (covSV / averageSV) * 100.0;
    }

    sm.addAbundanceStudyVariableItem(averageSV);
    sm.addAbundanceVariationStudyVariableItem(covSV);
  }

  private void getStudyVariables(HashMap<String, List<RawDataFile>> svhash,
      HashMap<RawDataFile, Assay> rawDataFileToAssay) {
    int studyVarCount = 0;
    if (svhash.keySet().size() == 0) {
      StudyVariable studyVariable = new StudyVariable().id(1).name("undefined");
      studyVariable.setAssayRefs(mtd.getAssay());
      mtd.addStudyVariableItem(studyVariable);
    }
    for (String key : svhash.keySet()) {
      studyVarCount++;
      StudyVariable studyVariable = new StudyVariable().id(studyVarCount).name(key).description(key)
          .averageFunction(new Parameter().cvLabel("MS").cvAccession("MS:1002883").name("mean"));
      for (RawDataFile file : svhash.get(key)) {
        studyVariable = studyVariable.addAssayRefsItem(rawDataFileToAssay.get(file));
      }
      mtd.addStudyVariableItem(studyVariable);
    }
  }

  private void createSVHash(HashMap<String, List<RawDataFile>> svhash, RawDataFile file) {
    for (UserParameter<?, ?> p : project.getParameters()) {
      if (p.getName().contains("study variable")) {
        if (svhash.containsKey(String.valueOf(project.getParameterValue(p, file)))) {
          svhash.get(String.valueOf(project.getParameterValue(p, file))).add(file);
        } else {
          List<RawDataFile> l = new ArrayList<>();
          l.add(file);
          svhash.put(String.valueOf(project.getParameterValue(p, file)), l);
        }
        break;
      }
    }
  }

  @Nullable
  private MsRun getMsRunData(int fileCounter, RawDataFile file) {
    MsRun msRun = new MsRun();
    msRun.id(fileCounter);
    msRun.setLocation("file:///" + file.getAbsolutePath().replaceAll("\\\\", "/"));
    int dotIn = file.getName().indexOf(".");
    String fileFormat = "";
    if (dotIn != -1) {
      fileFormat = file.getName().substring(dotIn + 1);
    }
    msRun.setFormat(
        new Parameter().cvLabel("MS").cvAccession("MS:1000584").name(fileFormat + " file"));
    msRun.setIdFormat(new Parameter().cvLabel("MS").cvAccession("MS:1000774")
        .name("multiple peak list nativeID format"));

    List<Parameter> polPara = getPolarityParameters(file);

    if (polPara == null) {
      return null;
    }

    msRun.setScanPolarity(polPara);
    return msRun;
  }

  @Nullable
  private List<Parameter> getPolarityParameters(RawDataFile file) {
    List<Parameter> polPara = new ArrayList<>();
    for (PolarityType scanPol : file.getDataPolarity()) {
      String polarity = "";
      String polCVA = "";
      if (scanPol.equals(PolarityType.POSITIVE)) {
        polarity = "positive scan";
        polCVA = "MS:1000130";
      } else if (scanPol.equals(PolarityType.NEGATIVE)) {
        polarity = "negative scan";
        polCVA = "MS:1000129";
      } else {
        setStatus(TaskStatus.ERROR);
        setErrorMessage(
            STR."Invalid scan polarity \{polPara} encountered for file \{file.getName()}.");
        return null;
      }
      Parameter p = new Parameter().cvLabel("MS").cvAccession(polCVA).name(polarity);
      polPara.add(p);
    }
    return polPara;
  }

  private Integer getChargeParameterFromAdduct(String adduct) {

    Pattern pattern = Pattern.compile("(\\])(\\+)([0-9]?)");
    Matcher matcher = pattern.matcher(adduct);

    if (matcher.find()) {
      String match = matcher.group(3);

      if (!match.equals("")) {
        return Integer.parseInt(match); //return absolute value
      } else if (matcher.group(2).equals("+") | matcher.group(2).equals("-")) {
        return 1;
      }
    }

    return DEFAULT_INTEGER_VALUE;
  }

  @NotNull
  private String generateNewFilename(FeatureList featureList) {
    String cleanPlName = featureList.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
    // Substitute
    String newFilename = fileName.getPath().replaceAll(Pattern.quote(plNamePattern), cleanPlName);
    return newFilename;
  }
}
