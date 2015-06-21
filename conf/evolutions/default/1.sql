# --- !Ups

CREATE TABLE Transactions (
  id INTEGER  AUTO_INCREMENT PRIMARY KEY,
  body VARCHAR
);

# --- !Downs

DROP TABLE IF EXISTS Transactions;