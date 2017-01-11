(ns sample.component.store
  (:require [com.stuartsierra.component :as component]
            [clojure.string :as s]
            [meta-merge.core :refer [meta-merge]]
            ))

(defprotocol IStore
  (<get [this id])
  (put> [this id o]))


(defrecord Store []
  component/Lifecycle
  (start [component]
    (assoc component :data (atom {"foo" "bar"})))
  (stop [component]
    (dissoc component :data))
  IStore
  (<get [this id] (get @(:data this) id))
  (put> [this id o] (swap! (:data this) assoc id o)))


(defn construct-store []
  (map->Store {}))

