import type { PubChemResult, ChEMBLResult, COCONUTResult } from '../../types/integrations';
const pubchemLogo = '/assets/logos/pubchem.png';
const chemblLogo = '/assets/logos/chembl.png';
const coconutLogo = '/assets/logos/coconut.png';

interface DatabaseLookupResultsProps {
  results: {
    pubchem: PubChemResult | null;
    chembl: ChEMBLResult | null;
    coconut: COCONUTResult | null;
  };
}

export function DatabaseLookupResults({ results }: DatabaseLookupResultsProps) {
  return (
    <div className="space-y-4">
      {/* PubChem Result */}
      <PubChemCard result={results.pubchem} />

      {/* ChEMBL Result */}
      <ChEMBLCard result={results.chembl} />

      {/* COCONUT Result */}
      <COCONUTCard result={results.coconut} />
    </div>
  );
}

function PubChemCard({ result }: { result: PubChemResult | null }) {
  if (!result) {
    return (
      <div className="border border-gray-200 rounded-lg p-4">
        <div className="flex items-center gap-2">
          <img src={pubchemLogo} alt="PubChem" className="w-5 h-5 rounded-sm object-contain" />
          <span className="font-medium text-gray-900">PubChem</span>
          <span className="text-xs text-gray-400">Failed to query</span>
        </div>
      </div>
    );
  }

  return (
    <div className={`border rounded-lg p-4 ${result.found ? 'border-blue-200 bg-blue-50' : 'border-gray-200'}`}>
      <div className="flex items-center gap-2 mb-2">
        <img src={pubchemLogo} alt="PubChem" className="w-5 h-5 rounded-sm object-contain" />
        <span className="font-medium text-gray-900">PubChem</span>
        {result.found ? (
          <span className="px-2 py-0.5 bg-blue-100 text-blue-800 text-xs rounded-full">Found</span>
        ) : (
          <span className="px-2 py-0.5 bg-gray-100 text-gray-600 text-xs rounded-full">Not Found</span>
        )}
      </div>

      {result.found && (
        <div className="mt-3 space-y-2 text-sm">
          <div className="grid grid-cols-2 gap-2">
            <div>
              <span className="text-gray-500">CID:</span>{' '}
              <span className="font-mono text-blue-700">{result.cid}</span>
            </div>
            {result.iupac_name && (
              <div className="col-span-2">
                <span className="text-gray-500">IUPAC:</span>{' '}
                <span className="text-gray-900">{result.iupac_name}</span>
              </div>
            )}
            {result.molecular_formula && (
              <div>
                <span className="text-gray-500">Formula:</span>{' '}
                <span className="font-mono">{result.molecular_formula}</span>
              </div>
            )}
            {result.molecular_weight && (
              <div>
                <span className="text-gray-500">MW:</span>{' '}
                <span>{result.molecular_weight.toFixed(2)}</span>
              </div>
            )}
          </div>
          {result.synonyms && result.synonyms.length > 0 && (
            <div>
              <span className="text-gray-500">Synonyms:</span>{' '}
              <span className="text-gray-700">{result.synonyms.slice(0, 5).join(', ')}</span>
            </div>
          )}
          {result.url && (
            <a
              href={result.url}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-1 text-blue-600 hover:text-blue-800"
            >
              View on PubChem
              <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
              </svg>
            </a>
          )}
        </div>
      )}
    </div>
  );
}

