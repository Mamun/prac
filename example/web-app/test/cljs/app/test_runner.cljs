(ns app.test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [app.core-test]))

(enable-console-print!)

(doo-tests 'app.core-test)
