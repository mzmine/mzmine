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
import java.util.List;

public class FrameToFileTask extends AbstractTask {

  private String description;
  private double finishedPercentage;
  private ParameterSet parameters;
  private RawDataFile file;
  private boolean threading;
  private int finishedFiles;
  private int numFrames;

  public FrameToFileTask(RawDataFile file, ParameterSet parameters) {
    this.file = file;
    this.parameters = parameters;
    description = "";
    finishedPercentage = 0;
    threading = true;
    numFrames = 1;
    setStatus(TaskStatus.WAITING);
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    return (double) finishedFiles / numFrames;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    if (!(file instanceof IMSRawDataFile)) {
      setStatus(TaskStatus.FINISHED);
      return;
    }

    numFrames = ((IMSRawDataFile) file).getNumberOfFrames();
    List<Frame> allFrames = ((IMSRawDataFile) file).getFrames();
    List<List<Frame>> frameLists =
        (threading) ? Lists.partition(allFrames, 300)
            : Lists.partition(allFrames, allFrames.size() + 1);

    for (List<Frame> frames : frameLists) {
      SaveThread thread = new SaveThread(frames);
      MZmineCore.getTaskController().addTask(thread);
    }

    setStatus(TaskStatus.FINISHED);
  }

  private synchronized void incrementFinishedFiles() {
    finishedFiles++;
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
      for (Frame frame : frames) {
        if (FrameToFileTask.this.isCanceled() || SaveThread.this.isCanceled()) {
          setStatus(TaskStatus.CANCELED);
          return;
        }
        try {
          RawDataFileWriter newFile = MZmineCore
              .createNewFile(file.getName() + " - " + frame.getScanDefinition());

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
