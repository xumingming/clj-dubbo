(ns dubbo.core
  (:import [com.alibaba.dubbo.config ApplicationConfig
            ReferenceConfig RegistryConfig])
  (:import [com.alibaba.dubbo.rpc.service GenericService]))

(def ^{} registry-atom (atom nil))
(def ^{} services-atom (atom {}))

(defn create-registry
  "Creates a dubbo registry with the specified address."
  [address]
  (doto (RegistryConfig.)
    (.setAddress address)))

(defn set-registry!
  "Sets the dubbo registry"
  [address]
  (reset! registry-atom (create-registry address)))

(defn create-service
  "Creates a service instance for the specified service name."
  [service-name]
  (let [reference (doto (ReferenceConfig.)
                    (.setApplication (ApplicationConfig. "generic-consumer"))
                    (.setInterface service-name)
                    (.setGeneric true)
                    (.setRegistry @registry-atom))
        service (.get reference)]
    service))

(defn create-service-if-needed [service-name]
  (when (not (contains? @services-atom service-name))
    (let [service (create-service service-name)
          service-info {:name service-name
                        :obj service
                        :methods {}}]
      ;; put service into cache
      (swap! services-atom assoc-in [service-name] service-info))))

(defn get-service
  "Gets a service instance for the specified full-quantified service name"
  [service-name]
    (get-in @services-atom [service-name :obj]))

(defmacro def-service-method [service-name method-name param-types param-names]
  ;; create service
  (create-service-if-needed service-name)
  ;; add the method information into the services-atom
  (swap! services-atom assoc-in
         [service-name :methods method-name]
         {:param-types param-types})
  (let [method-name-sym (symbol method-name)
        param-names-syms (map symbol param-names)]
    `(defn ~method-name-sym [~@param-names-syms]
       (let [param-types# (get-in @services-atom
                                  [~service-name :methods
                                   ~method-name :param-types])
             service# (get-service ~service-name)]
         (.$invoke ^GenericService service# ~method-name
                         (into-array String param-types#)
                         (into-array Object [~@param-names-syms]))))))


(defn list-services
  "Lists all the dubbo services stub we defined."
  []
  (keys @services-atom))

(defn list-service-methods
  "Lists all the methods of a dubbo services we defined."
  [service-name]
  (keys (get-in @services-atom [service-name :methods])))

;; set the registry
(set-registry! "127.0.0.1:9090")
;; def the remote method stub
(def-service-method "com.alibaba.dubbo.demo.DemoService" "sayHello" ["java.lang.String"] ["name"])
#_(sayHello "xumingmingv")


