package jse.physics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import jse.world.WorldView;

public final class CollisionSystem {

    public List<CollisionPair> findOverlaps(WorldView world) {
        var entities = world.entities();
        var pairs = new ArrayList<CollisionPair>();
        for (int i = 0; i < entities.size(); i++) {
            for (int j = i + 1; j < entities.size(); j++) {
                var first = entities.get(i);
                var second = entities.get(j);
                if (first.bounds().overlaps(second.bounds())) {
                    pairs.add(new CollisionPair(Math.min(first.id(), second.id()),
                            Math.max(first.id(), second.id())));
                }
            }
        }
        pairs.sort(Comparator.comparingLong(CollisionPair::firstId)
                .thenComparingLong(CollisionPair::secondId));
        return List.copyOf(pairs);
    }
}
