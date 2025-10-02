package no.imr.korona.region;

/**
 * Used as return value for functions used when inserting a layer boundary.
 */
public record LayerAndBoundaryPair<T extends LayerBoundary>(Layer layer, T boundary) {
}
