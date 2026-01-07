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

import com.google.common.reflect.ClassPath;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import testutils.MZmineTestUtil;

public class CheckModuleDocsTest {

  private static final Logger logger = Logger.getLogger(CheckModuleDocsTest.class.getName());

  @Test
  @Disabled
  void testDocumentationLinks() {
//    MZmineTestUtil.startMzmineCore();

    try {
      ClassPath classPath = ClassPath.from(MZmineModule.class.getClassLoader());
      classPath.getTopLevelClassesRecursive("io.github.mzmine.modules").forEach(classInfo -> {
        try {
          Object o = classInfo.load().getDeclaredConstructor().newInstance();
          if (o instanceof MZmineModule mod) {
            final ParameterSet parameterSet = mod.getParameterSetClass().getDeclaredConstructor()
                .newInstance();
            final String helpUrl = parameterSet.getOnlineHelpUrl();
            if (helpUrl == null) {
              logger.info("Module\t" + mod.getName() + "\t" + mod.getClass().getName()
                  + "\thas no documentation link");
            }
          }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 ExceptionInInitializerError | NoSuchMethodException | NullPointerException |
                 NoClassDefFoundError | IllegalStateException e) {
          //               can go silent
          //              logger.log(Level.INFO, e.getMessage(), e);
        }
      });
    } catch (IOException e) {
      logger.severe("Cannot instantiate classPath for DataType.class. Cannot load projects.");
    }
  }
}
