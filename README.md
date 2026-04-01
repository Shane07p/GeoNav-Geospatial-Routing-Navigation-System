# GeoNav — Geospatial Routing & Navigation System

A console-based geospatial routing engine built in Java. The system models a city map and supports route planning using **Dijkstra's Algorithm** (shortest distance) and **A\* Search** (fastest time), powered by a **QuadTree** spatial index for efficient nearest-neighbor queries and a **POI system** for finding nearby places by category.

---

## Project Structure

```
final/
├── Main.java                        # Interactive console UI
├── model/
│   ├── Node.java                    # Geographic location (id, name, lat, lon)
│   ├── Edge.java                    # Road segment (distance, speed limit)
│   ├── Route.java                   # Query result (path, distance, time)
│   └── PointOfInterest.java         # POI (name, category, rating, coords)
├── graph/
│   └── Graph.java                   # Adjacency-list weighted graph
├── spatial/
│   ├── QuadTree.java                # Spatial index for nearest-neighbor queries
│   └── POIQuadTree.java             # K-nearest POI search (max-heap pruning)
├── strategy/
│   ├── RoutingStrategy.java         # Routing algorithm interface
│   ├── DijkstraStrategy.java        # Shortest-distance pathfinding
│   └── AStarStrategy.java           # Fastest-time pathfinding (heuristic)
└── engine/
    └── NavigationSystem.java        # Core engine coordinating graph, QuadTree & routing
```

---

## Features

| # | Feature | Description |
|---|---------|-------------|
| 1 | Show All Locations | Lists every node with coordinates |
| 2 | Find Route | Single source → destination route |
| 3 | Switch Strategy | Toggle between Dijkstra and A\* at runtime |
| 4 | Find Nearest Location | QuadTree-powered nearest-neighbor query |
| 5 | Add Location | Dynamically insert a new node |
| 6 | Add Road | Dynamically insert a bidirectional edge |
| 7 | Map Statistics | Node count, edge count, active strategy |
| 8 | Delete Location | Remove a node and all its connected roads |
| 9 | Multi-Stop Route | Chained waypoint routing (A → B → C → …) |
| 10 | Compare Routes | Side-by-side Dijkstra vs A\* comparison |
| 11 | Find Nearby Places | K-nearest POI search by category (Hotels, Restaurants, Malls, Theatres) with option to navigate |
| 12 | Add a Place | Dynamically add a new POI with category and rating |

---

## Time Complexity of Operations

Let **V** = number of nodes (vertices), **E** = number of edges (roads).

### Graph Operations

| Operation | Time Complexity | Explanation |
|-----------|:---------------:|-------------|
| Add Node | **O(1)** | HashMap insertion |
| Add Edge | **O(1)** | Appends to two adjacency lists |
| Remove Node | **O(V + E)** | Removes from map, then scans all adjacency lists to purge edges pointing to the deleted node |
| Get Neighbors | **O(1)** | Direct HashMap lookup |
| Get Node by ID | **O(1)** | Direct HashMap lookup |
| Edge Count | **O(V)** | Sums all adjacency list sizes |

### QuadTree Operations

| Operation | Average Case | Worst Case | Explanation |
|-----------|:------------:|:----------:|-------------|
| Insert | **O(log V)** | **O(V)** | Recursively subdivides; degrades if all points are co-located |
| Nearest Neighbor | **O(log V)** | **O(V)** | Branch-and-bound pruning skips irrelevant quadrants |
| K-Nearest (POI) | **O(K log N)** | **O(N)** | Max-heap of size K with branch-and-bound pruning per category |

### Routing Algorithms

| Algorithm | Time Complexity | Space Complexity | Explanation |
|-----------|:---------------:|:----------------:|-------------|
| Dijkstra | **O((V + E) log V)** | **O(V)** | Min-heap priority queue; each node extracted once, each edge relaxed once |
| A\* Search | **O((V + E) log V)** | **O(V)** | Same worst case as Dijkstra, but the heuristic prunes the search space — in practice explores fewer nodes |
| Multi-Stop Route | **O(k · (V + E) log V)** | **O(V)** | Chains *k − 1* individual route queries for *k* stops |
| Route Comparison | **O((V + E) log V)** | **O(V)** | Runs both algorithms sequentially |

### Dynamic Map Editing

| Operation | Time Complexity | Explanation |
|-----------|:---------------:|-------------|
| Add Location | **O(V log V)** | Inserts node, then rebuilds QuadTree |
| Remove Location | **O(V + E)** | Removes node and edges, then rebuilds QuadTree |
| Find Nearest | **O(log V)** | Delegates to QuadTree |
| Add POI | **O(N log N)** | Inserts POI, then rebuilds that category's POIQuadTree |
| Find K-Nearest POI | **O(K log N)** | Max-heap pruned search within one category tree |

---

## Mathematics Behind the A\* Heuristic

