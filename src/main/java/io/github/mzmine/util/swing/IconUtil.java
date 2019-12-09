/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.util.swing;

import java.awt.Image;
import java.net.URL;

import javax.annotation.Nonnull;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class IconUtil {

    public static @Nonnull ImageIcon loadIconFromResources(
            final @Nonnull String resourcePath) {
        final URL iconResourcePath = IconUtil.class.getClassLoader()
                .getResource(resourcePath);
        if (iconResourcePath == null)
            throw new IllegalArgumentException(
                    "Could not find an icon file at path " + resourcePath);
        final ImageIcon icon = new ImageIcon(iconResourcePath);
        return icon;
    }

    public static @Nonnull Icon loadIconFromResources(
            final @Nonnull String resourcePath, final int width) {
        final ImageIcon icon = loadIconFromResources(resourcePath);
        final ImageIcon scaledIcon = scaled(icon, width);
        return scaledIcon;
    }

    public static @Nonnull ImageIcon scaled(final ImageIcon icon,
            final int width) {
        int height = Math.round(
                icon.getIconHeight() / (float) icon.getIconWidth() * width);
        Image image = icon.getImage();
        Image newimg = image.getScaledInstance(width, height,
                java.awt.Image.SCALE_SMOOTH);
        return new ImageIcon(newimg);
    }

}
