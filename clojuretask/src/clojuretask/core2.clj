(ns clojuretask.core2
  (:require [clojure.test :as test]
            [clojure.string :as str]))

;; 过滤:移除所有能被该素数整除的数 递归:对剩余序列重复此过程
(defn sieve [start]
  (cons (first start)
        (lazy-seq
          (sieve (filter #(not= 0 (mod % (first start)))
                         (rest start))))))

;; 定义一个无限素数序列 primeSieve
(def primeSieve (sieve (iterate inc 2)))

;; 取前 n 个素数
(defn takePrimes [n]
  (take n primeSieve))

;; 用于测量获取第n个素数所花费的时间
(defn takeTimeSieveInString [n]
  (nth (str/split
        (with-out-str (time (nth primeSieve n)))
        #" ")
       2))

;; 将字符串时间转为浮点数
(defn takeTimeFloat [n]
  (Float/parseFloat (takeTimeSieveInString n)))

(defn run-tests []
  (test/deftest Task02Test
    (test/testing "Prime sieve tests:"
      ;; 打印第1000个素数的耗时
      (println "Time for 1000th prime:" (takeTimeFloat 1000) "ms")

      ;; 验证多次取同一个素数是否一致
;;       (println "999th prime:" (nth primeSieve 999))
      (time (println "999th prime:" (nth primeSieve 999)))
;;       (println "999th prime again:" (nth primeSieve 999))
      (time (println "999th prime again:" (nth primeSieve 999)))

      ;; 验证第一个素数是2
      (test/is (= (nth primeSieve 0) 2))

      ;; 验证前5个素数序列是否正确
      (test/is (= (takePrimes 5) [2 3 5 7 11])))))

(defn -main []
  (println "==========")
  (run-tests)
  ;; 使用 clojure.test/run-tests 运行所有测试，并打印结果
  (test/run-tests 'clojuretask.core2)
  (println "============="))

(-main)
