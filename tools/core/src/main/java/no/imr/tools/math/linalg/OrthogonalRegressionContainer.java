package no.imr.tools.math.linalg;

/**
 * An immutable class holding results from orthogonal regression.
 * <ol>
 * <li>the direction</li>
 * <li>the ratio of between the first and second singular value</li>
 * </ol>
 * A ratio of 1 means they are equal and the direction is not trustworthy, higher ratios indicate high
 * confidence in extracted direction
 */
public record OrthogonalRegressionContainer(Vec3 direction, float ratio) {
}
