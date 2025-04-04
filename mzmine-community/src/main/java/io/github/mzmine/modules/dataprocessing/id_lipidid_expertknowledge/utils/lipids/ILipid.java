package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.lipids;

import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts.CommonAdductNegative;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts.CommonAdductPositive;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts.CommonISFNegative;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts.CommonISFPositive;

import java.util.Map;

public interface ILipid {

    String getName();
    String getAbbr();
    boolean getContainsNitrogen();
    boolean getIsPolar();

    Map<CommonAdductPositive, Integer> getPositiveAdducts();
    Map<CommonAdductNegative, Integer> getNegativeAdducts();
    Map<CommonISFPositive, Integer> getPositiveISF();
    Map<CommonISFNegative, Integer> getNegativeISF();
}

