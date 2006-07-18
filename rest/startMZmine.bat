@echo off
cls
java -Dj3d.rend=d3d -Djava.util.logging.config.file=conf/logging.properties -Xms512m -Xmx2048m -jar MZmine.jar