### Core Formula

A\* evaluates each node using:

```
f(n) = g(n) + h(n)
```

| Symbol | Meaning |
|--------|---------|
| `g(n)` | Actual travel time from the source to node *n* |
| `h(n)` | Estimated remaining travel time from *n* to the destination |
| `f(n)` | Estimated total cost of the cheapest path through *n* |

The priority queue always expands the node with the lowest `f(n)`, steering the search towards the goal.

### Heuristic Function

```
h(n) = haversine_distance(n, dest) / max_speed
```

- **haversine_distance** = great-circle (straight-line on Earth) distance between *n* and the destination
- **max_speed** = maximum speed limit on any road (60 km/h in our map)

This gives the *minimum possible travel time* by assuming a straight-line path at the fastest speed.

### The Haversine Formula

Calculates the great-circle distance between two points on Earth given their latitudes (φ) and longitudes (λ):

```
a = sin²(Δφ/2) + cos(φ₁) · cos(φ₂) · sin²(Δλ/2)
c = 2 · atan2(√a, √(1 − a))
d = R · c
```

| Variable | Meaning |
|----------|---------|
| φ₁, φ₂ | Latitudes of the two points (in radians) |
| Δφ | φ₂ − φ₁ (latitude difference) |
| Δλ | λ₂ − λ₁ (longitude difference) |
| R | Earth's mean radius = 6371 km |
| d | Great-circle distance in km |

### Why This Heuristic Is Admissible (Never Overestimates)

A heuristic is **admissible** if it never overestimates the true remaining cost. Ours satisfies this because:

1. **Haversine distance ≤ actual road distance** — the straight-line distance is always shorter than or equal to any path along roads
2. **Dividing by the maximum speed** — assumes the fastest possible travel at every point, so estimated time ≤ actual travel time

Since `h(n) ≤ h*(n)` (true optimal cost), A\* is guaranteed to find the optimal solution.

### Dijkstra vs A\*: When Do They Differ?

| Aspect | Dijkstra | A\* |
|--------|----------|-----|
| **Optimizes for** | Shortest distance | Fastest time |
| **Edge weight used** | `distance` (km) | `distance / speed` (hours) |
| **Heuristic** | None (h = 0) | Haversine / max speed |
| **Nodes explored** | All reachable within optimal distance | Fewer — heuristic prunes unpromising directions |

In our city map, roads have varying speed limits (12–100 km/h). Dijkstra may prefer a short alley (low distance, low speed), while A\* prefers a longer highway (high distance, high speed) because it minimizes *time*.

---

## POI System — K-Nearest Search

### How It Works

Each Point of Interest belongs to a **category** (HOTEL, RESTAURANT, MALL, THEATRE). POIs are indexed in **per-category POIQuadTrees** for efficient spatial queries.

### K-Nearest Algorithm (Max-Heap)

The search maintains a max-heap of size K (farthest of the K at the top):

```
findKNearest(lat, lon, k):
    maxHeap = PriorityQueue (farthest-first)

    for each POI p in this leaf:
        dist = euclidean(lat, lon, p.lat, p.lon)
        if heap.size < k:
            heap.add(p, dist)
        else if dist < heap.peek().dist:
            heap.poll()       // remove farthest
            heap.add(p, dist) // add closer one

    if divided:
        // PRUNE: skip quadrants whose closest point
        // is farther than current K-th best
        recurse into children
```

The pruning step is what makes this `O(K log N)` instead of `O(N)`.

### Distance Display

POI distances shown to the user use the **Haversine formula** (real km), while QuadTree search uses **Euclidean distance** on lat/lon (faster, same ordering for nearby points).

---

## How to Run

```bash
# Compile
javac -d out model\Node.java model\Edge.java model\Route.java model\PointOfInterest.java graph\Graph.java spatial\QuadTree.java spatial\POIQuadTree.java strategy\RoutingStrategy.java strategy\DijkstraStrategy.java strategy\AStarStrategy.java engine\NavigationSystem.java Main.java

# Run
java -cp out Main
```

---

## Demo City Map

The system comes preloaded with **14 locations**, **23 bidirectional roads**, and **33 points of interest** modelling a fictional city:

| Road Type | Speed Range | Example |
|-----------|:-----------:|---------|
| Alleys / Lanes | 12–20 km/h | Narrow Alley, Dirt Road |
| City Roads | 25–40 km/h | Boulevard, Ring Road |
| Highways | 50–60 km/h | Coastal Highway |
| Expressways | 70–100 km/h | Airport Expressway |

### Preloaded POIs

| Category | Count | Rating Range |
|----------|:-----:|:------------:|
| Hotels | 12 | 2.8 – 4.9 |
| Restaurants | 10 | 3.0 – 4.7 |
| Malls | 6 | 3.5 – 4.5 |
| Theatres | 5 | 3.6 – 4.7 |
