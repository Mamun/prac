(defproject rscrap "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.1.6"]
                 [hiccup "1.0.5"]
                 [ring-server "0.3.1"]

                 [enlive "1.1.6"]
                 [environ "1.0.3"]
                 [ring/ring-defaults "0.2.1"]
                 [clj-http "3.1.0"]]
  :plugins [[lein-ring "0.8.12"]]
  :ring {:handler rscrap.handler/app
         :init rscrap.handler/init
         :destroy rscrap.handler/destroy}
  :profiles
  {:uberjar {:aot :all}
   :production
   {:ring
    {:open-browser? false, :stacktraces? false, :auto-reload? false}}
   :dev
   {:repl-options   {:port 4555}
    :dependencies [[ring-mock "0.1.5"] [ring/ring-devel "1.3.1"]]}})
