package io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.lipidfragmentannotation;

import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidFragment;
import java.util.List;

public interface ILipidFragmentFactory {

  List<LipidFragment> findLipidFragments();

}
