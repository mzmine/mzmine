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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.PeptideUtils;

public class PeptideFragmentation {

    private FragmentIon[] yIons;
    private FragmentIon[] yDoubleIons;
    private FragmentIon[] bIons;
    private FragmentIon[] bDoubleIons;
    private FragmentIon[] aIons;
    private FragmentIon[] aDoubleIons;
    private FragmentIon[] cIons;
    private FragmentIon[] cDoubleIons;
    private FragmentIon[] xIons;
    private FragmentIon[] xDoubleIons;
    private FragmentIon[] zIons;
    private FragmentIon[] zDoubleIons;
    private FragmentIon[] zhIons;
    private FragmentIon[] zhDoubleIons;
    private FragmentIon[] zhhIons;
    private FragmentIon[] zhhDoubleIons;
    private FragmentIon[] precursorIons;
    private FragmentIon[] immoniumIons;

    public PeptideFragmentation(Peptide peptide,
	    PeptideIdentityDataFile dataFile) {

	SerieIonType[] ionSeriesRules = dataFile.getIonSeriesRules();
	HashMap<String, Double> defaultMasses = dataFile.getDefaultMasses();

	String sequence = peptide.getSequence();
	double precursorMass = peptide.getPrecursorMass();
	int precursorCharge = peptide.getPrecursorCharge();
	double oxygenMass = defaultMasses.get("Oxygen");
	double hydrogenMass = defaultMasses.get("Hydrogen");
	double nitrogenMass = defaultMasses.get("Nitrogen");
	double carbonMass = defaultMasses.get("Carbon");

	double[] peptideMasses = PeptideUtils.calculatePeptideMasses(
		defaultMasses, sequence, peptide.getModifications());

	// b-ions
	Vector<FragmentIon> bSeries = PeptideUtils.calculateBions(
		peptideMasses, hydrogenMass);
	FragmentIon[] singleBIons = bSeries.toArray(new FragmentIon[0]);
	// double charged b-ions
	Vector<FragmentIon> bDoubleSeries = PeptideUtils
		.calculateDoubleChargedIons(singleBIons, hydrogenMass,
			FragmentIonType.B_DOUBLE_ION);

	// b-ion with a loss of H2O
	int index = PeptideUtils.getFirstBionLossH2O(sequence);
	bSeries.addAll(PeptideUtils.calculateIonsLossH2O(singleBIons,
		hydrogenMass, oxygenMass, index, false,
		FragmentIonType.B_H2O_ION));
	// double charged b-ion with a loss of H2O
	bDoubleSeries.addAll(PeptideUtils.calculateIonsLossH2O(singleBIons,
		hydrogenMass, oxygenMass, index, true,
		FragmentIonType.B_H2O_DOUBLE_ION));

	// b-ion with a loss of NH3
	index = PeptideUtils.getFirstBionLossNH3(sequence);

	bSeries.addAll(PeptideUtils.calculateIonsLossNH3(singleBIons,
		hydrogenMass, nitrogenMass, index, false,
		FragmentIonType.B_NH3_ION));
	// double charged b-ion with a loss of NH3
	bDoubleSeries.addAll(PeptideUtils.calculateIonsLossNH3(singleBIons,
		hydrogenMass, nitrogenMass, index, true,
		FragmentIonType.B_NH3_DOUBLE_ION));

	this.bIons = bSeries.toArray(new FragmentIon[0]);
	this.bDoubleIons = bDoubleSeries.toArray(new FragmentIon[0]);

	// Y-ions
	Vector<FragmentIon> ySeries = PeptideUtils.calculateYions(
		peptideMasses, hydrogenMass, oxygenMass);
	FragmentIon[] singleYIons = ySeries.toArray(new FragmentIon[0]);
	// double charged y-ions
	Vector<FragmentIon> yDoubleSeries = PeptideUtils
		.calculateDoubleChargedIons(singleYIons, hydrogenMass,
			FragmentIonType.Y_DOUBLE_ION);

	// y-ion with a loss of H2O
	index = PeptideUtils.getFirstYionLossH2O(sequence);
	ySeries.addAll(PeptideUtils.calculateIonsLossH2O(singleYIons,
		hydrogenMass, oxygenMass, index, false,
		FragmentIonType.Y_H2O_ION));
	// double charged y-ion with a loss of H2O
	yDoubleSeries.addAll(PeptideUtils.calculateIonsLossH2O(singleYIons,
		hydrogenMass, oxygenMass, index, true,
		FragmentIonType.Y_H2O_DOUBLE_ION));

	// y-ion with a loss of NH3
	index = PeptideUtils.getFirstYionLossNH3(sequence);
	ySeries.addAll(PeptideUtils.calculateIonsLossNH3(singleYIons,
		hydrogenMass, nitrogenMass, index, false,
		FragmentIonType.Y_NH3_ION));
	// double charged y-ion with a loss of NH3
	yDoubleSeries.addAll(PeptideUtils.calculateIonsLossNH3(singleYIons,
		hydrogenMass, nitrogenMass, index, true,
		FragmentIonType.Y_NH3_DOUBLE_ION));

	this.yIons = ySeries.toArray(new FragmentIon[0]);
	this.yDoubleIons = yDoubleSeries.toArray(new FragmentIon[0]);

	/*
	 * Calculates only those fragment ions depending of the ion series rule.
	 * This rules depends on the kind of instrument (mass spec).
	 */

	for (int i = 0; i < ionSeriesRules.length; i++) {
	    switch (ionSeriesRules[i]) {
	    case A_SERIES:
		// a-ions
		Vector<FragmentIon> aSeries = PeptideUtils.calculateAions(
			singleBIons, carbonMass, oxygenMass, hydrogenMass);
		FragmentIon[] singleAIons = aSeries.toArray(new FragmentIon[0]);
		// double charged a-ions
		Vector<FragmentIon> aDoubleSeries = PeptideUtils
			.calculateDoubleChargedIons(singleAIons, hydrogenMass,
				FragmentIonType.A_DOUBLE_ION);

		// a-ion with a loss of H2O
		index = PeptideUtils.getFirstBionLossH2O(sequence);
		aSeries.addAll(PeptideUtils.calculateIonsLossH2O(singleAIons,
			hydrogenMass, oxygenMass, index, false,
			FragmentIonType.A_H2O_ION));
		// double charged a-ion with a loss of H2O
		aDoubleSeries.addAll(PeptideUtils.calculateIonsLossH2O(
			singleAIons, hydrogenMass, oxygenMass, index, true,
			FragmentIonType.A_H2O_DOUBLE_ION));

		// a-ion with a loss of NH3
		index = PeptideUtils.getFirstBionLossNH3(sequence);
		aSeries.addAll(PeptideUtils.calculateIonsLossNH3(singleAIons,
			hydrogenMass, nitrogenMass, index, false,
			FragmentIonType.A_NH3_ION));
		// double charged a-ion with a loss of NH3
		aDoubleSeries.addAll(PeptideUtils.calculateIonsLossNH3(
			singleAIons, hydrogenMass, nitrogenMass, index, true,
			FragmentIonType.A_NH3_DOUBLE_ION));

		this.aIons = aSeries.toArray(new FragmentIon[0]);
		this.aDoubleIons = aDoubleSeries.toArray(new FragmentIon[0]);

		break;
	    case C_SERIES:
		// c-ions
		Vector<FragmentIon> cSeries = PeptideUtils.calculateCions(
			singleBIons, nitrogenMass, hydrogenMass);
		FragmentIon[] singleCIons = cSeries.toArray(new FragmentIon[0]);
		// double charged c-ions
		Vector<FragmentIon> cDoubleSeries = PeptideUtils
			.calculateDoubleChargedIons(singleCIons, hydrogenMass,
				FragmentIonType.C_DOUBLE_ION);

		this.cIons = singleCIons;
		this.cIons = cDoubleSeries.toArray(new FragmentIon[0]);

		break;
	    case X_SERIES:
		// x-ions
		Vector<FragmentIon> xSeries = PeptideUtils.calculateXions(
			singleYIons, carbonMass, oxygenMass, hydrogenMass);
		FragmentIon[] singleXIons = xSeries.toArray(new FragmentIon[0]);
		// double charged x-ions
		Vector<FragmentIon> xDoubleSeries = PeptideUtils
			.calculateDoubleChargedIons(singleXIons, hydrogenMass,
				FragmentIonType.X_DOUBLE_ION);

		this.xIons = singleXIons;
		this.xDoubleIons = xDoubleSeries.toArray(new FragmentIon[0]);

		break;
	    case Z_SERIES:
		// z-ions
		Vector<FragmentIon> zSeries = PeptideUtils.calculateZions(
			singleYIons, nitrogenMass, hydrogenMass);
		FragmentIon[] singleZIons = zSeries.toArray(new FragmentIon[0]);
		// double charged z-ions
		Vector<FragmentIon> zDoubleSeries = PeptideUtils
			.calculateDoubleChargedIons(singleZIons, hydrogenMass,
				FragmentIonType.Z_DOUBLE_ION);

		this.zIons = singleZIons;
		this.zDoubleIons = zDoubleSeries.toArray(new FragmentIon[0]);

		break;
	    case ZH_SERIES:
		// z-ions
		Vector<FragmentIon> zhSeries = PeptideUtils.calculateZions(
			singleYIons, nitrogenMass, hydrogenMass);
		FragmentIon[] singleZHIons = zhSeries
			.toArray(new FragmentIon[0]);
		// zh-ions
		zhSeries = PeptideUtils.calculateZHions(singleZHIons,
			hydrogenMass);
		singleZHIons = zhSeries.toArray(new FragmentIon[0]);
		// double charged zh-ions
		Vector<FragmentIon> zhDoubleSeries = PeptideUtils
			.calculateDoubleChargedIons(singleZHIons, hydrogenMass,
				FragmentIonType.ZH_DOUBLE_ION);

		this.zhIons = singleZHIons;
		this.zhDoubleIons = zhDoubleSeries.toArray(new FragmentIon[0]);

		break;
	    case ZHH_SERIES:
		// z-ions
		Vector<FragmentIon> zhhSeries = PeptideUtils.calculateZions(
			singleYIons, nitrogenMass, hydrogenMass);
		FragmentIon[] singleZHHIons = zhhSeries
			.toArray(new FragmentIon[0]);
		// zhh-ions
		zhhSeries = PeptideUtils.calculateZHHions(singleZHHIons,
			hydrogenMass);
		singleZHHIons = zhhSeries.toArray(new FragmentIon[0]);
		// double charged zhh-ions
		Vector<FragmentIon> zhhDoubleSeries = PeptideUtils
			.calculateDoubleChargedIons(singleZHHIons,
				hydrogenMass, FragmentIonType.ZHH_DOUBLE_ION);

		this.zhhIons = singleZHHIons;
		this.zhhIons = zhhDoubleSeries.toArray(new FragmentIon[0]);

		break;
	    case A_DOUBLE_SERIES:
		break;
	    case B_DOUBLE_SERIES:
		break;
	    case B_SERIES:
		break;
	    case C_DOUBLE_SERIES:
		break;
	    case X_DOUBLE_SERIES:
		break;
	    case Y_DOUBLE_SERIES:
		break;
	    case Y_SERIES:
		break;
	    case ZHH_DOUBLE_SERIES:
		break;
	    case ZH_DOUBLE_SERIES:
		break;
	    case Z_DOUBLE_SERIES:
		break;
	    default:
		break;
	    }
	}

	this.precursorIons = PeptideUtils.calculatePrecursorIons(precursorMass,
		precursorCharge, hydrogenMass, oxygenMass, nitrogenMass);

	this.immoniumIons = PeptideUtils.calculateImmoniumIons(sequence,
		defaultMasses);

    }

