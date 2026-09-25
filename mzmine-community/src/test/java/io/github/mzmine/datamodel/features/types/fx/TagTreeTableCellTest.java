/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.datamodel.features.types.fx;

import fxinitializer.InitJavaFX;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.WINDOWS)
class TagTreeTableCellTest {

  @BeforeAll
  static void initFx() {
    InitJavaFX.init();
  }

  private static void runOnFxAndWait(@NotNull final Runnable action) throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    final Throwable[] error = new Throwable[1];
    final long deadline = System.currentTimeMillis() + 30_000;
    while (true) {
      try {
        Platform.runLater(() -> {
          try {
            Platform.setImplicitExit(false);
            action.run();
          } catch (Throwable thrown) {
            error[0] = thrown;
          } finally {
            latch.countDown();
          }
        });
        break;
      } catch (IllegalStateException toolkitNotInitialized) {
        if (System.currentTimeMillis() > deadline) {
          throw toolkitNotInitialized;
        }
        Thread.sleep(50);
      }
    }
    Assertions.assertTrue(latch.await(30, TimeUnit.SECONDS), "JavaFX task did not finish");
    if (error[0] != null) {
      throw new AssertionError(error[0]);
    }
  }

  @Test
  void keepsCheckboxesForStoredBitsBeyondTheConfiguredLabels() throws Exception {
    runOnFxAndWait(() -> {
      final List<String> configuredLabels = List.of("A", "B", "C", "D");
      final BitSet tags = new BitSet();
      tags.set(1);
      tags.set(4);

      final List<String> displayLabels = TagTreeTableCell.labelsForTags(configuredLabels, tags);
      Assertions.assertEquals(List.of("A", "B", "C", "D", ""), displayLabels);
      Assertions.assertEquals(4, configuredLabels.size());

      tags.clear(4);
      Assertions.assertEquals(configuredLabels,
          TagTreeTableCell.labelsForTags(configuredLabels, tags));
    });
  }

  @Test
  void usesConfiguredCountForFewerBitsAndFillsSparseExtraIndices() throws Exception {
    runOnFxAndWait(() -> {
      final List<String> configuredLabels = List.of("A", "B", "C", "D");
      final BitSet tags = new BitSet();
      tags.set(1);
      Assertions.assertEquals(configuredLabels,
          TagTreeTableCell.labelsForTags(configuredLabels, tags));
      Assertions.assertEquals(configuredLabels,
          TagTreeTableCell.labelsForTags(configuredLabels, null));

      tags.set(7);
      Assertions.assertEquals(List.of("A", "B", "C", "D", "", "", "", ""),
          TagTreeTableCell.labelsForTags(configuredLabels, tags));
    });
  }
}
