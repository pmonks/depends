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
            [dependency-reader.reader :as dr])
  (:use [clojure.tools.cli :only [cli]]
        [clojure.pprint :only [pprint]])
  (:gen-class))

(defn -main
  "Calculate class file(s) dependencies from the command line."
  [& args]
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))

  (let [[options args banner] (cli args
                                   ["-f" "--file"      "Print the dependencies for a single .class file." :default false]
                                   ["-d" "--directory" "Print the dependencies for all .class files recursively in the given directory." :default false]
                                   ["-h" "--help"      "Show help" :default false :flag true])]
    (let [file                   (:file      options)
          directory              (:directory options)
          help                   (:help      options)
          file-or-directory-name (first args)]
      (if (or help (nil? file-or-directory-name))
        (println (str banner "\n Args\t\t\tDesc\n ----\t\t\t----\n file-or-directory-name\tThe .class filename or directory containing .class files to print dependency information for.\n"))
        (cond
          file      (pprint (dr/class-info-from-file file-or-directory-name))
          directory (pprint (dr/classes-info file-or-directory-name))
          :else     (println "wat?!?"))))))