    /**
     * Returns a FragmentIon[] with the b-ions.
     * 
     * @return FragmentIonImpl[] Bions.
     */
    public FragmentIon[] getBions() {
	return bIons;
    }

    /**
     * Returns a FragmentIon[] with double b-ions.
     * 
     * @return FragmentIonImpl[] Bions.
     */
    public FragmentIon[] getBDoubleIons() {
	return bDoubleIons;
    }

    /**
     * Returns a FragmentIon[] with the y-ions.
     * 
     * @return FragmentIon[] y-ions.
     */
    public FragmentIon[] getYions() {
	return yIons;
    }

    /**
     * Returns a FragmentIon[] with double y-ions.
     * 
     * @return FragmentIon[] y-ions.
     */
    public FragmentIon[] getYDoubleIons() {
	return yDoubleIons;
    }

    /**
     * Returns a FragmentIon[] with the a-ions.
     * 
     * @return FragmentIon[] a-ions.
     */
    public FragmentIon[] getAions() {
	return aIons;
    }

    /**
     * Returns a FragmentIon[] with double a-ions.
     * 
     * @return FragmentIon[] a-ions.
     */
    public FragmentIon[] getADoubleIons() {
	return aDoubleIons;
    }

    /**
     * Returns a FragmentIon[] with the c-ions.
     * 
     * @return FragmentIon[] c-ions.
     */
    public FragmentIon[] getCions() {
	return cIons;
    }

