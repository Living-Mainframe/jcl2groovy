(ns jcl2groovy.core-test
  (:require [clojure.test :refer :all]
            [jcl2groovy.core :refer :all]))

(require '[jcl2groovy.preprocessor.cleanup :as cleanup])

;;(deftest test-remove-comments-1
  ;;(testing
    ;; (test-remove-comments
    ;;  '("//TESTJOB JOB PARM        COMMENT 1"
    ;;    "//TESTDD  DD  PARM1,PARM2 COMMENT 2")
    ;;  '("//TESTJOB JOB PARM"
    ;;    "//TESTDD  DD  PARM1,PARM2"))
    
    ;; (test-remove-comments
    ;;  '("//TESTSTEP EXEC PARM  COMMENT                                          X"
    ;;    "// CONTINUED COMMENT LINE 1                                            X"
    ;;    "// CONTINUED COMMENT LINE 2")
    ;;  '("//TESTSTEP EXEC PARM"))
     
    ;; (test-remove-comments
    ;;  '("//TESTSTEP EXEC PARM1,"
    ;;    "// PARM2 COMMENT                                                          X"
    ;;    "// CONTINUED COMMENT LINE 1                                               X"
    ;;    "// CONTINUED COMMENT LINE 2")
    ;;  '("//TESTSTEP EXEC PARM1,"
    ;;    "// PARM2"))
    ;; ))

(deftest test-remove-comment-fields
  (letfn [(t [lines expected] (is (= (cleanup/remove-comment-fields lines) expected)))]
    (testing
      
      (t [] [])

      (t ["//TEST DD PARM1, COMMENT"
          "          PARM2"]
         ["//TEST DD PARM1,"
          "          PARM2"])

      ;;(t ["//TEST DD PARM1 COMMENT"]
      ;;   ["//TEST DD PARM1"])
      
      )))

(deftest test-join-statements
  (letfn [(t [lines expected] (is (= (cleanup/join-statements lines) expected)))]
    (testing

      (t [] [])

      (t ["//TEST JOB PARM1," "// PARM2"]
         ["//TEST JOB PARM1,PARM2"])

      (t ["//TEST JOB PARM1," "// PARM2," "// PARM3"]
         ["//TEST JOB PARM1,PARM2,PARM3"])

      (t ["//TEST EXEC PARM='this is a" "//              long parameter'"]
         ["//TEST EXEC PARM='this is a long parameter'"])

      (t ["//TEST EXEC PARM='this is a" "//              very" "//              long parameter'"]
         ["//TEST EXEC PARM='this is a very long parameter'"]))))
