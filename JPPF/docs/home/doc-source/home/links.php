<?php
  require_once("db_settings.inc.php");
  $currentPage="Links";
?>
$template{name="about-page-header" title="Links"}$

		<h1 align="center">Related Links</h1>
<?php
    $link = mysql_connect($jppf_db_server, $jppf_db_user, $jppf_db_pwd) or die('Could not connect: ' . mysql_error());
    mysql_select_db($jppf_db_name) or die('Could not select database');

		$query = 'SELECT * FROM links_groups ORDER BY group_id ASC';
		$result = mysql_query($query) or die('Query failed: ' . mysql_error());
		$groups = array();
		while ($line = mysql_fetch_array($result, MYSQL_ASSOC))
		{
			$groups[$line["group_id"]] = $line["desc"];
		}
		mysql_free_result($result);
?>

    <div class="blockWithHighlightedTitle">
<?php
		$count = 0;
		foreach ($groups as $key => $value)
		{
?>
			<h2><?php printf('%s', $value); ?></h2>
<?php
			$query = "SELECT * FROM links WHERE group_id = '$key' ORDER BY link_id ASC";
			$result = mysql_query($query) or die('Query failed: ' . mysql_error());
			while ($line = mysql_fetch_array($result, MYSQL_ASSOC))
			{
?>
				<ul>
<?php
				printf("<li><div align='justify'><span class='linksub'><a href='%s'>%s</a>:</span> %s</div></li>", $line["url"], $line["title"], $line["desc"]);
?>
				</ul>
<?php
			}
?>
<?php
		}
		// Closing connection
		mysql_close($link);
?>
    <br></div>
$template{name="about-page-footer"}$
