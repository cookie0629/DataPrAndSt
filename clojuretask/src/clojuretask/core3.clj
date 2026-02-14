(ns clojuretask.core3
  (:require [clojure.test :as test]))

;; 对单个数据块进行过滤，并包装成 future 实现异步执行
(defn internalFilter [function block]
  (future (doall (filter function block))))

(defn parallelFilterInternal [function seq]
  (let [size 3]
    (->> (partition-all size seq) ;;将序列分成每3个元素一组的小块
         (map #(internalFilter function %))                 ;; 对每个块应用internalFilter函数，创建future
         (doall)                                            ;; 强制对map返回的惰性序列进行求值，确保所有future都启动
         (map deref)                                        ;; 获取每个future的结果（deref会阻塞直到future完成）
         (concat))))                                        ;; 连接所有块的结果


;;将序列分成每200个元素一组的大块,对每个大块调用 parallelFilterInternal 进行并行处理
(defn parallelFilter [function seq]
  (flatten (apply concat (map #(parallelFilterInternal function %)
                              (partition-all 200 seq)))))

(defn check [a]
  (Thread/sleep 10)
  (even? a))

(defn bigSeq [n]
  (take n (iterate inc 1)))

(time (println (->> (iterate inc 1)
                    (parallelFilter check)
                    (take 1000)
                    )))

(time (println (->> (iterate inc 1)
                    (filter check)
                    (take 1000)
                    )))

(test/deftest lab3Test
  (test/testing "action:"
    (test/is (= (parallelFilter even? (bigSeq 10)) (list 2 4 6 8 10)))))




;; 注代码使用future为每个块创建单独的线程
;; 线程数量没有显式控制，由Clojure运行时管理future的线程池
;; 这种实现适合I/O密集型或计算密集型的过滤操作，可以充分利用多核CPU的优势
;; 但对于小数据集或简单操作，线程创建和管理的开销可能会超过并行化的收益