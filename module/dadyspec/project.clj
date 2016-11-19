(defproject dadyspec "0.1.0-SNAPSHOT"
            :description "FIXME: write description"
            :url "http://example.com/FIXME"
            :license {:name "Eclipse Public License"
                      :url  "http://www.eclipse.org/legal/epl-v10.html"}
            :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                           [org.clojure/clojurescript "1.9.293"]]
  :plugins [[lein-cljsbuild "1.1.1"]
            [lein-figwheel "0.5.8"]
            [lein-doo "0.1.6"]
            [lein-cloverage "1.0.6"]
            [jonase/eastwood "0.2.2"]
            [codox "0.8.12"]
            [lein-midje "3.0.0"]
            [lein-pprint "1.1.1"]]
  :figwheel {:server-port    3001                           ;; default
             :css-dirs       ["dev-resources/public/css"]   ;; watch and update CSS
             :server-logfile "target/figwheel.log"}

  :cljsbuild {:builds
              {:app
               {:source-paths ["src" "dev"]
                :figwheel     {:devcards true}
                :compiler     {:main                 app.core
                               :asset-path           "js/compiled/out"
                               :output-dir           "dev-resources/public/js/compiled/out"
                               :output-to            "dev-resources/public/js/compiled/app.js"
                               :source-map-timestamp true}}}}
  :profiles {:dev {:repl-options   {:port 4555}
                   :codox          {:src-linenum-anchor-prefix "L"
                                    :sources                   ["src"]}
                   :dependencies   [[org.clojure/tools.namespace "0.3.0-alpha3"]
                                    [clj-time "0.12.2"]
                                    [cheshire "5.6.3"]
                                    [org.clojure/tools.nrepl "0.2.12"]
                                    [org.clojure/test.check "0.9.0"]
                                    [ch.qos.logback/logback-classic "1.1.3"]
                                    [devcards "0.2.2" ]]}})


