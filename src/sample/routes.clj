(ns sample.routes
  (:require
   [clojure.java.io :as io]
   [sample.component.store :as store]
   [com.stuartsierra.component :as component]
   [io.pedestal.http.body-params :as body-params]
   [io.pedestal.http.content-negotiation :refer [negotiate-content]]
   [io.pedestal.http.ring-middlewares :as ring-mw]
   [io.pedestal.http :as bootstrap ]
   [io.pedestal.interceptor :refer [interceptor]]
   [io.pedestal.interceptor.chain :refer [terminate]]
   [io.pedestal.http.route :as route]
   [ring.util.response :refer [response not-found content-type]]))

;;(def supported-types ["text/html" "application/edn" "application/json" "text/plain"])

(def attach-guid
  " Example Interceptor Adds a GUID to Request Context"
  (interceptor
   {:name ::attach-guid
    :enter (fn [context]
             (let [guid (java.util.UUID/randomUUID)]
               (println "Request GUID: " guid)
               (assoc context ::guid guid)
               ))}))

(def ruok
  " Simple Interceptor to check if service is up"
  (interceptor {:name ::ruok
                :enter (fn [context]
                         (assoc context :response
                                {:status 200
                                 :body "imok"
                                 :headers {"Content-Type" "text/html"
                                           "X-GUID" (str (::guid context))}
                                 }))}))

(def get-from-store
  " Get an Item from the Store"
  (interceptor
   {:name ::get-from-store
    :enter (fn [context]
             (assoc context :response
                    {:status 200
                     :body (-> context ::dbval)
                     }
                    ))}))

(def lookup-by-id
  " Pulls Item from Store and adds it to context.  Terminates if not found and throws 404"
  (interceptor
   {:name ::lookup-by-id
    :enter (fn [context]
             (let [k (-> context :request :path-params :key)
                   v (store/<get (-> context :request :store) k)]
               (if-not v
                 (-> context
                     terminate
                     (assoc :response (not-found (str "key not found " k))))
                 (assoc context ::dbval v))))}))


(def put-to-store
  " Add an Item to the Store "
  (interceptor
   {:name ::put-to-store
    :enter (fn [context]
             (let [store (-> context :request :store)
                   k (-> context :request :path-params :key)
                   v (-> context :request :json-params)]
               (if-not v
                 (-> context
                     terminate
                     (assoc :response (not-found (str " json object not posted " v))))
                 (do
                   (store/put> store k v)
                   (assoc context :response
                          (response "ok"))
                   ))))}))


(defn add-store
  " Adds a store to context"
  [store]
  (interceptor {:name  ::add-store
                :enter (fn [context] (assoc-in context [:request :store] store))}))

;; TODO: coerce content types
;;http://pedestal.io/guides/hello-world-content-types#_returning_to_content_types 
(defn build-routes
  [store]
  (route/expand-routes
   [[
     ["/" ^:interceptors [attach-guid
                          (body-params/body-params)]
      ["/ruok"  {:get ruok}]
      ["/store" ^:interceptors [(add-store store)]
       ["/get/:key" ^:interceptors [lookup-by-id] {:get get-from-store}]
       ["/put/:key" ^:interceptors [(negotiate-content ["application/json"])] {:post put-to-store}]
       ]
      ]
     ]]))


(defrecord Routes [routes graphstore]
  component/Lifecycle
  (start [this]
    (assoc this :routes (build-routes (:store this))))
  (stop [this] this))

(defn construct-routes []
  (map->Routes {}))
