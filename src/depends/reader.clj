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

(ns depends.reader
  (:require [clojure.tools.logging :as log]
            [clojure.java.io       :as io]
            [clojure.set           :as set]
            [clojure.string        :as s]))

(def ^:private version-name-map
  "Map of class version numbers to human readable equivalent."
  {
    (org.objectweb.asm.Opcodes/V1_1) "1.1"
    (org.objectweb.asm.Opcodes/V1_2) "1.2"
    (org.objectweb.asm.Opcodes/V1_3) "1.3"
    (org.objectweb.asm.Opcodes/V1_4) "1.4"
    (org.objectweb.asm.Opcodes/V1_5) "1.5"
    (org.objectweb.asm.Opcodes/V1_6) "1.6"
    (org.objectweb.asm.Opcodes/V1_7) "1.7"
;    (org.objectweb.asm.Opcodes/V1_8) "1.8"     ; Only once Clojure embeds ASM 5.0+, or allows us to use our own version
  })

(def ^:private type-codes-to-names
  {
    \B (.getClassName org.objectweb.asm.Type/BYTE_TYPE)
    \C (.getClassName org.objectweb.asm.Type/CHAR_TYPE)
    \D (.getClassName org.objectweb.asm.Type/DOUBLE_TYPE)
    \F (.getClassName org.objectweb.asm.Type/FLOAT_TYPE)
    \I (.getClassName org.objectweb.asm.Type/INT_TYPE)
    \J (.getClassName org.objectweb.asm.Type/LONG_TYPE)
    \S (.getClassName org.objectweb.asm.Type/SHORT_TYPE)
    \V (.getClassName org.objectweb.asm.Type/VOID_TYPE)
    \Z (.getClassName org.objectweb.asm.Type/BOOLEAN_TYPE)
  })

(def ^:private primitive-type-names (vals type-codes-to-names))

