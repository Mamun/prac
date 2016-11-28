(defproject app "0.1.0-alpha-SNAPSHOT"
  :description "dadysql micro service example  "
  :url "https://github.com/dadysql/example/dateservice"
  :license {:alias "Eclipse Public License"
            :url   "http://www.eclipse.org/legal/epl-v10.html"}
  :scm {:dir ".."}

  :uberjar-name "web-app.jar"
  :main app.core
  :repl-options {:init-ns user}
  :clean-targets ^{:protect false} [:target-path :compile-path "resources/public/js"]

  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/clojurescript "1.9.293" :scope "provided"]
                 [org.immutant/web "2.1.3"                  ;; default Web server
                  :exclusions [ch.qos.logback/logback-core
                               org.slf4j/slf4j-api]]
                 [ch.qos.logback/logback-classic "1.1.3"]

                 [com.h2database/h2 "1.3.154"]
                 [c3p0/c3p0 "0.9.1.2"]
                 [dadysql-http "0.1.0-SNAPSHOT" :exclusions [org.clojure/tools.analyzer
                                                             org.clojure/data.priority-map]]
                 [devcards "0.2.1-5" :scope "provided"]]


  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-figwheel "0.5.0-6"]]

  :figwheel {:server-port    3001                           ;; default
             :css-dirs       ["resources/public/css"]       ;; watch and update CSS
             :ring-handler   user/http-handler
             :server-logfile "target/figwheel.log"}

  :cljsbuild {:builds
              {:app
               {:figwheel {:devcards true}
                :compiler {:main                 app.debug
                           :asset-path           "js/compiled/out"
                           :output-dir           "resources/public/js/compiled/out"
                           :output-to            "resources/public/js/compiled/app.js"
                           :source-map-timestamp true}}}}

  :profiles {:dev     {:source-paths ["dev"]
                       :dependencies [[org.clojure/test.check "0.9.0"]
                                      [figwheel "0.5.0-6"]
                                      [figwheel-sidecar "0.5.0-6"]
                                      [com.cemerick/piggieback "0.2.1"]
                                      [org.clojure/tools.nrepl "0.2.12"]]

                       :repl-options {:port             4555
                                      :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}
             :uberjar {:source-paths ^:replace ["src"]
                       :resource-paths ^:replace []
                       :hooks        [leiningen.cljsbuild]
                       :manifest     {"Class-Path" "lib"}
                       :omit-source  true
                       :aot          :all
                       :cljsbuild    {:builds
                                      {:app
                                       {:source-paths ^:replace ["src"]
                                        :compiler     {:optimizations :advanced
                                                       :main          app.core
                                                       :devcards      true
                                                       :output-to     "resources/public/js/compiled/app.min.js"
                                                       :asset-path    "js/compiled/out"
                                                       :output-dir    "resources/public/js/compiled/out"
                                                       :pretty-print  false}}}}}})

