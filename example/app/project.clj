(defproject app "0.1.0-alpha-SNAPSHOT"
  :description "tiesql micro service example  "
  :url "https://github.com/tiesql/example/dateservice"
  :license {:alias "Eclipse Public License"
            :url   "http://www.eclipse.org/legal/epl-v10.html"}
  :scm {:dir ".."}

  :main app

  :dependencies [[org.clojure/clojure "1.8.0-RC5"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]

                 [ring "1.4.0"
                  :exclusions [ring/ring-jetty-adapter
                               ring/ring-devel]]

                 [compojure "1.1.6"]
                 [org.immutant/web "2.1.3"                  ;; default Web server
                  :exclusions [ch.qos.logback/logback-core
                               org.slf4j/slf4j-api]]
                 [ch.qos.logback/logback-classic "1.1.3"]

                 [com.h2database/h2 "1.3.154"]
                 [c3p0/c3p0 "0.9.1.2"]

                 [tiesql "0.1.0-alpha-SNAPSHOT"]
                 [tiesql-http "0.1.0-SNAPSHOT"]

                 ]

  :profiles {:uberjar {:main         app
                       :aot          [app]
                       :omit-source  true
                       :uberjar-name "app-boot.jar"
                       :manifest     {"Class-Path" "lib lib/*"}}})

