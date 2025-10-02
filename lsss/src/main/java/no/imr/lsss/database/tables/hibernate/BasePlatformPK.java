package no.imr.lsss.database.tables.hibernate;

public interface BasePlatformPK extends BaseNationPK {
   short getPlatform();

   void setPlatform(short platform);
}
