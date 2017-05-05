(ns sample.app-test
  (:require
   [com.stuartsierra.component :as component]
   [clojure.test :refer :all]
   [io.pedestal.test :refer :all]
   [sample.routes :refer [build-routes]]
   [sample.component.store :as store]
   [io.pedestal.http :as http]
   [cheshire.core :as json]))

(set! *warn-on-reflection* true)

(defn mock-store []
  (let [x (atom {})]
    (reify store/IStore
      (<get [this id] (get @x id))
      (put> [this id o] (swap! x assoc id o)))))


(defn test-service
  [store]
  (->> (build-routes store) 
       (assoc {::http/router :linear-search} ::http/routes)
       http/create-servlet
       ::http/service-fn))

(deftest heath-check-test
  (is (= (:body (response-for (test-service (mock-store)) :get "/ruok")) "imok")))

;;uuid-ish
(deftest request-guid
  (is (=
       36
       (-> (mock-store)
           test-service
           (response-for :get "/ruok")
           :headers
           (get "X-GUID")
           count))))


(deftest sample-routes
  (is (=
       (-> (test-service (mock-store))
           (response-for  :get "/ruok")
           :headers
           (get "Content-Type"))
       "text/plain")))


(deftest put-store
  (is (= [200 "ok"]
         ((juxt :status :body)
           (response-for (test-service (mock-store))
                               :post "/store/put/foo"
                               :headers {"Content-Type" "application/json"}        
                               :body "{\"a\":\"b\"}"
                               )))))

(deftest get-store
  (let [store (mock-store)]
    (store/put> store "foo" "ok")
    (is (= [200 "ok"]
           ((juxt :status :body)
            (response-for (test-service store)
                          :get "/store/get/foo"
                          ))))))


;; todo: how to pass query-params to test
#_(deftest sample-schema
  (let [[status1 body1] ((juxt :status :body)
                         (response-for (test-service (mock-store))
                                       :post "/sample"
                                       :headers {"Content-Type" "application/json"}
                                       :body "{\"name\": \"test\", \"id\": 7}"))
        [status2 body2] ((juxt :status :body)
                         (response-for (test-service (mock-store))
                                       :post "/sample"
                                       :headers {"Content-Type" "application/json"}
                                       :query-params {:opt "zzz"}
                                       :body "{\"name\": \"test\", \"id\": 8}"))
        hash1 (:hash (json/parse-string body1 true))
        hash2 (:hash (json/parse-string body2 true))]
    (is (= 200 status1))
    (is (= "7-test-abc" hash1))
    (is (= 200 status2))
    (is (= "8-test-zzz" hash2))))



#_(deftest echo-route
  (is (= (:body (response-for (test-service) :post "/echo"
                              :headers {"Content-Type" "application/json" }
                              :body "{\"hello\":\"world\"}"))
         "{\"hello\":\"world\"}")))

