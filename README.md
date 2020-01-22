# SparkGraph
The aim of this project is to show how to setup an OLAP job running on a Spark cluster consuming data from JanusGraph.

## Setup
For this test we need to install the following:
- JanusGraph v0.3.2 (which came with TinkerPop v3.3.3) available [here](https://github.com/JanusGraph/janusgraph/releases/tag/v0.3.2).
- Spark v2.2.0 that you can get on the Spark [download archives](https://archive.apache.org/dist/spark/spark-2.2.0/). We are going to need the [spark-2.2.0-bin-hadoop2.7.tgz](https://archive.apache.org/dist/spark/spark-2.2.0/spark-2.2.0-bin-hadoop2.7.tgz)

The installation consist only on extract the each file content, so go ahead and extract both some where.

#### Janus Graph
Now go into you JanusGraph installation and start the server with:
```shell script
$ ./bin/janushgraph.sh start
```
If everything goes fine you will see something like this on the terminal:
```shell script
Forking Cassandra...
Running `nodetool statusthrift`.. OK (returned exit status 0 and printed string "running").
Forking Elasticsearch...
Connecting to Elasticsearch (127.0.0.1:9200)................ OK (connected to 127.0.0.1:9200).
Forking Gremlin-Server...
Connecting to Gremlin-Server (127.0.0.1:8182)............ OK (connected to 127.0.0.1:8182).
Run gremlin.sh to connect.
```
The standard config shipped with Janus starts a server with one graph with his storage backed on Cassandra and his index backed on Elasticsearch. This is specified on the file `conf/gremlin-server/gremlin-server.yaml`.

To check that everything is in place we are going to load some data to our graph. To do so we are going to follow the standard example explained in the [JanusGraph docs](https://docs.janusgraph.org/getting-started/basic-usage/) but using different settings file. So first start a gremlin shell:
```shell script
$ ./bin/gremlin.sh
```
And then load the _"graph of the gods"_ dataset:
```groovy
gremlin> graph = JanusGraphFactory.open('conf/gremlin-server/janusgraph-cql-es-server.properties')
==>standardjanusgraph[cql:[127.0.0.1]]
gremlin> GraphOfTheGodsFactory.load(graph)
==>null
gremlin> graph.traversal().V()
13:48:23 WARN  org.janusgraph.graphdb.transaction.StandardJanusGraphTx  - Query requires iterating over all vertices [()]. For better performance, use indexes
==>v[4128]
==>v[4160]
==>v[4256]
==>v[8312]
==>v[12408]
==>v[4272]
==>v[8368]
==>v[4216]
==>v[8224]
==>v[8352]
==>v[4344]
==>v[16504
```
Now you have a working JanusGraph server storing data in Cassandra using CQL and indexing in Elasticsearch.

#### Spark
We are going to run a minimal Spark setup consisting in a master node and one worker running on the same machine. Before start running anything we need to extend the class path used by the Sparck driver and executor so they includes the JanusGraph artifacts. To do so we need to export `SPARK_DIST_CLASSPATH` to the Spark environment by editing the `spark-env.sh` file, so go inside the folder where you extracted the Spark _tgz_ and run:
```shell script
$ echo 'export SPARK_DIST_CLASSPATH="/path/to/your/janus/graph/installation/lib/*"' > conf/spark-env.sh
```
Now, there is a conflict with the version of guava shipped with Spark and the one needed by `spark-gremlin` artifact that provide us the [SparkGraphComputer](http://tinkerpop.apache.org/docs/3.3.3/reference/#sparkgraphcomputer) class we use [here](src/main/java/org/expero/SparkJanusGraph.java#20) which actually do the work of distribute the traversal across the Spark cluster.

To solve this problem we need to manually replace the guava jar on the Spark installation with the version we have installed as a dependency for this project (for this you might need to build this project first, check [the section below](#run-it)):
```shell script
$ cd /your/spark/installation
$ mv jars/guava-14.0.1.jar .  # backup the existing jar
$ cp ~/.m2/repository/com/google/guava/guava/18.0/guava-18.0.jar jars
```
Now we are ready to start the master and worker nodes. We need to first start the master node
```shell script
$ ./sbin/start-master.sh
```
Now you should be able to to go to [http://localhost:8080](http://localhost:8080) and see a dashboard. At the top of the screen is a file named _"URL"_ with the `spark://` protocol, that's the master URL.

We need to pass that URL to the worker node and later to our settings so we know where the Spark master is. Now we start one worker with:
```shell script
$ ./sbin/start-slave.sh spark://master-url:port
```

## Run it
With JanusGraph and Spark running we are ready to go. This project will fetch all the vertices on the graph, build a `List<Vertex>` and print out the result. What it expect as parameter are the settings to create an embedded graph that use the Cassandra instance where our data is. Lucky for us these settings are shipped with the JanusGraph installation under `conf/hadoop-graph/read-cassandra.properties`, the only thing we need to do update the Spark settings there so they point to the master node we just start. To do so edit the file and update the `spark.master` value so it looks like this:
```yaml
spark.master=spark://master-url:port
```

Get this repo in your local machine, open a terminal there and do the following:
```shell script
$ mvn clean package
$ mvn exec:java -Dexec.mainClass="org.expero.SparkJanusGraph" -Dexec.args="/path/to/your/janus/graph/installation/conf/hadoop-graph/read-cassandra.properties"
```
If everything were setup correctly you should get an output that contains the following (among other messages):
```shell script
...
Running query...
...
Processed 12 vertex on a Spark job
```

## About versions
`spark-gremlin` heavily relies on the internals of Spark so there is a tight relation between their versions. There is no detailed list of version compatibility, the more secure way to perform the check is navigate to the [pom.xml in the TinkerPop repo](https://github.com/apache/tinkerpop/blob/3.3.3/pom.xml) and check the Spark version set there. We can navigate the TinkerPop versions by switching the tags on the repo.
This is why we pick so specific versions of JanusGraph and Spark (because JanusGraph v0.3.2 depends on TinkerPop v0.3.3 which depends on Spark v0.2.2)

At the time of write this there is no version of TinkerPop that support Spark v0.2.3