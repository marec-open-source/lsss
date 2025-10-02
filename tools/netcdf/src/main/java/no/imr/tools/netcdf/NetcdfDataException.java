package no.imr.tools.netcdf;

import java.io.IOException;

public final class NetcdfDataException extends IOException {
   public NetcdfDataException(String message) {
      super(message);
   }

   public NetcdfDataException(String message, Throwable cause) {
      super(message, cause);
   }
}
