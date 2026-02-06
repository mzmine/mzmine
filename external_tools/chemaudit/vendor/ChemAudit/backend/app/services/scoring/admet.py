"""
ADMET Scorer

Calculates ADMET-related predictions:
- Synthetic Accessibility Score (SAscore)
- ESOL Solubility (LogS)
- Fraction sp3 (Fsp3) - complexity/3D character
- CNS MPO Score
- Bioavailability indicators
"""

from dataclasses import dataclass
from typing import Optional

from rdkit import Chem
from rdkit.Chem import Crippen, Descriptors, Lipinski, rdMolDescriptors


@dataclass
class SyntheticAccessibilityResult:
    """Synthetic accessibility score result."""

    score: float
    classification: str  # "easy", "moderate", "difficult"
    interpretation: str


@dataclass
class SolubilityResult:
    """ESOL solubility prediction result."""

    log_s: float  # log mol/L
    solubility_mg_ml: float
    classification: str  # "highly_soluble", "soluble", "moderate", "poor", "insoluble"
    interpretation: str


@dataclass
class ComplexityResult:
    """Molecular complexity metrics."""

    fsp3: float
    num_stereocenters: int
    num_rings: int
    num_aromatic_rings: int
    bertz_ct: float
    classification: str  # "flat", "moderate", "3d"
    interpretation: str


@dataclass
class CNSMPOResult:
    """CNS MPO score result."""

    score: float
    components: dict
    cns_penetrant: bool
    interpretation: str


@dataclass
class BioavailabilityResult:
    """Bioavailability indicators."""

    tpsa: float
    rotatable_bonds: int
    hbd: int
    hba: int
    mw: float
    logp: float
    oral_absorption_likely: bool
    cns_penetration_likely: bool
    interpretation: str


@dataclass
class PfizerRuleResult:
    """Pfizer 3/75 Rule result."""

    passed: bool
    logp: float
    tpsa: float
    interpretation: str


@dataclass
class GSKRuleResult:
    """GSK 4/400 Rule result."""

    passed: bool
    mw: float
    logp: float
    interpretation: str


@dataclass
class GoldenTriangleResult:
    """Golden Triangle (Abbott) analysis."""

    in_golden_triangle: bool
    mw: float
    logd: float  # Using LogP as proxy for LogD at pH 7.4
    interpretation: str


@dataclass
class ADMETResult:
    """Complete ADMET prediction results."""

    synthetic_accessibility: SyntheticAccessibilityResult
    solubility: SolubilityResult
    complexity: ComplexityResult
    cns_mpo: Optional[CNSMPOResult]
    bioavailability: BioavailabilityResult
    pfizer_rule: Optional[PfizerRuleResult] = None
    gsk_rule: Optional[GSKRuleResult] = None
    golden_triangle: Optional[GoldenTriangleResult] = None
    molar_refractivity: Optional[float] = None
    interpretation: str = ""


