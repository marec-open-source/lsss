# LSSS - Large Scale Survey System

LSSS is a software system for analyzing large amounts of acoustic data from echosounders and sonars.

For information about MAREC and LSSS, see https://marec.no.

For information about the LSSS APIs, see [doc/LsssApi.md](doc/LsssApi.md)

## Building and running LSSS

Before building and running LSSS it is necessary to install JDK 21.
It is recommended to use [Eclipse Temurin from Adoptium](https://adoptium.net/).

To run LSSS:

```shell
./gradlew runLsss
```

To build a zip-file with LSSS:

```shell
./gradlew distZip
```

The resulting zip-file is located in `lsss/release/build/distributions`.

## Pre-built binaries

Pre-built binaries can be downloaded from https://marec.no/downloads.htm.

## About this repository

This open-source repository is a subset of a separate closed-source repository at 
[NORCE Research AS](https://www.norceresearch.no/).
The active development of LSSS happens in the NORCE-repository.
This repository is updated with snapshots from the NORCE-repository when needed.
