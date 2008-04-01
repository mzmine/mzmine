package net.sf.mzmine.modules.peakpicking.anothercentroid;

class Bin {

	private ConstructionIsotopePattern pattern;
	private float lowMZ;
	private float highMZ;

	private int currentScanCount = 0;
	private float previouslyOfferedIntensity = 0.0f;

	Bin(ConstructionIsotopePattern pattern, float lowMZ, float highMZ) {
		this.pattern = pattern;
		this.lowMZ = lowMZ;
		this.highMZ = highMZ;
	}

	float getLowMZ() {
		return lowMZ;
	}

	float getHighMZ() {
		return highMZ;
	}

	void offerIntensity(float intensity) {
		if (previouslyOfferedIntensity < intensity)
			previouslyOfferedIntensity = intensity;
	}

	void moveToNextScan() {
		pattern.addIntensity(currentScanCount, previouslyOfferedIntensity);
		previouslyOfferedIntensity = 0.0f;
		currentScanCount++;
	}

}
