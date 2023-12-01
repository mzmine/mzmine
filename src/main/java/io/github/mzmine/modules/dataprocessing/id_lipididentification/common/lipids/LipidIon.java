package io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids;

import io.github.mzmine.datamodel.IonizationType;

public record LipidIon(ILipidAnnotation lipidAnnotation, IonizationType ionizationType, Double mz) {

}

