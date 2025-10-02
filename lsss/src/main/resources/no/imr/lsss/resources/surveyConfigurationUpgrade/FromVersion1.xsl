<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

   <!-- Identity transform -->
   <xsl:template match="@*|node()">
      <xsl:copy>
         <xsl:apply-templates select="@*|node()"/>
      </xsl:copy>
   </xsl:template>

   <!-- Version -->
   <xsl:template match="/unit">
      <xsl:copy>
         <xsl:copy-of select="@*"/>
         <xsl:attribute name="version">2</xsl:attribute>
         <xsl:apply-templates/>
      </xsl:copy>
   </xsl:template>

   <!-- AcousticCategoryConf -->
   <xsl:template match="/unit/unit[@name='AcousticCategoryConf']/configuration">
      <configuration>
         <parameters>
            <parameter name="StoreRawDataSpecies"><xsl:copy-of select="/unit/unit[@name='ModuleConf']/unit[@name='InterpretationModule']/configuration/parameters/parameter[@name='StoreRawDataSpecies']/text()"/></parameter>
         </parameters>
         <xsl:apply-templates/>
      </configuration>
   </xsl:template>
   <xsl:template match="/unit/unit[@name='ModuleConf']/unit[@name='InterpretationModule']/configuration/parameters/parameter[@name='StoreRawDataSpecies']">
      <!-- Remove -->
   </xsl:template>

</xsl:stylesheet>
