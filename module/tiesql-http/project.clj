(defproject tiesql-http "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0-RC5" :scope "provided"]
                 [org.clojure/clojurescript "1.7.228" :scope "provided"]
                 [ring "1.4.0" :scope "provided"]
                 [ring-middleware-format "0.6.0"
                  :exclusions [ring
                               org.clojure/core.memoize
                               org.clojure/tools.reader]]
                 [tiesql "0.1.0-alpha-SNAPSHOT" :scope "provided"]
                 [cljs-ajax "0.5.2"]]

  :plugins [[lein-cljsbuild "1.1.1"]]

  :min-lein-version "2.5.3"
  :source-paths ["src/clj" "src/cljs"]
  :test-paths ["test/clj"]

  :profiles {:dev {:repl-options {:port 4555}
                   :source-paths ["src-dev"]
                   :resource-paths [ "../../test-i"]
                   :dependencies [[compojure "1.1.6"]
                                  [org.immutant/web "2.1.3" ;; default Web server
                                   :exclusions [ch.qos.logback/logback-core
                                                org.slf4j/slf4j-api]]
                                  [ch.qos.logback/logback-classic "1.1.3"]

                                  [com.h2database/h2 "1.3.154"]
                                  [c3p0/c3p0 "0.9.1.2"]]}}


  :cljsbuild {:builds
              {:app
               {:source-paths ["src/cljs"]
                :compiler     {:asset-path           "out"
                               :output-dir           "target/out"
                               :output-to            "target/tiesql_http.js"
                               :source-map-timestamp true}}}})

