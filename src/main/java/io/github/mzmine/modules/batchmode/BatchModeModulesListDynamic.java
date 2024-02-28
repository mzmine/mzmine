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

package io.github.mzmine.modules.batchmode;

import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineRunnableModule;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class BatchModeModulesListDynamic {

  public static final List<Class<MZmineProcessingModule>> BATCH_MODULES_DYNAMIC;
  public static final List<Class<MZmineRunnableModule>> RUNNABLE_MODULES_DYNAMIC;
  private static final Logger logger = Logger.getLogger(BatchModeModulesList.class.getName());

  static {
    List<Class<MZmineProcessingModule>> batchModules = new ArrayList<>();
    List<Class<MZmineRunnableModule>> runnableModules = new ArrayList<>();

    try {
      ClassPath path = ClassPath.from(MZmineModule.class.getClassLoader());
      for (ClassInfo classInfo : path.getTopLevelClassesRecursive("io.github.mzmine.modules")) {

        if (!classInfo.getSimpleName().contains("Module")) {
          continue;
        }

        final Class<?> clazz = classInfo.load();
        // skip abstract classes and interfaces
        if (Modifier.isAbstract(clazz.getModifiers()) || Modifier.isInterface(
            clazz.getModifiers())) {
          continue;
        }

        if (!isValidModule(clazz)) {
          continue;
        }

        for (Class<?> anInterface : clazz.getInterfaces()) {
          if (anInterface.equals(MZmineProcessingModule.class)) {
            batchModules.add((Class<MZmineProcessingModule>) clazz);
          }
          if (anInterface.equals(MZmineRunnableModule.class)) {
            runnableModules.add((Class<MZmineRunnableModule>) clazz);
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    BATCH_MODULES_DYNAMIC = Collections.unmodifiableList(sortModulesList(batchModules));
    RUNNABLE_MODULES_DYNAMIC = Collections.unmodifiableList(sortModulesList(runnableModules));

    batchModules.removeAll(BatchModeModulesList.MODULES);
    logger.finest(() -> "Modules in dynamic list that are not in old list: " + batchModules.stream()
        .map(c -> c.getSimpleName()).collect(Collectors.joining(", ")).toString());
  }

  /**
   * Checks if the class implements {@link MZmineProcessingModule} or {@link MZmineRunnableModule}
   * and is not annotated as {@link DeprecatedModule}.
   */
  private static <T> boolean isValidModule(Class<T> clazz) {
    boolean valid = false;
    for (Class<?> anInterface : clazz.getInterfaces()) {
      // check if the correct interface is implemented
      if (anInterface.equals(MZmineProcessingModule.class) || anInterface.equals(
          MZmineRunnableModule.class)) {
        valid = true;
      }
      if (anInterface.equals(DeprecatedModule.class)) {
        logger.finest(() -> "Not adding deprecated module " + clazz.getName()
            + " to batch mode modules list.");
        return false;
      }
    }

    return valid;
  }

  public static <T extends MZmineRunnableModule> List<Class<T>> sortModulesList(
      List<Class<T>> list) {
    return list.stream().sorted((clazz1, clazz2) -> {
      T instance1 = MZmineCore.getModuleInstance(clazz1);
      T instance2 = MZmineCore.getModuleInstance(clazz2);

      final int mainCategoryCompare = instance1.getModuleCategory().getMainCategory()
          .compareTo(instance2.getModuleCategory().getMainCategory());
      if (mainCategoryCompare != 0) {
        return mainCategoryCompare;
      }
      return instance1.getModuleCategory().compareTo(instance2.getModuleCategory());
    }).toList();

  }
}
