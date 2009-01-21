@echo off
java -Djava.util.logging.config.file=conf/logging.properties -Xms1024m -Xmx1384m -cp MZmine2.jar net.sf.mzmine.main.mzmineclient.MZmineClient
