package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.lipids;

import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts.CommonAdductNegative;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts.CommonAdductPositive;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts.CommonISFNegative;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts.CommonISFPositive;

import java.util.HashMap;
import java.util.Map;
/**
 * This enum contains the most common lipids according to a CEMBIO paper:
 * Martínez, S., Fernández-García, M., Londoño-Osorio, S., Barbas, C., & Gradillas, A. (2024). Highly reliable LC-MS lipidomics database for efficient human plasma profiling based on NIST SRM 19501. Journal of Lipid Research, 65(11), 100671. https://doi.org/10.1016/j.jlr.2024.100671
 * It represents nineteen lipids.
 */
public enum Lipid {
    CAR("Carnitines", "CAR", true, true,
            new HashMap<>() {{
                // ESI+ Adducts
                put(CommonAdductPositive.M_PLUS_H, 1);
            }},
            new HashMap<>() {{
                // ESI- Adducts (empty)
            }},
            new HashMap<>() {{
                // ESI+ ISF (empty)
            }},
            new HashMap<>() {{
                // ESI- ISF (empty)
            }}),
    LPC("Monoacylglycerophosphocholines", "LPC", true, true,
            new HashMap<>() {{
                // ESI+
                put(CommonAdductPositive.M_PLUS_H, 1);
                put(CommonAdductPositive.M_PLUS_NA, 2);
                put(CommonAdductPositive.M_PLUS_K, 3);
                put(CommonAdductPositive.M_PLUS_C2H7N2, 2);
            }},
            new HashMap<>() {{
                // ESI-
                put(CommonAdductNegative.M_PLUS_CH3COO, 1);
                put(CommonAdductNegative.M_PLUS_CH3COO_PLUS_CH3COONa, 2);
                put(CommonAdductNegative.M_PLUS_CH3COO_PLUS_2CH3COONa, 3);
                put(CommonAdductNegative.M_PLUS_CH3COO_PLUS_3CH3COONa, 3);
                put(CommonAdductNegative.M_PLUS_HCOO, 3);
                put(CommonAdductNegative.M_PLUS_HCOO_PLUS_CH3COONa, 3);
                put(CommonAdductNegative.M_PLUS_CL, 3);
            }},
            new HashMap<>() {{
                // ESI+ ISF
                put(CommonISFPositive.M_PLUS_H_MINUS_H2O, 1);
            }},
            new HashMap<>() {{
                // ESI- ISF
                put(CommonISFNegative.M_PLUS_CH3COO_MINUS_CH3COOCH3, 1);
            }}),
    LPC_OP("Monoalkylglycerophosphocholines", "LPC O/P", true, true,
            new HashMap<>() {
                {
                    //ESI+
                    put(CommonAdductPositive.M_PLUS_H, 1);
                    put(CommonAdductPositive.M_PLUS_NA, 2);
                    put(CommonAdductPositive.M_PLUS_K, 6);
                    put(CommonAdductPositive.M_PLUS_C2H7N2, 6);
                }
            },
            new HashMap<>() {
                {
                    //ESI-
                    put(CommonAdductNegative.M_PLUS_CH3COO, 1);
                    put(CommonAdductNegative.M_PLUS_CH3COO_PLUS_CH3COONa, 2);
                    put(CommonAdductNegative.M_PLUS_CH3COO_PLUS_2CH3COONa, 6);
                    put(CommonAdductNegative.M_PLUS_CH3COO_PLUS_3CH3COONa, 6);
                    put(CommonAdductNegative.M_PLUS_HCOO, 6);
                    put(CommonAdductNegative.M_PLUS_HCOO_PLUS_CH3COONa, 6);
                    put(CommonAdductNegative.M_PLUS_CL, 6);
                }
            },
            new HashMap<>() {
                {
                    //ESI+
                    put(CommonISFPositive.M_PLUS_H_MINUS_H2O, 6);
                }
            },
            new HashMap<>() {
                {
                    //ESI-
                    put(CommonISFNegative.M_PLUS_CH3COO_MINUS_CH3COOCH3, 6);
                }
            }),
    PC("Diacylglycerophosphocholines", "PC", true, true, new HashMap<>() {
        {
            //ESI+
            put(CommonAdductPositive.M_PLUS_H, 1);
            put(CommonAdductPositive.M_PLUS_NA, 2);
            put(CommonAdductPositive.M_PLUS_K, 3);
            put(CommonAdductPositive.M_PLUS_C2H7N2, 2);
        }
    },
            new HashMap<>() {
                {
                    //ESI-
                    put(CommonAdductNegative.M_PLUS_CH3COO, 1);
                    put(CommonAdductNegative.M_PLUS_CH3COO_PLUS_CH3COONa, 2);
                    put(CommonAdductNegative.M_PLUS_CH3COO_PLUS_2CH3COONa, 3);
                    put(CommonAdductNegative.M_PLUS_CH3COO_PLUS_3CH3COONa, 4);
                    put(CommonAdductNegative.M_PLUS_HCOO, 5);
                    put(CommonAdductNegative.M_PLUS_HCOO_PLUS_CH3COONa, 5);
                    put(CommonAdductNegative.M_PLUS_CL, 5);
                }
            },
            new HashMap<>() {
                {
                }
            },
            new HashMap<>() {{
                //ESI-
                put(CommonISFNegative.M_PLUS_CH3COO_MINUS_CH3COOCH3, 1);
            }}),
    PC_OP("Alkylacylglycerophosphocholines", "PC O/P", true, true, new HashMap<>() {
        {
            //ESI+
            put(CommonAdductPositive.M_PLUS_H, 1);
            put(CommonAdductPositive.M_PLUS_NA, 2);
            put(CommonAdductPositive.M_PLUS_K, 6);
            put(CommonAdductPositive.M_PLUS_C2H7N2, 2);
        }
    },
            new HashMap<>() {
                {
                    //ESI-
                    put(CommonAdductNegative.M_PLUS_CH3COO, 1);
                    put(CommonAdductNegative.M_PLUS_CH3COO_PLUS_CH3COONa, 2);
                    put(CommonAdductNegative.M_PLUS_CH3COO_PLUS_2CH3COONa, 3);
                    put(CommonAdductNegative.M_PLUS_CH3COO_PLUS_3CH3COONa, 3);
                    put(CommonAdductNegative.M_PLUS_HCOO, 6);
                    put(CommonAdductNegative.M_PLUS_HCOO_PLUS_CH3COONa, 6);
                    put(CommonAdductNegative.M_PLUS_CL, 6);
                }
            },
            new HashMap<>() {
                {
                }
            },
            new HashMap<>() {{
                //ESI-
                put(CommonISFNegative.M_PLUS_CH3COO_MINUS_CH3COOCH3, 1);
            }}),
    LPE("Monoacylglycerophosphoethanolamines", "LPE", true, true, new HashMap<>() {
        {
            //ESI+
            put(CommonAdductPositive.M_PLUS_H, 1);
            put(CommonAdductPositive.M_PLUS_NA, 2);
            put(CommonAdductPositive.M_PLUS_K, 6);
            put(CommonAdductPositive.M_PLUS_C2H7N2, 6);
        }
    },
            new HashMap<>() {
                {
                    //ESI-
                    put(CommonAdductNegative.M_MINUS_H, 1);
                    put(CommonAdductNegative.M_MINUS_H_PLUS_CH3COONa, 2);
                }
            },
            new HashMap<>() {
                {
                    //ESI+
                    put(CommonISFPositive.M_PLUS_H_MINUS_H2O, 6);
                }
            },
            new HashMap<>() {{

            }}),
    LPE_OP("Monoalkylglycerophosphoethanolamines", "LPE O/P", true, true, new HashMap<>() {
        {
            //ESI+
            put(CommonAdductPositive.M_PLUS_H, 1);
            put(CommonAdductPositive.M_PLUS_NA, 6);
            put(CommonAdductPositive.M_PLUS_K, 6);
            put(CommonAdductPositive.M_PLUS_C2H7N2, 6);
        }
    },
            new HashMap<>() {
                {
                    //ESI-
                    put(CommonAdductNegative.M_MINUS_H, 1);
                    put(CommonAdductNegative.M_MINUS_H_PLUS_CH3COONa, 6);
                }
            },
            new HashMap<>() {
                {
                    //ESI+
                    put(CommonISFPositive.M_PLUS_H_MINUS_H2O, 6);
                }
            },
            new HashMap<>() {{

            }}),
    PE("Diacylglycerophosphoethanolamines", "PE", true, true, new HashMap<>() {
        {
            //ESI+
            put(CommonAdductPositive.M_PLUS_H, 1);
            put(CommonAdductPositive.M_PLUS_NA, 2);
            put(CommonAdductPositive.M_PLUS_K, 6);
            put(CommonAdductPositive.M_PLUS_C2H7N2, 6);
        }
    },
            new HashMap<>() {
                {
                    //ESI-
                    put(CommonAdductNegative.M_MINUS_H, 1);
                    put(CommonAdductNegative.M_MINUS_H_PLUS_CH3COONa, 2);
                }
            },
            new HashMap<>() {{
            }},
            new HashMap<>() {{
            }}),
    PE_OP("Acyl/Alkyl/Alkenyl-glycerophosphoethanolamines", "PE O/P", true, true, new HashMap<>() {
        {
            //ESI+
            put(CommonAdductPositive.M_PLUS_H, 1);
            put(CommonAdductPositive.M_PLUS_NA, 2);
            put(CommonAdductPositive.M_PLUS_K, 3);
            put(CommonAdductPositive.M_PLUS_C2H7N2, 3);
        }
    },
            new HashMap<>() {
                {
                    //ESI-
                    put(CommonAdductNegative.M_MINUS_H, 1);
                    put(CommonAdductNegative.M_MINUS_H_PLUS_CH3COONa, 2);
                }
            },
            new HashMap<>() {{
            }},
            new HashMap<>() {{
            }}),
    SM("Sphingomyelins", "SM", true, true, new HashMap<>() {
        {
            //ESI+
            put(CommonAdductPositive.M_PLUS_H, 1);
            put(CommonAdductPositive.M_PLUS_NA, 2);
            put(CommonAdductPositive.M_PLUS_K, 3);
            put(CommonAdductPositive.M_PLUS_C2H7N2, 3);
        }
    },
            new HashMap<>() {
                {
                    //ESI-
                    put(CommonAdductNegative.M_PLUS_CH3COO, 1);
                    put(CommonAdductNegative.M_PLUS_CH3COO_PLUS_CH3COONa, 2);
                    put(CommonAdductNegative.M_PLUS_CH3COO_PLUS_2CH3COONa, 3);
                    put(CommonAdductNegative.M_PLUS_CH3COO_PLUS_3CH3COONa, 3);
                    put(CommonAdductNegative.M_PLUS_HCOO, 3);
                    put(CommonAdductNegative.M_PLUS_HCOO_PLUS_CH3COONa, 3);
                    put(CommonAdductNegative.M_PLUS_CL, 3);
                }
            },
            new HashMap<>() {{
            }},
            new HashMap<>() {{
                //ESI-
                put(CommonISFNegative.M_PLUS_CH3COO_MINUS_CH3COOCH3, 1);
            }}),
    Cer("Ceramides", "Cer", true, true, new HashMap<>() {
        {
            //ESI+
            put(CommonAdductPositive.M_PLUS_H, 1);
            put(CommonAdductPositive.M_PLUS_NA, 2);
            put(CommonAdductPositive.M_PLUS_K, 3);
            put(CommonAdductPositive.M_PLUS_C2H7N2, 1);
        }
    },
            new HashMap<>() {
                {

                    //ESI-
                    put(CommonAdductNegative.M_MINUS_H, 2);
                    put(CommonAdductNegative.M_PLUS_CH3COO, 1);
                    put(CommonAdductNegative.M_PLUS_HCOO, 3);
                    put(CommonAdductNegative.M_PLUS_CL, 2);
                }
            },
            new HashMap<>() {
                {
                    //ESI+
                    put(CommonISFPositive.M_PLUS_H_MINUS_H2O, 1);
                }
            },
            new HashMap<>() {{
            }}),
    HexCer("Hexosylceramides", "HexCer", true, true, new HashMap<>() {
        {
            //ESI+
            put(CommonAdductPositive.M_PLUS_H, 1);
            put(CommonAdductPositive.M_PLUS_NA, 2);
            put(CommonAdductPositive.M_PLUS_K, 3);
        }
    },
            new HashMap<>() {
                {

                    //ESI-
                    put(CommonAdductNegative.M_MINUS_H, 2);
                    put(CommonAdductNegative.M_MINUS_H_PLUS_CH3COONa, 4);
                    put(CommonAdductNegative.M_PLUS_CH3COO, 1);
                    put(CommonAdductNegative.M_PLUS_CL, 3);
                }
            },
            new HashMap<>() {
                {

                    //ESI+
                    put(CommonISFPositive.M_PLUS_H_MINUS_H2O, 1);
                }
            },
            new HashMap<>() {{
            }}),
    FA("Fatty acids", "FA", false, true, new HashMap<>() {
        {
        }
    },
            new HashMap<>() {
                {
                    //ESI-
                    put(CommonAdductNegative.M_MINUS_H, 1);
                    put(CommonAdductNegative.M_MINUS_H_PLUS_CH3COONa, 2);
                }
            },
            new HashMap<>() {{
            }},
            new HashMap<>() {{
            }}),
    LPI("Monoacylglycerophosphoinositols", "LPI", false, true, new HashMap<>() {
        {
            //ESI+
            put(CommonAdductPositive.M_PLUS_H, 6);
            put(CommonAdductPositive.M_PLUS_NA, 6);
            put(CommonAdductPositive.M_PLUS_K, 6);
            put(CommonAdductPositive.M_PLUS_C2H7N2, 6);
            put(CommonAdductPositive.M_PLUS_NH4, 6);
        }
    },
            new HashMap<>() {
                {
                    //ESI-
                    put(CommonAdductNegative.M_MINUS_H, 1);
                    put(CommonAdductNegative.M_MINUS_H_PLUS_CH3COONa, 6);
                }
            },
            new HashMap<>() {
                {
                    //ESI+
                    put(CommonISFPositive.M_PLUS_H_MINUS_H2O, 6);
                }
            },
            new HashMap<>() {{
            }}),
    PI("Diacylglycerophosphoinositols", "PI", false, true, new HashMap<>() {
        {
            //ESI+
            put(CommonAdductPositive.M_PLUS_H, 1);
            put(CommonAdductPositive.M_PLUS_NA, 1);
            put(CommonAdductPositive.M_PLUS_K, 3);
            put(CommonAdductPositive.M_PLUS_C2H7N2, 2);
            put(CommonAdductPositive.M_PLUS_NH4, 1);
        }
    },
            new HashMap<>() {
                {
                    //ESI-
                    put(CommonAdductNegative.M_MINUS_H, 1);
                    put(CommonAdductNegative.M_MINUS_H_PLUS_CH3COONa, 2);
                }
            },
            new HashMap<>() {{
            }},
            new HashMap<>() {{
            }}),
    DG("Diglycerides", "DG", false, true, new HashMap<>() {
        {
            //ESI+
            put(CommonAdductPositive.M_PLUS_H, 3);
            put(CommonAdductPositive.M_PLUS_NA, 2);
            put(CommonAdductPositive.M_PLUS_K, 3);
            put(CommonAdductPositive.M_PLUS_C2H7N2, 3);
            put(CommonAdductPositive.M_PLUS_NH4, 1);
        }
    },
            new HashMap<>() {
                {
                    //ESI-
                    put(CommonAdductNegative.M_PLUS_CH3COO, 1);
                    put(CommonAdductNegative.M_PLUS_HCOO, 3);
                    put(CommonAdductNegative.M_PLUS_CL, 2);
                }
            },
            new HashMap<>() {
                {
                    //ESI+
                    put(CommonISFPositive.M_PLUS_H_MINUS_H2O, 1);
                }
            },
            new HashMap<>() {{
            }}),
    TG("Triglycerides", "TG", false, false, new HashMap<>() {
        {
            //ESI+
            put(CommonAdductPositive.M_PLUS_H, 4);
            put(CommonAdductPositive.M_PLUS_NA, 2);
            put(CommonAdductPositive.M_PLUS_K, 3);
            put(CommonAdductPositive.M_PLUS_C2H7N2, 1);
            put(CommonAdductPositive.M_PLUS_NH4, 1);
        }
    },
            new HashMap<>() {{
            }},
            new HashMap<>() {{
            }},
            new HashMap<>() {{
            }}),
    CE("Cholesterol esters", "CE", false, false, new HashMap<>() {
        {
            //ESI+
            put(CommonAdductPositive.M_PLUS_H, 3);
            put(CommonAdductPositive.M_PLUS_NA, 1);
            put(CommonAdductPositive.M_PLUS_K, 2);
            put(CommonAdductPositive.M_PLUS_C2H7N2, 4);
            put(CommonAdductPositive.M_PLUS_NH4, 1);
        }
    },
            new HashMap<>() {{
            }},
            new HashMap<>() {
                {
                    //ESI+
                    put(CommonISFPositive.CHOLESTADIENE_ION, 1);
                }
            },
            new HashMap<>() {{
            }}),
    Chol("Cholesterol", "Chol", false, false, new HashMap<>() {
        {
            //ESI+
            put(CommonAdductPositive.M_PLUS_H, 2);
            put(CommonAdductPositive.M_PLUS_NH4, 1);
        }
    },
            new HashMap<>() {{
            }},
            new HashMap<>() {
                {
                    //ESI+
                    put(CommonISFPositive.CHOLESTADIENE_ION, 1);
                }
            },
            new HashMap<>() {{
            }});


