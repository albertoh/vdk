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
            <xsl:for-each select="/response/lst[@name='grouped']/lst[@name='code']/arr/lst" >
                <xsl:call-template name="group">
                    <xsl:with-param name="index" select="position()" />
                </xsl:call-template>
            </xsl:for-each>
        </ul>
    </xsl:template>
    
    <xsl:template name="group">
        <xsl:param name="index" />
        <xsl:variable name="numDocs" select="./result[@name='doclist']/@numFound" />
        <xsl:variable name="hasCollapsed" select="$numDocs &gt; 1" />
        <xsl:variable name="hasMoreCollapsed" select="$numDocs &gt; count(result[@name='doclist']/doc)" />
        <xsl:variable name="contentid" select="str[@name='groupValue']" />
        <li>
            <xsl:attribute name="id">res_<xsl:value-of select="$contentid" /></xsl:attribute>
            <xsl:attribute name="class">res<xsl:if test="$hasMoreCollapsed"> collapsed</xsl:if></xsl:attribute>
            <input type="hidden" class="groupid">
                <xsl:attribute name="value"><xsl:value-of select="$contentid" /></xsl:attribute>
            </input>
            <input type="hidden" class="numDocs">
                <xsl:attribute name="value"><xsl:value-of select="$numDocs" /></xsl:attribute>
            </input>
            <input type="hidden" class="identifier">
                <xsl:attribute name="value"><xsl:value-of select="result[@name='doclist']/doc/str[@name='id']" /></xsl:attribute>
            </input>
            <table width="100%"><tr>
                <td valign="top">
                    <div>
                        
                        <span style="float:left;">
                            <a class="ui-icon ui-icon-extlink" target="_view">
                                <xsl:attribute name="href">original?id=<xsl:value-of select="result[@name='doclist']/doc/str[@name='id']" />&amp;path=<xsl:value-of select="result[@name='doclist']/doc/str[@name='file']"/></xsl:attribute>view</a>
                        </span>
                        <div class="diff ui-state-error" style="float:left;display:none;"><span class="ui-icon ui-icon-alert">has diff</span>
                        <div class="titles diffs">
                        <xsl:for-each select="result[@name='doclist']/doc" >
                            <div>
                                <img width="16" hspace="3px">
                                    <xsl:attribute name="src">
                                        <xsl:call-template name="icons"><xsl:with-param name="zdroj" select="./str[@name='zdroj']"/></xsl:call-template>
                                    </xsl:attribute>
                                </img>
                                <span class="title">
                                    <xsl:value-of select="./arr[@name='title']/str" />
                                </span>
                            </div>
                        </xsl:for-each>  
                        </div>
                        </div>
                        <xsl:if test="not($numDocs = 0)"><span style="color:#545454;font-style:italic;"> (<xsl:value-of select="$numDocs" />&#160;<xsl:choose>
                        <xsl:when test="$numDocs = 1">
                            <xsl:value-of select="rb:getString($i18n,'results.collapsed.singular')"/>
                        </xsl:when>
                        <xsl:when test="$numDocs &lt; 5">
                            <xsl:value-of select="rb:getString($i18n,'results.collapsed.plural_1')"/>
                            
                            &#160;<xsl:value-of select="rb:getString($i18n,'field.code_type')"/>&#160;
                            <xsl:value-of select="./result[@name='doclist']/doc/str[@name='code_type']" />
                        </xsl:when>
                        <xsl:when test="$numDocs &gt; 4">
                            <xsl:value-of select="rb:getString($i18n,'results.collapsed.plural_2')"/>
                            &#160;<xsl:value-of select="rb:getString($i18n,'field.code_type')"/>&#160;
                            <xsl:value-of select="./result[@name='doclist']/doc/str[@name='code_type']" />
                        </xsl:when>
                        </xsl:choose>)</span></xsl:if>
                    </div>
                    <div><span class="title"><b><xsl:value-of select="result[@name='doclist']/doc/arr[@name='title']/str" /></b></span></div>
                    <xsl:if test="./result[@name='doclist']/doc/arr[@name='author']/str">
                    <div><span><xsl:value-of select="rb:getString($i18n,'field.authors')"/></span>: 
                        <xsl:value-of select="./result[@name='doclist']/doc/arr[@name='author']/str" /></div>
                    </xsl:if>
                    
                    <xsl:if test="./result[@name='doclist']/doc/str[@name='ccnb']/text()">
                    <div><span><xsl:value-of select="rb:getString($i18n,'field.ccnb')"/></span>: 
                        <xsl:value-of select="./result[@name='doclist']/doc/str[@name='ccnb']" /></div>
                    </xsl:if>    

                    <xsl:if test="./result[@name='doclist']/doc/str[@name='isbn']/text()">
                    <div><span><xsl:value-of select="rb:getString($i18n,'field.isbn')"/></span>: 
                        <xsl:value-of select="./result[@name='doclist']/doc/str[@name='isbn']" /></div>
                    </xsl:if>    


                    <xsl:for-each select="result[@name='doclist']/doc" >
                        <xsl:call-template name="hit">
                            <xsl:with-param name="index" select="$index" />
                            <xsl:with-param name="contentid" select="$contentid" />
                            <xsl:with-param name="numDocs" select="$numDocs" />
                            <xsl:with-param name="hasCollapsed" select="$hasCollapsed" />
                        </xsl:call-template>
                    </xsl:for-each>
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
                            
                            </tr>
                        </thead>
                        <tbody></tbody>
                    </table>  
        
                    <div style="float:right;"></div>
                </td>
                <td class="actions" valign="top" width="100px">
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
    
    <xsl:template name="hit">
        <xsl:param name="index" />
        <xsl:param name="contentid" />
        <xsl:param name="numDocs" />
        <xsl:param name="hasCollapsed" />
        
        <xsl:variable name="zdroj" select="./str[@name='zdroj']" />  
        <div class="ex">
            <xsl:attribute name="data-ex">{"exemplare":[
                <xsl:for-each select="./arr[@name='ex']/str">
                    <xsl:value-of  select="." /><xsl:if test="position()!=last()">,</xsl:if>
                </xsl:for-each>]}
            </xsl:attribute>
            <xsl:attribute name="data-icon">
                 <xsl:call-template name="icons"><xsl:with-param name="zdroj" select="./str[@name='zdroj']"/></xsl:call-template>
            </xsl:attribute>
            <xsl:attribute name="data-zdroj"><xsl:value-of select="$zdroj" /></xsl:attribute>  
            <xsl:attribute name="data-uri"><xsl:value-of select="./str[@name='url']" /></xsl:attribute>  
                 
        </div> 
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
        <xsl:choose>
        <xsl:when test="contains($zdroj, 'MZK')">img/icons/zdroj/mzk.gif</xsl:when>
        <xsl:when test="contains($zdroj, 'VKOL')">img/icons/zdroj/vkol.gif</xsl:when>
        <xsl:when test="contains($zdroj, 'NKC')">img/icons/zdroj/nkp.gif</xsl:when>
        <xsl:otherwise>img/icons/<xsl:value-of select="$zdroj"/>.gif</xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
</xsl:stylesheet>