function ChEMBLCard({ result }: { result: ChEMBLResult | null }) {
  if (!result) {
    return (
      <div className="border border-gray-200 rounded-lg p-4">
        <div className="flex items-center gap-2">
          <img src={chemblLogo} alt="ChEMBL" className="w-5 h-5 rounded-sm object-contain" />
          <span className="font-medium text-gray-900">ChEMBL</span>
          <span className="text-xs text-gray-400">Failed to query</span>
        </div>
      </div>
    );
  }

  const phaseLabels: Record<number, string> = {
    0: 'Preclinical',
    1: 'Phase I',
    2: 'Phase II',
    3: 'Phase III',
    4: 'Approved',
  };

  return (
    <div className={`border rounded-lg p-4 ${result.found ? 'border-purple-200 bg-purple-50' : 'border-gray-200'}`}>
      <div className="flex items-center gap-2 mb-2">
        <img src={chemblLogo} alt="ChEMBL" className="w-5 h-5 rounded-sm object-contain" />
        <span className="font-medium text-gray-900">ChEMBL</span>
        {result.found ? (
          <span className="px-2 py-0.5 bg-purple-100 text-purple-800 text-xs rounded-full">Found</span>
        ) : (
          <span className="px-2 py-0.5 bg-gray-100 text-gray-600 text-xs rounded-full">Not Found</span>
        )}
        {result.found && result.max_phase !== undefined && result.max_phase > 0 && (
          <span className="px-2 py-0.5 bg-yellow-100 text-amber-800 dark:bg-yellow-900/30 dark:text-yellow-400 text-xs rounded-full">
            {phaseLabels[result.max_phase] || `Phase ${result.max_phase}`}
          </span>
        )}
      </div>

      {result.found && (
        <div className="mt-3 space-y-2 text-sm">
          <div className="grid grid-cols-2 gap-2">
            <div>
              <span className="text-gray-500">ChEMBL ID:</span>{' '}
              <span className="font-mono text-purple-700">{result.chembl_id}</span>
            </div>
            {result.pref_name && (
              <div>
                <span className="text-gray-500">Name:</span>{' '}
                <span className="text-gray-900">{result.pref_name}</span>
              </div>
            )}
            {result.molecule_type && (
              <div>
                <span className="text-gray-500">Type:</span>{' '}
                <span>{result.molecule_type}</span>
              </div>
            )}
            <div>
              <span className="text-gray-500">Bioactivities:</span>{' '}
              <span className="font-medium">{result.bioactivity_count}</span>
            </div>
          </div>

          {result.bioactivities.length > 0 && (
            <div className="mt-2">
              <span className="text-gray-500 text-xs">Top bioactivities:</span>
              <div className="mt-1 space-y-1">
                {result.bioactivities.slice(0, 3).map((act, i) => (
                  <div key={i} className="text-xs bg-white rounded px-2 py-1 border border-purple-100">
                    <span className="font-medium">{act.target_name || act.target_chembl_id}</span>
                    {act.activity_value && (
                      <span className="ml-2 text-gray-600">
                        {act.activity_type}: {act.activity_value} {act.activity_unit}
                      </span>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {result.url && (
            <a
              href={result.url}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-1 text-purple-600 hover:text-purple-800"
            >
              View on ChEMBL
              <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
              </svg>
            </a>
          )}
        </div>
      )}
    </div>
  );
}

function COCONUTCard({ result }: { result: COCONUTResult | null }) {
  if (!result) {
    return (
      <div className="border border-gray-200 rounded-lg p-4">
        <div className="flex items-center gap-2">
          <img src={coconutLogo} alt="COCONUT" className="w-5 h-5 rounded-sm object-contain" />
          <span className="font-medium text-gray-900">COCONUT</span>
          <span className="text-xs text-gray-400">Failed to query</span>
        </div>
      </div>
    );
  }

  return (
    <div className={`border rounded-lg p-4 ${result.found ? 'border-amber-200 bg-amber-50 dark:border-amber-800 dark:bg-amber-900/20' : 'border-gray-200 dark:border-gray-700'}`}>
      <div className="flex items-center gap-2 mb-2">
        <img src={coconutLogo} alt="COCONUT" className="w-5 h-5 rounded-sm object-contain" />
        <span className="font-medium text-gray-900 dark:text-gray-100">COCONUT</span>
        {result.found ? (
          <span className="px-2 py-0.5 bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400 text-xs rounded-full">Natural Product</span>
        ) : (
          <span className="px-2 py-0.5 bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 text-xs rounded-full">Not Found</span>
        )}
      </div>

      {result.found && (
        <div className="mt-3 space-y-2 text-sm">
          <div className="grid grid-cols-2 gap-2">
            <div>
              <span className="text-gray-500">COCONUT ID:</span>{' '}
              <span className="font-mono text-amber-700 dark:text-amber-400">{result.coconut_id}</span>
            </div>
            {result.name && (
              <div>
                <span className="text-gray-500">Name:</span>{' '}
                <span className="text-gray-900">{result.name}</span>
              </div>
            )}
            {result.organism && (
              <div className="col-span-2">
                <span className="text-gray-500">Organism:</span>{' '}
                <span className="italic text-gray-700">{result.organism}</span>
                {result.organism_type && (
                  <span className="text-gray-500 ml-1">({result.organism_type})</span>
                )}
              </div>
            )}
            {result.nplikeness != null && (
              <div>
                <span className="text-gray-500">NP-likeness:</span>{' '}
                <span className={result.nplikeness > 0 ? 'text-amber-700 dark:text-amber-400' : 'text-gray-600 dark:text-gray-400'}>
                  {result.nplikeness.toFixed(2)}
                </span>
              </div>
            )}
          </div>

          {result.url && (
            <a
              href={result.url}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-1 text-amber-600 hover:text-amber-800 dark:text-amber-400 dark:hover:text-amber-300"
            >
              View on COCONUT
              <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
              </svg>
            </a>
          )}
        </div>
      )}

      {!result.found && (
        <p className="text-xs text-gray-500 mt-2">
          Not found in the COCONUT natural products database.
        </p>
      )}
    </div>
  );
}
