;
; Copyright © 2013 Peter Monks (pmonks@gmail.com)
;
; This work is licensed under the Creative Commons Attribution-ShareAlike 3.0
; Unported License. To view a copy of this license, visit
; http://creativecommons.org/licenses/by-sa/3.0/ or send a letter to Creative
; Commons, 444 Castro Street, Suite 900, Mountain View, California, 94041, USA.
;

(ns dependency-reader.reader
  (:require [clojure.tools.logging    :as log]
            [clojure.java.io          :as io]
            [dependency-reader.parser :as drp]))

(defn classes-info
  "Returns the dependencies of all class files in the given location (which may be a file, a directory or an archive,
   expressed as either a String or a java.io.File).
   See dependency-reader.parser for the structure of each entry in the result vector."
  [file-or-directory]
  (let [tfile-or-directory (net.java.truevfs.access.TFile. file-or-directory)]
    (if (.isDirectory tfile-or-directory)
      (let [listing      (file-seq tfile-or-directory)  ; Note: this handles recursion into sub-archives / sub-sub-archives etc. for us
            class-files  (filter #(and (.canRead %) (.isFile %) (.endsWith (.getName %) ".class")) listing)]
          (vec (concat (map #(drp/class-info % (.getPath %)) class-files))))
      (conj [] (drp/class-info tfile-or-directory (.getPath tfile-or-directory))))))
