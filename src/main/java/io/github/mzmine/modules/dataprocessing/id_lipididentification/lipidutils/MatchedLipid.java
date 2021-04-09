package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils;

import java.util.Set;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidFragment;

public class MatchedLipid {

  private ILipidAnnotation lipidAnnotation;
  private Double accurateMz;
  private IonizationType ionizationType;
  private Set<LipidFragment> matchedFragments;
  private Double msMsScore;
  private String comment;

  public MatchedLipid(ILipidAnnotation lipidAnnotation, Double accurateMz,
      IonizationType ionizationType, Set<LipidFragment> matchedFragments, Double msMsScore) {
    this.lipidAnnotation = lipidAnnotation;
    this.accurateMz = accurateMz;
    this.ionizationType = ionizationType;
    this.matchedFragments = matchedFragments;
    this.msMsScore = msMsScore;
  }

  public ILipidAnnotation getLipidAnnotation() {
    return lipidAnnotation;
  }

  public void setLipidAnnotation(ILipidAnnotation lipidAnnotation) {
    this.lipidAnnotation = lipidAnnotation;
  }

  public Double getAccurateMz() {
    return accurateMz;
  }

  public void setAccurateMz(Double accurateMz) {
    this.accurateMz = accurateMz;
  }

  public IonizationType getIonizationType() {
    return ionizationType;
  }

  public void setIonizationType(IonizationType ionizationType) {
    this.ionizationType = ionizationType;
  }

  public Set<LipidFragment> getMatchedFragments() {
    return matchedFragments;
  }

  public void setMatchedFragments(Set<LipidFragment> matchedFragments) {
    this.matchedFragments = matchedFragments;
  }

  public Double getMsMsScore() {
    return msMsScore;
  }

  public void setMsMsScore(Double msMsScore) {
    this.msMsScore = msMsScore;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  @Override
  public String toString() {
    return lipidAnnotation.getAnnotation();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((accurateMz == null) ? 0 : accurateMz.hashCode());
    result = prime * result + ((comment == null) ? 0 : comment.hashCode());
    result = prime * result + ((ionizationType == null) ? 0 : ionizationType.hashCode());
    result = prime * result + ((lipidAnnotation.getAnnotation() == null) ? 0
        : lipidAnnotation.getAnnotation().hashCode());
    result = prime * result + ((matchedFragments == null) ? 0 : matchedFragments.hashCode());
    result = prime * result + ((msMsScore == null) ? 0 : msMsScore.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    MatchedLipid other = (MatchedLipid) obj;
    if (accurateMz == null) {
      if (other.accurateMz != null)
        return false;
    } else if (!accurateMz.equals(other.accurateMz))
      return false;
    if (comment == null) {
      if (other.comment != null)
        return false;
    } else if (!comment.equals(other.comment))
      return false;
    if (ionizationType != other.ionizationType)
      return false;
    if (lipidAnnotation.getAnnotation() == null) {
      if (other.lipidAnnotation.getAnnotation() != null)
        return false;
    } else if (!lipidAnnotation.getAnnotation().equals(other.lipidAnnotation.getAnnotation()))
      return false;
    if (matchedFragments == null) {
      if (other.matchedFragments != null)
        return false;
    } else if (!matchedFragments.equals(other.matchedFragments))
      return false;
    if (msMsScore == null) {
      if (other.msMsScore != null)
        return false;
    } else if (!msMsScore.equals(other.msMsScore))
      return false;
    return true;
  }


}
