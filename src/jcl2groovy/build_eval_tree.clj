(ns jcl2groovy.build-eval-tree
  (:gen-class))

(require '[clojure.string :as string])
(require '[jcl2groovy.utils :as jcl-utils])

(defn exec-cond
  "Generate an if statement from the COND paramater of an EXEC statement."
  [parameters]
  (let
   [operators
    {
     "GT" ">",
     "GE" ">=",
     "EQ" "==",
     "LT" "<",
     "LE" "<=",
     "NE" "!="
    }]
    (if (not (nil? (get parameters "COND")))
      (if (= (count (:positional (get parameters "COND"))) 2)
        (str
         "    if(rc "
         (get operators (nth (:positional (get parameters "COND")) 1))
         " "
         (nth (:positional (get parameters "COND")) 0)
         ") return\n\n")
        (str "    // @TODO condition: " (get parameters "COND") "\n"))
      ""))
  )

; map of keyword → function
(def funcs
  {:comment
   (fn [c] (str "//" c "\n"))
   
   :unknown
   (fn [& _] "")

   :job
   (fn [name _]
     (str
      "// job " name "\n"
      ;"steps = []\n"
      ;"rc = 0\n"
      ;"symbol = [:]\n"
      "\n"))

   :exec 
   (fn [name parameters & statements]
     (str
      "// step " name "\n"
      "steps.add({\n"
      (exec-cond parameters)
      "    stepname = '" name "'\n"
      "    MVSExec mvsexec = new MVSExec()\n"
      "    mvsexec.setPgm('" (get parameters "PGM") "')\n"
      "    mvsexec.setParm(\"\"\"" (string/replace (get parameters "PARM" "") #"(^'|'$)" "") "\"\"\")\n"
      (string/join statements)
      "    rc = mvsexec.execute()\n"
      "})\n// end " name "\n\n"))
   
   :dd
   (fn [name parameters & _]
     (str
      "    mvsexec.dd(new DDStatement()"

      ;; name
      (condp = name
        "STEPLIB" "\n        .name('TASKLIB')"
        "" ""
        (str "\n        .name('" name "')"))

      ;; dsn
      (if (not (nil? (get parameters "DSN")))
        (str "\n        .dsn(\"" (get parameters "DSN") "\")")
        "")
      
      ;; disp
      (let [disp (if (map? (get parameters "DISP")) (:positional (get parameters "DISP" "")) [(get parameters "DISP" "")])]
        (str
         "\n        .options('"
         (string/join " " (map string/lower-case (filter (fn [x] (not= "PASS" x)) disp)))
         "')"
         (if (some (fn [x] (= "PASS" x)) disp)
           "\n        .pass(true)"
           "")))

      ;; in-stream data set
      (if (nil? (:in-stream parameters))
        ""
        (str
         "\n        .instreamData(\n            \"\"\""
         (string/join "\"\"\" +\n            \"\"\"" (:in-stream parameters))
         "\"\"\")"))
      ")\n"

      ;; copy to hfs
      (if (= "*" (get parameters "SYSOUT"))
        (str "    mvsexec.copy(new CopyToHFS().ddName(\"" name "\").file(new File(\"/dev/fd0\")))\n")
        "")))
   
   :set
   (fn [_ param]
     (string/join
      (for [symbol (keys (dissoc param :positional))]
        (str "    symbol." (jcl-utils/jcl-name-to-groovy-name symbol) " = '" (string/replace (get param symbol) #"(^'|'$)" "") "'\n"))))
   
   :if
   (fn [_ condition] (str "// @TODO if " condition "\n"))
   
   :else
   (fn [& _] "// else\n")
   
   :endif
   (fn [& _] "// endif\n")})

(defn build-eval-tree [syntax-tree]
  (map
   (fn [e]
     (cond
       (keyword? e) (e funcs)
       (seq? e) (build-eval-tree e)
       :else e))
   syntax-tree))