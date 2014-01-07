;
; Copyright © 2013 Peter Monks (pmonks@gmail.com)
;
; This work is licensed under the Creative Commons Attribution-ShareAlike 3.0
; Unported License. To view a copy of this license, visit
; http://creativecommons.org/licenses/by-sa/3.0/ or send a letter to Creative
; Commons, 444 Castro Street, Suite 900, Mountain View, California, 94041, USA.
;

(ns depends.neo4jwriter
  (:require [clojure.tools.logging           :as log]
            [clojure.java.io                 :as io]
            [clojurewerkz.neocons.rest       :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.batch :as nb]
            ))

(def ^:private default-neo4j-coords "http://localhost:7474/db/data/")

(defn- strip-nils
  "Strip entries with nil values from the given map."
  [m]
  (apply dissoc m (for [[k v] m :when (nil? v)] k)))

(defn- edge-to-relationship-batch-op
  [name-id-map edge]
  (let [source-id (name-id-map (:source edge))
        target-id (name-id-map (:target edge))
        type      (:type edge)]
  {
    :method "POST"
    :to     (str "/node/" source-id "/relationships")
    :body
    {
      :to   (str target-id)
      :type type
    }
  }))

(defn- create-edges!
  [nodes edges]
  (let [name-id-map (zipmap (map #(:name (:data %)) nodes) (map :id nodes))
        batch-ops   (map #(edge-to-relationship-batch-op name-id-map %) (sort-by :id edges))]
    (doall (nb/perform batch-ops))))

(defn write-dependencies!
  "Writes class dependencies into a Neo4J database. Returns nil."
  ([dependencies] (write-dependencies! default-neo4j-coords dependencies))
  ([neo4j-coords dependencies]
    (nr/connect! neo4j-coords)
    (let [nodes         (first  dependencies)
          edges         (second dependencies)
          created-nodes (doall (nn/create-batch (map strip-nils nodes)))]
      (create-edges! created-nodes edges)
      nil)))
