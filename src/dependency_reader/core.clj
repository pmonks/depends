;
; Copyright © 2013 Peter Monks (pmonks@gmail.com)
;
; This work is licensed under the Creative Commons Attribution-ShareAlike 3.0
; Unported License. To view a copy of this license, visit
; http://creativecommons.org/licenses/by-sa/3.0/ or send a letter to Creative
; Commons, 444 Castro Street, Suite 900, Mountain View, California, 94041, USA.
;

(ns dependency-reader.core
  (:require [clojure.string           :as s]
            [dependency-reader.reader :as dr]
            [clojure.data.json        :as json])
  (:use [clojure.tools.cli :only [cli]]
        [clojure.pprint :only [pprint]])
  (:gen-class))

(defn -main
  "Calculate class file(s) dependencies from the command line."
  [& args]
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))

  (let [[options args banner] (cli args
                                   ["-e" "--edn"  "Produce EDN output instead of JSON (the default)" :flag true :default false]
                                   ["-h" "--help" "Show help"                                        :flag true :default false])]
    (let [source (first args)
          edn    (:edn  options)
          help   (:help options)]
      (if (or (nil? source) help)
        (println (str banner "\n Args\t\t\tDesc\n ----\t\t\t----\n source\t\t\tThe source to scan for .class files, to print dependency information for.\n"))
        (do
          ; Look at the crap TrueVFS makes us do, just to add support for .AMP files (ZIP files under another name) #fail
          (.setArchiveDetector (net.java.truevfs.access.TConfig/current)
                               (net.java.truevfs.access.TArchiveDetector. "zip|jar|war|ear|amp"
                                                                          (net.java.truevfs.comp.zipdriver.ZipDriver.)))
          (let [result (dr/classes-info source)]
            (if edn
              (pprint      result)
              (json/pprint result :escape-unicode false))
            (try
              (net.java.truevfs.access.TVFS/umount)
              (catch java.util.ServiceConfigurationError sce
                (comment "Ignore this exception because TrueVFS is noisy as crap.")))))))))