    /**
     * Complete name of the lipid (eg: Carnitines).
     */
    private final String name;
    /**
     * Abbreviation of the lipid (eg: CAR).
     */
    private final String abbr;
    /**
     * Stores if it contains nitrogen or not.
     * True = it DOES contain nitrogen, False = it DOES NOT contain nitrogen.
     */
    private final boolean containsNitrogen;
    /**
     * Stores if it is polar or not.
     * True = it IS polar, False = it IS NOT polar.
     */
    private final boolean isPolar;
    /**
     * Relate the different enumerations of adducts and ISF with their hierarchy level.
     * The hierarchy is 1 highest and 5 lowest, 6 is expected but not seen. Detected in in-source fragmentation is also 1.
     * The hierarchy indicates intensity levels, therefore, an adduct with level 1 has higher intensity than an adduct with level 3, for example.
     * This one represents ESI+ adducts.
     */
    private final Map<CommonAdductPositive, Integer> positiveAdducts;
    /**
     * Relate the different enumerations of adducts and ISF with their hierarchy level.
     * The hierarchy is 1 highest and 5 lowest, 6 is expected but not seen. Detected in in-source fragmentation is also 1.
     * The hierarchy indicates intensity levels, therefore, an adduct with level 1 has higher intensity than an adduct with level 3, for example.
     * This one represents ESI- adducts.
     */
    private final Map<CommonAdductNegative, Integer> negativeAdducts;
    /**
     * Relate the different enumerations of adducts and ISF with their hierarchy level.
     * The hierarchy is 1 highest and 5 lowest, 6 is expected but not seen. Detected in in-source fragmentation is also 1.
     * The hierarchy indicates intensity levels, therefore, an adduct with level 1 has higher intensity than an adduct with level 3, for example.
     * This one represents ESI+ ISF.
     */
    private final Map<CommonISFPositive, Integer> positiveISF;
    /**
     * Relate the different enumerations of adducts and ISF with their hierarchy level.
     * The hierarchy is 1 highest and 5 lowest, 6 is expected but not seen. Detected in in-source fragmentation is also 1.
     * The hierarchy indicates intensity levels, therefore, an adduct with level 1 has higher intensity than an adduct with level 3, for example.
     * This one represents ESI- ISF.
     */
    private final Map<CommonISFNegative, Integer> negativeISF;

