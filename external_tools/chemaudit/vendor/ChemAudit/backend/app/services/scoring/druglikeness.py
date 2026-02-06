"""
Drug-likeness Scorer

Calculates drug-likeness metrics including:
- Lipinski's Rule of Five (Ro5)
- QED (Quantitative Estimate of Drug-likeness)
- Veber Rules
- Rule of Three (Ro3) for fragments
- Ghose Filter
- Egan Filter
- Muegge Filter
"""

from dataclasses import dataclass, field
from typing import Dict, Optional

from rdkit import Chem
from rdkit.Chem import QED, Crippen, Descriptors, Lipinski, rdMolDescriptors


@dataclass
class LipinskiResult:
    """Lipinski's Rule of Five results."""

    passed: bool
    violations: int
    mw: float
    logp: float
    hbd: int
    hba: int
    details: Dict[str, bool] = field(default_factory=dict)


@dataclass
class QEDResult:
    """QED score results."""

    score: float
    properties: Dict[str, float]
    interpretation: str


@dataclass
class VeberResult:
    """Veber rules results."""

    passed: bool
    rotatable_bonds: int
    tpsa: float


@dataclass
class RuleOfThreeResult:
    """Rule of Three (fragment-likeness) results."""

    passed: bool
    violations: int
    mw: float
    logp: float
    hbd: int
    hba: int
    rotatable_bonds: int
    tpsa: float


@dataclass
class GhoseResult:
    """Ghose filter results."""

    passed: bool
    violations: int
    mw: float
    logp: float
    atom_count: int
    molar_refractivity: float


@dataclass
class EganResult:
    """Egan filter results."""

    passed: bool
    logp: float
    tpsa: float


@dataclass
class MueggeResult:
    """Muegge filter results."""

    passed: bool
    violations: int
    details: Dict[str, bool] = field(default_factory=dict)


@dataclass
class DrugLikenessResult:
    """Complete drug-likeness scoring results."""

    lipinski: LipinskiResult
    qed: QEDResult
    veber: VeberResult
    ro3: RuleOfThreeResult
    ghose: Optional[GhoseResult] = None
    egan: Optional[EganResult] = None
    muegge: Optional[MueggeResult] = None
    interpretation: str = ""


