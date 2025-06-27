// 使用 docker 來執行，這樣就不用安裝 scala-cli 了
// docker run --rm -v $(pwd):/workspace -w /workspace virtuslab/scala-cli learning-notes/2-learn-trait.scala

case class Merchant(
    id: String,
    name: String,
    email: String,
    isActive: Boolean = true
)

case class Order(
    id: String,
    merchantId: String,
    amount: Double,
    status: OrderStatus
)

sealed trait OrderStatus
case object Pending extends OrderStatus
case object Processing extends OrderStatus
case class Completed(transactionId: String) extends OrderStatus
case class Failed(reason: String) extends OrderStatus

trait Payable {
  def processPayment(amount: Double): Either[String, String]
  def validateAmount(amount: Double): Boolean = amount > 0.0
  def formatAmount(amount: Double): String = f"$$${amount}%.2f"
}

case class PaymentProcessor(merchantId: String) extends Payable {
  def processPayment(amount: Double): Either[String, String] = {
    if (!validateAmount(amount)) {
      Left("Invalid amount") // Left is Error in Rust
    } else {
      Right(s"Payment processed: ${formatAmount(amount)} for $merchantId") // Right is Ok in Rust
    }
  }
}

object MerchantService {
  private val merchantsInDb = Map (
    "mer_demo_1" -> Merchant("mer_demo_1", "Demo Corp", "demo_corp@gmail.com"),
    "mer_demo_2" -> Merchant("mer_demo_2", "Demo Corp 2", "demo_corp_2@gmail.com")
  )
  
  private val ordersInDb = List(
      Order("order_1", "mer_demo_1", 100.0, Pending),
      Order("order_2", "mer_demo_1", 200.0, Completed("tx_1")),
      Order("order_2", "mer_demo_1", 100.0, Completed("tx_2")),
      Order("order_3", "mer_demo_2", 50.0, Failed("Card declined"))
  )
  
  def findMerchant(id: String): Option[Merchant] = merchantsInDb.get(id)
  
  def findOrdersByMerchant(merchantId: String): List[Order] = ordersInDb.filter(_.merchantId == merchantId)

  def categorizeOrder(order: Order): String = order match {
    case Order(_, merchantId, amount, Completed(txnId)) if amount > 100 => 
      s"Completed High-Value Order: $merchantId, $amount, $txnId"
    case Order(_, _, amount, Completed(txnId)) => 
      s"Completed Order: $amount, $txnId"
    case Order(_, "mer_demo_1", _, _) => "Demo Corp"
    case _ => "Regular order"
  }

  // 學 for-comprehension
  def getMerchantSummary(merchantId: String): Option[String] = for {
    merchant <- findMerchant(merchantId) // Option/ Either 的話，用 <- 來取 Some/ Right 的值
    if merchant.isActive
    orders = findOrdersByMerchant(merchantId) // List 的話，用 = 來取值
    if orders.nonEmpty
  } yield {
    val totalAmount = orders.map(_.amount).sum
    val completedCount = orders.count(_.status match {
      case Completed(_) => true
      case _ =>false
    })
    val allOrderCategories = orders.map{ order => categorizeOrder(order) }.mkString("\n")
    s"Merchant ${merchant.name}, Orders: ${orders.length}, Completed: $completedCount, totalAmount: $totalAmount \n\n All Order Summary \n$allOrderCategories"
  }
}


object DemoMain extends App { // extends App 之後這個就是 Main function
    println("Start")

    println("\n===1. Learn class===")
    val merchant = Merchant("mer_demo_1", "Demo Corp", "demo_corp@gmail.com")
    println(s"Created merchant: $merchant")

    println("\n===2. Learn update class===")
    val updatedMerchant = merchant.copy(isActive = false)
    println(s"Updated merchant: $updatedMerchant")

    // 拆解字段
    println("\n===3. Learn class destruct===")
    val Merchant(id, name, _, _) = merchant // 按照 case class 裡面 parameter 的順序
    println(s"Merchant ID= $id, Name=$name")

    println("\n===4. Learn Trait===")
    val paymentProcessor = PaymentProcessor(merchant.id)
    val paymentResult = paymentProcessor.processPayment(100.0)
    println(s"Payment result: $paymentResult")

    println("\n===5. Learn List===")
    val orders = List(
      Order("order_1", merchant.id, 100.0, Pending),
      Order("order_2", merchant.id, 200.0, Completed("tx_1")),
      Order("order_3", merchant.id, 50.0, Failed("Card declined"))
    )
    println(s"Order: $orders")
    
    println("\n===6. Learn object===")
    val foundMerchant = MerchantService.findMerchant(merchant.id)
    println(s"Found merchant: $foundMerchant.")
    
    println("\n===7. Learn match===")
    foundMerchant match {
      case Some(merchant) => // 不需要大括號
        val foundOrders = MerchantService.findOrdersByMerchant(merchant.id)
        println(s"Found orders: $foundOrders")
      case _ => println("Merchant Not found")
    }

    println("\n===7.1. Learn fold===")
    // 使用 fold 來簡化 match 的寫法
    foundMerchant.fold(
      println("Merchant Not found")  // None 的情況
    ) { merchant =>                   // Some 的情況
      val foundOrders = MerchantService.findOrdersByMerchant(merchant.id)
      println(s"Found orders: $foundOrders")
    }
    
    println("\n===8. Learn for comprehension===")
    val summary = MerchantService.getMerchantSummary(merchant.id)
    println(s"Summary: $summary")
    
    println("\n===9. Learn foreach===")
    orders.foreach { order =>
      println(s"Order: $order")
    }
    
    println("\n===10. Learn map===")
    val combinedOrderSummary = orders.map{ order =>
      s"${order.id}, ${order.status}"
    }.mkString(", ")
    println(s"Summary: $combinedOrderSummary")
    
    println("\n===11. Learn getOrElse===")
    val noDataMerchantSummary = MerchantService.getMerchantSummary("mer_not_exist_id").getOrElse("No Data")
    println(s"MerchantSummary: $noDataMerchantSummary")
    
    println("\n\nFinished")
}

