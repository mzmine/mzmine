/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.desktop.preferences;

import java.awt.Window;
import java.text.DecimalFormat;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalModuleParameter;
import net.sf.mzmine.parameters.parametertypes.WindowSettingsParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.util.ExitCode;

import org.w3c.dom.Element;

public class MZminePreferences extends SimpleParameterSet {

    public static final NumberFormatParameter mzFormat = new NumberFormatParameter(
            "m/z value format", "Format of m/z values", false,
            new DecimalFormat("0.0000"));

    public static final NumberFormatParameter rtFormat = new NumberFormatParameter(
            "Retention time value format", "Format of retention time values",
            false, new DecimalFormat("0.00"));

    public static final NumberFormatParameter intensityFormat = new NumberFormatParameter(
            "Intensity format", "Format of intensity values", true,
            new DecimalFormat("0.0E0"));

    public static final NumOfThreadsParameter numOfThreads = new NumOfThreadsParameter();

    public static final OptionalModuleParameter proxySettings = new OptionalModuleParameter(
            "Use proxy", "Use proxy for internet connection?",
            new ProxySettings());

    public static final FileNameParameter rExecPath = new FileNameParameter(
            "R executable path",
            "Full R executable file path (If left blank, MZmine will try to find out automatically). On Windows, this should point to your R.exe file.");

    public static final BooleanParameter sendStatistics = new BooleanParameter(
            "Send anonymous statistics",
            "Allow MZmine to send anonymous statistics on the module usage?",
            true);

    public static final WindowSettingsParameter windowSetttings = new WindowSettingsParameter();

    public MZminePreferences() {
        super(new Parameter[] { mzFormat, rtFormat, intensityFormat,
                numOfThreads, proxySettings, rExecPath, sendStatistics,
                windowSetttings });
    }

    @Override
    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {

        ExitCode retVal = super.showSetupDialog(parent, valueCheckRequired);

        if (retVal == ExitCode.OK) {

            // Update proxy settings
            updateSystemProxySettings();

            // Repaint windows to update number formats
            MZmineCore.getDesktop().getMainWindow().repaint();
        }

        return retVal;
    }

    public void loadValuesFromXML(Element xmlElement) {
        super.loadValuesFromXML(xmlElement);
        updateSystemProxySettings();
    }

    private void updateSystemProxySettings() {
        // Update system proxy settings
        Boolean proxyEnabled = getParameter(proxySettings).getValue();
        if ((proxyEnabled != null) && (proxyEnabled)) {
            ParameterSet proxyParams = getParameter(proxySettings)
                    .getEmbeddedParameters();
            String address = proxyParams
                    .getParameter(ProxySettings.proxyAddress).getValue();
            String port = proxyParams.getParameter(ProxySettings.proxyPort)
                    .getValue();
            System.setProperty("http.proxySet", "true");
            System.setProperty("http.proxyHost", address);
            System.setProperty("http.proxyPort", port);
        } else {
            System.clearProperty("http.proxySet");
            System.clearProperty("http.proxyHost");
            System.clearProperty("http.proxyPort");
        }
    }

}
