import random
from collections import Counter

import pandas as pd
from tqdm import tqdm

from rdkit import Chem
from rdkit import RDLogger
from rdkit.Chem import Descriptors

random.seed(42)

lg = RDLogger.logger()
lg.setLevel(RDLogger.CRITICAL)

def read_from_sdf(path):
    res = []
    app = False
    with open(path, 'r') as f:
        for line in tqdm(f.readlines(), desc='Loading SDF structures', leave=False):
            if app:
                res.append(line.strip())
                app = False
            if line.startswith('> <SMILES>'):
                app = True

    return res

def filter(mol):
    try:
        smi = Chem.MolToSmiles(mol, isomericSmiles=False) # remove stereochemistry information
        mol = Chem.MolFromSmiles(smi)

        if "." in smi:
            return False
        
        if Descriptors.MolWt(mol) >= 1500:
            return False
        
        for atom in mol.GetAtoms():
            if atom.GetFormalCharge() != 0:
                return False
    except:
        return False
    
    return True

FILTER_ATOMS = {'C', 'N', 'S', 'O', 'F', 'Cl', 'H', 'P'}

def filter_with_atom_types(mol):
    try:
        smi = Chem.MolToSmiles(mol, isomericSmiles=False) # remove stereochemistry information
        mol = Chem.MolFromSmiles(smi)

        if "." in smi:
            return False
        
        if Descriptors.MolWt(mol) >= 1500:
            return False
        
        for atom in mol.GetAtoms():
            if atom.GetFormalCharge() != 0:
                return False
            if atom.GetSymbol() not in FILTER_ATOMS:
                return False
    except:
        return False
    
    return True

########## CANOPUS DATASET ##########

canopus_split = pd.read_csv('../data/canopus/splits/canopus_hplus_100_0.tsv', sep='\t')

canopus_labels = pd.read_csv('../data/canopus/labels.tsv', sep='\t')
canopus_labels["name"] = canopus_labels["spec"]
canopus_labels = canopus_labels[["name", "smiles"]].reset_index(drop=True)

canopus_labels = canopus_labels.merge(canopus_split, on="name")

canopus_train_inchis = []
canopus_test_inchis = []
canopus_val_inchis = []

for i in tqdm(range(len(canopus_labels)), desc="Converting CANOPUS SMILES to InChI", leave=False):
    
    mol = Chem.MolFromSmiles(canopus_labels.loc[i, "smiles"])
    smi = Chem.MolToSmiles(mol, isomericSmiles=False) # remove stereochemistry information
    mol = Chem.MolFromSmiles(smi)
    inchi = Chem.MolToInchi(mol)

    if canopus_labels.loc[i, "split"] == "train":
        if filter(mol):
            canopus_train_inchis.append(inchi)
    elif canopus_labels.loc[i, "split"] == "test":
        canopus_test_inchis.append(inchi)
    elif canopus_labels.loc[i, "split"] == "val":
        canopus_val_inchis.append(inchi)

canopus_train_df = pd.DataFrame(set(canopus_train_inchis), columns=["inchi"])
canopus_train_df.to_csv("../data/fp2mol/canopus/preprocessed/canopus_train.csv", index=False)

canopus_test_df = pd.DataFrame(canopus_test_inchis, columns=["inchi"])
canopus_test_df.to_csv("../data/fp2mol/canopus/preprocessed/canopus_test.csv", index=False)

canopus_val_df = pd.DataFrame(canopus_val_inchis, columns=["inchi"])
canopus_val_df.to_csv("../data/fp2mol/canopus/preprocessed/canopus_val.csv", index=False)

excluded_inchis = set(canopus_test_inchis + canopus_val_inchis)

########## MSG DATASET ##########

msg_split = pd.read_csv('../data/msg/split.tsv', sep='\t')

msg_labels = pd.read_csv('../data/msg/labels.tsv', sep='\t')
msg_labels["name"] = msg_labels["spec"]
msg_labels = msg_labels[["name", "smiles"]].reset_index(drop=True)

msg_labels = msg_labels.merge(msg_split, on="name")

