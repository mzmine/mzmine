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

package io.github.mzmine.datamodel.features.types;

import com.google.common.reflect.ClassPath;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * Hold single instance of all data types
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class DataTypes {

  private static final Logger logger = Logger.getLogger(DataTypes.class.getName());

  // map class to instance
  public static final HashMap<Class<? extends DataType>, DataType> TYPES = new HashMap<>();
  // map unique ID to instance
  private static final HashMap<String, DataType<?>> map = new HashMap<>();

  static {
    try {
      ClassPath classPath = ClassPath.from(DataType.class.getClassLoader());
      classPath.getTopLevelClassesRecursive("io.github.mzmine.datamodel.features.types")
          .forEach(classInfo -> {
            try {
              Object o = classInfo.load().getDeclaredConstructor().newInstance();
              if (o instanceof DataType dt) {
                map.put(dt.getUniqueID(), dt);
                TYPES.put(dt.getClass(), dt);
              }
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
              //               can go silent
              //              logger.log(Level.INFO, e.getMessage(), e);
            }
          });
    } catch (IOException e) {
      logger.severe("Cannot instantiate classPath for DataType.class. Cannot load projects.");
    }
  }

  private DataTypes() {
  }

  @Nullable
  public static DataType<?> getTypeForId(String uniqueId) {
    return map.get(uniqueId);
  }

  public static <T> DataType<T> get(Class<? extends DataType<T>> clazz) {
    return TYPES.get(clazz);
  }

  public static boolean isMainAnnotation(DataType<?> dataType) {
    return dataType instanceof AnnotationType && dataType instanceof SubColumnsFactory;
  }
}
