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

/*
 * This class comes from the JChemPaint project. Big thanks to the authors of JChemPaint!
 */
package net.sf.mzmine.util.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;

import org.openscience.cdk.event.ICDKChangeListener;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.tools.periodictable.PeriodicTable;

/**
 * JPanel version of the periodic system.
 *
 * @author Egon Willighagen
 * @author Geert Josten
 * @author Miguel Rojas
 * @author Konstantin Tokarev
 * @author Mark Rijnbeek
 */
public class PeriodicTablePanel extends JPanel {

    private static final long serialVersionUID = -2539418347261469740L;

    Vector<ICDKChangeListener> listeners = null;
    String selectedElement = null;

    private JPanel panel;
    // private JLabel label;
    private JLayeredPane layeredPane;

    private Map<JButton, Color> buttoncolors = new HashMap<JButton, Color>();

    public static int APPLICATION = 0;
    /* default */
    public static int JCP = 1;

    /*
     * set if the button should be written with html - which takes too long time
     * for loading APPLICATION = with html JCP = default
     */

    /**
     * Constructor of the PeriodicTablePanel object
     */
    public PeriodicTablePanel() {
	super();
	setLayout(new BorderLayout());
	layeredPane = new JLayeredPane();
	layeredPane.setPreferredSize(new Dimension(581, 435));
	JPanel tp = PTPanel();
	tp.setBounds(8, 85, 570, 340);

	panel = CreateLabelProperties(null);

	layeredPane.add(tp, new Integer(0));
	layeredPane.add(panel, new Integer(1));
	add(layeredPane);
    }

