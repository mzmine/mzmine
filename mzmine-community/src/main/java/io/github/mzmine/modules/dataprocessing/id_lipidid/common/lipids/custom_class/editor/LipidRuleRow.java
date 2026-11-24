package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.editor;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRuleRating;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidRuleModification;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidRuleObservation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidRuleTarget;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import io.mzio.general.Result;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One editable row in the rule editor of the custom lipid class editor. The rule type is split into
 * target and observation. Chain rules have a plus and a minus formula, e.g., plus H and minus O,
 * which are stored as one chain plus or minus formula rule.
 */
public class LipidRuleRow {

  /**
   * One or more formula parts with an optional sign, like +O2-H2, see
   * {@link io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidChainFormulaModification}
   */
  private static final Pattern SIGNED_FORMULA = Pattern.compile("^[+-]?[^+-]+([+-][^+-]+)*$");
  private static final Pattern SIGNED_PART = Pattern.compile("([+-])([^+-]+)");

  private final ObjectProperty<@NotNull IonizationType> ionization = new SimpleObjectProperty<>(
      IonizationType.POSITIVE_HYDROGEN);
  /**
   * null for a rule that only defines an MS1 adduct without MS2 fragment
   */
  private final ObjectProperty<@Nullable LipidRuleTarget> target = new SimpleObjectProperty<>(
      LipidRuleTarget.HEAD_GROUP);
  private final ObjectProperty<@NotNull LipidRuleObservation> observation = new SimpleObjectProperty<>(
      LipidRuleObservation.FRAGMENT_ION);
  // head group fragment ion or neutral loss
  private final StringProperty formula = new SimpleStringProperty("");
  // chain modification, atoms added to and removed from the chain
  private final StringProperty plusFormula = new SimpleStringProperty("");
  private final StringProperty minusFormula = new SimpleStringProperty("");
  private final ObjectProperty<@NotNull LipidAnnotationLevel> level = new SimpleObjectProperty<>(
      LipidAnnotationLevel.SPECIES_LEVEL);
  private final ObjectProperty<@NotNull LipidFragmentationRuleRating> rating = new SimpleObjectProperty<>(
      LipidFragmentationRuleRating.MAJOR);
  private final IntegerProperty relativeIntensity = new SimpleIntegerProperty(0);
  private final StringBinding issue;
  private final StringBinding description;

  public LipidRuleRow() {
    target.addListener((_, oldTarget, newTarget) -> {
      // decision: keep the observation valid when the target changes, e.g., the precursor can
      // only be a fragment ion
      final List<LipidRuleObservation> observations = newTarget == null ? List.of()
          : LipidFragmentationRuleType.validObservations(newTarget, getModification());
      if (!observations.isEmpty() && !observations.contains(observation.get())) {
        observation.set(observations.getFirst());
      }
      // decision: chain fragments identify chains, head group and precursor confirm the class
      final boolean wasChain = oldTarget != null && oldTarget.getChainType() != null;
      final boolean isChain = newTarget != null && newTarget.getChainType() != null;
      if (wasChain != isChain) {
        level.set(isChain ? LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL
            : LipidAnnotationLevel.SPECIES_LEVEL);
      }
    });
    issue = Bindings.createStringBinding(() -> {
      final Result result = validate();
      return result.isOk() ? null : result.message();
    }, ionization, target, observation, formula, plusFormula, minusFormula, relativeIntensity);
    description = Bindings.createStringBinding(this::describe, target, observation, formula,
        plusFormula, minusFormula);
  }

  /**
   * @param rule a rule of a lipid class
   * @return a row with the values of the rule
   */
  public static @NotNull LipidRuleRow of(@NotNull LipidFragmentationRule rule) {
    final LipidRuleRow row = new LipidRuleRow();
    row.ionization.set(rule.getIonizationType());
    final LipidFragmentationRuleType type = rule.getLipidFragmentationRuleType();
    final String ruleFormula = Objects.requireNonNullElse(rule.getMolecularFormula(), "").strip();
    if (type == null) {
      row.target.set(null);
    } else {
      row.target.set(type.getTarget());
      row.observation.set(type.getObservation());
      switch (type.getModification()) {
        case NONE -> row.formula.set(ruleFormula);
        case PLUS_FORMULA -> row.setChainFormula(ruleFormula, false);
        case MINUS_FORMULA -> row.setChainFormula(ruleFormula, true);
      }
    }
    if (rule.getLipidFragmentInformationLevelType() != null) {
      row.level.set(rule.getLipidFragmentInformationLevelType());
    }
    if (rule.getLipidFragmentationRuleRating() != null) {
      row.rating.set(rule.getLipidFragmentationRuleRating());
    }
    row.relativeIntensity.set(rule.getRelativeIntensityWeight());
    return row;
  }

