(ns jcl2groovy.preprocessor.cleanup
  (:gen-class))

(require '[clojure.string :as string])

(defn gets-continued?
  "Checks if and how `line` gets continued by the following line.
   `is-continuation` describes if `line` is itself a continuation of the previous line."
  [line is-continuation]
  (cond
    ;; comment statements can not be continued
    (re-matches #"^//\*.*$" line)
    :no-continuation

    ;; line starts a continued parameter enclosed in '
    (and (not= is-continuation :continuation-apostrophe) (odd? (count (re-seq #"'" line))))
    :continuation-apostrophe

    ;; line continues a further continued parameter enclosed in '
    (and (= is-continuation :continuation-apostrophe)  (even? (count (re-seq #"'" line))))
    :continuation-apostrophe

    ;; line ends a continued parameter enclosed in '
    (and (= is-continuation :continuation-apostrophe) (odd? (count (re-seq #"'" line))))
    :no-continuation

    ;; line ends with ,
    (= \, (last line))
    :continuation-comma

    ;; line doesn't get continued
    :else
    :no-continuation))

;; (defn remove-comment-fields
;;   "Remove the comment field from all `jcl-lines`."
;;   [jcl-lines]

;;   (letfn [(remove-comment-from-line
;;       ;; Removes the comment field from a given line
;;             [line is-continuation]
;;             line)]

;;     (loop [i 0 is-comment false is-continuation :no-continuation lines []]
;;       (if (< i (count jcl-lines))

;;         (let [line (nth jcl-lines i) next-is-commment (> (count line) 71) next-is-continuation false]
;;           (if is-comment

;;             ;; ignore lines that continue a comment field
;;             (recur (+ 1 i) next-is-commment next-is-continuation lines)

;;             ;; if the statement has a comment field, remove it
;;             (recur (+ i 1) next-is-commment next-is-continuation (conj lines (remove-comment-from-line line is-continuation)))))

;;         lines))))

(defn remove-comment-fields
  "Remove the comment field from all `jcl-lines`."
  [jcl-lines]
  (map
   (fn
     [line]
     (string/replace line #", +.*$" ",")) ;; TODO: remove all comment fields
   
   jcl-lines))

(defn remove-comment-statements
  "Remove all JCL comment statements from `jcl-lines`."
  [jcl-lines]
  (let [comment-re #"^//\*.*"]
    (filter (fn [line] (not (re-matches comment-re line))) jcl-lines)))

(defn join-statements
  "Join continued JCL statements, comment fields have to be removed first"
  [jcl-lines]

  (if (> (count jcl-lines) 1)

    (loop [i 1 lines [(nth jcl-lines 0)] is-continuation :no-continuation]
      (if (< i (count jcl-lines))

        (let [line (nth jcl-lines i) is-continuation (gets-continued? (nth jcl-lines (- i 1)) is-continuation)]
          (cond

            (= is-continuation :continuation-comma)
            (recur
             (+ 1 i)
             (conj
              (vec (take (- (count lines) 1) lines))
              (str (last lines) (string/replace line #"// +" ""))) is-continuation)

            (= is-continuation :continuation-apostrophe)
            (recur
             (+ 1 i)
             (conj
              (vec (take (- (count lines) 1) lines))
              (str (last lines) (string/replace line #"// {13}" ""))) is-continuation)

            :else
            (recur (+ 1 i) (conj lines line) is-continuation)))

        lines))

    jcl-lines))
