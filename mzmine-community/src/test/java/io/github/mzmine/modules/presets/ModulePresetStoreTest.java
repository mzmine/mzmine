/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.presets;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.batchmode.BatchModeModule;
import io.github.mzmine.modules.batchmode.BatchModeParameters;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.dataprocessing.align_join.JoinAlignerModule;
import io.github.mzmine.modules.dataprocessing.align_join.JoinAlignerParameters;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.presets.PresetCategory;
import java.io.File;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testutils.MZmineTestUtil;

class ModulePresetStoreTest {

  private static ModulePresetStore store;

  @BeforeAll
  static void beforeAll() {
    MZmineTestUtil.startMzmineCore();
    store = new ModulePresetStore(MZmineCore.getModuleInstance(BatchModeModule.class));
  }

  @Test
  void loadBatchPreset() {
    final File file = new File(getClass().getResource("batchmode.mzpresets").getFile());
    final ModulePreset preset = store.loadFromFile(file);
    Assertions.assertNotNull(preset);
    Assertions.assertEquals("tims", preset.name());
    Assertions.assertEquals(PresetCategory.MODULES, preset.presetCategory());
    Assertions.assertEquals("BatchModeModule", preset.presetGroup());
    final ParameterSet params = preset.parameters();
    Assertions.assertEquals(2, params.getParameters().length);

    final BatchQueue queue = params.getValue(BatchModeParameters.batchQueue);
    Assertions.assertEquals(17, queue.size());

    Assertions.assertEquals(AllSpectralDataImportModule.class.getSimpleName(),
        queue.get(0).getModule().getClass().getSimpleName());
    Assertions.assertEquals(2,
        queue.get(0).getParameterSet().getValue(AllSpectralDataImportParameters.fileNames).length);
    Assertions.assertEquals(new File("D:\\random\\path\\test2.d"),
        queue.get(0).getParameterSet().getValue(AllSpectralDataImportParameters.fileNames)[0]);

    Assertions.assertEquals(JoinAlignerModule.class.getSimpleName(),
        queue.get(14).getModule().getClass().getSimpleName());
    Assertions.assertEquals(8.0,
        queue.get(14).getParameterSet().getValue(JoinAlignerParameters.MZTolerance)
            .getPpmTolerance(), 0.0001);

  }

  @Test
  void loadBatchPresetWithErrorInParamName() {
    final File file = new File(getClass().getResource("batchmode_error.mzpresets").getFile());
    final ModulePreset preset = store.loadFromFile(file);
    Assertions.assertNotNull(preset);
    Assertions.assertEquals("tims", preset.name());
    Assertions.assertEquals(PresetCategory.MODULES, preset.presetCategory());
    Assertions.assertEquals("BatchModeModule", preset.presetGroup());
    final ParameterSet params = preset.parameters();
    Assertions.assertEquals(2, params.getParameters().length);

    final BatchQueue queue = params.getValue(BatchModeParameters.batchQueue);
    Assertions.assertEquals(17, queue.size());

    Assertions.assertEquals(AllSpectralDataImportModule.class.getSimpleName(),
        queue.get(0).getModule().getClass().getSimpleName());
    Assertions.assertEquals(2,
        queue.get(0).getParameterSet().getValue(AllSpectralDataImportParameters.fileNames).length);
    Assertions.assertEquals(new File("D:\\random\\path\\test2.d"),
        queue.get(0).getParameterSet().getValue(AllSpectralDataImportParameters.fileNames)[0]);

    Assertions.assertEquals(JoinAlignerModule.class.getSimpleName(),
        queue.get(14).getModule().getClass().getSimpleName());
    Assertions.assertEquals(8.0,
        queue.get(14).getParameterSet().getValue(JoinAlignerParameters.MZTolerance)
            .getPpmTolerance(), 0.0001);

  }

}