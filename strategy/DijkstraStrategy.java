package strategy;

import graph.Graph;
import model.Edge;
import model.Node;
import model.Route;

import java.util.*;

/**
 * Dijkstra's Algorithm — finds the SHORTEST DISTANCE path.
 *
 * Uses a min-priority-queue keyed on cumulative distance.
 * Guarantees the optimal shortest-distance path in a non-negative weighted graph.
 */

public class DijkstraStrategy implements RoutingStrategy {

    @Override
    public String getStrategyName() {
        return "Dijkstra's Algorithm (Shortest Distance)";
    }

    @Override
    public Route findRoute(Graph graph, Node source, Node destination) {
       
        Map<String, Double> dist = new HashMap<>();

        Map<String, String> prev = new HashMap<>();
       
        Set<String> visited = new HashSet<>();

        PriorityQueue<NodeEntry> queue = new PriorityQueue<>();

     
        for (Node node : graph.getAllNodes()) {
            dist.put(node.getId(), Double.MAX_VALUE);
        }
        dist.put(source.getId(), 0.0);
        queue.add(new NodeEntry(source.getId(), 0.0));

        while (!queue.isEmpty()) {
            NodeEntry current = queue.poll();
            String currentId = current.nodeId;

            if (visited.contains(currentId))
                continue;
            visited.add(currentId);

            if (currentId.equals(destination.getId()))
                break;

            for (Edge edge : graph.getNeighbors(currentId)) {
                String neighborId = edge.getDestination().getId();
                if (visited.contains(neighborId))
                    continue;

                double newDist = dist.get(currentId) + edge.getDistance();
                if (newDist < dist.get(neighborId)) {
                    dist.put(neighborId, newDist);
                    prev.put(neighborId, currentId);
                    queue.add(new NodeEntry(neighborId, newDist));
                }
            }
        }

        return reconstructRoute(graph, source, destination, dist, prev);
    }

    protected Route reconstructRoute(Graph graph, Node source, Node destination, Map<String, Double> dist, Map<String, String> prev) {
        if (dist.get(destination.getId()) == Double.MAX_VALUE) {
            return null; 
        }

        List<Node> path = new ArrayList<>();
        String current = destination.getId();

        while (current != null) {
            path.add(graph.getNode(current));
            current = prev.get(current);
        }
        
        Collections.reverse(path);

        double totalTime = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            for (Edge edge : graph.getNeighbors(path.get(i).getId())) {
                if (edge.getDestination().getId().equals(path.get(i + 1).getId())) {
                    totalTime += edge.getTravelTime();
                    break;
                }
            }
        }

        return new Route(path, dist.get(destination.getId()), totalTime);
    }

    protected static class NodeEntry implements Comparable<NodeEntry> {
        String nodeId;
        double priority;

        NodeEntry(String nodeId, double priority) {
            this.nodeId = nodeId;
            this.priority = priority;
        }

        @Override
        public int compareTo(NodeEntry other) {
            return Double.compare(this.priority, other.priority);
        }
    }
}
