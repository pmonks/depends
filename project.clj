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

(defproject org.clojars.pmonks/depends "0.2.0-SNAPSHOT"
  :description      "Reads dependency information from compiled .class files."
  :url              "https://github.com/pmonks/depends"
  :url              "https://github.com/pmonks/multigrep"
  :license          {:name "Eclipse Public License"
                     :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :javac-target     "1.7"
  :dependencies [
                  [org.clojure/clojure                          "1.5.1"]
                  [org.clojure/data.json                        "0.2.4"]
                  [org.clojure/tools.cli                        "0.3.1"]
                  [org.clojure/tools.trace                      "0.7.6"]
                  [org.clojure/tools.logging                    "0.2.6"]
                  [clojurewerkz/neocons                         "2.0.1"]
                  [ch.qos.logback/logback-classic               "1.1.1"]
                  [io.aviso/pretty                              "0.1.9"]
                  [org.ow2.asm/asm                              "4.2"]
                  [net.java.truevfs/truevfs-kernel-impl         "0.10.5"]  ; Ugh TrueVFS' dependencies are crap
                  [net.java.truevfs/truevfs-access              "0.10.5"]
                  [net.java.truevfs/truevfs-driver-file         "0.10.5"]
                  [net.java.truevfs/truevfs-driver-zip          "0.10.5"]
                  [net.java.truevfs/truevfs-driver-jar          "0.10.5"]
                  [net.java.truecommons/truecommons-key-disable "2.3.3"]
;                  [lacij                                        "0.9.0" :exclusions [org.clojure/clojure]]
                ]
  :profiles  {:dev {:dependencies [
                                   [midje          "1.6.2"]
                                   [clj-ns-browser "1.3.1"]
                                  ]}
             :uberjar {:aot :all}
  }
  :uberjar-merge-with {#"META-INF/services/.*" [slurp str spit]}   ; Merge Java ServiceLocator descriptors during uberjar construction
  :jvm-opts ^:replace []  ; Stop Leiningen from turning off JVM optimisations - makes it slower to start but ensures code runs as fast as possible
  :main depends.core)
