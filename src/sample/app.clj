(ns sample.app
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.log :as log]
            [meta-merge.core :refer [meta-merge]]
            [sample.runtime :as runtime]
            [sample.system :as sys]
            [sample.config :as config]
            [clojure.string :as s]
            )
  (:gen-class))

(set! *warn-on-reflection* true)

(defn print-info
  " Prints the system info and a Cool logo "
  [system]
  (letfn [(fn-str [& args] (s/join \newline args))]
    (print (fn-str
            ""
            ""
            "                  :ydmmmmmmhhyssooooooosyhmmmmmdho-                        "
            "                 sMMMMMMMMMMMMMMMo..hMMMMMMMMMMMMMMh`                      "
            "                `NMMMMMMMMMMMMMMMmyyNMMMMMMMMMMMMMMM+                      "
            "                 dMMMMMMMMMMMMMMM:  oMMMMMMMMMMMMMMM/                      "
            "                 :NMMMMMMMMMMMMMs   `mMMMMMMMMMMMMMd`                      "
            "                  -dMMMMMMMMMMm/     `yMMMMMMMMMMMd.                       "
            "                   `-ohmNNNdy/`        -sdNMMMNmy/`                        "
            "                  .dd                     `..`` +d:                        "
            "                  .NN:                          hMy                        "
            "                  -MMm`    .ohdhs/.-+yhhy/`    :MMy                        "
            "                  +MMMy  .yNMMMMMMMMMMMMMMm+` `dMMd                        "
            "                  oMMMMhyNMs.:+oss/+o++:.-MMdsmMMMm`                       "
            "                  hMMMMMMMMh -osyyyyhys/ :MMMMMMMMN.                       "
            "                  yMMMMMMMMM/    ydd-   .dMMMMMMMMM-                       "
            "                  sMMMMMMMMMMmyo+mMMyoshNMMMMMMMMMM-                       "
            "                  +MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMN.                       "
            "                  .mMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMs                        "
            "                   hMMMMMMMMMMMMMMMMMMMMMMMMMMMMMN-                        "
            "                   -dMMMMMMMMMMMMMMMMMMMMMMMMMMMMy                         "
            "                    `hMMMMMMMMMMMMMMMMMMMMMMMMMNd.                         "
            "                     `oMMMMMMMMMMMMMMMMMMMMMMMd-                           "
            "                       :yNMMMMMMMMMMMMMMMMMMh-`                            "
            "                         `/ymMMMMMMMMMMMNdo-                               "
            "                             .:///sys+/.`                                  "
            ""
            ""
            ""))
    
    (println "Starting HTTP server on port" (-> system :http :port))))

   
(def prod-config
  {:http {:env :prod}
   :neo4j {:env :prod}})

(def config
  (meta-merge config/defaults
              config/environ
              prod-config))

(defn -main
  [& args]
  (let [system (sys/new-system config)]
    (print-info system)
    (runtime/set-default-uncaught-exception-handler!
     (fn [thread e]
       (log/error :message "Uncaught exception, system exiting."
                  :exception e
                  :thread thread)
       (System/exit 1)))
    (runtime/add-shutdown-hook!
     ::stop-system #(do
                      (log/info :message "System exiting, running shutdown hooks.")
                      (component/stop system)))
    (component/start system)))
