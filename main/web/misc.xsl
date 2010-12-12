<xsl:stylesheet
	version="2.0"
	xmlns="http://www.w3.org/1999/xhtml"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="M">
	<xsl:variable name="file" select="@file"/>
	<xsl:variable name="line" select="@line"/>
	<a href="{$file}.html#{$line}">
		<xsl:value-of select="@sign"/>
	</a>
</xsl:template>

<xsl:template match="F">
	<xsl:variable name="file" select="@file"/>
	<xsl:variable name="line" select="@line"/>
    <a href="{$file}.html#{$line}">
    	<xsl:value-of select="@sign"/>
	</a>
</xsl:template>

<xsl:template match="V">
	<xsl:variable name="file" select="@file"/>
	<xsl:variable name="line" select="@line"/>
    <a href="{$file}.html#{$line}">
		<xsl:value-of select="@name"/>
	</a>
</xsl:template>

<xsl:template match="T">
	<xsl:variable name="file" select="@file"/>
	<xsl:variable name="line" select="@line"/>
    <a href="{$file}.html#{$line}">
		<xsl:value-of select="@name"/>
	</a>
</xsl:template>

<xsl:template match="O">
	<xsl:text>{</xsl:text>
		<xsl:for-each select="id(@Cids)">
   			<xsl:apply-templates select="."/>
			<xsl:if test="position()!=last()">
				<xsl:text>, </xsl:text>
			</xsl:if>
		</xsl:for-each>
	<xsl:text>}</xsl:text>
</xsl:template>

<xsl:template match="C">
    <xsl:text>[</xsl:text>
		<xsl:choose>
			<xsl:when test="@ids">
				<xsl:for-each select="id(@ids)">
    				<xsl:apply-templates select="."/>
					<xsl:if test="position()!=last()">
						::<wbr/>
					</xsl:if>
				</xsl:for-each>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>main</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
    <xsl:text>]</xsl:text>
</xsl:template>

<xsl:template match="A">
    <xsl:apply-templates select="id(@Mid)"/> <br/>
    Object:  <xsl:apply-templates select="id(@Oid)"/> <br/>
    Context: <xsl:apply-templates select="id(@Cid)"/>
</xsl:template>

<xsl:template match="H">
    <xsl:variable name="file" select="@file"/>
    <xsl:variable name="line" select="@line"/>
    <a href="{$file}.html#{$line}">
		<xsl:for-each select="tokenize(@type, '\.')">
			<xsl:value-of select="."/>
			<xsl:if test="position()!=last()">.<wbr/></xsl:if>
		</xsl:for-each>
	</a>
</xsl:template>

<xsl:template match="I">
    <xsl:variable name="file" select="@file"/>
    <xsl:variable name="line" select="@line"/>
	<a href="{$file}.html#{$line}">
		<xsl:value-of select="id(@Mid)/@sign"/>
	</a>
</xsl:template>

<xsl:template match="L">
    <xsl:variable name="file" select="@file"/>
    <xsl:variable name="line" select="@line"/>
	<a href="{$file}.html#{$line}">
		<xsl:value-of select="id(@Mid)/@sign"/>
	</a>
</xsl:template>

<xsl:template match="E">
	<xsl:variable name="file" select="@file"/>
	<xsl:variable name="line" select="@line"/>
	<a href="{$file}.html#{$line}">
		<xsl:value-of select="id(@Mid)/@sign"/>
	</a>
	(<xsl:value-of select="@rdwr"/>)
</xsl:template>

</xsl:stylesheet>

