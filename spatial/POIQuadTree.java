package spatial;

import model.PointOfInterest;

import java.util.*;

/**
 * QuadTree for Point of Interest objects.
 * Supports K-nearest-neighbor queries using a max-heap with branch-and-bound pruning.
 */

public class POIQuadTree {

    private static final int CAPACITY = 4;
    private final double minLat, maxLat, minLon, maxLon;

    private final List<PointOfInterest> points;
    private boolean divided;

    private POIQuadTree northwest, northeast, southwest, southeast;

    public POIQuadTree(double minLat, double maxLat, double minLon, double maxLon) {
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
        this.points = new ArrayList<>();
        this.divided = false;
    }

    public boolean insert(PointOfInterest poi) {
        if (!contains(poi.getLatitude(), poi.getLongitude())) {
            return false;
        }

        if (points.size() < CAPACITY && !divided) {
            points.add(poi);
            return true;
        }

        if (!divided) {
            subdivide();
        }

        if (northwest.insert(poi)) return true;
        if (northeast.insert(poi)) return true;
        if (southwest.insert(poi)) return true;
        if (southeast.insert(poi)) return true;

        return false;
    }

    // K-Nearest Neighbor Search

    /**
     * Find the K nearest POIs to a given lat/lon. We will use a maxHeap
     * Branch-and-bound pruning skips quadrants that can't contain closer points.
     */

    public List<PointOfInterest> findKNearest(double lat, double lon, int k) {
        PriorityQueue<double[]> maxHeap = new PriorityQueue<>(
            (a, b) -> Double.compare(b[0], a[0])
        );
        List<PointOfInterest> poiList = new ArrayList<>();

        findKNearestHelper(lat, lon, k, maxHeap, poiList);

        List<PointOfInterest> result = new ArrayList<>(poiList);
        result.sort((a, b) -> {
            double da = euclideanDist(lat, lon, a.getLatitude(), a.getLongitude());
            double db = euclideanDist(lat, lon, b.getLatitude(), b.getLongitude());
            return Double.compare(da, db);
        });

        return result;
    }

    private void findKNearestHelper(double lat, double lon, int k, PriorityQueue<double[]> maxHeap, List<PointOfInterest> poiList) {
        // Prune: if this quad's closest point is farther than our K-th best
        if (maxHeap.size() >= k && distToQuad(lat, lon) >= maxHeap.peek()[0]) {
            return;
        }

        for (PointOfInterest p : points) {
            double d = euclideanDist(lat, lon, p.getLatitude(), p.getLongitude());
            if (maxHeap.size() < k) {
                maxHeap.add(new double[]{d});
                poiList.add(p);
            } else if (d < maxHeap.peek()[0]) {
                // Remove the farthest POI
                double farthestDist = maxHeap.poll()[0];
                // Find and remove the POI with that distance from poiList
                for (int i = poiList.size() - 1; i >= 0; i--) {
                    double di = euclideanDist(lat, lon, poiList.get(i).getLatitude(), poiList.get(i).getLongitude());
                    if (Math.abs(di - farthestDist) < 1e-12) {
                        poiList.remove(i);
                        break;
                    }
                }
                maxHeap.add(new double[]{d});
                poiList.add(p);
            }
        }

        if (divided) {
            northwest.findKNearestHelper(lat, lon, k, maxHeap, poiList);
            northeast.findKNearestHelper(lat, lon, k, maxHeap, poiList);
            southwest.findKNearestHelper(lat, lon, k, maxHeap, poiList);
            southeast.findKNearestHelper(lat, lon, k, maxHeap, poiList);
        }
    }
    
    private boolean contains(double lat, double lon) {
        return lat >= minLat && lat <= maxLat && lon >= minLon && lon <= maxLon;
    }

    private void subdivide() {
        double midLat = (minLat + maxLat) / 2;
        double midLon = (minLon + maxLon) / 2;

        northwest = new POIQuadTree(midLat, maxLat, minLon, midLon);
        northeast = new POIQuadTree(midLat, maxLat, midLon, maxLon);
        southwest = new POIQuadTree(minLat, midLat, minLon, midLon);
        southeast = new POIQuadTree(minLat, midLat, midLon, maxLon);

        for (PointOfInterest p : points) {
            northwest.insert(p);
            northeast.insert(p);
            southwest.insert(p);
            southeast.insert(p);
        }
        points.clear();
        divided = true;
    }

    private double distToQuad(double lat, double lon) {
        double closestLat = Math.max(minLat, Math.min(lat, maxLat));
        double closestLon = Math.max(minLon, Math.min(lon, maxLon));
        return euclideanDist(lat, lon, closestLat, closestLon);
    }

    private double euclideanDist(double lat1, double lon1, double lat2, double lon2) {
        double dLat = lat1 - lat2;
        double dLon = lon1 - lon2;
        return Math.sqrt(dLat * dLat + dLon * dLon);
    }
}
