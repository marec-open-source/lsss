# LSSS API

LSSS has different types of APIs.

The [LSSS Scripting HTTP API](#lsss-scripting-http-api)
is intended for scripting and remote controlling the LSSS user interface.

The [LSSS Plugin Java API](#lsss-plugin-java-api)
is intended for developing plugins that extend the functionality of LSSS with,
for example, new windows and new layers on the echogram and map.

## LSSS Scripting HTTP API

The scripting HTTP API is intended for remote controlling the LSSS user interface.
It can be used from any language or framework capable of making HTTP requests.
LSSS has an embedded HTTP server that responds to the HTTP requests.
For more information about the scripting HTTP API,
see https://marec.no/lsss/help/latest/#/page/lsss/LsssServerConf.

LSSS can be extended via [packages](https://marec.no/lsss/help/latest/#/page/lsss/Packages)
with custom functionality such as callbacks, keystrokes, toolbar buttons, and menus.
These custom user interface elements are associated with 
[actions](https://marec.no/lsss/help/latest/#/page/lsss/Packages/Actions),
which are Python scripts using the scripting HTTP API.

## LSSS Plugin Java API

The plugin Java API is intended for developing plugins that extend the functionality
of LSSS with, for example, new windows and new layers on the echogram and map.

Even if both LSSS and the plugin API are written in Java, it is possible to use the API
from other JVM languages, such as Kotlin.

For details about the API, see the [Javadoc](https://marec.no/lsss/plugin-api/latest/javadoc/).

Several example projects demonstrate how to use the API:
* [Hello world](https://github.com/marec-open-source/lsss-plugin-hello-world) — 
  A minimal LSSS plugin containing an echogram overlay that displays the text "Hello echogram!"
* [Hello Kotlin](https://github.com/marec-open-source/lsss-plugin-hello-kotlin) —
  A minimal LSSS plugin similar to the Hello world plugin, but implemented in Kotlin.
* [MAREC example](https://github.com/marec-open-source/lsss-plugin-marec-example) —
  An LSSS plugin containing various examples of what the API can be used for, such as:
  - A map overlay displaying [GeoJSON](https://geojson.org/) files.
  - An echogram overlay displaying "EchogramJSON" files. "EchogramJSON" is similar to
    GeoJson, but using coordinates time and depth instead of longitude and latitude.
  - An echogram plot function for the sun elevation, computed from geographical position and time.
  - Echogram plot functions for time series loaded from files. 
