/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.dataprocessing.align_join.JoinAlignerModule;
import io.github.mzmine.modules.dataprocessing.align_join.JoinAlignerParameters;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogramBuilderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvingDimension;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.centroid.CentroidMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.centroid.CentroidMassDetectorParameters;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingModule;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingParameters;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.savitzkygolay.SavitzkyGolayParameters;
import io.github.mzmine.modules.dataprocessing.filter_groupms2.GroupMS2SubParameters;
import io.github.mzmine.modules.dataprocessing.filter_isotopegrouper.IsotopeGrouperModule;
import io.github.mzmine.modules.dataprocessing.filter_isotopegrouper.IsotopeGrouperParameters;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import java.io.File;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * {@link Lifecycle#PER_CLASS} creates only one test instance of this class and executes everything
 * in sequence. As we are using data import, chromatogram building, ... Only with this option the
 * init (@BeforeAll) and tearDown method are not static.
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
@DisplayName("Test Feature Finding")
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
//@Disabled
public class FeatureFindingTest {

  private static final Logger logger = Logger.getLogger(FeatureFindingTest.class.getName());
  private static MZmineProject project;
  private final String sample1 = "DOM_a.mzML";
  private final String sample2 = "DOM_b.mzXML";
  private final String chromSuffix = "chrom";
  private final String smoothSuffix = "smooth";
  private final String deconSuffix = "decon";
  private final String deisotopeSuffix = "deiso";
  private final String featureFilterSuffix = "ffilter";
  private final String rowFilterSuffix = "rowFilter";
  private final String alignedName = "aligned";
  private final String gapFilledSuffix = "gap";
  private ModularFeatureList lastFlistA = null;
  private ModularFeatureList lastFlistB = null;

  /**
   * Init MZmine core in headless mode with the options -r (keep running) and -m (keep in memory)
   */
  @BeforeAll
  public void init() {
    //    logger.info("Running MZmine");
    //    MZmineCore.main(new String[]{"-r", "-m", "all"});
    logger.info("Getting project");
    project = MZmineCore.getProjectManager().getCurrentProject();
  }

  @AfterAll
  public void tearDown() {
    // we need to clean the project after this integration test
    MZmineTestUtil.cleanProject();
  }


  @Test
  @Order(1)
  @DisplayName("Test advanced data import of mzML and mzXML with mass detection")
  void dataImportTest() throws InterruptedException {
    File[] files = new File[]{new File(
        FeatureFindingTest.class.getClassLoader().getResource("rawdatafiles/DOM_a.mzML").getFile()),
        new File(FeatureFindingTest.class.getClassLoader().getResource("rawdatafiles/DOM_b.mzXML")
            .getFile())};

    AllSpectralDataImportParameters paramDataImport = new AllSpectralDataImportParameters();
    paramDataImport.setParameter(AllSpectralDataImportParameters.fileNames, files);
    paramDataImport.setParameter(SpectralLibraryImportParameters.dataBaseFiles, new File[0]);
    paramDataImport.setParameter(AllSpectralDataImportParameters.advancedImport, true);
    AdvancedSpectraImportParameters advancedImport = paramDataImport.getParameter(
        AllSpectralDataImportParameters.advancedImport).getEmbeddedParameters();
    advancedImport.setParameter(AdvancedSpectraImportParameters.msMassDetection, true);
    advancedImport.setParameter(AdvancedSpectraImportParameters.ms2MassDetection, true);
    // create centroid mass detectors
    advancedImport.getParameter(AdvancedSpectraImportParameters.msMassDetection)
        .getEmbeddedParameter().setValue(createCentroidMassDetector(1E5));
    advancedImport.getParameter(AdvancedSpectraImportParameters.ms2MassDetection)
        .getEmbeddedParameter().setValue(createCentroidMassDetector(0));

    logger.info("Testing advanced data import of mzML and mzXML with direct mass detection");
    TaskResult finished = MZmineTestUtil.callModuleWithTimeout(30,
        AllSpectralDataImportModule.class, paramDataImport);

    // should have finished by now
    assertEquals(TaskResult.FINISHED, finished, () -> switch (finished) {
      case TIMEOUT -> "Timeout during data import. Not finished in time.";
      case ERROR -> "Error during data import.";
      case FINISHED -> "";
    });

    assertEquals(2, project.getDataFiles().length);
    int filesTested = 0;
    for (RawDataFile raw : project.getCurrentRawDataFiles()) {
      // check all scans and mass lists
      for (Scan scan : raw.getScans()) {
        assertNotNull(scan);
        assertNotNull(scan.getMassList());
      }
      switch (raw.getName()) {
        case sample1 -> {
          // num of scans, ms1, ms2
          assertEquals(521, raw.getNumOfScans());
          assertEquals(87, raw.getScanNumbers(1).size());
          assertEquals(434, raw.getScanNumbers(2).size());
          // number of data points
          assertEquals(2400, raw.getMaxRawDataPoints());
          assertEquals(2400, raw.getMaxCentroidDataPoints());
          // check two scans
          Scan scan = raw.getScan(0);
          assertEquals(1, scan.getMSLevel());
          assertNotEquals(MassSpectrumType.PROFILE, scan.getSpectrumType()); // not
          assertEquals(794, scan.getBasePeakIndex());
          assertEquals(PolarityType.POSITIVE, scan.getPolarity());
          // MS2 scan
          Scan scanMS2 = raw.getScan(1);
          assertEquals(2, scanMS2.getMSLevel());
          assertNotEquals(MassSpectrumType.PROFILE, scanMS2.getSpectrumType()); // not
          assertEquals(221, scanMS2.getBasePeakIndex());
          assertEquals(PolarityType.POSITIVE, scanMS2.getPolarity());
          filesTested++;
        }
        case sample2 -> {
          // num of scans, ms1, ms2
          assertEquals(521, raw.getNumOfScans());
          assertEquals(87, raw.getScanNumbers(1).size());
          assertEquals(434, raw.getScanNumbers(2).size());
          // number of data points
          assertEquals(2410, raw.getMaxRawDataPoints());
          assertEquals(2410, raw.getMaxCentroidDataPoints());
          // check two scans
          Scan scan = raw.getScan(1);
          assertEquals(1, scan.getMSLevel());
          assertNotEquals(MassSpectrumType.PROFILE, scan.getSpectrumType()); // not
          assertEquals(203, scan.getBasePeakIndex());
          assertEquals(PolarityType.POSITIVE, scan.getPolarity());
          // MS2 scan
          Scan scanMS2 = raw.getScan(2);
          assertEquals(2, scanMS2.getMSLevel());
          assertNotEquals(MassSpectrumType.PROFILE, scanMS2.getSpectrumType()); // not
          assertEquals(239, scanMS2.getBasePeakIndex());
          assertEquals(PolarityType.POSITIVE, scanMS2.getPolarity());
          filesTested++;
        }
      }
    }
    // both files tested
    assertEquals(2, filesTested);
  }

  @Test
  @Order(2)
  @DisplayName("Test ADAP chromatogram builder")
  void chromatogramBuilderTest() throws InterruptedException {

    ADAPChromatogramBuilderParameters paramChrom = new ADAPChromatogramBuilderParameters();
    paramChrom.getParameter(ADAPChromatogramBuilderParameters.dataFiles)
        .setValue(RawDataFilesSelectionType.ALL_FILES);
    paramChrom.setParameter(ADAPChromatogramBuilderParameters.scanSelection, new ScanSelection(1));
    paramChrom.setParameter(ADAPChromatogramBuilderParameters.minimumScanSpan, 4);
    paramChrom.setParameter(ADAPChromatogramBuilderParameters.mzTolerance,
        new MZTolerance(0.002, 10));
    paramChrom.setParameter(ADAPChromatogramBuilderParameters.minHighestPoint, 3E5);
    paramChrom.setParameter(ADAPChromatogramBuilderParameters.minGroupIntensity, 1E5);
    paramChrom.setParameter(ADAPChromatogramBuilderParameters.suffix, chromSuffix);

    logger.info("Testing ADAPChromatogramBuilder");
    TaskResult finished = MZmineTestUtil.callModuleWithTimeout(30,
        ModularADAPChromatogramBuilderModule.class, paramChrom);

    // should have finished by now
    assertEquals(TaskResult.FINISHED, finished, () -> switch (finished) {
      case TIMEOUT -> "Timeout during chromatogram builder. Not finished in time.";
      case ERROR -> "Error during chromatogram builder.";
      case FINISHED -> "";
    });

    assertEquals(project.getCurrentFeatureLists().size(), 2);
    // test feature lists
    int filesTested = 0;
    for (FeatureList flist : project.getCurrentFeatureLists()) {
      assertEquals(1, flist.getNumberOfRawDataFiles());
      assertEquals(2, flist.getAppliedMethods().size());

      // check default sorting of rows
      assertTrue(MZmineTestUtil.isSorted(flist));

      if (equalsFeatureListName(flist, sample1, chromSuffix)) {
        assertEquals(974, flist.getNumberOfRows());
        // check number of chromatogram scans (equals MS1 scans)
        assertEquals(87, flist.getSeletedScans(flist.getRawDataFile(0)).size());

        // check random row
        FeatureListRow row = flist.getRow(100);
        assertEquals(flist, row.getFeatureList());
        assertEquals(101, row.getID());
        assertTrue(row.getAverageMZ() > 430.2075);
        assertTrue(row.getAverageRT() > 7.26);
        assertTrue(row.getAverageRT() < 7.27);
        assertTrue(row.getAverageHeight() > 586139);
        assertTrue(row.getAverageArea() > 160966);

        IonTimeSeries<? extends Scan> data = row.getFeatures().get(0).getFeatureData();
        assertEquals(44, data.getNumberOfValues());
        assertEquals(44, data.getSpectra().size());

        filesTested++;
      } else if (equalsFeatureListName(flist, sample2, chromSuffix)) {
        assertEquals(1027, flist.getNumberOfRows());
        // check number of chromatogram scans (equals MS1 scans)
        assertEquals(87, flist.getSeletedScans(flist.getRawDataFile(0)).size());

        filesTested++;
      }
    }
    // both files tested
    assertEquals(2, filesTested);

    lastFlistA = (ModularFeatureList) project.getFeatureList(getName(sample1, chromSuffix));
    lastFlistB = (ModularFeatureList) project.getFeatureList(getName(sample2, chromSuffix));
  }

  @Test
  @Order(3)
  @DisplayName("Test chromatogram Smoothing")
  void chromatogramSmootherTest() throws InterruptedException {
    double maxRelAreaChange = 0.25;

    assertNotNull(lastFlistA);
    assertNotNull(lastFlistB);

    ParameterSet sgParam = new SavitzkyGolayParameters().cloneParameterSet();
    sgParam.setParameter(SavitzkyGolayParameters.mobilitySmoothing, false);
    sgParam.getParameter(SavitzkyGolayParameters.rtSmoothing).setValue(true);
    sgParam.getParameter(SavitzkyGolayParameters.rtSmoothing).getEmbeddedParameter().setValue(5);

    SmoothingParameters paramSmooth = new SmoothingParameters();
    paramSmooth.getParameter(SmoothingParameters.featureLists)
        .setValue(new FeatureListsSelection(lastFlistA, lastFlistB));
    paramSmooth.setParameter(SmoothingParameters.handleOriginal, OriginalFeatureListOption.KEEP);
    paramSmooth.setParameter(SmoothingParameters.smoothingAlgorithm,
        new MZmineProcessingStepImpl<>(SmoothingParameters.sgSmoothing, sgParam));
    paramSmooth.setParameter(SmoothingParameters.suffix, smoothSuffix);

    logger.info("Testing chromatogram smoothing (RT, 5 dp)");
    TaskResult finished = MZmineTestUtil.callModuleWithTimeout(30, SmoothingModule.class,
        paramSmooth);

    // should have finished by now
    assertEquals(TaskResult.FINISHED, finished, () -> switch (finished) {
      case TIMEOUT -> "Timeout during chromatogram smoothing. Not finished in time.";
      case ERROR -> "Error during chromatogram smoothing.";
      case FINISHED -> "";
    });

    assertEquals(4, project.getCurrentFeatureLists().size());
    // test feature lists
    ModularFeatureList processed1 = (ModularFeatureList) project.getFeatureList(
        getName(sample1, chromSuffix, smoothSuffix));
    ModularFeatureList processed2 = (ModularFeatureList) project.getFeatureList(
        getName(sample2, chromSuffix, smoothSuffix));

    // already save feature lists to last for next steps
    ModularFeatureList lastFlistA = this.lastFlistA;
    ModularFeatureList lastFlistB = this.lastFlistB;
    this.lastFlistA = processed1;
    this.lastFlistB = processed2;

    //test
    assertNotNull(processed1);
    assertNotNull(processed2);

    // same size
    assertEquals(lastFlistA.getNumberOfRows(), processed1.getNumberOfRows());
    assertEquals(lastFlistB.getNumberOfRows(), processed2.getNumberOfRows());
    assertEquals(lastFlistA.getAppliedMethods().size() + 1, processed1.getAppliedMethods().size());
    assertEquals(lastFlistB.getAppliedMethods().size() + 1, processed2.getAppliedMethods().size());

    for (int i = 0; i < lastFlistA.getNumberOfRows(); i++) {
      // same order and number of data points after smoothing
      FeatureListRow a = lastFlistA.getRow(i);
      FeatureListRow b = processed1.getRow(i);
      Feature fa = a.getFeatures().get(0);
      Feature fb = b.getFeatures().get(0);
      assertEquals(fa.getNumberOfDataPoints(), fb.getNumberOfDataPoints());
      assertEquals(fa.getScanNumbers().size(), fb.getScanNumbers().size());
      // retention time might actually resonably change by a lot - therefore no test
      assertEquals(a.getAverageMZ(), b.getAverageMZ(), 0.005, "mz change to high");

      // area change is greater than 25 % for some features
      //      assertTrue(Precision.equals(a.getAverageArea(), b.getAverageArea(), 0, maxRelAreaChange),
      //          () -> MessageFormat.format(
      //              "area change is too high (more then {4}) for IDs: {0} and {1} with areas: {2}, {3}",
      //              a.getID(), b.getID(), a.getAverageArea(), b.getAverageArea(), maxRelAreaChange));
    }

    for (int i = 0; i < lastFlistB.getNumberOfRows(); i++) {
      // same order and number of data points after smoothing
      FeatureListRow a = lastFlistB.getRow(i);
      FeatureListRow b = processed2.getRow(i);
      Feature fa = a.getFeatures().get(0);
      Feature fb = b.getFeatures().get(0);
      assertEquals(fa.getNumberOfDataPoints(), fb.getNumberOfDataPoints());
      assertEquals(fa.getScanNumbers().size(), fb.getScanNumbers().size());
      // retention time might actually resonably change by a lot - therefore no test
      assertEquals(a.getAverageMZ(), b.getAverageMZ(), 0.005, "mz change to high");

      // area change is greater than 25 % for some features
      //      assertTrue(Precision.equals(a.getAverageArea(), b.getAverageArea(), 0, maxRelAreaChange),
      //          () -> MessageFormat.format(
      //              "area change is too high (more then {4}) for IDs: {0} and {1} with areas: {2}, {3}",
      //              a.getID(), b.getID(), a.getAverageArea(), b.getAverageArea(), maxRelAreaChange));
    }
  }


  @Test
  @Order(4)
  @DisplayName("Test local minimum feature resolver")
  void chromatogramDeconvolutionTest() throws InterruptedException {

    assertNotNull(lastFlistA);
    assertNotNull(lastFlistB);

    MinimumSearchFeatureResolverParameters generalParam = new MinimumSearchFeatureResolverParameters();
    generalParam.getParameter(MinimumSearchFeatureResolverParameters.PEAK_LISTS)
        .setValue(new FeatureListsSelection(lastFlistA, lastFlistB));
    generalParam.setParameter(MinimumSearchFeatureResolverParameters.handleOriginal,
        OriginalFeatureListOption.KEEP);
    generalParam.setParameter(
        MinimumSearchFeatureResolverParameters.CHROMATOGRAPHIC_THRESHOLD_LEVEL, 0.8);
    generalParam.setParameter(MinimumSearchFeatureResolverParameters.dimension,
        ResolvingDimension.RETENTION_TIME);
    generalParam.setParameter(MinimumSearchFeatureResolverParameters.MIN_ABSOLUTE_HEIGHT, 3E5);
    generalParam.setParameter(MinimumSearchFeatureResolverParameters.MIN_NUMBER_OF_DATAPOINTS, 4);
    generalParam.setParameter(MinimumSearchFeatureResolverParameters.MIN_RATIO, 1.8);
    generalParam.setParameter(MinimumSearchFeatureResolverParameters.MIN_RELATIVE_HEIGHT, 0d);
    generalParam.setParameter(MinimumSearchFeatureResolverParameters.PEAK_DURATION,
        Range.closed(0.02, 1d));
    generalParam.setParameter(MinimumSearchFeatureResolverParameters.SEARCH_RT_RANGE, 0.15);
    generalParam.setParameter(MinimumSearchFeatureResolverParameters.SUFFIX, deconSuffix);

    // group ms2
    generalParam.setParameter(MinimumSearchFeatureResolverParameters.groupMS2Parameters, true);
    GroupMS2SubParameters groupMS2SubParameters = generalParam.getParameter(
        MinimumSearchFeatureResolverParameters.groupMS2Parameters).getEmbeddedParameters();
    groupMS2SubParameters.setParameter(GroupMS2SubParameters.limitRTByFeature, false);
    groupMS2SubParameters.setParameter(GroupMS2SubParameters.mzTol, new MZTolerance(0.05, 10));
    groupMS2SubParameters.setParameter(GroupMS2SubParameters.rtTol,
        new RTTolerance(0.15f, Unit.MINUTES));

    logger.info("Testing chromatogram deconvolution");
    TaskResult finished = MZmineTestUtil.callModuleWithTimeout(45,
        MinimumSearchFeatureResolverModule.class, generalParam);

    // should have finished by now
    assertEquals(TaskResult.FINISHED, finished, () -> switch (finished) {
      case TIMEOUT -> "Time out during feature deconvolution. Not finished in time.";
      case ERROR -> "Error during feature deconvolution.";
      case FINISHED -> "";
    });

    logger.info("Lists after deconvolution:  " + project.getCurrentFeatureLists().stream()
        .map(FeatureList::getName).collect(Collectors.joining(", ")));
    assertEquals(6, project.getCurrentFeatureLists().size());
    // test feature lists
    ModularFeatureList processed1 = (ModularFeatureList) project.getFeatureList(
        getName(sample1, chromSuffix, smoothSuffix, deconSuffix));
    ModularFeatureList processed2 = (ModularFeatureList) project.getFeatureList(
        getName(sample2, chromSuffix, smoothSuffix, deconSuffix));

    // already save feature lists to last for next steps
    ModularFeatureList lastFlistA = this.lastFlistA;
    ModularFeatureList lastFlistB = this.lastFlistB;
    this.lastFlistA = processed1;
    this.lastFlistB = processed2;

    //test
    assertNotNull(processed1);
    assertNotNull(processed2);

    // check default sorting of rows
    assertTrue(MZmineTestUtil.isSorted(processed1));
    assertTrue(MZmineTestUtil.isSorted(processed2));

    // methods +1
    assertEquals(lastFlistA.getAppliedMethods().size() + 1, processed1.getAppliedMethods().size());
    assertEquals(lastFlistB.getAppliedMethods().size() + 1, processed2.getAppliedMethods().size());

    assertEquals(127, processed1.getNumberOfRows());
    assertEquals(131, processed2.getNumberOfRows());
  }


  @Test
  @Order(5)
  @DisplayName("Test deisotoper")
  void deisotoperTest() throws InterruptedException {

    assertNotNull(lastFlistA);
    assertNotNull(lastFlistB);

    IsotopeGrouperParameters generalParam = new IsotopeGrouperParameters();
    generalParam.getParameter(IsotopeGrouperParameters.peakLists)
        .setValue(new FeatureListsSelection(lastFlistA, lastFlistB));
    generalParam.setParameter(IsotopeGrouperParameters.handleOriginal,
        OriginalFeatureListOption.KEEP);
    generalParam.setParameter(IsotopeGrouperParameters.maximumCharge, 2);
    generalParam.setParameter(IsotopeGrouperParameters.mobilityTolerace, false);
    generalParam.setParameter(IsotopeGrouperParameters.monotonicShape, true);
    generalParam.setParameter(IsotopeGrouperParameters.mzTolerance, new MZTolerance(0.003, 10));
    generalParam.setParameter(IsotopeGrouperParameters.rtTolerance,
        new RTTolerance(0.1f, Unit.MINUTES));
    generalParam.setParameter(IsotopeGrouperParameters.representativeIsotope,
        IsotopeGrouperParameters.ChooseTopIntensity);
    generalParam.setParameter(IsotopeGrouperParameters.suffix, deisotopeSuffix);

    logger.info("Testing deisotoping");
    TaskResult finished = MZmineTestUtil.callModuleWithTimeout(30, IsotopeGrouperModule.class,
        generalParam);

    // should have finished by now
    assertEquals(TaskResult.FINISHED, finished, () -> switch (finished) {
      case TIMEOUT -> "Timeout during isotope grouper. Not finished in time.";
      case ERROR -> "Error during isotope grouper.";
      case FINISHED -> "";
    });

    assertEquals(8, project.getCurrentFeatureLists().size());
    // test feature lists
    ModularFeatureList processed1 = (ModularFeatureList) project.getFeatureList(
        getName(sample1, chromSuffix, smoothSuffix, deconSuffix, deisotopeSuffix));
    ModularFeatureList processed2 = (ModularFeatureList) project.getFeatureList(
        getName(sample2, chromSuffix, smoothSuffix, deconSuffix, deisotopeSuffix));

    // already save feature lists to last for next steps
    ModularFeatureList lastFlistA = this.lastFlistA;
    ModularFeatureList lastFlistB = this.lastFlistB;
    this.lastFlistA = processed1;
    this.lastFlistB = processed2;

    assertNotNull(processed1);
    assertNotNull(processed2);

    // check default sorting of rows
    assertTrue(MZmineTestUtil.isSorted(processed1));
    assertTrue(MZmineTestUtil.isSorted(processed2));

    // methods +1
    assertEquals(lastFlistA.getAppliedMethods().size() + 1, processed1.getAppliedMethods().size());
    assertEquals(lastFlistB.getAppliedMethods().size() + 1, processed2.getAppliedMethods().size());
    // less feature list rows
    assertTrue(lastFlistA.getNumberOfRows() > processed1.getNumberOfRows());
    assertTrue(lastFlistB.getNumberOfRows() > processed2.getNumberOfRows());

    assertEquals(108, processed1.getNumberOfRows());
    assertEquals(104, processed2.getNumberOfRows());

    // has isotope pattern
    assertNotNull(
        processed1.streamFeatures().map(Feature::getIsotopePattern).filter(Objects::nonNull)
            .findFirst().orElse(null), "No isotope pattern");
    assertNotNull(
        processed2.streamFeatures().map(Feature::getIsotopePattern).filter(Objects::nonNull)
            .findFirst().orElse(null), "No isotope pattern");

    // any with charge
    assertTrue(processed1.streamFeatures().mapToInt(Feature::getCharge).anyMatch(c -> c > 0),
        "No charge detected");
    assertTrue(processed2.streamFeatures().mapToInt(Feature::getCharge).anyMatch(c -> c > 0),
        "No charge detected");
  }


  @Test
  @Order(6)
  @DisplayName("Test join aligner")
  void alignmentTest() throws InterruptedException {

    assertNotNull(lastFlistA);
    assertNotNull(lastFlistB);

    JoinAlignerParameters generalParam = new JoinAlignerParameters();
    generalParam.getParameter(JoinAlignerParameters.peakLists)
        .setValue(new FeatureListsSelection(lastFlistA, lastFlistB));
    generalParam.setParameter(JoinAlignerParameters.compareIsotopePattern, false);
    generalParam.setParameter(JoinAlignerParameters.compareSpectraSimilarity, false);
    generalParam.setParameter(JoinAlignerParameters.mobilityTolerance, false);
    generalParam.setParameter(JoinAlignerParameters.mobilityWeight, 0d);
    generalParam.setParameter(JoinAlignerParameters.MZTolerance, new MZTolerance(0.003, 10));
    generalParam.setParameter(JoinAlignerParameters.MZWeight, 3d);
    generalParam.setParameter(JoinAlignerParameters.RTTolerance,
        new RTTolerance(0.2f, Unit.MINUTES));
    generalParam.setParameter(JoinAlignerParameters.RTWeight, 1d);
    generalParam.setParameter(JoinAlignerParameters.SameChargeRequired, false);
    generalParam.setParameter(JoinAlignerParameters.SameIDRequired, false);
    generalParam.setParameter(JoinAlignerParameters.handleOriginal, OriginalFeatureListOption.KEEP);
    generalParam.setParameter(JoinAlignerParameters.peakListName, alignedName);

    logger.info("Testing join aligner");
    TaskResult finished = MZmineTestUtil.callModuleWithTimeout(30, JoinAlignerModule.class,
        generalParam);

    // should have finished by now
    assertEquals(TaskResult.FINISHED, finished, () -> switch (finished) {
      case TIMEOUT -> "Timeout during feature join aligner. Not finished in time.";
      case ERROR -> "Error during join aligner.";
      case FINISHED -> "";
    });

    assertEquals(9, project.getCurrentFeatureLists().size());
    // test feature lists
    ModularFeatureList processed1 = (ModularFeatureList) project.getFeatureList(
        getName(alignedName));

    // already save feature lists to last for next steps
    ModularFeatureList lastFlistA = this.lastFlistA;
    this.lastFlistA = processed1;

    assertNotNull(processed1);

    // check default sorting of rows
    assertTrue(MZmineTestUtil.isSorted(processed1));

    // 2 raw
    assertEquals(2, processed1.getRawDataFiles().size());

    // methods +1
    assertEquals(lastFlistA.getAppliedMethods().size() + 1, processed1.getAppliedMethods().size());
    // less feature list rows
    assertEquals(155, processed1.getNumberOfRows());

    // at least one row with 2 features
    assertTrue(processed1.stream()
            .anyMatch(row -> row.getFeatures().stream().filter(Objects::nonNull).count() == 2),
        "No row found with 2 features");
  }

  private MZmineProcessingStep<MassDetector> createCentroidMassDetector(double noise) {
    CentroidMassDetector detect = MZmineCore.getModuleInstance(CentroidMassDetector.class);
    CentroidMassDetectorParameters param = new CentroidMassDetectorParameters();
    param.setParameter(CentroidMassDetectorParameters.noiseLevel, noise);
    param.setParameter(CentroidMassDetectorParameters.detectIsotopes, false);
    return new MZmineProcessingStepImpl<>(detect, param);
  }


  private String getName(String sample, String... suffix) {
    StringBuilder s = new StringBuilder(sample);
    for (String suf : suffix) {
      s.append(" ");
      s.append(suf);
    }
    return s.toString();
  }

  private boolean equalsFeatureListName(FeatureList flist, String sample, String... suffix) {
    return flist.getName().equals(getName(sample, suffix));
  }
}
