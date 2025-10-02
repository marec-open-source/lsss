-- Lines starting with -- are comments.
-- This file will be executed line by line.

-- SQL syntax may vary between databases.
-- The statements below are valid for PostgreSQL, JavaDB/Derby and HSQLDB.
-- If necessary, please edit the statements according to your database.

-- Task 1: Create table SurveyInfo
CREATE TABLE SURVEYINFO (NATION SMALLINT NOT NULL, PLATFORM SMALLINT NOT NULL, SURVEY INTEGER NOT NULL, INFOKEY VARCHAR(40) NOT NULL, INFOINDEX INTEGER NOT NULL, INFOVALUE VARCHAR(30000), PRIMARY KEY(NATION, PLATFORM, SURVEY, INFOKEY, INFOINDEX))
ALTER TABLE SURVEYINFO ADD CONSTRAINT FKISKKCATFVC558FVA4IQ1XC55S FOREIGN KEY (NATION, PLATFORM, SURVEY) REFERENCES SURVEY

-- Task 2: Set database version to 5
UPDATE DBPARAMETER SET PARVALUE = '5' WHERE PARNAME = 'version'
