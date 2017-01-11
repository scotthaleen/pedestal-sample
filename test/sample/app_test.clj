(ns sample.app-test
  (:require
   [com.stuartsierra.component :as component]
   [clojure.test :refer :all]
   [io.pedestal.test :refer :all]
   [sample.routes :refer [build-routes]]
   [sample.component.store :as store]
   [io.pedestal.http :as http]))

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
       "text/html")))


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
                          ))))
    
    )


  )

#_(deftest echo-route
  (is (= (:body (response-for (test-service) :post "/echo"
                              :headers {"Content-Type" "application/json" }
                              :body "{\"hello\":\"world\"}"))
         "{\"hello\":\"world\"}")))

