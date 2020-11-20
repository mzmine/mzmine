package io.github.mzmine.modules.dataprocessing.filter_framestofile;

import com.google.common.collect.Lists;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.RawDataFileWriter;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;

public class FrameToFileTask extends AbstractTask {

  private String description;
  private ParameterSet parameters;
  private RawDataFile file;
  private boolean threading;
  private LongProperty finishedFiles;
  private int numFrames;
  private final int minFrame;
  private int maxFrame;

  public FrameToFileTask(RawDataFile file, ParameterSet parameters) {
    this.file = file;
    this.parameters = parameters;
    description = "";
    threading = true;
    numFrames = 1;
    minFrame = parameters.getParameter(FrameToFileParameters.minFrame).getValue();
    maxFrame = parameters.getParameter(FrameToFileParameters.maxFrame).getValue();

    if (!(file instanceof IMSRawDataFile)) {
      throw new IllegalArgumentException("Not an IMSRawDataFile");
    }

    if(maxFrame <= 0) {
      maxFrame = ((IMSRawDataFile) file).getNumberOfFrames();
    }

    if (minFrame > maxFrame) {
      throw new IllegalArgumentException("minFrame > maxFrame");
    }

    numFrames = maxFrame - minFrame;

    finishedFiles = new SimpleLongProperty(0L);
    setStatus(TaskStatus.WAITING);
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    return (double) finishedFiles.getValue() / numFrames;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    List<Frame> allEligibleFrames = ((IMSRawDataFile) file).getFrames().stream()
        .filter(frame -> frame.getFrameId() >= minFrame && frame.getFrameId() <= maxFrame).collect(
            Collectors.toList());
    List<List<Frame>> frameLists =
        (threading) ? Lists.partition(allEligibleFrames, 300)
            : Lists.partition(allEligibleFrames, allEligibleFrames.size() + 1);

    for (List<Frame> frames : frameLists) {
      SaveThread thread = new SaveThread(frames);
      MZmineCore.getTaskController().addTask(thread);
    }
    setStatus(TaskStatus.FINISHED);
  }

  private synchronized void incrementFinishedFiles() {
    finishedFiles.setValue(finishedFiles.getValue() + 1);
  }

  private class SaveThread extends AbstractTask {

    private String desc;
    private double perc;
    private final List<Frame> frames;

    public SaveThread(List<Frame> frames) {
      this.frames = frames;
      desc = "";
      setStatus(TaskStatus.WAITING);
    }

    @Override
    public String getTaskDescription() {
      return desc;
    }

    @Override
    public double getFinishedPercentage() {
      return perc;
    }

    @Override
    public void run() {
      setStatus(TaskStatus.PROCESSING);
      NumberFormat rtFormat = new DecimalFormat("#.0000");
      for (Frame frame : frames) {
        if (FrameToFileTask.this.isCanceled() || SaveThread.this.isCanceled()) {
          setStatus(TaskStatus.CANCELED);
          return;
        }
        try {
          final String newFilename = (file.getName() + " - " + frame.getScanDefinition() + "Frame "
              + frame.getFrameId() + " RT-" + rtFormat.format(frame.getRetentionTime()))
              .replaceAll("\\.", "_");
          RawDataFileWriter newFile = MZmineCore
              .createNewFile(newFilename);

          for (Scan scan : frame.getMobilityScans()) {
            SimpleScan newScan = new SimpleScan(null, scan.getScanNumber(), scan.getMSLevel(),
                scan.getMobility(), scan.getPrecursorMZ(), scan.getPrecursorCharge(),
                scan.getFragmentScanNumbers(), scan.getDataPoints(), scan.getSpectrumType(),
                scan.getPolarity(), scan.getScanDefinition(), scan.getDataPointMZRange());
            newFile.addScan(newScan);
          }

          MZmineCore.getProjectManager().getCurrentProject().addFile(newFile.finishWriting());
          incrementFinishedFiles();
          perc = frames.indexOf(frame) / frames.size();
          desc = file.getName() + ": " + frames.indexOf(frame) + "/" + frames.size();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      setStatus(TaskStatus.FINISHED);
    }
  }
}
