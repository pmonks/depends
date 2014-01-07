;
; Copyright © 2013,2014 Peter Monks (pmonks@gmail.com)
;
; This work is licensed under the Creative Commons Attribution-ShareAlike 3.0
; Unported License. To view a copy of this license, visit
; http://creativecommons.org/licenses/by-sa/3.0/ or send a letter to Creative
; Commons, 444 Castro Street, Suite 900, Mountain View, California, 94041, USA.
;

(ns depends.svgwriter
  (:require [clojure.tools.logging :as log]
            [clojure.java.io       :as io]
            ))

(comment
(defn add-ns-names
  [map deps]
  (reduce (fn [map nsname]
            (add-node map (keyword nsname) nsname :width 250))
          map
          (set (concat (keys deps) (apply concat (vals deps))))))

(defn link-ns-to-deps
  [map ns ns-which-used-it]
  (reduce (fn [map n]
            (printf "%s -> %s;\n" n ns)
            (add-edge map (geneid) (keyword ns) (keyword n)))
          map
          ns-which-used-it))

(defn add-ns-links
  [map deps]
  (reduce (fn [map nsname]
            (link-ns-to-deps map nsname (deps nsname)))
          map
          (keys deps)))

(defn build-svggraph
  [deps]
  (-> (graph)
      (add-ns-names deps)
      (add-ns-links deps)
      (layout :hierarchical)
      (build)))
)