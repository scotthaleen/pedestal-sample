(ns sample.config
  (:require [environ.core :refer [env]]))

(set! *warn-on-reflection* true)

(def defaults
  {:http {:port 3000}})

;; Command Line Java Opts
;; http.port

;; Pull configuration from Environment 
(def environ
  {:http {:port (some-> env ^String (:http-port) Integer.)
          ;;:file-path (some-> env ^String (:http-public-dir))
          }
   :some-other-compoment {
                          :url (some-> env ^String (:url))
                          }})
