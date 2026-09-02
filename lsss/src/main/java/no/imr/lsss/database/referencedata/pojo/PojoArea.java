package no.imr.lsss.database.referencedata.pojo;

import org.jspecify.annotations.Nullable;

import java.util.List;

public record PojoArea(
      short nation,
      int areaId,
      String areaName,
      @Nullable List<Integer> acousticCategories
) {
}
