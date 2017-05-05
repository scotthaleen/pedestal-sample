(defproject sample-web "0.1.0-SNAPSHOT"
  :description " Sample Pedestal Web Server "
  :dependencies [
                 [org.clojure/clojure "1.8.0"]

                 
                 ;;[org.clojure/tools.cli "0.3.5"]
                 
                 ;; pedestal
                 [io.pedestal/pedestal.service "0.5.1"]
                 [io.pedestal/pedestal.jetty "0.5.1"]
                 [pedestal-api "0.3.1"]
                 [metosin/ring-swagger "0.23.0"]


                 ;;enviorment
                 [environ "1.1.0"]
                 [com.stuartsierra/component "0.3.2"]
                 [meta-merge "0.1.1"]

                 ;; Authentication
                 ;;[buddy "1.2.0"]

                 ;; joda time
                 ;; [clj-time "0.12.2"]

                 ;;[org.clojure/core.cache "0.6.5"]
                 ;;[org.clojure/core.async "0.2.395"]

                 ;;[com.rpl/specter "0.13.2"]

                 ;;Logging
                 [ch.qos.logback/logback-classic "1.1.8" :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.22"]
                 [org.slf4j/jcl-over-slf4j "1.7.22"]
                 [org.slf4j/log4j-over-slf4j "1.7.22"]]

  ;;:global-vars {*warn-on-reflection* true  *assert* false}
  :resource-paths ["resources"]
  :plugins [[lein-pprint "1.1.2"]
            [lein-environ "1.1.0"]
            [lein-auto "0.1.3"]
            ;;[lein-codox "0.10.1"]
            ]

  ;;:codox {:output-path "target/docs/"  :source-uri "http://github.com/{user}/{project}/blob/{version}/{filepath}#L{line}"}
  
  :default {:file-pattern #"\.(clj|cljs|cljc)$"}
  :main ^:skip-aot sample.app
  :jvm-opts ["-Xms1g" "-Xmx4g"]
  :target-path "target/%s/"
  :profiles {
             :dev [:project/dev :profiles/dev]
             :uberjar [:project/uberjar]
             :aot { :aot :all }             
             :project/dev {:dependencies [[reloaded.repl "0.2.1"]
                                          [org.clojure/tools.namespace "0.2.11"]
                                          [org.clojure/tools.nrepl "0.2.12"]
                                          [eftest "0.1.2"]]
                           :source-paths ["dev"]
                           :jvm-opts ["-Dhttp.port=4000"]
                           :repl-options {:init-ns user}
                           :env { }}
             :profiles/dev { }
             :project/uberjar {
                               :aot :all
                               :uberjar-name ~(str
                                               "sample-web-%s"
                                               (try
                                                 (str "-"
                                                      (clojure.string/trim
                                                       (let [{:keys [exit out err]}
                                                             (clojure.java.shell/sh "git"
                                                                                    "rev-parse"
                                                                                    "--short"
                                                                                    "HEAD")]
                                                         (if-not (= 0 exit)
                                                           (throw (Exception. err))
                                                           out))))
                                                 (catch Exception e
                                                   (println "WARNING: unable to add git revision " \newline "Cause: " (:cause (Throwable->map e)))))
                                               "-standalone.jar") 
                               }})
