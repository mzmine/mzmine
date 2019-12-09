/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.utility;

import java.util.logging.Logger;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;

import io.github.mzmine.datamodel.impl.ExtendedIsotopePattern;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ProcessedDataPoint;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPIsotopePatternResult;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPResult.ResultType;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.IsotopePatternUtils;

/**
 * Used to calculate parameter values based on the results of data point
 * processing modules.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class DynamicParameterUtils {

    private static Logger logger = Logger
            .getLogger(DynamicParameterUtils.class.getName());

    private static float lowerElementBoundaryPercentage = 0.75f;
    private static float upperElementBoundaryPercentage = 1.5f;

    public static float getLowerElementBoundaryPercentage() {
        return lowerElementBoundaryPercentage;
    }

    public static float getUpperElementBoundaryPercentage() {
        return upperElementBoundaryPercentage;
    }

    public static void setLowerElementBoundaryPercentage(
            float lowerElementBoundaryPercentage) {
        DynamicParameterUtils.lowerElementBoundaryPercentage = lowerElementBoundaryPercentage;
    }

    public static void setUpperElementBoundaryPercentage(
            float upperElementBoundaryPercentage) {
        DynamicParameterUtils.upperElementBoundaryPercentage = upperElementBoundaryPercentage;
    }

    /**
     * Creates an ElementParameter based on the previous processing results. If
     * no results were detected, the default value is returned. Upper and lower
     * boundaries are chosen according to lowerElementBoundaryPercentage and
     * upperElementBoundaryPercentage values of this utility class. These values
     * can be set via {@link #setLowerElementBoundaryPercentage} and
     * {@link #setUpperElementBoundaryPercentage}. The elements contained in
     * 
     * @param dp
     *            The data point to build a parameter for.
     * @param def
     *            The default set of parameters.
     * @return The built ElementsParameter
     */
    public static MolecularFormulaRange buildFormulaRangeOnIsotopePatternResults(
            ProcessedDataPoint dp, MolecularFormulaRange def) {

        DPPIsotopePatternResult result = (DPPIsotopePatternResult) dp
                .getFirstResultByType(ResultType.ISOTOPEPATTERN);
        if (result == null)
            return def;

        if (!(result.getValue() instanceof ExtendedIsotopePattern))
            return def;

        ExtendedIsotopePattern pattern = (ExtendedIsotopePattern) result
                .getValue();
        String form = IsotopePatternUtils
                .makePatternSuggestion(pattern.getIsotopeCompositions());

        MolecularFormulaRange range = new MolecularFormulaRange();

        IMolecularFormula formula = FormulaUtils
                .createMajorIsotopeMolFormula(form);
        if (formula == null) {
            logger.finest("could not generate formula for m/z " + dp.getMZ()
                    + " " + form);
            return def;
        }

        for (IIsotope isotope : def.isotopes())
            range.addIsotope(isotope, def.getIsotopeCountMin(isotope),
                    def.getIsotopeCountMax(isotope));

        for (IIsotope isotope : formula.isotopes()) {
            if (range.contains(isotope))
                continue;

            int count = formula.getIsotopeCount(isotope);

            range.addIsotope(isotope,
                    (int) (count * lowerElementBoundaryPercentage),
                    (int) (count * upperElementBoundaryPercentage));
        }

        for (IIsotope isotope : range.isotopes()) {
            int min = range.getIsotopeCountMin(isotope);
            int max = range.getIsotopeCountMax(isotope);
            // logger.info("m/z = " + dp.getMZ() + " " + isotope.getSymbol() + "
            // " + min + " - " + max);
        }

        return range;
    }

}
