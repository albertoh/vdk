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
                <xsl:call-template name="doc">
                    <xsl:with-param name="index" select="position()" />
                </xsl:call-template>
            </xsl:for-each>
        </ul>
    </xsl:template>
    
    <xsl:template name="doc">
        <xsl:param name="index" />
        <xsl:variable name="numDocs" select="count(./arr[@name='id']/str)" />
        <xsl:variable name="code" select="./str[@name='code']" />
        <xsl:variable name="hasCollapsed" select="$numDocs &gt; 1" />
        <li>
            <xsl:attribute name="id">res_<xsl:value-of select="$code" /></xsl:attribute>
            <xsl:attribute name="class">res<xsl:if test="hasCollapsed"> collapsed</xsl:if></xsl:attribute>
            <input type="hidden" class="code">
                <xsl:attribute name="value"><xsl:value-of select="$code" /></xsl:attribute>
            </input>
            <input type="hidden" class="identifier">
                <xsl:attribute name="value"><xsl:value-of select="./str[@name='id']" /></xsl:attribute>
            </input>
            <input type="hidden" class="numDocs">
                <xsl:attribute name="value"><xsl:value-of select="$numDocs" /></xsl:attribute>
            </input>
            <table width="100%"><tr>
                <td valign="top">
                    <div>
                        <xsl:if test="./arr[@name='nabidka']/str">  
                            <div class="nabidka">Nabizeno:      
                            <xsl:for-each select="./arr[@name='nabidka']/str">
                                <div>
                                    <xsl:attribute name="data-id">
                                        <xsl:value-of  select="." />
                                    </xsl:attribute>
                                </div>
                            </xsl:for-each>
                            </div>
                        </xsl:if>
                        <span style="float:left;">
                            <a class="ui-icon ui-icon-extlink" target="_view">
                                <xsl:attribute name="title">nahlédnout originální metadata</xsl:attribute>
                                <xsl:attribute name="href">original?id=<xsl:value-of select="./arr[@name='id']/str" />&amp;path=<xsl:value-of select="./arr[@name='file']/str"/></xsl:attribute>view</a>
                        </span>
                        <span class="offerdoc" style="float:left;">
                            <a class="ui-icon ui-icon-flag" >
                                <xsl:attribute name="title">přidat do nabídky</xsl:attribute>
                                <xsl:attribute name="href">javascript:vdk.addToOffer('<xsl:value-of select="$code" />')</xsl:attribute>offer</a>
                        </span>
                        <span class="demanddoc" style="float:left;">
                            <a class="ui-icon ui-icon-cart" >
                                <xsl:attribute name="title">přidat do poptávky</xsl:attribute>
                                <xsl:attribute name="href">javascript:vdk.addToDemand('<xsl:value-of select="$code" />')</xsl:attribute>demand</a>
                        </span>
                        <span class="csv" style="float:left;">
                            <a class="ui-icon ui-icon-document" >
                                <xsl:attribute name="title">csv format</xsl:attribute>
                                <xsl:attribute name="data-csv"><xsl:value-of select="./arr[@name='export']/str" /></xsl:attribute>
                                <xsl:attribute name="href">javascript:vdk.showCSV(this)</xsl:attribute>csv</a>
                        </span>
                        <div class="diff ui-state-error" style="float:left;display:none;"><span class="ui-icon ui-icon-alert">has diff</span>
                        <div class="titles diffs">
                        <xsl:for-each select="./arr[@name='title']/str" >
                            <xsl:variable name="idx" select="position()" />
                            <div>
                                <img width="16" hspace="3px">
                                    <xsl:attribute name="src">
                                        <xsl:call-template name="icons">
                                            <xsl:with-param name="zdroj" select="/response/result/doc[position()=$index]/arr[@name='zdroj']/str[position()=$idx]"/>
                                        </xsl:call-template>
                                    </xsl:attribute>
                                </img>
                                <span class="title">
                                    <xsl:value-of select="." />
                                </span>
                            </div>
                        </xsl:for-each>  
                        </div>
                        </div>
                        <xsl:if test="not($numDocs = 0)"> (<xsl:value-of select="$numDocs" />&#160;<xsl:choose>
                        <xsl:when test="$numDocs = 1">
                            <xsl:value-of select="rb:getString($i18n,'results.collapsed.singular')"/>
                        </xsl:when>
                        <xsl:when test="$numDocs &lt; 5">
                            <xsl:value-of select="rb:getString($i18n,'results.collapsed.plural_1')"/>
                            
                            &#160;<xsl:value-of select="rb:getString($i18n,'field.code_type')"/>&#160;
                            <xsl:value-of select="./str[@name='code_type']" />
                        </xsl:when>
                        <xsl:when test="$numDocs &gt; 4">
                            <xsl:value-of select="rb:getString($i18n,'results.collapsed.plural_2')"/>
                            &#160;<xsl:value-of select="rb:getString($i18n,'field.code_type')"/>&#160;
                            <xsl:value-of select="./str[@name='code_type']" />
                        </xsl:when>
                        </xsl:choose>)</xsl:if>
                    </div>
                    <div class="title"><xsl:value-of select="./arr[@name='title']/str" /></div>
                    <xsl:if test="./arr[@name='author']/str">
                    <div><span><xsl:value-of select="rb:getString($i18n,'field.authors')"/></span>: 
                        <xsl:for-each select="./arr[@name='author']/str"><xsl:value-of select="." />; </xsl:for-each></div>
                    </xsl:if>
                    
                    <xsl:if test="./arr[@name='mistovydani']/str">
                    <div><span><xsl:value-of select="rb:getString($i18n,'field.vydavatel')"/></span>: 
                        <xsl:for-each select="./arr[@name='vydavatel']/str">
                            <xsl:variable name="pos" select="position()" />
                            <xsl:value-of select="." />
                            (<xsl:value-of select="../../arr[@name='mistovydani']/str[position()=$pos]" />, <xsl:value-of select="../../arr[@name='datumvydani']/str[position()=$pos]" />) 
                        </xsl:for-each>
                    </div>
                    </xsl:if>
                    
                    <xsl:if test="./str[@name='ccnb']/text()">
                    <div><span><xsl:value-of select="rb:getString($i18n,'field.ccnb')"/></span>: 
                        <xsl:value-of select="./arr[@name='ccnb']/str" /></div>
                    </xsl:if>    

                    <xsl:if test="./result[@name='doclist']/doc/str[@name='isbn']/text()">
                    <div><span><xsl:value-of select="rb:getString($i18n,'field.isbn')"/></span>: 
                        <xsl:value-of select="./result[@name='doclist']/doc/str[@name='isbn']" /></div>
                    </xsl:if>    
                    
                    <xsl:call-template name="exemplare">
                        <xsl:with-param name="index" select="$index" />
                    </xsl:call-template>
                    
                    <table class="tex">
                        <thead>
                            <tr>
                            <th></th>
                            <th>signatura</th>
                            <th>status</th>
                            <th>dilciKnih</th>
                            
                            <th>rocnik/svazek</th>
                            <th>cislo</th>
                            <th>rok</th>
                            <th class="actions">
                                <span class="ui-icon ui-icon-plus">expand/collapse</span>
                            </th>
                            
                            </tr>
                        </thead>
                        <tbody></tbody>
                    </table>  
        
                    <div style="float:right;"></div>
                </td>
                    <!--
                <td class="actions" valign="top" width="100px">
                </td>
                    -->
            </tr></table> 
        </li>
        <li class="line"></li>
    </xsl:template>
    
    <xsl:template name="exemplare">
        <xsl:param name="index" />
        <xsl:variable name="zdroj" select="./str[@name='zdroj']" />  
        <div class="ex">
            <xsl:attribute name="data-ex">{"exemplare":[
                <xsl:for-each select="./arr[@name='ex']/str">
                    <xsl:value-of  select="." /><xsl:if test="position()!=last()">,</xsl:if>
                </xsl:for-each>]}
            </xsl:attribute>
            
            <xsl:attribute name="data-nabidka">{"exemplare":[
                <xsl:for-each select="./arr[@name='ex_nabidka']/str">
                    <xsl:value-of  select="." /><xsl:if test="position()!=last()">,</xsl:if>
                </xsl:for-each>]}
            </xsl:attribute>
        </div> 
    </xsl:template>
    
    <xsl:template name="icons">
        <xsl:param name="zdroj" />
        <xsl:choose>
        <xsl:when test="contains($zdroj, 'MZK')">img/icons/zdroj/mzk.gif</xsl:when>
        <xsl:when test="contains($zdroj, 'VKOL')">img/icons/zdroj/vkol.gif</xsl:when>
        <xsl:when test="contains($zdroj, 'NKC')">img/icons/zdroj/nkp.gif</xsl:when>
        <xsl:otherwise>img/icons/<xsl:value-of select="$zdroj"/>.gif</xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    
    
</xsl:stylesheet>
