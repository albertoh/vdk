<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:java="http://xml.apache.org/xalan/java"
  xmlns:rb="cz.incad.xsl.ResourceBundleService"
  exclude-result-prefixes="java rb"
  version="1.0">
    <xsl:output method="html"/>
    <xsl:param name="locale" select="'locale'" />
    <xsl:param name="q" select="q" />
    <xsl:param name="collapseBy" select="collapseBy" />
    <xsl:variable name="i18n" select="rb:getBundle('labels', $locale)"/>
    <xsl:template match="/">
        <ul>
            <xsl:for-each select="//doc" >
                <xsl:call-template name="hit">
                    <xsl:with-param name="index" select="position()" />
                </xsl:call-template>
            </xsl:for-each>
        </ul>
    </xsl:template>
    
    <xsl:template name="hit">
        <xsl:param name="index" />
        <xsl:variable name="hasCollapsed" select="./@FCOCOUNT &gt; 1" />
        <xsl:variable name="contentid" select="str[@name='ccnb']" />
        <xsl:variable name="numDocs" select="count(arr[@name='id']/str)" />
        <li class="res">
            <xsl:attribute name="id">result_<xsl:value-of select="position()" /></xsl:attribute>
            
            <input type="hidden" class="id">
                <xsl:attribute name="value"><xsl:value-of select="$contentid" /></xsl:attribute>
            </input>
            <input type="hidden" class="ccnb">
                <xsl:attribute name="value">
                    <xsl:value-of select="./str[@name='ccnb']" />
                </xsl:attribute>
            </input>
            <xsl:variable name="zdroj" select="./str[@name='zdroj']" />
            <table width="100%"><tr>
                <!--
                <td valign="top" width="20px">
                    <xsl:call-template name="icons">
                        <xsl:with-param name="zdroj" select="$zdroj" />
                    </xsl:call-template>
                </td>
                -->
                <td valign="top">
                    <div>
                        <xsl:value-of select="./arr[@name='title']/str" />
                        
                        <xsl:if test="not($numDocs = 0)"> (<xsl:value-of select="$numDocs" />&#160;<xsl:choose>
                            <xsl:when test="$numDocs = 1">
                                <xsl:value-of select="rb:getString($i18n,'results.collapsed.singular')"/>
                            </xsl:when>
                            <xsl:when test="$numDocs &lt; 5">
                                <xsl:value-of select="rb:getString($i18n,'results.collapsed.plural_1')"/>
                                <a href="">+</a>
                            </xsl:when>
                            <xsl:when test="$numDocs &gt; 4">
                                <xsl:value-of select="rb:getString($i18n,'results.collapsed.plural_2')"/>
                                <a href="">+</a>
                            </xsl:when>
                            </xsl:choose>)</xsl:if>
                            <a>
                                <xsl:attribute name="href"><xsl:value-of select="./arr[@name='url']" /></xsl:attribute>
                                ...
                            </a>
                    </div>
                    <xsl:if test="./arr[@name='author']/str">
                    <div><span><xsl:value-of select="rb:getString($i18n,'field.authors')"/></span>: 
                        <xsl:value-of select="./arr[@name='author']/str" /></div>
                    </xsl:if>
                    
                    <div><span><xsl:value-of select="rb:getString($i18n,'field.ccnb')"/></span>: 
                        <xsl:value-of select="./str[@name='ccnb']" /></div>
                        
                    <xsl:choose>
                        <xsl:when test="./FIELD[@NAME='generic1'] &gt; 4"> 
                            <div><xsl:value-of select="./FIELD[@NAME='generic1']" /> exemplářů</div>
                        </xsl:when>
                        <xsl:when test="./FIELD[@NAME='generic1'] &gt; 1"> 
                            <div><xsl:value-of select="./FIELD[@NAME='generic1']" /> exempláře</div>
                        </xsl:when>
                        <xsl:when test="./FIELD[@NAME='generic1'] = 1"> 
                            <div><xsl:value-of select="./FIELD[@NAME='generic1']" /> exemplář</div>
                        </xsl:when>
                    </xsl:choose>
                    <div class="ex">
                        <xsl:attribute name="data-ex">{"exemplare":[
                            <xsl:for-each select="./arr[@name='ex']/str">
                                <xsl:value-of  select="." /><xsl:if test="position()!=last()">,</xsl:if>
                            </xsl:for-each>]}
                        </xsl:attribute>
                        <xsl:choose>
                        <xsl:when test="contains($zdroj, 'MZK')">
                            <xsl:attribute name="data-icon">img/icons/zdroj/mzk.gif</xsl:attribute>
                        </xsl:when>
                        <xsl:when test="contains($zdroj, 'VKOL')">
                            <xsl:attribute name="data-icon">img/icons/zdroj/vkol.gif</xsl:attribute>
                        </xsl:when>
                        <xsl:when test="contains($zdroj, 'NKC')">
                            <xsl:attribute name="data-icon">img/icons/zdroj/nkp.gif</xsl:attribute>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:attribute name="data-icon">img/empty.gif</xsl:attribute>
                        </xsl:otherwise>
                        </xsl:choose>
                        <xsl:attribute name="data-zdroj"><xsl:value-of select="$zdroj" /></xsl:attribute>
                    </div>    
                    <div style="float:right;"></div>
                </td>
                <td width="120px">
                    <!--
                    <xsl:call-template name="actions">
                        <xsl:with-param name="zdroj" select="./FIELD[@NAME='zdroj']" />
                        <xsl:with-param name="contentid" select="$contentid" />
                    </xsl:call-template>
                    -->
                </td>
            </tr></table>    
            
        </li>
        <li class="line"></li>
    </xsl:template>
    
    <xsl:template name="title">
        <xsl:param name="index" />
        <xsl:param name="record"  />
        <!--
        <a>
            <xsl:attribute name="id">title_<xsl:value-of select="$index" /></xsl:attribute>
            <xsl:attribute name="href">javascript:showDetail(<xsl:value-of select="$index" />);</xsl:attribute>
            <xsl:copy-of select="./FIELD[@NAME='title']" />
        </a>
        -->
        <div style="display:block;" >
        <span style="float:left; font-weight:bold;">
            <xsl:attribute name="id">title_<xsl:value-of select="$index" /></xsl:attribute>
            <xsl:copy-of select="$record/title/text() | $record/title/*" />
            
        </span>
        </div>                
        <div style="clear:both;" >
        </div>                
    </xsl:template>
    <xsl:template name="icons">
        <xsl:param name="zdroj" />
        <div class="icons" >
            <img border="0" class="valign" style="margin-top:3px;">
                <xsl:choose>
                <xsl:when test="contains($zdroj, 'MZK')">
                    <xsl:attribute name="src">img/icons/zdroj/mzk.gif</xsl:attribute>
                </xsl:when>
                <xsl:when test="contains($zdroj, 'Olomouci')">
                    <xsl:attribute name="src">img/icons/zdroj/vkol.gif</xsl:attribute>
                </xsl:when>
                <xsl:when test="contains($zdroj, 'NKP')">
                    <xsl:attribute name="src">img/icons/zdroj/nkp.gif</xsl:attribute>
                </xsl:when>
                </xsl:choose>
                <xsl:attribute name="title"><xsl:value-of select="$zdroj" /></xsl:attribute>
            </img>
        </div>
    </xsl:template>
    
</xsl:stylesheet>
