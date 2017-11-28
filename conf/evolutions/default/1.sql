# User schema

# --- !Ups

CREATE TABLE user (

  id    INT          NOT NULL AUTO_INCREMENT,
  first VARCHAR(255),
  last  VARCHAR(255),
  email VARCHAR(255) NOT NULL,
  about VARCHAR(255),
  PRIMARY KEY (id)

);

# --- !Downs

DROP TABLE user;