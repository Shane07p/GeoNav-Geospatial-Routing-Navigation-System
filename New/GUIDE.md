# 🧭 GeoNav — Learning Guide

> **Geospatial Routing & Navigation Engine**
> Demo City • 14 locations • 23 roads • 5 drivers

---

## 📂 Project Structure

```
final/
├── Main.java                      ← Console UI + demo city map
├── New/                           ← YOU ARE HERE (model & graph basics)
│   ├── model/
│   │   ├── Node.java              ← A location on the map
│   │   ├── Edge.java              ← A road between two locations
│   │   └── Route.java             ← A complete path (result of routing)
│   └── graph/
│       └── Graph.java             ← Adjacency-list graph (stores the map)
├── model/
│   └── Driver.java                ← A ride-share driver with GPS position
├── strategy/
│   ├── RoutingStrategy.java       ← Interface (Strategy Pattern)
│   ├── DijkstraStrategy.java      ← Shortest-distance algorithm
│   └── AStarStrategy.java         ← Fastest-time algorithm (uses heuristic)
├── spatial/
│   ├── QuadTree.java              ← Spatial index for locations
│   └── DriverQuadTree.java        ← Spatial index for drivers
└── engine/
    └── NavigationSystem.java      ← Orchestrator (ties everything together)
```

---

## 🧱 Part 1 — The Data Model (`New/model/`)

These are the **building blocks** — simple classes that represent real-world data.

### 1.1 `Node.java` — A Map Location

```java
public class Node {
    private final String id;        // unique short code, e.g. "CTR"
    private final String name;      // display name, e.g. "Central Plaza"
    private final double latitude;
    private final double longitude;
}
```

**OOP concepts learned:**
| Concept | How it's used |
|---|---|
| **Encapsulation** | All fields are `private final` — only accessible via getters |
| **Immutability** | Once created, a Node can never change (all fields are `final`) |
| **`equals()` / `hashCode()`** | Overridden to compare by `id` — two Nodes with the same ID are "equal" even if other fields differ |
| **`Objects.equals()` / `Objects.hash()`** | Null-safe utility from `java.util.Objects` |

**Key takeaway:** Node uses **value-based equality** — identity comes from the `id` field, not memory address.

---

### 1.2 `Edge.java` — A Road

```java
public class Edge {
    private final Node source;       // where the road starts
    private final Node destination;  // where the road ends
    private final double distance;   // km
    private final double speedLimit; // km/h
}
```

**OOP concepts learned:**
| Concept | How it's used |
|---|---|
| **Composition** | An Edge *contains* two Node references (has-a relationship) |
| **Derived property** | `getTravelTime()` returns `distance / speedLimit` — calculated, not stored |
| **Encapsulation** | Internal data is hidden; you can only read it via getters |

**Key takeaway:** Speed limits create interesting routing tradeoffs — a short alley (1.2 km at 15 km/h) takes longer than a highway (15 km at 60 km/h). This is why Dijkstra and A\* can give **different paths**.

---

### 1.3 `Route.java` — A Path Result

```java
public class Route {
    private final List<Node> nodes;         // ordered path: [A, B, C, D]
    private final double totalDistance;      // sum of edge distances (km)
    private final double totalTime;          // sum of edge travel times (hrs)
}
```

**OOP concepts learned:**
| Concept | How it's used |
|---|---|
| **Immutability** | `Collections.unmodifiableList()` wraps the node list — callers can't modify the route |
| **Defensive copying** | Protects internal state from external modification |
| **`toString()`** | Generates a formatted box with path, distance, and time for display |

**Key takeaway:** `Collections.unmodifiableList()` is a common Java pattern for making "read-only views" of collections.

---

## 🗺️ Part 2 — The Graph (`New/graph/`)

### 2.1 `Graph.java` — Adjacency List

```java
public class Graph {
    private final Map<String, Node>       nodes;     // id → Node
    private final Map<String, List<Edge>> adjacency; // id → outgoing edges
}
```

**How edges work:**
- `addEdge(A, B, dist, speed)` creates **two** Edge objects (A→B and B→A) — all roads are **bidirectional**
- Stored in a `LinkedHashMap` to preserve insertion order (so locations list in the same order you add them)

**OOP concepts learned:**
| Concept | How it's used |
|---|---|
| **Composition** | Graph *owns* Nodes and Edges |
| **Encapsulation** | Internal maps are private; access only through methods |
| **`LinkedHashMap`** | Preserves order (unlike regular `HashMap`) |
| **`Collections.emptyList()`** | Returns an empty list for unknown node IDs instead of `null` — avoids `NullPointerException` |