class ADMETScorer:
    """
    Calculates ADMET-related predictions for molecules.

    Implements:
    - SAscore: Synthetic accessibility (1-10, lower = easier)
    - ESOL: Aqueous solubility prediction
    - Fsp3: Fraction of sp3 carbons (complexity)
    - CNS MPO: CNS multiparameter optimization score
    - Bioavailability indicators
    """

    # ESOL model coefficients (Delaney 2004)
    ESOL_INTERCEPT = 0.16
    ESOL_LOGP_COEF = -0.63
    ESOL_MW_COEF = -0.0062
    ESOL_ROTB_COEF = 0.066
    ESOL_AP_COEF = -0.74

    # SAscore fragment contributions (simplified)
    # Full implementation would use the trained model from RDKit Contrib

    def score(
        self, mol: Chem.Mol, include_cns_mpo: bool = True, include_rules: bool = True
    ) -> ADMETResult:
        """
        Calculate ADMET predictions for a molecule.

        Args:
            mol: RDKit molecule object
            include_cns_mpo: Include CNS MPO score calculation
            include_rules: Include Pfizer/GSK/Golden Triangle rules

        Returns:
            ADMETResult with all ADMET predictions
        """
        # Calculate common descriptors
        mw = Descriptors.MolWt(mol)
        logp = Crippen.MolLogP(mol)
        tpsa = Descriptors.TPSA(mol)
        rotatable_bonds = Lipinski.NumRotatableBonds(mol)
        hbd = Lipinski.NumHDonors(mol)
        hba = Lipinski.NumHAcceptors(mol)
        molar_refractivity = Crippen.MolMR(mol)

        # Calculate individual components
        sa_result = self._calculate_synthetic_accessibility(mol)
        solubility = self._calculate_esol(mol, logp, mw, rotatable_bonds)
        complexity = self._calculate_complexity(mol)
        bioavailability = self._calculate_bioavailability(
            mw, logp, tpsa, rotatable_bonds, hbd, hba
        )

        # CNS MPO (optional)
        cns_mpo = None
        if include_cns_mpo:
            cns_mpo = self._calculate_cns_mpo(mw, logp, tpsa, hbd)

        # Additional rules
        pfizer_rule = None
        gsk_rule = None
        golden_triangle = None
        if include_rules:
            pfizer_rule = self._calculate_pfizer_rule(logp, tpsa)
            gsk_rule = self._calculate_gsk_rule(mw, logp)
            golden_triangle = self._calculate_golden_triangle(mw, logp)

        # Overall interpretation
        interpretation = self._get_interpretation(
            sa_result,
            solubility,
            complexity,
            cns_mpo,
            bioavailability,
            pfizer_rule,
            gsk_rule,
        )

        return ADMETResult(
            synthetic_accessibility=sa_result,
            solubility=solubility,
            complexity=complexity,
            cns_mpo=cns_mpo,
            bioavailability=bioavailability,
            pfizer_rule=pfizer_rule,
            gsk_rule=gsk_rule,
            golden_triangle=golden_triangle,
            molar_refractivity=round(molar_refractivity, 2),
            interpretation=interpretation,
        )

    def _calculate_synthetic_accessibility(
        self, mol: Chem.Mol
    ) -> SyntheticAccessibilityResult:
        """
        Calculate synthetic accessibility score.

        Uses a heuristic approach based on:
        - Ring complexity
        - Stereochemistry
        - Unusual elements
        - Size

        Full SAscore would use the trained model from RDKit Contrib.
        """
        try:
            # Try to use RDKit's SA_Score if available
            try:
                from rdkit.Contrib.SA_Score import sascorer

                score = sascorer.calculateScore(mol)
            except ImportError:
                # Fallback: heuristic calculation
                score = self._calculate_sa_heuristic(mol)

            # Classification
            if score < 4:
                classification = "easy"
                interpretation = "Likely straightforward to synthesize"
            elif score < 6:
                classification = "moderate"
                interpretation = "Moderate synthetic complexity"
            else:
                classification = "difficult"
                interpretation = "Challenging synthesis expected"

            return SyntheticAccessibilityResult(
                score=round(score, 2),
                classification=classification,
                interpretation=interpretation,
            )
        except Exception as e:
            return SyntheticAccessibilityResult(
                score=5.0,
                classification="unknown",
                interpretation=f"Calculation error: {str(e)}",
            )

    def _calculate_sa_heuristic(self, mol: Chem.Mol) -> float:
        """Heuristic SA score when trained model unavailable."""
        score = 1.0  # Base score

        # Size penalty based on heavy atom count
        num_atoms = mol.GetNumHeavyAtoms()
        size_penalties = [(50, 2.0), (35, 1.0), (25, 0.5)]
        for threshold, penalty in size_penalties:
            if num_atoms > threshold:
                score += penalty
                break

        # Ring complexity penalty
        ring_info = mol.GetRingInfo()
        num_rings = ring_info.NumRings()
        ring_penalties = [(5, 2.0), (3, 1.0)]
        for threshold, penalty in ring_penalties:
            if num_rings > threshold:
                score += penalty
                break

        # Bridged/spiro atoms penalty
        try:
            num_bridgehead = rdMolDescriptors.CalcNumBridgeheadAtoms(mol)
            num_spiro = rdMolDescriptors.CalcNumSpiroAtoms(mol)
            score += 0.5 * (num_bridgehead + num_spiro)
        except Exception:
            pass

        # Stereocenters penalty
        try:
            num_chiral = len(Chem.FindMolChiralCenters(mol, includeUnassigned=True))
            chiral_penalties = [(4, 2.0), (2, 1.0), (0, 0.5)]
            for threshold, penalty in chiral_penalties:
                if num_chiral > threshold:
                    score += penalty
                    break
        except Exception:
            pass

        # Unusual elements penalty
        common_elements = {"C", "N", "O", "S", "F", "Cl", "Br", "H"}
        unusual_count = sum(
            1 for atom in mol.GetAtoms() if atom.GetSymbol() not in common_elements
        )
        score += 0.5 * unusual_count

        # Macrocycle penalty (rings > 8 atoms)
        if any(len(ring) > 8 for ring in ring_info.AtomRings()):
            score += 1.0

        return min(10.0, max(1.0, score))

    def _calculate_esol(
        self, mol: Chem.Mol, logp: float, mw: float, rotatable_bonds: int
    ) -> SolubilityResult:
        """
        Calculate ESOL aqueous solubility prediction.

        Based on Delaney (2004) ESOL model.
        """
        try:
            # Calculate aromatic proportion
            num_atoms = mol.GetNumHeavyAtoms()
            aromatic_atoms = sum(1 for atom in mol.GetAtoms() if atom.GetIsAromatic())
            aromatic_proportion = aromatic_atoms / num_atoms if num_atoms > 0 else 0

            # ESOL equation: log(Sw) = 0.16 - 0.63*cLogP - 0.0062*MW + 0.066*RB - 0.74*AP
            log_s = (
                self.ESOL_INTERCEPT
                + self.ESOL_LOGP_COEF * logp
                + self.ESOL_MW_COEF * mw
                + self.ESOL_ROTB_COEF * rotatable_bonds
                + self.ESOL_AP_COEF * aromatic_proportion
            )

            # Convert to mg/mL (approximate)
            # log_s is in mol/L, convert to mg/mL
            solubility_mol_l = 10**log_s
            solubility_mg_ml = solubility_mol_l * mw / 1000

            # Classification
            if log_s >= -1:
                classification = "highly_soluble"
                interpretation = "Highly soluble (> 100 mg/mL)"
            elif log_s >= -3:
                classification = "soluble"
                interpretation = "Soluble (1-100 mg/mL)"
            elif log_s >= -4:
                classification = "moderate"
                interpretation = "Moderately soluble (0.1-1 mg/mL)"
            elif log_s >= -5:
                classification = "poor"
                interpretation = "Poorly soluble (< 0.1 mg/mL)"
            else:
                classification = "insoluble"
                interpretation = "Very poorly soluble"

            return SolubilityResult(
                log_s=round(log_s, 2),
                solubility_mg_ml=round(solubility_mg_ml, 4),
                classification=classification,
                interpretation=interpretation,
            )
        except Exception as e:
            return SolubilityResult(
                log_s=0.0,
                solubility_mg_ml=0.0,
                classification="unknown",
                interpretation=f"Calculation error: {str(e)}",
            )

    def _calculate_complexity(self, mol: Chem.Mol) -> ComplexityResult:
        """Calculate molecular complexity metrics."""
        try:
            # Fsp3 (fraction of sp3 carbons)
            fsp3 = rdMolDescriptors.CalcFractionCSP3(mol)

            # Stereocenters
            try:
                chiral_centers = Chem.FindMolChiralCenters(mol, includeUnassigned=True)
                num_stereocenters = len(chiral_centers)
            except Exception:
                num_stereocenters = 0

            # Ring counts
            num_rings = rdMolDescriptors.CalcNumRings(mol)
            num_aromatic_rings = Descriptors.NumAromaticRings(mol)

            # Bertz complexity
            try:
                from rdkit.Chem import GraphDescriptors

                bertz_ct = GraphDescriptors.BertzCT(mol)
            except Exception:
                bertz_ct = 0.0

            # Classification based on Fsp3
            if fsp3 < 0.25:
                classification = "flat"
                interpretation = "Flat/aromatic molecule. May have selectivity issues."
            elif fsp3 < 0.42:
                classification = "moderate"
                interpretation = "Moderate 3D character."
            else:
                classification = "3d"
                interpretation = (
                    "Good 3D character. Associated with better clinical outcomes."
                )

            return ComplexityResult(
                fsp3=round(fsp3, 3),
                num_stereocenters=num_stereocenters,
                num_rings=num_rings,
                num_aromatic_rings=num_aromatic_rings,
                bertz_ct=round(bertz_ct, 1),
                classification=classification,
                interpretation=interpretation,
            )
        except Exception as e:
            return ComplexityResult(
                fsp3=0.0,
                num_stereocenters=0,
                num_rings=0,
                num_aromatic_rings=0,
                bertz_ct=0.0,
                classification="unknown",
                interpretation=f"Calculation error: {str(e)}",
            )

    def _calculate_cns_mpo(
        self, mw: float, logp: float, tpsa: float, hbd: int
    ) -> CNSMPOResult:
        """
        Calculate CNS MPO (Multiparameter Optimization) score.

        Based on Wager et al. (2010) - Pfizer CNS MPO algorithm.
        Score 0-6, higher is better for CNS penetration.
        """
        try:
            components = {}

            # MW component (desirability function)
            if mw <= 360:
                components["mw"] = 1.0
            elif mw <= 500:
                components["mw"] = 1.0 - (mw - 360) / 140
            else:
                components["mw"] = 0.0

            # LogP component
            if logp <= 3:
                components["logp"] = 1.0
            elif logp <= 5:
                components["logp"] = 1.0 - (logp - 3) / 2
            else:
                components["logp"] = 0.0

            # TPSA component
            if tpsa <= 40:
                components["tpsa"] = 1.0
            elif tpsa <= 90:
                components["tpsa"] = 1.0 - (tpsa - 40) / 50
            else:
                components["tpsa"] = 0.0

            # HBD component
            if hbd == 0:
                components["hbd"] = 1.0
            elif hbd <= 3:
                components["hbd"] = 1.0 - hbd * 0.25
            else:
                components["hbd"] = 0.0

            # LogD and pKa would require additional calculations
            # Using simplified 4-parameter version
            components["logd"] = 0.5  # Placeholder
            components["pka"] = 0.5  # Placeholder

            # Calculate total score (sum of components)
            score = sum(components.values())

            # Interpretation
            cns_penetrant = score >= 4
            if score >= 5:
                interpretation = "Excellent CNS penetration predicted"
            elif score >= 4:
                interpretation = "Good CNS penetration predicted"
            elif score >= 3:
                interpretation = "Moderate CNS penetration"
            else:
                interpretation = "Poor CNS penetration predicted"

            return CNSMPOResult(
                score=round(score, 2),
                components={k: round(v, 2) for k, v in components.items()},
                cns_penetrant=cns_penetrant,
                interpretation=interpretation,
            )
        except Exception as e:
            return CNSMPOResult(
                score=0.0,
                components={},
                cns_penetrant=False,
                interpretation=f"Calculation error: {str(e)}",
            )

    def _calculate_bioavailability(
        self,
        mw: float,
        logp: float,
        tpsa: float,
        rotatable_bonds: int,
        hbd: int,
        hba: int,
    ) -> BioavailabilityResult:
        """Calculate bioavailability indicators."""
        # Oral absorption prediction (based on multiple rules)
        lipinski_ok = mw <= 500 and logp <= 5 and hbd <= 5 and hba <= 10
        veber_ok = rotatable_bonds <= 10 and tpsa <= 140
        oral_absorption_likely = lipinski_ok and veber_ok

        # CNS penetration prediction
        cns_penetration_likely = (
            tpsa <= 90 and mw <= 450 and hbd <= 3 and logp >= 1 and logp <= 4
        )

        # Interpretation
        parts = []
        if oral_absorption_likely:
            parts.append("Good predicted oral absorption.")
        else:
            issues = []
            if mw > 500:
                issues.append("high MW")
            if logp > 5:
                issues.append("high lipophilicity")
            if tpsa > 140:
                issues.append("high polarity")
            if rotatable_bonds > 10:
                issues.append("high flexibility")
            parts.append(f"Oral absorption concerns: {', '.join(issues)}.")

        if cns_penetration_likely:
            parts.append("Likely CNS penetrant.")
        else:
            parts.append("Limited CNS penetration expected.")

        return BioavailabilityResult(
            tpsa=round(tpsa, 2),
            rotatable_bonds=rotatable_bonds,
            hbd=hbd,
            hba=hba,
            mw=round(mw, 2),
            logp=round(logp, 2),
            oral_absorption_likely=oral_absorption_likely,
            cns_penetration_likely=cns_penetration_likely,
            interpretation=" ".join(parts),
        )

    def _calculate_pfizer_rule(self, logp: float, tpsa: float) -> PfizerRuleResult:
        """
        Calculate Pfizer 3/75 Rule.

        Compounds with LogP > 3 AND TPSA < 75 Å² have higher risk of toxicity.
        Based on Hughes et al. (2008) - Pfizer's analysis of compound promiscuity.
        """
        # Rule: LogP > 3 AND TPSA < 75 indicates toxicity risk
        at_risk = logp > 3 and tpsa < 75
        passed = not at_risk

        if passed:
            interpretation = "Passes Pfizer 3/75 rule - lower toxicity risk."
        else:
            interpretation = (
                f"Fails Pfizer 3/75 rule (LogP={logp:.1f}>3, TPSA={tpsa:.0f}<75). "
                "Higher risk of toxicity and promiscuity."
            )

        return PfizerRuleResult(
            passed=passed,
            logp=round(logp, 2),
            tpsa=round(tpsa, 2),
            interpretation=interpretation,
        )

    def _calculate_gsk_rule(self, mw: float, logp: float) -> GSKRuleResult:
        """
        Calculate GSK 4/400 Rule.

        Compounds with MW <= 400 AND LogP <= 4 tend to have better outcomes.
        Based on Gleeson (2008) - GSK's analysis of ADMET properties.
        """
        # Rule: MW <= 400 AND LogP <= 4 is favorable
        passed = mw <= 400 and logp <= 4

        if passed:
            interpretation = "Passes GSK 4/400 rule - favorable ADMET profile expected."
        else:
            issues = []
            if mw > 400:
                issues.append(f"MW={mw:.0f}>400")
            if logp > 4:
                issues.append(f"LogP={logp:.1f}>4")
            interpretation = (
                f"Fails GSK 4/400 rule ({', '.join(issues)}). "
                "May have suboptimal ADMET properties."
            )

        return GSKRuleResult(
            passed=passed,
            mw=round(mw, 2),
            logp=round(logp, 2),
            interpretation=interpretation,
        )

    def _calculate_golden_triangle(
        self, mw: float, logp: float
    ) -> GoldenTriangleResult:
        """
        Calculate Golden Triangle (Abbott) analysis.

        The Golden Triangle defines a region of MW and LogD/LogP where
        compounds are more likely to have good permeability and metabolic stability.
        Region: MW 200-450, LogP/LogD -0.5 to 5
        Based on Johnson et al. (2009) - Abbott's analysis.
        """
        # Using LogP as proxy for LogD at pH 7.4 (rough approximation)
        in_triangle = 200 <= mw <= 450 and -0.5 <= logp <= 5

        if in_triangle:
            interpretation = "Within Golden Triangle - favorable permeability and metabolic stability."
        else:
            issues = []
            if mw < 200:
                issues.append(f"MW={mw:.0f}<200")
            elif mw > 450:
                issues.append(f"MW={mw:.0f}>450")
            if logp < -0.5:
                issues.append(f"LogP={logp:.1f}<-0.5")
            elif logp > 5:
                issues.append(f"LogP={logp:.1f}>5")
            interpretation = (
                f"Outside Golden Triangle ({', '.join(issues)}). "
                "May have permeability or metabolic stability concerns."
            )

        return GoldenTriangleResult(
            in_golden_triangle=in_triangle,
            mw=round(mw, 2),
            logd=round(logp, 2),  # LogP as proxy
            interpretation=interpretation,
        )

    def _get_interpretation(
        self,
        sa: SyntheticAccessibilityResult,
        sol: SolubilityResult,
        complexity: ComplexityResult,
        cns: Optional[CNSMPOResult],
        bioav: BioavailabilityResult,
        pfizer: Optional[PfizerRuleResult] = None,
        gsk: Optional[GSKRuleResult] = None,
    ) -> str:
        """Generate overall ADMET interpretation."""
        parts = []

        # Synthesis
        parts.append(f"Synthesis: {sa.classification} (SA score: {sa.score}).")

        # Solubility
        parts.append(f"Solubility: {sol.classification} (LogS: {sol.log_s}).")

        # Complexity
        parts.append(
            f"Complexity: Fsp3={complexity.fsp3} ({complexity.classification})."
        )

        # Bioavailability
        if bioav.oral_absorption_likely:
            parts.append("Oral bioavailability predicted.")
        else:
            parts.append("Oral bioavailability concerns.")

        # Safety rules
        if pfizer and not pfizer.passed:
            parts.append("Pfizer 3/75: toxicity risk.")
        if gsk and not gsk.passed:
            parts.append("GSK 4/400: suboptimal ADMET.")

        return " ".join(parts)


# Module-level convenience function
_scorer = ADMETScorer()


def calculate_admet(
    mol: Chem.Mol, include_cns_mpo: bool = True, include_rules: bool = True
) -> ADMETResult:
    """
    Calculate ADMET predictions for a molecule.

    Args:
        mol: RDKit molecule object
        include_cns_mpo: Include CNS MPO score
        include_rules: Include Pfizer/GSK/Golden Triangle rules

    Returns:
        ADMETResult with all ADMET predictions
    """
    return _scorer.score(mol, include_cns_mpo, include_rules)
