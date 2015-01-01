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

package net.sf.mzmine.modules.visualization.threed;

import static java.awt.GridBagConstraints.CENTER;
import static java.awt.GridBagConstraints.EAST;
import static java.awt.GridBagConstraints.HORIZONTAL;
import static java.awt.GridBagConstraints.NONE;
import static java.awt.GridBagConstraints.WEST;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.GUIUtils;
import visad.Display;
import visad.DisplayRealType;
import visad.ScalarMap;
import visad.TextControl;
import visad.VisADException;
import visad.util.ColorMapWidget;
import visad.util.GMCWidget;

/**
 * 3D visualizer properties dialog
 */
public class ThreeDPropertiesDialog extends JDialog {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    // Logger.
    private static final Logger LOG = Logger
	    .getLogger(ThreeDPropertiesDialog.class.getName());

    // Dialog title.
    private static final String TITLE = "3D visualizer properties";

    // Slider maximum.
    private static final int SLIDER_MAX = 10000;

    // Maximum scale factor.
    private static final double MAX_SCALE = 2.0;

    // Empty insets.
    private static final Insets EMPTY_INSETS = new Insets(0, 0, 0, 0);

    // The display to control
    private final ThreeDDisplay display;

    /**
     * Create the dialog.
     *
     * @param display3D
     *            the 3D display to control
     */
    public ThreeDPropertiesDialog(final ThreeDDisplay display3D) {

	super(MZmineCore.getDesktop().getMainWindow(), TITLE, false);

	// Initialize.
	display = display3D;

	setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

	// color map widget.
	final ScalarMap clrMap = findMapByType(Display.RGB);
	if (clrMap != null) {
	    try {
		final ColorMapWidget mapWidget = new ColorMapWidget(clrMap);
		mapWidget.setBorder(BorderFactory
			.createTitledBorder("Color Map"));
		add(mapWidget);
	    } catch (VisADException e) {
		LOG.log(Level.WARNING, "Failed to create color map widget", e);
	    } catch (RemoteException e) {
		LOG.log(Level.WARNING, "Failed to create color map widget", e);
	    }
	}

	// Graphics mode control.
	add(createGraphicsModePanel());

	// Z-axis scaling.
	add(createZAxisPanel());

	// Label control.
	add(createLabelPanel());

	// Done button.
	GUIUtils.addButton(this, "Done", null, new ActionListener() {

	    @Override
	    public void actionPerformed(final ActionEvent e) {
		setVisible(false);
	    }
	}).setAlignmentX(Component.CENTER_ALIGNMENT);

	// Display dialog.
	pack();
	setLocationRelativeTo(MZmineCore.getDesktop().getMainWindow());
    }

    /**
     * Find a map of a given display type.
     *
     * @param type
     *            the type.
     * @return the first map added to the display of the given type, or null if
     *         no match is found.
     */
    private ScalarMap findMapByType(final DisplayRealType type) {

	ScalarMap scalarMap = null;
	for (Iterator<?> iterator = display.getMapVector().iterator(); scalarMap == null
		&& iterator.hasNext();) {
	    final ScalarMap map = (ScalarMap) iterator.next();
	    if (type.equals(map.getDisplayScalar())) {
		scalarMap = map;
	    }
	}
	return scalarMap;
    }

    // Locks widget updates.
    private boolean locked = false;

