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
         <xsl:attribute name="version">1</xsl:attribute>
         <xsl:apply-templates/>
      </xsl:copy>
   </xsl:template>

   <!-- AppPreprocessingConf -->
   <xsl:template match="/unit/unit/@name[.='PreprocessingConf']">
      <xsl:attribute name="name">AppPreprocessingConf</xsl:attribute>
   </xsl:template>

</xsl:stylesheet>
