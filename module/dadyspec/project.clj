(defproject dadyspec "0.1.0-SNAPSHOT"
            :description "FIXME: write description"
            :url "http://example.com/FIXME"
            :license {:name "Eclipse Public License"
                      :url  "http://www.eclipse.org/legal/epl-v10.html"}
            :dependencies [[org.clojure/clojure "1.9.0-alpha14"]]
  :profiles {:dev {:repl-options   {:port 4555}
                   :codox          {:src-linenum-anchor-prefix "L"
                                    :sources                   ["src"]}
                   :plugins        [[lein-cloverage "1.0.6"]
                                    [jonase/eastwood "0.2.2"]
                                    [codox "0.8.12"]
                                    [lein-midje "3.0.0"]
                                    [lein-pprint "1.1.1"]]
                   :dependencies   [[org.clojure/tools.namespace "0.3.0-alpha3"]
                                    [clj-time "0.12.2"]
                                    [cheshire "5.6.3"]
                                    [org.clojure/tools.nrepl "0.2.12"]
                                    [org.clojure/test.check "0.9.0"]
                                    [ch.qos.logback/logback-classic "1.1.3"]]}})


