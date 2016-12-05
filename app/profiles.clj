{:dev      {:plugins      [[lein-midje "3.2.1"]]
            :source-paths ["src" "dev"]
            :dependencies [[org.clojure/test.check "0.9.0"]]}

 :repl     {:plugins      [[cider/cider-nrepl "0.15.0-SNAPSHOT"]
                           #_[refactor-nrepl "2.2.0"]
                           #_[lein-figwheel "0.5.0-6"]]
            :dependencies [[org.clojure/tools.nrepl "0.2.12"]
                           [figwheel "0.5.0-6"]
                           [figwheel-sidecar "0.5.0-6"]
                           [com.cemerick/piggieback "0.2.1"]]
            :repl-options {:port             4555
                           :timeout          120000
                           :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}

 :uberjar {:source-paths   ^:replace ["src"]
           :resource-paths ^:replace []
           :hooks          [leiningen.cljsbuild]
           :manifest       {"Class-Path" "lib"}
           :omit-source    true
           :aot            :all
           :cljsbuild      {:builds
                            {:app
                             {:source-paths ^:replace ["src"]
                              :compiler     {:optimizations :advanced
                                             :main          app.core
                                             :devcards      true
                                             :output-to     "resources/public/js/compiled/app.min.js"
                                             :asset-path    "js/compiled/out"
                                             :output-dir    "resources/public/js/compiled/out"
                                             :pretty-print  false}}}}}


 }                                                          ;; using the key id

