/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

/* Code created was by or on behalf of Syngenta and is released under the open source license in use for the
 * pre-existing code or project. Syngenta does not assert ownership or copyright any over pre-existing work.
 */

package net.sf.mzmine.util;

import java.util.logging.Logger;

import org.rosuda.JRI.RMainLoopCallbacks;
import org.rosuda.JRI.Rengine;

/**
 * Utilities for interfacing with R.
 * 
 * @author $Author$
 * @version $Revision$
 */
public class RUtilities {

    // Logger.
    private static final Logger LOG = Logger.getLogger(RUtilities.class
            .getName());

    /**
     * R semaphore - all usage of R engine must be synchronized using this
     * semaphore.
     */
    public static final Object R_SEMAPHORE = new Object();

    // An R Engine singleton.
    private static Rengine rEngine = null;

    /**
     * Utility class - no public access.
     */
    private RUtilities() {
        // no public access.
    }

    /**
     * Gets the R Engine.
     * 
     * @return the R Engine - creating it if necessary.
     */
    public static Rengine getREngine() {

        synchronized (R_SEMAPHORE) {

            if (rEngine == null) {

                try {

                    LOG.finest("Checking R Engine.");

                    /*
                     * For some reason if we run Rengine.versionCheck() and R is
                     * not installed, it will crash the JVM. This was observed
                     * at least on Windows and Mac OS X. However, if we call
                     * System.loadLibrary("jri") before calling Rengine class,
                     * the crash is avoided and we can catch the
                     * UnsatisfiedLinkError properly.
                     */
                    System.loadLibrary("jri");

                    if (!Rengine.versionCheck()) {
                        throw new IllegalStateException("JRI version mismatch");
                    }

                } catch (UnsatisfiedLinkError error) {
                    throw new IllegalStateException(
                            "Could not start R. Please check if R is installed and path to the "
                                    + "libraries is set properly in the startMZmine script.");
                }

                LOG.finest("Creating R Engine.");
                rEngine = new Rengine(new String[] { "--vanilla" }, false,
                        new LoggerConsole());

                LOG.finest("Rengine created, waiting for R.");
                if (!rEngine.waitForR()) {
                    throw new IllegalStateException("Could not start R");
                }

            }
            return rEngine;
        }
    }

    /**
     * Logs all output.
     */
    private static class LoggerConsole implements RMainLoopCallbacks {
        @Override
        public void rWriteConsole(final Rengine re, final String text,
                final int oType) {
            LOG.finest(text);
        }

        @Override
        public void rBusy(final Rengine re, final int which) {
            LOG.finest("rBusy(" + which + ')');
        }

        @Override
        public String rReadConsole(final Rengine re, final String prompt,
                final int addToHistory) {
            return null;
        }

        @Override
        public void rShowMessage(final Rengine re, final String message) {
            LOG.finest("rShowMessage \"" + message + '\"');
        }

        @Override
        public String rChooseFile(final Rengine re, final int newFile) {
            return null;
        }

        @Override
        public void rFlushConsole(final Rengine re) {
        }

        @Override
        public void rLoadHistory(final Rengine re, final String filename) {
        }

        @Override
        public void rSaveHistory(final Rengine re, final String filename) {
        }
    }
}
