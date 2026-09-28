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

package io.github.mzmine.modules.batchmode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import testutils.MZmineTestUtil;

/**
 * Batch files created before a module was removed must still load: the removed step is dropped and
 * reported, all other steps are kept. See {@link ModuleMappingUtils#getRemovedModules()}.
 */
class RemovedModuleBatchLoadTest {

  private static final String KEPT_MODULE = AllSpectralDataImportModule.class.getName();

  @BeforeAll
  static void initMzmine() {
    MZmineTestUtil.startMzmineCore();
  }

  @Test
  void removedStepIsSkippedAndReported() throws Exception {
    final Map<String, RemovedModule> removedModules = ModuleMappingUtils.getRemovedModules();
    assertFalse(removedModules.isEmpty(), "test needs at least one registered removed module");
    final RemovedModule removed = removedModules.values().iterator().next();

    final List<String> errorMessages = new ArrayList<>();
    final BatchQueue queue = loadBatch(errorMessages, removed.className(), KEPT_MODULE);

    // the removed step is dropped, the surrounding step survives
    assertEquals(1, queue.size(), "only the still existing step should remain in the queue");
    assertEquals(KEPT_MODULE, queue.getFirst().getModule().getClass().getName());

    // the warning names the module, its class and why it is gone
    final String warnings = String.join("\n", errorMessages);
    assertTrue(warnings.contains(removed.name()),
        () -> "warning should name the module: " + warnings);
    assertTrue(warnings.contains(removed.className()),
        () -> "warning should name the class: " + warnings);
    assertTrue(warnings.contains(removed.description()),
        () -> "warning should explain the removal: " + warnings);
  }

  @Test
  void everyRemovedModuleIsReallyGone() {
    for (final RemovedModule removed : ModuleMappingUtils.getRemovedModules().values()) {
      assertTrue(classIsAbsent(removed.className()),
          () -> "%s is listed as removed but the class still exists".formatted(
              removed.className()));
    }
  }

  private static boolean classIsAbsent(final String className) {
    try {
      Class.forName(className);
      return false;
    } catch (ClassNotFoundException e) {
      return true;
    }
  }

  /**
   * Build a minimal batch xml with one step per given module class, in order.
   */
  private static BatchQueue loadBatch(final List<String> errorMessages, final String... methods)
      throws Exception {
    final StringBuilder xml = new StringBuilder("<batch mzmine_version=\"4.9.14\">");
    for (final String method : methods) {
      xml.append("<batchstep method=\"").append(method).append("\" parameter_version=\"1\"/>");
    }
    xml.append("</batch>");

    final Element root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(new ByteArrayInputStream(xml.toString().getBytes(StandardCharsets.UTF_8)))
        .getDocumentElement();
    return BatchQueue.loadFromXml(root, errorMessages, false);
  }
}
