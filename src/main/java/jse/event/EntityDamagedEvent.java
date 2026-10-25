package jse.event;

public record EntityDamagedEvent(long entityId, int damageAmount, int currentHealth) implements GameEvent {}
