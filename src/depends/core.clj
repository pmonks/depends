;
; Copyright © 2013,2014 Peter Monks (pmonks@gmail.com)
;
; This work is licensed under the Creative Commons Attribution-ShareAlike 3.0
; Unported License. To view a copy of this license, visit
; http://creativecommons.org/licenses/by-sa/3.0/ or send a letter to Creative
; Commons, 444 Castro Street, Suite 900, Mountain View, California, 94041, USA.
;

(ns depends.core
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
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))

  (try
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
        (log/info "Starting...")

        ; Look at the crap TrueVFS makes us do, just to add support for .AMP files (ZIP files under another name) #fail
        (.setArchiveDetector (net.java.truevfs.access.TConfig/current)
                             (net.java.truevfs.access.TArchiveDetector. "zip|jar|war|ear|amp"
                                                                        (net.java.truevfs.comp.zipdriver.ZipDriver.)))
        (log/info (str "Calculating dependencies from " source "..."))
        (let [dependencies (dr/classes-info source)]
          (if edn
            (do
              (log/info (str "Writing dependencies to stdout as EDN..."))
              (pprint dependencies)))   ; Is this the right way to emit EDN?
          (if json
            (do
              (log/info (str "Writing dependencies to stdout as JSON..."))
              (json/pprint dependencies :escape-unicode false)))
          (if neo4j-coords
            (do
              (log/info (str "Writing dependencies to Neo4J..."))
              (neo/write-dependencies! neo4j-coords dependencies)))
;            (if svg
;              (do
;                (log/info (str "Writing dependencies to Neo4J..."))
;                (svg/write-dependencies dependencies)))
          (log/info "Complete.")
          nil)))
    (catch Exception e
      (log/error e)
      (aviso/write-exception e))))