**Key takeaway:** The adjacency list is the core data structure. Everything else (routing, spatial queries) operates on this graph.

---

## ⚡ Part 3 — Routing Algorithms (`strategy/`)

This package demonstrates the **Strategy Pattern** — one of the most important OOP design patterns.

### 3.1 `RoutingStrategy.java` — The Interface

```java
public interface RoutingStrategy {
    Route findRoute(Graph graph, Node source, Node destination);
    String getStrategyName();
}
```

**Why an interface?** So we can swap algorithms at runtime:
```java
nav.setStrategy(new DijkstraStrategy());   // switch to shortest distance
nav.setStrategy(new AStarStrategy());      // switch to fastest time
```

### 3.2 `DijkstraStrategy.java` — Shortest Distance

- Optimizes for **shortest total distance** (km)
- Uses a **min-priority-queue** keyed on cumulative distance
- Guarantees the optimal path in non-negative weighted graphs

**Algorithm summary:**
1. Start at source with distance 0
2. Pick the unvisited node with smallest distance
3. Update distances to all its neighbours
4. Repeat until destination is reached

### 3.3 `AStarStrategy.java` — Fastest Time

- Optimizes for **shortest travel time** (hours)
- Uses `f(n) = g(n) + h(n)` where:
  - `g(n)` = actual time from source to n
  - `h(n)` = **heuristic** estimate of remaining time
- Heuristic: `haversine_distance / max_speed` (never overestimates → **admissible**)

**OOP concepts in this package:**
| Concept | How it's used |
|---|---|
| **Abstraction** | `RoutingStrategy` defines *what* to do, not *how* |
| **Polymorphism** | Both strategies implement the same interface, but behave differently |
| **Strategy Pattern** | Algorithms are interchangeable at runtime |
| **Inner class** | `NodeEntry` is a private helper for the priority queue |

---

## 🌲 Part 4 — Spatial Indexing (`spatial/`)

### 4.1 `QuadTree.java` — Find Nearest Location

A **QuadTree** recursively divides a 2D area into 4 quadrants (NW, NE, SW, SE) for fast nearest-neighbor search.

```
┌───────────┬───────────┐
│           │           │
│    NW     │    NE     │
│           │           │
├───────────┼───────────┤
│           │           │
│    SW     │    SE     │
│           │           │
└───────────┴───────────┘
```

**Complexity:**
| Operation | Brute Force | QuadTree |
|---|---|---|
| Find nearest | O(n) | O(log n) avg |
| Insert | O(1) | O(log n) avg |

### 4.2 `DriverQuadTree.java` — Find Nearest Driver(s)

Same concept as `QuadTree`, but stores `Driver` objects and supports **K-nearest** queries.

**OOP concepts:**
| Concept | How it's used |
|---|---|
| **Recursion** | Tree subdivides itself recursively |
| **Branch-and-bound pruning** | Skips quadrants that can't have closer points |
| **Inner class** | `NearestResult` is a mutable container for search results |

---

## 🚗 Part 5 — Driver & Ride-Sharing (`model/Driver.java`)

```java
public class Driver {
    private final String id;
    private final String name;
    private double latitude;    // NOT final — drivers can move!
    private double longitude;
}
```

**Key difference from Node:** Driver position is **mutable** (`updatePosition()` method).

---

## 🔧 Part 6 — The Engine (`engine/NavigationSystem.java`)

The **orchestrator** that connects everything:

```
NavigationSystem
├── Graph           (the map)
├── QuadTree        (spatial index for locations)
├── DriverQuadTree  (spatial index for drivers)
├── RoutingStrategy (current algorithm — swappable)
└── Drivers         (Map of registered drivers)
```

**Features:**
| Feature | Method | Description |
|---|---|---|
| Single route | `findRoute(src, dest)` | A→B using current strategy |
| Multi-stop | `findMultiStopRoute(stops)` | A→B→C→D chaining legs |
| Compare | `compareRoutes(src, dest)` | Run Dijkstra AND A\* side-by-side |
| Nearest driver | `findNearestDriverETA(nodeId)` | Find closest driver + ETA |
| Add location | `addLocation(node)` | Dynamically add to map |
| Add road | `addRoad(src, dest, dist, speed)` | Dynamically connect locations |

**OOP concepts:**
| Concept | How it's used |
|---|---|
| **Composition** | NavigationSystem *has* a Graph, QuadTree, etc. |
| **Delegation** | Routing is delegated to the current `RoutingStrategy` |
| **Strategy Pattern** | Algorithm is swappable via `setStrategy()` |
| **Static inner class** | `ComparisonResult` and `DriverETAResult` bundle return data |

