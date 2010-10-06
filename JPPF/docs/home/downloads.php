<?php $currentPage="Download" ?>
<html>
		<head>
		<title>JPPF Downloads
</title>
		<meta name="description" content="An open-source, Java-based, framework for parallel computing.">
		<meta name="keywords" content="JPPF, java, parallel computing, distributed computing, grid computing, parallel, distributed, cluster, grid, gloud, open source">
		<meta HTTP-EQUIV="Content-Type" content="text/html; charset=UTF-8">
		<link rel="shortcut icon" href="images/jppf-icon.ico" type="image/x-icon">
		<link rel="stylesheet" type="text/css" href="/jppf.css" title="Style">
	</head>
	<body>
		<div align="center">
		<div class="gwrapper" align="center">
					<?php
		if (!isset($currentPage))
		{
			$currentPage = $_REQUEST["page"];
			if (($currentPage == NULL) || ($currentPage == ""))
			{
				$currentPage = "Home";
			}
		}
		if ($currentPage != "Forums")
		{
		?>
		<div style="background-color: #E2E4F0; margin: 0px;height: 10px"><img src="/images/frame_top.gif"/></div>
		<?php
		}
		?>
		<!--<div class="jppfheader" style="border-left: 1px solid #6D78B6; border-right: 1px solid #6D78B6">-->
			<table width="100%" cellspacing="0" cellpadding="0" border="0" class="jppfheader" style="border-left: 1px solid #6D78B6; border-right: 1px solid #6D78B6">
				<tr style="height: 80px">
					<td width="20"></td>
					<td width="400" align="left" valign="center"><a href="/"><img src="/images/logo2.gif" border="0" alt="JPPF"/></a></td>
					<td align="right">
						<table border="0" cellspacing="0" cellpadding="0" style="height: 30px; background-color:transparent;">
							<tr>
								<td style="background: transparent url('/images/buttons/<?php if ($currentPage == "Home") echo "btn_start.gif"; else echo "btn_active_start.gif"; ?>') repeat-x scroll left bottom; width: 9px"></td>
								<td style="width: 1px"></td>
											<?php
			$itemClass = "";
			if ($currentPage == "Home")
			{
			?>
			<td class="headerMenuItem2" style="background: transparent url('/images/buttons/btn_main.gif') repeat-x scroll left bottom;">&nbsp;<a href="/index.php" class="headerMenuItem2">Home</a>&nbsp;</td>
			<?php
			}
			else
			{
			?>
			<td class="headerMenuItem" style="background: transparent url('/images/buttons/tab_active_main.gif') repeat-x scroll left bottom;">&nbsp;<a href="/index.php" class="headerMenuItem">Home</a>&nbsp;</td>
			<?php
			}
			?>
			<td style="width: 1px"></td>
											<?php
			$itemClass = "";
			if ($currentPage == "About")
			{
			?>
			<td class="headerMenuItem2" style="background: transparent url('/images/buttons/btn_main.gif') repeat-x scroll left bottom;">&nbsp;<a href="/about.php" class="headerMenuItem2">About</a>&nbsp;</td>
			<?php
			}
			else
			{
			?>
			<td class="headerMenuItem" style="background: transparent url('/images/buttons/tab_active_main.gif') repeat-x scroll left bottom;">&nbsp;<a href="/about.php" class="headerMenuItem">About</a>&nbsp;</td>
			<?php
			}
			?>
			<td style="width: 1px"></td>
											<?php
			$itemClass = "";
			if ($currentPage == "Download")
			{
			?>
			<td class="headerMenuItem2" style="background: transparent url('/images/buttons/btn_main.gif') repeat-x scroll left bottom;">&nbsp;<a href="/downloads.php" class="headerMenuItem2">Download</a>&nbsp;</td>
			<?php
			}
			else
			{
			?>
			<td class="headerMenuItem" style="background: transparent url('/images/buttons/tab_active_main.gif') repeat-x scroll left bottom;">&nbsp;<a href="/downloads.php" class="headerMenuItem">Download</a>&nbsp;</td>
			<?php
			}
			?>
			<td style="width: 1px"></td>
											<?php
			$itemClass = "";
			if ($currentPage == "Documentation")
			{
			?>
			<td class="headerMenuItem2" style="background: transparent url('/images/buttons/btn_main.gif') repeat-x scroll left bottom;">&nbsp;<a href="/wiki" class="headerMenuItem2">Documentation</a>&nbsp;</td>
			<?php
			}
			else
			{
			?>
			<td class="headerMenuItem" style="background: transparent url('/images/buttons/tab_active_main.gif') repeat-x scroll left bottom;">&nbsp;<a href="/wiki" class="headerMenuItem">Documentation</a>&nbsp;</td>
			<?php
			}
			?>
			<td style="width: 1px"></td>
											<?php
			$itemClass = "";
			if ($currentPage == "Forums")
			{
			?>
			<td class="headerMenuItem2" style="background: transparent url('/images/buttons/btn_main.gif') repeat-x scroll left bottom;">&nbsp;<a href="/forums" class="headerMenuItem2">Forums</a>&nbsp;</td>
			<?php
			}
			else
			{
			?>
			<td class="headerMenuItem" style="background: transparent url('/images/buttons/tab_active_main.gif') repeat-x scroll left bottom;">&nbsp;<a href="/forums" class="headerMenuItem">Forums</a>&nbsp;</td>
			<?php
			}
			?>
			<td style="width: 1px"></td>
								<td style="background: transparent url('/images/buttons/<?php if ($currentPage == "Forums") echo "btn_end.gif"; else echo "btn_active_end.gif"; ?>') repeat-x scroll right bottom; width: 9px"></td>
							</tr>
						</table>
					</td>
					<td width="20"></td>
				</tr>
			</table>
		<!--</div>-->
					<table border="0" cellspacing="0" cellpadding="5" width="100%px" style="border: 1px solid #6D78B6; border-top: 8px solid #6D78B6;">
			<tr>
				<td style="background-color: #FFFFFF">
				<div class="sidebar">
																				<?php
											$itemClass = "";
											if ($currentPage == "Home") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/" class="<?php echo $itemClass; ?>">&raquo; Home</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "About") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/about.php" class="<?php echo $itemClass; ?>">&raquo; About</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "Download") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/downloads.php" class="<?php echo $itemClass; ?>">&raquo; Download</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "Features") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/features.php" class="<?php echo $itemClass; ?>">&raquo; Features</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "Documentation") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/wiki" class="<?php echo $itemClass; ?>">&raquo; Documentation</a><br>
											</div>
															<a href="/api-2.0" class="<?php if ($currentPage == Javadoc) echo 'aboutMenuItem'; else echo 'aboutMenuItem2'; ?>">&raquo; Javadoc</a><br>
															<?php
											$itemClass = "";
											if ($currentPage == "Samples") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/samples-pack/index.php" class="<?php echo $itemClass; ?>">&raquo; Samples</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "License") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/license.php" class="<?php echo $itemClass; ?>">&raquo; License</a><br>
											</div>
				<hr style="background-color: #6D78B6"/>
															<?php
											$itemClass = "";
											if ($currentPage == "Press") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/press.php" class="<?php echo $itemClass; ?>">&raquo; Press</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "Release notes") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/release_notes.php?version=2.1" class="<?php echo $itemClass; ?>">&raquo; Release notes</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "Quotes") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/quotes.php" class="<?php echo $itemClass; ?>">&raquo; Quotes</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "Screenshots") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/screenshots.php" class="<?php echo $itemClass; ?>">&raquo; Screenshots</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "News") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/news.php" class="<?php echo $itemClass; ?>">&raquo; News</a><br>
											</div>
				<hr style="background-color: #6D78B6"/>
															<?php
											$itemClass = "";
											if ($currentPage == "Contacts") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/contacts.php" class="<?php echo $itemClass; ?>">&raquo; Contacts</a><br>
											</div>
															<?php
											$itemClass = "";
											if ($currentPage == "Services") $itemClass = 'aboutMenuItem';
											else $itemClass = 'aboutMenuItem2'; 
											?>
											<div class="<?php echo $itemClass; ?>">
											<a href="/services.php" class="<?php echo $itemClass; ?>">&raquo; Services</a><br>
											</div>
				<br/>
				</div>
				<div class="content">
	<?php $base = "http://sourceforge.net/projects/jppf-project/files/jppf-project/jppf%202.2/"; ?>
	<a name="2.0"></a>
	<h1>JPPF 2.2 Downloads</h1>
	<h3>Installer</h3>
	<a href="/download/jppf_ws.jnlp">Start the web installer by clicking here</a> (requires Java Web Start 1.5 or later)<br>
	<h3>Deployable module binaries</h3>
	<a href="<?php echo $base . 'JPPF-2.2-driver.zip/download'; ?>">JPPF server/driver distribution</a><br>
	<a href="<?php echo $base . 'JPPF-2.2-node.zip/download'; ?>">JPPF node distribution</a><br>
	<a href="<?php echo $base . 'JPPF-2.2-admin-ui.zip/download'; ?>">JPPF administration and monitoring console</a><br>
	<a href="<?php echo $base . 'JPPF-2.2-application-template.zip/download'; ?>">JPPF application template</a> (fully working, fully commented, to use as a starting point).<p>
	<h3>Source code and documentation</h3>
	<a href="<?php echo $base . 'JPPF-2.2-full-src.zip/download'; ?>">Full distribution with source code and required libraries</a><br>
	User Guide: <a href="/wiki">view online</a> or <a href="<?php echo $base . 'JPPF-2.2-User-Guide.zip/download'; ?>">download the PDF</a><br>
	API documentation: <a href="/api-2.0">browse online</a> or <a href="<?php echo $base . 'JPPF-2.2-api.zip/download'; ?>">download</a><p>
	<h3>Connectors and add-ons</h3>
	<a href="<?php echo $base . 'JPPF-2.2-GigaSpaces.zip/download'; ?>">GigaSpaces XAP connector</a><br>
	<a href="<?php echo $base . 'JPPF-2.2-j2ee-connector.zip/download'; ?>">J2EE Connector</a><br>
	<a href="<?php echo $base . 'JPPF-2.2-multiplexer.zip/download'; ?>">JPPF TCP multiplexer</a><p>
	<h3>Samples and tutorials</h3>
	<a href="<?php echo $base . 'JPPF-2.2-samples-pack.zip/download'; ?>">JPPF samples pack</a><br>
	Make sure to get started with our <a href="/wiki/index.php?title=A_first_taste_of_JPPF">online tutorial</a>
	<hr>
	<?php $base = "http://sourceforge.net/projects/jppf-project/files/jppf-project/jppf%201.9.2/"; ?>
	<a name="1.9.2"></a>
	<h1>JPPF 1.9.2 Downloads</h1>
	<h3>Source distribution</h3>
	<a href="<?php echo $base . 'jppf-src-1.9.2.zip/download'; ?>">Full distribution with source code and required libraries<p>
	<h3>Deployable module binaries</h3>
	<a href="<?php echo $base . 'jppf-driver-bin-1.9.2.zip/download'; ?>">JPPF server/driver distribution</a><br>
	<a href="<?php echo $base . 'jppf-node-bin-1.9.2.zip/download'; ?>">JPPF node distribution</a><br>
	<a href="<?php echo $base . 'jppf-gui-bin-1.9.2.zip/download'; ?>">JPPF administration and monitoring console</a><p>
	<h3>Connectors</h3>
	<a href="<?php echo $base . 'jppf-j2ee-connector-1.9.2.zip/download'; ?>">J2EE connector</a><br>
	<a href="<?php echo $base . 'JPPF-GigaSpaces-1.9.2.zip/download'; ?>">GigaSpaces XAP connector</a><p>
	<h3>Samples and add-ons</h3>
	<a href="<?php echo $base . 'jppf-samples-pack-1.9.2.zip/download'; ?>">JPPF samples pack</a><br>
	<a href="<?php echo $base . 'JPPFQuickStart-1.9.2.zip/download'; ?>">JPPF quick start guide</a><br>
	<a href="<?php echo $base . 'jppf-1.9.2-screensaver-win32.zip/download'; ?>">JPPF Windows node screen saver</a><br>
	<a href="<?php echo $base . 'jppf-multiplexer-bin-1.9.2.zip/download'; ?>">JPPF TCP multiplexer</a><p>
	<h3>API Documentation (Javadoc)</h3>
	<a href="<?php echo $base . 'jppf-api-1.9.2.zip/download'; ?>">Download</a> or <a href="/api-2.0">Browse online</a><p>
	<h3>All 1.9.2 files</h3>
	<a href="<?php echo $base ?>">All files</a><p>
	<h3>All JPPF releases</h3>
	All JPPF files can be found from <a href="http://sourceforge.net/projects/jppf-project/files/jppf-project"><b>this location</b></a>.
				</div>
									</td>
				</tr>
			</table>
				<!--<div align="center" style="width: 100%; border-left: 1px solid #6D78B6; border-right: 1px solid #6D78B6">-->
		<table border="0" cellspacing="0" cellpadding="0" width="100%" class="jppffooter">
			<tr><td colspan="*" style="height: 10px"></td></tr>
			</tr>
			<tr>
				<td align="center" style="font-size: 9pt; color: #6D78B6">
					<a href="http://sourceforge.net/donate/index.php?group_id=135654"><img src="http://images.sourceforge.net/images/project-support.jpg" width="88" height="32" border="0" alt="Support This Project" /></a>
				</td>
				<td align="center" style="font-size: 9pt; color: #6D78B6">Copyright &copy; 2005-2010 JPPF.org</td>
				<td align="right">
					<a href="http://www.parallel-matters.com"><img src="/images/pm_logo_tiny.jpg" border="0" alt="Powered by Parallel Matters" /></a>&nbsp;
					<a href="http://sourceforge.net/projects/jppf-project">
						<img src="http://sflogo.sourceforge.net/sflogo.php?group_id=135654&type=10" width="80" height="15" border="0"
							alt="Get JPPF at SourceForge.net. Fast, secure and Free Open Source software downloads"/>
					</a>
				</td>
				<td style="width: 10px"></td>
			</tr>
			<tr><td colspan="*" style="height: 10px"></td></tr>
		</table>
	<!--</div>-->
	<div style="background-color: #E2E4F0; width: 100%;"><img src="/images/frame_bottom.gif" border="0"/></div>
		</div>
		</div>
	</body>
</html>
