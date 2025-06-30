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

import static java.util.Comparator.reverseOrder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.reflect.ClassPath;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.batchmode.BatchModeModulesList;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.io.JsonUtils;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import testutils.MZmineTestUtil;

public class ListModuleDocMainTest {

  private static final Logger logger = Logger.getLogger(ListModuleDocMainTest.class.getName());
  private static final File file = new File(
      "D:\\OneDrive - mzio GmbH\\mzio\\development\\modules\\mzmine_modules4.7.8.json");

  @BeforeAll
  static void init() {
    MZmineTestUtil.startMzmineCore();
  }

  @Disabled
  @Test
  void listModulesFromFile() throws IOException {
    List<ModuleRecord> records = JsonUtils.MAPPER.readValue(file, new TypeReference<>() {
    });

    logger.info(records.stream().map(Object::toString).collect(Collectors.joining("\n")));
  }

  @Disabled
  @Test
  void listModules() throws IOException {

    final Set<String> batchModules = BatchModeModulesList.MODULES.stream().map(Class::getName)
        .collect(Collectors.toSet());

    final Set<String> toolsAndVisual = BatchModeModulesList.TOOLS_AND_VISUALIZERS.stream()
        .map(Class::getName).collect(Collectors.toSet());

    // module list is not loaded yet...
//    final Set<String> initializedModules = List.copyOf(MZmineCore.getAllModules()).stream()
//        .map(Object::getClass).map(Class::getName).collect(Collectors.toSet());

    final List<ModuleRecord> records = new ArrayList<>();

    final List<ModuleParameters> allModules = listAllModules();
    for (var modParam : allModules) {
      MZmineModule module = modParam.module();
      ParameterSet parameters = modParam.parameterSet();
      if (parameters == null) {
        parameters = ConfigService.getConfiguration().getModuleParameters(module.getClass());
      }

      final boolean deprecated = module.getClass().isAnnotationPresent(Deprecated.class);

      final String moduleClassName = module.getClass().getName();
      final boolean batchModule = batchModules.contains(moduleClassName);
      final boolean toolsOrVisual = toolsAndVisual.contains(moduleClassName);

      final String docLink = parameters == null ? null : parameters.getOnlineHelpUrl();

      records.add(new ModuleRecord(module.getName(), module.getClass().getSimpleName(), deprecated,
          batchModule, toolsOrVisual, docLink));
    }

    records.sort(Comparator.comparing(ModuleRecord::isDeprecated)
        .thenComparing(ModuleRecord::isBatchModule, reverseOrder())
        .thenComparing(ModuleRecord::isToolOrVisual, reverseOrder())
        .thenComparing(ModuleRecord::name));

    JsonUtils.MAPPER.writer().writeValue(file, records);
  }

  List<ModuleParameters> listAllModules() {
    List<ModuleParameters> allModules = new ArrayList<>();
    try {
      ClassPath classPath = ClassPath.from(MZmineModule.class.getClassLoader());
      classPath.getTopLevelClassesRecursive("io.github.mzmine.modules").forEach(classInfo -> {
        try {
          final Class<?> clazz = classInfo.load();
          if (!MZmineModule.class.isAssignableFrom(clazz)) {
            return;
          }
          
          Object o = clazz.getDeclaredConstructor().newInstance();
          if (o instanceof MZmineModule mod) {
            ParameterSet parameterSet = null;
            try {
              parameterSet = mod.getParameterSetClass().getDeclaredConstructor().newInstance();
            } catch (Throwable _) {
            }

            allModules.add(new ModuleParameters(mod, parameterSet));
          }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 ExceptionInInitializerError | NoSuchMethodException | NullPointerException |
                 NoClassDefFoundError | IllegalStateException e) {
          //               can go silent
          //              logger.log(Level.INFO, e.getMessage(), e);
        }
      });
    } catch (IOException e) {
    }
    return allModules;
  }


  record ModuleParameters(MZmineModule module, @Nullable ParameterSet parameterSet) {

  }


  record ModuleRecord(String name, String className, boolean isDeprecated, boolean isBatchModule,
                      boolean isToolOrVisual, String docLink) {

  }
}
