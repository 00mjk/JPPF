# Android demo

<h3>What does the sample do?</h3>
<p>This sample demonstrates how a Java task can be executed on an Android node.

<h3>How do I run it?</h3>
<ol class="samplesList">
  <li>Install a JPPF server and start it. For information on how to set up a server, please refer to the <a href="https://www.jppf.org/doc/6.0/index.php?title=Introduction">JPPF documentation</a>.</li>
  <li>Install a JPPF node on a physical or virtual Android device. For instructions on how to do this, please refer to this documentation section: TBD</li>
  <li>open a command prompt in JPPF-x.y-samples-pack/AndroidDemo</li>
  <li>build the sample: type "<b>ant dex.jar</b>"; this will create a file named <b>dex-demo.jar</b>, intended to be dynamically loaded by the Android node</li>
  <li>Run the demo: in the same commond prompt type "<b>./run.sh</b>" on Linux/Unix or "<b>run.bat</b>" on Windows</li>
</ol>

<h3>Source files</h3>
<ul class="samplesList">
  <li><a href="build.xml">build.xml</a>: this is the build script, where the Java jar is converted to dex format</li>
  <li><a href="src/org/jppf/example/android/demo/Runner.java">Runner.java</a>: The entry point for the demo, builds a JPPF job for execution on Android and submits it to the grid</li>
  <li><a href="src/org/jppf/example/android/demo/DemoAndroidTask.java">DemoAndroidTask.java</a>: a simple task example, excuted on an Android node</li>
</ul>

<h3>I have additional questions and comments, where can I go?</h3>
<p>If you need more insight into the code of this demo, you can consult the Java source files located in the <b>AndroidDemo/src</b> folder.
<p>In addition, There are 2 privileged places you can go to:
<ul>
  <li><a href="https://www.jppf.org/forums"/>The JPPF Forums</a></li>
  <li><a href="https://www.jppf.org/doc/6.0/">The JPPF documentation</a></li>
</ul>