    /**
     * Returns a FragmentIon[] with double c-ions.
     * 
     * @return FragmentIon[] c-ions.
     */
    public FragmentIon[] getCDoubleIons() {
	return cDoubleIons;
    }

    /**
     * Returns a FragmentIon[] with the x-ions.
     * 
     * @return FragmentIon[] x-ions.
     */
    public FragmentIon[] getXions() {
	return xIons;
    }

    /**
     * Returns a FragmentIon[] with double x-ions.
     * 
     * @return FragmentIon[] x-ions.
     */
    public FragmentIon[] getXDoubleIons() {
	return xDoubleIons;
    }

    /**
     * Returns a FragmentIon[] with the z-ions.
     * 
     * @return FragmentIon[] z-ions.
     */
    public FragmentIon[] getZions() {
	return zIons;
    }

    /**
     * Returns a FragmentIon[] with the z-ions.
     * 
     * @return FragmentIon[] z-ions.
     */
    public FragmentIon[] getZDoubleIons() {
	return zDoubleIons;
    }

    /**
     * Returns a FragmentIon[] with double zh-ions.
     * 
     * @return FragmentIon[] zh-ions.
     */
    public FragmentIon[] getZHDoubleIons() {
	return zhDoubleIons;
    }

    /**
     * Returns a FragmentIon[] with the zhh-ions.
     * 
     * @return FragmentIon[] zhh-ions.
     */
    public FragmentIon[] getZHHions() {
	return zhhIons;
    }

