;
; Copyright © 2013 Peter Monks (pmonks@gmail.com)
;
; This work is licensed under the Creative Commons Attribution-ShareAlike 3.0
; Unported License. To view a copy of this license, visit
; http://creativecommons.org/licenses/by-sa/3.0/ or send a letter to Creative
; Commons, 444 Castro Street, Suite 900, Mountain View, California, 94041, USA.
;

(ns dependency-reader.core
  (:require [clojure.string                :as s]
            [dependency-reader.reader      :as dr]
            [dependency-reader.neo4jwriter :as neo]
            [clojure.data.json             :as json])
  (:use [clojure.tools.cli :only [cli]]
        [clojure.pprint :only [pprint]])
  (:gen-class))

(defn -main
  "Calculate class file(s) dependencies from the command line."
  [& args]
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))

  (let [[options args banner] (cli args
                                   ["-j" "--json"  "Produce JSON output (default)"                              :default false :flag true]
                                   ["-e" "--edn"   "Produce EDN output instead of JSON"                         :default false :flag true]
                                   ["-n" "--neo4j" "Write dependency information to the specified Neo4J server" :default false]
                                   ["-h" "--help"  "Show help"                                                  :default false :flag true])
        source                (first args)
        json                  (:json options)
        edn                   (:edn  options)
        neo4j                 (:neo4j  options)
        help                  (:help options)]
    (if (or help (nil? source))
      (println (str banner "\n Args\t\t\tDesc\n ----\t\t\t----\n source\t\t\tDetermines the dependencies of all class files in the given location (which may be a .class file, a directory or an archive). Must be provided.\n"))
      (do
        ; Look at the crap TrueVFS makes us do, just to add support for .AMP files (ZIP files under another name) #fail
        (.setArchiveDetector (net.java.truevfs.access.TConfig/current)
                             (net.java.truevfs.access.TArchiveDetector. "zip|jar|war|ear|amp"
                                                                        (net.java.truevfs.comp.zipdriver.ZipDriver.)))
        (let [dependencies (dr/classes-info source)]
          (if edn
            (pprint dependencies))   ; Is this the right way to emit EDN?
          (if json
            (json/pprint dependencies :escape-unicode false))
          (if (not (empty? neo4j))
            (neo/write-class-dependencies-to-neo neo4j dependencies))
          (try
            (net.java.truevfs.access.TVFS/umount)
            (catch java.util.ServiceConfigurationError sce
              (comment "Ignore this exception because TrueVFS is noisy as crap.")))
          nil)))))
