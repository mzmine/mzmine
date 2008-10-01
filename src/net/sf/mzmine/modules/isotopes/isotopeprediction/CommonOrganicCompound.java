/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */


package net.sf.mzmine.modules.isotopes.isotopeprediction;

public enum CommonOrganicCompound {
	
	GLYCINE ("Gly","C2H5O2N","Glycine"),
	ALANINE ("Ala","C3H7O2N","Alanine"),
	VALINE ("Val","C5H11O2N","Valine"),
	LEUCINE ("Leu","C6H13O2N","Leucine"),
	ISOLEUCINE ("Ile","C6H13O2N","Isoleucine"),
	SERINE ("Ser","C3H7O3N","Serine"),
	THREONINE ("Thr","C4H9O3N","Threonine"),
	CYSTEINE ("Cys","C3H7O2NS","Cysteine"),
	METHIONINE ("Met","C5H11O2NS","Methionine"),
	PROLINE ("Pro","C5H9O2N","Proline"),
	ASPARTIC_ACID ("Asp","C4H7O4N","Aspartic acid"),
	ASPARAGINE ("Asn","C4H8O3N2","Asparagine"),
	GLUTAMIC_ACID ("Glu","C5H9O4N","Glutamic acid"),
	GLUTAMINE ("Gln","C5H10O3N2","Glutamine"),
	HISTIDINE ("His","C6H9O2N3","Histidine"),
	LYSINE ("Lys","C6H14O2N2","Lysine"),
	ARGININE ("Arg","C6H14O2N4","Arginine"),
	PHENYLALANINE ("Phe","C9H11O2N","Phenylalanine"),
	TYROSINE ("Tyr","C9H11O3N","Tyrosine"),
	TRYPTOPHAN ("Trp","C11H12O2N2","Tryptophan"),
	ACETYL ("Ac","C2H3O","Acetyl"),
	ACETOACETYL ("Acac","C4H5O2","Acetoacetyl (AcAc)"),
	ACET ("Acet","C2H3O","Acetyl (usually Ac)"),
	ACETAMIDOMETHYL ("Acm","C3H6NO","Acetamidomethyl"),
	ADAMANTYLOXY ("Adao","C10H15O","Adamantyloxy"),
	WATER ("Aq","H2O","Aqua (water, as ligand)"),
	BIPYRIDYL ("Bipy","C10H8N2","2,2'-Bipyridyl (also bpy; as ligand)"),
	BENZYL ("Bn","C7H7","Benzyl"),
	BORON_CAGE ("Bo","C2B10H10","Boron cage - 2H"),
	TERT_BUTOXYCARBONYL ("Boc","C5H9O2","tert-Butoxycarbonyl"),
	BENZYLOXYMETHYL ("Bom","C8H9O","Benzyloxymethyl"),
	BROMOBENZYLOXYCARBONYL ("Brz","C8H6BrO2","2-Bromobenzyloxycarbonyl"),
	BROSYL  ("Bs","C6H4BrSO2","Brosyl (4-bromobenzenesulfonyl)"),
	N_BUTYL ("Bu","C4H9","n-Butyl"),
	TERT_BUTOXYMETHYL ("Bum","C5H11O","tert-Butoxymethyl"),
	BENZ ("Bzl","C7H7","Benzyl (also Bn)"),
	BENZYLOXY ("Bzlo","C7H7O","Benzyloxy"),
	BENZYLOXYCARBONYL ("Cbz","C8H7O2","Benzyloxycarbonyl"),
	CYCLOHEXYLOXY ("Chxo","C6H11O","Cyclohexyloxy"),
	CHLOROBENZYLOXYCARBONYL ("Clz","C8H6ClO2","2-Chlorobenzyloxycarbonyl"),
	CYCLOPENTADIENYL ("Cp","C5H5","Cyclopentadienyl"),
	CYCLOHEXYL ("Cy","C6H11","Cyclohexyl"),
	DEUTERIUM ("D","[2H]","Deuterium"),
	DANSYL ("Dan","C12H12NSO2","Dansyl"),
	DDE ("Dde","O2H13C10","Dde"),
	DIISOPROPYLPHENYL ("Dip","C12H17","2,6-Diisopropylphenyl"),
	DIMETHOXYBENZYL ("Dmb","C9H11O2","2,6-Dimethoxybenzyl"),
	DINITROPHENYL ("Dnp","C6H3N2O4","2,4-Dinitrophenyl"),
	DIPHENLY_PHENANTHROLINE ("Dpphen","C24H16N2","4,7-Diphenly-1,10-phenanthroline (as ligand)"),
	ETHOXYETHYL ("Ee","C4H9O","1-Ethoxyethyl (EE)"),
	ETHYLENEDIAMINE ("En","C2H8N2","Ethylenediamine (en; as ligand)"),
	ETHYL ("Et","C2H5","Ethyl"),
	FLUORENYLMETHOXYCARBONYL ("Fmoc","C15H11O2","Fluorenylmethoxycarbonyl"),
	FORMYL ("For","CHO","Formyl"),
	N_HEXYL ("Hex","C6H13","n-Hexyl"),
	ISO_BUTYL ("Ibu","C4H9","iso-Butyl (usually i-Bu)"),
	ISOPINOCAMPHENYL ("Ipc","C10H17O","Isopinocamphenyl"),
	ISO_PROPYL ("Ipr","C3H7","iso-Propyl (usually i-Pr)"),
	DIMETHYL_DIOXOCYCLOHEXYLIDENE_METHYLBUTYL ("Ivdde","C14H21O2","1-[4,4-dimethyl-2,6-dioxocyclohexylidene]3-methylbutyl"),
	DIMETHOXYBENZHYDRYL ("Mbh","C15H15O2","4,4'-Dimethoxybenzhydryl"),
	METHYL ("Me","CH3","Methyl"),
	METHYLBENZYL ("Mebzl","C8H9","4-Methylbenzyl"),
	METHOXYETHOXYMETHYL ("Mem","C4H9O2","2-Methoxyethoxymethyl"),
	METHOXYBENZYL_DIMETHYL_DIOXOCYCLOHEXYLIDENE_METHYLBUTYL ("Meobzl","C8H9O","4-Methoxybenzyl1-[4,4-dimethyl-2,6-dioxocyclohexylidene]3-methylbutyl"),
	MESITYL ("Mes","C9H11","Mesityl (2,4,6-trimethylphen-1-yl)"),
	METHOXYTRITYL ("Mmt","C20H17O","4-Methoxytrityl"),
	METHOXYMETHYL ("Mom","C2H5O","Methoxymethyl"),
	PARA_METHOXYPHENYLMETHYL ("Mpm","C7H7O","para-Methoxyphenylmethyl"),
	MESYL ("Ms","CH3SO2","Mesyl (methanesulphonyl)"),
	PENTAMETHYLCHROMAN_SULPHONYL_R ("Mtc","S1O3H19C14","2,2,5,7,8-pentamethylchroman-6-sulphonyl"),
	METHYLTHIOMETHYL ("Mtm","C2H5S","Methylthiomethyl"),
	TRIMETHYLBENZENESULPHONYL ("Mtr","C10H13O3S","4-Methoxy-2,3,6-trimethylbenzenesulphonyl"),
	MESITYLENE_SULPHONYL ("Mts","C9H11O2S","Mesitylene-2-sulphonyl"),
	METHYLTRITYL ("Mtt","C20H17","4-Methyltrityl"),
	NAPHTYL ("Naph","C10H7","Naphtyl (also Np)"),
	NITRO_PYRIDINESULPHENYL ("Npys","C5H3O2N2S","3-Nitro-2-pyridinesulphenyl"),
	PARA_NITROBENZENESULPHONYL ("Ns","C6H4NO5S","para-Nitrobenzenesulphonyl"),
	ODMAB ("Odmab","C20H26NO3","Odmab (2-(ethylhexyl)-4-(dimethylamino)benzoate)"),
	PENTAMETHYLDIHYDROBENZOFURANE_SULFONYL ("Pbf","S1O3H17C13","2,2,4,6,7-pentamethyldihydrobenzofurane-5-sulfonyl"),
	POLYETHYLENEGLYCOL_FRAGMENT ("Peg","C2H4O","-Polyethyleneglycol fragment-"),
	N_PENTYL ("Pent","C5H11","n-Pentyl"),
	PHENYL ("Ph","C6H5","Phenyl"),
	PHENANTHROLINE ("Phen","C12H8N2","1,10-Phenanthroline (phen; as ligand)"),
	PHTHALOYL ("Phth","C8H4O4","Phthaloyl (2-; 1,2-benzenedicarboxyl)"),
	PIVALOYL ("Piv","C5H9O","Pivaloyl (trimethylacetyl; also Pv)"),
	PENTAMETHYLCHROMAN_SULPHONYL ("Pmc","C14H19O3S","2,2,5,7,8-Pentamethylchroman-6-sulphonyl"),
	PARA_METHOXYPHENYL ("Pmp","C7H7O","para-Methoxyphenyl"),
	N_PROPYL ("Prop","C3H7","n-Propyl (usually Pr)"),
	SEC_BUTYL ("Sbu","C4H9","sec-Butyl (usually s-Bu)"),
	TRIMETHYLSILYL_ETHOXYMETHYL ("Sem","C6H15OSi","2-(Trimethylsilyl)ethoxymethyl"),
	SIAMYL ("Sia","C5H11","Siamyl (3-methyl-2-butyl)"),
	TRITIUM ("T","[3H]","Tritium"),
	TRIMETHYLACETAMIDOMETHYL ("Tacm","C6H12NO","Trimethylacetamidomethyl"),
	TERT_BUTYLDIMETHYLSILYL ("Tbdms","C6H15Si","tert-Butyldimethylsilyl (also Tbs)"),
	TERT_BUTYLDIPHENYLSILYL ("Tbdps","C16H19Si","tert-Butyldiphenylsilyl"),
	TRIMETHYLSILYL_METHYL_PHENYL ("Tbt","C27H59Si6","2,4,6-tris[bis(trimethylsilyl)methyl]phenyl"),
	TERT_BUTYL ("Tbu","C4H9","tert-Butyl (usually t-Bu)"),
	TERT_BUTOXY ("Tbuo","C4H9O","tert-Butoxy"),
	TERT_BUTYLTHIO ("Tbuthio","C4H9S","tert-Butylthio"),
	TRIETHYLENEGLYCOL_FRAGMENT ("Teg","C6H12O2","-Triethyleneglycol fragment-"),
	TRIETHYLSILYL ("Tes","C6H15Si","Triethylsilyl"),
	TRIFLUOROMETHANESULFONYL ("Tf","CF3SO2","Triflate, triflyl (trifluoromethanesulfonyl)"),
	TRIFLUOROACETYL ("Tfa","C2F3O","Trifluoroacetyl"),
	THYMIDINE_2H ("Thd","C10H13N2O5","Thymidine - 2H"),
	THEXYL ("Thex","C6H13","Thexyl (2,3-dimethyl-2-butyl) (also Thx)"),
	TETRAHYDROPYRANYL ("Thp","C5H9O2","Tetrahydropyranyl"),
	TRIISOPROPYLSILYL ("Tips","C9H21Si","Triisopropylsilyl"),
	TRIMETHYLSILYL ("Tms","C3H9Si","Trimethylsilyl"),
	P_TOLYL ("Tol","C7H7","p-Tolyl"),
	TOSYL ("Tos","C7H7O2S","Tosyl (also Ts)"),
	TRYPTYL ("Tpt","C20H13","Tryptyl"),
	TRICHLOROETHYLOXYCARBONYL ("Troc","C3H2Cl3O2","Trichloroethyloxycarbonyl"),
	TRITYL ("Trt","C19H15","Trityl (also Tr, Try)"),
	XANTHYL ("Xan","C13H9O","Xanthyl"),
	BENZYLOXYCARBONYL_Z ("Z","C8H7O2","Benzyloxycarbonyl"),
	BENZOYL ("Bz","C7H5O","Benzoyl");
	
	private final String name, formula, description;
	
	CommonOrganicCompound (String name, String formula, String description){
		this.name = name;
		this.formula = formula;
		this.description = description;
	}
	
	public String getName(){
		return this.name;
	}
	
	public String getFormula(){
		return this.formula;
	}
	
	public String getDescription(){
		return this.description;
	}

}
