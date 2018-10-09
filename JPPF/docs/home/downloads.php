<?php $currentPage="Download" ?>
<html lang="en" xml:lang="en" xmlns="http://www.w3.org/1999/xhtml">
	  <head>
    <title>JPPF Downloads
</title>
    <meta name="description" content="The open source grid computing solution">
    <meta name="keywords" content="JPPF, java, parallel computing, distributed computing, grid computing, parallel, distributed, cluster, grid, cloud, open source, android, .net">
    <meta HTTP-EQUIV="Content-Type" content="text/html; charset=UTF-8">
    <link rel="shortcut icon" href="/images/jppf-icon.ico" type="image/x-icon">
    <link rel="stylesheet" type="text/css" href="/jppf.css" title="Style">
  </head>
	<body>
		<div align="center">
		<div class="gwrapper" align="center">
			<div style="display: none">JPPF, java, parallel computing, distributed computing, grid computing, parallel, distributed, cluster, grid, cloud, open source, android, .net</div>
    <?php
    if (!isset($currentPage)) {
      $currentPage = $_REQUEST["page"];
      if (($currentPage == NULL) || ($currentPage == "")) {
        $currentPage = "Home";
      }
    }
    if ($currentPage != "Forums") {
    ?>
    <div style="background-color: #E2E4F0">
      <div class="frame_top"/></div>
    </div>
    <?php
    }
    ?>
    <table width="100%" cellspacing="0" cellpadding="0" border="0" class="jppfheader" style="border-left: 1px solid #6D78B6; border-right: 1px solid #6D78B6">
      <tr style="height: 80px">
        <td width="15"></td>
        <td width="191" align="left" valign="center"><a href="/"><img src="/images/logo2.gif" border="0" alt="JPPF" style="box-shadow: 4px 4px 4px #6D78B6;"/></a></td>
        <td width="140" align="center" style="padding-left: 5px; padding-right: 5px"><h3 class="header_slogan">The open source<br>grid computing<br>solution</h3></td>
        <td width="80"></td>
        <td align="right">
          <table border="0" cellspacing="0" cellpadding="0" style="height: 30px; background-color:transparent;">
            <tr>
              <td style="width: 1px"></td>
              <?php $cl = (($currentPage == "Home") ? "headerMenuItem2" : "headerMenuItem") . " " . "header_item_start"; ?>
<td class="<?php echo $cl; ?>">&nbsp;<a href="/index.php" class="<?php echo $cl; ?>">Home</a>&nbsp;</td>
<td style="width: 1px"></td>
              <?php $cl = (($currentPage == "About") ? "headerMenuItem2" : "headerMenuItem") . " " . ""; ?>
<td class="<?php echo $cl; ?>">&nbsp;<a href="/about.php" class="<?php echo $cl; ?>">About</a>&nbsp;</td>
<td style="width: 1px"></td>
              <?php $cl = (($currentPage == "Features") ? "headerMenuItem2" : "headerMenuItem") . " " . ""; ?>
<td class="<?php echo $cl; ?>">&nbsp;<a href="/features.php" class="<?php echo $cl; ?>">Features</a>&nbsp;</td>
<td style="width: 1px"></td>
              <?php $cl = (($currentPage == "Download") ? "headerMenuItem2" : "headerMenuItem") . " " . ""; ?>
<td class="<?php echo $cl; ?>">&nbsp;<a href="/downloads.php" class="<?php echo $cl; ?>">Download</a>&nbsp;</td>
<td style="width: 1px"></td>
              <?php $cl = (($currentPage == "Documentation") ? "headerMenuItem2" : "headerMenuItem") . " " . ""; ?>
<td class="<?php echo $cl; ?>">&nbsp;<a href="/doc" class="<?php echo $cl; ?>">Documentation</a>&nbsp;</td>
<td style="width: 1px"></td>
              <?php $cl = (($currentPage == "Forums") ? "headerMenuItem2" : "headerMenuItem") . " " . "header_item_end"; ?>
