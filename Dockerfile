FROM amd64/ubuntu:latest
LABEL maintainer="Robin Schmid <rschmid1789@gmail.com>"

#RUN echo 'debconf debconf/frontend select Noninteractive' | debconf-set-selections
RUN apt-get -y update && \
    apt-get install -y apt-utils    --no-install-recommends && \
    apt-get install -y libfreetype6 --no-install-recommends && \
    apt-get install -y fontconfig   --no-install-recommends && \
    apt-get install -y fonts-dejavu --no-install-recommends

# optional remove app index for smaller container
# RUN rm -rf /var/lib/apt/lists/*

# install xdg dependency
# somehow on WSL xdg needed this directory - otherwise error
#RUN mkdir /usr/share/desktop-directories/
#RUN apt-get install -y xdg-utils
#RUN apt-get install -y libgl1

# needed to download mzmine
#RUN apt-get install -y wget         --no-install-recommends



# download mzmine-ubuntu artifact for latest build
# wget --no-check-certificate https://nightly.link/robinschmid/mzmine3/actions/artifacts/44356954.zip
# download latest release
#RUN wget https://github.com/mzmine/mzmine3/releases/download/v3.4.27/mzmine-linux-installer_3.4.27.deb

# Install .deb file
#RUN dpkg -i mzmine-linux-installer_3.4.27.deb
#COPY mzmine-linux-installer_3.4.27.deb ./
COPY /build/jpackage/MZmine /opt/MZmine

# Run command
ENTRYPOINT ["/opt/MZmine/bin/MZmine"]
#CMD ["/opt/MZmine/bin/MZmine"]