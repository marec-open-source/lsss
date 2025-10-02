package no.imr.korona.computation.tracking.data;

import no.imr.tools.math.linalg.Vec3;

public record StateVector(Vec3 position, Vec3 velocity, float ts) {
}
