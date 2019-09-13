package net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.Range;
import de.unijena.bioinf.ChemistryBase.ms.SpectrumProperty;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.MassSpectrumType;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.RawDataFileWriter;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleScan;
import net.sf.mzmine.project.impl.RawDataFileImpl;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

public class CsvReadTask extends AbstractTask {

  private Logger logger = Logger.getLogger(CsvReadTask.class.getName());

  protected String dataSource;
  private File file;
  private MZmineProject project;
  private RawDataFileImpl newMZmineFile;
  private RawDataFile finalRawDataFile;

  private int totalScans, parsedScans;

  public CsvReadTask(MZmineProject project, File fileToOpen, RawDataFileWriter newMZmineFile) {
    this.project = project;
    this.file = fileToOpen;
    this.newMZmineFile = (RawDataFileImpl) newMZmineFile;
  }

  @Override
  public String getTaskDescription() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public double getFinishedPercentage() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    Scanner scanner;

    logger.setLevel(Level.ALL);

    try {
      scanner = new Scanner(file);

      dataSource = getFileName(scanner);
      if (dataSource == null) {
        setErrorMessage("Could not open data file " + file.getAbsolutePath());
        setStatus(TaskStatus.ERROR);
        return;
      }
      logger.info("opening raw file " + dataSource);

      String acquisitionDate = getAcqusitionDate(scanner);
      if (acquisitionDate == null) {
        setErrorMessage("Could not find acquisition date in file " + file.getAbsolutePath());
        setStatus(TaskStatus.ERROR);
        return;
      }

      logger.info("Date of acquisition " + acquisitionDate);

      // scanner.useDelimiter(",");

      List<String> mzsList = new ArrayList<String>();
      String mstype = "";
      String ions = "";
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        logger.fine("checking line: " + line + " for 'Time [sec]'...");
        if (line.startsWith("Time [Sec]")) {
          String[] axes = line.split(",");
          logger.fine("Found axes" + Arrays.toString(axes));
          for (int i = 1; i < axes.length; i++) {
            String axis = axes[i];
            ions += axis + ", ";
            if (axis.contains("->")) {
              mstype = "MS/MS";
              logger.fine("axis " + axis + " is an ms^2 scan");
              String mz = axis.substring(axis.indexOf("-> ") + 3);
              mz.trim();
              logger.fine("Axis " + axis + " was scanned at m/z = '" + mz + "'");
              mzsList.add(mz);
            } else {
              logger.severe("Invalid axis labelling, please contact the developers.");
            }
          }
          break;
        }
      }

      int[] mzs = new int[mzsList.size()];
      for (int i = 0; i < mzsList.size(); i++)
        mzs[i] = Integer.valueOf(mzsList.get(i));

      Range<Double> mzRange = Range.closed((double) mzs[0] - 10, (double) mzs[1] + 10);

      int scanNumber = 1;

      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        if (line == null || line.trim().equals(""))
          continue;
        String[] columns = line.split(",");
        if (columns == null || columns.length != mzs.length + 1)
          continue;

        double rt = Double.valueOf(columns[0]);

        DataPoint dataPoints[] = new SimpleDataPoint[mzs.length];
        for (int i = 0; i < dataPoints.length; i++) {
          String intensity = columns[i + 1];
          dataPoints[i] = new SimpleDataPoint(mzs[i], Double.valueOf(intensity));
        }

        Scan scan = new SimpleScan(null, scanNumber, 1, rt, 0.0, 1, null, dataPoints,
            MassSpectrumType.CENTROIDED, PolarityType.POSITIVE,
            "ICP-" + mstype + " " + ions.substring(0, ions.length() - 2), mzRange);

        newMZmineFile.addScan(scan);
        scanNumber++;
      }

      finalRawDataFile = newMZmineFile.finishWriting();

      project.addFile(finalRawDataFile);

    } catch (Exception e) {
      setErrorMessage(e.getMessage());
      setStatus(TaskStatus.ERROR);
      return;
    }

    this.setStatus(TaskStatus.FINISHED);
  }

  private @Nullable String getFileName(@Nonnull Scanner scanner) {
    String path = null;
    while (scanner.hasNextLine()) {
      String line = scanner.nextLine();
      if (line.contains(":") && line.contains("\\")) {
        path = line;
        return path;
      }
    }
    return path;
  }

  private @Nullable String getAcqusitionDate(@Nonnull Scanner scanner) {
    String acquisitionDate = null;

    while (scanner.hasNextLine()) {
      String line = scanner.nextLine();
      if (line.startsWith("Acquired")) {
        int begin = line.indexOf(":") + 2;
        line.subSequence(begin, begin + (new String("00/00/0000 00:00:00")).length());
        return line;
      }
    }
    return acquisitionDate;
  }

}
