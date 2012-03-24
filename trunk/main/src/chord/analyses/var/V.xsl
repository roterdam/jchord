<xsl:stylesheet
	version="2.0"
	xmlns="http://www.w3.org/1999/xhtml"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:import href="M.xsl"/>

<xsl:template match="V">
    <xsl:text>&lt;</xsl:text>
		<xsl:apply-templates select="id(@Mid)"/>
    <xsl:text> </xsl:text>
	<xsl:value-of select="@name"/>
    <xsl:text>&gt;</xsl:text>
</xsl:template>

</xsl:stylesheet>

