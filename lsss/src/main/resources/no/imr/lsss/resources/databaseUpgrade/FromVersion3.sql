-- Lines starting with -- are comments.
-- This file will be executed line by line.

-- SQL syntax may vary between databases.
-- The statements below are valid for PostgreSQL.
-- If necessary, please edit the statements according to your database.

-- Task 1: Rename column COMMENT to STANDARDCOMMENT for table STANDARDCOMMENT
ALTER TABLE STANDARDCOMMENT RENAME COLUMN COMMENT TO STANDARDCOMMENT

-- Task 2: Rename column COMMENT to SURVEYDESCRIPTION for table SURVEY
ALTER TABLE SURVEY RENAME COLUMN COMMENT TO SURVEYDESCRIPTION

-- Task 3: Rename column COMMENT to STANDARDCOMMENT for table COMMENT
ALTER TABLE COMMENT RENAME COLUMN COMMENT TO STANDARDCOMMENT

-- Task 4: Rename table COMMENT to OBSERVATIONCOMMENT
ALTER TABLE COMMENT RENAME TO OBSERVATIONCOMMENT

-- Task 5: Set database version to 4
UPDATE DBPARAMETER SET PARVALUE = '4' WHERE PARNAME = 'version'
