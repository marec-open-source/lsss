<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

   <!-- Identity transform -->
   <xsl:template match="@*|node()">
      <xsl:copy>
         <xsl:apply-templates select="@*|node()"/>
      </xsl:copy>
   </xsl:template>

   <!-- Upgrade version to version 2 -->
   <xsl:template match="/survey">
      <xsl:copy>
         <xsl:copy-of select="@*"/>
         <xsl:attribute name="version">2</xsl:attribute>
         <xsl:apply-templates/>
      </xsl:copy>
   </xsl:template>

   <!-- Display -->
   <xsl:template match="/survey/windowconfiguration">
      <xsl:apply-templates/>
   </xsl:template>
   <xsl:template match="/survey/windowconfiguration/maindisplay">
      <display>
         <xsl:apply-templates/>
      </display>
   </xsl:template>
   <xsl:template match="/survey/windowconfiguration/maindisplay/*">
      <moduleManager>
         <xsl:attribute name="name"><xsl:value-of select="name()"/></xsl:attribute>
         <xsl:apply-templates select="@*|node()"/>
      </moduleManager>
   </xsl:template>
   <xsl:template match="/survey/windowconfiguration/maindisplay/*/*">
      <module>
         <xsl:attribute name="name"><xsl:value-of select="name()"/></xsl:attribute>
         <xsl:attribute name="visible"><xsl:value-of select="visible"/></xsl:attribute>
         <xsl:attribute name="floating"><xsl:value-of select="floating"/></xsl:attribute>
         <xsl:attribute name="relativeSize"><xsl:value-of select="relativesize"/></xsl:attribute>
         <xsl:if test="floating/text() = 'true'">
            <xsl:text>&#xa;</xsl:text>
            <window>
               <xsl:attribute name="x"><xsl:value-of select="floating/@xpos"/></xsl:attribute>
               <xsl:attribute name="y"><xsl:value-of select="floating/@ypos"/></xsl:attribute>
               <xsl:attribute name="width"><xsl:value-of select="floating/@xsize"/></xsl:attribute>
               <xsl:attribute name="height"><xsl:value-of select="floating/@ysize"/></xsl:attribute>
            </window>
            <xsl:text>&#xa;</xsl:text>
         </xsl:if>
      </module>
   </xsl:template>

</xsl:stylesheet>
