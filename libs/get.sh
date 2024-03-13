#!/bin/sh
# SPDX-FileCopyrightText: 2024 Swarovski-Optik AG & Co KG.
# SPDX-License-Identifier: Apache-2.0

set -e

version="0.13.0"

libs="
com/swarovskioptik/comm/SOBase/$version/SOBase-$version.jar
com/swarovskioptik/comm/SharedDefinitions/$version/SharedDefinitions-$version.jar
com/swarovskioptik/comm/FileTransferManager/$version/FileTransferManager-$version.aar
com/swarovskioptik/comm/ResubscribingReplayingShare/$version/ResubscribingReplayingShare-$version.jar
com/swarovskioptik/comm/SOCommMediaClient/$version/SOCommMediaClient-$version.aar
com/swarovskioptik/comm/SOCommOutsideAPI/$version/SOCommOutsideAPI-$version.aar
com/swarovskioptik/comm/SOLogger/$version/SOLogger-$version.jar
com/swarovskioptik/comm/WifiMqttApi/$version/WifiMqttApi-$version.aar
com/swarovskioptik/mqtt/client/mqtt-client-wrapper-rx/0.1.0/mqtt-client-wrapper-rx-0.1.0.jar
"

# TODO check username and password for non empty

BASE_URL=https://maven.tailored-apps.com/repository/maven-swarovski-optik/

for lib in $libs; do
    wget $BASE_URL/$lib --user $USERNAME --password $PASSWORD
done