    private JPanel PTPanel() {

	JPanel panel = new JPanel();
	listeners = new Vector<ICDKChangeListener>();
	panel.setLayout(new GridLayout(0, 19));

	// --------------------------------
	Box.createHorizontalGlue();
	panel.add(Box.createHorizontalGlue());
	JButton butt = new JButton("1");
	butt.setBorder(new EmptyBorder(2, 2, 2, 2));
	panel.add(butt);
	// --------------------------------
	for (int i = 0; i < 16; i++) {
	    Box.createHorizontalGlue();
	    panel.add(Box.createHorizontalGlue());
	}
	butt = new JButton("18");
	butt.setBorder(new EmptyBorder(2, 2, 2, 2));
	panel.add(butt);

	butt = new JButton("1");
	butt.setBorder(new EmptyBorder(2, 2, 2, 2));
	panel.add(butt);
	panel.add(createButton(GT.get("H")));

	butt = new JButton("2");
	butt.setBorder(new EmptyBorder(2, 2, 2, 2));
	panel.add(butt);
	for (int i = 0; i < 10; i++) {
	    panel.add(Box.createHorizontalGlue());
	}
	butt = new JButton("13");
	butt.setBorder(new EmptyBorder(2, 2, 2, 2));
	panel.add(butt);

	butt = new JButton("14");
	butt.setBorder(new EmptyBorder(2, 2, 2, 2));
	panel.add(butt);

	butt = new JButton("15");
	butt.setBorder(new EmptyBorder(2, 2, 2, 2));
	panel.add(butt);

	butt = new JButton("16");
	butt.setBorder(new EmptyBorder(2, 2, 2, 2));
	panel.add(butt);

	butt = new JButton("17");
	butt.setBorder(new EmptyBorder(2, 2, 2, 2));
	panel.add(butt);
	//

	panel.add(createButton(GT.get("He")));

	butt = new JButton("2");
	butt.setBorder(new EmptyBorder(2, 2, 2, 2));
	panel.add(butt);

	panel.add(createButton(GT.get("Li")));

	panel.add(createButton(GT.get("Be")));
	for (int i = 0; i < 10; i++) {
	    panel.add(Box.createHorizontalGlue());
	}
	// no metall
	panel.add(createButton(GT.get("B")));
	panel.add(createButton(GT.get("C")));
	panel.add(createButton(GT.get("N")));
	panel.add(createButton(GT.get("O")));
	panel.add(createButton(GT.get("F")));
	//
	panel.add(createButton(GT.get("Ne")));

	butt = new JButton("3");
	butt.setBorder(new EmptyBorder(2, 2, 2, 2));
	panel.add(butt);
	panel.add(createButton(GT.get("Na")));
	panel.add(createButton(GT.get("Mg")));

	butt = new JButton("3");
	butt.setBorder(new EmptyBorder(2, 2, 2, 2));
	panel.add(butt);
	butt = new JButton("4");
	butt.setBorder(new EmptyBorder(2, 2, 2, 2));
	panel.add(butt);
	butt = new JButton("5");
	butt.setBorder(new EmptyBorder(2, 2, 2, 2));
	panel.add(butt);
	butt = new JButton("6");
	butt.setBorder(new EmptyBorder(2, 2, 2, 2));
	panel.add(butt);
	butt = new JButton("7");
	butt.setBorder(new EmptyBorder(2, 2, 2, 2));
	panel.add(butt);
	butt = new JButton("8");
	butt.setBorder(new EmptyBorder(2, 2, 2, 2));
	panel.add(butt);
	butt = new JButton("9");
	butt.setBorder(new EmptyBorder(2, 2, 2, 2));
	panel.add(butt);
	butt = new JButton("10");
	butt.setBorder(new EmptyBorder(2, 2, 2, 2));
	panel.add(butt);
	butt = new JButton("11");
	butt.setBorder(new EmptyBorder(2, 2, 2, 2));
	panel.add(butt);
	butt = new JButton("12");
	butt.setBorder(new EmptyBorder(2, 2, 2, 2));
	panel.add(butt);
	// no metall
	panel.add(createButton(GT.get("Al")));
	panel.add(createButton(GT.get("Si")));
	panel.add(createButton(GT.get("P")));
	panel.add(createButton(GT.get("S")));
	panel.add(createButton(GT.get("Cl")));
	//
	panel.add(createButton(GT.get("Ar")));

	butt = new JButton("4");
	butt.setBorder(new EmptyBorder(2, 2, 2, 2));
	panel.add(butt);
	panel.add(createButton(GT.get("K")));
	panel.add(createButton(GT.get("Ca")));
	// transition
	panel.add(createButton(GT.get("Sc")));
	panel.add(createButton(GT.get("Ti")));
	panel.add(createButton(GT.get("V")));
	panel.add(createButton(GT.get("Cr")));
	panel.add(createButton(GT.get("Mn")));
	panel.add(createButton(GT.get("Fe")));
	panel.add(createButton(GT.get("Co")));
	panel.add(createButton(GT.get("Ni")));
	panel.add(createButton(GT.get("Cu")));
	panel.add(createButton(GT.get("Zn")));
	// no metall
	panel.add(createButton(GT.get("Ga")));
	panel.add(createButton(GT.get("Ge")));
	panel.add(createButton(GT.get("As")));
	panel.add(createButton(GT.get("Se")));
	panel.add(createButton(GT.get("Br")));
	//
	panel.add(createButton(GT.get("Kr")));

	butt = new JButton("5");
	butt.setBorder(new EmptyBorder(2, 2, 2, 2));
	panel.add(butt);
	panel.add(createButton(GT.get("Rb")));
	panel.add(createButton(GT.get("Sr")));
	// transition
	panel.add(createButton(GT.get("Y")));
	panel.add(createButton(GT.get("Zr")));
	panel.add(createButton(GT.get("Nb")));
	panel.add(createButton(GT.get("Mo")));
	panel.add(createButton(GT.get("Tc")));
	panel.add(createButton(GT.get("Ru")));
	panel.add(createButton(GT.get("Rh")));
	panel.add(createButton(GT.get("Pd")));
	panel.add(createButton(GT.get("Ag")));
	panel.add(createButton(GT.get("Cd")));
	// no metall
	panel.add(createButton(GT.get("In")));
	panel.add(createButton(GT.get("Sn")));
	panel.add(createButton(GT.get("Sb")));
	panel.add(createButton(GT.get("Te")));
	panel.add(createButton(GT.get("I")));
	//
	panel.add(createButton(GT.get("Xe")));

	butt = new JButton("6");
	butt.setBorder(new EmptyBorder(2, 2, 2, 2));
	panel.add(butt);
	panel.add(createButton(GT.get("Cs")));
	panel.add(createButton(GT.get("Ba")));
	// transition
	panel.add(createButton(GT.get("La")));
	panel.add(createButton(GT.get("Hf")));
	panel.add(createButton(GT.get("Ta")));
	panel.add(createButton(GT.get("W")));
	panel.add(createButton(GT.get("Re")));
	panel.add(createButton(GT.get("Os")));
	panel.add(createButton(GT.get("Ir")));
	panel.add(createButton(GT.get("Pt")));
	panel.add(createButton(GT.get("Au")));
	panel.add(createButton(GT.get("Hg")));
	// no metall
	panel.add(createButton(GT.get("Tl")));
	panel.add(createButton(GT.get("Pb")));
	panel.add(createButton(GT.get("Bi")));
	panel.add(createButton(GT.get("Po")));
	panel.add(createButton(GT.get("At")));
	//
	panel.add(createButton(GT.get("Rn")));

	butt = new JButton("7");
	butt.setBorder(new EmptyBorder(2, 2, 2, 2));
	panel.add(butt);
	panel.add(createButton(GT.get("Fr")));
	panel.add(createButton(GT.get("Ra")));
	// transition
	panel.add(createButton(GT.get("Ac")));
	panel.add(createButton(GT.get("Rf")));
	panel.add(createButton(GT.get("Db")));
	panel.add(createButton(GT.get("Sg")));
	panel.add(createButton(GT.get("Bh")));
	panel.add(createButton(GT.get("Hs")));
	panel.add(createButton(GT.get("Mt")));
	panel.add(createButton(GT.get("Ds")));
	panel.add(createButton(GT.get("Rg")));
	for (int i = 0; i < 10; i++) {
	    panel.add(Box.createHorizontalGlue());
	}
	// Acti
	panel.add(createButton(GT.get("Ce")));
	panel.add(createButton(GT.get("Pr")));
	panel.add(createButton(GT.get("Nd")));
	panel.add(createButton(GT.get("Pm")));
	panel.add(createButton(GT.get("Sm")));
	panel.add(createButton(GT.get("Eu")));
	panel.add(createButton(GT.get("Gd")));
	panel.add(createButton(GT.get("Tb")));
	panel.add(createButton(GT.get("Dy")));
	panel.add(createButton(GT.get("Ho")));
	panel.add(createButton(GT.get("Er")));
	panel.add(createButton(GT.get("Tm")));
	panel.add(createButton(GT.get("Yb")));
	panel.add(createButton(GT.get("Lu")));
	for (int i = 0; i < 5; i++) {
	    panel.add(Box.createHorizontalGlue());
	}
	// Lacti
	panel.add(createButton(GT.get("Th")));
	panel.add(createButton(GT.get("Pa")));
	panel.add(createButton(GT.get("U")));
	panel.add(createButton(GT.get("Np")));
	panel.add(createButton(GT.get("Pu")));
	panel.add(createButton(GT.get("Am")));
	panel.add(createButton(GT.get("Cm")));
	panel.add(createButton(GT.get("Bk")));
	panel.add(createButton(GT.get("Cf")));
	panel.add(createButton(GT.get("Es")));
	panel.add(createButton(GT.get("Fm")));
	panel.add(createButton(GT.get("Md")));
	panel.add(createButton(GT.get("No")));
	panel.add(createButton(GT.get("Lr")));
	// End
	panel.setVisible(true);
	return panel;
    }

