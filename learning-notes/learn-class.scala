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

object DemoMain extends App { // extends App 之後這個就是 Main function
    val merchant = Merchant("1", "Demo Corp", "demo_corp@gmail.com")
    println(s"1. Create $merchant")

    val updatedMerchant = merchant.copy(isActive = false)
    println(s"2. Update merchant $merchant to $updatedMerchant")

    // 拆解字段
    val Merchant(id, name, _, _) = merchant // 按照 case class 裡面 parameter 的順序
    println(s"3. class destruct: ID= $id, Name=$name")
}
