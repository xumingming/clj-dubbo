## dubbo.clj

call [Dubbo](https://github.com/alibaba/dubbo) service in clojure.

[Dubbo](https://github.com/alibaa/dubbo) is a distributed service framework created by [Alibaba](http://www.alibaba.com). This lib makes it possible to call existing java
service exposed by Dubbo using clojure AND in clojure way.

## Usage

Suppose we have the following service:

``` java
package com.alibaba.dubbo.demo;

public interface DemoService {
    String sayHello(String name);
    
    int add(int a, int b);
    
    Person findPerson(String name);
    
    String   savePerson(Person p);
}
```

And the implementation is:

``` java
package com.alibaba.dubbo.demo.provider;
 
import com.alibaba.dubbo.demo.DemoService;
import com.alibaba.dubbo.demo.Person;
 
public class DemoServiceImpl implements DemoService {
 
    public String sayHello(String name) {
        return "Hello " + name;
    }

    @Override
    public int add(int a, int b) {
        return a + b;
    }
 
    public Person findPerson(String name) {
        Person ret = new Person();
        ret.setAge(20);
        ret.setName(name);
        
        return ret;
    }

    @Override
    public String savePerson(Person p) {
        return "From server: name:" + p.getName() + ", age: " + p.getAge();
    }
}
```

* Define a remote service method:

``` clojure
(def-service-method "com.alibaba.dubbo.demo.DemoService" "sayHello" ["java.lang.String"] ["name"])
```

The params are:
** service name
** method name
** param types
** param names

This will define a function named `sayHello` which accepts a single param named `name` , when you call this function, it will delegate the call to the remote service method: `com.alibaba.dubbo.demo.DemoService#sayHello`

* Call this remote service method:

``` clojure
(sayHello "james")
;; "Hello james"
```

* Call a service method with more than one params.
Just treat it as a function with mutiple functions, e.g. following shows how we call the `add` method:

``` clojure
(add 1 2)
;; 3
```
* Call a service method whose param is a POJO.
For service method whose param is a POJO, you just need to treat the param as a map in clojure, e.g. following shows how we call the `savePerson` method:

``` clojure
(savePerson {:name "james" :age (int 12)})
;; "From server: name:james, age: 12"
```

* Call a service method whose return value is a POJO.
the POJO return value will be converted to a clojure map, and the property name whill be converted to keyword type, e.g. following shows how we call the `findPerson` method:

``` clojure
(findPerson "james")
;; {:name "james", :age 20, :class "com.alibaba.dubbo.demo.Person"}
```

## License

Copyright (C) 2012 xumingming

Distributed under the Eclipse Public License.
