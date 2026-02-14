(ns clojuretask.core1)

;; Отфильтрировать буквы алфавита, которые совпадают с последним символом слова
(defn filterLast [word alphabet]
  (apply list (filter #(not= (last word) %) alphabet)))

;;Сгенерировать все возможные расширения для одного слова
(defn foo [word alphabet]
  ; 先将单词拆分成字符列表,使用map将每个过滤后的字母附加到word后面
  (apply list (map #(str word %) (filterLast (clojure.string/split word #"") alphabet))))

;; Обработывать список слов и сгенеририровать расширение для каждого слова
(defn bar [words alphabet]
  (apply concat (map #(foo % alphabet) words)))

;; Сгенерировать все комбинации строк длины n
(defn c1 [alphabet n]
  (reduce bar (repeat n alphabet)))

(defn -main [& args]
  (println (c1 (list "a" "b" "c" "d") 4)))

