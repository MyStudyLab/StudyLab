# Star schema

# --- !Ups

CREATE TABLE star (
  id         INT       NOT NULL AUTO_INCREMENT,
  user_id    INT       NOT NULL,
  journal_id INT       NOT NULL,
  time       TIMESTAMP NOT NULL DEFAULT current_timestamp,
  PRIMARY KEY (id),
  FOREIGN KEY (user_id) REFERENCES user (id),
  FOREIGN KEY (journal_id) REFERENCES journal (id)
);

# --- !Downs

DROP TABLE star;