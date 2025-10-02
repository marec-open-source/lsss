package no.imr.lsss.database.tables.hibernate;

import no.imr.tools.database.hibernate.BaseCompPK;

public interface BaseNationPK extends BaseCompPK {
   short getNation();

   void setNation(short nation);
}
