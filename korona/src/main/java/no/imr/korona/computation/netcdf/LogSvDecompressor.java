package no.imr.korona.computation.netcdf;

import no.imr.korona.data.DataException;
import no.imr.korona.data.ping.items.channel.PowerData;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;

public final class LogSvDecompressor {
   private final short fillValue;
   private final float addOffset;
   private final float scaleFactor;

   public LogSvDecompressor(Variable variable) throws DataException {
      DataType dataType = variable.getDataType();
      if (dataType != DataType.USHORT) {
         throw new DataException("Cannot decompress data type " + dataType + " in variable " + variable.getFullName());
      }
      fillValue = getAttributeAsNumber(variable, CDM.FILL_VALUE).shortValue();
      addOffset = getAttributeAsNumber(variable, CDM.ADD_OFFSET).floatValue();
      scaleFactor = getAttributeAsNumber(variable, CDM.SCALE_FACTOR).floatValue();
   }

   private static Number getAttributeAsNumber(Variable variable, String attributeName) throws DataException {
      Attribute attribute = variable.attributes().findAttribute(attributeName);
      if (attribute == null) {
         throw new DataException("No attribute " + attributeName + " in variable " + variable.getFullName());
      }
      Number number = attribute.getNumericValue();
      if (number == null) {
         throw new DataException("Attribute " + attribute + " in variable " + variable.getFullName() + " is not a number");
      }
      return number;
   }

   public OffsetValues decompressToSv(short[] compressedValues) {
      int iEnd = compressedValues.length;
      while (iEnd > 0 && compressedValues[iEnd - 1] == fillValue) {
         iEnd--;
      }
      int iBegin = 0;
      while (iBegin < iEnd && compressedValues[iBegin] == fillValue) {
         iBegin++;
      }
      float[] sv = new float[iEnd - iBegin];
      for (int i = 0; i < sv.length; i++) {
         short compressedValue = compressedValues[i + iBegin];
         if (compressedValue == fillValue) {
            sv[i] = 0;
         } else {
            float logSv = (0xffff & compressedValue) * scaleFactor + addOffset;
            sv[i] = PowerData.logSvToSv(logSv);
         }
      }
      return new OffsetValues(iBegin, sv);
   }
}