    /**
     * create button. Define the color of the font and background
     *
     * @param elementS
     *            String of the element
     * @return button JButton
     */
    private JButton createButton(String elementS) {
	Color colorF = new Color(0, 0, 0);

	Color colorB = null;
	String serie = PeriodicTable.getChemicalSeries(elementS);
	if (serie.equals("Noble Gasses"))
	    colorB = new Color(255, 153, 255);
	else if (serie.equals("Halogens"))
	    colorB = new Color(255, 153, 153);
	else if (serie.equals("Nonmetals"))
	    colorB = new Color(255, 152, 90);
	else if (serie.equals("Metalloids"))
	    colorB = new Color(255, 80, 80);
	else if (serie.equals("Metals"))
	    colorB = new Color(255, 50, 0);
	else if (serie.equals("Alkali Earth Metals"))
	    colorB = new Color(102, 150, 255);
	else if (serie.equals("Alkali Metals"))
	    colorB = new Color(130, 130, 255);
	else if (serie.equals("Transition metals"))
	    colorB = new Color(255, 255, 110);
	else if (serie.equals("Lanthanides"))
	    colorB = new Color(255, 255, 150);
	else if (serie.equals("Actinides"))
	    colorB = new Color(255, 255, 200);

	JButton button = new ElementButton(elementS, new ElementMouseAction(),
		elementS, colorF);
	button.setBackground(colorB);
	button.setName(elementS);
	buttoncolors.put(button, colorB);

	return button;
    }

