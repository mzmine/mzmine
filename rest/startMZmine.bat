@echo off
java -Dj3d.rend=d3d -Djava.util.logging.config.file=conf/logging.properties -Xms1024m -Xmx1384m -cp MZmine2.jar net.sf.mzmine.main.MZmineClient
