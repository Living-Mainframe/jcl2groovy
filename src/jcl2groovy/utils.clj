(ns jcl2groovy.utils)

(require '[clojure.string :as string])

(defn jcl-name-to-groovy-name
  "Replaces characters that are valid in JCL identifiers but invalid in Groovy identifiers."
  [name]
  (->
   name
   (string/replace "$" "d_")
   (string/replace "@" "a_")
   (string/replace "#" "h_")))