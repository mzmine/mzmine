package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils;

public enum LipidChainType {

	ACYL_CHAIN("Acyl chain"), //
	ALKYL_CHAIN("Alkyl chain");

	private String name;

	LipidChainType(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}