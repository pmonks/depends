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

(defn- archive?
  [^java.io.File file]
  (let [tfile (net.java.truevfs.access.TFile. file)]
    (.isArchive tfile)))

(defmulti ^:private process
  "Process the given item."
  #(.isDirectory %))

(defmethod ^:private process true
  [^java.io.File directory]
  (let [directory-listing (file-seq directory)
        class-files       (filter #(and (.isFile %) (.canRead %) (.endsWith (.getName %) ".class")) directory-listing)
        sub-archives      (filter #(and (archive? %) (.canRead %) (not= % directory))               directory-listing)]
    (vec (into (map #(drp/class-info % (.getPath %)) class-files) (concat (map process sub-archives))))))

(defmethod ^:private process false
  [^java.io.File file]
  (conj [] (drp/class-info file (.getPath file))))

(defmulti classes-info
  "Returns the dependencies of all class files in the given location (which may be a file, a directory or an archive).
   See dependency-reader.parser for the structure of each entry in the result."
  class)

(defmethod classes-info java.io.File
  [^java.io.File file-or-directory]
  (process file-or-directory))

(defmethod classes-info java.lang.String
  [^java.lang.String directory]
  (classes-info (net.java.truevfs.access.TFile. directory)))
