/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils;

import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidCategories;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidMainClasses;

public class LipidParsingUtils {

    public static LipidFragmentationRuleType lipidFragmentationRuleNameToLipidFragmentationRuleType(
            String lipidFragmentationRuleName) {
        LipidFragmentationRuleType[] lipidFragmentationRuleTypes =
                LipidFragmentationRuleType.class.getEnumConstants();
        for (LipidFragmentationRuleType lipidFragmentationRuleType : lipidFragmentationRuleTypes) {
            if (lipidFragmentationRuleType.name().equals(lipidFragmentationRuleName)) {
                return lipidFragmentationRuleType;
            }
        }
        return null;
    }

    public static LipidAnnotationLevel lipidAnnotationLevelNameToLipidAnnotationLevel(
            String lipidAnnotationLevelName) {
        LipidAnnotationLevel[] lipidAnnotationLevels = LipidAnnotationLevel.class.getEnumConstants();
        for (LipidAnnotationLevel lipidAnnotationLevel : lipidAnnotationLevels) {
            if (lipidAnnotationLevel.name().equals(lipidAnnotationLevelName)) {
                return lipidAnnotationLevel;
            }
        }
        return null;
    }

    public static LipidCategories lipidCategoryNameToLipidLipidCategory(String lipidCategoryName) {
        LipidCategories[] lipidCategories = LipidCategories.class.getEnumConstants();
        for (LipidCategories lipidCategory : lipidCategories) {
            if (lipidCategory.name().equals(lipidCategoryName)) {
                return lipidCategory;
            }
        }
        return null;
    }

    public static LipidMainClasses lipidMainClassNameToLipidLipidMainClass(
            String lipidMainClassName) {
        LipidMainClasses[] lipidMainClasses = LipidMainClasses.class.getEnumConstants();
        for (LipidMainClasses lipidMainClass : lipidMainClasses) {
            if (lipidMainClass.name().equals(lipidMainClassName)) {
                return lipidMainClass;
            }
        }
        return null;
    }

    public static LipidChainType lipidChainTypeNameToLipidChainType(String lipidChainTypeName) {
        LipidChainType[] lipidChainTypes = LipidChainType.class.getEnumConstants();
        for (LipidChainType lipidChainType : lipidChainTypes) {
            if (lipidChainType.name().equals(lipidChainTypeName)) {
                return lipidChainType;
            }
        }
        return null;
    }

    public static LipidClasses lipidClassNameToLipidClass(String lipidClassName) {
        LipidClasses[] lipidClasses = LipidClasses.class.getEnumConstants();
        for (LipidClasses lipidClass : lipidClasses) {
            if (lipidClass.getName().equals(lipidClassName)) {
                return lipidClass;
            }
        }
        return null;
    }

}
