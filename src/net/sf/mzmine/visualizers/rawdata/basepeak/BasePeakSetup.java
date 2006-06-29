/*
 * Copyright 2006 The MZmine Development Team
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

package net.sf.mzmine.visualizers.rawdata.basepeak;

import java.text.NumberFormat;

import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.userinterface.dialogs.ParameterSetupDialog;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;
import net.sf.mzmine.visualizers.rawdata.threed.ThreeDVisualizer;

/**
 * 
 */
public class BasePeakSetup {

    public static void showSetupDialog(RawDataFile rawDataFile) {

        String[] paramNames = new String[3];
        paramNames[0] = "MS level";
        paramNames[1] = "Minimum retention time";
        paramNames[2] = "Maximum retention time";
        

        double[] paramValues = new double[3];
        paramValues[0] = 1;
        paramValues[1] = rawDataFile.getDataMinRT(1);
        paramValues[2] = rawDataFile.getDataMaxRT(1);

        NumberFormat[] numberFormats = new NumberFormat[3];
        numberFormats[0] = NumberFormat.getIntegerInstance();
        numberFormats[1] = NumberFormat.getNumberInstance();
        numberFormats[1].setMinimumFractionDigits(0);
        numberFormats[2] = NumberFormat.getNumberInstance();
        numberFormats[2].setMinimumFractionDigits(0);
        

        // Show parameter setup dialog
        MainWindow mainWin = MainWindow.getInstance();
        ParameterSetupDialog psd = new ParameterSetupDialog(mainWin,
                "Base peak visualizer parameters (" + rawDataFile + ")", paramNames,
                paramValues, numberFormats);
        psd.setVisible(true);

        // Check if user clicked Cancel-button
        if (psd.getExitCode() == -1) {
            return;
        }

        // Read parameter values from dialog
        int msLevel = (int) psd.getFieldValue(0);
        int scanNumbers[] = rawDataFile.getScanNumbers(msLevel);
        if (scanNumbers == null) {
            mainWin.displayErrorMessage("No scans at MS level " + msLevel);
            return;
        }
        
        double rtMin = psd.getFieldValue(1);
        double rtMax = psd.getFieldValue(2);
        
        if (rtMax <= rtMin) {
            mainWin.displayErrorMessage("Invalid bounds");
            return;
        }
        
        new BasePeakVisualizer(rawDataFile, msLevel, rtMin, rtMax);
        
        
    }
}
