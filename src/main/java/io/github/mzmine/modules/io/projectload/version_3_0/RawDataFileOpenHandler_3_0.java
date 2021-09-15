package io.github.mzmine.modules.io.projectload.version_3_0;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.batchmode.BatchModeModule;
import io.github.mzmine.modules.batchmode.BatchModeParameters;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.io.projectload.RawDataFileOpenHandler;
import io.github.mzmine.modules.io.projectsave.RawDataFileSaveHandler;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.AllTasksFinishedListener;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.StreamCopy;
import io.github.mzmine.util.ZipUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.zip.ZipFile;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class RawDataFileOpenHandler_3_0 extends AbstractTask implements RawDataFileOpenHandler {

  private static final Logger logger = Logger.getLogger(RawDataFileOpenHandler_3_0.class.getName());
  private int numSteps = 1;
  private int processedSteps = 0;

  private InputStream batchFileStream;
  private MZmineProject project;
  private AbstractTask currentTask;
  private ZipFile zipFile;

  public RawDataFileOpenHandler_3_0() {
    super(null);
  }

  public InputStream getBatchFileStream() {
    return batchFileStream;
  }

  public void setBatchFileStream(InputStream batchFileStream) {
    this.batchFileStream = batchFileStream;
  }

  public MZmineProject getProject() {
    return project;
  }

  public void setProject(MZmineProject project) {
    this.project = project;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    if (batchFileStream != null) {
      try {
        if (!loadRawDataFiles()) {
          setErrorMessage("Error while loading raw data files.");
          setStatus(TaskStatus.ERROR);
          return;
        }
      } catch (InterruptedException e) {
        setErrorMessage("Error while loading raw data files.");
        logger.log(Level.WARNING, e.getMessage(), e);
        setStatus(TaskStatus.ERROR);
        return;
      }
    }

    setStatus(TaskStatus.FINISHED);
  }

  private List<BatchQueue> loadBatchQueues()
      throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {

    final List<BatchQueue> queues = new ArrayList<>();

    final File tempFile = File.createTempFile(RawDataFileSaveHandler.TEMP_FILE_NAME, ".tmp");
    final FileOutputStream fstream = new FileOutputStream(tempFile);
    StreamCopy copyMachine = new StreamCopy();
    copyMachine.copy(batchFileStream, fstream);
    fstream.close();

    final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    final Document batchQueuesDocument = dBuilder.parse(tempFile);

    final XPathFactory factory = XPathFactory.newInstance();
    final XPath xpath = factory.newXPath();

    XPathExpression expr = xpath.compile("//" + RawDataFileSaveHandler.ROOT_ELEMENT + "/"
        + RawDataFileSaveHandler.BATCH_QUEUES_ROOT);

    NodeList nodes = (NodeList) expr.evaluate(batchQueuesDocument, XPathConstants.NODESET);
    if (nodes.getLength() != 1) {
      logger.warning(
          () -> "NodeList for element " + RawDataFileSaveHandler.BATCH_QUEUES_ROOT + " is != 1.");
    }

    final Element batchQueuesRoot = (Element) nodes.item(0);
    final NodeList batchQueues = batchQueuesRoot
        .getElementsByTagName(RawDataFileSaveHandler.BATCH_QUEUE_ELEMENT);

    for (int i = 0; i < batchQueues.getLength(); i++) {
      Element queueElement = (Element) batchQueues.item(i);
      BatchQueue batchQueue = BatchQueue.loadFromXml(queueElement);

      if (!batchQueue.isEmpty()) {
        queues.add(batchQueue);
      }
    }

    tempFile.delete();

    return queues;
  }

  @Override
  public String getTaskDescription() {
    return "Importing raw data files from project. Processing import batch step " + processedSteps
        + "/" + numSteps + ".";
  }

  @Override
  public double getFinishedPercentage() {
    return currentTask != null ? numSteps / (double) processedSteps / currentTask
        .getFinishedPercentage() : 0d;
  }

  public boolean loadRawDataFiles() throws InterruptedException {
    if (batchFileStream == null) {
      return false;
    }

    try {
      List<BatchQueue> batchQueues = loadBatchQueues();
      numSteps = batchQueues.size();

      Path tempDir = Files.createTempDirectory("mzmine_msdatafiles_temp");

      for (BatchQueue batchQueue : batchQueues) {
        final ParameterSet param = MZmineCore.getConfiguration()
            .getModuleParameters(BatchModeModule.class).cloneParameterSet();

        resolvePathsUnpackFiles(batchQueue, tempDir);

        param.setParameter(BatchModeParameters.batchQueue, batchQueue);

        final BatchModeModule batchModule = MZmineCore.getModuleInstance(BatchModeModule.class);
        final List<Task> tasks = new ArrayList<>();
        batchModule.runModule(project, param, tasks);

        List<AbstractTask> abstractTasks = tasks.stream().filter(t -> t instanceof AbstractTask)
            .map(t -> (AbstractTask) t).toList();
        currentTask = abstractTasks.get(0);

        AtomicBoolean finished = new AtomicBoolean(false);
        AtomicBoolean success = new AtomicBoolean(true);
        AllTasksFinishedListener listener = new AllTasksFinishedListener(abstractTasks, true,
            c -> finished.set(true), c -> success.set(false), c -> success.set(false));

        MZmineCore.getTaskController().addTasks(tasks.toArray(Task[]::new));

        while (!finished.get()) {
          Thread.sleep(100);
          if (isCanceled()) {
            return false;
          }
        }

        if (!success.get()) {
          return false;
        }

        processedSteps++;
      }

      if(!tempDir.toFile().delete()) {
        tempDir.toFile().deleteOnExit();
      }
    } catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException e) {
      logger.log(Level.WARNING, "Cannot load batch queues for raw data import.", e);
      setErrorMessage("Cannot load batch queues for raw data import.");
      setStatus(TaskStatus.ERROR);
      return false;
    }
    return true;
  }

  /**
   * Unpacks all raw data files of the given batch queue into the tempDir.
   *
   * @param batchQueue the batch queue.
   * @param tempDir    The temp dir.
   */
  private void resolvePathsUnpackFiles(BatchQueue batchQueue, @NotNull final Path tempDir)
      throws IOException {
    for (MZmineProcessingStep<MZmineProcessingModule> step : batchQueue) {
      for (Parameter<?> parameter : step.getParameterSet().getParameters()) {
        if (parameter instanceof FileNamesParameter fnp) {
          File[] files = fnp.getValue();
          File[] newFiles = new File[files.length];
          for (int i = 0; i < files.length; i++) {
            final File file = files[i];
            String path = file.getPath();
            Matcher matcher = RawDataFileSaveHandler.DATA_FILE_PATTERN.matcher(path);
            if (matcher.matches()) {
              path = matcher.group(2);
              ZipUtils.unzipEntry(path.replaceAll("\\\\", "/"), zipFile, tempDir.toFile());
              newFiles[i] = new File(tempDir.toFile(), path);
            } else {
              newFiles[i] = files[i];
            }
          }
          fnp.setValue(newFiles);
        }
      }
    }
  }

  @Override
  public void setZipFile(ZipFile zipFile) {
    this.zipFile = zipFile;
  }
}
