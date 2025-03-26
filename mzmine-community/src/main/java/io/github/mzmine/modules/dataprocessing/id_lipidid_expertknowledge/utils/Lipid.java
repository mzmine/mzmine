package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils;

import java.util.HashMap;
import java.util.Map;

public enum Lipid implements ILipid {
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


    private final String name;
    private final String abbr;
    private final boolean containsNitrogen;
    private final boolean isPolar;
    //The hierarchy is 1 highest and 5 lowest, 6 is expected but not seen
    //Detected in in-source fragmentation is also 1
    private final Map<CommonAdductPositive, Integer> positiveAdducts;
    private final Map<CommonAdductNegative, Integer> negativeAdducts;
    private final Map<CommonISFPositive, Integer> positiveISF;
    private final Map<CommonISFNegative, Integer> negativeISF;



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

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getAbbr() {
        return abbr;
    }

    @Override
    public boolean getContainsNitrogen() {
        return containsNitrogen;
    }

    @Override
    public boolean getIsPolar() {
        return isPolar;
    }

    public Map<CommonAdductPositive, Integer> getPositiveAdducts() {
        return positiveAdducts;
    }

    public Map<CommonAdductNegative, Integer> getNegativeAdducts() {
        return negativeAdducts;
    }

    public Map<CommonISFPositive, Integer> getPositiveISF() {
        return positiveISF;
    }

    public Map<CommonISFNegative, Integer> getNegativeISF() {
        return negativeISF;
    }
}
