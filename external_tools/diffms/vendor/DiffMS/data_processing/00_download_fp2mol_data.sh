# build datadir file structure
mkdir data/
mkdir data/fp2mol/
mkdir data/fp2mol/raw/

cd data/fp2mol/raw/

# download raw data
wget https://hmdb.ca/system/downloads/current/structures.zip
unzip structures.zip

wget https://clowder.edap-cluster.com/api/files/6616d8d7e4b063812d70fc95/blob
unzip blob

wget https://coconut.s3.uni-jena.de/prod/downloads/2025-03/coconut_csv-03-2025.zip
unzip coconut_csv-03-2025.zip

wget https://media.githubusercontent.com/media/molecularsets/moses/master/data/dataset_v1.csv
mv dataset_v1.csv moses.csv