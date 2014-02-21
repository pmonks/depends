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