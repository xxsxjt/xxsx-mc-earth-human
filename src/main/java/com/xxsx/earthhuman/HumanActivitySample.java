package com.xxsx.earthhuman;

public record HumanActivitySample(
        int walkingCm,
        int sprintingCm,
        int swimmingCm,
        int climbingCm,
        int ridingCm,
        int elytraCm,
        int flyingCm) {
    public static final HumanActivitySample EMPTY = new HumanActivitySample(0, 0, 0, 0, 0, 0, 0);

    private static final double WALK_REFERENCE_BLOCKS = 4.3D;
    private static final double SPRINT_REFERENCE_BLOCKS = 5.6D;
    private static final double SWIM_REFERENCE_BLOCKS = 3.0D;
    private static final double CLIMB_REFERENCE_BLOCKS = 2.0D;
    private static final double RIDE_REFERENCE_BLOCKS = 8.0D;
    private static final double ELYTRA_REFERENCE_BLOCKS = 15.0D;
    private static final double FLY_REFERENCE_BLOCKS = 8.0D;

    public HumanActivitySample {
        walkingCm = Math.max(0, walkingCm);
        sprintingCm = Math.max(0, sprintingCm);
        swimmingCm = Math.max(0, swimmingCm);
        climbingCm = Math.max(0, climbingCm);
        ridingCm = Math.max(0, ridingCm);
        elytraCm = Math.max(0, elytraCm);
        flyingCm = Math.max(0, flyingCm);
    }

    public HumanActivityType dominantType() {
        HumanActivityType type = HumanActivityType.IDLE;
        double score = 0.02D;
        if (walkingFactor() > score) {
            type = HumanActivityType.WALKING;
            score = walkingFactor();
        }
        if (sprintingFactor() > score) {
            type = HumanActivityType.SPRINTING;
            score = sprintingFactor();
        }
        if (swimmingFactor() > score) {
            type = HumanActivityType.SWIMMING;
            score = swimmingFactor();
        }
        if (climbingFactor() > score) {
            type = HumanActivityType.CLIMBING;
            score = climbingFactor();
        }
        if (ridingFactor() > score) {
            type = HumanActivityType.RIDING;
            score = ridingFactor();
        }
        if (elytraFactor() > score) {
            type = HumanActivityType.ELYTRA;
            score = elytraFactor();
        }
        if (flyingFactor() > score) {
            type = HumanActivityType.FLYING;
        }
        return type;
    }

    public boolean hasMovement() {
        return (long)walkingCm + sprintingCm + swimmingCm + climbingCm + ridingCm + elytraCm + flyingCm > 10L;
    }

    public boolean hasPhysicalExertion() {
        return (long)walkingCm + sprintingCm + swimmingCm + climbingCm > 10L
                || elytraCm > 100
                || flyingCm > 100;
    }

    public double exertionGain() {
        return walkingFactor() * 0.18D
                + sprintingFactor() * 0.46D
                + swimmingFactor() * 0.40D
                + climbingFactor() * 0.38D
                + ridingFactor() * 0.01D
                + elytraFactor() * 0.06D
                + flyingFactor() * 0.03D;
    }

    public double fatigueGain(double exertionLoad) {
        double load = HumanStatus.clamp(exertionLoad, 0.0D, 100.0D);
        return walkingFactor() * (0.13D + load * 0.0025D)
                + sprintingFactor() * (0.46D + load * 0.0060D)
                + swimmingFactor() * (0.38D + load * 0.0050D)
                + climbingFactor() * (0.34D + load * 0.0045D)
                + ridingFactor() * 0.01D
                + elytraFactor() * (0.045D + load * 0.0010D)
                + flyingFactor() * (0.02D + load * 0.0005D);
    }

    public double hydrationExtraLoss() {
        return walkingFactor() * 0.010D
                + sprintingFactor() * 0.035D
                + swimmingFactor() * 0.025D
                + climbingFactor() * 0.025D
                + elytraFactor() * 0.005D
                + flyingFactor() * 0.003D;
    }

    public double walkingFactor() {
        return factor(walkingCm, WALK_REFERENCE_BLOCKS);
    }

    public double sprintingFactor() {
        return factor(sprintingCm, SPRINT_REFERENCE_BLOCKS);
    }

    public double swimmingFactor() {
        return factor(swimmingCm, SWIM_REFERENCE_BLOCKS);
    }

    public double climbingFactor() {
        return factor(climbingCm, CLIMB_REFERENCE_BLOCKS);
    }

    public double ridingFactor() {
        return factor(ridingCm, RIDE_REFERENCE_BLOCKS);
    }

    public double elytraFactor() {
        return factor(elytraCm, ELYTRA_REFERENCE_BLOCKS);
    }

    public double flyingFactor() {
        return factor(flyingCm, FLY_REFERENCE_BLOCKS);
    }

    public double factor(HumanActivityType type) {
        return switch (type) {
            case WALKING -> walkingFactor();
            case SPRINTING -> sprintingFactor();
            case SWIMMING -> swimmingFactor();
            case CLIMBING -> climbingFactor();
            case RIDING -> ridingFactor();
            case ELYTRA -> elytraFactor();
            case FLYING -> flyingFactor();
            default -> 0.0D;
        };
    }

    private static double factor(int distanceCm, double referenceBlocks) {
        return HumanStatus.clamp(distanceCm / 100.0D / referenceBlocks, 0.0D, 2.0D);
    }
}
