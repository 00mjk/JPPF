<?php $currentPage="Home" ?>
$template{name="about-page-header" title="Home"}$
	<table border="0" cellpadding="0" cellspacing="0" style="vertical-align: middle; height: 250px; width: 750px; background-image: url('/images/test1.gif'); background-repeat: no-repeat; background-attachment: scroll">
	<tr><td align="center" valign="center"><img src="/images/Animation1.gif" border="0" alt="JPPF" style="background: transparent"/></td></tr>
	</table>
	<div style="margin: 15px; ">
	<p style="text-align: justify; font-size: 12pt">JPPF enables applications with large processing power requirements to be run on any number of computers, in order to dramatically reduce their processing time.
	This is done by splitting an application into smaller parts that can be executed simultaneously on different machines.
	<p style="text-align: justify; font-size: 12pt">Start easily with our <a href="/wiki/index.php?title=A_first_taste_of_JPPF">JPPF Tutorial</a>.
	</div>

	<div class="column1">
		<?php
			$link = mysql_connect('localhost', 'pervasiv_jppfadm', 'tri75den')
				 or die('Could not connect: ' . mysql_error());
			mysql_select_db('pervasiv_jppfweb') or die('Could not select database');
			$query = 'SELECT * FROM news ORDER BY date DESC';
			$result = mysql_query($query) or die('Query failed: ' . mysql_error());
		?>
		<h3>Latest Releases</h3>
		<?php
			for ($i=1; $i<=3; $i++)
			{
				$line = mysql_fetch_array($result, MYSQL_ASSOC);
				printf("<a href='news.php#news%d' style='font-size: 10pt'><span style='white-space: nowrap'>%s %s</span></a><br>", $i, date("n/j/Y", strtotime($line["date"])), $line["title"]);
			}
			mysql_free_result($result);
			mysql_close($link);
		?>

		<div align="left">
			<br><b>Feeds: </b>
			<a href="http://sourceforge.net/export/projnews.php?group_id=135654&limit=10&flat=1&show_summaries=1"><img src="images/feed-16x16.gif" border="0"/></a>
			<a href="http://sourceforge.net/export/projnews.php?group_id=135654&limit=10&flat=1&show_summaries=1">News</a>
			&nbsp;<a href="http://sourceforge.net/export/rss2_projnews.php?group_id=135654&rss_fulltext=1"><img src="images/feed-16x16.gif" border="0"/></a>
			<a href="http://sourceforge.net/export/rss2_projnews.php?group_id=135654&rss_fulltext=1">Releases</a>
			&nbsp;&nbsp;<a href="/news.php"><b style="color: #6D78B6">All News</b></a>
		</div>
		<br>

		<h3>Our users say</h3>
		<a href="quotes.php" style="text-decoration: none">... we have found the framework to be extremely powerful and easy to work with...</a>
		<p><a href="quotes.php" style="text-decoration: none">... The ability to adapt our existing technology without having to redesign or rethink entire processes is fantastic ...</a>
		<br>

		<!--
		<h3>Licensing & Status</h3>
		JPPF is licensed under the <a href="/license.php"><b>Apache License, Version 2.0</b></a>
		<br>Latest stable version: Version 1.9.1
		-->
	</div>

	<div class="column2">

		<h3>Easy and powerful</h3>
		&bull;&nbsp;a JPPF grid can be up and running in minutes<br>
		&bull;&nbsp;dynamically scalable on-demand<br>
		&bull;&nbsp;connectors with leading J2EE application servers<br>
		&bull;&nbsp;connector with GigaSpaces eXtreme Application Platform<br>
		&bull;&nbsp;easy programming model<br>
		&bull;&nbsp;fine-grained monitoring and administration<br>
		&bull;&nbsp;fault-tolerance and self-repair capabilities<br>
		&bull;&nbsp;exceptional level of service and reliability<br>
		&bull;&nbsp;fully documented samples, using JPPF on real-life problems<br>
		&bull;&nbsp;flexible open-source licensing with Apache 2.0

		<h3>Contribute</h3>
		<b>Browse our <a href="./wiki" target=_top>documentation</a></b><br>
		<b>Find support, share your ideas, in our <a href="./forums" target=_top>discussion forums</a></b><br>
		<b>Browse and contribute to our <a href="http://sourceforge.net/tracker/?atid=733518&group_id=135654&func=browse" target=_top>bugs database</a></b><br>
		<b>Browse and contribute to our <a href="http://sourceforge.net/tracker/?atid=733521&group_id=135654&func=browse" target=_top>feature requests database</a></b>

	</div>

$template{name="about-page-footer"}$