;
; Copyright © 2013 Peter Monks (pmonks@gmail.com)
;
; This work is licensed under the Creative Commons Attribution-ShareAlike 3.0
; Unported License. To view a copy of this license, visit
; http://creativecommons.org/licenses/by-sa/3.0/ or send a letter to Creative
; Commons, 444 Castro Street, Suite 900, Mountain View, California, 94041, USA.
;

(defproject depends "0.1.0-SNAPSHOT"
  :min-lein-version "2.0.0"
  :description "Reads dependency information from compiled .class files."
  :url "https://github.com/pmonks/depends"
  :license {:name "Creative Commons Attribution-ShareAlike 3.0 Unported License."
            :url "http://creativecommons.org/licenses/by-sa/3.0/"}
  :javac-target "1.7"
  :dependencies [
                  [org.clojure/clojure                  "1.5.1"]
                  [org.clojure/data.json                "0.2.3"]
                  [org.clojure/tools.cli                "0.2.4"]
                  [org.clojure/tools.trace              "0.7.6"]
                  [org.clojure/tools.logging            "0.2.6"]
                  [clojurewerkz/neocons                 "1.1.0"]
                  [ch.qos.logback/logback-classic       "1.0.13"]
                  [org.ow2.asm/asm                      "4.2"]
                  [net.java.truevfs/truevfs-kernel-impl "0.10.4"]  ; Ugh TrueVFS' dependencies are crap
                  [net.java.truevfs/truevfs-access      "0.10.4"]
                  [net.java.truevfs/truevfs-driver-file "0.10.4"]
                  [net.java.truevfs/truevfs-driver-zip  "0.10.4"]
                  [net.java.truevfs/truevfs-driver-jar  "0.10.4"]
                  [lacij                                "0.8.1" :exclusions [org.clojure/clojure]]
                ]
  :profiles {:dev {:dependencies [
                                   [midje "1.5.1"]
                                   [clj-ns-browser "1.3.1"]
                                 ]}}
  :jvm-opts ^:replace []  ; Stop Leiningen from turning off JVM optimisations - makes it slower to start but ensures code runs as fast as possible
  :main depends.core)