class DrugLikenessScorer:
    """
    Scores molecules for drug-likeness using multiple established filters.

    Implements:
    - Lipinski's Rule of Five: MW<=500, LogP<=5, HBD<=5, HBA<=10
    - QED: Composite score 0-1 based on 8 molecular properties
    - Veber: RotBonds<=10, TPSA<=140
    - Rule of Three: Fragment screening criteria
    - Ghose: MW 160-480, LogP -0.4 to 5.6, atoms 20-70, MR 40-130
    - Egan: LogP<=5.88, TPSA<=131.6
    - Muegge: Multi-parameter stringent filter
    """

    # Lipinski thresholds
    LIPINSKI_MW = 500
    LIPINSKI_LOGP = 5
    LIPINSKI_HBD = 5
    LIPINSKI_HBA = 10

    # Veber thresholds
    VEBER_ROTBONDS = 10
    VEBER_TPSA = 140

    # Rule of Three thresholds
    RO3_MW = 300
    RO3_LOGP = 3
    RO3_HBD = 3
    RO3_HBA = 3
    RO3_ROTBONDS = 3
    RO3_TPSA = 60

    # Ghose thresholds
    GHOSE_MW_RANGE = (160, 480)
    GHOSE_LOGP_RANGE = (-0.4, 5.6)
    GHOSE_ATOMS_RANGE = (20, 70)
    GHOSE_MR_RANGE = (40, 130)

    # Egan thresholds
    EGAN_LOGP = 5.88
    EGAN_TPSA = 131.6

    # Muegge thresholds
    MUEGGE_MW_RANGE = (200, 600)
    MUEGGE_LOGP_RANGE = (-2, 5)
    MUEGGE_TPSA = 150
    MUEGGE_RINGS = 7
    MUEGGE_CARBONS = 4
    MUEGGE_HETEROATOMS = 1
    MUEGGE_ROTBONDS = 15
    MUEGGE_HBD = 5
    MUEGGE_HBA = 10

    def score(self, mol: Chem.Mol, include_extended: bool = True) -> DrugLikenessResult:
        """
        Calculate drug-likeness scores for a molecule.

        Args:
            mol: RDKit molecule object
            include_extended: Include Ghose, Egan, Muegge filters

        Returns:
            DrugLikenessResult with all scoring components
        """
        # Calculate common descriptors once
        mw = Descriptors.MolWt(mol)
        logp = Crippen.MolLogP(mol)
        hbd = Lipinski.NumHDonors(mol)
        hba = Lipinski.NumHAcceptors(mol)
        rotatable_bonds = Lipinski.NumRotatableBonds(mol)
        tpsa = Descriptors.TPSA(mol)

        # Core filters
        lipinski = self._calculate_lipinski(mw, logp, hbd, hba)
        qed_result = self._calculate_qed(mol)
        veber = self._calculate_veber(rotatable_bonds, tpsa)
        ro3 = self._calculate_ro3(mw, logp, hbd, hba, rotatable_bonds, tpsa)

        # Extended filters
        ghose = None
        egan = None
        muegge = None

        if include_extended:
            atom_count = mol.GetNumHeavyAtoms()
            mr = Crippen.MolMR(mol)
            ghose = self._calculate_ghose(mw, logp, atom_count, mr)
            egan = self._calculate_egan(logp, tpsa)
            muegge = self._calculate_muegge(
                mol, mw, logp, tpsa, rotatable_bonds, hbd, hba
            )

        # Generate interpretation
        interpretation = self._get_interpretation(lipinski, qed_result, veber, ro3)

        return DrugLikenessResult(
            lipinski=lipinski,
            qed=qed_result,
            veber=veber,
            ro3=ro3,
            ghose=ghose,
            egan=egan,
            muegge=muegge,
            interpretation=interpretation,
        )

    def _calculate_lipinski(
        self, mw: float, logp: float, hbd: int, hba: int
    ) -> LipinskiResult:
        """Calculate Lipinski's Rule of Five."""
        details = {
            "mw_ok": mw <= self.LIPINSKI_MW,
            "logp_ok": logp <= self.LIPINSKI_LOGP,
            "hbd_ok": hbd <= self.LIPINSKI_HBD,
            "hba_ok": hba <= self.LIPINSKI_HBA,
        }

        violations = sum(1 for ok in details.values() if not ok)
        passed = violations <= 1  # Allow 1 violation

        return LipinskiResult(
            passed=passed,
            violations=violations,
            mw=round(mw, 2),
            logp=round(logp, 2),
            hbd=hbd,
            hba=hba,
            details=details,
        )

    def _calculate_qed(self, mol: Chem.Mol) -> QEDResult:
        """Calculate QED score."""
        try:
            score = QED.qed(mol)
            properties = QED.properties(mol)

            # Convert named tuple to dict
            prop_dict = {
                "mw": round(properties.MW, 2),
                "alogp": round(properties.ALOGP, 2),
                "hba": properties.HBA,
                "hbd": properties.HBD,
                "psa": round(properties.PSA, 2),
                "rotb": properties.ROTB,
                "arom": properties.AROM,
                "alerts": properties.ALERTS,
            }

            # Interpretation
            if score >= 0.67:
                interpretation = "Favorable drug-likeness"
            elif score >= 0.49:
                interpretation = "Moderate drug-likeness"
            else:
                interpretation = "Unfavorable drug-likeness"

            return QEDResult(
                score=round(score, 3),
                properties=prop_dict,
                interpretation=interpretation,
            )
        except Exception as e:
            return QEDResult(
                score=0.0,
                properties={},
                interpretation=f"QED calculation failed: {str(e)}",
            )

    def _calculate_veber(self, rotatable_bonds: int, tpsa: float) -> VeberResult:
        """Calculate Veber rules for oral bioavailability."""
        passed = rotatable_bonds <= self.VEBER_ROTBONDS and tpsa <= self.VEBER_TPSA

        return VeberResult(
            passed=passed,
            rotatable_bonds=rotatable_bonds,
            tpsa=round(tpsa, 2),
        )

    def _calculate_ro3(
        self,
        mw: float,
        logp: float,
        hbd: int,
        hba: int,
        rotatable_bonds: int,
        tpsa: float,
    ) -> RuleOfThreeResult:
        """Calculate Rule of Three for fragment-likeness."""
        violations = 0

        if mw >= self.RO3_MW:
            violations += 1
        if logp > self.RO3_LOGP:
            violations += 1
        if hbd > self.RO3_HBD:
            violations += 1
        if hba > self.RO3_HBA:
            violations += 1
        if rotatable_bonds > self.RO3_ROTBONDS:
            violations += 1
        if tpsa > self.RO3_TPSA:
            violations += 1

        return RuleOfThreeResult(
            passed=violations == 0,
            violations=violations,
            mw=round(mw, 2),
            logp=round(logp, 2),
            hbd=hbd,
            hba=hba,
            rotatable_bonds=rotatable_bonds,
            tpsa=round(tpsa, 2),
        )

    def _calculate_ghose(
        self, mw: float, logp: float, atom_count: int, mr: float
    ) -> GhoseResult:
        """Calculate Ghose filter."""
        violations = 0

        if not (self.GHOSE_MW_RANGE[0] <= mw <= self.GHOSE_MW_RANGE[1]):
            violations += 1
        if not (self.GHOSE_LOGP_RANGE[0] <= logp <= self.GHOSE_LOGP_RANGE[1]):
            violations += 1
        if not (self.GHOSE_ATOMS_RANGE[0] <= atom_count <= self.GHOSE_ATOMS_RANGE[1]):
            violations += 1
        if not (self.GHOSE_MR_RANGE[0] <= mr <= self.GHOSE_MR_RANGE[1]):
            violations += 1

        return GhoseResult(
            passed=violations == 0,
            violations=violations,
            mw=round(mw, 2),
            logp=round(logp, 2),
            atom_count=atom_count,
            molar_refractivity=round(mr, 2),
        )

    def _calculate_egan(self, logp: float, tpsa: float) -> EganResult:
        """Calculate Egan filter for absorption."""
        passed = logp <= self.EGAN_LOGP and tpsa <= self.EGAN_TPSA

        return EganResult(
            passed=passed,
            logp=round(logp, 2),
            tpsa=round(tpsa, 2),
        )

    def _calculate_muegge(
        self,
        mol: Chem.Mol,
        mw: float,
        logp: float,
        tpsa: float,
        rotatable_bonds: int,
        hbd: int,
        hba: int,
    ) -> MueggeResult:
        """Calculate Muegge filter."""
        # Count carbons and heteroatoms
        carbon_count = sum(1 for atom in mol.GetAtoms() if atom.GetSymbol() == "C")
        heteroatom_count = sum(
            1 for atom in mol.GetAtoms() if atom.GetSymbol() not in ("C", "H")
        )
        ring_count = rdMolDescriptors.CalcNumRings(mol)

        details = {
            "mw_ok": self.MUEGGE_MW_RANGE[0] <= mw <= self.MUEGGE_MW_RANGE[1],
            "logp_ok": self.MUEGGE_LOGP_RANGE[0] <= logp <= self.MUEGGE_LOGP_RANGE[1],
            "tpsa_ok": tpsa <= self.MUEGGE_TPSA,
            "rings_ok": ring_count <= self.MUEGGE_RINGS,
            "carbons_ok": carbon_count > self.MUEGGE_CARBONS,
            "heteroatoms_ok": heteroatom_count > self.MUEGGE_HETEROATOMS,
            "rotbonds_ok": rotatable_bonds <= self.MUEGGE_ROTBONDS,
            "hbd_ok": hbd <= self.MUEGGE_HBD,
            "hba_ok": hba <= self.MUEGGE_HBA,
        }

        violations = sum(1 for ok in details.values() if not ok)

        return MueggeResult(
            passed=violations == 0,
            violations=violations,
            details=details,
        )

    def _get_interpretation(
        self,
        lipinski: LipinskiResult,
        qed: QEDResult,
        veber: VeberResult,
        ro3: RuleOfThreeResult,
    ) -> str:
        """Generate overall drug-likeness interpretation."""
        parts = []

        # Lipinski assessment
        if lipinski.passed:
            if lipinski.violations == 0:
                parts.append("Fully compliant with Lipinski's Rule of Five.")
            else:
                parts.append(
                    f"Lipinski compliant ({lipinski.violations} violation allowed)."
                )
        else:
            parts.append(
                f"Lipinski violations: {lipinski.violations} "
                "(oral bioavailability may be limited)."
            )

        # QED assessment
        parts.append(f"QED score: {qed.score} ({qed.interpretation.lower()}).")

        # Veber assessment
        if veber.passed:
            parts.append("Good predicted oral bioavailability (Veber rules).")
        else:
            issues = []
            if veber.rotatable_bonds > self.VEBER_ROTBONDS:
                issues.append("high flexibility")
            if veber.tpsa > self.VEBER_TPSA:
                issues.append("high polarity")
            parts.append(f"Oral bioavailability concerns: {', '.join(issues)}.")

        # Fragment assessment
        if ro3.passed:
            parts.append("Fragment-like (suitable for FBDD).")

        return " ".join(parts)


# Module-level convenience function
_scorer = DrugLikenessScorer()


def calculate_druglikeness(
    mol: Chem.Mol, include_extended: bool = True
) -> DrugLikenessResult:
    """
    Calculate drug-likeness scores for a molecule.

    Args:
        mol: RDKit molecule object
        include_extended: Include Ghose, Egan, Muegge filters

    Returns:
        DrugLikenessResult with all scoring components
    """
    return _scorer.score(mol, include_extended)
