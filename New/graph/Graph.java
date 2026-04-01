package New.graph;

import New.model.Edge;
import New.model.Node;

import java.util.*;

/**
 * Adjacency-list based weighted graph.
 * Supports bidirectional edges (roads you can travel both ways).
 */

public class Graph {
    private final Map<String, Node> nodes; // id -> Node;
    private final Map<String, List<Edge>> adjacency; // NodeId -> outgoint edges;

    public Graph() {
        this.nodes = new LinkedHashMap<>();
        this.adjacency = new LinkedHashMap<>();
    }

    // mutators
    public void addNode(Node node) {
        nodes.put(node.getId(), node);
        adjacency.putIfAbsent(node.getId(), new ArrayList<>());
    }
    
    // getters 

    public void addEdge(Node source, Node destination, double distance, double speedLimit) {
        // bidirectional edge
        Edge forward = new Edge(source, destination, distance, speedLimit);
        Edge backward = new Edge(destination, source, distance, speedLimit);

        adjacency.get(source.getId()).add(forward);
        adjacency.get(destination.getId()).add(backward);
    }

    public List<Edge> getNeighbours(String nodeId) {
        return adjacency.getOrDefault(nodeId, Collections.emptyList()); // return empty list to not get NullPointerException
    }

    public Node getNode(String id) {
        return nodes.get(id);
    }

    public Collection<Node> getAllNode() {
        return nodes.values();
    }

    public int getNodeCount() {
        return nodes.size();
    } 
    
    public int getEdgeCount() {
        int res = 0;

        for(List<Edge> edges : adjacency.values()) {
            res += edges.size();
        }

        return res / 2;
    }
}
