;
; Copyright © 2013 Peter Monks (pmonks@gmail.com)
;
; This work is licensed under the Creative Commons Attribution-ShareAlike 3.0
; Unported License. To view a copy of this license, visit
; http://creativecommons.org/licenses/by-sa/3.0/ or send a letter to Creative
; Commons, 444 Castro Street, Suite 900, Mountain View, California, 94041, USA.
;

(ns depends.reader
  (:require [clojure.tools.logging :as log]
            [clojure.java.io       :as io]
            [clojure.set           :as set]))

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

(def ^:private primitive-type-names
  #{
    "byte"
    "short"
    "int"
    "long"
    "float"
    "double"
    "boolean"
    "char"
    "void"
    })

(defn- in?
  "True if val is in coll."  ; It boggles my mind that Clojure doesn't have this in the stdlib...
  [val coll]
  (boolean (some #(= val %) coll)))

(defn- fix-type-name
  [^String type-name]
  (if (nil? type-name)
    nil
    (.replaceAll (.replaceAll type-name "/" ".") "\\[\\]" "")))

(defn- fix-descriptor
  [^String desc]
  (if (nil? desc)
    nil
    (fix-type-name (.getClassName (org.objectweb.asm.Type/getType desc)))))

(defn- typeof
  [access-bitmask]
  (cond
    (not= 0 (bit-and access-bitmask org.objectweb.asm.Opcodes/ACC_INTERFACE))  :interface
    (not= 0 (bit-and access-bitmask org.objectweb.asm.Opcodes/ACC_ENUM))       :enum
    (not= 0 (bit-and access-bitmask org.objectweb.asm.Opcodes/ACC_ANNOTATION)) :annotation
    :else                                                                      :class))

(defn split-fqtypename
  "Returns a vector of two elements - the first is the FQ package of the type, the second the type name."
  [^String fqtypename]
  (if (nil? fqtypename)
    nil
    (let [split-index (.lastIndexOf fqtypename ".")]
      (if (= -1 split-index)   ; fqtypename doesn't have a package e.g. it's a primitive
        [nil fqtypename]
        [(subs fqtypename 0 split-index) (subs fqtypename (inc split-index))]))))

(defn primitive?
  [^String fqtypename]
  (in? fqtypename primitive-type-names))

(defn- create-dependency
  [source target type]
  {
    :source     source
    :target     target
    :type       type
  })

(defn- create-dependencies
  [source targets type]
  (if (or (empty? targets) (nil? targets))
    #{}
    (loop [result            #{}
           current-target    (first targets)
           remaining-targets (rest  targets)]
      (if (empty? remaining-targets)
        (conj result (create-dependency source current-target type))
        (recur (conj result (create-dependency source current-target type))
               (first remaining-targets)
               (rest remaining-targets))))))

(defn- visit
  [class-info version access-bitmask class-name signature super-name interfaces source]
  (let [fixed-class-name      (fix-type-name class-name)
        fixed-super-name      (fix-type-name super-name)
        fixed-interface-names (map fix-type-name interfaces)
        dependencies          (second class-info)
        [package typename]    (split-fqtypename fixed-class-name)]
    [
      {
        :name              fixed-class-name
        :package           package
        :typename          typename
        :type              (typeof access-bitmask)
        :class-version     version
        :class-version-str (version-name-map version)
        :source            source
      }
      (into dependencies (conj (create-dependencies fixed-class-name fixed-interface-names :implements)
                               (create-dependency fixed-class-name fixed-super-name :extends)))
    ]))

(defn- visit-field
  [class-info access-bitmask field-name desc signature value]
  (let [info             (first class-info)
        class-name       (:name info)
        dependencies     (second class-info)
        fixed-field-name (fix-descriptor desc)]
    [
      info
      (conj dependencies (create-dependency class-name fixed-field-name :uses))
    ]))

(defn- visit-annotation
  [class-info annotation-name desc]
  (let [info                  (first class-info)
        class-name            (:name info)
        dependencies          (second class-info)
        fixed-annotation-type (fix-descriptor desc)]
    [
      info
      (conj dependencies (create-dependency class-name fixed-annotation-type :uses))
    ]))

(defn- visit-method
  [class-info access-bitmask method-name ^String desc signature exceptions]
  (let [info                  (first class-info)
        class-name            (:name info)
        dependencies          (second class-info)
        fixed-exception-names (map fix-type-name exceptions)
        argument-types        (map #(fix-type-name (.getClassName %)) (org.objectweb.asm.Type/getArgumentTypes desc))
        return-type           (fix-type-name (.getClassName (org.objectweb.asm.Type/getReturnType desc)))]
    [
      info
      (into dependencies (conj (create-dependencies class-name (into fixed-exception-names argument-types) :uses)
                               (create-dependency class-name return-type :uses)))
    ]))

; This doesn't appear to be useful - it's called regardless of who the parent of the inner class is
(defn- visit-inner-class
  [class-info inner-class-name outer-name inner-name access-bitmask]
  (let [info                   (first class-info)
        class-name             (:name info)
        dependencies           (second class-info)
        fixed-inner-class-name (fix-type-name inner-class-name)]
    [
      info
      (conj dependencies (create-dependency class-name fixed-inner-class-name :inner-class))
    ]))

(defn- visit-local-variable
  [class-info local-variable-name desc signature start end index]
  (let [info                      (first class-info)
        class-name                (:name info)
        dependencies              (second class-info)
        fixed-local-variable-type (fix-descriptor desc)]
    [
      info
      (conj dependencies (create-dependency class-name fixed-local-variable-type :uses))
    ]))

(defn- visit-method-call
  [class-info opcode owner name ^String desc]
  (let [info              (first class-info)
        class-name        (:name info)
        dependencies      (second class-info)
        fixed-method-type (fix-type-name owner)
        argument-types    (map #(fix-type-name (.getClassName %)) (org.objectweb.asm.Type/getArgumentTypes desc))
        return-type       (fix-type-name (.getClassName (org.objectweb.asm.Type/getReturnType desc)))]
    [
      info
      (if (or (nil? fixed-method-type)
              (= fixed-method-type class-name))
        dependencies
        (into dependencies (conj (create-dependencies class-name argument-types    :uses)
                                 (create-dependency   class-name return-type       :uses)
                                 (create-dependency   class-name fixed-method-type :uses))))
    ]))

(defn- visit-field-usage
  [class-info opcode owner name desc]
  (let [info             (first class-info)
        class-name       (:name info)
        dependencies     (second class-info)
        fixed-field-type (fix-type-name owner)]
    [
      info
      (if (or (nil? fixed-field-type)
              (= fixed-field-type class-name))
        dependencies
        (conj dependencies (create-dependency class-name fixed-field-type :uses)))
    ]))

(defn- visit-try-catch-block
  [class-info start end handler type]
  (let [info                 (first class-info)
        class-name           (:name info)
        dependencies         (second class-info)
        fixed-exception-type (fix-type-name type)]
    [
      info
      (if (nil? fixed-exception-type)
        dependencies
        (conj dependencies (create-dependency class-name fixed-exception-type :uses)))
    ]))


(defn- class-of-first
  [& args]
  (class (first args)))

(defn- build-class-info-for-missing-dependency
  "Constructs a class-info for the given fully qualified type name if it doesn't already exist in
   class-infos, or nil otherwise."
  [fqtypename]
  (let [[package typename] (split-fqtypename fqtypename)]    
    {
      :name     fqtypename
      :package  package
      :typename typename
      :type    (if (primitive? fqtypename) :primitive :unknown)
    }))

(defn- class-info-for-missing-dependencies
  "Adds missing relationship targets to the list of class-info maps."
  [relationships]
  (let [sources              (set (map :source relationships))
        targets              (set (map :target relationships))
        missing-dependencies (set/difference targets sources)]
    (map build-class-info-for-missing-dependency missing-dependencies)))


; Public functions start here

(defmulti class-info
  "Returns information for the given class, as a vector of two elements.  The first is a map of class information, of this shape:
  {
    :name                  \"fully.qualified.typename\"
    :package               \"package\"
    :typename              \"typename\"
    :source                \"source\"
    :type                  :class OR :interface OR :annotation OR :enum
    :class-version         49
    :class-version-str     \"1.5\"
  }

  While the second is the set of relationships between classes, where each entry is a map of this shape:
  #{
    {
      :source     \"sourceTypeName\"
      :target     \"targetTypeName\"
      :type       :extends OR :implements OR :uses OR :inner-class OR :parent-class
    }
    ...
  }

  Notes:
   * Keys in the first map may have empty or nil values.
   * Each source/target pair may appear more than once in the relationship set, albeit with a different relationship type each time."
  class-of-first)

(defmethod class-info java.io.InputStream
  ([^java.io.InputStream class-input-stream] (class-info class-input-stream nil))
  ([^java.io.InputStream class-input-stream
    ^java.lang.String    source]
   (let [result             (atom [{} #{}])
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
                                     (swap! result visit-local-variable local-variable-name desc signature start end index))
                                   (visitMethodInsn [opcode owner name desc]
                                     (swap! result visit-method-call opcode owner name desc))
                                   (visitFieldInsn [opcode owner name desc]
                                     (swap! result visit-field-usage opcode owner name desc))
                                   (visitTryCatchBlock [start end handler type]
                                     (swap! result visit-try-catch-block start end handler type)))
         class-visitor      (proxy [org.objectweb.asm.ClassVisitor]
                                   [org.objectweb.asm.Opcodes/ASM4]
                                   (visit [version access-bitmask class-name signature super-name interfaces]
                                     (swap! result visit version access-bitmask class-name signature super-name interfaces source))
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
     (try
       (.accept class-reader class-visitor 0)
       (catch ArrayIndexOutOfBoundsException aioobe
        (log/warn aioobe (str "Class " source " could not be parsed and has been skipped."))))
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
  "Returns a vector of two elements.  The first element is a vector containing class-info maps for each of the
   class files in the given location (which may be a .class file, a directory or an archive, expressed as either
   a String or a java.io.File).

   The second element is a set of maps containing each dependency found.

   See the class-info function for details on the structure of the class-info and dependency maps."
  [file-or-directory]
  (let [tfile-or-directory (net.java.truevfs.access.TFile. file-or-directory)]
    (if (.isDirectory tfile-or-directory)
      (let [listing             (file-seq tfile-or-directory)  ; Note: this handles recursion into sub-archives / sub-sub-archives etc. for us
            class-files         (filter #(and (.canRead %) (.isFile %) (.endsWith (.getName %) ".class")) listing)
            class-files-info    (map #(class-info % (.getPath %)) class-files)
            merged-info         (vec (map first class-files-info))
            merged-dependencies (reduce set/union (map second class-files-info))]
        [(into merged-info (class-info-for-missing-dependencies merged-dependencies)) merged-dependencies])
      (class-info tfile-or-directory (.getPath tfile-or-directory)))))

