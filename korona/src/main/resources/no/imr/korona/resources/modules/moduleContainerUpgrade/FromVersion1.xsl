<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

   <!-- Identity transform -->
   <xsl:template match="@*|node()">
      <xsl:copy>
         <xsl:apply-templates select="@*|node()"/>
      </xsl:copy>
   </xsl:template>

   <!-- Version -->
   <xsl:template match="/ModuleContainer/@version">
      <xsl:attribute name="version">2</xsl:attribute>
   </xsl:template>

   <!-- TrackingModule -->
   <xsl:template match="module[@name='TrackingModule']/parameters/parameter[@name='TrackerType']/text()[.='Broadband data' or .='Narrowband data']">
      <xsl:text>Peak</xsl:text>
   </xsl:template>

   <xsl:template match="module[@name='TrackingModule']/parameters/parameter[@name='MaxDirectivityCorrection']">
      <parameter name="MaxGainCompensation">
         <xsl:value-of select="text() div 2"/>
      </parameter>
   </xsl:template>

</xsl:stylesheet>
