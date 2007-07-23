#!/bin/sh

SCRIPTDIR=`dirname "$0"`
cd "$SCRIPTDIR"

java -Djava.util.logging.config.file=conf/logging.properties -Xms512m -Xmx2048m -cp MZmine.jar net.sf.mzmine.main.MZmineClient
