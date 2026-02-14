(ns clojuretask.core4)

(declare supply-msg)
(declare notify-msg)

(defn storage
  [ware notify-step & consumers]
  (let [counter (atom 0 :validator #(>= % 0))  ; 计数器atom，验证器确保值不小于0
        worker-state {:storage     counter,     ; agent的工作状态
                      :ware        ware,
                      :notify-step notify-step,
                      :consumers   consumers}]
    {:storage counter,
     :ware    ware,
     :worker  (agent worker-state)}))          ; 创建agent来管理存储状态

(defn factory
  "创建一个新的工厂
   参数:
     amount - 每个生产周期生产的物品数量
     duration - 生产周期时长（毫秒）
     target-storage - 用于存放产品的目标存储仓库（通过supply-msg发送）
     ware-amounts - 单个生产周期所需的原材料名称和数量列表（交替出现的键值对）
   返回值:
     返回一个包含以下内容的map:
       :worker - 一个agent，用于接收notify-msg消息"
  [amount duration target-storage & ware-amounts]
  (let [bill (apply hash-map ware-amounts)  ; 将参数列表转换为原材料账单map
        ;; 初始化缓冲区，记录已收集的原材料数量
        buffer (reduce-kv (fn [acc k _] (assoc acc k 0))
                          {} bill)
        ;; 工厂agent的状态结构:
        ;;   :amount - 每个周期生产的物品数量
        ;;   :duration - 生产周期时长
        ;;   :target-storage - 存放产品的目标存储仓库
        ;;   :bill - map结构，键为原材料名称，值为所需数量，显示完成一个生产周期需要消耗的原材料
        ;;   :buffer - 与:bill结构相似的map，显示已经收集到的原材料数量；这是唯一可变的部分
        worker-state {:amount         amount,
                      :duration       duration,
                      :target-storage target-storage,
                      :bill           bill,
                      :buffer         buffer}]
    {:worker (agent worker-state)}))

(defn source
  "创建一个源，这是一个线程，每个周期生产'amount'数量的物品并存入'target-storage'
   参数:
     amount - 每个周期生产的物品数量
     duration - 周期时长（毫秒）
     target-storage - 目标存储仓库
   返回值:
     返回一个Thread对象，需要显式启动"
  [amount duration target-storage]
  (new Thread
       (fn []
         (Thread/sleep duration)  ; 等待一个周期
         (send (target-storage :worker) supply-msg amount)  ; 向存储仓库发送供应消息
         (recur))))  ; 递归调用，实现持续生产

(defn supply-msg
  "可以发送给存储仓库worker的消息，通知应该添加指定'amount'数量的物品
   将指定数量的物品添加到存储仓库，并通知所有注册的工厂
   参数:
     state - 存储仓库agent的状态（参见'storage'函数的结构）
     amount - 要添加的物品数量"
  [state amount]
  (swap! (state :storage) #(+ % amount))  ; 更新计数器，这里不会失败
  (let [ware (state :ware)
        cnt @(state :storage)             ; 当前存储数量
        notify-step (state :notify-step)  ; 日志记录步长
        consumers (state :consumers)]     ; 消费者工厂列表
    ;; 日志记录部分，notify-step == 0 表示不记录日志
    (when (and (> notify-step 0)
               (> (int (/ cnt notify-step))     ; 检查是否跨过了某个步长阈值
                  (int (/ (- cnt amount) notify-step))))
      ;; 格式化输出当前时间和存储状态
      (println (.format (new java.text.SimpleDateFormat "hh.mm.ss.SSS") (new java.util.Date))
               "|" ware "amount: " cnt))
    ;; 工厂通知部分
    (when consumers
      ;; 随机顺序通知所有消费者，避免顺序依赖
      (doseq [consumer (shuffle consumers)]
        (send (consumer :worker) notify-msg ware (state :storage) amount))))
  state)  ; agent本身是不可变的，只保存配置信息

(defn notify-msg
  "可以发送给工厂worker的消息，通知指定'amount'数量的'ware'物品
   刚刚被放入'storage-atom'存储中
   参数:
     state - 工厂agent的状态（参见'factory'函数中的注释了解详情）
     ware - 原材料名称
     storage-atom - 存储仓库的atom引用
     amount - 可用的原材料数量
   实现逻辑:
     - 尝试从'storage-atom'中获取所需的原材料（如果必要）
     - 如果获取不成功，需要正确处理验证异常
     - 如果获取成功，将原材料放入内部的':buffer'
     - 当所有类型的原材料都按照:bill的要求收集足够时，启动新的生产周期（等待指定时长）
     - 生产周期完成后，从内部的':buffer'中移除已使用的原材料，并通过'supply-msg'通知':target-storage'
     - 在任何情况下都返回新的agent状态（可能修改了':buffer'）"
  [state ware storage-atom amount]
  ;; 第一步：尝试从存储中获取原材料并更新缓冲区
  (let [state1 (assoc state
                 :buffer
                 (let [bill-cnt (get (state :bill) ware)       ; 账单中需要的数量
                       buffer-cnt (get (state :buffer) ware)]  ; 缓冲区中已有的数量
                   (if (> bill-cnt buffer-cnt)  ; 如果还需要更多这种原材料
                     (let [ware-cnt-to-get (min (- bill-cnt buffer-cnt) @storage-atom)]  ; 计算能获取的数量
                       (try
                         ;; 尝试从存储中取出原材料
                         (swap! storage-atom #(- % ware-cnt-to-get))
                         ;; 更新缓冲区
                         (assoc (state :buffer) ware (+ buffer-cnt ware-cnt-to-get))
                         (catch IllegalStateException e
                           ;; 如果取出失败（比如数量不足），保持原缓冲区不变
                           (state :buffer))))
                     (state :buffer))))]  ; 如果不需要更多这种原材料，保持缓冲区不变

    ;; 第二步：检查是否收集够了所有原材料
    (let [not-enough (not-any?
                       #(>= (get (state1 :buffer) %)  ; 检查每种原材料是否足够
                            (get (state1 :bill) %))
                       (keys (state1 :bill)))]
      (if not-enough
        ;; 如果原材料不足，直接返回当前状态
        state1
        ;; 如果原材料充足，开始生产
        (let [;; 从缓冲区中扣除使用的原材料
              state2 (assoc state1
                       :buffer
                       (into {} (map (fn [[k, v]] [k, (- v (get (state1 :bill) k))])
                                     (state1 :buffer))))]
          ;; 等待生产周期完成
          (Thread/sleep (state :duration))
          ;; 生产完成后，向目标存储仓库发送产品
          (send ((state :target-storage) :worker) supply-msg (state :amount))
          ;; 返回更新后的状态
          state2)))))


;; 安全存储：存储"Safe"，日志步长为1
(def safe-storage (storage "Safe" 1))

;; 安全工厂：每个周期生产1个Safe，周期3000ms，需要3个Metal
(def safe-factory (factory 1 3000 safe-storage "Metal" 3))

;; 布谷鸟钟存储：存储"Cuckoo-clock"，日志步长为1
(def cuckoo-clock-storage (storage "Cuckoo-clock" 1))

;; 布谷鸟钟工厂：每个周期生产1个Cuckoo-clock，周期2000ms，需要5个Lumber和10个Gears
(def cuckoo-clock-factory (factory 1 2000 cuckoo-clock-storage "Lumber" 5 "Gears" 10))

;; 齿轮存储：存储"Gears"，日志步长为20，通知布谷鸟钟工厂
(def gears-storage (storage "Gears" 20 cuckoo-clock-factory))

;; 齿轮工厂：每个周期生产4个Gears，周期1000ms，需要4个Ore
(def gears-factory (factory 4 1000 gears-storage "Ore" 4))

;; 金属存储：存储"Metal"，日志步长为5，通知安全工厂
(def metal-storage (storage "Metal" 5 safe-factory))

;; 金属工厂：每个周期生产1个Metal，周期1000ms，需要10个Ore
(def metal-factory (factory 1 1000 metal-storage "Ore" 10))

;; 木材存储：存储"Lumber"，日志步长为20，通知布谷鸟钟工厂
(def lumber-storage (storage "Lumber" 20 cuckoo-clock-factory))

;; 木材厂：源，每4000ms生产5个Lumber到木材存储
(def lumber-mill (source 5 4000 lumber-storage))

;; 矿石存储：存储"Ore"，日志步长为10，通知金属工厂和齿轮工厂
(def ore-storage (storage "Ore" 10 metal-factory gears-factory))

;; 矿石矿：源，每1000ms生产2个Ore到矿石存储
(def ore-mine (source 2 1000 ore-storage))

;;; 启动源和整个生产过程
(defn start []
  (.start ore-mine)   ; 启动矿石矿
  (.start lumber-mill)) ; 启动木材厂

;;; 停止运行中的过程
;;; 停止后需要重新编译代码来重置整个过程
(defn stop []
  (.stop ore-mine)    ; 停止矿石矿
  (.stop lumber-mill)) ; 停止木材厂

;; 启动整个生产系统
(start)