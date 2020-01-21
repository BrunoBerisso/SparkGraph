package org.expero;

import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.spark.process.computer.SparkGraphComputer;
import org.apache.tinkerpop.gremlin.structure.util.GraphFactory;

public class SparkJanusGraph {

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            System.out.println("Please specify the location of the reader-graph.properties file");
            System.exit(1);
        }

        GraphTraversalSource g = GraphFactory.open(args[0])
                .traversal().withComputer(SparkGraphComputer.class);

        System.out.println(("Running query..."));
        Long value = g.V().count().next();
        System.out.println("Processed " + value + " vertex on a Spark job");

        System.exit(0);
    }
}
