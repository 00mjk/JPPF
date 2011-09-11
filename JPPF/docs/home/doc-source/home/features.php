<?php $currentPage="Features" ?>
$template{name="about-page-header" title="Features"}$

<h1>JPPF 2.5 features</h1>

<h3>Ease of use</h3>
<ul>
	<li>simple APIs requiring small or no learning curve</li>
	<li>automatic deployment of application code on the grid</li>
	<li>ability to reuse existing or legacy objects without modification</li>
	<li>"happy path" with no additional configuration</li>
	<li>automatic server discovery</li>
	<li>convenient reusable application template to quickly and easily start developing JPPF applications</li>
	<li>straightforward Executor Service interface to the JPPF grid, with high throughput enhancements</li>
</ul>

<h3>Self-repair and recovery</h3>
<ul>
	<li>automated node reconnection with failover strategy</li>
	<li>automated client reconnection with failover strategy</li>
	<li>fault tolerance with job requeuing</li>
	<li>detection, recovery from hard-disconnects of remote nodes</li>
</ul>

<h3>Job-level SLA</h3>
<ul>
	<li>job execution policies enable rule-based node filtering</li>
	<li>maximum number of nodes a job can run on (grid partitioning)</li>
	<li>job prioritization</li>
	<li>job scheduled start date</li>
	<li>job scheduled expiration date</li>
	<li>broadcast jobs</li>
</ul>

<h3>Management and monitoring</h3>
<ul>
	<li>task-level events</li>
	<li>job-level events</li>
	<li>server performance statistics</li>
	<li>server performance charts</li>
	<li>user-defined charts</li>
	<li>remote server control and monitoring</li>
	<li>remote nodes control and monitoring</li>
	<li>cpu utilization monitoring</li>
	<li>management of load-balancing</li>
	<li>management and monitoring available via APIs and graphical user interface (administration console)</li>
	<li>access to remote servers and nodes logs via the JMX-based logger (integrates with Log4j and JDK logging)</li>
</ul>

<h3>Platform extensibility</h3>
<ul>
	<li>All management beans are pluggable, users can add their own management modules at server or node level</li>
	<li>Startup classes: users can add their own initialization modules at server and node startup</li>
	<li>Security: any data transiting over the network can now be encrypted by the way of user-defined transformations</li>
	<li>Pluggable load-balancing modules allow users to write their own load balancing strategies</li>
	<li>Ability to specify alternative serialization schemes</li>
	<li>Subscription to nodes life cycle events</li>
</ul>

<h3>Performance and resources efficiency</h3>
<ul>
	<li>multiple configurable load-balancing algorithms</li>
	<li>adaptive load-balancing adjusts in real-time to workload changes</li>
	<li>memory-aware components with disk overflow</li>
	<li>client-side server connection pools</li>
</ul>

<h3>Dynamic topology scaling</h3>
<ul>
	<li>nodes can be added and removed dynamically from the grid</li>
	<li>servers can be added and removed dynamically from the grid</li>
	<li>servers can work alone or linked in P2P topology with other servers</li>
	<li>ability to run a node in the same JVM as the server</li>
</ul>

<h3>Third-party connectors</h3>
<ul>
	<li>J2EE connector, JCA 1.5 compliant, deployed as a standard resource adapter</li>
	<li>GigaSpaces XAP connector</li>
	<li>Apache Tomcat connector</li>
</ul>

<h3>Ease of integration</h3>
<ul>
	<li><a href="samples-pack/FTPServer/Readme.php">Apache FTP server</a></li>
	<li><a href="samples-pack/DataDependency/Readme.php">Hazelcast data grid</a></li>
	<li><a href="samples-pack/NodeLifeCycle/Readme.php">Atomikos transaction manager and JDBC database</a></li>
</ul>

<h3>Add-ons</h3>
<ul>
	<li>TCP multiplexer, routes JPPF traffic through a single TCP port to work with firewalled environments</li>
</ul>

<h3>Deployment modes</h3>
<ul>
	<li>all components deployable as standalone Java applications</li>
	<li>servers and nodes deployable as Linux/Unix daemons</li>
	<li>servers and nodes deployable as Windows services</li>
	<li>application client deployment as a Web, J2EE or GigaSpaces XAP application
	<li>nodes can run in idle system mode (CPU scavenging)</li>
</ul>

<h3>Execution modes</h3>
<ul>
	<li>synchronous and asynchronous job submissions</li>
	<li>client can execute in local mode (benefits to systems with many CPUs)</li>
	<li>client can execute in distributed mode (execution delegated to remote nodes)</li>
	<li>client can execute in mixed local/distributed mode with adaptive load-balancing</li>
</ul>


<h3>Full fledged samples</h3>
<ul>
	<li><a href="samples-pack/Fractals/Readme.php">Mandelbrot / Julia set fractals generation</a></li>
	<li><a href="samples-pack/SequenceAlignment/Readme.php">Protein and DNA sequence alignment</a></li>
	<li><a href="samples-pack/WebSearchEngine/Readme.php">Distributed web crawler and search engine</a></li>
	<li><a href="samples-pack/TomcatPort/Readme.php">Tomcat 5.5/6.0 port</a></li>
	<li><a href="samples-pack/CustomMBeans/Readme.php">Pluggable management beans sample</a></li>
	<li><a href="samples-pack/DataEncryption/Readme.php">Network data encryption sample</a></li>
	<li><a href="samples-pack/StartupClasses/Readme.php">Customized server and node initialization sample</a></li>
	<li><a href="samples-pack/MatrixMultiplication/Readme.php">Basic dense matrix multiplication parallelization sample</a></li>
	<li><a href="samples-pack/DataDependency/Readme.php">Simulation of large portfolio updates</a></li>
	<li><a href="samples-pack/NodeTray/Readme.php">JPPF node health monitor in the system tray</a></li>
	<li><a href="samples-pack/CustomLoadBalancer/Readme.php">An example of a sophisticated load-balancer implementation</a></li>
	<li><a href="samples-pack/TaskNotifications/Readme.php">A customization that allows tasks to send notifications while executing</a></li>
	<li><a href="samples-pack/IdleSystem/Readme.php">An extension that enables nodes to run only when the machine is idle</a></li>
	<li><a href="samples-pack/NodeLifeCycle/Readme.php">Control of database transactions via node life cycle events</a></li>
	<li><a href="samples-pack/Nbody/Readme.php">Parallel N-body problem applied to anti-protons trapped in a  magnetic field</a></li>
	<li><a href="samples-pack/FTPServer/Readme.php">How to embed and use an FTP server in JPPF</a></li>
</ul>

$template{name="about-page-footer"}$
