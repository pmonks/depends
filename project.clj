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

(defproject org.clojars.pmonks/depends "0.4.0-SNAPSHOT"
  :description      "Reads dependency information from compiled .class files."
  :url              "https://github.com/pmonks/depends"
  :license          {:name "Eclipse Public License"
                     :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.0.0"
  :javac-target     "1.7"
  :dependencies [
                  [org.clojure/clojure                          "1.6.0"]
                  [org.clojure/data.json                        "0.2.5"]
                  [org.clojure/tools.cli                        "0.3.1"]
                  [org.clojure/tools.logging                    "0.3.0"]
                  [clojurewerkz/neocons                         "3.0.0"]
                  [ch.qos.logback/logback-classic               "1.1.2"]
                  [io.aviso/pretty                              "0.1.12"]
                  [org.ow2.asm/asm                              "5.0.3"]
                  [net.java.truevfs/truevfs-kernel-impl         "0.10.6"]  ; Ugh TrueVFS' dependencies are crap
                  [net.java.truevfs/truevfs-access              "0.10.6"]
                  [net.java.truevfs/truevfs-driver-file         "0.10.6"]
                  [net.java.truevfs/truevfs-driver-zip          "0.10.6"]
                  [net.java.truevfs/truevfs-driver-jar          "0.10.6"]
                  [net.java.truecommons/truecommons-key-disable "2.3.4"]
                ]
  :profiles {:dev {:dependencies [[midje      "1.6.3"]]
                   :plugins      [[lein-midje "3.1.3"]]}   ; Don't remove this or travis-ci will assplode!
              :uberjar {:aot :all}}
  :uberjar-merge-with {#"META-INF/services/.*" [slurp str spit]}   ; Awaiting Leiningen 2.3.5 - see https://github.com/technomancy/leiningen/issues/1455
  :uberjar-exclusions [#".*\.disabled"]
  :jar-exclusions     [#".*\.disabled"]
  :main depends.main)
