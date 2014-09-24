<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:java="http://xml.apache.org/xalan/java"
exclude-result-prefixes="java"
version="1.0">
<xsl:output method="text" omit-xml-declaration="yes"  encoding="UTF-8" indent="no" />
<xsl:template match="/">
<xsl:for-each select="//doc" >
    <xsl:value-of select="./arr[@name='export']/str)" />&#13;
    <!--
    <xsl:value-of select="concat(
normalize-space(./arr[@name='id']/str), '&#009;',
normalize-space(./str[@name='code']), '&#009;',
normalize-space(./arr[@name='ccnb']/str), '&#009;',
normalize-space(./arr[@name='titlemd5']/str), '&#009;',
normalize-space(./arr[@name='authormd5']/str), '&#009;',
normalize-space(./arr[@name='mistovydani']/str), '&#009;',
normalize-space(./arr[@name='vydavatel']/str), '&#009;',
normalize-space(./arr[@name='datumvydani']/str), '&#009;')" />&#13;
-->
<xsl:text>
</xsl:text>
          </xsl:for-each>
    </xsl:template>
</xsl:stylesheet>