msg_train_inchis = []
msg_test_inchis = []
msg_val_inchis = []

for i in tqdm(range(len(msg_labels)), desc="Converting MSG SMILES to InChI", leave=False):
    
    mol = Chem.MolFromSmiles(msg_labels.loc[i, "smiles"])
    smi = Chem.MolToSmiles(mol, isomericSmiles=False) # remove stereochemistry information
    mol = Chem.MolFromSmiles(smi)
    inchi = Chem.MolToInchi(mol)

    if msg_labels.loc[i, "split"] == "train":
        if filter(mol):
            msg_train_inchis.append(inchi)
    elif msg_labels.loc[i, "split"] == "test":
        msg_test_inchis.append(inchi)
    elif msg_labels.loc[i, "split"] == "val":
        msg_val_inchis.append(inchi)

msg_train_df = pd.DataFrame(set(msg_train_inchis), columns=["inchi"])
msg_train_df.to_csv("../data/fp2mol/msg/preprocessed/msg_train.csv", index=False)

msg_test_df = pd.DataFrame(msg_test_inchis, columns=["inchi"])
msg_test_df.to_csv("../data/fp2mol/msg/preprocessed/msg_test.csv", index=False)

msg_val_df = pd.DataFrame(msg_val_inchis, columns=["inchi"])
msg_val_df.to_csv("../data/fp2mol/msg/preprocessed/msg_val.csv", index=False)

excluded_inchis.update(msg_test_inchis + msg_val_inchis)

########## HMDB DATASET ##########

hmdb_set = set()
raw_smiles = read_from_sdf('../data/fp2mol/raw/structures.sdf')
for smi in tqdm(raw_smiles, desc='Cleaning HMDB structures', leave=False):
    try:
        mol = Chem.MolFromSmiles(smi)
        smi = Chem.MolToSmiles(mol, isomericSmiles=False) # remove stereochemistry information
        mol = Chem.MolFromSmiles(smi)
        if filter_with_atom_types(mol):
            hmdb_set.add(Chem.MolToInchi(mol))
    except:
        pass

hmdb_inchis = list(hmdb_set)
random.shuffle(hmdb_inchis)

hmdb_train_inchis = hmdb_inchis[:int(0.95 * len(hmdb_inchis))]
hmdb_val_inchis = hmdb_inchis[int(0.95 * len(hmdb_inchis)):]

hmdb_train_inchis = [inchi for inchi in hmdb_train_inchis if inchi not in excluded_inchis]

hmdb_train_df = pd.DataFrame(hmdb_train_inchis, columns=["inchi"])
hmdb_train_df.to_csv("../data/fp2mol/hmdb/preprocessed/hmdb_train.csv", index=False)

hmdb_val_df = pd.DataFrame(hmdb_val_inchis, columns=["inchi"])
hmdb_val_df.to_csv("../data/fp2mol/hmdb/preprocessed/hmdb_val.csv", index=False)

########## DSSTox DATASET ##########

dss_set_raw = set()
for i in tqdm(range(1, 14), desc='Loading DSSTox structures', leave=False):
    df = pd.read_excel(f'../data/fp2mol/raw/DSSToxDump{i}.xlsx')
    dss_set_raw.update(df[df['SMILES'].notnull()]['SMILES'])

dss_set = set()
for smi in tqdm(dss_set_raw, desc='Cleaning DSSTox structures', leave=False):
    try:
        mol = Chem.MolFromSmiles(smi)
        smi = Chem.MolToSmiles(mol, isomericSmiles=False) # remove stereochemistry information
        mol = Chem.MolFromSmiles(smi)
        if filter_with_atom_types(mol):
            dss_set.add(Chem.MolToInchi(mol))
    except:
        pass

dss_inchis = list(dss_set)
random.shuffle(dss_inchis)

dss_train_inchis = dss_inchis[:int(0.95 * len(dss_inchis))]
dss_val_inchis = dss_inchis[int(0.95 * len(dss_inchis)):]

dss_train_inchis = [inchi for inchi in dss_train_inchis if inchi not in excluded_inchis]

