/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.datamodel.features.types;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hold single instance of all data types
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class DataTypes {

  private static final Logger logger = Logger.getLogger(DataTypes.class.getName());
  public static ConcurrentHashMap<Class<? extends DataType>, DataType> TYPES = new ConcurrentHashMap<>();

  public static <T> DataType<T> get(Class<? extends DataType<T>> clazz) {
    return TYPES.computeIfAbsent(clazz, key -> {
      try {
        return key.getDeclaredConstructor().newInstance();
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
        logger.log(Level.SEVERE, "Cannot create instance of datatype: " + key.getName(), e);
        return null;
      }
    });
  }

}
