CREATE TABLE users (
       id varchar(12) PRIMARY KEY,
       auth_token varchar(64) NOT NULL,
       created_at TIMESTAMP default CURRENT_TIMESTAMP,
       updated_at TIMESTAMP,
       is_deleted BOOLEAN NOT NULL default FALSE
);
