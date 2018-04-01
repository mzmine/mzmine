/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.desktop.impl;

import java.awt.Font;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import javafx.embed.swing.JFXPanel;
import net.sf.mzmine.util.components.MultiLineToolTipUI;

/**
 * This class has just a single method which sets desktop (Swing) properties for MZmine 2
 */
public class DesktopSetup {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  public void init() {

    // Get tooltip manager instance
    ToolTipManager tooltipManager = ToolTipManager.sharedInstance();

    // Set tooltip display after 10 ms
    tooltipManager.setInitialDelay(10);

    // Never dismiss tooltips
    tooltipManager.setDismissDelay(Integer.MAX_VALUE);

    // Prepare default fonts
    Font defaultFont = new Font("SansSerif", Font.PLAIN, 13);
    Font smallFont = new Font("SansSerif", Font.PLAIN, 11);
    Font tinyFont = new Font("SansSerif", Font.PLAIN, 10);

    // Set default font
    Enumeration<Object> keys = UIManager.getDefaults().keys();
    while (keys.hasMoreElements()) {
      Object key = keys.nextElement();
      Object value = UIManager.get(key);
      if (value instanceof Font)
        UIManager.put(key, defaultFont);
    }

    // Set small font where necessary
    UIManager.put("List.font", smallFont);
    UIManager.put("Table.font", smallFont);
    UIManager.put("ToolTip.font", tinyFont);

    // Set platform look & feel
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      // ignore
    }

    // Set tooltip UI to support multi-line tooltips
    UIManager.put("ToolTipUI", MultiLineToolTipUI.class.getName());
    UIManager.put(MultiLineToolTipUI.class.getName(), MultiLineToolTipUI.class);

    // If we are running on Mac OS X, we can setup some Mac-specific
    // features. The MacSpecificSetup class is located in
    // lib/macspecificsetup.jar, including source code. Using reflection we
    // prevent the MacSpecificSetup class to be loaded on other platforms
    // than Mac
    if (System.getProperty("os.name").toLowerCase().contains("mac")) {
      try {
        String className = "MacSpecificSetup";
        Class<?> macSetupClass = Class.forName(className);
        Object macSetup = macSetupClass.newInstance();
        Method setupMethod = macSetupClass.getMethod("init");
        setupMethod.invoke(macSetup, new Object[0]);
      } catch (Throwable e) {
        logger.log(Level.WARNING, "Error setting mac-specific properties", e);
      }
    }

    // Let the OS decide the location of new windows. Otherwise, all windows
    // would appear at the top left corner by default.
    System.setProperty("java.awt.Window.locationByPlatform", "true");

    // Initialize JavaFX
    logger.finest("Initializing the JavaFX subsystem by creating a JFXPanel instance");
    @SuppressWarnings("unused")
    JFXPanel dummyPanel = new JFXPanel();

  }

}
