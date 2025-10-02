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
         <xsl:attribute name="version">3</xsl:attribute>
         <xsl:apply-templates/>
      </xsl:copy>
   </xsl:template>

   <!-- AcousticCategoryConf -->
   <xsl:template match="/unit/unit[@name='AcousticCategoryConf']/configuration">
      <configuration>
         <xsl:apply-templates select="@*|node()"/>
         <speciesMappings>
            <xsl:apply-templates select="/unit/unit[@name='AcousticCategoryConf']/configuration/species/speciesMappings/node()"/>
         </speciesMappings>
      </configuration>
   </xsl:template>
   <xsl:template match="/unit/unit[@name='AcousticCategoryConf']/configuration/species">
      <species>
         <xsl:apply-templates select="@*|node()"/>
      </species>
   </xsl:template>
   <xsl:template match="/unit/unit[@name='AcousticCategoryConf']/configuration/species/specy">
      <species>
         <!-- Leave species name as mixed content text. It is informative only, and not used during parsing. -->
         <xsl:apply-templates select="@*|node()"/>
      </species>
   </xsl:template>
   <xsl:template match="/unit/unit[@name='AcousticCategoryConf']/configuration/species/speciesMappings">
      <!-- Moved one level out -->
   </xsl:template>
   <xsl:template match="/unit/unit[@name='AcousticCategoryConf']/configuration/species/speciesMappings/specie">
      <species>
         <!-- Leave species name as mixed content text. It is informative only, and not used during parsing. -->
         <xsl:apply-templates select="@*|node()"/>
      </species>
   </xsl:template>

</xsl:stylesheet>
