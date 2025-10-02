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
         <xsl:attribute name="version">4</xsl:attribute>
         <xsl:apply-templates/>
      </xsl:copy>
   </xsl:template>

   <!-- DeepVisionDataConf -->
   <xsl:template match="/unit/unit[@name='DataConf']/unit[@name='DeepVisionDataConf']/configuration/parameters">
      <parameters>
         <xsl:apply-templates select="@*|node()"/>
         <xsl:copy-of select="//unit[@name='PelagicEchogramModule']/unit[@name='DeepVisionPathEchogramOverlay']/configuration/parameters/parameter[@name='DistanceMapping']"/>
         <xsl:copy-of select="//unit[@name='PelagicEchogramModule']/unit[@name='DeepVisionPathEchogramOverlay']/configuration/parameters/parameter[@name='DistanceBehindShip']"/>
      </parameters>
   </xsl:template>

   <!-- DeepVisionPathEchogramOverlay -->
   <xsl:template match="//unit[@name='DeepVisionPathEchogramOverlay']/configuration/parameters/parameter[@name='DistanceMapping']"/>
   <xsl:template match="//unit[@name='DeepVisionPathEchogramOverlay']/configuration/parameters/parameter[@name='DistanceBehindShip']"/>

</xsl:stylesheet>
