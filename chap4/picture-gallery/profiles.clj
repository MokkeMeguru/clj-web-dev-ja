{:profiles/dev
 {:env
  {:env "dev"
   :database-adapter "postgresql"
   :database-name "pic_gallery"
   :database-username "meguru"
   :database-password "emacs"
   :database-server-name "dev_db"
   :database-port-number "5432"
   :migrations-folder "migrations"
   :log-level "info"}}
 :profiles/test
 {:env
  {:env "dev"
   :database-adapter "postgresql"
   :database-name "pic_gallery"
   :database-username "meguru"
   :database-password "emacs"
   :database-server-name "test_db"
   :database-port-number "5432"
   :migrations-folder "migrations"
   :log-level "info"}}}
