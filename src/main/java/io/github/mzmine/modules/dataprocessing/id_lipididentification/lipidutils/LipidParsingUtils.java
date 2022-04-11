/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA.
 *
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
