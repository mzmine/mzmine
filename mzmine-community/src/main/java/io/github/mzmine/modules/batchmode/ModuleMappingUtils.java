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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains mappings of old module names and packages to new ones for batch config import.
 */
public class ModuleMappingUtils {

  /**
   *
   * @return map of old to new module names
   */
  public static Map<String, String> getOldModuleNamesMap() {
    Map<String, String> oldNames = HashMap.newHashMap(5);
    oldNames.put("io.mzio.mzminepro.modules.otherdata.filt_shifttraces.ShiftTracesModule",
        "io.mzio.mzminepro.modules.otherdata.filt_shifttraces.ShiftTrimAndBinTracesModule");

    return oldNames;
  }

  /**
   * Modules that were removed from mzmine. A batch step referencing one of these is dropped with a
   * warning instead of failing the whole batch file, because the step cannot be recreated.
   *
   * @return removed processing modules, keyed by the class name written to batch files
   */
  public static Map<String, RemovedModule> getRemovedModules() {
    final List<RemovedModule> removed = List.of(//
        new RemovedModule(
            "io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.relations.IonNetRelationsModule",
            "Relations between ion identity networks",
            "Removed as it was rarely used and made load/save of ions impossible. All other ion identity networking steps are unaffected."));

    final Map<String, RemovedModule> map = HashMap.newHashMap(removed.size());
    for (final RemovedModule module : removed) {
      map.put(module.className(), module);
    }
    return map;
  }

}
