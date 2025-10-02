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
      <xsl:attribute name="version">4</xsl:attribute>
   </xsl:template>

   <!-- RegionModule -> SchoolDetectionModule -->
   <xsl:template match="module/@name[.='RegionModule']">
      <xsl:attribute name="name">SchoolDetectionModule</xsl:attribute>
   </xsl:template>
   <xsl:template match="module[@name='RegionModule']/parameters/parameter/@name[.='BlindZoneDepth']">
      <xsl:attribute name="name">MinDepth</xsl:attribute>
   </xsl:template>
   <xsl:template match="module[@name='RegionModule']/parameters/parameter[@name='Thresholds']">
      <parameter name="Threshold">
         <xsl:if test="contains(text(), ',')">
            <xsl:value-of select="substring-before(text(), ',')"/>
         </xsl:if>
         <xsl:if test="not(contains(text(), ','))">
            <xsl:value-of select="text()"/>
         </xsl:if>
      </parameter>
   </xsl:template>
   <xsl:template match="module[@name='RegionModule']/parameters/parameter[@name='Overlap']">
   </xsl:template>

</xsl:stylesheet>