---

## 🏙️ Part 7 — The Demo City Map

The city has **14 locations** connected by **23 roads** with varying speeds:

```
      Airport (AIR)
         │ 30km @100km/h (expressway!)
     North Gate (NTH)
      ╱       ╲
Old Town ── Castle
    │
  Station (STN) ── Central (CTR) ── Garden (GDN)
                       │
                    Market (MKT) ──────── Hillview (HLV)
                       │           15km @60km/h highway
                   Tech Hub (THB)
                    ╱       ╲
             University    Lakeside (LKS)
                              │
                         Ind. Park ── Port Area (PRT)
```

**Road types (speed matters!):**
| Type | Speed | Example |
|---|---|---|
| Narrow alley | 12–15 km/h | Garden→Station (1.2 km) |
| City road | 25–35 km/h | Central→Garden (1.5 km) |
| Highway | 55–60 km/h | Market→Hillview (15 km) |
| Expressway | 80–100 km/h | North Gate→Airport (30 km) |

**Why this matters:** Dijkstra picks short alleys (less distance), while A\* picks highways (less time). Try comparing routes to see the difference!

---

## 🎮 Running the Application

```bash
# From the 'final' directory:
javac -d out Main.java
java -cp out Main
```

**Menu options:**
| # | Feature |
|---|---|
| 1 | View all 14 locations |
| 2 | Find route between two locations |
| 3 | Switch algorithm (Dijkstra ↔ A\*) |
| 4 | Find nearest location to GPS coordinates |
| 5 | Add a new location |
| 6 | Add a new road |
| 7 | View map stats |
| 9 | Multi-stop routing (waypoints) |
| 10 | Compare Dijkstra vs A\* side-by-side |
| 11 | Find nearest driver + ETA |
| 12 | Add a new driver |

---

## 📊 OOP Concepts Summary

| Concept | Where to find it |
|---|---|
| **Encapsulation** | All model classes (private fields + getters) |
| **Immutability** | `Node`, `Edge`, `Route` (all fields `final`) |
| **Composition** | Edge has Nodes, Graph has Nodes+Edges, NavigationSystem has everything |
| **Abstraction** | `RoutingStrategy` interface |
| **Polymorphism** | Dijkstra and A\* both implement `RoutingStrategy` |
| **Strategy Pattern** | Swap routing algorithm at runtime |
| **Inner Classes** | `NodeEntry`, `NearestResult`, `ComparisonResult`, `DriverETAResult` |
| **Collections API** | `HashMap`, `LinkedHashMap`, `ArrayList`, `PriorityQueue`, `Collections.unmodifiableList()` |
| **`equals` / `hashCode`** | `Node.java`, `Driver.java` |
| **Defensive Copying** | `Route` constructor wraps list in `unmodifiableList` |

---

## 🧪 Try These Experiments

1. **Compare algorithms:** Use option 9 to route from `STN` (Grand Station) to `PRT` (Port Area) — watch Dijkstra and A\* pick different paths!

2. **Multi-stop trip:** Use option 8 to plan: `CTR, MKT, AIR` (city center to market to airport)

3. **Find nearest driver:** Use option 11 at pickup point `CTR` — see which of the 5 drivers is closest

4. **Add a shortcut:** Use option 6 to add a direct highway from `CTR` to `AIR` (40 km, 120 km/h), then compare routes again

5. **Explore the QuadTree:** Add a location outside the city bounds with option 5 — the QuadTree auto-expands!

---

## 🛠️ Build It Yourself — New Features

These sections guide you through implementing the 3 advanced features. Each builds on Parts 1-6 above.

---

### Feature A: Multi-Stop Routing

**Goal:** Chain individual route legs to navigate through waypoints (A -> B -> C -> D).

**Where:** Add `findMultiStopRoute(List<String> stopIds)` to `NavigationSystem.java`

**Pseudocode:**
```
findMultiStopRoute(stopIds):
    IF stopIds.size < 2: return null

    combinedPath = empty list
    totalDist = 0
    totalTime = 0

    FOR i = 0 to stopIds.size - 2:
        leg = findRoute(stopIds[i], stopIds[i+1])   // uses existing findRoute
        IF leg is null: return null                  // one leg failed

        IF i > 0:
            legNodes = leg.nodes.subList(1, end)     // skip first node (it's duplicate)
        ELSE:
            legNodes = leg.nodes

        combinedPath.addAll(legNodes)
        totalDist += leg.totalDistance
        totalTime += leg.totalTime

    RETURN new Route(combinedPath, totalDist, totalTime)
```

