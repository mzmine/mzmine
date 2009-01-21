#!/bin/sh

SCRIPTDIR=`dirname "$0"`
cd "$SCRIPTDIR"

java -Djava.util.logging.config.file=conf/logging.properties -Xdock:name="MZmine 2" -Xdock:icon="icons/MZmineIcon.png" -Dapple.laf.useScreenMenuBar=true -Xms1024m -Xmx2048m -cp MZmine2.jar net.sf.mzmine.main.mzmineclient.MZmineClient
