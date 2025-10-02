<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

   <!-- Identity transform -->
   <xsl:template match="@*|node()">
      <xsl:copy>
         <xsl:apply-templates select="@*|node()"/>
      </xsl:copy>
   </xsl:template>

   <!-- Version -->
   <xsl:template match="/ModuleContainer">
      <xsl:copy>
         <xsl:copy-of select="@*"/>
         <xsl:attribute name="version">1</xsl:attribute>
         <xsl:apply-templates/>
      </xsl:copy>
   </xsl:template>

   <!-- DepthModule -->
   <xsl:template match="module[@name='DepthModule']/parameters/parameter/@name[.='Minimum depth threshold factor']">
      <xsl:attribute name="name">MinimumDepthThresholdFactor</xsl:attribute>
   </xsl:template>

</xsl:stylesheet>
