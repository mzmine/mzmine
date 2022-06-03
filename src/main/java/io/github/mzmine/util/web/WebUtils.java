/*
 * Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.util.web;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.ParseException;

public class WebUtils {

  private static final Logger logger = Logger.getLogger(WebUtils.class.getName());

  public static void openURL(String url) {
    if (Desktop.isDesktopSupported() && !url.isBlank()) {
      try {
        Desktop.getDesktop().browse(new URI(url));
      } catch (ParseException | IOException | URISyntaxException e) {
        logger.log(Level.WARNING, "Cannot open browser for URL: " + url, e);
      }
    }
  }
}
