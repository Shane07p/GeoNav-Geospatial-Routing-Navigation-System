package New.Strategy;

import New.graph.Graph;
import New.model.Node;
import New.model.Route;

/**
 * Strategy Pattern interface for routing algorithms.
 * Allows swapping between different pathfinding algorithms at runtime.
 */

public interface RoutingStrategy {

    /**
     * Find a route between two nodes in the graph.
     *
     * @param graph       the map graph
     * @param source      starting node
     * @param destination target node
     * @return a Route object, or null if no path exists
     */

    Route findRoute(Graph graph, Node source, Node destination);

    // Current Strategy of the routing algorithm
    String getStrategyName();
}
