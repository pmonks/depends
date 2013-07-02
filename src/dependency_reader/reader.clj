;
; Copyright © 2013 Peter Monks (pmonks@gmail.com)
;
; This work is licensed under the Creative Commons Attribution-ShareAlike 3.0
; Unported License. To view a copy of this license, visit
; http://creativecommons.org/licenses/by-sa/3.0/ or send a letter to Creative
; Commons, 444 Castro Street, Suite 900, Mountain View, California, 94041, USA.
;

(ns dependency-reader.reader
  (:require [clojure.tools.logging :as log]
            [clojure.java.io       :as io]))

(def ^:private version-name-map {
                                  45 "1.0 or 1.1",
                                  46 "1.2",
                                  47 "1.3",
                                  48 "1.4",
                                  49 "1.5",
                                  50 "1.6",
                                  51 "1.7",
                                  52 "1.8"   ; Speculative!
                                 })

(defn- fix-type-name
  [^String name]
  (.replaceAll name "/" "."))

(defn- add-dependency
  [dependencies dependency type]
  (if (contains? dependencies dependency)
    (let [existing-dependency-types (dependencies dependency)
          new-dependency-types      (conj existing-dependency-types type)]
      (merge dependencies {dependency new-dependency-types}))
    (merge dependencies {dependency #{type}})))

(defn- add-dependencies
  [dependencies new-dependencies type]
  (loop [result                 dependencies
         current-dependency     (first new-dependencies)
         remaining-dependencies (rest  new-dependencies)]
    (if (empty? remaining-dependencies)
      (add-dependency result current-dependency type)
      (recur (add-dependency result current-dependency type) (first remaining-dependencies) (rest remaining-dependencies)))))

(defn- visit
  [class-info version access class-name signature super-name interfaces]
  (let [fixed-class-name      (fix-type-name class-name)
        fixed-super-name      (fix-type-name super-name)
        fixed-interface-names (vec (map fix-type-name interfaces))
        existing-dependencies (:dependencies class-info)]
    (merge class-info
           { :name              fixed-class-name
             :class-version     version
             :class-version-str (version-name-map version)
             :dependencies      (add-dependency (add-dependencies existing-dependencies fixed-interface-names :implements)
                                                fixed-super-name :extends) } )))

(defn- visit-field
  [class-info access field-name desc signature value]
  (merge class-info
         {
           :dependencies (add-dependency (:dependencies class-info) (.getClassName (org.objectweb.asm.Type/getType desc)) :uses)
         }))

(defn class-info
  "Takes an input stream of class bytes and returns the dependencies it contains as a map with this shape:

  {
    :name                  \"typeName\"
    :type                  :class OR :interface OR :annotation OR :enum
    :class-version         49
    :class-version-str     \"1.5\"
    :dependencies          {\"dependentTypeName\" #{:extends :implements :uses :inner-class}
                            ...}
  }

  The input stream is consumed, but _not_ closed."
  [^java.io.InputStream class-input-stream]
  (let [result        (atom { :name              nil
                              :type              nil
                              :class-version     nil
                              :class-version-str nil
                              :dependencies      {} })
        class-reader  (new org.objectweb.asm.ClassReader class-input-stream)
        class-visitor (proxy [org.objectweb.asm.ClassVisitor] ; classes & interfaces
                             [org.objectweb.asm.Opcodes/ASM4] ; constructor params
                             ; Overridden functions
                             (visit [version access class-name signature super-name interfaces]
                               (swap! result visit version access class-name signature super-name interfaces))
                             (visitField [access field-name desc signature value]
                               (swap! result visit-field access field-name desc signature value)
                               nil)
                          )]
    (.accept class-reader class-visitor 0)
    @result))

(defn class-info-from-file
  "As per class-file, but takes a filename as a string."
  [^String filename]
  (with-open [class-input-stream (new java.io.FileInputStream filename)]
    (class-info class-input-stream)))