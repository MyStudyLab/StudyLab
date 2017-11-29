# User schema

# --- !Ups

CREATE TABLE user (

  id       INT          NOT NULL AUTO_INCREMENT,
  username VARCHAR(255) NOT NULL UNIQUE,
  first    VARCHAR(255),
  last     VARCHAR(255),
  email    VARCHAR(255) NOT NULL,
  joined   TIMESTAMP    NOT NULL DEFAULT current_timestamp,
  about    VARCHAR(255),
  PRIMARY KEY (id)

);

# --- !Downs

DROP TABLE user;