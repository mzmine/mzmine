/*
    Copyright 2005 VTT Biotechnology

    This file is part of MZmine.

    MZmine is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    MZmine is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MZmine; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/
package net.sf.mzmine.util;


public class Logger {

	private static final int OUTPUT_DISABLED = 0;
	private static final int OUTPUT_ONSCREEN = 1;
	private static final int OUTPUT_TOFILE = 2;

	private static int outputMode = OUTPUT_DISABLED;

	public static void setOutputOnScreen() {
		outputMode = OUTPUT_ONSCREEN;
	}

	public static void disableOutput() {
		outputMode = OUTPUT_DISABLED;
	}

	public static void put(String message) {
		if (outputMode==OUTPUT_ONSCREEN) {
			System.out.println(message);
		}
	}

	public static void putFatal(String message) {
		if ((outputMode==OUTPUT_ONSCREEN) || (outputMode==OUTPUT_DISABLED)) {
			System.out.println(message);
		}
	}

}