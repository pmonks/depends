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

(def ^:private version-name-map
  "Map of class version numbers to human readable equivalent."
  {
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
  [^String type-name]
  (.replaceAll (.replaceAll type-name "/" ".") "\\[\\]" ""))

(defn- fix-descriptor
  [^String desc]
  (fix-type-name (.getClassName (org.objectweb.asm.Type/getType desc))))

(defn- split-fqtypename
  "Returns a vector of two elements - the first is the FQ package of the type, the second the type name."
  [^String fqtypename]
  (if (nil? fqtypename)
    nil
    (let [split-index (.lastIndexOf fqtypename ".")]
      (if (= -1 split-index)   ; fqtypename doesn't have a package e.g. it's a primitive
        [nil fqtypename]
        [(subs fqtypename 0 split-index) (subs fqtypename (inc split-index))]))))

(defn- add-dependency
  [dependencies dependency dependency-type]
  (if (contains? dependencies dependency)
    (let [existing-dependency-types (dependencies dependency)
          new-dependency-types      (conj existing-dependency-types dependency-type)]
      (merge dependencies {dependency new-dependency-types}))
    (merge dependencies {dependency #{dependency-type}})))

(defn- add-dependencies
  [dependencies new-dependencies dependency-type]
  (if (or (empty? new-dependencies) (nil? new-dependencies))
    dependencies
    (loop [result                 dependencies
           current-dependency     (first new-dependencies)
           remaining-dependencies (rest  new-dependencies)]
      (if (empty? remaining-dependencies)
        (add-dependency result current-dependency dependency-type)
        (recur (add-dependency result current-dependency dependency-type) (first remaining-dependencies) (rest remaining-dependencies))))))

(defn- typeof
  [access-bitmask]
  (cond
    (not= 0 (bit-and access-bitmask org.objectweb.asm.Opcodes/ACC_INTERFACE))  :interface
    (not= 0 (bit-and access-bitmask org.objectweb.asm.Opcodes/ACC_ENUM))       :enum
    (not= 0 (bit-and access-bitmask org.objectweb.asm.Opcodes/ACC_ANNOTATION)) :annotation
    :else                                                                      :class))

(defn- visit
  [class-info version access-bitmask class-name signature super-name interfaces]
  (let [fixed-class-name      (fix-type-name class-name)
        fixed-super-name      (fix-type-name super-name)
        fixed-interface-names (vec (map fix-type-name interfaces))
        existing-dependencies (:dependencies class-info)
        package-and-class     (split-fqtypename fixed-class-name)]
    (merge class-info
           { :name              fixed-class-name
             :package           (first package-and-class)
             :typename          (second package-and-class)
             :type              (typeof access-bitmask)
             :class-version     version
             :class-version-str (version-name-map version)
             :dependencies      (add-dependency (add-dependencies existing-dependencies fixed-interface-names :implements)
                                                fixed-super-name :extends) } )))

(defn- visit-field
  [class-info access-bitmask field-name desc signature value]
  (let [existing-dependencies (:dependencies class-info)
        fixed-field-name      (fix-descriptor desc)]
    (merge class-info
           {
             :dependencies (add-dependency existing-dependencies fixed-field-name :uses)
           })))

(defn- visit-annotation
  [class-info annotation-name desc]
  (let [existing-dependencies (:dependencies class-info)
        fixed-annotation-type (fix-descriptor desc)]
    (merge class-info
           {
             :dependencies (add-dependency existing-dependencies fixed-annotation-type :inner-class)  ; Not sure this is correct
           })))

(defn- visit-method
  [class-info access-bitmask method-name ^String desc signature exceptions]
  (let [fixed-exception-names (vec (map fix-type-name exceptions))
        existing-dependencies (:dependencies class-info)
        argument-types        (vec (map #(fix-type-name (.getClassName %)) (org.objectweb.asm.Type/getArgumentTypes desc)))
        return-type           (fix-type-name (.getClassName (org.objectweb.asm.Type/getReturnType desc)))]
    (merge class-info
           {
             :dependencies (add-dependency (add-dependencies (add-dependencies existing-dependencies fixed-exception-names :uses) argument-types :uses) return-type :uses)
           })))

; This doesn't appear to be useful - it's called regardless of who the parent of the inner class is
(defn- visit-inner-class
  [class-info inner-class-name outer-name inner-name access-bitmask]
  (let [existing-dependencies  (:dependencies class-info)
        fixed-inner-class-name (fix-type-name inner-class-name)]
    (merge class-info
           {
             :dependencies (add-dependency existing-dependencies fixed-inner-class-name :inner-class)
           })))

(defn- visit-local-variable
  [class-info local-variable-name desc signature start end index]
  (let [existing-dependencies     (:dependencies class-info)
        fixed-local-variable-type (fix-descriptor desc)]
    (merge class-info
           {
             :dependencies (add-dependency existing-dependencies fixed-local-variable-type :uses)
           })))

(defn- class-of-first
  [& args]
  (class (first args)))

; Public functions start here

(defmulti class-info
  "Returns the dependencies of the given class, as a map with this shape:
  {
    :name                  \"fully.qualified.typename\"
    :package               \"package\"
    :typename              \"typename\"
    :source                \"source\"
    :type                  :class OR :interface OR :annotation OR :enum
    :class-version         49
    :class-version-str     \"1.5\"
    :dependencies          {\"dependentTypeName\" #{:extends :implements :uses :inner-class :parent-class}
                            ...}
  }
  All of these keys will be present in the map, however they may have empty or nil values."
  class-of-first)

(defmethod class-info java.io.InputStream
  ([^java.io.InputStream class-input-stream] (class-info class-input-stream nil))
  ([^java.io.InputStream class-input-stream
    ^java.lang.String    source]
   (let [result             (atom { :name              nil
                                    :package           nil
                                    :typename          nil
                                    :source            source
                                    :type              nil
                                    :class-version     nil
                                    :class-version-str nil
                                    :dependencies      {} })
         class-reader       (org.objectweb.asm.ClassReader. class-input-stream)
         annotation-visitor (proxy [org.objectweb.asm.AnnotationVisitor]
                                   [org.objectweb.asm.Opcodes/ASM4]
                                   (visitAnnotation [name desc]
                                     (swap! result visit-annotation name desc)
                                     ;annotation-visitor))
                                     nil))
         field-visitor      (proxy [org.objectweb.asm.FieldVisitor]
                                   [org.objectweb.asm.Opcodes/ASM4]
                                   (visitAnnotation [desc visible]
                                     annotation-visitor))
         method-visitor     (proxy [org.objectweb.asm.MethodVisitor]
                                   [org.objectweb.asm.Opcodes/ASM4]
                                   (visitLocalVariable [local-variable-name desc signature start end index]
                                     (swap! result visit-local-variable local-variable-name desc signature start end index)))
         class-visitor      (proxy [org.objectweb.asm.ClassVisitor]
                                   [org.objectweb.asm.Opcodes/ASM4]
                                   (visit [version access-bitmask class-name signature super-name interfaces]
                                     (swap! result visit version access-bitmask class-name signature super-name interfaces))
                                   (visitField [access-bitmask field-name desc signature value]
                                     (swap! result visit-field access-bitmask field-name desc signature value)
                                     field-visitor)
                                   (visitMethod [access-bitmask method-name desc signature exceptions]
                                     (swap! result visit-method access-bitmask method-name desc signature exceptions)
                                     method-visitor)
; As mentioned above, visiting inner classes doesn't seem very useful
;                                   (visitInnerClass [inner-class-name outer-name inner-name access-bitmask]
;                                     (swap! result visit-inner-class inner-class-name outer-name inner-name access-bitmask))
                            )]
     (.accept class-reader class-visitor 0)
     @result)))

(defmethod class-info net.java.truevfs.access.TFile
  ([^net.java.truevfs.access.TFile file] (class-info file nil))
  ([^net.java.truevfs.access.TFile file
    ^java.lang.String              source]
   (with-open [class-input-stream (net.java.truevfs.access.TFileInputStream. file)]
     (class-info class-input-stream source))))

(defmethod class-info java.io.File
  ([^java.io.File file] (class-info file nil))
  ([^java.io.File     file
    ^java.lang.String source]
   (with-open [class-input-stream (java.io.BufferedInputStream. (java.io.FileInputStream. file))]
     (class-info class-input-stream))))

(defmethod class-info java.lang.String
  ([^java.lang.String file] (class-info file nil))
  ([^java.lang.String file
    ^java.lang.String source]
  (class-info (net.java.truevfs.access.TFile. file) source)))

(defn classes-info
  "Returns the dependencies of all class files in the given location (which may be a .class file, a directory or an archive,
   expressed as either a String or a java.io.File).
   See dependency-reader.parser for the structure of each entry in the result vector."
  [file-or-directory]
  (let [tfile-or-directory (net.java.truevfs.access.TFile. file-or-directory)]
    (if (.isDirectory tfile-or-directory)
      (let [listing      (file-seq tfile-or-directory)  ; Note: this handles recursion into sub-archives / sub-sub-archives etc. for us
            class-files  (filter #(and (.canRead %) (.isFile %) (.endsWith (.getName %) ".class")) listing)]
          (vec (concat (map #(class-info % (.getPath %)) class-files))))
      (conj [] (class-info tfile-or-directory (.getPath tfile-or-directory))))))


; Functions for manipulating dependencies, after they've been constructed
(defn nodes
  "Returns the nodes (types) in the dependency graph, in this shape:
  [
    {
      :id \"fully.qualified.typename\"
      :data
      {
        :name                  \"fully.qualified.typename\"
        :package               \"package\"
        :typename              \"typename\"
        :source                \"source\"
        :type                  :class OR :interface OR :annotation OR :enum
        :class-version         49
        :class-version-str     \"1.5\"
      }
    }
    ...
  ]
  This function is provided to support subsequent graph processing (most graph libraries seem to want the graph in two lists,
  one containing nodes and the other edges)."
  [dependencies]
  (vec (map #({ :id   (:name %)
                :data { :name              (:name              %)
                        :package           (:package           %)
                        :typename          (:typename          %)
                        :source            (:source            %)
                        :type              (:type              %)
                        :class-version     (:class-version     %)
                        :class-version-str (:class-version-str %)
                      }}) dependencies)))

(defn- get-edge-2
  [source dependency]
  (let [target           (key dependency)
        dependency-types (val dependency)]
    (map #({ :source source
             :target target
             :type   % }) dependency-types)))

(defn- get-edge
  [type-info]
  (let [source       (:name         type-info)
        dependencies (:dependencies type-info)]
    (map #(get-edge-2 source %) dependencies)))

(defn edges
  "Returns the edges (dependencies) in the dependency graph, in this shape:
  [
    {
      :source \"sourceTypeName\"
      :target \"targetTypeName\"
      :type   :extends OR :implements OR :uses OR :inner-class OR :parent-class
    }
    ...
  ]
  This function is provided to support subsequent graph processing (most graph libraries seem to want the graph in two lists,
  one containing nodes and the other edges)."
  [dependencies]
  (into [] (map get-edge dependencies)))

(defn graph
  "Returns a vector of two elements - the first containing the nodes of the dependency graph (see doc for nodes for details), the second the edges (see doc for edges for details)."
  [classes-info]
  [(nodes classes-info) (edges class-info)])