<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:java="http://xml.apache.org/xalan/java"
  xmlns:ue="cz.incad.xsl.XmlUnescapeService"
  exclude-result-prefixes="java ue"
  version="1.0">
    <xsl:output method="xml"/>
    <xsl:template match="@*|node()">
      <xsl:copy>
        <xsl:apply-templates select="@*|node()"/>
      </xsl:copy>
    </xsl:template>
    <xsl:template match="@USEDHITS"/>
    <xsl:template match="@UNIT"/>
    <xsl:template match="@MAXRANK"/>
    <xsl:template match="@TIME"/>
    <xsl:template match="@SAMPLECOUNT"/>
    <xsl:template match="@HITCOUNT"/>
    <xsl:template match="@RATIO"/>
    <xsl:template match="@MIN"/>
    <xsl:template match="@SCORE"/>
    <xsl:template match="@MAX"/>
    <xsl:template match="@MEAN"/>
    <xsl:template match="@ENTROPY"/>
    <xsl:template match="@SUM"/>
    <xsl:template match="@MOREHITS"/>
    <xsl:template match="QUERYTRANSFORMS"/>
    <xsl:template match="NAVIGATIONENTRY[@NAME='companiesnavigator']" />
    <xsl:template match="PAGENAVIGATION"/>
    <xsl:template match="FIELD[@NAME='title']">
        <xsl:variable name="x"><xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy></xsl:variable>
        <FIELD NAME="title"><xsl:value-of select="ue:unescape($x)" /></FIELD>
    </xsl:template>
    <xsl:template match="FIELD[@NAME='xml']">
        <xsl:variable name="x"><xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy></xsl:variable>
        <FIELD NAME="xml"><xsl:value-of select="ue:unescape($x)" disable-output-escaping="yes"/></FIELD>
    </xsl:template>
    <xsl:template match="FIELD[@NAME='teaser']">
        <FIELD NAME="teaser"><xsl:value-of select="ue:unescape(.)" disable-output-escaping="yes"/></FIELD>
    </xsl:template>
    <xsl:template match="key">
        &lt;i&gt;<xsl:value-of select="." disable-output-escaping="yes"/>&lt;/i&gt;
    </xsl:template>
</xsl:stylesheet>
