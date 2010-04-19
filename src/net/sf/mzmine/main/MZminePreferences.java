/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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

package net.sf.mzmine.main;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.util.Iterator;

import net.sf.mzmine.data.StorableParameterSet;
import net.sf.mzmine.desktop.impl.MainWindow;
import net.sf.mzmine.util.NumberFormatter;
import net.sf.mzmine.util.NumberFormatter.FormatterType;

import org.dom4j.Element;

/**
 * 
 */
public class MZminePreferences implements StorableParameterSet {

	public static final String FORMAT_ELEMENT_NAME = "format";
	public static final String FORMAT_TYPE_ATTRIBUTE_NAME = "type";
	public static final String FORMAT_TYPE_ATTRIBUTE_MZ = "m/z";
	public static final String FORMAT_TYPE_ATTRIBUTE_RT = "Retention time";
	public static final String FORMAT_TYPE_ATTRIBUTE_INT = "Intensity";
	public static final String MAINWINDOW_ELEMENT_NAME = "mainwindow";
	public static final String X_ELEMENT_NAME = "x";
	public static final String Y_ELEMENT_NAME = "y";
	public static final String WIDTH_ELEMENT_NAME = "width";
	public static final String HEIGHT_ELEMENT_NAME = "height";
	public static final String THREADS_ELEMENT_NAME = "threads";
	public static final String PROXY = "proxy_settings";
	public static final String PROXY_ADDRESS = "proxy_address";
	public static final String PROXY_PORT = "proxy_port";

	public static final int MAXIMIZED = -1;

	private NumberFormatter mzFormat, rtFormat, intensityFormat;
	private int mainWindowX, mainWindowY, mainWindowWidth, mainWindowHeight;

	private boolean autoNumberOfThreads = true;
	private int manualNumberOfThreads = 2;

	private boolean proxyServer = false;
	private String proxyAddress = "";
	private String proxyPort = "";

	public MZminePreferences() {
		this.mzFormat = new NumberFormatter(FormatterType.NUMBER, "0.000");
		this.rtFormat = new NumberFormatter(FormatterType.TIME, "m:ss");
		this.intensityFormat = new NumberFormatter(FormatterType.NUMBER,
				"0.00E0");
	}

	/**
	 * @return Returns the intensityFormat.
	 */
	public NumberFormatter getIntensityFormat() {
		return intensityFormat;
	}

	/**
	 * @return Returns the mzFormat.
	 */
	public NumberFormatter getMZFormat() {
		return mzFormat;
	}

	/**
	 * @return Returns the rtFormat.
	 */
	public NumberFormatter getRTFormat() {
		return rtFormat;
	}

	/**
	 * @see net.sf.mzmine.data.StorableParameterSet#exportValuesToXML(org.dom4j.Element)
	 */
	public void exportValuesToXML(Element element) {

		MainWindow mainWindow = (MainWindow) MZmineCore.getDesktop();
		Point location = mainWindow.getLocation();
		mainWindowX = location.x;
		mainWindowY = location.y;
		int state = mainWindow.getExtendedState();
		Dimension size = mainWindow.getSize();
		if ((state & Frame.MAXIMIZED_HORIZ) != 0)
			mainWindowWidth = MAXIMIZED;
		else
			mainWindowWidth = size.width;
		if ((state & Frame.MAXIMIZED_VERT) != 0)
			mainWindowHeight = MAXIMIZED;
		else
			mainWindowHeight = size.height;

		Element mzFormatElement = element.addElement(FORMAT_ELEMENT_NAME);
		mzFormatElement.addAttribute(FORMAT_TYPE_ATTRIBUTE_NAME,
				FORMAT_TYPE_ATTRIBUTE_MZ);
		mzFormat.exportToXML(mzFormatElement);

		Element rtFormatElement = element.addElement(FORMAT_ELEMENT_NAME);
		rtFormatElement.addAttribute(FORMAT_TYPE_ATTRIBUTE_NAME,
				FORMAT_TYPE_ATTRIBUTE_RT);
		rtFormat.exportToXML(rtFormatElement);

		Element intensityFormatElement = element
				.addElement(FORMAT_ELEMENT_NAME);
		intensityFormatElement.addAttribute(FORMAT_TYPE_ATTRIBUTE_NAME,
				FORMAT_TYPE_ATTRIBUTE_INT);
		intensityFormat.exportToXML(intensityFormatElement);

		Element mainWindowElement = element.addElement(MAINWINDOW_ELEMENT_NAME);
		mainWindowElement.addElement(X_ELEMENT_NAME).setText(
				String.valueOf(mainWindowX));
		mainWindowElement.addElement(Y_ELEMENT_NAME).setText(
				String.valueOf(mainWindowY));
		mainWindowElement.addElement(WIDTH_ELEMENT_NAME).setText(
				String.valueOf(mainWindowWidth));
		mainWindowElement.addElement(HEIGHT_ELEMENT_NAME).setText(
				String.valueOf(mainWindowHeight));

		Element threadsElement = element.addElement(THREADS_ELEMENT_NAME);
		if (autoNumberOfThreads)
			threadsElement.addAttribute("auto", "true");
		threadsElement.setText(String.valueOf(manualNumberOfThreads));


		//Proxy Settings
		Element proxyElement = element.addElement(PROXY);
		if (proxyServer)
			proxyElement.addAttribute("activated", "true");

		Element addressElement = proxyElement.addElement(PROXY_ADDRESS);
		addressElement.setText(String.valueOf(proxyAddress));
		Element portElement = proxyElement.addElement(PROXY_PORT);
		portElement.setText(String.valueOf(proxyPort));

	}