    /**
     * Sets the selectedElement attribute of the PeriodicTablePanel object
     *
     * @param selectedElement
     *            The new selectedElement value
     */
    public void setSelectedElement(String selectedElement) {
	this.selectedElement = selectedElement;
    }

    /**
     * Gets the selectedElement attribute of the PeriodicTablePanel object
     *
     * @return The selectedElement value
     */
    public String getSelectedElement() throws IOException, CDKException {
	return selectedElement;
    }

    /**
     * Adds a change listener to the list of listeners
     *
     * @param listener
     *            The listener added to the list
     */

    public void addCDKChangeListener(ICDKChangeListener listener) {
	listeners.add(listener);
    }

    /**
     * Removes a change listener from the list of listeners
     *
     * @param listener
     *            The listener removed from the list
     */
    public void removeCDKChangeListener(ICDKChangeListener listener) {
	listeners.remove(listener);
    }

    /**
     * Notifies registered listeners of certain changes that have occurred in
     * this model.
     */
    public void fireChange() {
	EventObject event = new EventObject(this);
	for (int i = 0; i < listeners.size(); i++) {
	    ((ICDKChangeListener) listeners.get(i)).stateChanged(event);
	}
    }

    /**
     * get translated name of element
     *
     * @author Geoffrey R. Hutchison
     * @param atomic
     *            number of element
     * @return the name element to show
     */
    private String elementTranslator(int element) {
	String result;
	switch (element) {
	case 1:
	    result = GT.get("Hydrogen");
	    break;
	case 2:
	    result = GT.get("Helium");
	    break;
	case 3:
	    result = GT.get("Lithium");
	    break;
	case 4:
	    result = GT.get("Beryllium");
	    break;
	case 5:
	    result = GT.get("Boron");
	    break;
	case 6:
	    result = GT.get("Carbon");
	    break;
	case 7:
	    result = GT.get("Nitrogen");
	    break;
	case 8:
	    result = GT.get("Oxygen");
	    break;
	case 9:
	    result = GT.get("Fluorine");
	    break;
	case 10:
	    result = GT.get("Neon");
	    break;
	case 11:
	    result = GT.get("Sodium");
	    break;
	case 12:
	    result = GT.get("Magnesium");
	    break;
	case 13:
	    result = GT.get("Aluminum");
	    break;
	case 14:
	    result = GT.get("Silicon");
	    break;
	case 15:
	    result = GT.get("Phosphorus");
	    break;
	case 16:
	    result = GT.get("Sulfur");
	    break;
	case 17:
	    result = GT.get("Chlorine");
	    break;
	case 18:
	    result = GT.get("Argon");
	    break;
	case 19:
	    result = GT.get("Potassium");
	    break;
	case 20:
	    result = GT.get("Calcium");
	    break;
	case 21:
	    result = GT.get("Scandium");
	    break;
	case 22:
	    result = GT.get("Titanium");
	    break;
	case 23:
	    result = GT.get("Vanadium");
	    break;
	case 24:
	    result = GT.get("Chromium");
	    break;
	case 25:
	    result = GT.get("Manganese");
	    break;
	case 26:
	    result = GT.get("Iron");
	    break;
	case 27:
	    result = GT.get("Cobalt");
	    break;
	case 28:
	    result = GT.get("Nickel");
	    break;
	case 29:
	    result = GT.get("Copper");
	    break;
	case 30:
	    result = GT.get("Zinc");
	    break;
	case 31:
	    result = GT.get("Gallium");
	    break;
	case 32:
	    result = GT.get("Germanium");
	    break;
	case 33:
	    result = GT.get("Arsenic");
	    break;
	case 34:
	    result = GT.get("Selenium");
	    break;
	case 35:
	    result = GT.get("Bromine");
	    break;
	case 36:
	    result = GT.get("Krypton");
	    break;
	case 37:
	    result = GT.get("Rubidium");
	    break;
	case 38:
	    result = GT.get("Strontium");
	    break;
	case 39:
	    result = GT.get("Yttrium");
	    break;
	case 40:
	    result = GT.get("Zirconium");
	    break;
	case 41:
	    result = GT.get("Niobium");
	    break;
	case 42:
	    result = GT.get("Molybdenum");
	    break;
	case 43:
	    result = GT.get("Technetium");
	    break;
	case 44:
	    result = GT.get("Ruthenium");
	    break;
	case 45:
	    result = GT.get("Rhodium");
	    break;
	case 46:
	    result = GT.get("Palladium");
	    break;
	case 47:
	    result = GT.get("Silver");
	    break;
	case 48:
	    result = GT.get("Cadmium");
	    break;
	case 49:
	    result = GT.get("Indium");
	    break;
	case 50:
	    result = GT.get("Tin");
	    break;
	case 51:
	    result = GT.get("Antimony");
	    break;
	case 52:
	    result = GT.get("Tellurium");
	    break;
	case 53:
	    result = GT.get("Iodine");
	    break;
	case 54:
	    result = GT.get("Xenon");
	    break;
	case 55:
	    result = GT.get("Cesium");
	    break;
	case 56:
	    result = GT.get("Barium");
	    break;
	case 57:
	    result = GT.get("Lanthanum");
	    break;
	case 58:
	    result = GT.get("Cerium");
	    break;
	case 59:
	    result = GT.get("Praseodymium");
	    break;
	case 60:
	    result = GT.get("Neodymium");
	    break;
	case 61:
	    result = GT.get("Promethium");
	    break;
	case 62:
	    result = GT.get("Samarium");
	    break;
	case 63:
	    result = GT.get("Europium");
	    break;
	case 64:
	    result = GT.get("Gadolinium");
	    break;
	case 65:
	    result = GT.get("Terbium");
	    break;
	case 66:
	    result = GT.get("Dysprosium");
	    break;
	case 67:
	    result = GT.get("Holmium");
	    break;
	case 68:
	    result = GT.get("Erbium");
	    break;
	case 69:
	    result = GT.get("Thulium");
	    break;
	case 70:
	    result = GT.get("Ytterbium");
	    break;
	case 71:
	    result = GT.get("Lutetium");
	    break;
	case 72:
	    result = GT.get("Hafnium");
	    break;
	case 73:
	    result = GT.get("Tantalum");
	    break;
	case 74:
	    result = GT.get("Tungsten");
	    break;
	case 75:
	    result = GT.get("Rhenium");
	    break;
	case 76:
	    result = GT.get("Osmium");
	    break;
	case 77:
	    result = GT.get("Iridium");
	    break;
	case 78:
	    result = GT.get("Platinum");
	    break;
	case 79:
	    result = GT.get("Gold");
	    break;
	case 80:
	    result = GT.get("Mercury");
	    break;
	case 81:
	    result = GT.get("Thallium");
	    break;
	case 82:
	    result = GT.get("Lead");
	    break;
	case 83:
	    result = GT.get("Bismuth");
	    break;
	case 84:
	    result = GT.get("Polonium");
	    break;
	case 85:
	    result = GT.get("Astatine");
	    break;
	case 86:
	    result = GT.get("Radon");
	    break;
	case 87:
	    result = GT.get("Francium");
	    break;
	case 88:
	    result = GT.get("Radium");
	    break;
	case 89:
	    result = GT.get("Actinium");
	    break;
	case 90:
	    result = GT.get("Thorium");
	    break;
	case 91:
	    result = GT.get("Protactinium");
	    break;
	case 92:
	    result = GT.get("Uranium");
	    break;
	case 93:
	    result = GT.get("Neptunium");
	    break;
	case 94:
	    result = GT.get("Plutonium");
	    break;
	case 95:
	    result = GT.get("Americium");
	    break;
	case 96:
	    result = GT.get("Curium");
	    break;
	case 97:
	    result = GT.get("Berkelium");
	    break;
	case 98:
	    result = GT.get("Californium");
	    break;
	case 99:
	    result = GT.get("Einsteinium");
	    break;
	case 100:
	    result = GT.get("Fermium");
	    break;
	case 101:
	    result = GT.get("Mendelevium");
	    break;
	case 102:
	    result = GT.get("Nobelium");
	    break;
	case 103:
	    result = GT.get("Lawrencium");
	    break;
	case 104:
	    result = GT.get("Rutherfordium");
	    break;
	case 105:
	    result = GT.get("Dubnium");
	    break;
	case 106:
	    result = GT.get("Seaborgium");
	    break;
	case 107:
	    result = GT.get("Bohrium");
	    break;
	case 108:
	    result = GT.get("Hassium");
	    break;
	case 109:
	    result = GT.get("Meitnerium");
	    break;
	case 110:
	    result = GT.get("Darmstadtium");
	    break;
	case 111:
	    result = GT.get("Roentgenium");
	    break;
	case 112:
	    result = GT.get("Ununbium");
	    break;
	case 113:
	    result = GT.get("Ununtrium");
	    break;
	case 114:
	    result = GT.get("Ununquadium");
	    break;
	case 115:
	    result = GT.get("Ununpentium");
	    break;
	case 116:
	    result = GT.get("Ununhexium");
	    break;
	case 117:
	    result = GT.get("Ununseptium");
	    break;
	case 118:
	    result = GT.get("Ununoctium");
	    break;

	default:
	    result = GT.get("Unknown");
	}

	return result;
    }

