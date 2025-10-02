package no.imr.korona.region;

import no.imr.korona.data.util.geometry.EchogramPoint;
import org.jspecify.annotations.Nullable;

/**
 * Used as return value for functions used when splitting a layer boundary.
 *
 * @see LayerBoundary#split(EchogramPoint)
 */
record LayerBoundaryAndConnectorPair<T extends LayerBoundary>(@Nullable T endBoundary, LayerConnector connector) {
}
