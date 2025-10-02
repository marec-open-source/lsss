package no.imr.lsss.framework.extensions;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.InterpretationSettings;
import no.imr.tools.swing.WorkerDialog;
import no.marec.lsss.api.LsssAccess;
import no.marec.lsss.api.data.PingDataset;
import no.marec.lsss.api.echogram.EchogramData;
import no.marec.lsss.api.modules.EchogramPlot;
import no.marec.lsss.api.regions.Regions;
import no.marec.lsss.api.util.AsyncHandle;
import no.marec.lsss.api.util.Mouseover;
import no.marec.lsss.api.util.observing.Observable;

import java.util.function.Consumer;

public final class LsssAccessImpl implements LsssAccess {
   private final LSSS lsss;
   private PingDatasetImpl pingDataset = new PingDatasetImpl(DataFileSet.empty());
   private final EchogramDataImpl echogram;
   private final RegionsImpl regions;
   private final MouseoverImpl mouseover;
   private final EchogramPlotImpl echogramPlot;

   public LsssAccessImpl(LSSS lsss) {
      this.lsss = lsss;
      echogram = new EchogramDataImpl(lsss);
      regions = new RegionsImpl(lsss);
      mouseover = new MouseoverImpl(lsss);
      echogramPlot = new EchogramPlotImpl(lsss);
   }

   public void setup() {
      InterpretationSettings interpretationSettings = lsss.getInterpretationSettings();
      interpretationSettings.getDataFileChangeManager().addListener(dataFileSet -> pingDataset = new PingDatasetImpl(dataFileSet));
      echogram.setup();
   }

   LSSS getLsss() {
      return lsss;
   }

   @Override
   public PingDataset pingDataset() {
      return pingDataset;
   }

   @Override
   public EchogramData echogram() {
      return echogram;
   }

   @Override
   public Regions regions() {
      return regions;
   }

   @Override
   public Observable<?> reloadData() {
      return lsss.getInterpretationSettings().getReloadChangeManager();
   }

   @Override
   public Mouseover mouseover() {
      return mouseover;
   }

   @Override
   public EchogramPlot echogramPlot() {
      return echogramPlot;
   }

   @Override
   public void runCancellableTask(String message, Consumer<AsyncHandle> task) {
      new WorkerDialog(lsss::getReferenceComponent, message)
            .setModalDialog(false)
            .start(task::accept);
   }
}
