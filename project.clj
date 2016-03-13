;
; Copyright © 2014-2016 Peter Monks (pmonks@gmail.com)
;
; All rights reserved. This program and the accompanying materials
; are made available under the terms of the Eclipse Public License v1.0
; which accompanies this distribution, and is available at
; http://www.eclipse.org/legal/epl-v10.html
;
; Contributors:
;    Peter Monks - initial implementation

(defproject org.clojars.pmonks/depends "0.4.0-SNAPSHOT"
  :description      "Reads dependency information from compiled .class files."
  :url              "https://github.com/pmonks/depends"
  :license          {:name "Eclipse Public License"
                     :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.5.0"
  :javac-target     "1.7"
  :dependencies [
                  [org.clojure/clojure                          "1.8.0"]
                  [org.clojure/data.json                        "0.2.6"]
                  [org.clojure/tools.cli                        "0.3.3"]
                  [org.clojure/tools.logging                    "0.3.1"]
                  [clojurewerkz/neocons                         "3.1.0"]
                  [ch.qos.logback/logback-classic               "1.1.6"]
                  [io.aviso/pretty                              "0.1.24"]
                  [org.ow2.asm/asm                              "5.1"]
                  [net.java.truevfs/truevfs-kernel-impl         "0.11.0"]  ; Ugh TrueVFS' dependencies are crap
                  [net.java.truevfs/truevfs-access              "0.11.0"]
                  [net.java.truevfs/truevfs-driver-file         "0.11.0"]
                  [net.java.truevfs/truevfs-driver-zip          "0.11.0"]
                  [net.java.truevfs/truevfs-driver-jar          "0.11.0"]
                  [net.java.truecommons/truecommons-key-disable "2.5.0"]
                ]
  :profiles {:dev {:dependencies [[midje      "1.8.3"]]
                   :plugins      [[lein-midje "3.2"]]}   ; Don't remove this or travis-ci will assplode!
              :uberjar {:aot :all}}
  :uberjar-exclusions [#".*\.disabled"]
  :jar-exclusions     [#".*\.disabled"]
  :jvm-opts           ["-Xmx4g" "-server"]
  :main depends.main)