    /**
     * Creates a new Lipid object with the specified info.
     * @param name The name of the lipid.
     * @param abbr The abbreviation of the lipid.
     * @param containsNitrogen Whether it contains or not nitrogen.
     * @param isPolar Whether it is polar or not.
     * @param positiveAdducts Map with adducts and their hierarchy levels for ESI+.
     * @param negativeAdducts Map with adducts and their hierarchy levels for ESI-.
     * @param positiveISF Map with ISFs and their hierarchy levels for ESI+.
     * @param negativeISF Map with ISFs and their hierarchy levels for ESI-.
     */
    Lipid(String name, String abbr, boolean containsNitrogen, boolean isPolar,
          Map<CommonAdductPositive, Integer> positiveAdducts,
          Map<CommonAdductNegative, Integer> negativeAdducts,
          Map<CommonISFPositive, Integer> positiveISF,
          Map<CommonISFNegative, Integer> negativeISF) {
        this.name = name;
        this.abbr = abbr;
        this.containsNitrogen = containsNitrogen;
        this.isPolar = isPolar;
        this.positiveAdducts = positiveAdducts;
        this.negativeAdducts = negativeAdducts;
        this.positiveISF = positiveISF;
        this.negativeISF = negativeISF;
    }

    /**
     * Gets the full name of the Lipid (eg:Carnitines).
     * @return The name of the lipid.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the abbreviation of the Lipid (eg:CAR).
     * @return The abbreviation of the lipid.
     */
    public String getAbbr() {
        return abbr;
    }
}
