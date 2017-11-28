# Journal schema

# --- !Ups

CREATE TABLE journal (
  id      INT  NOT NULL AUTO_INCREMENT,
  user_id INT  NOT NULL,
  text    TEXT NOT NULL,
  PRIMARY KEY (id),
  FOREIGN KEY (user_id) REFERENCES user (id)
);

# --- !Downs

DROP TABLE journal;