    /**
     * get translated name of element
     *
     * @author Konstantin Tokarev
     * @param chemical
     *            serie to translate
     * @return the String to show
     */
    public String serieTranslator(String serie) {
	if (serie.equals("Noble Gasses"))
	    return GT.get("Noble Gases");
	else if (serie.equals("Halogens"))
	    return GT.get("Halogens");
	else if (serie.equals("Nonmetals"))
	    return GT.get("Nonmetals");
	else if (serie.equals("Metalloids"))
	    return GT.get("Metalloids");
	else if (serie.equals("Metals"))
	    return GT.get("Metals");
	else if (serie.equals("Alkali Earth Metals"))
	    return GT.get("Alkali Earth Metals");
	else if (serie.equals("Alkali Metals"))
	    return GT.get("Alkali Metals");
	else if (serie.equals("Transition metals"))
	    return GT.get("Transition metals");
	else if (serie.equals("Lanthanides"))
	    return GT.get("Lanthanides");
	else if (serie.equals("Actinides"))
	    return GT.get("Actinides");
	else
	    return GT.get("Unknown");
    }

    /**
     * get translated name of phase
     *
     * @author Konstantin Tokarev
     * @param phase
     *            name to translate
     * @return the String to show
     */
    public String phaseTranslator(String serie) {
	if (serie.equals("Gas"))
	    return GT.get("Gas");
	else if (serie.equals("Liquid"))
	    return GT.get("Liquid");
	else if (serie.equals("Solid"))
	    return GT.get("Solid");
	else
	    return GT.get("Unknown");
    }

