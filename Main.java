import engine.NavigationSystem;
import graph.Graph;
import model.Node;
import model.PointOfInterest;
import model.Route;
import spatial.QuadTree;
import strategy.AStarStrategy;
import strategy.DijkstraStrategy;
import strategy.RoutingStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

/**
 * Main - Interactive console UI for the Geospatial Routing & Navigation System.
 */
public class Main {

    private static final RoutingStrategy DIJKSTRA = new DijkstraStrategy();
    private static final RoutingStrategy A_STAR = new AStarStrategy();

    private static final String CYAN = "\u001B[36m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[2m";
    private static final String RESET = "\u001B[0m";

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        NavigationSystem navSystem = buildCampusMap();

        printBanner();

        boolean running = true;
        while (running) {
            printMenu(navSystem);
            System.out.print(CYAN + "  > " + RESET);
            String choice = sc.nextLine().trim();
            System.out.println();

            switch (choice) {
                case "1":
                    showAllLocations(navSystem);
                    break;
                case "2":
                    findRoute(navSystem, sc);
                    break;
                case "3":
                    switchStrategy(navSystem);
                    break;
                case "4":
                    findNearest(navSystem, sc);
                    break;
                case "5":
                    addLocation(navSystem, sc);
                    break;
                case "6":
                    addRoad(navSystem, sc);
                    break;
                case "7":
                    showMapStats(navSystem);
                    break;
                case "8":
                    deleteLocation(navSystem, sc);
                    break;
                case "9":
                    multiStopRoute(navSystem, sc);
                    break;
                case "10":
                    compareRoutes(navSystem, sc);
                    break;
                case "11":
                    findNearbyPlaces(navSystem, sc);
                    break;
                case "12":
                    addPlace(navSystem, sc);
                    break;
                case "0":
                    running = false;
                    System.out.println(DIM + "  Goodbye! Thank you for using GeoNav." + RESET + "\n");
                    break;
                default:
                    System.out.println(RED + "  Invalid choice. Enter 0-12." + RESET + "\n");
            }
        }

