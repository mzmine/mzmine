#!/bin/sh

SCRIPTDIR=`dirname "$0"`
cd "$SCRIPTDIR"

java -Djava.util.logging.config.file=conf/logging.properties -Xdock:name="MZmine 2" -Xms512m -Xmx2048m -cp MZmine.jar net.sf.mzmine.main.MZmineClient
