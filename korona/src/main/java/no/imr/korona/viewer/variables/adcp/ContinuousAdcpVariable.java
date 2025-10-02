package no.imr.korona.viewer.variables.adcp;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.viewer.variables.ContinuousVariable;
import no.imr.korona.viewer.variables.ContinuousVariableResult;
import no.imr.korona.viewer.variables.ContinuousVariableSettings;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.logging.Level;

/**
 * Continuous variables available from {@link PowerData}.
 */
abstract class ContinuousAdcpVariable extends ContinuousVariable {
   private final String variablePath;

   ContinuousAdcpVariable(String variablePath, Name name, ContinuousVariableSettings settings, Unit unit, ExportTransform exportTransform) {
      super(AdcpVariableFactory.ADCP_VARIABLE_GROUP, name, settings, unit, exportTransform);

      this.variablePath = variablePath;
   }

   @Override
   public boolean isUsableInContext() {
      return true;
   }

   @Override
   public final ContinuousVariableResult evaluate(int channel, Ping ping) {
      AdcpLookup adcpLookup = AdcpData.lookup(variablePath, ping);
      if (adcpLookup == null) {
         return ContinuousVariableResult.EMPTY;
      }
      AdcpFile adcpFile = adcpLookup.adcpFile();
      Variable variable = adcpLookup.variable();
      int timeIndex = adcpLookup.timeIndex();
      try {
         synchronized (adcpFile.dataset) {
            float[] values = evaluate(variable, timeIndex);
            return new ContinuousVariableResult(values, adcpFile.depthRange(timeIndex, values.length));
         }
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error reading " + variable.getFullName()
               + " from " + adcpFile.dataset.getLocation(), e);
         return ContinuousVariableResult.EMPTY;
      }
   }

   abstract float[] evaluate(Variable variable, int timeIndex) throws IOException;
}
