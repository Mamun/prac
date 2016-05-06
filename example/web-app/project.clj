(defproject web-app "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0-RC5"]
                 [org.clojure/clojurescript "1.7.228" :scope "provided"]
                 [ring "1.4.0"
                  :exclusions [ring/ring-jetty-adapter]]
                 [ring/ring-defaults "0.2.0"]
                 [ring.middleware.logger "0.5.0"]
                 ;[compojure "1.4.0"]
                 ;[org.omcljs/om "1.0.0-alpha28"]

                 [compojure "1.1.6"]
                 [selmer "1.0.4"]

                 [c3p0/c3p0 "0.9.1.2"]

                 [ring-webjars "0.1.1"]
                 [org.webjars/bootstrap "3.3.5"]
                 ;[org.webjars/material-design-lite "1.1.1"]

                 [tiesql "0.1.0-alpha-SNAPSHOT"]
                 [tiesql-http "0.1.0-SNAPSHOT"]

                 [com.h2database/h2 "1.3.154"]

                 ;[cljs-ajax "0.5.2" :scope "provided"]
                 ;[com.cognitect/transit-cljs "0.8.225" :scope "provided"]
                 [secretary "1.2.3" :scope "provided"]
                 [reagent "0.6.0-alpha" :scope "provided"]
                 [reagent-forms "0.5.21" :scope "provided"]
                 ;[reagent-reforms "0.4.3" :scope "provided"]
                 [re-frame "0.7.0-alpha-3" :scope "provided"]
                 [kibu/pushy "0.3.6" :scope "provided"]
                 [devcards "0.2.1-5" :scope "provided"]

                 [org.immutant/web "2.1.3"                  ;; default Web server
                  :exclusions [ch.qos.logback/logback-core
                               org.slf4j/slf4j-api]]
                 [ch.qos.logback/logback-classic "1.1.3"]]

  :plugins [[lein-cljsbuild "1.1.1"]
            [lein-environ "1.0.1"]]

  :min-lein-version "2.5.3"
  :source-paths ["src/clj" "src/cljs" "dev"]
  :test-paths ["test/clj"]
  :clean-targets ^{:protect false} [:target-path :compile-path "resources/public/js"]
  :uberjar-name "web-app.jar"

  ;; Use `lein run` if you just want to start a HTTP server, without figwheel
  :main app.server
  :repl-options {:init-ns user}

  :cljsbuild {:builds
              {:app
               {:source-paths ["src/cljs" "dev"]
                :figwheel     {;:devcards true
                               }
                :compiler     {:main                 app.card
                               :asset-path           "js/compiled/out"
                               :output-dir           "resources/public/js/compiled/out"
                               :output-to            "resources/public/js/compiled/web_app.js"
                               :source-map-timestamp true}}}}

  :figwheel {:server-port    3001                           ;; default
             :css-dirs       ["resources/public/css"]       ;; watch and update CSS
             :ring-handler   user/http-handler
             :server-logfile "target/figwheel.log"}
  :doo {:build "test"}
  :profiles {:dev
             {:dependencies [[figwheel "0.5.0-6"]
                             [figwheel-sidecar "0.5.0-6"]
                             [com.cemerick/piggieback "0.2.1"]
                             [org.clojure/tools.nrepl "0.2.12"]]
              :repl-options {:port 4555}
              :plugins      [[lein-figwheel "0.5.0-6"]
                             [lein-doo "0.1.6"]]
              :cljsbuild    {:builds
                             {:test
                              {:source-paths ["src/cljs" "test/cljs"]
                               :compiler
                                             {:output-to     "resources/public/js/compiled/testable.js"
                                              :asset-path    "js/compiled/out"
                                              :output-dir    "resources/public/js/compiled/out"
                                              :main          app.test-runner
                                              :optimizations :none}}}}}
             :uberjar
             {:source-paths ^:replace ["src/clj"]
              :hooks        [leiningen.cljsbuild]
              :omit-source  true
              :aot          :all
              :cljsbuild    {:builds
                             {:app
                              {:source-paths ^:replace ["src/cljs"]
                               :compiler     {:optimizations :advanced
                                              :pretty-print  false}}}}}})