        sc.close();
    }

    // ══════════════════════════════════════════════════════════
    // BUILD DEMO CITY MAP
    // ══════════════════════════════════════════════════════════

    private static NavigationSystem buildCampusMap() {
        Graph graph = new Graph();

        // ── 14 City Locations ───────────────────────────────────
        Node central = new Node("CTR", "Central Plaza", 12.9756, 77.6068);
        Node garden = new Node("GDN", "Sunset Garden", 12.9763, 77.5929);
        Node market = new Node("MKT", "Riverside Market", 12.9784, 77.6408);
        Node techHub = new Node("THB", "Tech Hub", 12.9352, 77.6245);
        Node oldTown = new Node("OLD", "Old Town", 12.9966, 77.5713);
        Node station = new Node("STN", "Grand Station", 12.9772, 77.5722);
        Node univrst = new Node("UNI", "University", 12.9308, 77.5838);
        Node lakeSide = new Node("LKS", "Lakeside", 12.9116, 77.6389);
        Node indPark = new Node("IND", "Industrial Park", 12.8456, 77.6603);
        Node hillView = new Node("HLV", "Hillview", 12.9698, 77.7500);
        Node northGte = new Node("NTH", "North Gate", 13.0358, 77.5970);
        Node airport = new Node("AIR", "Sky Airport", 13.1989, 77.7068);
        Node castle = new Node("CST", "Castle District", 12.9988, 77.5921);
        Node portArea = new Node("PRT", "Port Area", 12.8650, 77.7870);

        for (Node n : new Node[] {
                central, garden, market, techHub, oldTown, station, univrst,
                lakeSide, indPark, hillView, northGte, airport, castle, portArea
        })
            graph.addNode(n);

        // ── Roads — varying speeds creates Dijkstra vs A* divergence ─
        //
        // Alleys/lanes: low speed (12-20 km/h) => short distance, slow time
        // City roads: medium (25-40 km/h)
        // Highways: fast (55-80 km/h) => longer distance, fast time
        // Expressway: very fast (100 km/h)
        //
        // Result: Dijkstra picks short alleys, A* picks faster highways.

        // City center connections
        graph.addEdge(central, garden, 1.5, 25); // boulevard
        graph.addEdge(central, market, 5.0, 40); // main avenue
        graph.addEdge(central, station, 2.0, 20); // congested street
        graph.addEdge(garden, castle, 2.8, 30); // scenic route
        graph.addEdge(garden, station, 1.2, 15); // narrow alley
        graph.addEdge(garden, oldTown, 3.5, 35); // bridge road

        // Inner ring connections
        graph.addEdge(market, techHub, 5.5, 35); // ring road
        graph.addEdge(market, hillView, 15.0, 60); // highway — long but FAST
        graph.addEdge(techHub, univrst, 4.0, 30); // city road
        graph.addEdge(techHub, lakeSide, 3.2, 25); // shortcut
        graph.addEdge(univrst, station, 4.5, 20); // congested stretch
        graph.addEdge(univrst, lakeSide, 6.0, 12); // dirt road — VERY SLOW

        // Outer ring / highway connections
        graph.addEdge(lakeSide, indPark, 10.0, 55); // coastal highway
        graph.addEdge(lakeSide, portArea, 12.0, 50); // outer ring
        graph.addEdge(indPark, portArea, 15.0, 70); // expressway — long but very fast
        graph.addEdge(hillView, portArea, 18.0, 80); // expressway — longest but fastest

        // North connections
        graph.addEdge(oldTown, northGte, 5.0, 40); // north avenue
        graph.addEdge(castle, northGte, 4.5, 35); // castle road north
        graph.addEdge(castle, oldTown, 2.5, 25); // heritage lane

        // Airport expressway
        graph.addEdge(northGte, airport, 30.0, 100); // expressway — 30 km at 100 km/h!

        // Cross-city shortcuts (congested)
        graph.addEdge(station, oldTown, 3.0, 15); // slow trolley route
        graph.addEdge(techHub, indPark, 16.0, 45); // factory road (congested)

        double minLat = 12.84, maxLat = 13.21, minLon = 77.56, maxLon = 77.80;
        QuadTree quadTree = new QuadTree(minLat, maxLat, minLon, maxLon);
        for (Node node : graph.getAllNodes())
            quadTree.insert(node);

        NavigationSystem nav = new NavigationSystem(graph, quadTree, DIJKSTRA,
                minLat, maxLat, minLon, maxLon);

        // ── Demo POIs (hotels, restaurants, malls, theatres) ──────

        // HOTELS (12)
        nav.addPOI(new PointOfInterest("H1",  "Grand Plaza Hotel",     "HOTEL", 4.2, 12.9760, 77.6075, "CTR"));
        nav.addPOI(new PointOfInterest("H2",  "Sunset Inn",            "HOTEL", 4.8, 12.9768, 77.5935, "GDN"));
        nav.addPOI(new PointOfInterest("H3",  "City Lodge",            "HOTEL", 3.5, 12.9790, 77.6415, "MKT"));
        nav.addPOI(new PointOfInterest("H4",  "Tech Towers Hotel",     "HOTEL", 4.0, 12.9358, 77.6250, "THB"));
        nav.addPOI(new PointOfInterest("H5",  "Heritage House",        "HOTEL", 4.6, 12.9970, 77.5720, "OLD"));
        nav.addPOI(new PointOfInterest("H6",  "Station View Hotel",    "HOTEL", 3.2, 12.9778, 77.5728, "STN"));
        nav.addPOI(new PointOfInterest("H7",  "Campus Stay",           "HOTEL", 3.8, 12.9315, 77.5845, "UNI"));
        nav.addPOI(new PointOfInterest("H8",  "Lakeside Resort",       "HOTEL", 4.9, 12.9120, 77.6395, "LKS"));
        nav.addPOI(new PointOfInterest("H9",  "Industrial Inn",        "HOTEL", 2.8, 12.8462, 77.6610, "IND"));
        nav.addPOI(new PointOfInterest("H10", "Hillview Lodge",        "HOTEL", 4.4, 12.9705, 77.7510, "HLV"));
        nav.addPOI(new PointOfInterest("H11", "North Gate Hotel",      "HOTEL", 3.9, 13.0365, 77.5978, "NTH"));
        nav.addPOI(new PointOfInterest("H12", "Sky Airport Hotel",     "HOTEL", 4.1, 13.1995, 77.7075, "AIR"));

        // RESTAURANTS (10)
        nav.addPOI(new PointOfInterest("R1",  "Spice Garden",          "RESTAURANT", 4.5, 12.9752, 77.6062, "CTR"));
        nav.addPOI(new PointOfInterest("R2",  "Garden Cafe",           "RESTAURANT", 4.0, 12.9770, 77.5932, "GDN"));
        nav.addPOI(new PointOfInterest("R3",  "Market Bites",          "RESTAURANT", 3.8, 12.9780, 77.6400, "MKT"));
        nav.addPOI(new PointOfInterest("R4",  "Lakeside Grill",        "RESTAURANT", 4.3, 12.9110, 77.6382, "LKS"));
        nav.addPOI(new PointOfInterest("R5",  "Castle Bistro",         "RESTAURANT", 4.7, 12.9992, 77.5928, "CST"));
        nav.addPOI(new PointOfInterest("R6",  "Old Town Kitchen",      "RESTAURANT", 4.1, 12.9962, 77.5708, "OLD"));
        nav.addPOI(new PointOfInterest("R7",  "Tech Canteen",          "RESTAURANT", 3.3, 12.9348, 77.6238, "THB"));
        nav.addPOI(new PointOfInterest("R8",  "Port Fish House",       "RESTAURANT", 4.6, 12.8655, 77.7878, "PRT"));
        nav.addPOI(new PointOfInterest("R9",  "Hilltop Diner",         "RESTAURANT", 3.9, 12.9695, 77.7495, "HLV"));
        nav.addPOI(new PointOfInterest("R10", "Station Snacks",        "RESTAURANT", 3.0, 12.9775, 77.5725, "STN"));

        // MALLS (6)
        nav.addPOI(new PointOfInterest("M1",  "Central Mall",          "MALL", 4.0, 12.9758, 77.6072, "CTR"));
        nav.addPOI(new PointOfInterest("M2",  "Tech Plaza",            "MALL", 3.5, 12.9355, 77.6248, "THB"));
        nav.addPOI(new PointOfInterest("M3",  "Market Square Mall",    "MALL", 4.2, 12.9788, 77.6412, "MKT"));
        nav.addPOI(new PointOfInterest("M4",  "Lakeside Galleria",     "MALL", 4.5, 12.9118, 77.6392, "LKS"));
        nav.addPOI(new PointOfInterest("M5",  "Castle Court",          "MALL", 3.8, 12.9985, 77.5918, "CST"));
        nav.addPOI(new PointOfInterest("M6",  "Hillview Centre",       "MALL", 4.1, 12.9700, 77.7505, "HLV"));

        // THEATRES (5)
        nav.addPOI(new PointOfInterest("T1",  "Star Cinema",           "THEATRE", 4.3, 12.9755, 77.6065, "CTR"));
        nav.addPOI(new PointOfInterest("T2",  "Castle Playhouse",      "THEATRE", 4.7, 12.9990, 77.5925, "CST"));
        nav.addPOI(new PointOfInterest("T3",  "Garden Amphitheatre",   "THEATRE", 4.0, 12.9765, 77.5932, "GDN"));
        nav.addPOI(new PointOfInterest("T4",  "Lakeside IMAX",         "THEATRE", 4.5, 12.9115, 77.6388, "LKS"));
        nav.addPOI(new PointOfInterest("T5",  "Market Multiplex",      "THEATRE", 3.6, 12.9782, 77.6405, "MKT"));

        return nav;
    }

    // ══════════════════════════════════════════════════════════
    // MENU ACTIONS (1-10)
    // ══════════════════════════════════════════════════════════

    private static void showAllLocations(NavigationSystem nav) {
        System.out.println(BOLD + "  LOCATIONS ON MAP" + RESET);
        System.out.println(DIM + "  ────────────────────────────────────────────" + RESET);
        System.out.printf("  " + DIM + "%-5s" + RESET + "  %-16s  %10s  %10s\n",
                "ID", "Name", "Latitude", "Longitude");
        System.out.println(DIM + "  ────────────────────────────────────────────" + RESET);

        for (Node node : nav.getGraph().getAllNodes()) {
            System.out.printf("  " + CYAN + "%-5s" + RESET + "  %-16s  %10.4f  %10.4f\n",
                    node.getId(), node.getName(), node.getLatitude(), node.getLongitude());
        }
        System.out.println(DIM + "  ────────────────────────────────────────────" + RESET);
        System.out.printf("  " + DIM + "Total: %d locations" + RESET + "\n\n", nav.getGraph().getNodeCount());
    }

    private static void findRoute(NavigationSystem nav, Scanner sc) {
        System.out.println(BOLD + "  FIND ROUTE" + RESET);
        System.out.println(DIM + "  Strategy: " + nav.getStrategy().getStrategyName() + RESET);
        System.out.println();

        System.out.print("  " + YELLOW + "Source ID      : " + RESET);
        String srcId = sc.nextLine().trim().toUpperCase();
        System.out.print("  " + YELLOW + "Destination ID : " + RESET);
        String destId = sc.nextLine().trim().toUpperCase();

        if (srcId.equals(destId)) {
            System.out.println("\n  " + YELLOW + "You're already there!" + RESET + "\n");
            return;
        }

        long t0 = System.nanoTime();
        Route route = nav.findRoute(srcId, destId);
        long elapsed = System.nanoTime() - t0;

        if (route == null) {
            System.out.println("\n  " + RED + "No route found. Check that IDs are correct." + RESET + "\n");
        } else {
            printRouteBox(route, elapsed, GREEN);
        }
    }

    private static void switchStrategy(NavigationSystem nav) {
        if (nav.getStrategy() instanceof DijkstraStrategy) {
            nav.setStrategy(A_STAR);
        } else {
            nav.setStrategy(DIJKSTRA);
        }
        System.out.println("  " + GREEN + "Switched to: " + BOLD
                + nav.getStrategy().getStrategyName() + RESET + "\n");
    }

    private static void findNearest(NavigationSystem nav, Scanner sc) {
        System.out.println(BOLD + "  FIND NEAREST LOCATION" + RESET);
        System.out.println();
        System.out.print("  " + YELLOW + "Latitude  : " + RESET);
        double lat = Double.parseDouble(sc.nextLine().trim());
        System.out.print("  " + YELLOW + "Longitude : " + RESET);
        double lon = Double.parseDouble(sc.nextLine().trim());

        Node nearest = nav.findNearestNode(lat, lon);
        if (nearest != null) {
            System.out.println("\n  " + GREEN + "Nearest: " + BOLD + nearest.getName()
                    + RESET + GREEN + " (" + nearest.getId() + ")"
                    + " [" + nearest.getLatitude() + ", " + nearest.getLongitude() + "]" + RESET + "\n");
        } else {
            System.out.println("\n  " + RED + "No locations found." + RESET + "\n");
        }
    }

    private static void addLocation(NavigationSystem nav, Scanner sc) {
        System.out.println(BOLD + "  ADD NEW LOCATION" + RESET);
        System.out.println();
        System.out.print("  " + YELLOW + "Location ID   : " + RESET);
        String id = sc.nextLine().trim().toUpperCase();
        if (nav.getGraph().getNode(id) != null) {
            System.out.println("\n  " + RED + "ID '" + id + "' already exists." + RESET + "\n");
            return;
        }
        System.out.print("  " + YELLOW + "Location Name : " + RESET);
        String name = sc.nextLine().trim();
        System.out.print("  " + YELLOW + "Latitude      : " + RESET);
        double lat = Double.parseDouble(sc.nextLine().trim());
        System.out.print("  " + YELLOW + "Longitude     : " + RESET);
        double lon = Double.parseDouble(sc.nextLine().trim());

        nav.addLocation(new Node(id, name, lat, lon));
        System.out.println("\n  " + GREEN + "Added: " + BOLD + name + RESET + GREEN
                + " (" + id + ")" + RESET + "\n");
    }

    private static void deleteLocation(NavigationSystem nav, Scanner sc) {
        System.out.println(BOLD + "  DELETE LOCATION" + RESET);
        System.out.println(DIM + "  (removes the location and all connected roads)" + RESET);
        System.out.println();
        System.out.print("  " + YELLOW + "Location ID : " + RESET);
        String id = sc.nextLine().trim().toUpperCase();

        if (nav.getGraph().getNode(id) == null) {
            System.out.println("\n  " + RED + "No location with ID '" + id + "' found." + RESET + "\n");
            return;
        }

        String name = nav.getGraph().getNode(id).getName();
        boolean removed = nav.removeLocation(id);
        if (removed) {
            System.out.println("\n  " + GREEN + "Deleted: " + BOLD + name + RESET + GREEN
                    + " (" + id + ") and all connected roads." + RESET + "\n");
        } else {
            System.out.println("\n  " + RED + "Failed to delete location." + RESET + "\n");
        }
    }

    private static void addRoad(NavigationSystem nav, Scanner sc) {
        System.out.println(BOLD + "  ADD NEW ROAD" + RESET);
        System.out.println(DIM + "  (creates a bidirectional connection)" + RESET);
        System.out.println();
        System.out.print("  " + YELLOW + "From ID           : " + RESET);
        String srcId = sc.nextLine().trim().toUpperCase();
        System.out.print("  " + YELLOW + "To ID             : " + RESET);
        String destId = sc.nextLine().trim().toUpperCase();
        if (nav.getGraph().getNode(srcId) == null || nav.getGraph().getNode(destId) == null) {
            System.out.println("\n  " + RED + "Invalid ID(s). Add locations first." + RESET + "\n");
            return;
        }
        System.out.print("  " + YELLOW + "Distance (km)     : " + RESET);
        double dist = Double.parseDouble(sc.nextLine().trim());
        System.out.print("  " + YELLOW + "Speed limit (km/h): " + RESET);
        double speed = Double.parseDouble(sc.nextLine().trim());

        nav.addRoad(srcId, destId, dist, speed);
        System.out.println("\n  " + GREEN + "Road added: " + BOLD + srcId + " <-> " + destId
                + RESET + GREEN + " (" + dist + " km, " + speed + " km/h)" + RESET + "\n");
    }

    private static void showMapStats(NavigationSystem nav) {
        System.out.println(BOLD + "  MAP STATISTICS" + RESET);
        System.out.println(DIM + "  ────────────────────────────────────────────" + RESET);
        System.out.printf("  Locations  : " + CYAN + "%d" + RESET + "\n", nav.getGraph().getNodeCount());
        System.out.printf("  Roads      : " + CYAN + "%d" + RESET + "\n", nav.getGraph().getEdgeCount());
        System.out.printf("  Strategy   : " + CYAN + "%s" + RESET + "\n", nav.getStrategy().getStrategyName());
        System.out.println(DIM + "  ────────────────────────────────────────────" + RESET + "\n");
    }

    private static void multiStopRoute(NavigationSystem nav, Scanner sc) {
        System.out.println(BOLD + "  MULTI-STOP ROUTE" + RESET);
        System.out.println(DIM + "  Enter stop IDs separated by commas (min 2)" + RESET);
        System.out.println(DIM + "  Example: MG, LIB, CAF, HOS" + RESET);
        System.out.println();

        System.out.print("  " + YELLOW + "Stops: " + RESET);
        String input = sc.nextLine().trim().toUpperCase();
        String[] parts = input.split("\\s*,\\s*");

        if (parts.length < 2) {
            System.out.println("\n  " + RED + "Need at least 2 stops." + RESET + "\n");
            return;
        }

        List<String> stops = new ArrayList<>();
        for (String p : parts) {
            if (!p.isEmpty())
                stops.add(p);
        }

        long t0 = System.nanoTime();
        Route route = nav.findMultiStopRoute(stops);
        long elapsed = System.nanoTime() - t0;

        if (route == null) {
            System.out.println("\n  " + RED + "No route found. Check IDs and connectivity." + RESET + "\n");
        } else {
            System.out.println();
            System.out.println(MAGENTA + "  ╔══════════════════════════════════════════════════╗" + RESET);
            System.out.println(MAGENTA + "  ║  MULTI-STOP ROUTE                                ║" + RESET);
            System.out.println(MAGENTA + "  ╠══════════════════════════════════════════════════╣" + RESET);

            System.out.println(MAGENTA + "  ║  " + RESET + "Stops: " + BOLD + String.join(" -> ", stops) + RESET);

            StringBuilder pathStr = new StringBuilder();
            for (int i = 0; i < route.getNodes().size(); i++) {
                if (i > 0)
                    pathStr.append(" -> ");
                pathStr.append(route.getNodes().get(i).getName());
            }
            System.out.println(MAGENTA + "  ║  " + RESET + "Path : " + DIM + pathStr + RESET);
            System.out.printf(MAGENTA + "  ║  " + RESET + "Total Distance : " + CYAN + "%.2f km" + RESET + "\n",
                    route.getTotalDistance());
            System.out.printf(MAGENTA + "  ║  " + RESET + "Total Time     : " + CYAN + "%.1f minutes" + RESET + "\n",
                    route.getTotalTime() * 60);
            System.out.printf(MAGENTA + "  ║  " + RESET + DIM + "Computed in %.3f ms" + RESET + "\n",
                    elapsed / 1_000_000.0);
            System.out.println(MAGENTA + "  ╚══════════════════════════════════════════════════╝" + RESET + "\n");
        }
    }

    private static void compareRoutes(NavigationSystem nav, Scanner sc) {
        System.out.println(BOLD + "  ROUTE COMPARISON — Dijkstra vs A*" + RESET);
        System.out.println();
        System.out.print("  " + YELLOW + "Source ID      : " + RESET);
        String srcId = sc.nextLine().trim().toUpperCase();
        System.out.print("  " + YELLOW + "Destination ID : " + RESET);
        String destId = sc.nextLine().trim().toUpperCase();

        NavigationSystem.ComparisonResult cmp = nav.compareRoutes(srcId, destId);
        if (cmp == null) {
            System.out.println("\n  " + RED + "Invalid IDs." + RESET + "\n");
            return;
        }

        System.out.println();
        System.out.println(BOLD + "  ┌─────────────────────────┬─────────────────────────┐" + RESET);
        System.out.printf(BOLD + "  │ %-24s" + RESET + BOLD + "│ %-24s│" + RESET + "\n",
                CYAN + " Dijkstra (Distance)" + RESET, MAGENTA + " A* Search (Time)" + RESET);
        System.out.println(BOLD + "  ├─────────────────────────┼─────────────────────────┤" + RESET);

        // Path
        String dPath = cmp.dijkstraRoute != null ? formatPathShort(cmp.dijkstraRoute) : "No route";
        String aPath = cmp.aStarRoute != null ? formatPathShort(cmp.aStarRoute) : "No route";
        System.out.printf("  │ %-24s│ %-24s│\n", " " + dPath, " " + aPath);

        // Distance
        String dDist = cmp.dijkstraRoute != null
                ? String.format("%.2f km", cmp.dijkstraRoute.getTotalDistance())
                : "-";
        String aDist = cmp.aStarRoute != null
                ? String.format("%.2f km", cmp.aStarRoute.getTotalDistance())
                : "-";
        System.out.printf("  │ Dist: %-18s│ Dist: %-18s│\n", dDist, aDist);

        // Time
        String dTime = cmp.dijkstraRoute != null
                ? String.format("%.1f min", cmp.dijkstraRoute.getTotalTime() * 60)
                : "-";
        String aTime = cmp.aStarRoute != null
                ? String.format("%.1f min", cmp.aStarRoute.getTotalTime() * 60)
                : "-";
        System.out.printf("  │ Time: %-18s│ Time: %-18s│\n", dTime, aTime);

        // Computation time
        System.out.printf("  │ Comp: %-18s│ Comp: %-18s│\n",
                String.format("%.3f ms", cmp.dijkstraTimeNs / 1_000_000.0),
                String.format("%.3f ms", cmp.aStarTimeNs / 1_000_000.0));

        System.out.println("  └─────────────────────────┴─────────────────────────┘");
        System.out.println();
    }

    // ══════════════════════════════════════════════════════════
    // POI FEATURES (11-12)
    // ══════════════════════════════════════════════════════════

    private static void findNearbyPlaces(NavigationSystem nav, Scanner sc) {
        System.out.println(BOLD + "  FIND NEARBY PLACES" + RESET);
        System.out.println();

        // Show available categories
        Set<String> categories = nav.getCategories();
        if (categories.isEmpty()) {
            System.out.println("  " + RED + "No places loaded." + RESET + "\n");
            return;
        }

        System.out.println("  " + DIM + "Categories:" + RESET);
        String[] catArray = categories.toArray(new String[0]);
        for (int i = 0; i < catArray.length; i++) {
            int count = nav.getPOIsByCategory(catArray[i]).size();
            System.out.printf("  " + CYAN + BOLD + "  %d." + RESET + " %s " + DIM + "(%d places)" + RESET + "\n",
                    i + 1, catArray[i], count);
        }
        System.out.println();
        System.out.print("  " + YELLOW + "Choose category (1-" + catArray.length + "): " + RESET);
        int catChoice;
        try {
            catChoice = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("\n  " + RED + "Invalid choice." + RESET + "\n");
            return;
        }
        if (catChoice < 1 || catChoice > catArray.length) {
            System.out.println("\n  " + RED + "Invalid choice." + RESET + "\n");
            return;
        }
        String selectedCat = catArray[catChoice - 1];

        // Get user location
        System.out.print("  " + YELLOW + "Your location (Node ID): " + RESET);
        String nodeId = sc.nextLine().trim().toUpperCase();
        Node userNode = nav.getGraph().getNode(nodeId);
        if (userNode == null) {
            System.out.println("\n  " + RED + "Unknown location ID." + RESET + "\n");
            return;
        }

        // Find top 3 nearest
        List<PointOfInterest> nearest = nav.findNearestPOIs(selectedCat,
                userNode.getLatitude(), userNode.getLongitude(), 3);

        if (nearest.isEmpty()) {
            System.out.println("\n  " + RED + "No " + selectedCat + " places found." + RESET + "\n");
            return;
        }

        // Display results
        System.out.println();
        System.out.println(BOLD + "  TOP " + nearest.size() + " " + selectedCat + "S NEAR " + userNode.getName() + RESET);
        System.out.println(DIM + "  ────────────────────────────────────────────────────────" + RESET);
        System.out.printf("  " + DIM + "%-3s %-22s %-14s %s" + RESET + "\n", "#", "Name", "Rating", "Distance");
        System.out.println(DIM + "  ────────────────────────────────────────────────────────" + RESET);

        for (int i = 0; i < nearest.size(); i++) {
            PointOfInterest poi = nearest.get(i);
            double dist = haversineDist(userNode.getLatitude(), userNode.getLongitude(),
                    poi.getLatitude(), poi.getLongitude());
            System.out.printf("  " + CYAN + BOLD + "%-3d" + RESET + " %-22s "
                    + YELLOW + "%-14s" + RESET + " " + GREEN + "%.1f km" + RESET + "\n",
                    i + 1, poi.getName(), poi.getStars() + " (" + poi.getRating() + ")", dist);
        }
        System.out.println(DIM + "  ────────────────────────────────────────────────────────" + RESET);

        // Let user choose one to navigate to
        System.out.println();
        System.out.print("  " + YELLOW + "Navigate to (1-" + nearest.size() + ", or 0 to skip): " + RESET);
        int navChoice;
        try {
            navChoice = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            navChoice = 0;
        }

        if (navChoice < 1 || navChoice > nearest.size()) {
            System.out.println(DIM + "  Returning to menu." + RESET + "\n");
            return;
        }

        // Route to chosen POI
        PointOfInterest chosen = nearest.get(navChoice - 1);
        System.out.println();
        System.out.println(BOLD + "  NAVIGATING TO: " + RESET + CYAN + chosen.getName() + RESET);

        long t0 = System.nanoTime();
        Route route = nav.findRoute(nodeId, chosen.getNearestNodeId());
        long elapsed = System.nanoTime() - t0;

        if (route == null) {
            System.out.println("  " + RED + "No route found to " + chosen.getName() + "." + RESET + "\n");
            return;
        }

        printRouteBox(route, elapsed, GREEN);
    }

    private static void addPlace(NavigationSystem nav, Scanner sc) {
        System.out.println(BOLD + "  ADD A PLACE (POI)" + RESET);
        System.out.println();

        System.out.print("  " + YELLOW + "Place ID       : " + RESET);
        String id = sc.nextLine().trim().toUpperCase();
        System.out.print("  " + YELLOW + "Place Name     : " + RESET);
        String name = sc.nextLine().trim();
        System.out.print("  " + YELLOW + "Category       : " + RESET);
        String cat = sc.nextLine().trim().toUpperCase();
        System.out.print("  " + YELLOW + "Rating (1-5)   : " + RESET);
        double rating = Double.parseDouble(sc.nextLine().trim());
        System.out.print("  " + YELLOW + "Latitude       : " + RESET);
        double lat = Double.parseDouble(sc.nextLine().trim());
        System.out.print("  " + YELLOW + "Longitude      : " + RESET);
        double lon = Double.parseDouble(sc.nextLine().trim());
        System.out.print("  " + YELLOW + "Nearest Node ID: " + RESET);
        String nearNode = sc.nextLine().trim().toUpperCase();

        if (nav.getGraph().getNode(nearNode) == null) {
            System.out.println("\n  " + RED + "Unknown node '" + nearNode + "'." + RESET + "\n");
            return;
        }

        nav.addPOI(new PointOfInterest(id, name, cat, rating, lat, lon, nearNode));
        System.out.println("\n  " + GREEN + "Place added: " + BOLD + name + RESET + GREEN
                + " [" + cat + "] " + rating + " stars" + RESET + "\n");
    }

    // Approximate distance in km (haversine formula)
    private static double haversineDist(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // ══════════════════════════════════════════════════════════
    // UI HELPERS
    // ══════════════════════════════════════════════════════════

    private static void printRouteBox(Route route, long elapsedNs, String color) {
        System.out.println();
        System.out.println(color + "  ╔══════════════════════════════════════════╗" + RESET);
        System.out.println(color + "  ║  ROUTE FOUND                             ║" + RESET);
        System.out.println(color + "  ╠══════════════════════════════════════════╣" + RESET);

        StringBuilder pathStr = new StringBuilder();
        for (int i = 0; i < route.getNodes().size(); i++) {
            if (i > 0)
                pathStr.append(" -> ");
            pathStr.append(route.getNodes().get(i).getName());
        }
        System.out.println(color + "  ║  " + RESET + "Path : " + BOLD + pathStr + RESET);
        System.out.printf(color + "  ║  " + RESET + "Distance : " + CYAN + "%.2f km" + RESET + "\n",
                route.getTotalDistance());
        System.out.printf(color + "  ║  " + RESET + "Time     : " + CYAN + "%.1f minutes" + RESET + "\n",
                route.getTotalTime() * 60);
        System.out.printf(color + "  ║  " + RESET + DIM + "Computed in %.3f ms" + RESET + "\n",
                elapsedNs / 1_000_000.0);
        System.out.println(color + "  ╚══════════════════════════════════════════╝" + RESET + "\n");
    }

    private static String formatPathShort(Route route) {
        List<Node> nodes = route.getNodes();
        if (nodes.size() <= 3) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < nodes.size(); i++) {
                if (i > 0)
                    sb.append("->");
                sb.append(nodes.get(i).getId());
            }
            return sb.toString();
        }
        return nodes.get(0).getId() + "->...->" + nodes.get(nodes.size() - 1).getId()
                + " (" + nodes.size() + " stops)";
    }

    private static void printBanner() {
        System.out.println();
        System.out.println(CYAN + BOLD + "   ____            _   _             " + RESET);
        System.out.println(CYAN + BOLD + "  / ___| ___  ___ | \\ | | __ ___   __" + RESET);
        System.out.println(CYAN + BOLD + " | |  _ / _ \\/ _ \\|  \\| |/ _` \\ \\ / /" + RESET);
        System.out.println(CYAN + BOLD + " | |_| |  __/ (_) | |\\  | (_| |\\ V / " + RESET);
        System.out.println(CYAN + BOLD + "  \\____|\\___|\\___/|_| \\_|\\__,_| \\_/  " + RESET);
        System.out.println();
        System.out.println(DIM + "  Geospatial Routing & Navigation Engine" + RESET);
        System.out.println(DIM + "  Demo City | 14 locations | 23 roads | 33 places" + RESET);
        System.out.println();
    }

    private static void printMenu(NavigationSystem nav) {
        String strat = (nav.getStrategy() instanceof DijkstraStrategy) ? "Dijkstra" : "A*";

        System.out.println(DIM + "  ────────────────────────────────────────────" + RESET);
        System.out.println("  " + BOLD + " 1" + RESET + "  Show All Locations");
        System.out.println("  " + BOLD + " 2" + RESET + "  Find Route");
        System.out.println("  " + BOLD + " 3" + RESET + "  Switch Strategy " + DIM + "[" + strat + "]" + RESET);
        System.out.println("  " + BOLD + " 4" + RESET + "  Find Nearest Location");
        System.out.println("  " + MAGENTA + BOLD + " 5" + RESET + "  Add Location");
        System.out.println("  " + MAGENTA + BOLD + " 6" + RESET + "  Add Road");
        System.out.println("  " + BOLD + " 7" + RESET + "  Map Statistics");
        System.out.println("  " + RED + BOLD + " 8" + RESET + "  Delete Location");
        System.out.println("  " + CYAN + BOLD + " 9" + RESET + "  Multi-Stop Route");
        System.out.println("  " + CYAN + BOLD + "10" + RESET + "  Compare Routes " + DIM + "(Dijkstra vs A*)" + RESET);
        System.out.println("  " + CYAN + BOLD + "11" + RESET + "  Find Nearby Places " + DIM + "(Hotels, Restaurants...)" + RESET);
        System.out.println("  " + MAGENTA + BOLD + "12" + RESET + "  Add a Place");
        System.out.println("  " + DIM + " 0" + RESET + "  Exit");
        System.out.println(DIM + "  ────────────────────────────────────────────" + RESET);
    }
}
