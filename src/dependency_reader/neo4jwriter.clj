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
            [clojurewerkz.neocons.rest.cypher        :as cy]))

(def ^:private neo4j-coordinates "http://localhost:7474/db/data/")

(defn- create-or-update-node
  [new-node-map]
  )

(defn write-class-dependencies-to-neo
  "Writes class dependencies into a Neo4J database."
  [dependencies]
  (nr/connect! neo4j-coordinates)
  (let [;idx-typename (nrl/create-index "by-typename"  {:unique true})
        ;idx-type     (nrl/create-index "by-type")
        nodes        (doall (map #(nn/create { :name              (:name %)
                                               :source            (:source %)
                                               :type              (:type %)
                                               :class-version     (:class-version %)
                                               :class-version-str (:class-version-str %) }) dependencies))]
;        nodes (doall (nb/perform dependencies))]                            ; Step 2: Bulk import the raw data as nodes in the graph
;    (doall (map #(nn/add-to-index (:id %) (:name idx) "typename" (:name (:data %)) true) nodes)) ; Step 3: index the nodes by classname
;    ()                                                              ; Step 4: relate the nodes via their dependencies
  ))