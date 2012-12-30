(ns dubbo.core
  (:import [java.util HashMap])
  (:import [com.alibaba.dubbo.config ApplicationConfig
            ReferenceConfig RegistryConfig])
  (:import [com.alibaba.dubbo.rpc.service GenericService]))

(def ^{} registry-atom (atom nil))
(def ^{} services-atom (atom {}))
(declare list-services)

(defn create-registry
  "Creates a dubbo registry with the specified address."
  [address]
  (doto (RegistryConfig.)
    (.setAddress address)))

(defn set-registry!
  "Sets the dubbo registry"
  [address]
  (reset! registry-atom (create-registry address)))

(defn create-service-reference0
  "Creates a service instance for the specified service name."
  [registry service-name]
  (let [reference (doto (ReferenceConfig.)
                    (.setApplication (ApplicationConfig. "dubbo.clj"))
                    (.setInterface service-name)
                    (.setGeneric true)
                    (.setRegistry registry))]
    reference))

(defn create-service-reference
  [service-name]
  (create-service-reference0 @registry-atom service-name))

(defn service-exists? [service-name]
  (contains? @services-atom service-name))

(defn create-service-if-needed [service-name]
  (when (not (service-exists? service-name))
    (let [reference (create-service-reference service-name)
          service-info {:name service-name
                        :obj reference
                        :methods {}}]
      ;; put service into cache
      (swap! services-atom assoc-in [service-name] service-info))))

(defn get-service
  "Gets a service instance for the specified full-quantified service name"
  [service-name]
    (.get (get-in @services-atom [service-name :obj])))

(defn close-service [service-name]
  (let [service-reference (get-in @services-atom [service-name :obj])]
    ;; close the service reference
    (.destroy service-reference)
    ;; remove it from the service-atom
    (swap! services-atom dissoc service-name)))

(defn close-all-services []
  (let [all-service-names (list-services)]
    (doseq [service-name all-service-names]
      (close-service service-name))))

(defn clojurify [obj]
  ;; if its a map, we prettify it:
  ;; 1) string key -> keyword key
  ;; 2) HashMap -> clojure map
  (if (instance? HashMap obj)
    (let [ret (map (fn [[k v]] [(keyword k) v]) obj)
          ret (into {} ret)]
      ret)
    obj))

(defn javarify [param]
  (if (map? param)
    (let [ret (map (fn [[k v]] [(name k) v]) param)
          java-map (HashMap.)]
      (doseq [[k v] ret]
        (.put java-map k v))
      java-map)
    param))

(defn add-service-method
  [services-atom service-name method-name param-types]
    ;; add the method information into the services-atom
  (swap! services-atom assoc-in
         [service-name :methods method-name]
         {:param-types param-types}))

(defmacro def-service-method
  "Defines a remote service method as local function in current namespace.

  service-name  Full qualified service name. e.g. com.alibaba.dubbo.demo.DemoService
  method-name   Method name in the service. e.g. sayHello
  param-types   Parameter types of this method. e.g. [\"java.lang.String\"]
  param-names   Parameter names of this method. e.g. [\"name\"]
                This is mainly for call the function conviniently.

  Example:
    (def-service-method \"com.alibaba.dubbo.demo.DemoService\"
                        \"sayHello\"
                        [\"java.lang.String\"]
                        [\"name\"])
"
  [service-name method-name param-types param-names]
  ;; create service
  (create-service-if-needed service-name)
  ;; add the method info
  (add-service-method services-atom service-name method-name param-types)
  ;; define the local function stub
  (let [method-name-sym (symbol method-name)
        param-names-syms (map symbol param-names)]
    `(defn ~method-name-sym [~@param-names-syms]
       (let [param-types# (get-in @services-atom
                                  [~service-name :methods
                                   ~method-name :param-types])
             service# (get-service ~service-name)
             ;; javarify param
             params# [~@param-names-syms]
             params# (map javarify params#)
             result# (.$invoke ^GenericService service# ~method-name
                         (into-array String param-types#)
                         (into-array Object params#))
             ;; prettify the result
             result# (clojurify result#)]
         result#))))


(defn list-services
  "Lists all the dubbo services stub we defined."
  []
  (keys @services-atom))

(defn list-service-methods
  "Lists all the methods of a dubbo services we defined."
  [service-name]
  (keys (get-in @services-atom [service-name :methods])))

