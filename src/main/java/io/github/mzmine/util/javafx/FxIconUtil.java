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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.util.javafx;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;

import javafx.scene.image.Image;

public class FxIconUtil {

  public static @Nonnull Image loadImageFromResources(final @Nonnull String resourcePath) {
    final InputStream iconResource =
        FxIconUtil.class.getClassLoader().getResourceAsStream(resourcePath);
    if (iconResource == null)
      throw new IllegalArgumentException("Could not find an icon file at path " + resourcePath);
    final Image icon = new Image(iconResource);
    try {
      iconResource.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return icon;
  }

}
