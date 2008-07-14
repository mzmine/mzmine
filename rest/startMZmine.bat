@echo off
cls
java -Dj3d.rend=d3d -Djava.library.path=lib -Djava.util.logging.config.file=conf/logging.properties -Xms512m -Xmx1384m -cp MZmine2.jar net.sf.mzmine.main.MZmineClient
