(ns jcl2groovy.parser
  (:gen-class))

(require '[clojure.string :as string])

(defn parse-statement
  "Parses an individual JCL statement."
  [line]
  (when (re-matches #"^//[^ ]* +[^ ]+.*$" line)
    (list
     ;; type of the statement
     (string/replace (re-find #"^//[^ ]* +[^ ]+" line) #"^//[^ ]* +" "")

     ;; name field
     (string/replace (re-find #"^//[^ ]*" line) #"^//" "")

     ;; parameters
     (string/replace line #"^//[^ ]* +[^ ]+ *" ""))))

(defn parse-parameters-1
  "Split the parameter field."
  [parameter-field]

  (if (= (count parameter-field) 0)
    []
    (loop [in parameter-field out [] temp "" depth 0 in-string? false]
     (if (= (count in) 0)
       (conj out temp)
       (let [in-first (subs in 0 1) in-rest (subs in 1)]
         (condp = in-first
           "'" (recur in-rest out (str temp in-first) depth (not in-string?))
           "(" (if in-string?
                 (recur in-rest out (str temp in-first) depth in-string?)
                 (recur in-rest out (str temp in-first) (+ depth 1) in-string?))
           ")" (if in-string?
                 (recur in-rest out (str temp in-first) depth in-string?)
                 (recur in-rest out (str temp in-first) (- depth 1) in-string?))
           "," (if (or in-string? (> depth 0))
                 (recur in-rest out (str temp in-first) depth in-string?)
                 (recur in-rest (conj out temp) "" depth in-string?))
           (recur in-rest out (str temp in-first) depth in-string?)))))))

(defn parse-parameters-2
  "Convert the split parameters to a map."
  [parameters]

  (letfn [(parse-subparameters [subparameters]
   (if (re-matches #"^\(.*\)$" subparameters)
     (->> (string/replace subparameters #"(^\(|\)$)" "") parse-parameters-1 parse-parameters-2)
     (string/trim subparameters)))]

  (loop [parameters parameters
         out        {:positional []}]
    (if (pos? (count parameters))
      (if (re-matches #"^[A-Z@#$]+=.+" (nth parameters 0))
        ;; keyword parameter
        (recur (vec (rest parameters)) (assoc out (re-find #"^[A-Z@#$]+" (first parameters)) (parse-subparameters (string/replace (first parameters) #"^[A-Z@#$]+=" ""))))
        ;; positional parameter
        (recur (vec (rest parameters)) (assoc out :positional (conj (get out :positional) (parse-subparameters (first parameters))))))
      out))))

(defn parse-jcl
  "Parses the preprocessed JCL."
  [jcl-lines]
  (loop [i          0
         parse-tree []
         last-dd    nil
         in-stream  nil
         dlm        "/*"]
    (if (< i (count jcl-lines))

       ;; then
      (let [line                   (nth jcl-lines i)
            [type name parameters] (parse-statement line)
            parameters             (->> parameters parse-parameters-1 parse-parameters-2)]
        (cond
          ;; in-stream data set
          (not (nil? last-dd))
          (cond
            (= line dlm) ;; in-stream data set ended by delimiter
            (recur
             (inc i)
             (conj parse-tree (list :dd (nth last-dd 1) (assoc (nth last-dd 2) :in-stream in-stream)))
             nil
             []
             "/*")

            (re-matches #"//.+" line) ;; in-stream data set ended by statement
            (recur
             i
             (conj parse-tree (list :dd (nth last-dd 1) (assoc (nth last-dd 2) :in-stream in-stream)))
             nil
             []
             "/*")

            :else ;; line is part of the in-stream data set
            (recur (inc i) parse-tree last-dd (conj in-stream line) dlm))

          ;; comment
          (re-matches #"^//\*.*" line)
          (recur (inc i)
                 (conj parse-tree
                       (list :comment (subs line 3 (count line)))) nil nil "/*")

          (= type "JOB")
          (recur (inc i) (conj parse-tree (list :job name parameters)) nil nil "/*")

          (= type "EXEC")
          (recur (inc i) (conj parse-tree (list :exec name parameters)) nil nil "/*")

          (= type "DD")
          (if (and (vector? (:positional parameters)) (= "*" (first (:positional parameters))))
            (recur (inc i) parse-tree (list :dd name parameters) [] (get parameters "DLM" "/*"))
            (recur (inc i) (conj parse-tree (list :dd name parameters)) nil nil "/*"))

          (= type "SET")
          (recur (inc i) (conj parse-tree (list :set name parameters)) nil nil "/*")

          (= type "IF")
          (recur (inc i) (conj parse-tree (list :if name parameters)) nil nil "/*")

          (= type "ELSE")
          (recur (inc i) (conj parse-tree (list :else name parameters)) nil nil "/*")

          (= type "ENDIF")
          (recur (inc i) (conj parse-tree (list :endif)) nil nil "/*")

          :else
          (recur (inc i) (conj parse-tree (list :unknown line)) nil nil "/*")))

       ;; else
      (apply list parse-tree))))

(defn build-syntax-tree
  "Creates the syntax tree from the parse tree."
  [parse-tree]
  (loop [i 0 state :normal syntax-tree [] temp []]
    (if (< i (count parse-tree))

      (if (= state :exec)
        (condp = (nth (nth parse-tree i) 0)
          :exec (recur (+ 1 i) :exec (conj syntax-tree (apply list temp)) (vec (nth parse-tree i)))
          ;:if (recur (+ 1 i) :exec (conj syntax-tree (nth parse-tree i)) temp)
          ;:else (recur (+ 1 i) :exec (conj syntax-tree (nth parse-tree i)) temp)
          ;:endif (recur (+ 1 i) :exec (conj syntax-tree (nth parse-tree i)) temp)
          (recur (+ 1 i) :exec syntax-tree (conj temp (nth parse-tree i))))

        (if (= (nth (nth parse-tree i) 0) :exec)
          (recur (+ 1 i) :exec syntax-tree (vec (nth parse-tree i)))
          (recur (+ 1 i) :normal (conj syntax-tree (nth parse-tree i)) temp)))

      (if (= state :exec)
        (apply list (conj syntax-tree (apply list temp)))
        (apply list syntax-tree)))))
