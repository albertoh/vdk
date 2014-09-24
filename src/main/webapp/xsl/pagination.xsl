<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:java="http://xml.apache.org/xalan/java"
  xmlns:rb="cz.incad.xsl.ResourceBundleService"
  version="1.0">
    <xsl:output method="html"/>
    <xsl:param name="locale" select="locale" />
    <xsl:variable name="i18n" select="rb:getBundle('labels', $locale)"/>
    
    
    <xsl:template name="pagination">
        <xsl:param name="rows"/>
        <xsl:param name="start"/>
        <xsl:param name="numDocs"/>
        <xsl:if test="$numDocs &gt; 0">
            <xsl:variable name="pageStart"><xsl:choose>
                <xsl:when test="$start - $rows*3 &lt; 1">1</xsl:when>
                <xsl:otherwise><xsl:value-of select="$start - $rows*3" /></xsl:otherwise>
            </xsl:choose></xsl:variable>
            <div style="float:right;" class="pagination">
                <xsl:if test="$start &gt; $rows">
                    <a class="previous"><xsl:attribute name="href">javascript:gotoOffset(<xsl:value-of select="$start - $rows - 1" />)</xsl:attribute> &lt;&lt;</a>
                </xsl:if>
                <xsl:call-template name="page">
                    <xsl:with-param name="pageNum"><xsl:value-of select="1" /></xsl:with-param>
                    <xsl:with-param name="rows"><xsl:value-of select="$rows" /></xsl:with-param>
                    <xsl:with-param name="start"><xsl:value-of select="$pageStart" /></xsl:with-param>
                    <xsl:with-param name="numDocs"><xsl:value-of select="$numDocs" /></xsl:with-param>
                    <xsl:with-param name="firsthit"><xsl:value-of select="$start" /></xsl:with-param>
                </xsl:call-template>
                <xsl:if test="$numDocs &gt; $rows + $start">
                    <a class="next"><xsl:attribute name="href">javascript:gotoOffset(<xsl:value-of select="$rows + $start - 1" />)</xsl:attribute> &gt;&gt;</a>
                </xsl:if>
            </div>
        </xsl:if>
    </xsl:template>
    
    <xsl:template name="page">
        <xsl:param name="pageNum"/>
        <xsl:param name="rows"/>
        <xsl:param name="start"/>
        <xsl:param name="numDocs"/>
        <xsl:param name="firsthit"/>
        &#160;<a>
            <xsl:attribute name="href">javascript:gotoOffset(<xsl:value-of select="$start - 1" />);</xsl:attribute>
            <xsl:if test="$start = $firsthit+1">
                <xsl:attribute name="class">sel</xsl:attribute>
            </xsl:if><span><xsl:value-of select="$start" />-<xsl:choose>
            <xsl:when test="$numDocs &gt; $rows + $start"><xsl:value-of select="$rows + $start - 1" /></xsl:when>
            <xsl:otherwise><xsl:value-of select="$numDocs" /></xsl:otherwise></xsl:choose></span></a>&#160;|
        <xsl:if test="($pageNum &lt; 7) and ($numDocs &gt; $start + $rows)">
            <xsl:call-template name="page">
                <xsl:with-param name="pageNum"><xsl:value-of select="$pageNum + 1" /></xsl:with-param>
                <xsl:with-param name="rows"><xsl:value-of select="$rows" /></xsl:with-param>
                <xsl:with-param name="start"><xsl:value-of select="$start + $rows" /></xsl:with-param>
                <xsl:with-param name="numDocs"><xsl:value-of select="$numDocs" /></xsl:with-param>
                    <xsl:with-param name="firsthit"><xsl:value-of select="$firsthit" /></xsl:with-param>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>
</xsl:stylesheet>