    /**
     * Description of the Class
     *
     * @author steinbeck
     * @cdk.created February 10, 2004
     */
    public class ElementMouseAction implements MouseListener {

	public void mouseClicked(MouseEvent e) {
	    fireChange();
	}

	public void mouseEntered(MouseEvent e) {
	    ElementButton button = (ElementButton) e.getSource();
	    setSelectedElement(button.getElement());

	    layeredPane.remove(panel);
	    panel = CreateLabelProperties(button.getElement());
	    layeredPane.add(panel, new Integer(1));
	    layeredPane.repaint();

	    button.setBackground(Color.LIGHT_GRAY);
	}

	public void mouseExited(MouseEvent e) {
	    ((ElementButton) e.getSource()).setBackground(buttoncolors.get(e
		    .getSource()));
	}

	public void mousePressed(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
	}
    }

    /**
     * This action fragment a molecule which is on the frame JChemPaint
     *
     */
    class BackAction extends AbstractAction {

	private static final long serialVersionUID = -8708581865777449553L;

	public void actionPerformed(ActionEvent e) {
	    layeredPane.remove(panel);
	    panel = CreateLabelProperties(null);
	    layeredPane.add(panel, new Integer(1));
	    layeredPane.repaint();
	}
    }

    class ElementButton extends JButton {

