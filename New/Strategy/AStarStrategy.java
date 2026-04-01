package New.Strategy;

import New.graph.Graph;
import New.model.Node;
import New.model.Edge;
import New.model.Route;

import New.Strategy.RoutingStrategy;
import java.util.*;

/**
 * A* Search Algorithm — finds the FASTEST TIME path.
 *
 * Uses a min-priority-queue keyed on f(n) = g(n) + h(n), where:
 * - g(n) = actual travel time from source to n
 * - h(n) = heuristic estimate of remaining time (haversine distance / max
 * speed)
 *
 * The haversine heuristic is admissible (never overestimates), guaranteeing
 * optimality.
 */

public class AStarStrategy implements RoutingStrategy {

    private static final double MAX_SPEED_KMH = 60.0;
    
    @Override
    public Route findRoute(Graph graph, Node source, Node destination) {
        
    }

    @Override
    public String getStrategyName() {
        return "A* Search (Fastest Time)";
    }
}