**Key idea:** Each leg's destination is the next leg's source — so skip the first node of legs 2+ to avoid duplicates.

**Test it:** `findMultiStopRoute(["CTR", "MKT", "PRT"])` should return a valid route through all 3 stops.

---

### Feature B: Route Comparison

**Goal:** Run both Dijkstra and A\* on the same query and show results side-by-side.

**Where:** Add `compareRoutes(String srcId, String destId)` to `NavigationSystem.java`

**Implementation hints:**
1. Create a **static inner class** `ComparisonResult`:
   ```java
   public static class ComparisonResult {
       public final Route dijkstraRoute;
       public final Route aStarRoute;
       public final long dijkstraTimeNs;   // computation time
       public final long aStarTimeNs;
   }
   ```

2. In the method:
   ```
   compareRoutes(srcId, destId):
       t0 = System.nanoTime()
       dijkstraResult = DIJKSTRA.findRoute(graph, src, dest)
       t1 = System.nanoTime()
       aStarResult = A_STAR.findRoute(graph, src, dest)
       t2 = System.nanoTime()

       return new ComparisonResult(dijkstraResult, aStarResult, t1-t0, t2-t1)
   ```

**OOP concept:** `ComparisonResult` is a **data transfer object** — it bundles related data for the UI to display.

**Test it:** Compare `CTR` to `PRT` — Dijkstra should find a shorter (24.5 km) but slower (64 min) path, while A\* finds a longer (38 km) but faster (36 min) path.

---

### Feature C: Driver Management + K-Nearest

This requires **two new classes:**

#### C1: `model/Driver.java`

Like `Node`, but position is **mutable** (drivers move):

```java
public class Driver {
    private final String id;
    private final String name;
    private double latitude;    // NOT final!
    private double longitude;

    // + updatePosition(lat, lon) method
}
```

#### C2: `spatial/DriverQuadTree.java`

Same structure as `QuadTree`, but:
- Stores `Driver` objects instead of `Node`
- Supports **K-nearest-neighbors** (find K closest drivers, not just 1)

**K-Nearest algorithm** uses a **max-heap** of size K:

```
findKNearest(lat, lon, k):
    maxHeap = PriorityQueue sorted FARTHEST-first (max-heap)

    findKNearestHelper(lat, lon, k, maxHeap)

    result = drain maxHeap into list
    reverse list (so closest is first)
    return result

findKNearestHelper(lat, lon, k, maxHeap):
    // PRUNE: if this quad's closest point is farther than our K-th best
    IF maxHeap.size >= k AND distToQuad(lat, lon) >= maxHeap.peek().dist:
        return   // skip this entire quadrant!

    FOR each driver d in this leaf:
        dist = euclidean(lat, lon, d.lat, d.lon)
        IF maxHeap.size < k:
            maxHeap.add(d, dist)
        ELSE IF dist < maxHeap.peek().dist:
            maxHeap.poll()          // remove farthest
            maxHeap.add(d, dist)    // add closer one

    IF divided:
        recurse into nw, ne, sw, se
```

**Why max-heap?** We always keep the K closest. The farthest of the K sits at the top — easy to check if a new candidate is closer.

#### C3: Add to `NavigationSystem.java`

Add these methods:
- `addDriver(Driver)` — add + rebuild driver tree
- `findNearestDrivers(lat, lon, k)` — delegate to DriverQuadTree
- `findNearestDriverETA(nodeId)` — find closest driver, compute route + ETA

**ETA calculation:**
```
findNearestDriverETA(pickupNodeId):
    pickup = graph.getNode(pickupNodeId)
    nearest = findNearestDrivers(pickup.lat, pickup.lon, 1)
    driver = nearest[0]
    driverNode = quadTree.findNearest(driver.lat, driver.lon)  // snap to road
    route = dijkstra.findRoute(driverNode, pickup)
    return new DriverETAResult(driver, driverNode, route)
```

**Test it:** With 5 drivers scattered across the city, calling `findNearestDriverETA("STN")` should find the driver closest to Grand Station.

---

### Build Order for New Features

```
[ ] Feature A: Multi-stop routing (just one method in NavigationSystem)
[ ] Feature B: Route comparison (ComparisonResult class + one method)
[ ] Feature C1: Driver.java (simple model class)
[ ] Feature C2: DriverQuadTree.java (copy QuadTree, modify for K-nearest)
[ ] Feature C3: Wire drivers into NavigationSystem
[ ] Add new menu options to Main.java
[ ] Test all features
```
