/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.batchmode;

import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineProcessingModule;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class BatchModeModulesList {

  public static final List<Class<? extends MZmineProcessingModule>> MODULES_DYNAMIC;
  private static final Logger logger = Logger.getLogger(BatchModeModulesList.class.getName());

  static {
    List<Class<? extends MZmineProcessingModule>> modulesList = new ArrayList<>();

    try {
      ClassPath path = ClassPath.from(MZmineModule.class.getClassLoader());
      for (ClassInfo classInfo : path.getTopLevelClassesRecursive("io.github.mzmine.modules")) {
        if (classInfo.getSimpleName().contains("Module")) {
          final Class<?> clazz = classInfo.load();

          // skip abstract classes and interfaces
          if (Modifier.isAbstract(clazz.getModifiers()) || Modifier.isInterface(
              clazz.getModifiers())) {
            continue;
          }

          for (Class<?> anInterface : clazz.getInterfaces()) {
            // check if the correct interface is implemented
            if (anInterface.equals(MZmineProcessingModule.class)) {
              try {
                final Object o = clazz.getDeclaredConstructor().newInstance();

                // don't add deprecated modules
                if (o instanceof DeprecatedModule) {
                  logger.finest(() -> "Not adding deprecated module " + clazz.getName()
                      + " to batch mode modules list.");
                  break;
                }
                logger.finest(
                    () -> "Adding module " + clazz.getName() + " to batch mode modules list");
                modulesList.add((Class<? extends MZmineProcessingModule>) o.getClass());
              } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                // silent
              }
              break;
            }
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    MODULES_DYNAMIC = Collections.unmodifiableList(modulesList);
  }

  private BatchModeModulesList() {
  }
}