dss_train_df = pd.DataFrame(dss_train_inchis, columns=["inchi"])
dss_train_df.to_csv("../data/fp2mol/dss/preprocessed/dss_train.csv", index=False)

dss_val_df = pd.DataFrame(dss_val_inchis, columns=["inchi"])
dss_val_df.to_csv("../data/fp2mol/dss/preprocessed/dss_val.csv", index=False)

########## COCONUT DATASET ##########

coconut_df = pd.read_csv('../data/fp2mol/raw/coconut_csv-03-2025.csv')

coconut_set_raw = set(coconut_df["canonical_smiles"])

coconut_set = set()
for smi in tqdm(coconut_set_raw, desc='Cleaning COCONUT structures', leave=False):
    try:
        mol = Chem.MolFromSmiles(smi)
        smi = Chem.MolToSmiles(mol, isomericSmiles=False) # remove stereochemistry information
        mol = Chem.MolFromSmiles(smi)
        if filter_with_atom_types(mol):
            coconut_set.add(Chem.MolToInchi(mol))
    except:
        pass

coconut_inchis = list(coconut_set)
random.shuffle(coconut_inchis)

coconut_train_inchis = coconut_inchis[:int(0.95 * len(coconut_inchis))]
coconut_val_inchis = coconut_inchis[int(0.95 * len(coconut_inchis)):]

coconut_train_inchis = [inchi for inchi in coconut_train_inchis if inchi not in excluded_inchis]

coconut_train_df = pd.DataFrame(coconut_train_inchis, columns=["inchi"])
coconut_train_df.to_csv("../data/fp2mol/coconut/preprocessed/coconut_train.csv", index=False)

coconut_val_df = pd.DataFrame(coconut_val_inchis, columns=["inchi"])
coconut_val_df.to_csv("../data/fp2mol/coconut/preprocessed/coconut_val.csv", index=False)


########## MOSES DATASET ##########

moses_df = pd.read_csv('../data/fp2mol/raw/moses.csv')

moses_set_raw = set(moses_df["SMILES"])

moses_set = set()
for smi in tqdm(moses_set_raw, desc='Cleaning MOSES structures', leave=False):
    try:
        mol = Chem.MolFromSmiles(smi)
        smi = Chem.MolToSmiles(mol, isomericSmiles=False) # remove stereochemistry information
        mol = Chem.MolFromSmiles(smi)
        if filter_with_atom_types(mol):
            moses_set.add(Chem.MolToInchi(mol))
    except:
        pass

moses_inchis = list(moses_set)
random.shuffle(moses_inchis)

moses_train_inchis = moses_inchis[:int(0.95 * len(moses_inchis))]
moses_val_inchis = moses_inchis[int(0.95 * len(moses_inchis)):]

moses_train_inchis = [inchi for inchi in moses_train_inchis if inchi not in excluded_inchis]

moses_train_df = pd.DataFrame(moses_train_inchis, columns=["inchi"])
moses_train_df.to_csv("../data/fp2mol/moses/preprocessed/moses_train.csv", index=False)

moses_val_df = pd.DataFrame(moses_val_inchis, columns=["inchi"])
moses_val_df.to_csv("../data/fp2mol/moses/preprocessed/moses_val.csv", index=False)

########## COMBINED DATASET ##########

combined_inchis = hmdb_inchis + dss_inchis + coconut_inchis + moses_inchis
combined_inchis = list(set(combined_inchis))
random.shuffle(combined_inchis)

combined_train_inchis = combined_inchis[:int(0.95 * len(combined_inchis))]
combined_val_inchis = combined_inchis[int(0.95 * len(combined_inchis)):]
combined_train_inchis = [inchi for inchi in combined_train_inchis if inchi not in excluded_inchis]

combined_train_df = pd.DataFrame(combined_train_inchis, columns=["inchi"])
combined_train_df.to_csv("../data/fp2mol/combined/preprocessed/combined_train.csv", index=False)

combined_val_df = pd.DataFrame(combined_val_inchis, columns=["inchi"])
combined_val_df.to_csv("../data/fp2mol/combined/preprocessed/combined_val.csv", index=False)