    /**
     * Returns a FragmentIon[] with double zhh-ions.
     * 
     * @return FragmentIon[] zhh-ions.
     */
    public FragmentIon[] getZHHDoubleIons() {
	return zhhDoubleIons;
    }

    /**
     * This method returns all the theoretical fragment ions according with to
     * the fragmentation rules and the series ion founded (return ions with
     * significance equal or greater than the parameter).
     *
     * [Mascot 2.2]
     * 
     * 0 a 1 place holder 2 a++ 3 b 4 place holder 5 b++ 6 y 7 place holder 8
     * y++ 9 c 10 c++ 11 x 12 x++ 13 z 14 z++ 15 z+H 16 z+H++ 17 z+2H 18 z+2H++
     * 
     * [Example] 0 0 0 2 0 1 1 0 1 0 1 0 0 2 0 0 2 0 1 0 1 2 3 4 5 6 7 8 9 10 11
     * 12 13 14 15 16 17 18
     * 
     * 
     * @param aIonSeriesIndex
     * @return FragmentIonImpl[] Returns a FragmentIonImpl[] with fragmentions
     *         of the requested type. (by int aIonSeries read from the
     *         iIonSeries int[].
     */
    public FragmentIon[] getFragmentIons(PeptideIonSerie ionSeriesFound,
	    IonSignificance significanceFilter) {

	FragmentIon[] ionFragments = new FragmentIon[0];
	HashMap<SerieIonType, IonSignificance> fragmentSeries = ionSeriesFound
		.getFragmentSeries();
	Iterator<SerieIonType> it = fragmentSeries.keySet().iterator();
	SerieIonType ionType;
	IonSignificance significanceIon;

	while (it.hasNext()) {

	    ionType = (SerieIonType) it.next();
	    significanceIon = fragmentSeries.get(ionType);

	    if (significanceFilter == significanceIon) {
		switch (ionType) {
		case A_SERIES:
		    ionFragments = CollectionUtils.concat(ionFragments, aIons);
		    break;
		case A_DOUBLE_SERIES:
		    ionFragments = CollectionUtils.concat(ionFragments,
			    aDoubleIons);
		    break;
		case B_SERIES:
		    ionFragments = CollectionUtils.concat(ionFragments, bIons);
		    break;
		case B_DOUBLE_SERIES:
		    ionFragments = CollectionUtils.concat(ionFragments,
			    bDoubleIons);
		    break;
		case Y_SERIES:
		    ionFragments = CollectionUtils.concat(ionFragments, yIons);
		    break;
		case Y_DOUBLE_SERIES:
		    ionFragments = CollectionUtils.concat(ionFragments,
			    yDoubleIons);
		    break;
		case C_SERIES:
		    ionFragments = CollectionUtils.concat(ionFragments, cIons);
		    break;
		case C_DOUBLE_SERIES:
		    ionFragments = CollectionUtils.concat(ionFragments,
			    cDoubleIons);
		    break;
		case X_SERIES:
		    ionFragments = CollectionUtils.concat(ionFragments, xIons);
		    break;
		case X_DOUBLE_SERIES:
		    ionFragments = CollectionUtils.concat(ionFragments,
			    xDoubleIons);
		    break;
		case Z_SERIES:
		    ionFragments = CollectionUtils.concat(ionFragments, zIons);
		    break;
		case Z_DOUBLE_SERIES:
		    ionFragments = CollectionUtils.concat(ionFragments,
			    zDoubleIons);
		    break;
		case ZH_SERIES:
		    ionFragments = CollectionUtils.concat(ionFragments, zhIons);
		    break;
		case ZH_DOUBLE_SERIES:
		    ionFragments = CollectionUtils.concat(ionFragments,
			    zhDoubleIons);
		    break;
		case ZHH_SERIES:
		    ionFragments = CollectionUtils
			    .concat(ionFragments, zhhIons);
		    break;
		case ZHH_DOUBLE_SERIES:
		    ionFragments = CollectionUtils.concat(ionFragments,
			    zhhDoubleIons);
		    break;
		default:
		    break;
		}
	    }
	}

	ionFragments = CollectionUtils.concat(ionFragments, precursorIons);
	ionFragments = CollectionUtils.concat(ionFragments, immoniumIons);

	return ionFragments;
    }

