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

import com.google.common.collect.Comparators;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.AllTasksFinishedListener;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.FeatureListRowSorter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class MZmineTestUtil {

  /**
   * Call a module in MZmineCore and wait for it to finish.
   *
   * @param timeoutSeconds timeout in seconds. if the module does not finish in time, exception is
   *                       thrown
   * @param moduleClass    the module to run
   * @param parameters     the module parameters
   * @return true if module finishes
   * @throws InterruptedException if module does not finish in time
   */
  public static TaskResult callModuleWithTimeout(long timeoutSeconds,
      @NotNull Class<? extends MZmineRunnableModule> moduleClass,
      @NotNull ParameterSet parameters) throws InterruptedException {
    return callModuleWithTimeout(timeoutSeconds, TimeUnit.SECONDS, moduleClass, parameters);
  }

  /**
   * Call a module in MZmineCore and wait for it to finish.
   *
   * @param timeout     timeout in time unit. if the module does not finish in time, exception is
   *                    thrown
   * @param unit        the time unit for the timeout
   * @param moduleClass the module to run
   * @param parameters  the module parameters
   * @return true if module finishes
   * @throws InterruptedException if module does not finish in time
   */
  public static TaskResult callModuleWithTimeout(long timeout, TimeUnit unit,
      @NotNull Class<? extends MZmineRunnableModule> moduleClass,
      @NotNull ParameterSet parameters) throws InterruptedException {
    List<Task> tasks = MZmineCore
        .runMZmineModule(moduleClass, parameters);
    List<AbstractTask> abstractTasks = new ArrayList<>(tasks.size());
    for (Task t : tasks) {
      if (t instanceof AbstractTask at) {
        abstractTasks.add(at);
      } else {
        throw new UnsupportedOperationException(
            "Currently only abstract tasks can be tested with this method");
      }
    }

    // wait for all tasks to finish
    List<String> errorMessage = Collections.synchronizedList(new ArrayList<>());
    CountDownLatch lock = new CountDownLatch(1);
    new AllTasksFinishedListener(abstractTasks, false, at -> {
      // free lock if succeeded
      lock.countDown();
    }, errorTasks -> {
      // concat error and free lock
      errorTasks.stream().map(AbstractTask::getErrorMessage).filter(Objects::nonNull)
          .forEach(errorMessage::add);
      lock.countDown();
    });

    // wait
    if(!lock.await(timeout, unit)) {
      return  TaskResult.TIMEOUT;
    } ;

    if (errorMessage.size() > 0) {
      throw new RuntimeException(
          "Error in task for module " + moduleClass.getName() + ".  " + errorMessage.stream()
              .collect(Collectors.joining("; ")));
    }
    if(abstractTasks.stream().allMatch(task -> task.isFinished()))
    return TaskResult.FINISHED;
    else {
      return TaskResult.ERROR;
    }
  }

  /**
   * Check for default sorting
   *
   * @param flist feature list
   * @return
   */
  public static boolean isSorted(FeatureList flist) {
    return Comparators.isInOrder(flist.getRows(), FeatureListRowSorter.DEFAULT_RT);
  }

  public static void cleanProject() {
    MZmineCore.getProjectManager().setCurrentProject(new MZmineProjectImpl());
  }
}
