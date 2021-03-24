CREATE TABLE pic_images (
       blob text PRIMARY KEY,
       id uuid REFERENCES pics(id) ON DELETE CASCADE NOT NULL,
       index integer NOT NULL
);
