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

package net.sf.mzmine.modules.peaklistmethods.identification.mascot.data;

import java.awt.Color;

public enum FragmentIonType {

    B_ION(new Color(0, 0, 255), "b-ion"), B_DOUBLE_ION(new Color(0, 0, 255),
	    "double charged b-ion"), B_H2O_ION(new Color(0, 125, 200),
	    "b-ion - H2O"), B_H2O_DOUBLE_ION(new Color(0, 125, 200),
	    "double charged b-ion - H2O"), B_NH3_ION(new Color(153, 0, 255),
	    "b-ion - NH3"), B_NH3_DOUBLE_ION(new Color(153, 0, 255),
	    "double charged b-ion - NH3"),

    Y_ION(new Color(0, 0, 0), "y-ion"), Y_DOUBLE_ION(new Color(0, 0, 0),
	    "double charged y-ion"), Y_H2O_ION(new Color(0, 70, 135),
	    "y-ion - H2O"), Y_H2O_DOUBLE_ION(new Color(0, 70, 135),
	    "double charged y-ion - H2O"), Y_NH3_ION(new Color(155, 0, 155),
	    "y-ion - NH3"), Y_NH3_DOUBLE_ION(new Color(155, 0, 155),
	    "charged y-ion - NH3"),

    A_ION(new Color(153, 0, 0), "b-ion - CO"), A_DOUBLE_ION(
	    new Color(0, 139, 0), "double charged a-ion"), A_H2O_ION(new Color(
	    171, 161, 255), "a-ion with a loss of H2O"), A_H2O_DOUBLE_ION(
	    new Color(171, 161, 255), "double charged a-ion - H2O"), A_NH3_ION(
	    new Color(248, 151, 202), "a-ion - NH3"), A_NH3_DOUBLE_ION(
	    new Color(248, 151, 202), "double charged a-ion - NH3"),

    X_ION(new Color(78, 200, 0), "y-ion + 'CO' - 'H2'"), X_DOUBLE_ION(
	    new Color(78, 200, 0), "double charged x-ion"),

    C_ION(new Color(188, 0, 255), "b-ion + 'NH3'"), C_DOUBLE_ION(new Color(188,
	    0, 255), "double charged c-ion"),

    Z_ION(new Color(255, 140, 0), "y-ion - 'NH3'"), Z_DOUBLE_ION(new Color(64,
	    179, 0), "double charged z-ion"),

    ZH_ION(new Color(255, 140, 0), "y-ion - 'NH3' + 'H'"), ZH_DOUBLE_ION(
	    new Color(64, 179, 0), "double charged zh-ion"),

    ZHH_ION(new Color(255, 140, 0), "y-ion - 'NH3' + 'H2'"), ZHH_DOUBLE_ION(
	    new Color(64, 179, 0), "double charged zhh-ion"),

    PRECURSOR(Color.red, "precursor"), PRECURSOR_H2O(Color.red,
	    "precursor - H2O"), PRECURSOR_NH3(Color.red, "precursor - NH3"),

    IMMONIUM_A(Color.gray, "immoniumIon Ala"), IMMONIUM_R(Color.gray,
	    "immoniumIon Arg"), IMMONIUM_N(Color.gray, "immoniumIon Asn"), IMMONIUM_D(
	    Color.gray, "immoniumIon Asp"), IMMONIUM_C(Color.gray,
	    "immoniumIon Cys"), IMMONIUM_E(Color.gray, "immoniumIon Glu"), IMMONIUM_Q(
	    Color.gray, "immoniumIon Gln"), IMMONIUM_G(Color.gray,
	    "immoniumIon Gly"), IMMONIUM_H(Color.gray, "immoniumIon His"), IMMONIUM_I(
	    Color.gray, "immoniumIon Ile"), IMMONIUM_L(Color.gray,
	    "immoniumIon Leu"), IMMONIUM_K(Color.gray, "immoniumIon Lys"), IMMONIUM_M(
	    Color.gray, "immoniumIon Met"), IMMONIUM_F(Color.gray,
	    "immoniumIon Phe"), IMMONIUM_P(Color.gray, "immoniumIon Pro"), IMMONIUM_S(
	    Color.gray, "immoniumIon Ser"), IMMONIUM_T(Color.gray,
	    "immoniumIon Thr"), IMMONIUM_W(Color.gray, "immoniumIon Trp"), IMMONIUM_Y(
	    Color.gray, "immoniumIon Tyr"), IMMONIUM_V(Color.gray,
	    "immoniumIon Val");

    private Color color;
    private String description;

    private FragmentIonType(Color color, String description) {
	this.color = color;
	this.description = description;
    }

    public Color getColor() {
	return color;
    }

    public String getName() {
	return description;
    }

}
