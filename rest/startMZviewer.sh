#!/bin/sh

SCRIPTDIR=`dirname "$0"`
cd "$SCRIPTDIR"

java -Djava.util.logging.config.file=conf/logging.properties -Xms1024m -Xmx1024m -cp MZmine2.jar net.sf.mzmine.main.mzmineviewer.MZviewer
