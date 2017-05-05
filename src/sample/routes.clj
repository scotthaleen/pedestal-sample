(ns sample.routes
  (:require
   [clojure.java.io :as io]
   [sample.component.store :as store]
   [com.stuartsierra.component :as component]
   ;;[io.pedestal.http.body-params :as body-params]
   [io.pedestal.http.content-negotiation :refer [negotiate-content]]
   [io.pedestal.http.ring-middlewares :as ring-mw]
   [io.pedestal.http :as bootstrap ]
   [io.pedestal.interceptor :refer [interceptor]]
   [io.pedestal.interceptor.chain :refer [terminate]]
   [io.pedestal.http.route :as route]
   [schema.core :as s]
   [route-swagger.doc :as doc]
   [ring.swagger.json-schema :as rjs]
   [pedestal-api
    [core :as api]
    [swagger :as swagger]]
   [ring.util.response :refer [response not-found content-type]]))

;;(def supported-types ["text/html" "application/edn" "application/json" "text/plain"])

#_(defn valid? [schema value]
    (try
      [true (s/validate schema value)]
      (catch clojure.lang.ExceptionInfo e
        [false (:cause (Throwable->map e))])))


(s/defschema ResponseJSON {s/Keyword s/Any})

(defn- context-terminate
  " Helper function to terminate a request "
  [ctx status reason]
  (-> ctx
      terminate
      (assoc :response
             {:status status
              :body reason})))

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
  (swagger/annotate
   {:summary    "Health Check "
    :description " Simple Interceptor to check if service is up"
    :parameters {}
    :responses  {200 {:body s/Str}}}
   (interceptor {:name ::ruok
                 :enter (fn [context]
                          (assoc context :response
                                 {:status 200
                                  :body "imok"
                                  :headers {"X-GUID" (str (::guid context))}
                                  }))})))

(s/defschema SampleRequestSchema 
  {:name s/Str
   :id s/Int})

(s/defschema SampleResponseSchema
  (merge SampleRequestSchema
         {:hash s/Str}))


(def sample-schema
  (swagger/annotate
   {:summary    "Sample Schema Request/Response"
    :description " Example of schema validation "
    :parameters {:body-params SampleRequestSchema
                 :query-params {(s/optional-key :opt) (rjs/field s/Str {:description "random string default: abc "})}}
    :responses  {200 {:body SampleResponseSchema}}}
   (interceptor
    {:name ::sample-schema
     :enter (fn [context]
              (let [[name id] ((juxt :name :id) (-> context :request :body-params)) 
                    opt-string (get-in context [:request :query-params :opt] "abc")]
                (assoc context :response
                       {:status 200
                        :body {:name name
                               :id id 
                               :hash (str id "-" name "-" opt-string)}}
                       )))})))


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
                   v (-> context :request :body-params)]
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

(defn routes*
  " Add Swagger Documentation to Routes "
  [doc routes]
  (-> routes
      route/expand-routes
      (doc/with-swagger doc)))

;; TODO: coerce content types
;;http://pedestal.io/guides/hello-world-content-types#_returning_to_content_types 
(defn build-routes
  [store]
  (routes*
   {:info {:title "Sample Web Server"
           :description " ... "
           :version "1.0"}
    :tags [{:name "health_check" :description " check if service is available "}]}
   [[["/" ^:interceptors [attach-guid
                          api/error-responses
                          (api/negotiate-response)
                          (api/body-params)
                          api/common-body
                          (api/coerce-request)
                          (api/validate-response)]
      ["/ruok"  {:get ruok}]
      ["/sample" {:post sample-schema}]
      ["/store" ^:interceptors [(add-store store)]
       ["/get/:key" ^:interceptors [lookup-by-id] {:get get-from-store}]
       ["/put/:key" ^:interceptors [(negotiate-content ["application/json"])] {:post put-to-store}]
       ]
      ["/swagger.json" {:get api/swagger-json}]
      ["/ui/*resource" {:get api/swagger-ui}]
      
      ]]]))


(defrecord Routes [routes graphstore]
  component/Lifecycle
  (start [this]
    (assoc this :routes (build-routes (:store this))))
  (stop [this] this))

(defn construct-routes []
  (map->Routes {}))
