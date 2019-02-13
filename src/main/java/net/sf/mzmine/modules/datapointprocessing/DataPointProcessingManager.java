package net.sf.mzmine.modules.datapointprocessing;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import net.sf.mzmine.main.MZmineCore;

public class DataPointProcessingManager implements Runnable {

  private static final DataPointProcessingManager inst = new DataPointProcessingManager();
  private int MAX_RUNNING = 5;

  private static Logger logger = Logger.getLogger(DataPointProcessingManager.class.getName());

  private List<DataPointProcessingController> waiting;
  private List<DataPointProcessingController> running;

  public static DataPointProcessingManager getInst() {
    return inst;
  }

  public void addController(DataPointProcessingController controller) {
    synchronized (waiting) {
      if (waiting.contains(controller)) {
        logger.info("Warning: Controller was already added to waiting list at index "
            + waiting.indexOf(controller) + "/" + waiting.size() + ". Skipping.");
        return;
      }
      waiting.add(controller);
    }
    logger.finest("Controller added to waiting list. (size = " + waiting.size() + ")");
  }

  public void removeWaitingController(DataPointProcessingController controller) {
    if (!waiting.contains(controller))
      return;

    synchronized (waiting) {
      waiting.remove(controller);
    }
    logger.finest("Controller removed from wating list. (size = " + waiting.size() + ")");
  }

  private void addRunningController(DataPointProcessingController controller) {
    synchronized (running) {
      if (running.contains(controller)) {
        logger.info("Warning: Controller was already added to waiting list at index "
            + running.indexOf(controller) + "/" + running.size() + ". Skipping.");
        return;
      }
      running.add(controller);
    }
    logger.finest("Controller added to running list. (size = " + running.size() + ")");
  }

  private void removeRunningController(DataPointProcessingController controller) {
    if (!running.contains(controller))
      return;

    synchronized (running) {
      running.remove(controller);
    }
    logger.finest("Controller removed from running list. (size = " + running.size() + ")");
  }

  public void startNextController() {
    if (running.size() >= MAX_RUNNING && waiting.size() < 1)
      return;

    DataPointProcessingController next;

    synchronized (waiting) {
      next = waiting.get(0);
      removeWaitingController(next);
    }

    running.add(next);
    next.execute();
    logger.finest("Started controller from running list. (size = " + running.size() + ")");
  }

  @Override
  public void run() {
    while (true) {

      try {
        TimeUnit.MILLISECONDS.sleep(200);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }


}
