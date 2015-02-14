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

(ns depends.main
  (:require [clojure.string        :as s]
            [clojure.tools.logging :as log]
            [clojure.data.json     :as json]
            [io.aviso.exception    :as aviso]
            [depends.reader        :as dr]
            [depends.neo4jwriter   :as neo]
;            [depends.svgwriter     :as svg]
            )
  (:use [clojure.tools.cli :only [cli]]
        [clojure.pprint :only [pprint]])
  (:gen-class))

(defn -main
  "Calculate class file(s) dependencies from the command line."
  [& args]
;  (try
    (let [[options args banner] (cli args
                                     ["-j" "--json"  "Write JSON to stdout"                :default false :flag true]
                                     ["-e" "--edn"   "Write EDN to stdout"                 :default false :flag true]
                                     ["-n" "--neo4j" "Write to the specified Neo4J server" :default false :flag false]
;                                     ["-s" "--svg"   "Write SVG to stdout"                 :default false :flag true]
                                     ["-h" "--help"  "Show help"                           :default false :flag true])
          source                (first  args)
          json                  (:json  options)
          edn                   (:edn   options)
          neo4j-coords          (:neo4j options)
;          svg                   (:svg   options)
          help                  (:help  options)]
      (if (or help (nil? source))
        (println (str banner "\n Args\t\t\tDesc\n ----\t\t\t----\n source\t\t\tDetermines the dependencies of all class files in the given location (which may be a .class file, a directory or an archive). Must be provided.\n"))
        (do
          (log/info "Starting...")

          (log/info (str "Calculating dependencies from " source "..."))
          (let [dependencies (dr/classes-info source)]
            (when edn
              (log/info (str "Writing dependencies to stdout as EDN..."))
              (pprint dependencies))   ; Is this the right way to emit EDN?
            (when json
              (log/info (str "Writing dependencies to stdout as JSON..."))
              (json/pprint dependencies :escape-unicode false))
            (when neo4j-coords
              (log/info (str "Writing dependencies to Neo4J..."))
              (neo/write-dependencies! neo4j-coords dependencies))
;              (when svg
;                (log/info (str "Writing dependencies to Neo4J..."))
;                (svg/write-dependencies dependencies))
            (log/info "Complete.")
            nil))))
;    (catch Exception e
;      (log/error e)
;      (println (aviso/format-exception e))))
      )
