(ns jcl2groovy.preprocessor
  (:gen-class))

(require '[jcl2groovy.preprocessor.cleanup :as cleanup])
(require '[jcl2groovy.preprocessor.resolve :as resolve])

(defn preprocess-jcl
  "The JCL preprocessor: removes comment fields, resolves INCLUDE statements and joins statements."
  [jcl-lines paths]
  (->>
   (resolve/resolve-include jcl-lines paths)
   cleanup/remove-comment-fields
   cleanup/join-statements
   (map resolve/resolve-symbols)))