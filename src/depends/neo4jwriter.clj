;
; Copyright © 2013 Peter Monks (pmonks@gmail.com)
;
; This work is licensed under the Creative Commons Attribution-ShareAlike 3.0
; Unported License. To view a copy of this license, visit
; http://creativecommons.org/licenses/by-sa/3.0/ or send a letter to Creative
; Commons, 444 Castro Street, Suite 900, Mountain View, California, 94041, USA.
;

;####TODO: Look at using the batch APIs instead of inserting everything one-at-a-time

(ns depends.neo4jwriter
  (:require [clojure.tools.logging                   :as log]
            [clojure.java.io                         :as io]
            [clojurewerkz.neocons.rest               :as nr]
            [clojurewerkz.neocons.rest.nodes         :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrl]
            [clojurewerkz.neocons.rest.batch         :as nb]
            [depends.reader                          :as dr]
            ))

(def ^:private default-neo4j-coords "http://localhost:7474/db/data/")

(defn- strip-nils
  "Strip entries with nil values from the given map."
  [m]
  (apply dissoc m (for [[k v] m :when (nil? v)] k)))

(defn- create-node!
  [node]
  (nn/create (strip-nils node)))

(defn- find-node-by-name
  [name]
  (try
    (nn/find-one "node_auto_index" "name" name)   ; Note: requires that auto-indexing of the "name" property be enabled in neo4j.properties.
    (catch clojure.lang.ExceptionInfo ei
      (throw (Exception. "Neo4J: unable to find node by name in automatic index. Is neo4j.properties up to date?" ei)))))

(defn- find-or-create-node-by-name
  [edge]
  (let [fqtypename (:target edge)
        result     (find-node-by-name fqtypename)]
    (if (nil? result)
      (let [[package typename] (dr/split-fqtypename fqtypename)]
        (create-node!
          {
            :name     fqtypename
            :package  package
            :typename typename
            :type    (if (dr/primitive? fqtypename) :primitive :unknown)
          }))
      result)))

(defn- create-edge!
  [edge]
  (let [source-node (find-node-by-name           (:source edge))
        target-node (find-or-create-node-by-name edge)
        type        (:type edge)]
    (nrl/create source-node target-node type)))

(defn write-dependencies!
  "Writes class dependencies into a Neo4J database.  Returns nil."
  ([dependencies] (write-dependencies! default-neo4j-coords dependencies))
  ([neo4j-coords dependencies]
    (let [nodes (first  dependencies)
          edges (second dependencies)]
      (nr/connect! neo4j-coords)
      (doall (map create-node! nodes))
      (doall (map create-edge! edges))
      nil)))