  /**
   * Splits a stored chain formula into plus and minus parts.
   *
   * @param minusRule true for minus formula rules, which subtract the whole formula
   */
  private void setChainFormula(@NotNull String stored, boolean minusRule) {
    final String f = stored.replaceAll("\\s+", "");
    final StringBuilder plus = new StringBuilder();
    final StringBuilder minus = new StringBuilder();
    final boolean signed = SIGNED_FORMULA.matcher(f).matches() && (f.startsWith("+")
        || f.startsWith("-") || f.indexOf('+', 1) > 0 || f.indexOf('-', 1) > 0);
    if (signed) {
      final Matcher parts = SIGNED_PART.matcher(f.startsWith("+") || f.startsWith("-") ? f
          : "+" + f);
      while (parts.find()) {
        ("+".equals(parts.group(1)) ? plus : minus).append(parts.group(2));
      }
    } else {
      plus.append(f);
    }
    plusFormula.set((minusRule ? minus : plus).toString());
    minusFormula.set((minusRule ? plus : minus).toString());
  }

  /**
   * @return a copy of this row
   */
  public @NotNull LipidRuleRow copy() {
    final LipidRuleRow row = new LipidRuleRow();
    row.ionization.set(ionization.get());
    row.target.set(target.get());
    row.observation.set(observation.get());
    row.formula.set(formula.get());
    row.plusFormula.set(plusFormula.get());
    row.minusFormula.set(minusFormula.get());
    row.level.set(level.get());
    row.rating.set(rating.get());
    row.relativeIntensity.set(relativeIntensity.get());
    return row;
  }

  /**
   * @return the modification defined by the plus and minus formulas of chain rules
   */
  public @NotNull LipidRuleModification getModification() {
    final LipidRuleTarget t = target.get();
    if (t == null || t.getChainType() == null) {
      return LipidRuleModification.NONE;
    }
    final boolean plus = !normalized(plusFormula.get()).isEmpty();
    final boolean minus = !normalized(minusFormula.get()).isEmpty();
    if (!plus && !minus) {
      return LipidRuleModification.NONE;
    }
    // decision: plus and minus together are stored as signed formula in a plus rule. Only minus
    // stays a minus rule so that simple classes remain readable by older mzmine versions
    return plus ? LipidRuleModification.PLUS_FORMULA : LipidRuleModification.MINUS_FORMULA;
  }

  /**
   * @return the formula as stored in the rule, e.g., H, O or +H-O for chain rules
   */
  public @NotNull String getStoredFormula() {
    return switch (getModification()) {
      case NONE -> Objects.requireNonNullElse(formula.get(), "").strip();
      case MINUS_FORMULA -> normalized(minusFormula.get());
      case PLUS_FORMULA -> {
        final String minus = normalized(minusFormula.get());
        yield minus.isEmpty() ? normalized(plusFormula.get())
            : "+" + normalized(plusFormula.get()) + "-" + minus;
      }
    };
  }

  /**
   * Users may type +H in the plus field or -O in the minus field, the sign is implied.
   */
  private static @NotNull String normalized(@Nullable String formula) {
    return formula == null ? "" : formula.replaceAll("\\s+", "").replaceAll("^[+-]+", "");
  }

  /**
   * @return the rule type of the combination or empty if it is not supported or MS1 only
   */
  public @NotNull Optional<LipidFragmentationRuleType> getRuleType() {
    final LipidRuleTarget t = target.get();
    return t == null ? Optional.empty()
        : LipidFragmentationRuleType.of(t, getModification(), observation.get());
  }

