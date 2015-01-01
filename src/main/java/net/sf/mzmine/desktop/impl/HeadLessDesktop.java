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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.desktop.impl;

import java.awt.Color;
import java.awt.Window;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.swing.JFrame;
import javax.swing.event.TreeModelListener;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.util.ExitCode;

public class HeadLessDesktop implements Desktop {

    private static final String MODULE_NAME = "Desktop";

    private Logger logger = Logger.getLogger(this.getClass().getName());

    @Override
    public JFrame getMainWindow() {
	return null;
    }

    @Override
    public void setStatusBarText(String text) {
	throw new UnsupportedOperationException();
    }

    @Override
    public void setStatusBarText(String text, Color textColor) {
    }

    @Override
    public void displayMessage(Window window, String msg) {
	logger.info(msg);
    }

    @Override
    public void displayMessage(Window window, String title, String msg) {
	logger.info(msg);
    }

    @Override
    public void displayErrorMessage(Window window, String msg) {
	logger.severe(msg);
    }

    @Override
    public void displayErrorMessage(Window window, String title, String msg) {
	logger.severe(msg);
    }

    @Override
    public void displayException(Window window, Exception e) {
	e.printStackTrace();
    }

    @Override
    public RawDataFile[] getSelectedDataFiles() {
	throw new UnsupportedOperationException();
    }

    @Override
    public PeakList[] getSelectedPeakLists() {
	throw new UnsupportedOperationException();
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
	return SimpleParameterSet.class;
    }

    @Override
    public @Nonnull String getName() {
	return MODULE_NAME;
    }

    @Override
    public @Nonnull ExitCode exitMZmine() {
	System.exit(0);
	return ExitCode.OK;
    }

    @Override
    public void addRawDataTreeListener(TreeModelListener listener) {
	// TODO Auto-generated method stub

    }

    @Override
    public void addPeakListTreeListener(TreeModelListener listener) {
	// TODO Auto-generated method stub

    }

    @Override
    public void removeRawDataTreeListener(TreeModelListener listener) {
	// TODO Auto-generated method stub

    }

    @Override
    public void removePeakListTreeListener(TreeModelListener listener) {
	// TODO Auto-generated method stub

    }

}
