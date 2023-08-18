/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.util.io;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Thread safe way to push text from multiple threads (e.g., parallel stream) to a text file. This
 * task is designed to cache text that should be appended to a file.
 */
public class ParallelTextWriterTask extends AbstractTask {

  private final @Nullable Task mainTask;
  private final File file;
  private final ConcurrentLinkedQueue<String> textToAppend = new ConcurrentLinkedQueue<>();
  private boolean writeFinished = false;

  public ParallelTextWriterTask(@Nullable final Task mainTask, @NotNull File file,
      boolean startTask) {
    super(null, Instant.now());
    this.mainTask = mainTask;

    this.file = file;

    if (startTask) {
      MZmineCore.getTaskController().addTask(this, TaskPriority.HIGH);
    }
  }

  @Override
  public String getTaskDescription() {
    return "Exporting text file: %s (%d++ lines left)".formatted(file.getAbsolutePath(),
        textToAppend.size());
  }

  @Override
  public double getFinishedPercentage() {
    return isFinished() ? 1 : 0;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    String line = null;
    // Open file
    try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
//       wait for lines
      while (!writeFinished) {
        if (isCanceled()) {
          return;
        }

        while ((line = textToAppend.poll()) != null) {
          writer.append(line);
        }

        if (mainTask != null) {
          if (mainTask.isFinished()) {
            break;
          } else if (mainTask.isCanceled()) {
            cancel();
            return;
          }
        }

        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    } catch (IOException e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Cannot open file " + file + "  " + e.getMessage());
      return;
    }

    setStatus(TaskStatus.FINISHED);
  }

  public void setWriteFinished() {
    this.writeFinished = true;
  }

  public void append(final String text) {
    textToAppend.add(text);
  }

  public void appendLine(final String line) {
    append(line + "\n");
  }

  public void addAll(Collection<String> text) {
    textToAppend.addAll(text);
  }
}
