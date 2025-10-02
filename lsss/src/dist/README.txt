LSSS - Readme

________________________________________________________________

Contact information

   Home page:    https://marec.no/
   Email:        info@marec.no

________________________________________________________________

Installing LSSS on Windows

1. Run the installation program.

2. See the optional post-install steps below.

________________________________________________________________

Installing LSSS on Linux

1. Unzip the LSSS installation zip file for linux.
   The resulting directory is referred to as LSSS_INSTALL_SRC.

2. Install LSSS
   a. Unzip the LSSS zip-file in LSSS_INSTALL_SRC.
      The resulting directory is referred to as LSSS_HOME.

3. Start LSSS
   a. Start LSSS by running LSSS_HOME/lsss/LSSS.sh.

4. Optional
   a. Create a shortcut to the startup script on the desktop
      using the icon LSSS_HOME/lsss/LSSS.png.
   b. See the optional post-install steps below.

________________________________________________________________

Optional post-install steps

1. Create an account on a database server supported by LSSS,
   such as PostgreSQL. The JavaDB and HSQLDB databases can be used
   without an external database server, but note that HSQLDB may
   not be suited for large databases.

2. Change the size of the Java heap. Edit the startup script
     LSSS_HOME\lsss\LSSS.bat or LSSS_HOME/lsss/LSSS.sh
   or set the environment variable LSSS_MAX_MEMORY_MB.

3. Set up a local WMS map server.
