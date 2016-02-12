(defproject cljs-app "0.1.0-alpha-SNAPSHOT"
  :description "Clojure database access framework"
  :url "https://github.com/Mamun/tiesql/example/cljs-app"
  :license {:alias "Eclipse Public License"
            :url   "http://www.eclipse.org/legal/epl-v10.html"}
  :scm {:dir ".."}

  :clean-targets ^{:protect false} [:target-path :compile-path "resources/public/js/compiled"]

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/data.json "0.2.6"]

                 [ring "1.4.0"
                  :exclusions [ring/ring-jetty-adapter
                               ring/ring-devel]]

                 [ring-middleware-format "0.6.0"
                  :exclusions [ring
                               org.clojure/core.memoize
                               org.clojure/tools.reader]]
                 [compojure "1.1.6"]
                 [c3p0/c3p0 "0.9.1.2"]
                 [json-html "0.3.6"]
                 [hiccup "1.0.5"]
                 [ring-webjars "0.1.1"]
                 [org.webjars/bootstrap "3.3.5"]

                 [tiesql "0.1.0-alpha-SNAPSHOT"]
                 [com.stuartsierra/component "0.2.3"]

                 [org.immutant/web "2.1.0"                  ;; default Web server
                  :exclusions [ch.qos.logback/logback-core
                               org.slf4j/slf4j-api]]
                 [ch.qos.logback/logback-classic "1.1.3"]]

  :profiles {:dev  {:repl-options   {:port 4555}
                    :source-paths   ["dev"]
                    :resource-paths ["resources" "../../test-i"]
                    :main         app
                    :dependencies   [[org.clojure/tools.namespace "0.2.11"]
                                     [org.clojure/tools.nrepl "0.2.10"]
                                     [com.h2database/h2 "1.3.154"]
                                     [clj-http "2.0.0"]


                                     [org.clojure/clojurescript "1.7.228"]
                                     [figwheel-sidecar "0.5.0-3"]
                                     ;[clj-http "2.0.0"]
                                     [com.cognitect/transit-clj "0.8.283"]
                                     [com.cognitect/transit-cljs "0.8.225"]
                                     [devcards "0.2.1-5"]
                                     [cljs-ajax "0.5.2"]]}

             ;export LEIN_SNAPSHOTS_IN_RELEASE=1
             :prod {:main         app
                    :aot          [app]
                    :omit-source  true
                    :uberjar-name "app-boot.jar"
                    :manifest     {"Class-Path" "lib lib/*"}}})

