# Pedestal Sample Project

[![Build Status](https://travis-ci.org/scotthaleen/pedestal-sample.svg?branch=master)](https://travis-ci.org/scotthaleen/pedestal-sample)

### Project Setup

```
brew install leiningen
```

Create a **profiles.clj** in the root of the project and add custom overrides 

```clojure
{ :profiles/dev { :jvm-opts ["-Dhttp.port=8080"] }}
```

### Quick Start

```
$ lein run

$ curl localhost:4000/ruok
;;=> imok%

```
### Dev 

[Clojure Reload Workflow](http://thinkrelevance.com/blog/2013/06/04/clojure-workflow-reloaded)

```
lein repl

;; start application
user=> (go)

;; refesh code
user=> (reset)

;; run tests
user=> (test)
```

### Deployable

```
lein uberjar

$ java -cp target/sample-web-*-standalone.jar -Dhttp.port=8080 sample.app

$ curl localhost:8080/ruok
```


### Examples


```
$ curl localhost:4000/ruok
imok%

$ curl localhost:4000/store/get/foo
bar%

$ curl localhost:4000/store/get/foo1
key not found foo1%

$ curl -X POST localhost:4000/store/put/foo1 -d '{"foo1": "42"}'
 json object posted %

$ curl -X POST -H "Content-type: application/json" localhost:4000/store/put/foo1 -d '{"foo1": "42"}'
ok%

$ curl localhost:4000/store/get/foo1
{:foo1 "42"}%
```


Documentation: 

[12factor.net](https://12factor.net) <br/>
[Pedestal](https://github.com/pedestal/pedestal) <br/>
[Component](https://github.com/stuartsierra/component) <br/>
