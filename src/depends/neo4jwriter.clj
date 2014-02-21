;
; Copyright © 2014 Peter Monks (pmonks@gmail.com)
;
; All rights reserved. This program and the accompanying materials
; are made available under the terms of the Eclipse Public License v1.0
; which accompanies this distribution, and is available at
; http://www.eclipse.org/legal/epl-v10.html
;
; Contributors:
;    Peter Monks - initial implementation

(ns depends.neo4jwriter
  (:require [clojure.tools.logging           :as log]
            [clojure.java.io                 :as io]
            [clojurewerkz.neocons.rest       :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.batch :as nb]
            ))

(def ^:private default-neo4j-coords "http://localhost:7474/db/data/")
(def ^:private batch-size           50000)

(defn- strip-nils
  "Strip entries with nil values from the given map."
  [m]
  (apply dissoc m (for [[k v] m :when (nil? v)] k)))

(defn- edge-to-relationship-op
  "Creates a \"relationship op\" map for the given edge."
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

(defn- write-edges!
  "Writes the edges of the graph into a Neo4J database. Returns nil."
  [neo4j-nodes edges]
  (let [name-id-map (zipmap (map #(:name (:data %)) neo4j-nodes) (map :id neo4j-nodes))
        ops         (map #(edge-to-relationship-op name-id-map %) (sort-by :id edges))
        batches     (partition-all batch-size ops)]
    (doall (map nb/perform batches))
    nil))

(defn write-dependencies!
  "Writes class dependencies into a Neo4J database. Returns nil."
  ([dependencies] (write-dependencies! default-neo4j-coords dependencies))
  ([neo4j-coords dependencies]
    (log/info (str "Connecting to Neo4J server " neo4j-coords "..."))
    (nr/connect! neo4j-coords)

    (log/info (str "Writing " (count (first dependencies)) " nodes to Neo4J..."))
    (let [nodes       (first  dependencies)
          edges       (second dependencies)
          batches     (partition-all batch-size nodes)
          neo4j-nodes (doall (apply concat (map #(nn/create-batch (map strip-nils %)) batches)))]
      (log/info (str "Writing " (count edges) " edges to Neo4J..."))
      (write-edges! neo4j-nodes edges)
      nil)))