(def ^:private type-name-regex #"\[*([LBCDFIJSZV]?)([^;\s]*);?")  ; group 1 = descriptor (if any), group 2 = type name (if any)

(defn- in?
  "True if val is in coll."  ; It boggles my mind that Clojure doesn't have this in the stdlib...
  [val coll]
  (boolean (some #(= val %) coll)))

(defn- xor
  "Logical XOR, based on truthiness."  ; It boggles my mind that Clojure doesn't have this in the stdlib...
  [a b]
  (and (or a b) (not (and a b))))

(defn- determine-type-name
  [^String s]
  (when-not (or (nil? s) (zero? (.length (s/trim s))))
    (if-let [matches (re-matches type-name-regex (s/trim s))]
      (let [[_ ^String descriptor ^String type-name] matches]
        (if (or (zero? (.length (s/trim descriptor))) (= "L" (s/trim descriptor)))
          (s/replace (s/replace (s/trim type-name) \/ \.) "[]" "")       ; type
          (get type-codes-to-names (first (seq (s/trim descriptor))))))  ; primitive
      (throw (Exception. (str "Invalid type name or descriptor: " s))))))

(defn- determine-type-type
  [access-bitmask]
  (cond
    (not= 0 (bit-and access-bitmask org.objectweb.asm.Opcodes/ACC_INTERFACE))  :interface
    (not= 0 (bit-and access-bitmask org.objectweb.asm.Opcodes/ACC_ENUM))       :enum
    (not= 0 (bit-and access-bitmask org.objectweb.asm.Opcodes/ACC_ANNOTATION)) :annotation
    :else                                                                      :class))

(defn split-fqtypename
  "Returns a vector of two elements - the first is the FQ package of the type, the second the type name."
  [^String fqtypename]
  (when-not (nil? fqtypename)
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
  (let [fixed-class-name      (determine-type-name class-name)
        fixed-super-name      (determine-type-name super-name)
        fixed-interface-names (map determine-type-name interfaces)
        dependencies          (second class-info)
        [package typename]    (split-fqtypename fixed-class-name)]
    [
      {
        :name              fixed-class-name
        :package           package
        :typename          typename
        :type              (determine-type-type access-bitmask)
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
        fixed-field-name (determine-type-name desc)]
    [
      info
      (conj dependencies (create-dependency class-name fixed-field-name :uses))
    ]))

(defn- visit-annotation
  [class-info annotation-name desc]
  (let [info                  (first class-info)
        class-name            (:name info)
        dependencies          (second class-info)
        fixed-annotation-type (determine-type-name desc)]
    [
      info
      (conj dependencies (create-dependency class-name fixed-annotation-type :uses))
    ]))

(defn- visit-method
  [class-info access-bitmask method-name ^String desc signature exceptions]
  (let [info                  (first class-info)
        class-name            (:name info)
        dependencies          (second class-info)
        fixed-exception-names (map determine-type-name exceptions)
        argument-types        (map #(determine-type-name (.getClassName ^org.objectweb.asm.Type %)) (org.objectweb.asm.Type/getArgumentTypes desc))
        return-type           (determine-type-name (.getClassName (org.objectweb.asm.Type/getReturnType desc)))]
    (assert (not (xor desc return-type)) (str "desc=" desc ", return-type=" return-type))  ; Check that ASM doesn't f up the return type descriptor
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
        fixed-inner-class-name (determine-type-name inner-class-name)]
    [
      info
      (conj dependencies (create-dependency class-name fixed-inner-class-name :inner-class))
    ]))

(defn- visit-local-variable
  [class-info local-variable-name desc signature start end index]
  (let [info                      (first class-info)
        class-name                (:name info)
        dependencies              (second class-info)
        fixed-local-variable-type (determine-type-name desc)]
    [
      info
      (conj dependencies (create-dependency class-name fixed-local-variable-type :uses))
    ]))

(defn- visit-method-call
  ([class-info opcode owner name desc] (visit-method-call class-info opcode owner name desc false))    ; ASM v4
  ([class-info opcode owner name ^String desc itf]                                                     ; ASM v5
   (let [info              (first class-info)
         class-name        (:name info)
         dependencies      (second class-info)
         fixed-method-type (determine-type-name owner)
         argument-types    (map #(determine-type-name (.getClassName ^org.objectweb.asm.Type %)) (org.objectweb.asm.Type/getArgumentTypes desc))
         return-type       (determine-type-name (.getClassName (org.objectweb.asm.Type/getReturnType desc)))]
     (assert (not (xor owner fixed-method-type)) (str "owner=" owner ", fixed-method-type=" fixed-method-type))  ; Check that ASM doesn't f up the method type descriptor
     (assert (not (xor desc return-type)) (str "desc=" desc ", return-type=" return-type))  ; Check that ASM doesn't f up the return type descriptor
     [
       info
       (if (or (nil? fixed-method-type)
               (= fixed-method-type class-name))
         dependencies
         (into dependencies (conj (create-dependencies class-name argument-types    :uses)
                                  (create-dependency   class-name return-type       :uses)
                                  (create-dependency   class-name fixed-method-type :uses))))
     ])))

(defn- visit-field-usage
  [class-info opcode owner name desc]
  (let [info             (first class-info)
        class-name       (:name info)
        dependencies     (second class-info)
        fixed-field-type (determine-type-name owner)]
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
        fixed-exception-type (determine-type-name type)]
    [
      info
      (if (nil? fixed-exception-type)
        dependencies
        (conj dependencies (create-dependency class-name fixed-exception-type :uses)))
    ]))

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
  #(class (first %&)))

(defmethod class-info java.io.InputStream
  ([^java.io.InputStream class-input-stream] (class-info class-input-stream nil))
  ([^java.io.InputStream class-input-stream
    ^java.lang.String    source]
   (log/debug "Analysing" source "...")
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
                                   (visitMethodInsn [opcode owner name desc]                     ; ASM v4
                                     (swap! result visit-method-call opcode owner name desc))
;                                   (visitMethodInsn [opcode owner name desc itf]                ; ASM v5
;                                     (swap! result visit-method-call opcode owner name desc itf))
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
  ([^net.java.truevfs.access.TFile file] (class-info file (.getName file)))
  ([^net.java.truevfs.access.TFile file
    ^java.lang.String              source]
    (try
      (with-open [class-input-stream (net.java.truevfs.access.TFileInputStream. file)]
        (class-info class-input-stream source))
      (finally
        (net.java.truevfs.access.TVFS/umount file)))))

(defmethod class-info java.io.File
  ([^java.io.File file] (class-info file (.getName file)))
  ([^java.io.File     file
    ^java.lang.String source]
   (with-open [class-input-stream (java.io.BufferedInputStream. (java.io.FileInputStream. file))]
     (class-info class-input-stream))))

(defmethod class-info java.lang.String
  ([^java.lang.String file] (class-info file file))
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
  (log/debug "Analysing all classes in" file-or-directory "...")
  ; Look at the crap TrueVFS makes us do, just to add support for .AMP files (ZIP files under another name) #fail
  (.setArchiveDetector (net.java.truevfs.access.TConfig/current)
                       (net.java.truevfs.access.TArchiveDetector. "zip|jar|war|ear|amp"
                                                                  (net.java.truevfs.comp.zipdriver.ZipDriver.)))
  (let [tfile-or-directory (net.java.truevfs.access.TFile. file-or-directory)
        is-directory?      (.isDirectory tfile-or-directory)]
    (if is-directory?
      (let [listing             (try
                                  (doall (file-seq tfile-or-directory))  ; Note: this handles recursion into sub-archives / sub-sub-archives etc. for us
                                  (catch Exception e
                                    (throw (Exception. (str "Unexpected exception while scanning " file-or-directory ". Possibly a corrupted archive?") e))))
            class-files         (filter #(and (.canRead ^java.io.File %)
                                              (.isFile ^java.io.File %)
                                              (.endsWith ^String (.getName ^java.io.File %) ".class"))
                                        listing)
            class-files-info    (map #(class-info % (.getPath ^java.io.File %)) class-files)
            merged-info         (vec (map first class-files-info))
            merged-dependencies (reduce set/union (map second class-files-info))]
        [(into merged-info (class-info-for-missing-dependencies merged-dependencies)) merged-dependencies])
      (class-info tfile-or-directory (.getPath tfile-or-directory)))))
