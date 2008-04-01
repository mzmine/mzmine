/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.desktop.impl;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import net.sf.mzmine.io.PreloadLevel;
import net.sf.mzmine.main.MZmineCore;

import org.simplericity.macify.eawt.Application;
import org.simplericity.macify.eawt.ApplicationEvent;
import org.simplericity.macify.eawt.ApplicationListener;
import org.simplericity.macify.eawt.DefaultApplication;

/**
 * This class provides better support for Mac OS X using the macify library
 * http://simplericity.org/macify/
 */
public class MacSupport implements ApplicationListener {

    static void initMacSupport() {

        // Create an instance of this class to serve as ApplicationListener
        MacSupport support = new MacSupport();

        // Create an abstract Application
        Application application = new DefaultApplication();
        application.addApplicationListener(support);

        try {
            BufferedImage MZmineIcon = ImageIO.read(new File(
                    "icons/MZmineIcon.png"));
            application.setApplicationIconImage(MZmineIcon);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void handleAbout(ApplicationEvent event) {
        AboutDialog dialog = new AboutDialog();
        dialog.setVisible(true);
        event.setHandled(true);
    }

    public void handleOpenApplication(ApplicationEvent event) {
        // ignore
    }

    public void handleOpenFile(ApplicationEvent event) {
        File file = new File(event.getFilename());
        MZmineCore.getIOController().openFiles(new File[] { file },
                PreloadLevel.NO_PRELOAD);
        event.setHandled(true);
    }

    public void handlePreferences(ApplicationEvent event) {
        // ignore
    }

    public void handlePrintFile(ApplicationEvent event) {
        // ignore
    }

    public void handleQuit(ApplicationEvent event) {
        MZmineCore.exitMZmine();
        event.setHandled(false);
    }

    public void handleReopenApplication(ApplicationEvent event) {
        // ignore
    }

}