<td class="<?php echo $cl; ?>">&nbsp;<a href="/forums" class="<?php echo $cl; ?>">Forums</a>&nbsp;</td>
<td style="width: 1px"></td>
            </tr>
          </table>
        </td>
        <td width="15"></td>
      </tr>
    </table>
			<table border="0" cellspacing="0" cellpadding="5" width="100%px" style="border: 1px solid #6D78B6; border-top: 8px solid #6D78B6;">
			<tr>
				<td style="background-color: #FFFFFF">
				<div class="sidebar">
					        <?php if ($currentPage == "Home") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/" class="<?php echo $itemClass; ?>">&raquo; Home</a><br></div>
        <?php if ($currentPage == "About") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/about.php" class="<?php echo $itemClass; ?>">&raquo; About</a><br></div>
        <?php if ($currentPage == "Download") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/downloads.php" class="<?php echo $itemClass; ?>">&raquo; Download</a><br></div>
        <?php if ($currentPage == "Features") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/features.php" class="<?php echo $itemClass; ?>">&raquo; Features</a><br></div>
        <?php if ($currentPage == "Patches") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/patches.php" class="<?php echo $itemClass; ?>">&raquo; Patches</a><br></div>
        <?php if ($currentPage == "Samples") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/samples-pack/index.php" class="<?php echo $itemClass; ?>">&raquo; Samples</a><br></div>
        <?php if ($currentPage == "License") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/license.php" class="<?php echo $itemClass; ?>">&raquo; License</a><br></div>
        <?php if ($currentPage == "Source code") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="https://github.com/jppf-grid/JPPF" class="<?php echo $itemClass; ?>">&raquo; Source code</a><br></div>
        <hr/>
                <?php if ($currentPage == "All docs") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/doc" class="<?php echo $itemClass; ?>">&raquo; All docs</a><br></div>
        <?php if ($currentPage == "v6.1 (alpha)") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/6.1" class="<?php echo $itemClass; ?>">v6.1 (alpha)</a><br></div>
        <?php if ($currentPage == "v6.0") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/6.0" class="<?php echo $itemClass; ?>">v6.0</a><br></div>
        <?php if ($currentPage == "v5.2") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/5.2" class="<?php echo $itemClass; ?>">v5.2</a><br></div>
        <?php if ($currentPage == "v5.1") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/doc/5.1" class="<?php echo $itemClass; ?>">v5.1</a><br></div>
        <?php if ($currentPage == "All Javadoc") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/doc/#javadoc" class="<?php echo $itemClass; ?>">&raquo; All Javadoc</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/javadoc/6.1" class="<?php echo $itemClass; ?>">v6.1 (alpha)</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/javadoc/6.0" class="<?php echo $itemClass; ?>">v6.0</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/javadoc/5.2" class="<?php echo $itemClass; ?>">v5.2</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/javadoc/5.1" class="<?php echo $itemClass; ?>">v5.1</a><br></div>
        <?php if ($currentPage == "All .Net APIs") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/doc#csdoc" class="<?php echo $itemClass; ?>">&raquo; All .Net APIs</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/csdoc/6.1" class="<?php echo $itemClass; ?>">v6.1 (alpha)</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/csdoc/6.0" class="<?php echo $itemClass; ?>">v6.0</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/csdoc/5.2" class="<?php echo $itemClass; ?>">v5.2</a><br></div>
        <?php $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/csdoc/5.1" class="<?php echo $itemClass; ?>">v5.1</a><br></div>
        <hr/>
        <?php if ($currentPage == "Issue tracker") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/tracker/tbg" class="<?php echo $itemClass; ?>">&raquo; Issue tracker</a><br></div>
        <?php if ($currentPage == "bugs") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/1/search/1" class="<?php echo $itemClass; ?>">bugs</a><br></div>
        <?php if ($currentPage == "features") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/9/search/1" class="<?php echo $itemClass; ?>">features</a><br></div>
        <?php if ($currentPage == "enhancements") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/2/search/1" class="<?php echo $itemClass; ?>">enhancements</a><br></div>
        <?php if ($currentPage == "next version") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/8/search/1" class="<?php echo $itemClass; ?>">next version</a><br></div>
        <?php if ($currentPage == "maintenance") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>">&nbsp;&nbsp;&nbsp;<a href="/tracker/tbg/jppf/issues/find/saved_search/22/search/1" class="<?php echo $itemClass; ?>">maintenance</a><br></div>
        <hr/>
        <?php if ($currentPage == "Press") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/press.php" class="<?php echo $itemClass; ?>">&raquo; Press</a><br></div>
        <?php if ($currentPage == "Release notes") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/release_notes.php?version=6.0" class="<?php echo $itemClass; ?>">&raquo; Release notes</a><br></div>
        <?php if ($currentPage == "Quotes") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/quotes.php" class="<?php echo $itemClass; ?>">&raquo; Quotes</a><br></div>
        <?php if ($currentPage == "Screenshots") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/screenshots" class="<?php echo $itemClass; ?>">&raquo; Screenshots</a><br></div>
        <?php if ($currentPage == "CI") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/ci.php" class="<?php echo $itemClass; ?>">&raquo; CI</a><br></div>
        <?php if ($currentPage == "News") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/news.php" class="<?php echo $itemClass; ?>">&raquo; News</a><br></div>
        <hr/>
        <?php if ($currentPage == "Contacts") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/contacts.php" class="<?php echo $itemClass; ?>">&raquo; Contacts</a><br></div>
        <?php if ($currentPage == "Services") $itemClass = 'aboutMenuItem'; else $itemClass = 'aboutMenuItem2'; ?><div class="<?php echo $itemClass; ?>"><a href="/services.php" class="<?php echo $itemClass; ?>">&raquo; Services</a><br></div>
        <br/>
				</div>
				<div class="jppf_content">
  <table style="width: 100%"><tr>
    <td style="width: 59%"><h1 align="right">Downloads</h1></td>
    <td style="font-size: 10pt"><div align="right">Powered by<a href="https://www.ej-technologies.com/products/jprofiler/overview.html"> <img src="https://www.ej-technologies.com/images/product_banners/jprofiler_small.png"/><br>Java profiler</a></div></td>
  </tr></table>
  <div class="blockWithHighlightedTitle" style="vertical-align: middle">
    <table style="padding: 2px"><tr>
      <td style="width: 20px"><img src="images/icons/folder-download.png"></td>
      <td><h4>All JPPF releases:</h4></td>
      <td style="vertical-align: middle">
        <ul class="samplesList" style="margin-bottom: 0px; margin-left: 40px;">
          <li><a href="https://github.com/jppf-grid/JPPF/tags"><b>JPPF releases on Github</b></a></li>
        </ul>
      </td>
      <td style="vertical-align: middle">
        <ul class="samplesList" style="margin-bottom: 0px; margin-left: 70px;">
          <li><a href="http://sourceforge.net/projects/jppf-project/files/jppf-project"><b>Older releases on SourceForge</b></a></li>
        </ul>
      </td>
    </tr></table>
  </div>
  <br>
  <div class="column_left" style="text-align: justify; padding: 0px">
    <div class="blockWithHighlightedTitle">
      <?php
        $tag1 = "v_6_0";
        $ver1 = "6.0";
        $base = "https://github.com/jppf-grid/JPPF/releases/download/" . $tag1 . "/";
      ?>
      <a name="<?php echo $ver1 ?>"></a>
      <div align="left" style="border-bottom: solid 1px #B5C0E0; padding: 0px; margin-left: -5px; margin-right: -8px">
  <h1 style="margin: 10px 0px">&nbsp;<img src="images/icons/download.png" class="titleWithIcon"/>JPPF <?php echo $ver1 ?></h1>
