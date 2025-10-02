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
      <xsl:attribute name="version">3</xsl:attribute>
   </xsl:template>

   <!-- CombinationRemoverModule -->
   <xsl:template match="module[@name='CombinationRemoverModule']">
      <module name="ChannelRemovalModule">
         <parameters>
            <xsl:copy-of select="parameters/parameter[@name='Active']"/>
            <parameter name="ChannelsFromEnd">1</parameter>
         </parameters>
      </module>
   </xsl:template>

   <!-- RemoveSingleChannelModule -->
   <xsl:template match="module[@name='RemoveSingleChannelModule']">
      <module name="ChannelRemovalModule">
         <parameters>
            <xsl:copy-of select="parameters/parameter[@name='Active']"/>
            <parameter name="Channels">
               <xsl:value-of select="parameters/parameter[@name='ChannelNumber']/text()"/>
            </parameter>
         </parameters>
      </module>
   </xsl:template>

</xsl:stylesheet>
