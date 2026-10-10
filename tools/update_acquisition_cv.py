#!/usr/bin/env python3
"""Refresh canonical acquisition terms from public PSI-MS; never run during application startup.

Review the generated version/hash/diff before committing. The normal build uses the checked-in
TSV and does not fetch metadata or vocabulary over the network. Update acquisition-cv.NOTICE
when updating the vocabulary version; retain attribution and the upstream CC BY 4.0 licence.
"""
from pathlib import Path
import urllib.request,hashlib
url='https://raw.githubusercontent.com/HUPO-PSI/psi-ms-CV/master/psi-ms.obo'
data=urllib.request.urlopen(url, timeout=30).read()
text=data.decode();terms={}
for block in text.split('[Term]\n')[1:]:
 f={}; parents=[]
 for line in block.splitlines():
  if line.startswith('id: '): f['id']=line[4:]
  elif line.startswith('name: '):f['name']=line[6:]
  elif line.startswith('is_a: '):parents.append(line[6:].split()[0])
 if 'id'in f and 'name'in f: terms[f['id']] = (f['name'],parents)
def isa(key,root,seen=None):
 if key==root:return True
 seen=set()if seen is None else seen
 if key in seen:return False
 seen.add(key)
 return any(isa(p,root,seen)for p in terms.get(key,('',[]))[1])
roots={'INSTRUMENT_MODEL':['MS:1000031'],'ANALYZER':['MS:1000443'],'IONIZATION':['MS:1000008'],'DETECTOR':['MS:1000026'],'ACQUISITION_METHOD':['MS:1003213','MS:1003214']}
rows=[]
for cat,rs in roots.items():
 for k,(label,_)in sorted(terms.items()):
  if k.startswith('MS:')and any(isa(k,x)for x in rs):rows.append(f'{cat}\t{k}\t{label}')
out=Path(__file__).resolve().parents[1] / 'mzmine-community/src/main/resources/acquisition-cv.tsv'
out.write_text('# PSI-MS controlled vocabulary: '+url+'\n# Snapshot SHA256 '+hashlib.sha256(data).hexdigest()+'\n# '+text.splitlines()[1]+'\n# Only category, accession and canonical label; no definitions or file-provided text.\n'+'\n'.join(rows)+'\n')
print('Controlled acquisition terms:',len(rows))
