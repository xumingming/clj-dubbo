(ns dubbo.test.core
  (:use [clojure.test])
  (:use [dubbo.core])
  (:import [java.util HashMap])
  (:import [com.alibaba.dubbo.config ApplicationConfig
            ReferenceConfig RegistryConfig]))

;; set the registry
;;(set-registry! "127.0.0.1:9090")

;; def the remote method stub
;;(def-service-method "com.alibaba.dubbo.demo.DemoService" "sayHello" ["java.lang.String"] ["name"])

;;(def-service-method "com.alibaba.dubbo.demo.DemoService" "add" ["int" "int"] ["a" "b"])

;;(def-service-method "com.alibaba.dubbo.demo.DemoService" "findPerson" ["java.lang.String"] ["name"])

;;(def-service-method "com.alibaba.dubbo.demo.DemoService" "savePerson" ["com.alibaba.dubbo.demo.Person"] ["p"])
;;(sayHello "xumingmingv")

(deftest test-create-registry
  (let [address "127.0.0.1:9090"
        ^RegistryConfig registry (create-registry address)]
    (is (not (nil? registry)))
    (is (= address (.getAddress registry)))))

(deftest test-create-service-reference0
  (let [registry (create-registry "127.0.0.1:9090")
        service-name "com.test.DemoService"
        reference (create-service-reference0 registry service-name "1.0.0")]
    (is (not (nil? reference)))
    (is (= service-name (.getInterface reference)))
    (is (= "true" (.getGeneric reference)))))

(deftest test-clojurify
  (is (= 1 (clojurify 1)))
  (is (= "hello" (clojurify "hello")))
  (is (= {:name "james" :age 18} (clojurify (doto (HashMap.)
                           (.putAll {"name" "james" "age" 18}))))))

(deftest test-javarify
  (is (= 1 (javarify 1)))
  (is (= "hello" (javarify "hello")))
  (is (= {:name "james" :age 21} (clojurify (javarify {:name "james" :age 21}))))
  (is (= {:name "james" :age 21 :inner {"test" 1}} (clojurify (javarify {:name "james" :age 21 :inner {"test" 1}})))))