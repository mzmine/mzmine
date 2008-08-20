package net.sf.mzmine.modules.identification.pubchem;

public enum TypeOfIonization {
	NO_IONIZATION("No ionization", 0, 0), 
	POSITIVE_HYDROGEN("(+) Hydrogen", -1, 1.00794f), 
	NEGATIVE_HYDROGEN("(-) Hydrogen", 1, 1.00794f), 
	POSITIVE_POTASIO("(+) Potassium", -1, 39.0983f), 
	POSITIVE_SODIUM("(+) Sodium", -1, 22.98976928f); 

	private final String name; 
	private final int sign;
	private float mass; 

	TypeOfIonization(String name, int sign, float mass) {
		this.name = name;
		this.sign = sign;
		this.mass = mass;
	}
	
	public String typename(){
		return name;
	}
	
	public int getSign(){
		return sign;
	}
	
	public float getMass(){
		return mass;
	}

	public String toString(){
		return name;
	}
	
}