    /**
     * Creates a panel for scaling the z-axis.
     *
     * @return the newly created panel.
     */
    private JPanel createZAxisPanel() {

	final double maxIntensity = display.getMaxIntensity();
	final double linMax = MAX_SCALE * maxIntensity;
	final double logMax = MAX_SCALE * StrictMath.log10(linMax);

	// Create the slider.
	final JSlider slider = new JSlider(0, SLIDER_MAX);

	// Normalize value field.
	final JFormattedTextField textField = new JFormattedTextField(
		MZmineCore.getConfiguration().getIntensityFormat());
	textField.setColumns(8);
	textField.addPropertyChangeListener("value",
		new PropertyChangeListener() {
		    @Override
		    public void propertyChange(final PropertyChangeEvent evt) {
			final Number value = (Number) textField.getValue();
			if (value != null && !locked) {
			    try {
				locked = true;

				// Set maximum intensity.
				final double x = value.doubleValue();
				setMaximumIntensity(x);

				// Update the slider: it can operate in linear
				// or logarithmic mode.
				slider.setValue((int) ((double) SLIDER_MAX * (display
					.getUseLog10Intensity() ? StrictMath
					.log10(x) / logMax : x / linMax)));

			    } finally {
				locked = false;
			    }
			}
		    }
		});

	// Scaling widget.
	final JCheckBox checkBox = new JCheckBox("Use log10 scaling",
		display.getUseLog10Intensity());
	checkBox.setToolTipText("Applies logarithmic/linear scaling to the intensity axis");
	checkBox.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(final ActionEvent e) {
		try {
		    // Update scaling mode.
		    display.setUseLog10Intensity(checkBox.isSelected());
		    textField.setValue(null);
		    textField.setValue(maxIntensity); // ensure property change
						      // listener fires.
		} catch (VisADException ex) {
		    LOG.log(Level.WARNING, "Unable to apply log10 scaling", ex);
		} catch (RemoteException ex) {
		    LOG.log(Level.WARNING, "Unable to apply log10 scaling", ex);
		}
	    }
	});

	// Update listeners.
	slider.addChangeListener(new ChangeListener() {
	    @Override
	    public void stateChanged(final ChangeEvent e) {
		if (!locked) {

		    // Slider can operate in linear or logarithmic mode.
		    final double x = (double) slider.getValue()
			    / (double) SLIDER_MAX;
		    final double value = display.getUseLog10Intensity() ? StrictMath
			    .pow(10.0, logMax * x) : linMax * x;

		    // Set maximum intensity and synchronize text field.
		    try {
			locked = true;
			setMaximumIntensity(value);
			textField.setValue(value);
		    } finally {
			locked = false;
		    }
		}
	    }
	});

	// Set the value - this will update the slider too.
	textField.setValue(maxIntensity);

	// Labels.
	final JLabel maxLabel = new JLabel("Max");
	maxLabel.setToolTipText("The maximum value on the intensity axis");
	maxLabel.setLabelFor(slider);

	// Create the panel.
	final JPanel panel = new JPanel(new GridBagLayout());
	panel.setBorder(BorderFactory.createTitledBorder("Intensity Axis"));
	panel.add(maxLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, EAST,
		NONE, EMPTY_INSETS, 0, 0));
	panel.add(slider, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, CENTER,
		HORIZONTAL, new Insets(0, 5, 0, 5), 0, 0));
	panel.add(textField, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, WEST,
		NONE, EMPTY_INSETS, 0, 0));
	panel.add(checkBox, new GridBagConstraints(0, 1, 2, 1, 1.0, 0.0, WEST,
		NONE, new Insets(0, 5, 0, 5), 0, 0));

	return panel;
    }

    /**
     * Sets the intensity maximum.
     *
     * @param max
     *            the new maximum.
     */
    private void setMaximumIntensity(final double max) {
	try {
	    display.setIntensityRange(0.0, max);
	} catch (VisADException e) {
	    LOG.log(Level.WARNING, "Couldn't set intensity range.", e);
	} catch (RemoteException e) {
	    LOG.log(Level.WARNING, "Couldn't set intensity range.", e);
	}
    }

    /**
     * Creates a panel for controlling graphics.
     *
     * @return the panel.
     */
    private Component createGraphicsModePanel() {

	final JPanel panel = new JPanel();
	panel.setBorder(BorderFactory.createTitledBorder("Graphics Mode"));
	panel.add(new GMCWidget(display.getGraphicsModeControl()));
	return panel;
    }

    /**
     * Creates a panel for controlling labels.
     *
     * @return the newly created panel.
     */
    private Component createLabelPanel() {

	// Label font control.
	JButton fontButton = null;

	// Find the text map.
	final ScalarMap textMap = findMapByType(Display.Text);
	if (textMap != null) {
	    try {

		// Create a text control widget dialog and button to display it.
		final TextControlDialog dialog = new TextControlDialog(this,
			(TextControl) textMap.getControl());
		fontButton = new JButton("Font...");
		fontButton
			.setToolTipText("Configure label font characteristics");
		fontButton.addActionListener(new ActionListener() {

		    @Override
		    public void actionPerformed(final ActionEvent e) {
			dialog.setVisible(true);
		    }
		});
	    } catch (RemoteException e) {
		LOG.log(Level.WARNING, "Couldn't create text control widget.",
			e);
	    } catch (VisADException e) {
		LOG.log(Level.WARNING, "Couldn't create text control widget.",
			e);
	    }
	}

	// Widget for label color control.
	final JButton clrButton = new JButton("Color...");
	clrButton.setToolTipText("Configure label color");
	clrButton.addActionListener(new ActionListener() {

	    @Override
	    public void actionPerformed(final ActionEvent e) {
		final Color clr = JColorChooser.showDialog(
			ThreeDPropertiesDialog.this, "Label Color",
			display.getAnnotationColor());
		if (clr != null) {
		    try {
			display.setAnnotationColor(clr);
		    } catch (VisADException ex) {
			LOG.log(Level.WARNING, "Couldn't change label color.",
				ex);
		    } catch (RemoteException ex) {
			LOG.log(Level.WARNING, "Couldn't change label color.",
				ex);
		    }
		}
	    }
	});

	// Widget for label opacity control.
	final JCheckBox checkBox = new JCheckBox("Apply opacity map to labels",
		display.getUseAnnotationAlphaMap());
	checkBox.addActionListener(new ActionListener() {
	    @Override
	    public void actionPerformed(final ActionEvent e) {

		try {
		    display.setUseAnnotationAlphaMap(checkBox.isSelected());
		} catch (VisADException ex) {
		    LOG.log(Level.WARNING, "Couldn't change label opacity.", ex);
		} catch (RemoteException ex) {
		    LOG.log(Level.WARNING, "Couldn't change label opacity.", ex);
		}
	    }
	});

	// Create and layout the panel.
	final JPanel panel = new JPanel(new GridBagLayout());
	panel.setBorder(BorderFactory.createTitledBorder("Peak Labels"));
	if (fontButton != null) {
	    panel.add(fontButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
		    CENTER, NONE, EMPTY_INSETS, 0, 0));
	}
	panel.add(clrButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
		CENTER, NONE, EMPTY_INSETS, 0, 0));
	panel.add(checkBox, new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0, WEST,
		NONE, EMPTY_INSETS, 0, 0));

	return panel;
    }
}
