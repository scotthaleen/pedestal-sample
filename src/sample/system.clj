(ns sample.system
  (:require [com.stuartsierra.component :as component]
            [meta-merge.core :refer [meta-merge]]
            [sample.component.pedestal :refer [construct-server]]
            [sample.routes :refer [construct-routes]]
            [sample.component.store :refer [construct-store]]))


(set! *warn-on-reflection* true)

(def base-config
  "
  :allowed-origins with a function of to always return true. This allows all origins for cors.
  
  see cors - https://github.com/pedestal/pedestal/blob/master/service/src/io/pedestal/http/cors.clj#L60
  "
  {:http {:env :dev
          :router :linear-search
          ;;:resource-path "/html"
          :allowed-origins (constantly true)
          ;;:file-path nil
          :type :jetty
          }
   })


;; understanding component models
;; https://github.com/stuartsierra/component

(defn new-system [config]
  (let [config (meta-merge base-config config)]
    (-> (component/system-map
         :config config
         :store (construct-store)
         :routes (construct-routes)
         :http   (construct-server (:http config)))
        (component/system-using
         {:routes {:store :store}
          :http   {:routes :routes} }))))
;;        ^        ^
;;        |        | 
;;        |        |
;;        |        \- Keys in the System Map
;;        |
;;        \- Keys in component
;;
