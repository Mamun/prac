(ns tiesql.history
  (:require [secretary.core :as secretary]
            [goog.events :as e])
  (:import [goog.history EventType Html5History]
           [goog History]))


(secretary/set-config! :prefix "#")

(let [h (History.)]
  (goog.events/listen h EventType.NAVIGATE #(secretary/dispatch! (.-token %)))
  (doto h
    (.setEnabled true)))