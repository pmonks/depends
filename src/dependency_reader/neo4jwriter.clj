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

(def ^:private default-neo4j-coordinates "http://localhost:7474/db/data/")

(defn- setup
  [neo4jserver]
  (nr/connect! neo4jserver))

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
  (let [node (find-node-by-typename typename)]
    (if (nil? node)
      (nn/create { :name typename })
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

(defn write-class-dependencies-to-neo
  "Writes class dependencies into a Neo4J database."
  ([dependencies] (write-class-dependencies-to-neo default-neo4j-coordinates dependencies))
  ([neo4jserver dependencies]
   (setup neo4jserver)
   (let [nodes        (doall (map create-node dependencies))
         dependencies (doall (map create-relationships dependencies))]
   )))