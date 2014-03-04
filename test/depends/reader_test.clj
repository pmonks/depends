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

(ns depends.reader-test
  (:require [clojure.java.io :as    io]
            [midje.sweet     :refer :all]
            [depends.reader  :refer :all]))

(facts "About testCase1.class"
  (class-info "test/testCase1.class") => [{:class-version 51
                                           :class-version-str "1.7"
                                           :name "testCase1"
                                           :package nil
                                           :source "test/testCase1.class"
                                           :type :class
                                           :typename "testCase1"}
                                         #{{:source "testCase1" :target "java.lang.Object" :type :extends}
                                           {:source "testCase1" :target "void"             :type :uses}
                                           {:source "testCase1" :target "java.lang.Object" :type :uses}}]
  (class-info "test/testCase1.class" nil) => [{:class-version 51
                                               :class-version-str "1.7"
                                               :name "testCase1"
                                               :package nil
                                               :source nil
                                               :type :class
                                               :typename "testCase1"}
                                              #{{:source "testCase1" :target "java.lang.Object" :type :extends}
                                                {:source "testCase1" :target "void"             :type :uses}
                                                {:source "testCase1" :target "java.lang.Object" :type :uses}}])

