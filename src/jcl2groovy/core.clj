(ns jcl2groovy.core
  (:gen-class))

(require '[clojure.java.io :as io])
(require '[clojure.tools.cli :as cli])
(require '[clojure.pprint :as pprint])
(require '[clojure.string :as string])
(require '[jcl2groovy.preprocessor :as pre])
(require '[jcl2groovy.parser :as jcl-parser])
(require '[jcl2groovy.build-eval-tree :as build-eval-tree])

(defn find-todo
  "Find '@TODO' in the `groovy-code`."
  [groovy-code]
  (filter (fn [l] (.contains l "@TODO"))
          (map
           (fn [line number] (str number ": " (re-find #"@TODO.*" line)))
           (string/split-lines groovy-code)
           (iterate inc 1))))

(defn transpile-jcl
  [input-file include-paths debug? todo?]
  (with-open [jcl-reader (clojure.java.io/reader input-file)]
    
    (let [;; read the input file
          jcl-lines (line-seq jcl-reader)
          ;; preprocessing
          preprocessed-jcl (pre/preprocess-jcl jcl-lines include-paths)
          ;; parsing
          parse-tree (jcl-parser/parse-jcl preprocessed-jcl)
          syntax-tree (jcl-parser/build-syntax-tree parse-tree)
          ;; keywords → functions
          eval-tree (build-eval-tree/build-eval-tree syntax-tree)
          ;; generate groovy code
          groovy-code (apply str (map eval eval-tree))
          ;; add header and footer
          groovy-code (str (slurp (io/resource "header.groovy")) groovy-code (slurp (io/resource "footer.groovy")))
          ]
      
      (spit (str input-file ".groovy") groovy-code)

      (when todo?
        (doall (map println (find-todo groovy-code))))

      ;; write the intermediate representations ?
      (when debug?
        (with-open [preprocessed-writer (clojure.java.io/writer (str input-file ".preprocessed-jcl"))
                    parse-tree-writer (clojure.java.io/writer (str input-file ".parse-tree"))
                    syntax-tree-writer (clojure.java.io/writer (str input-file ".syntax-tree"))
                    eval-tree-writer (clojure.java.io/writer (str input-file ".eval-tree"))]
          (pprint/pprint preprocessed-jcl preprocessed-writer)
          (pprint/pprint parse-tree parse-tree-writer)
          (pprint/pprint syntax-tree syntax-tree-writer)
          (pprint/pprint eval-tree eval-tree-writer))))
    
    
    ))

(def cli-options
  [["-I" "--include PATH" "INCLUDE path" 
    :multi true
    :default [] 
    :update-fn conj]
   ["-g" "--debug" "dump debug information"]
   ["-t" "--todo" "print generated @TODO comments"]
   ["-h" "--help" "print this message"]])

(defn -main
  "Main function"
  [& args]

  (try
    ;; parse commandline arguments
    (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)]
      (cond
        errors
        (do (doall (map println errors))
            (System/exit 1))

        (:help options)
        (do (println "jcl2groovy [options] input.jcl")
            (println summary)
            (System/exit 0))

        (< (count arguments) 1)
        (do (println "No input file specified")
            (System/exit 1))

        (> (count arguments) 1)
        (do (println "Too many input files specified")
            (System/exit 1))

        :else
        (transpile-jcl (nth arguments 0) (:include options) (:debug options) (:todo options))))

    (catch Exception e (do (println (.getMessage e)) (System/exit 1)))))
