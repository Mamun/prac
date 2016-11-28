 (ns sparser
   (:require [clojure.java.io :as io]
             [clojure.tools.reader :as edn]
             [formatter :as f]))


(defn read-file-as-line-coll [file-name]
  (with-open [rdr (clojure.java.io/reader
                    (io/file file-name)
                    :encoding "ISO-8859-15")]
    (doall (line-seq rdr))))

(def FieldNo "FieldNo")

(defn parse-line-fields [field]
  (let [[f-name f-value] (clojure.string/split field #"=")
        f-name-temp (clojure.string/trim f-name)]
    (if (= f-name-temp FieldNo)
      {f-name-temp (edn/read-string f-value)}
      {f-name-temp (clojure.string/trim f-value)})))


(defn parse-line [line]
  (->> (clojure.string/split line #",")
       (map parse-line-fields)
       (into {})))


(defn is-field-110? [line-map]
  (if (= 110 (get line-map FieldNo))
    true
    false))


(defn partition-by-field-100 [line-coll]
  (let [[f & n] (doall (partition-by is-field-110? line-coll))]
    (->> (partition 2 n)
         (map flatten )
         (cons f ))))

(comment

  (->> (read-file-as-line-coll "test/in.txt")
       (mapv parse-line)
       (partition-by-field-100)
       (f/do-line-format))


  )

