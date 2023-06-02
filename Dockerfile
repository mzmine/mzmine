FROM debian:stable-slim
#FROM ubuntu:latest
LABEL maintainer="Robin Schmid <rschmid1789@gmail.com>"
COPY /build/jpackage/MZmine /opt/MZmine
# Run command
RUN chmod +x /opt/MZmine/bin/MZmine
ENTRYPOINT ["/opt/MZmine/bin/MZmine"]

# needed to download mzmine
#RUN apt-get install -y wget         --no-install-recommends

# optional remove app index for smaller container
# RUN rm -rf /var/lib/apt/lists/*


# download mzmine-ubuntu artifact for latest build
# wget --no-check-certificate https://nightly.link/robinschmid/mzmine3/actions/artifacts/44356954.zip
# download latest release
#RUN wget https://github.com/mzmine/mzmine3/releases/download/v3.4.27/mzmine-linux-installer_3.4.27.deb

# Install .deb file
#RUN dpkg -i mzmine-linux-installer_3.4.27.deb
#COPY mzmine-linux-installer_3.4.27.deb ./
#RUN apt install -y mzmine-linux-installer_3.4.27.deb
#RUN rm tmp/

#CMD ["/opt/MZmine/bin/MZmine"]