</div>
      <h3>Web Installer</h3>
      <a href="<?php echo '/download/' . $ver1 . '/JPPF-' . $ver1 . '-Web-Installer.jar'; ?>">Download the web installer jar</a> and run it by either:
      <ul class="list_nomargin">
        <li>double-clicking the downloaded file</li>
        <li>typing "<b>java -jar <?php echo 'JPPF-' . $ver1 . '-Web-Installer.jar'; ?></b>"</li>
      </ul>
      <h3>Deployable JPPF binaries</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-driver.zip'; ?>">Server/driver distribution</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-node.zip'; ?>">Node distribution</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-application-template.zip'; ?>">Application template</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-admin-ui.zip'; ?>">Desktop administration and monitoring console</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-admin-web.zip'; ?>">Web administration and monitoring console</a></li>
      </ul>
      <h3>Deployable .Net binaries</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-dotnet.zip'; ?>">.Net demo application</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-node-dotnet.zip'; ?>">.Net-enabled node distribution</a></li>
      </ul>
      <h3>Android Node</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-node-android-redist.zip'; ?>">Android node app binaries and dependencies</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-node-android-src.zip'; ?>">Full source as a Gradle/Android Studio project</a></li>
        <li><table cellpadding="3"><tr>
          <td valign="middle">
            <a href="<?php echo $base . 'JPPF-' . $ver1 . '-AndroidNode.apk'; ?>">You may also download the<br>APK directly to a device:</a>
          </td>
          <td>&nbsp;</td>
          <td valign="middle" style="white-space: nowrap">
            <a class="yhd2" href="<?php echo $base . 'JPPF-' . $ver1 . '-AndroidNode.apk'; ?>">
              <span style="vertical-align: top">Node APK</span>
            </a><br>
          </td>
        </tr></table></li>
      </ul>
      <h3>Source code and documentation</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo 'https://github.com/lolocohen/JPPF/archive/' . $tag1 . '.zip'; ?>">Full source code distribution</a></li>
        <li>User Guide: <a href="/doc/6.0">view online</a> or <a href="<?php echo $base . 'JPPF-' . $ver1 . '-User-Guide.zip'; ?>">download the PDF</a></li>
        <li>API documentation: <a href="/javadoc/6.0">browse online</a> or <a href="<?php echo $base . 'JPPF-' . $ver1 . '-api.zip'; ?>">download</a></li>
      </ul>
      <h3>Connectors and add-ons</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-j2ee-connector.zip'; ?>">J2EE Connector</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-jmxremote-nio.zip'; ?>">Standalone NIO-based JMX remote connector</a></li>
      </ul>
      <h3>Samples and tutorials</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver1 . '-samples-pack.zip'; ?>">JPPF samples pack</a></li>
        <li>Make sure to get started with our <a href="/doc/6.0/index.php?title=A_first_taste_of_JPPF">online tutorial</a><br/></li>
      </ul>
    </div>
    <br>
  </div>
  <div class="column_right" style="text-align: justify; padding: 0px">
    <div class="blockWithHighlightedTitle">
      <?php
        $ver2 = "5.2.9";
        $base = "http://sourceforge.net/projects/jppf-project/files/jppf-project/jppf%20" . $ver2 . "/";
      ?>
      <a name="<?php echo $ver2 ?>"></a>
      <div align="left" style="border-bottom: solid 1px #B5C0E0; padding: 0px; margin-left: -5px; margin-right: -8px">
  <h1 style="margin: 10px 0px">&nbsp;<img src="images/icons/download.png" class="titleWithIcon"/>JPPF <?php echo $ver2 ?></h1>
