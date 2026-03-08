(ns clojuretask.core5
  (:gen-class)) ;;

(defn philosopher-behavior [id left-fork right-fork {:keys [rounds eat-ms think-ms]}]
  (println (format ">> Философ %d готов" id))
  (let [total-retries (atom 0)] ;; 记录该哲学家总共经历的回滚次数
    (dotimes [r rounds]
      ;; 阶段 1: 思考
      (println (format "  [Философ %d] Раунд %d/%d: Думает..." id (inc r) rounds))
      (Thread/sleep think-ms)

      ;; 阶段 2: 进餐 (使用 STM 事务)
      (let [attempts (atom 0)]
        (dosync
         ;; 核心技巧：swap! atom 不受 STM 事务回滚的影响。
         (swap! attempts inc)

         ;; 尝试获取左手和右手的叉子 (更新引用计数)
         (alter left-fork update :use-count inc)
         (alter right-fork update :use-count inc)

         ;; 模拟进餐耗时 (注意：为了避免回滚时重复打印，这里不再打印文字)
         (Thread/sleep eat-ms))

        ;; 走出 dosync，说明事务已经成功提交。
        ;; 实际回滚次数 = 总尝试次数 - 1
        (let [rollbacks (dec @attempts)]
          (swap! total-retries + rollbacks)
          (if (> rollbacks 0)
            (println (format "  [Философ %d] Внимание! Съел после %d откатов транзакции." id rollbacks))
            (println (format "  [Философ %d] Успешно! Взял вилки с первой попытки, без откатов." id))))

        (println (format "  [Философ %d] Положил вилки, раунд завершен." id))))

    ;; 线程结束时，返回该哲学家总共遇到的回滚次数
    @total-retries))

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
                           right (nth forks (mod (inc i) n-philosophers))]
                       (philosopher-behavior i left right
                                             {:rounds rounds
                                              :eat-ms 1000   ;; 进餐耗时 1秒
                                              :think-ms 500} ;; 思考耗时 0.5秒
                                             )))))]
    ;; mapv deref 会等待所有 future 结束，并收集它们的返回值
    (let [retry-stats (mapv deref threads)]
      {:final-forks forks
       :retries retry-stats})))

;; --- 2. 主程序入口 ---

(defn -main [& args]
  (println "       Запуск симуляции «Обедающие философы» (Терминальная версия с мониторингом откатов)       ")

  (let [n 5       ;; 5个哲学家
        rounds 2] ;; 每个人吃2轮

    (let [results (run-simulation n rounds)
          final-forks (:final-forks results)
          retries (:retries results)]

      (println "\n              Симуляция завершена — Статистика ресурсов            ")
      (doseq [f final-forks]
        (let [state @f]
          (println (format "ID вилки: %d | Количество использований: %d" (:id state) (:use-count state)))))

      (println "\n              Симуляция завершена — Мониторинг откатов  ")
      (let [total-rollbacks (reduce + retries)]
        (doseq [i (range n)]
          (println (format "Философ %d всего пережил %d откатов транзакции" i (nth retries i))))
        (println (format "-> Всего в системе произошло %d перехватов откатов STM." total-rollbacks)))))

  (shutdown-agents) ;; 关闭代理线程池
  (println "Готово."))