	/**
	 * @see net.sf.mzmine.data.StorableParameterSet#importValuesFromXML(org.dom4j.Element)
	 */
	public void importValuesFromXML(Element element) {
		Iterator i = element.elements(FORMAT_ELEMENT_NAME).iterator();
		while (i.hasNext()) {
			Element formatElement = (Element) i.next();
			if (formatElement.attributeValue(FORMAT_TYPE_ATTRIBUTE_NAME)
					.equals(FORMAT_TYPE_ATTRIBUTE_MZ))
				mzFormat.importFromXML(formatElement);
			if (formatElement.attributeValue(FORMAT_TYPE_ATTRIBUTE_NAME)
					.equals(FORMAT_TYPE_ATTRIBUTE_RT))
				rtFormat.importFromXML(formatElement);
			if (formatElement.attributeValue(FORMAT_TYPE_ATTRIBUTE_NAME)
					.equals(FORMAT_TYPE_ATTRIBUTE_INT))
				intensityFormat.importFromXML(formatElement);
		}

		Element mainWindowElement = element.element(MAINWINDOW_ELEMENT_NAME);
		if (mainWindowElement != null) {
			mainWindowX = Integer.parseInt(mainWindowElement
					.elementText(X_ELEMENT_NAME));
			mainWindowY = Integer.parseInt(mainWindowElement
					.elementText(Y_ELEMENT_NAME));
			mainWindowWidth = Integer.parseInt(mainWindowElement
					.elementText(WIDTH_ELEMENT_NAME));
			mainWindowHeight = Integer.parseInt(mainWindowElement
					.elementText(HEIGHT_ELEMENT_NAME));
		}

		MainWindow mainWindow = (MainWindow) MZmineCore.getDesktop();
		if (mainWindowX > 0)
			mainWindow.setLocation(mainWindowX, mainWindowY);

		if ((mainWindowWidth > 0) || (mainWindowHeight > 0))
			mainWindow.setSize(mainWindowWidth, mainWindowHeight);

		int newState = Frame.NORMAL;
		if (mainWindowWidth == MAXIMIZED)
			newState |= Frame.MAXIMIZED_HORIZ;

		if (mainWindowHeight == MAXIMIZED)
			newState |= Frame.MAXIMIZED_VERT;

		mainWindow.setExtendedState(newState);

		Element threadsElement = element.element(THREADS_ELEMENT_NAME);
		if (threadsElement != null) {
			autoNumberOfThreads = (threadsElement.attributeValue("auto") != null);
			manualNumberOfThreads = Integer.parseInt(threadsElement.getText());
		}

		//Proxy settings
		Element proxyElement = element.element(PROXY);
		if (proxyElement != null) {
			proxyServer = (proxyElement.attributeValue("activated") != null);

			Element AddressElement = proxyElement.element(PROXY_ADDRESS);
			if (AddressElement != null) {				
				setProxyAddress(AddressElement.getText());
			}

			Element portElement = proxyElement.element(PROXY_PORT);
			if (portElement != null) {				
				setProxyPort(portElement.getText());
			}
		}

	}

	public MZminePreferences clone() {
		return new MZminePreferences();
	}

	public boolean isAutoNumberOfThreads() {
		return autoNumberOfThreads;
	}

	public void setAutoNumberOfThreads(boolean autoNumberOfThreads) {
		this.autoNumberOfThreads = autoNumberOfThreads;
	}

	public int getManualNumberOfThreads() {
		return manualNumberOfThreads;
	}

	public void setManualNumberOfThreads(int manualNumberOfThreads) {
		this.manualNumberOfThreads = manualNumberOfThreads;
	}

	// Proxy settings	
	public boolean isProxy(){
		return proxyServer;
	}

	public void setProxy(boolean proxy){
		this.proxyServer = proxy;
		if(!proxy){
			System.clearProperty("http.proxyHost");
			System.clearProperty("http.proxyPort");
		}
	}

	public String getProxyAddress(){		
		return proxyAddress;
	}

	public void setProxyAddress(String address){
		this.proxyAddress = address;
		if(isProxy()){
			System.setProperty("http.proxyHost", address);
		}
	}

	public String getProxyPort(){		
		return proxyPort;
	}

	public void setProxyPort(String port){
		this.proxyPort = port;
		if(isProxy()){
			System.setProperty("http.proxyPort", port);
		}
	}


}