    /**
     * This method returns all the theoretical fragment ions
     * 
     * @return FragmenIon[]
     */
    public FragmentIon[] getFragmentIons() {

	FragmentIon[] ionFragments = new FragmentIon[0];
	if (aIons != null)
	    ionFragments = CollectionUtils.concat(ionFragments, aIons);
	if (aDoubleIons != null)
	    ionFragments = CollectionUtils.concat(ionFragments, aDoubleIons);
	if (bIons != null)
	    ionFragments = CollectionUtils.concat(ionFragments, bIons);
	if (bDoubleIons != null)
	    ionFragments = CollectionUtils.concat(ionFragments, bDoubleIons);
	if (yIons != null)
	    ionFragments = CollectionUtils.concat(ionFragments, yIons);
	if (yDoubleIons != null)
	    ionFragments = CollectionUtils.concat(ionFragments, yDoubleIons);
	if (cIons != null)
	    ionFragments = CollectionUtils.concat(ionFragments, cIons);
	if (cDoubleIons != null)
	    ionFragments = CollectionUtils.concat(ionFragments, cDoubleIons);
	if (xIons != null)
	    ionFragments = CollectionUtils.concat(ionFragments, xIons);
	if (xDoubleIons != null)
	    ionFragments = CollectionUtils.concat(ionFragments, xDoubleIons);
	if (zIons != null)
	    ionFragments = CollectionUtils.concat(ionFragments, zIons);
	if (zDoubleIons != null)
	    ionFragments = CollectionUtils.concat(ionFragments, zDoubleIons);
	if (zhIons != null)
	    ionFragments = CollectionUtils.concat(ionFragments, zhIons);
	if (zhDoubleIons != null)
	    ionFragments = CollectionUtils.concat(ionFragments, zhDoubleIons);
	if (zhhIons != null)
	    ionFragments = CollectionUtils.concat(ionFragments, zhhIons);
	if (zhhDoubleIons != null)
	    ionFragments = CollectionUtils.concat(ionFragments, zhhDoubleIons);

	ionFragments = CollectionUtils.concat(ionFragments, precursorIons);
	ionFragments = CollectionUtils.concat(ionFragments, immoniumIons);

	return ionFragments;
    }

    /**
     * This method returns the theoretical fragment ions equal to the parameter
     * type
     * 
     * @param SerieIonType
     * 
     * @return FragmenIon[]
     */
    public FragmentIon[] getFragmentIons(SerieIonType ionType) {
	switch (ionType) {
	case A_SERIES:
	    return aIons;
	case A_DOUBLE_SERIES:
	    return aDoubleIons;
	case B_SERIES:
	    return bIons;
	case B_DOUBLE_SERIES:
	    return bDoubleIons;
	case Y_SERIES:
	    return yIons;
	case Y_DOUBLE_SERIES:
	    return yDoubleIons;
	case C_SERIES:
	    return cIons;
	case C_DOUBLE_SERIES:
	    return cDoubleIons;
	case X_SERIES:
	    return xIons;
	case X_DOUBLE_SERIES:
	    return xDoubleIons;
	case Z_SERIES:
	    return zIons;
	case Z_DOUBLE_SERIES:
	    return zDoubleIons;
	case ZH_SERIES:
	    return zhIons;
	case ZH_DOUBLE_SERIES:
	    return zhDoubleIons;
	case ZHH_SERIES:
	    return zhhIons;
	case ZHH_DOUBLE_SERIES:
	    return zhhDoubleIons;
	default:
	    return null;
	}
    }

}
