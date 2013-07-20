;
; Copyright © 2013 Peter Monks (pmonks@gmail.com)
;
; This work is licensed under the Creative Commons Attribution-ShareAlike 3.0
; Unported License. To view a copy of this license, visit
; http://creativecommons.org/licenses/by-sa/3.0/ or send a letter to Creative
; Commons, 444 Castro Street, Suite 900, Mountain View, California, 94041, USA.
;

(ns dependency-reader.neo4jwriter
  (:require [clojure.tools.logging                   :as log]
            [clojure.java.io                         :as io]
            [clojurewerkz.neocons.rest               :as nr]
            [clojurewerkz.neocons.rest.nodes         :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrl]
            [clojurewerkz.neocons.rest.batch         :as nb]
            [clojurewerkz.neocons.rest.cypher        :as cy]
            [clojurewerkz.neocons.rest.records       :as re]
            ))

(def ^:private default-neo4j-coords "http://localhost:7474/db/data/")

(defn- find-node-by-name
  [name]
  (nn/find-one "node_auto_index" "name" name))   ; Note: requires that auto-indexing of the "name" property be enabled.

(defn- create-node!
  [node]
  (nn/create (:data node)))

(defn- create-edge!
  [edge]
  (let [source-node (find-node-by-name (:source edge))
        target-node (find-node-by-name (:target edge))
        type        (:type edge)]
    (nrl/create source-node target-node type)))

(defn write-dependencies
  "Writes class dependencies into a Neo4J database."
  ([nodes edges] (write-dependencies default-neo4j-coords nodes edges))
  ([neo4j-coords nodes edges]
    (nr/connect! neo4j-coords)
    (doall (map create-node! nodes))
    (doall (map create-edge! edges))))
