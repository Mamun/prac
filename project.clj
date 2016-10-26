(defproject dadysql "0.1.0-alpha-SNAPSHOT"
  :description "Clojure sql database access framework"
  :url "https://github.com/Mamun/dadysql"
  :license {:alias "Eclipse Public License"
            :url   "http://www.eclipse.org/legal/epl-v10.html"}
  ;:offline? true
 :source-paths ["src" "src-jvm"]
  :dependencies  [[org.clojure/clojure "1.9.0-alpha13"]
                  ;    [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/core.async "0.2.382"]
                 [org.clojure/tools.reader "0.9.2"]
                 [org.clojure/tools.logging "0.3.1"]
           ;      [org.clojure/core.match "0.3.0-alpha4"]
          ;       [prismatic/schema "0.2.2"]
                 [org.clojure/java.jdbc "0.4.2"]]


  :profiles {:dev {:repl-options   {:port 4555}
                   :source-paths   ["dev"]
                   :test-paths     ["test-i"]
                   :codox          {:src-linenum-anchor-prefix "L"
                                    :sources                   ["src"]}
                   :plugins        [[lein-cloverage "1.0.6"]
                                    [jonase/eastwood "0.2.2"]
                                    [codox "0.8.12"]
                                    [lein-midje "3.0.0"]
                                    [lein-pprint "1.1.1"]]
                   :dependencies   [[org.clojure/tools.namespace "0.2.11"]
                                    [org.clojure/tools.nrepl "0.2.10"]

                                    [org.clojure/test.check "0.9.0"]

                                    [com.h2database/h2 "1.3.154"]
                                    [c3p0/c3p0 "0.9.1.2"]
                                    [ch.qos.logback/logback-classic "1.1.3"]]}
             }
  )



