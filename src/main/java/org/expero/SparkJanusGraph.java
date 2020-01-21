package org.expero;

import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.util.empty.EmptyGraph;

public class SparkJanusGraph {

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            System.out.println("Please specify the location of the remote-objects.yaml file");
            System.exit(1);
        }

        Cluster cluster = Cluster.open(args[0]);
        DriverRemoteConnection driver = DriverRemoteConnection.using(cluster, "a");
        GraphTraversalSource a = EmptyGraph.instance().traversal().withRemote(driver);

        Long value = a.V().count().next();
        System.out.println("Processed " + value + " vertex on a Spark job");
        System.exit(0);
    }
}
