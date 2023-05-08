(ns jcl2groovy.parser-test
  (:require [clojure.test :refer :all]
            [jcl2groovy.parser :refer :all]))

(deftest test-parse-parameters-1
  (letfn [(t [input expected] (is (= (parse-parameters-1 input) expected)))]
    (testing

      (t [] [])
      
      (t "A,B,C"
         ["A" "B" "C"])
      
      (t "A,(B,C)"
         ["A" "(B,C)"])
      
      (t "A='B,C'"
         ["A='B,C'"])
      
      (t "A=''''"
         ["A=''''"])
      
      (t "A=',',B"
         ["A=','" "B"])
      )))

(deftest test-parse-parameters-2
  (letfn [(t [input expected] (is (= (parse-parameters-2 input) expected)))]
    (testing

     (t [] {:positional []})
      
     (t ["A" "B"] {:positional ["A" "B"]})
      
     (t ["A=B"] {:positional [] "A" "B"})
      
     (t ["A" "B=C"] {:positional ["A"] "B" "C"})
      
     (t ["A" "B='='"] {:positional ["A"] "B" "'='"})
     
     (t ["(A,B)"] {:positional [{:positional ["A" "B"]}]})
      
     (t ["A=(B,C=D)" "E"] {:positional ["E"] "A" {:positional ["B"] "C" "D"}})
     )))
    
      