CREATE TABLE pics (
       id uuid PRIMARY KEY,
       user_id varchar(15) NOT NULL REFERENCES users(id),
       title varchar(128) NOT NULL,
       description varchar(1024),
       created_at TIMESTAMP default CURRENT_TIMESTAMP,
       updated_at TIMESTAMP,
       is_deleted BOOLEAN NOT NULL default FALSE,
       tcc_state tcc_state NOT NULL
);
