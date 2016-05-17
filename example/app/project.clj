(defproject app "0.1.0-alpha-SNAPSHOT"
  :description "dadysql micro service example  "
  :url "https://github.com/dadysql/example/dateservice"
  :license {:alias "Eclipse Public License"
            :url   "http://www.eclipse.org/legal/epl-v10.html"}
  :scm {:dir ".."}

  :main app.core
  :repl-options {:init-ns user}
  :clean-targets ^{:protect false} [:target-path :compile-path "resources/public/js"]

  :dependencies [[org.clojure/clojure "1.8.0-RC5"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]

                 [ring "1.4.0"
                  :exclusions [ring/ring-jetty-adapter]]

                 [compojure "1.1.6"]
                 [org.immutant/web "2.1.3"                  ;; default Web server
                  :exclusions [ch.qos.logback/logback-core
                               org.slf4j/slf4j-api]]
                 [ch.qos.logback/logback-classic "1.1.3"]

                 [com.h2database/h2 "1.3.154"]
                 [c3p0/c3p0 "0.9.1.2"]

                 [dadysql "0.1.0-alpha-SNAPSHOT"]
                 [dadysql-http "0.1.0-SNAPSHOT"]

                 [org.clojure/clojurescript "1.7.228" :scope "provided"]
                 [devcards "0.2.1-5" :scope "provided"]
                 ]

  :plugins [[lein-cljsbuild "1.1.1"]
            [lein-figwheel "0.5.0-6"]]

  :figwheel {:server-port    3001                           ;; default
             :css-dirs       ["resources/public/css"]       ;; watch and update CSS
             :ring-handler   user/http-handler
             :server-logfile "target/figwheel.log"}

  :cljsbuild {:builds
              {:app
               {:figwheel {:devcards true}
                :compiler {:main                 app.core
                           :asset-path           "js/compiled/out"
                           :output-dir           "resources/public/js/compiled/out"
                           :output-to            "resources/public/js/compiled/app.js"
                           :source-map-timestamp true}}}}

  :profiles {:dev     {:source-paths ["dev"]
                       :dependencies [[figwheel "0.5.0-6"]
                                      [figwheel-sidecar "0.5.0-6"]
                                      [com.cemerick/piggieback "0.2.1"]
                                      [org.clojure/tools.nrepl "0.2.12"]]
                       :repl-options {:port 4555}}})

