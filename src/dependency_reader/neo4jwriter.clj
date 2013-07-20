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

(defn- setup
  [neo4j-coords]
  (nr/connect! neo4j-coords))

; DUPLICATED FROM reader.clj REFACTOR ASAP!!!!
(defn- split-fqtypename
  "Returns a vector of two elements - the first is the FQ package of the type, the second the type name."
  [^String fqtypename]
  (if (nil? fqtypename)
    nil
    (let [split-index (.lastIndexOf fqtypename ".")]
      (if (= -1 split-index)   ; fqtypename doesn't have a package e.g. it's a primitive
        [nil fqtypename]
        [(subs fqtypename 0 split-index) (subs fqtypename (inc split-index))]))))

; ####TODO: REFACTOR ALL THIS CRUD TO USE NODES AND EDGES FROM THE READER
(defn- create-node
  [type-info]
  (let [fqtypename        (:name              type-info)
        package           (:package           type-info)
        typename          (:typename          type-info)
        source            (:source            type-info)
        type              (:type              type-info)
        class-version     (:class-version     type-info)
        class-version-str (:class-version-str type-info)
        node              (nn/create { :name              fqtypename
                                       :package           package
                                       :typename          typename
                                       :source            source
                                       :type              type
                                       :class-version     class-version
                                       :class-version-str class-version-str })]
    node))

(defn- find-node-by-typename
  [typename]
  (nn/find-one "node_auto_index" "name" typename))   ; Note: requires that auto-indexing of the "name" property be enabled.

(defn- find-or-create-node-by-typename
  [typename]
  (let [node               (find-node-by-typename typename)
        [package typename] (split-fqtypename typename)]
    (if (nil? node)
      (if (nil? package)
        (nn/create { :name     typename
                     :typename typename })
        (nn/create { :name     typename
                     :package  package
                     :typename typename }))
      node)))

(defn- create-relationship
  [source relationship]
  (let [source-node      (find-node-by-typename source)
        target-node      (find-or-create-node-by-typename (first relationship))
        dependency-types (second relationship)]
    (doall (map #(nrl/create source-node target-node %) dependency-types))))

(defn- create-relationships
  [type-info]
  (let [source       (:name         type-info)
        dependencies (:dependencies type-info)]
    (doall (map #(create-relationship source %) dependencies))))

(defn write-dependencies
  "Writes class dependencies into a Neo4J database."
  ([dependencies] (write-dependencies default-neo4j-coords dependencies))
  ([neo4j-coords dependencies]
   (setup neo4j-coords)
   (let [nodes        (doall (map create-node dependencies))
         dependencies (doall (map create-relationships dependencies))])))