</div>
      <h3>Web Installer</h3>
      <a href="<?php echo '/download/' . $ver2 . '/JPPF-' . $ver2 . '-Web-Installer.jar'; ?>">Download the web installer jar</a> and run it by either:
      <ul class="list_nomargin">
        <li>double-clicking the downloaded file</li>
        <li>typing "<b>java -jar <?php echo 'JPPF-' . $ver2 . '-Web-Installer.jar'; ?></b>"</li>
      </ul>
      <h3>Deployable module binaries</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver2 . '-driver.zip/download'; ?>">JPPF server/driver distribution</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver2 . '-node.zip/download'; ?>">JPPF node distribution</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver2 . '-admin-ui.zip/download'; ?>">JPPF administration and monitoring console</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver2 . '-application-template.zip/download'; ?>">JPPF application template</a>.</li>
      </ul>
      <h3>Deployable .Net binaries</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver2 . '-dotnet.zip/download'; ?>">JPPF .Net demo application</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver2 . '-node-dotnet.zip/download'; ?>">JPPF .Net-enabled node distribution</a></li>
      </ul>
      <h3>Android Node</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver2 . '-node-android-redist.zip/download'; ?>">Android node app binaries and dependencies</a></li>
        <li><a href="<?php echo $base . 'JPPF-' . $ver2 . '-node-android-src.zip/download'; ?>">Full source as a Gradle/Android Studio project</a></li>
        <li style="padding: 5px 0px">
          <table cellpadding="0"><tr>
            <td valign="bottom">
              <a href="<?php echo $base . 'JPPF-' . $ver2 . '-AndroidNode.apk/download'; ?>">You may also download the<br>APK directly to a device:</a>
            </td>
            <td>&nbsp;</td>
            <td valign="middle" style="white-space: nowrap">
              <a class="yhd2" href="<?php echo $base . 'JPPF-' . $ver2 . '-AndroidNode.apk/download'; ?>">
               <span style="vertical-align: top">Node APK</span>
              </a><br>
            </td>
          </tr></table>
        </li>
      </ul>
      <h3>Sources and documentation</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver2 . '-full-src.zip/download'; ?>">Full distribution with source code and required libraries</a></li>
        <li>User Guide: <a href="/doc/5.2">view online</a> or <a href="<?php echo $base . 'JPPF-' . $ver2 . '-User-Guide.zip/download'; ?>">download the PDF</a></li>
        <li>API documentation: <a href="/api-5">browse online</a> or <a href="<?php echo $base . 'JPPF-' . $ver2 . '-api.zip/download'; ?>">download</a></li>
      </ul>
      <h3>Connectors and add-ons</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver2 . '-j2ee-connector.zip/download'; ?>">J2EE Connector</a></li>
      </ul>
      <h3>Samples and tutorials</h3>
      <ul class="list_nomargin">
        <li><a href="<?php echo $base . 'JPPF-' . $ver2 . '-samples-pack.zip/download'; ?>">JPPF samples pack</a></li>
        <li>Make sure to get started with our <a href="/doc/v5/index.php?title=A_first_taste_of_JPPF">online tutorial</a></li>
      </ul>
    </div>
    <br>
  </div>
  <br>
