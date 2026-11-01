package jse.demo;

import java.util.List;
import java.util.Objects;
import jse.event.*;
import jse.physics.CollisionPair;
import jse.world.*;

public final class ArenaRules {
    public enum Status { RUNNING, WON, LOST }
    private final ArenaSettings settings;
    private final EventBus events;
    private int score;
    private double cooldown;
    private Status status = Status.RUNNING;
    public ArenaRules(ArenaSettings settings, EventBus events) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.events = Objects.requireNonNull(events, "events");
    }
    public int score() { return score; }
    public Status status() { return status; }

    public void applyCollisions(World world, List<CollisionPair> collisions, double dt, long tick) {
        if (!Double.isFinite(dt) || dt <= 0) throw new IllegalArgumentException("Rule delta must be finite and positive");
        if (status != Status.RUNNING) return;
        cooldown = Math.max(0, cooldown - dt);
        boolean enemyContact = false;
        Player player = player(world);
        for (var pair : collisions) {
            var first = world.findActive(pair.firstId()); var second = world.findActive(pair.secondId());
            if (first.isEmpty() || second.isEmpty()) continue;
            GameObject other;
            if (first.get() == player) other = second.get();
            else if (second.get() == player) other = first.get();
            else continue;
            if (other instanceof Pickup pickup) {
                world.enqueueRemove(pickup.id(), RemovalReason.COLLECTED);
                score = Math.addExact(score, pickup.value());
                events.publish(new ItemCollected(world.runId(), tick, player.id(), pickup.id(), score));
            } else if (other instanceof Enemy) enemyContact = true;
        }
        if (enemyContact && cooldown <= 0 && player.currentHealth() > 0) {
            int old = player.currentHealth();
            player.takeDamage(settings.contactDamage());
            cooldown = settings.damageCooldown();
            events.publish(new HealthChanged(world.runId(), tick, player.id(), old, player.currentHealth()));
        }
    }
    public void evaluateOutcome(World world, long tick) {
        if (status != Status.RUNNING) return;
        if (player(world).currentHealth() == 0) status = Status.LOST;
        else if (score >= settings.targetScore()) status = Status.WON;
        if (status != Status.RUNNING) events.publish(new GameFinished(world.runId(), tick, status, score));
    }
    private Player player(World world) {
        var players = new java.util.ArrayList<Player>();
        world.forEachActive(object -> { if (object instanceof Player player) players.add(player); });
        if (players.size() != 1) throw new IllegalStateException("Arena needs exactly one active Player");
        return players.get(0);
    }
}