  /**
   * @return ok or an error message describing what is wrong with this row
   */
  public @NotNull Result validate() {
    final LipidRuleTarget t = target.get();
    if (t == null) {
      return Result.ok();
    }
    final Optional<LipidFragmentationRuleType> type = getRuleType();
    if (type.isEmpty()) {
      return Result.error("%s cannot be observed as %s".formatted(t,
          observation.get().toString().toLowerCase()));
    }
    if (relativeIntensity.get() < 0 || relativeIntensity.get() > 100) {
      return Result.error("Relative intensity must be 0-100");
    }
    if (t.getChainType() != null) {
      for (final String part : List.of(plusFormula.get(), minusFormula.get())) {
        final String f = normalized(part);
        if (!f.isEmpty() && (f.contains("+") || f.contains("-")
            || FormulaUtils.createMajorIsotopeMolFormulaWithCharge(f) == null)) {
          return Result.error("Cannot read the formula " + part.strip());
        }
      }
    }
    final Result check = LipidFragmentationRule.validate(type.get(), getStoredFormula());
    return check.isOk() ? check : Result.error(check.message().strip());
  }

  /**
   * @return the rule or null if the row is invalid
   */
  public @Nullable LipidFragmentationRule toRule() {
    if (!validate().isOk()) {
      return null;
    }
    final IonizationType ion = ionization.get();
    final Optional<LipidFragmentationRuleType> type = getRuleType();
    if (type.isEmpty()) {
      // MS1 adduct only
      return new LipidFragmentationRule(ion.getPolarity(), ion);
    }
    final String f = type.get().requiresFromulaFragment() ? getStoredFormula() : "";
    return new LipidFragmentationRule(ion.getPolarity(), ion, type.get(), level.get(), f,
        rating.get(), relativeIntensity.get());
  }

  public ObjectProperty<@NotNull IonizationType> ionizationProperty() {
    return ionization;
  }

  public ObjectProperty<@Nullable LipidRuleTarget> targetProperty() {
    return target;
  }

  public ObjectProperty<@NotNull LipidRuleObservation> observationProperty() {
    return observation;
  }

  /**
   * @return formula of a head group fragment ion or neutral loss
   */
  public StringProperty formulaProperty() {
    return formula;
  }

  /**
   * @return atoms added to the chain, e.g., H
   */
  public StringProperty plusFormulaProperty() {
    return plusFormula;
  }

  /**
   * @return atoms removed from the chain, e.g., O
   */
  public StringProperty minusFormulaProperty() {
    return minusFormula;
  }

  public ObjectProperty<@NotNull LipidAnnotationLevel> levelProperty() {
    return level;
  }

  public ObjectProperty<@NotNull LipidFragmentationRuleRating> ratingProperty() {
    return rating;
  }

  public IntegerProperty relativeIntensityProperty() {
    return relativeIntensity;
  }

  /**
   * @return a plain sentence like "Neutral loss of acyl chain + H − O" without the adduct
   */
  public @NotNull String describe() {
    final LipidRuleTarget t = target.get();
    if (t == null) {
      return "MS1 adduct only, no fragment";
    }
    final String f = Objects.requireNonNullElse(formula.get(), "").strip();
    final String shown = f.isEmpty() ? "?" : f;
    final String part = switch (t) {
      case PRECURSOR -> "Precursor ion";
      case HEAD_GROUP -> observation.get() == LipidRuleObservation.NEUTRAL_LOSS ? shown
          : "Head group " + shown;
      default -> {
        final String plus = normalized(plusFormula.get());
        final String minus = normalized(minusFormula.get());
        yield t + (plus.isEmpty() ? "" : " + " + plus) + (minus.isEmpty() ? "" : " − " + minus);
      }
    };
    if (observation.get() == LipidRuleObservation.NEUTRAL_LOSS) {
      // decision: keep formulas as typed, only the chain names start lower case in a sentence
      final boolean startsWithName = t != LipidRuleTarget.HEAD_GROUP;
      return "Neutral loss of " + (startsWithName ? Character.toLowerCase(part.charAt(0))
          + part.substring(1) : part);
    }
    return t == LipidRuleTarget.PRECURSOR ? part : part + " fragment";
  }

  /**
   * @return binding of {@link #describe()}
   */
  public StringBinding descriptionBinding() {
    return description;
  }

  /**
   * @return a binding with the error message of {@link #validate()} or null if the row is valid
   */
  public StringBinding issueBinding() {
    return issue;
  }
}
