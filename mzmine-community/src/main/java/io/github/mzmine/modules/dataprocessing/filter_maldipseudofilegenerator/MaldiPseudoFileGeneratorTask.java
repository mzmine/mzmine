/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_maldipseudofilegenerator;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.ImagingScan;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.BuildingMobilityScan;
import io.github.mzmine.datamodel.impl.IMSImagingRawDataFileImpl;
import io.github.mzmine.datamodel.impl.SimpleImagingFrame;
import io.github.mzmine.datamodel.impl.SimpleImagingScan;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.MaldiSpotInfo;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.impl.ImagingRawDataFileImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.jetbrains.annotations.NotNull;

/**
 * Takes MALDI imaging files and splits them into multiple files by creating a single file for each
 * spot.
 */
public class MaldiPseudoFileGeneratorTask extends AbstractTask {

  private final ParameterSet parameters;
  private final MZmineProject project;
  RawDataFile[] files;
  private int processed = 0;

  protected MaldiPseudoFileGeneratorTask(ParameterSet parameters, MZmineProject project, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.parameters = parameters;
    this.project = project;
    files = parameters.getValue(MaldiPseudoFileGeneratorParameters.files).getMatchingRawDataFiles();
  }

  @Override
  public String getTaskDescription() {
    return null;
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    final Map<String, List<ImagingScan>> spotScanMap = new HashMap<>();
    final List<RawDataFile> newFiles = new ArrayList<>();
    for (final RawDataFile file : files) {
      if (!(file instanceof ImagingRawDataFile maldiFile)) {
        processed++;
        continue;
      }

      for (Scan scan : maldiFile.getScans()) {
        if (!(scan instanceof ImagingScan imagingScan)) {
          continue;
        }

        final MaldiSpotInfo maldiSpotInfo = imagingScan.getMaldiSpotInfo();
        if (maldiSpotInfo == null) {
          continue;
        }

        final String s = maldiSpotInfo.spotName();
        final List<ImagingScan> scans = spotScanMap.computeIfAbsent(s, spot -> new ArrayList<>());
        scans.add(imagingScan);
      }

      for (Entry<String, List<ImagingScan>> spotScanEntry : spotScanMap.entrySet()) {
        final String spot = spotScanEntry.getKey();
        final List<ImagingScan> scans = spotScanEntry.getValue();

        if (file instanceof IMSRawDataFile) {
          var newFile = new IMSImagingRawDataFileImpl(file.getName() + " " + spot, null, file.getMemoryMapStorage());
          for (ImagingScan scan : scans) {
            final SimpleImagingFrame newFrame = new SimpleImagingFrame(newFile, scan.getScanNumber(), scan.getMSLevel(), scan.getRetentionTime(),
                scan.getMzValues(new double[0]), scan.getIntensityValues(new double[0]), scan.getSpectrumType(), scan.getPolarity(), scan.getScanDefinition(),
                scan.getScanningMZRange(), ((Frame) scan).getMobilityType(), ((Frame) scan).getImsMsMsInfos(), scan.getInjectionTime());
            newFrame.setCoordinates(scan.getCoordinates());
            newFrame.setMaldiSpotInfo(scan.getMaldiSpotInfo());

            final List<BuildingMobilityScan> buildingMobilityScans = ((Frame) scan).getMobilityScans()
                .stream().map(mob -> new BuildingMobilityScan(mob.getMobilityScanNumber(), mob.getMzValues(new double[0]), mob.getIntensityValues(new double[0])))
                .toList();
            newFrame.setMobilityScans(buildingMobilityScans, false);
            newFile.addScan(newFrame);
            newFrame.setMobilities(((Frame) scan).getMobilities().toDoubleArray());
          }
          newFile.getAppliedMethods().add(
              new SimpleFeatureListAppliedMethod(MaldiPseudoFileGeneratorModule.class, parameters,
                  getModuleCallDate()));
          newFiles.add(newFile);
        } else {
          var newFile = new ImagingRawDataFileImpl(file.getName() + " " + spot, null,
              file.getMemoryMapStorage());
          for (ImagingScan scan : scans) {
            final SimpleImagingScan newScan = new SimpleImagingScan(newFile, scan.getScanNumber(),
                scan.getMSLevel(), scan.getRetentionTime(),
                scan.getPrecursorMz() != null ? scan.getPrecursorMz() : 0d,
                scan.getPrecursorCharge() != null ? scan.getPrecursorCharge() : 0,
                scan.getMzValues(new double[0]), scan.getIntensityValues(new double[0]),
                scan.getSpectrumType(), scan.getPolarity(), scan.getScanDefinition(),
                scan.getScanningMZRange(), scan.getCoordinates());
            newScan.setMaldiSpotInfo(scan.getMaldiSpotInfo());
            newFile.addScan(newScan);
          }
          newFile.getAppliedMethods().add(
              new SimpleFeatureListAppliedMethod(MaldiPseudoFileGeneratorModule.class, parameters,
                  getModuleCallDate()));
          newFile.setImagingParam(((ImagingRawDataFile) file).getImagingParam());
          newFiles.add(newFile);
        }
      }
      processed++;
    }

    FxThread.runLater(() -> {
      newFiles.forEach(project::addFile);
    });

    setStatus(TaskStatus.FINISHED);
  }
}
