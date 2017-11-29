# Journal schema

# --- !Ups

CREATE TABLE journal (
  id      INT       NOT NULL AUTO_INCREMENT,
  user_id INT       NOT NULL,
  text    TEXT      NOT NULL,
  public  BOOL      NOT NULL DEFAULT FALSE,
  pos     POINT,
  time    TIMESTAMP NOT NULL DEFAULT current_timestamp,
  PRIMARY KEY (id),
  FOREIGN KEY (user_id) REFERENCES user (id)
);

# --- !Downs

DROP TABLE journal;