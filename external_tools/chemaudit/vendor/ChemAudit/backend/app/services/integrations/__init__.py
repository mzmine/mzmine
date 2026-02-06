"""
External integrations service.

Provides clients for COCONUT, PubChem, and ChEMBL.
"""

from app.services.integrations.chembl import ChEMBLClient, get_bioactivity
from app.services.integrations.coconut import COCONUTClient, lookup_natural_product
from app.services.integrations.pubchem import PubChemClient, get_compound_info

__all__ = [
    "COCONUTClient",
    "lookup_natural_product",
    "PubChemClient",
    "get_compound_info",
    "ChEMBLClient",
    "get_bioactivity",
]
