package jse.observer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import jse.demo.ArenaRules;
import jse.event.EventBus;
import jse.event.GameFinished;
import jse.event.HealthChanged;
import jse.event.ItemCollected;
import jse.event.Subscription;
import jse.world.Player;

/**
 * Concrete observer holding the numbers a HUD shows, built only from events.
 */
public final class HudObserver implements AutoCloseable {

    private final List<Subscription> subscriptions = new ArrayList<>();
    private int score;
    private int health;
    private ArenaRules.Status result;

    public HudObserver(EventBus events, Player player) {
        Objects.requireNonNull(events, "events");
        this.health = Objects.requireNonNull(player, "player").currentHealth();
        subscriptions.add(events.subscribe(ItemCollected.class, e -> score = e.newScore()));
        subscriptions.add(events.subscribe(HealthChanged.class, e -> health = e.newHealth()));
        subscriptions.add(events.subscribe(GameFinished.class, e -> result = e.result()));
    }

    public int score() {
        return score;
    }

    public int health() {
        return health;
    }

    /**
     * Empty while the run is still going; WON or LOST afterwards.
     */
    public Optional<ArenaRules.Status> result() {
        return Optional.ofNullable(result);
    }

    @Override
    public void close() {
        subscriptions.forEach(Subscription::close);
        subscriptions.clear();
    }
}
