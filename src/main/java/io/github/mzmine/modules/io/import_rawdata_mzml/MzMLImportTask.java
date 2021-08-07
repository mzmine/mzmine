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

package io.github.mzmine.modules.io.import_rawdata_mzml;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImsMsMsInfo;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.BuildingMobilityScan;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.masslist.ScanPointerMassList;
import io.github.mzmine.datamodel.impl.masslist.SimpleMassList;
import io.github.mzmine.datamodel.MZmineException;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.io.import_rawdata_mzml.data.MzMLParser;
import io.github.mzmine.modules.io.import_rawdata_mzml.data.MzMLRawDataFile;
import io.github.mzmine.modules.io.import_rawdata_mzml.data.MzMLMsScan;
import io.github.mzmine.modules.io.import_rawdata_mzml.util.FileMemoryMapper;
import io.github.mzmine.project.impl.IMSRawDataFileImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExceptionUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javolution.text.CharArray;
import javolution.xml.internal.stream.XMLStreamReaderImpl;
import javolution.xml.stream.XMLStreamConstants;

/**
 * This class reads mzML 1.0 and 1.1.0 files (http://www.psidev.info/index.php?q=node/257) using the
 * jmzml library (http://code.google.com/p/jmzml/).
 */
public class MzMLImportTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final File fileToOpen;
  private InputStream inputStreamToOpen;

  // advanced processing will apply mass detection directly to the scans
  private final boolean applyMassDetection;

  private MzMLRawDataFile newRawFile;
  private MzMLParser parser;

  private MZmineProject project;
  private RawDataFile newMZmineFile;
  private int totalScans = 0, parsedScans;
  private String description;
  private MZmineProcessingStep<MassDetector> ms1Detector = null;
  private MZmineProcessingStep<MassDetector> ms2Detector = null;

  public MzMLImportTask(MZmineProject project, File fileToOpen, RawDataFile newMZmineFile) {
    this(project, fileToOpen, newMZmineFile, null);
  }

  public MzMLImportTask(MZmineProject project, File fileToOpen, RawDataFile newMZmineFile,
      AdvancedSpectraImportParameters advancedParam) {
    super(newMZmineFile.getMemoryMapStorage()); // storage in raw data file
    this.project = project;
    this.newMZmineFile = newMZmineFile;
    this.fileToOpen = fileToOpen;
    this.inputStreamToOpen = null;

    this.description = "Importing raw data file: " + fileToOpen.getName();
    if (advancedParam != null) {
      if (advancedParam.getParameter(AdvancedSpectraImportParameters.msMassDetection).getValue()) {
        this.ms1Detector = advancedParam
            .getParameter(AdvancedSpectraImportParameters.msMassDetection)
            .getEmbeddedParameter().getValue();
      }
      if (advancedParam.getParameter(AdvancedSpectraImportParameters.ms2MassDetection).getValue()) {
        this.ms2Detector = advancedParam
            .getParameter(AdvancedSpectraImportParameters.msMassDetection)
            .getEmbeddedParameter().getValue();
      }
    }

    this.applyMassDetection = ms1Detector != null || ms2Detector != null;

  }

  public MzMLImportTask(MZmineProject project, InputStream inputStreamToOpen,
      RawDataFile newMZmineFile
  ) {
    this(project, inputStreamToOpen, newMZmineFile, null);
  }

  public MzMLImportTask(MZmineProject project, InputStream inputStreamToOpen,
      RawDataFile newMZmineFile,
      AdvancedSpectraImportParameters advancedParam) {
    super(newMZmineFile.getMemoryMapStorage()); // storage in raw data file

    this.project = project;
    this.newMZmineFile = newMZmineFile;
    this.description = "Importing raw data file from a stream";
    this.fileToOpen = null;
    this.inputStreamToOpen = inputStreamToOpen;

    if (advancedParam != null) {
      if (advancedParam.getParameter(AdvancedSpectraImportParameters.msMassDetection).getValue()) {
        this.ms1Detector = advancedParam
            .getParameter(AdvancedSpectraImportParameters.msMassDetection)
            .getEmbeddedParameter().getValue();
      }
      if (advancedParam.getParameter(AdvancedSpectraImportParameters.ms2MassDetection).getValue()) {
        this.ms2Detector = advancedParam
            .getParameter(AdvancedSpectraImportParameters.msMassDetection)
            .getEmbeddedParameter().getValue();
      }
    }

    this.applyMassDetection = ms1Detector != null || ms2Detector != null;
  }

  public File getFileToOpen() {
    return fileToOpen;
  }


  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    try {

      InputStream is = null;

      if (fileToOpen != null) {
        logger.finest("Began parsing file: " + fileToOpen.getAbsolutePath());
        is = FileMemoryMapper.mapToMemory(fileToOpen);
      } else if (inputStreamToOpen != null) {
        logger.finest("Began parsing file from stream");
        is = inputStreamToOpen;
      } else {
        throw new MZmineException("Invalid input");
      }
      // It's ok to directly create this particular reader, this class is `public final`
      // and we precisely want that fast UFT-8 reader implementation
      final XMLStreamReaderImpl xmlStreamReader = new XMLStreamReaderImpl();
      xmlStreamReader.setInput(is, "UTF-8");

      this.parser = new MzMLParser(this);
      this.newRawFile = parser.getMzMLRawFile();

      int eventType;
      try {
        do {
          // check if parsing has been cancelled?
          if (isCanceled()) {
            return;
          }

          eventType = xmlStreamReader.next();

          switch (eventType) {
            case XMLStreamConstants.START_ELEMENT:
              final CharArray openingTagName = xmlStreamReader.getLocalName();
              parser.processOpeningTag(xmlStreamReader, is, openingTagName);
              break;

            case XMLStreamConstants.END_ELEMENT:
              final CharArray closingTagName = xmlStreamReader.getLocalName();
              parser.processClosingTag(xmlStreamReader, closingTagName);
              break;

            case XMLStreamConstants.CHARACTERS:
              parser.processCharacters(xmlStreamReader);
              break;
          }

        } while (eventType != XMLStreamConstants.END_DOCUMENT);

      } finally {
        if (xmlStreamReader != null) {
          xmlStreamReader.close();
        }
      }
      logger.finest("mzML parsing complete");

      totalScans = newRawFile.getScans().size();

      if (newMZmineFile instanceof IMSRawDataFileImpl) {
        ((IMSRawDataFileImpl) newMZmineFile).addSegment(
            Range.closed(1, newRawFile.getScans().size()));
        buildIonMobilityFile(newRawFile);
      } else {
        buildLCMSFile(newRawFile);
      }


    } catch (Throwable e) {
      e.printStackTrace();
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Error parsing mzML: " + ExceptionUtils.exceptionToString(e));
      return;
    }

    if (parsedScans == 0) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("No scans found");
      return;
    }

    logger.info("Finished parsing " + fileToOpen + ", parsed " + parsedScans + " scans");
    project.addFile(newMZmineFile);

    setStatus(TaskStatus.FINISHED);

  }

  private double[][] applyMassDetection(MZmineProcessingStep<MassDetector> msDetector,
      Scan scan) {
    // run mass detection on data object
    // [mzs, intensities]
    return msDetector.getModule().getMassValues(scan, msDetector.getParameterSet());
  }

  public void buildLCMSFile(MzMLRawDataFile file) throws IOException {
    for (MzMLMsScan mzMLScan : file.getScans()) {

      Scan newScan = MzMLConversionUtils.msdkScanToSimpleScan(newMZmineFile, mzMLScan);

      if (applyMassDetection) {

        double[][] mzIntensities = null;

        // apply mass detection
        if (ms1Detector != null && newScan.getMSLevel() == 1) {
          mzIntensities = applyMassDetection(ms1Detector, newScan);
        } else if (ms2Detector != null && newScan.getMSLevel() >= 2) {
          mzIntensities = applyMassDetection(ms2Detector, newScan);
        }

        if (mzIntensities != null) {
          SimpleMassList newMassList = new SimpleMassList(newMZmineFile.getMemoryMapStorage(),
              mzIntensities[0], mzIntensities[1]);
          newScan.addMassList(newMassList);
        } else {
          // Override data points and spectrum type
          ScanPointerMassList newMassList = new ScanPointerMassList(newScan);
          newScan.addMassList(newMassList);
        }

      }

      newMZmineFile.addScan(newScan);
      parsedScans++;
      description =
          "Importing " + file.getName() + ", parsed " + parsedScans + "/" + totalScans + " scans";
    }
  }

  public void buildIonMobilityFile(MzMLRawDataFile file) throws IOException {
    int mobilityScanNumberCounter = 0;
    int frameNumber = 1;
    SimpleFrame buildingFrame = null;

    final List<BuildingMobilityScan> mobilityScans = new ArrayList<>();
    final List<Double> mobilities = new ArrayList<>();
    final List<BuildingImsMsMsInfo> buildingImsMsMsInfos = new ArrayList<>();
    Set<ImsMsMsInfo> finishedImsMsMsInfos = null;
    final IMSRawDataFile newImsFile = (IMSRawDataFile) newMZmineFile;

    Integer lastScanId = null;

    for (MzMLMsScan mzMLScan : file.getScans()) {
      if (buildingFrame == null || Float.compare((mzMLScan.getRetentionTime() / 60f),
          buildingFrame.getRetentionTime()) != 0) {
        mobilityScanNumberCounter = 0; // mobility scan numbers start with 0!
        // waters uses different numbering for ms1 and ms2, so we need to reset if we start a new frame.
        lastScanId = null;

        if (buildingFrame != null) { // finish the frame
          final SimpleFrame finishedFrame = buildingFrame;
          finishedFrame.setMobilityScans(mobilityScans);
          finishedFrame
              .setMobilities(mobilities.stream().mapToDouble(Double::doubleValue).toArray());
          newImsFile.addScan(buildingFrame);

          mobilityScans.clear();
          mobilities.clear();
          if (!buildingImsMsMsInfos.isEmpty()) {
            finishedImsMsMsInfos = new HashSet<>();
            for (BuildingImsMsMsInfo info : buildingImsMsMsInfos) {
              finishedImsMsMsInfos.add(info.build(null, buildingFrame));
            }
            finishedFrame.setPrecursorInfos(finishedImsMsMsInfos);
          }
          buildingImsMsMsInfos.clear();
        }

        buildingFrame = new SimpleFrame(newImsFile, frameNumber, mzMLScan.getMsLevel(),
            mzMLScan.getRetentionTime() / 60f, 0, 0, null, null,
            MzMLConversionUtils.msdkToMZmineSpectrumType(mzMLScan.getSpectrumType()),
            mzMLScan.getPolarity(), mzMLScan.getScanDefinition(),
            mzMLScan.getScanningRange(), mzMLScan.getMobility().mt(), null);
        frameNumber++;

        description =
            "Importing " + file.getName() + ", parsed " + parsedScans + "/" + totalScans + " scans";
      }

      // I'm not proud of this piece of code, but some manufactures or conversion tools leave out
      // empty scans. however, we need that info for proper processing ~SteffenHeu
      if (lastScanId == null) {
        lastScanId = mzMLScan.getScanNumber();
      } else {
        Integer newScanId = mzMLScan.getScanNumber();
        final int missingScans = newScanId - lastScanId;
        if (missingScans > 1) {
          final Double lastMobility = mobilities.get(mobilities.size() - 1);
          final double nextMobility = mzMLScan.getMobility().mobility();
          final double deltaMobility = nextMobility - lastMobility;
          final double stepSize = deltaMobility / (missingScans + 1);

          for (int i = 0; i < missingScans; i++) {
            // make up for data saving options leaving out empty scans.
            // todo check if this works properly
            mobilityScans
                .add(new BuildingMobilityScan(mobilityScanNumberCounter, new double[0],
                    new double[0]));

            final Double calculatedMobility = lastMobility + (i + 1) * stepSize;
            mobilities.add(calculatedMobility);
            mobilityScanNumberCounter++;
          }
        }
        lastScanId = mzMLScan.getScanNumber();
      }

      mobilityScans.add(
          MzMLConversionUtils.msdkScanToMobilityScan(mobilityScanNumberCounter, mzMLScan));
      mobilities.add(mzMLScan.getMobility().mobility());
      MzMLConversionUtils.extractImsMsMsInfo(mzMLScan, buildingImsMsMsInfos, frameNumber,
          mobilityScanNumberCounter);
      mobilityScanNumberCounter++;
      parsedScans++;
    }
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    // Parsing has not started yet
    if (parser == null) {
      return 0.0;
    }
    // First stage - parsing mzML
    if (totalScans == 0) {
      return parser.getFinishedPercentage() * 0.5;
    }
    // Second stage - importing scan data
    return 0.5 + (0.5 * ((double) parsedScans / totalScans));
  }

}