</div>
				</td>
				</tr>
			</table>
			<table border="0" cellspacing="0" cellpadding="0" width="100%" class="jppffooter">
      <tr><td colspan="*" style="height: 10px"></td></tr>
      <tr>
        <td align="center" style="font-size: 9pt; color: #6D78B6">
          <a href="/"><img src="/images/jppf_group_large.gif" border="0" alt="JPPF"/></a>
        </td>
        <td align="middle" valign="middle" style="font-size: 9pt; color: #6D78B6">Copyright &copy; 2005-2018 JPPF.org</td>
        <td align="middle" valign="center">
          <!-- Google+ button -->
          <!--
          <div class="g-plusone" data-href="http://www.jppf.org" data-annotation="bubble" data-size="small" data-width="300"></div>
          <script type="text/javascript">
            (function() {
              var po = document.createElement('script'); po.type = 'text/javascript'; po.async = true;
              po.src = 'https://apis.google.com/js/platform.js';
              var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(po, s);
            })();
          </script>
          -->
          <!-- Twitter share button -->
          <a href="https://twitter.com/share" class="twitter-share-button" data-url="https://www.jppf.org" data-via="jppfgrid" data-count="horizontal" data-dnt="true">Tweet</a>
          <script>!function(d,s,id){var js,fjs=d.getElementsByTagName(s)[0],p=/^http:/.test(d.location)?'http':'https';if(!d.getElementById(id)){js=d.createElement(s);js.id=id;js.src=p+'://platform.twitter.com/widgets.js';fjs.parentNode.insertBefore(js,fjs);}}(document, 'script', 'twitter-wjs');</script>
          <!-- Facebook Like button -->
          <iframe src="https://www.facebook.com/plugins/like.php?href=https%3A%2F%2Fwww.jppf.org&amp;layout=button_count&amp;show_faces=true&amp;width=40&amp;action=like&amp;colorscheme=light&amp;height=20" scrolling="no" frameborder="0"
            class="like" allowTransparency="true"></iframe>
        </td>
        <td align="right">
          <a href="https://sourceforge.net/projects/jppf-project">
            <img src="https://sflogo.sourceforge.net/sflogo.php?group_id=135654&type=10" width="80" height="15" border="0"
              alt="Get JPPF at SourceForge.net. Fast, secure and Free Open Source software downloads"/>
          </a>
        </td>
        <td style="width: 10px"></td>
      </tr>
      <tr><td colspan="*" style="height: 10px"></td></tr>
    </table>
  <!--</div>-->
  <div style="background-color: #E2E4F0">
    <div class="frame_bottom"/></div>
  </div>
		</div>
		</div>
	</body>
</html>
