(ns clojuretask.core5
  (:gen-class)) ;;

(defn philosopher-behavior [id left-fork right-fork {:keys [rounds eat-ms think-ms]}]
  (println (format ">> Философ %d готов" id))
  (dotimes [r rounds]
    ;; 阶段 1: 思考
    (println (format "  [Философ %d] Раунд %d/%d: Думает..." id (inc r) rounds))
    (Thread/sleep think-ms)

    ;; 阶段 2: 进餐 (使用 STM 事务)
    (dosync
     ;; 尝试获取左手和右手的叉子 (更新引用计数)
     (alter left-fork update :use-count inc)
     (alter right-fork update :use-count inc)

     ;; 模拟进餐过程
     (println (format "  [Философ %d] !!! Взял вилки, начинает есть !!!" id))
     (Thread/sleep eat-ms))

    (println (format "  [Философ %d] Положил вилки, раунд завершен." id))))

(defn run-simulation [n-philosophers rounds]
  (println "\n=== Инициализация симуляции ===")
  ;; 创建叉子 (使用 Ref 进行状态管理)
  (let [forks (vec (for [i (range n-philosophers)]
                     (ref {:id i :use-count 0})))

        ;; 启动哲学家线程
        threads (doall
                 (for [i (range n-philosophers)]
                   (future
                     (let [left (nth forks i)
                           ;; 简单的防死锁策略：让最后一个哲学家反向拿叉子，或者依赖 STM 的重试机制
                           ;; 这里我们直接依赖 STM (Clojure 的 dosync 会自动处理死锁重试)
                           right (nth forks (mod (inc i) n-philosophers))]
                       (philosopher-behavior i left right
                                             {:rounds rounds
                                              :eat-ms 1000   ;; 进餐耗时 1秒 (为了让你看清)
                                              :think-ms 500} ;; 思考耗时 0.5秒
                                             )))))]
    ;; 等待所有线程结束
    (doseq [t threads] @t)

    ;; 返回最终的叉子状态
    forks))

(defn -main [& args]
  (println "       Запуск симуляции «Обедающие философы» (Терминальная версия)       ")

  (let [n 5       ;; 5个哲学家
        rounds 2] ;; 每个人吃2轮 (为了演示不用太长)

    (let [final-forks (run-simulation n rounds)]

      (println "              Симуляция завершена — Статистика            ")
      (doseq [f final-forks]
        (let [state @f]
          (println (format "ID вилки: %d | Количество использований: %d" (:id state) (:use-count state)))))))

  (shutdown-agents) ;; 关闭代理线程池，让程序能立即退出
  (println "Готово."))