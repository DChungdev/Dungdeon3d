package com.dungeondelvers.game.instance;

import tools.jackson.databind.JsonNode;

/**
 * Position / direction on the game plane. {@code y} is carried for the client but
 * ignored by gameplay logic, which works on XZ (network-protocol §4.1).
 */
public record Vec3(double x, double y, double z) {

    public static final Vec3 ZERO = new Vec3(0, 0, 0);

    public double distanceXz(Vec3 other) {
        double dx = other.x - x;
        double dz = other.z - z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public double lengthXz() {
        return Math.sqrt(x * x + z * z);
    }

    /** Moves from this point towards {@code target} by at most {@code maxDistance} on XZ. */
    public Vec3 moveTowardsXz(Vec3 target, double maxDistance) {
        double distance = distanceXz(target);
        if (distance <= maxDistance || distance == 0) {
            return new Vec3(target.x, 0, target.z);
        }
        double t = maxDistance / distance;
        return new Vec3(x + (target.x - x) * t, 0, z + (target.z - z) * t);
    }

    public Vec3 clampXz(double halfExtent) {
        return new Vec3(clamp(x, halfExtent), 0, clamp(z, halfExtent));
    }

    private static double clamp(double value, double halfExtent) {
        return Math.max(-halfExtent, Math.min(halfExtent, value));
    }

    /** Parses {@code {x, y, z}}; returns null if missing or any component is not a finite number. */
    public static Vec3 parse(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        JsonNode x = node.get("x");
        JsonNode z = node.get("z");
        JsonNode y = node.get("y");
        if (x == null || !x.isNumber() || z == null || !z.isNumber() || (y != null && !y.isNumber())) {
            return null;
        }
        Vec3 v = new Vec3(x.asDouble(), y == null ? 0 : y.asDouble(), z.asDouble());
        return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z) ? v : null;
    }
}
