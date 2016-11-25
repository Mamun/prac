(defproject dadysql-http "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0-alpha14" :scope "provided"]
                 [org.clojure/clojurescript "1.9.229" :scope "provided"]
                 [ring/ring-defaults "0.2.0"]
                 [ring "1.4.0"]
                 [ring-middleware-format "0.6.0"
                  :exclusions [ring
                               org.clojure/core.memoize
                               org.clojure/tools.reader]]
                 [compojure "1.1.6"]

                 [dadysql "0.1.0-alpha-SNAPSHOT" ]
                 [cljs-ajax "0.5.2" :scope "provided"]
                 [reagent "0.6.0"  :scope "provided"]
                 [re-frame "0.7.0-alpha-3" :scope "provided"]]

  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-figwheel "0.5.8"]
            [lein-doo "0.1.6"]]

  :min-lein-version "2.5.3"
  :source-paths ["src/clj" "src/cljs"]
  ;:test-paths ["test/clj"]
  :clean-targets ^{:protect false} [:target-path :compile-path "dev-resources/public/js"]

  :figwheel {:server-port    3001                           ;; default
             :css-dirs       ["dev-resources/public/css"]   ;; watch and update CSS
             :ring-handler   user/http-handler
             :server-logfile "target/figwheel.log"}

  :cljsbuild {:builds
              {:app
               {:source-paths ["src/cljs" "dev"]
                :figwheel     {:devcards true}
                :compiler     {:main                 app.core
                               :asset-path           "js/compiled/out"
                               :output-dir           "dev-resources/public/js/compiled/out"
                               :output-to            "dev-resources/public/js/compiled/app.js"
                               :source-map-timestamp true}}}}

  :profiles {:dev {:repl-options   {:port 4555
                                    :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                   :source-paths   [ "dev"]
                   :resource-paths ["../../test-i" "dev-resources"]
                   :dependencies   [[devcards "0.2.1-5" ]
                                    [org.immutant/web "2.1.3" ;; default Web server
                                     :exclusions [ch.qos.logback/logback-core
                                                  org.slf4j/slf4j-api]]
                                    [ch.qos.logback/logback-classic "1.1.3"]

                                    [com.h2database/h2 "1.3.154"]
                                    [c3p0/c3p0 "0.9.1.2"]


                                    [figwheel "0.5.0-6"]
                                    [figwheel-sidecar "0.5.0-6"]
                                    [com.cemerick/piggieback "0.2.1"]
                                    [org.clojure/tools.nrepl "0.2.12"]]}})

