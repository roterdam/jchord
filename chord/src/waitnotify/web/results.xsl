<xsl:stylesheet
	version="2.0"
	xmlns="http://www.w3.org/1999/xhtml"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:import href="misc.xsl"/>

<xsl:template match="/">
	<xsl:result-document href="{$m_filename}">
	<html>
	<head>
		<title>Wait Notify Errors</title>
		<link rel="stylesheet" href="style.css" type="text/css"/>
	</head>
	<body>
		<table class="summary">
			<tr>
				<td>Kind</td><td>Read access</td><td>Write access</td>
			</tr>
			<xsl:for-each select="results/waitNotifyErrorList/waitNotifyError">
				<xsl:variable name="kind" select="@kind"/>
				<xsl:variable name="e1id" select="@E1id"/>
				<xsl:variable name="e2id" select="@E2id"/>
 				<tr>
					<td>$kind</td>
					<td><xsl:apply-templates select="$e1id"/></td>
					<td><xsl:apply-templates select="$e2id"/> <br/>
				</tr>
			</xsl:for-each>
		</table>
	</body>
	</html>
	</xsl:result-document>
</xsl:template>

</xsl:stylesheet>

