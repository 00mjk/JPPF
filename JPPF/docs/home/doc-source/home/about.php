<?php $currentPage="About" ?>
$template{name="about-page-header" title="About JPPF"}$
	<div align="justify">
		<h3>What it is</h3>
		Simply put, JPPF enables applications with large processing power requirements to be run on any number of computers, in order to dramatically reduce their processing time.
		This is done by splitting an application into smaller parts that can be executed simultaneously on different machines.

		<h3>How it works</h3>
		<p>There are 2 aspects to this:
		<p><i>Dividing an application into smaller parts that can be executed independently and in parallel.</i>
		<br>JPPF provides facilities that make this effort a lot easier, faster and much less painful than without them.
		The result is a JPPF object called a "job", itself made of smaller independant parts called "tasks".
		<p><i>Executing the application on the JPPF Grid.</i>
		<br>A JPPF Grid is made of a server, to which any number of execution nodes are attached. A node is a JPPF software component that is generally installed and running on a separate machine.
		This is commonly called a master/slave architecture, where the work is distributed by the server (aka "master") to the nodes (aka "slaves").
		In JPPF terms, a unit of work is called a "job", and its constituting "tasks" are distributed by the server among the nodes for parallel execution.

		<h3>Advantages</h3>
		<p>Chief among JPPF benefits is its ease of installation, use and deployment. There is no need to spend days to write a "Hello World" application. A couple of minutes, up to a couple of hours at most, will suffice.
		Deploying JPPF components over a cluster is as simple as copying files over FTP or any network file system.
		JPPF allows developers to focus on their core software development, instead of wasting time on the complexities of parallel and distributed processing.
	`	<p>As a 100% Java framework, JPPF will run on any system that supports Java: MacOS, Windows, Linux, zOS, on any hardware from a simple laptop up to a mainframe computer.
		This does not mean that JPPF is limited to running Java jobs. You can run any application that is available on your platform as a JPPF job.
		For instance, you might want to run your favorite graphics suite in batch mode, to render multiple large, complex images all at once.
		<p>Another benefit of JPPF is a simplified, almost immediate, deployment process of your application on the grid.
		Even though your aplication will be run on many nodes at once, you only need to deploy it in a single location.
		By extending the Java class loading mechanism, JPPF removes most of the deployment burden from the application's life cycle, dramatically shortening the time-to-market and time-to-production.

		<h3>Outstanding features</h3>
		<p>There is a lot more to JPPF than running and deploying your applications on the grid.
		The features are so numerous that we have dedicated a separate page to enumerate them fully.
		For a complete list of what JPPF has to offer, please consult our <a href="features.php">full features list</a>.
		<p><b>Comprehensive, easy to use APIs.</b><br>
		Passing from a single-threaded application model to a grid-based parallel model can be a daunting task.
		JPPF facilitates this work by providing developers with a set of APIs that are simple, can be learned quickly and require a minimal or no modification of the existing code.
		<p><b>No configuration usage.</b><br>
		In most environments, JPPF can be deployed without any additional configuration burden. Nodes and application clients will automatically dicover the servers on the network.
		The server will automatically adapt to workload changes and optimize the throughput. Required code and libraries will be automatically deployed where they are needed.
		<p><b>Dynamic grid scaling and self-repair.</b><br>
		The JPPF grid is fault-tolerant, meaning that the failure of a node, or even a server, does not compromise the jobs currently executing or scheduled.
		In most cases, the performance degradation will be barely noticeable, as JPPF automatically adapts to topology and workload changes.
		Furthermore, nodes and servers can be dynamically started and will be automatically recognized, allowing JPPF to function in "crunch mode".
		In addition to this, lll JPPF components benefit from automatic recovery functionalities.
		<p><b>Job-level SLA.</b><br>
		Each job submitted to the JPPF grid runs within limits defined by its own SLA (service level agreement).
		This allows to specify the characteristics (i.e. available memory, processors, disk space, operating systems, etc.) of the nodes a job can run on, as well as how many nodes it can run on.
		As many functionalities in JPPF, this one can be dynamically adjusted, manually or automatically.
		<p><b>Management and monitoring.</b><br>
		Full-fledged management and monitoring features are provided out of the box: server and nodes status monitoring, detailed statistics and events, remote administration, job-level real-time monitoring and management, charts, cpu utilization (for billing).
		These functionalities are available via a graphical user interface as well as from the JPPF APIs.
		<p><b>Integration with leading application and web servers.</b><br>
		By complying with the Java Connector Architecture 1.5 specification, JPPF integrates seamlessly with and completes the offering of leading J2EE application servers:
		JBoss<sup>&reg;</sup>, Glassfish<sup>&reg;</sup>, IBM Websphere<sup>&reg;</sup>, Oracle Weblogic<sup>&reg;</sup>, Oracle OC4J<sup>&reg;</sup>.
		JPPF also integrates with GigaSpaces eXtreme Application Platform<sup>&reg;</sup> and Apache Tomcat web server<sup>&reg;</sup>
	</div>
	<br>
$template{name="about-page-footer"}$