	private static final long serialVersionUID = 1504183423628680664L;

	private String element;

	/**
	 * Constructor for the ElementButton object
	 *
	 * @param element
	 *            Description of the Parameter
	 */
	public ElementButton(String element) {
	    super("H");
	    this.element = element;
	}

	/**
	 * Constructor for the ElementButton object
	 * 
	 * @param element
	 *            Description of the Parameter
	 * @param e
	 *            Description of the Parameter
	 * @param color
	 *            Description of the Parameter
	 * @param controlViewer
	 *            Description of the Parameter
	 */
	public ElementButton(String element, MouseListener e,
		String buttonString, Color color) {
	    super(buttonString);
	    setForeground(color);
	    this.element = element;
	    setFont(new Font("Times-Roman", Font.BOLD, 15));
	    setBorder(new BevelBorder(BevelBorder.RAISED));
	    setToolTipText(elementTranslator(PeriodicTable
		    .getAtomicNumber(element)));
	    addMouseListener(e);
	}

	/**
	 * Gets the element attribute of the ElementButton object
	 *
	 * @return The element value
	 */
	public String getElement() {
	    return this.element;
	}
    }

    /**
     * create the Label
     *
     * @param elementSymbol
     *            String
     * @return pan JPanel
     */
    private JPanel CreateLabelProperties(String elementSymbol) {
	JPanel pan = new JPanel();
	pan.setLayout(new BorderLayout());
	Color color = new Color(255, 255, 255);
	Point origin = new Point(120, 20);
	JLabel label;
	if (elementSymbol != null) {
	    Integer group = PeriodicTable.getGroup(elementSymbol);
	    label = new JLabel(
		    "<html><FONT SIZE=+2>"
			    + elementTranslator(PeriodicTable
				    .getAtomicNumber(elementSymbol))
			    + " ("
			    + elementSymbol
			    + ")</FONT><br> "
			    + GT.get("Atomic number")
			    + " "
			    + PeriodicTable.getAtomicNumber(elementSymbol)
			    + (group != null ? ", " + GT.get("Group") + " "
				    + group : "") + ", " + GT.get("Period")
			    + " " + PeriodicTable.getPeriod(elementSymbol)
			    + "</html>");
	    label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	    pan.add(label, BorderLayout.NORTH);

	    label = new JLabel(
		    "<html><FONT> "
			    + GT.get("CAS RN:")
			    + " "
			    + PeriodicTable.getCASId(elementSymbol)
			    + "<br> "
			    + GT.get("Element Category:")
			    + " "
			    + serieTranslator(PeriodicTable
				    .getChemicalSeries(elementSymbol))
			    + "<br> "
			    + GT.get("State:")
			    + " "
			    + phaseTranslator(PeriodicTable
				    .getPhase(elementSymbol))
			    + "<br> "
			    + GT.get("Electronegativity:")
			    + " "
			    + (PeriodicTable
				    .getPaulingElectronegativity(elementSymbol) == null ? GT
				    .get("undefined") : PeriodicTable
				    .getPaulingElectronegativity(elementSymbol))
			    + "<br>" + "</FONT></html>");
	    label.setMinimumSize(new Dimension(165, 150));
	    label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	    pan.add(label, BorderLayout.CENTER);
	} else {
	    label = new JLabel("     " + GT.get("Periodic Table of elements"));
	    label.setHorizontalTextPosition(JLabel.CENTER);
	    label.setVerticalTextPosition(JLabel.CENTER);
	    label.setOpaque(true);
	    label.setBackground(color);
	    pan.add(label, BorderLayout.CENTER);
	}

	pan.setBackground(color);
	pan.setForeground(Color.black);
	pan.setBorder(BorderFactory.createLineBorder(Color.black));
	pan.setBounds(origin.x, origin.y, 255, 160);
	return pan;
    }

    private static class GT {
	private static String get(String s) {
	    return s;
	}
    }
}
