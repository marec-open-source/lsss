package no.imr.lsss.modules.interpretation;

import no.imr.lsss.database.tables.hibernate.Observation;
import no.imr.lsss.database.tables.hibernate.ObservationPK;
import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.lsss.database.tables.hibernate.ScatterData;
import no.imr.lsss.database.tables.hibernate.ScatterObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The result of {@link GridIntegrator#createScatters()}.
 */
final class GridIntegratorResult {
   final Map<ObservationPK, Observation> observations = new HashMap<>();
   final Map<Integer, ScatterObject> scatterObjects = new HashMap<>();
   final List<Scatter> scatters = new ArrayList<>();
   final List<ScatterData> scatterDatas = new ArrayList<>();

   GridIntegratorResult() {
   }
}
