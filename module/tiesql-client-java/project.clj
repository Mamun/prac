(defproject tiesql/client-java "0.1.0-alpha-SNAPSHOT"
  :description "It is Java client for tiesql framework "
  :url "https://github.com/Mamun/tiesql"
  :min-lein-version "2.0.0"
  :dependencies [[org.apache.httpcomponents/httpclient "4.5.1"]
                 [com.google.code.gson/gson "2.4"]
                 [com.cognitect/transit-java "0.8.307"]
                 [log4j/log4j "1.2.17"]
                 [junit/junit "4.11"]]
  :plugins [[lein-junit "1.1.8"]]
  :resource-paths ["resources"]
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java" "test/java"]
  :junit ["test/java"]
  :profiles {:dev {:dependencies [[junit/junit "4.11"]]}}
  )