package no.imr.tools.adm;

import java.time.LocalDate;
import java.util.List;

public record LicenseInfo(
      String id,
      String licensedTo,
      LocalDate expiration,
      List<String> features
) {
}
