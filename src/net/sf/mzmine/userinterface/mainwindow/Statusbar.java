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
package net.sf.mzmine.userinterface.mainwindow;
import java.awt.Dimension;
import java.awt.Graphics;
import java.text.DecimalFormat;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EtchedBorder;

import net.sf.mzmine.obsoletedatastructures.RawDataAtClient;
import net.sf.mzmine.util.FormatCoordinates;


public class Statusbar extends JPanel {

	private String statusText;
	private int currentSlot;
	private int fullSlot;


	private int cursorScan;
	private double cursorMZ;


	private JPanel statusTextPanel;
	private JLabel statusTextLabel;
	private JProgressBar statusProgBar;

	private JPanel coordsScanPanel;
	private JLabel coordsScanLabel;

	private JPanel coordsMZPanel;
	private JLabel coordsMZLabel;

	private String coordsScanLabelTxt;
	private String coordsMZLabelTxt;

	private MainWindow mainWin;

	private double coordsMZ;
	private double coordsRT;

	private DecimalFormat mzLabelFormat;
	private DecimalFormat scanLabelFormat;

	private final int statusBarHeight = 25;


	public Statusbar(MainWindow _mainWin) {
		super();

		mainWin = _mainWin;

		// setLayout(new BorderLayout());

		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setBorder(new EtchedBorder());

		statusTextPanel = new JPanel();
		statusTextPanel.setLayout(new BoxLayout(statusTextPanel, BoxLayout.X_AXIS));
		statusTextPanel.setBorder(new EtchedBorder(EtchedBorder.RAISED));

		statusProgBar = new JProgressBar();
		statusProgBar.setMinimumSize(new Dimension(100,statusBarHeight));
		statusProgBar.setPreferredSize(new Dimension(3200,statusBarHeight));
		statusProgBar.setVisible(false);

		statusTextLabel = new JLabel();
		statusTextLabel.setMinimumSize(new Dimension(100,statusBarHeight));
		statusTextLabel.setPreferredSize(new Dimension(3200,statusBarHeight));

		statusTextPanel.add(Box.createRigidArea(new Dimension(5,statusBarHeight)));
		statusTextPanel.add(statusTextLabel);
		statusTextPanel.add(statusProgBar);
		statusTextPanel.add(Box.createRigidArea(new Dimension(5,statusBarHeight)));


		//add(statusTextPanel, BorderLayout.WEST);
		//add(Box.createRigidArea(new Dimension(5,statusBarHeight)));
		add(statusTextPanel);
		//add(Box.createRigidArea(new Dimension(5,statusBarHeight)));
		//add(Box.createHorizontalGlue());


		coordsScanPanel = new JPanel();
		coordsScanPanel.setLayout(new BoxLayout(coordsScanPanel, BoxLayout.X_AXIS));
		coordsScanPanel.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		coordsScanPanel.setPreferredSize(new Dimension(100,statusBarHeight));

		coordsScanLabel = new JLabel();
		coordsScanLabel.setMinimumSize(new Dimension(100,statusBarHeight));
		coordsScanLabel.setPreferredSize(new Dimension(100,statusBarHeight));
		coordsScanLabel.setText("RT: ");

		// coordsScanPanel.add(Box.createHorizontalGlue());
		coordsScanPanel.add(Box.createRigidArea(new Dimension(5,statusBarHeight)));
		coordsScanPanel.add(coordsScanLabel);
		coordsScanPanel.add(Box.createRigidArea(new Dimension(5,statusBarHeight)));

		add(Box.createHorizontalGlue());
		add(coordsScanPanel);


		coordsMZPanel = new JPanel();
		coordsMZPanel.setLayout(new BoxLayout(coordsMZPanel, BoxLayout.X_AXIS));
		coordsMZPanel.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		coordsMZPanel.setPreferredSize(new Dimension(100,statusBarHeight));

		coordsMZLabel = new JLabel();
		coordsMZLabel.setMinimumSize(new Dimension(100,statusBarHeight));
		coordsMZLabel.setPreferredSize(new Dimension(100,statusBarHeight));
		coordsMZLabel.setText("M/Z: ");

		coordsMZPanel.add(Box.createRigidArea(new Dimension(5,statusBarHeight)));
		coordsMZPanel.add(coordsMZLabel);
		coordsMZPanel.add(Box.createRigidArea(new Dimension(5,statusBarHeight)));

		add(coordsMZPanel);

		clearCursorPosition();

		//add(coordsPanel, BorderLayout.EAST);



	}


	/**
	 * Set the text displayed in status bar
	 *
	 * @param t		Text for status bar
	 */
	public void setStatusText(String _statusText) {
		statusText = _statusText;
		statusTextLabel.setText(statusText);
		// statusTextPanel.update(statusTextPanel.getGraphics());


	}


	/**
	 * Sets the text displayed in status bar and progress indicators position
	 *
	 * @param t		Text for status bar
	 * @param currentSlot	Current position of progress indicator
	 * @param fullSlot		Last position of progress indicator
	 */
	public void setStatusProgBar(int _currentSlot, int _fullSlot) {
		//if (!statusProgBar.isVisible()) {
		statusProgBar.setVisible(true);
		//}
		statusProgBar.setMaximum(_fullSlot);
		statusProgBar.setValue(_currentSlot);

		// statusTextPanel.update(statusTextPanel.getGraphics());
	}

	public void disableProgBar() {
		statusProgBar.setVisible(false);
	}


	/**
	 * Sets the cursor position displayed in status bar to current cursor position in the given run
	 *
	 * @param msRun	Raw data file whose cursor location will be displayed on status bar
	 */
	public void setCursorPosition(RawDataAtClient rawData) {
		coordsMZ = rawData.getCursorPositionMZ();;
		coordsRT = rawData.getScanTime(rawData.getCursorPositionScan());
		repaint();
	}

	/**
	 * Sets the cursor position displayed in status bar to given mz, rt values
	 *
	 * @param mz	M/Z coordinate location
	 * @param rt	RT coordinate location
	 */
	public void setCursorPosition(double mz, double rt) {
		coordsMZ = mz;
		coordsRT = rt;
		repaint();
	}

	/**
	 * Sets the cursor position displayed in status bar to given mz, rt values
	 *
	 * @param mz	M/Z coordinate location
	 */
	public void setCursorPositionMZ(double mz) {
		coordsMZ = mz;
		repaint();
	}

	/**
	 * Sets the cursor position displayed in status bar to given mz, rt values
	 *
	 * @param rt	RT coordinate location
	 */
	public void setCursorPositionRT(double rt) {
		coordsRT = rt;
		repaint();
	}



	public void clearCursorPosition() {
		coordsMZ = -1;
		coordsRT = -1;
		repaint();
	}

	public void paint(Graphics g) {

		super.paint(g);

		if (coordsRT>=0)	{ coordsScanLabel.setText("RT: " + FormatCoordinates.formatRTValue(coordsRT)); }
		else				{ coordsScanLabel.setText("RT: "); }

		if (coordsMZ>0)		{ coordsMZLabel.setText("M/Z: " + FormatCoordinates.formatMZValue(coordsMZ)); }
		else 				{ coordsMZLabel.setText("M/Z: "); }

	}


}