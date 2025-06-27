// Cats 核心抽象練習
// 模擬 core-merchants-v2 中的模式

import cats._
import cats.data._
import cats.syntax.all._
import cats.effect.IO

// 1. 基礎領域模型（來自服務）
case class Merchant(id: String, name: String, email: String, isActive: Boolean)
case class Member(id: String, merchantId: String, email: String, permissions: Set[String])
case class Order(id: String, merchantId: String, amount: Double)

// 2. 錯誤類型
sealed trait ServiceError
case class NotFoundError(message: String) extends ServiceError
case class ValidationError(message: String) extends ServiceError  
case class DatabaseError(message: String) extends ServiceError

// 3. Repository 層模擬（類似服務中的 Doobie repositories）
object MockRepositories {
  
  private val merchants = Map(
    "1" -> Merchant("1", "Paidy Corp", "contact@paidy.com", true),
    "2" -> Merchant("2", "Inactive Merchant", "inactive@example.com", false)
  )
  
  private val members = List(   
    Member("m1", "1", "admin@paidy.com", Set("READ", "WRITE")),
    Member("m2", "1", "user@paidy.com", Set("READ"))
  )
  
  private val orders = List(
    Order("o1", "1", 150.0),
    Order("o2", "1", 250.0),
    Order("o3", "2", 100.0)
  )
  
  // 模擬可能失敗的數據庫操作
  def findMerchant(id: String): Either[ServiceError, Option[Merchant]] = {
    if (id == "error") Left(DatabaseError("Database connection failed"))
    else Right(merchants.get(id))
  }
  
  def findMembersByMerchant(merchantId: String): Either[ServiceError, List[Member]] = {
    if (merchantId == "error") Left(DatabaseError("Database connection failed"))
    else Right(members.filter(_.merchantId == merchantId))
  }
  
  def findOrdersByMerchant(merchantId: String): Either[ServiceError, List[Order]] = {
    if (merchantId == "error") Left(DatabaseError("Database connection failed"))
    else Right(orders.filter(_.merchantId == merchantId))
  }
}

// 4. Functor 示範 - 數據轉換
object FunctorExamples {
  
  // 簡單的 map 操作
  def transformMerchantName(maybeMerchant: Option[Merchant]): Option[String] = {
    // Functor[Option].map
    maybeMerchant.map(_.name.toUpperCase)
  }
  
  // Either 的 map 操作（只影響 Right 側）
  def formatMerchantInfo(result: Either[ServiceError, Merchant]): Either[ServiceError, String] = {
    result.map(m => s"Merchant: ${m.name} (${m.email})")
  }
  
  // List 的 map 操作
  def extractMemberEmails(members: List[Member]): List[String] = {
    members.map(_.email)
  }
  
  // 嵌套 Functor 的使用
  def transformOptionalMerchant(
    result: Either[ServiceError, Option[Merchant]]
  ): Either[ServiceError, Option[String]] = {
    // 使用嵌套的 map
    result.map(_.map(_.name))
  }
}

// 5. Applicative 示範 - 組合多個 Context
object ApplicativeExamples {
  
  // 組合多個 Option
  def combineMerchantData(
    maybeMerchant: Option[Merchant],
    maybeOrderCount: Option[Int],
    maybeTotalAmount: Option[Double]
  ): Option[String] = {
    // 使用 mapN 組合三個 Option
    (maybeMerchant, maybeOrderCount, maybeTotalAmount).mapN { (merchant, count, total) =>
      s"${merchant.name}: $count orders, total: $$${total}"
    }
  }
  
  // 組合多個 Either（累積錯誤的版本需要使用 Validated）
  def validateMerchantCreation(
    name: String,
    email: String
  ): Either[ServiceError, Merchant] = {
    val validName = if (name.nonEmpty) Right(name) else Left(ValidationError("Name cannot be empty"))
    val validEmail = if (email.contains("@")) Right(email) else Left(ValidationError("Invalid email"))
    
    // 只會返回第一個錯誤
    (validName, validEmail).mapN { (n, e) =>
      Merchant(java.util.UUID.randomUUID().toString, n, e, true)
    }
  }
  
  // 使用 Validated 累積所有錯誤
  def validateMerchantCreationWithAllErrors(
    name: String,
    email: String
  ): ValidatedNel[String, Merchant] = {
    val validName = if (name.nonEmpty) name.validNel else "Name cannot be empty".invalidNel
    val validEmail = if (email.contains("@")) email.validNel else "Invalid email".invalidNel
    
    // 累積所有錯誤
    (validName, validEmail).mapN { (n, e) =>
      Merchant(java.util.UUID.randomUUID().toString, n, e, true)
    }
  }
}

// 6. Monad 示範 - 鏈式操作和扁平化
object MonadExamples {
  
  // 使用 flatMap 進行鏈式操作
  def getMerchantWithMembers(merchantId: String): Either[ServiceError, (Merchant, List[Member])] = {
    for {
      merchantOpt <- MockRepositories.findMerchant(merchantId)
      merchant <- merchantOpt.toRight(NotFoundError(s"Merchant $merchantId not found"))
      members <- MockRepositories.findMembersByMerchant(merchantId)
    } yield (merchant, members)
  }
  
  // 複雜的 for-comprehension（模擬服務中的複雜業務邏輯）
  def getMerchantSummary(merchantId: String): Either[ServiceError, String] = {
    for {
      // 獲取商戶
      merchantOpt <- MockRepositories.findMerchant(merchantId)
      merchant <- merchantOpt.toRight(NotFoundError(s"Merchant $merchantId not found"))
      
      // 檢查商戶是否活躍
      _ <- Either.cond(merchant.isActive, (), ValidationError("Merchant is not active"))
      
      // 獲取成員和訂單
      members <- MockRepositories.findMembersByMerchant(merchantId)
      orders <- MockRepositories.findOrdersByMerchant(merchantId)
      
      // 計算統計數據
      totalAmount = orders.map(_.amount).sum
      adminCount = members.count(_.permissions.contains("WRITE"))
      
    } yield s"${merchant.name}: ${members.length} members (${adminCount} admins), ${orders.length} orders, total: $$${totalAmount}"
  }
  
  // 使用 traverse 處理列表中的每個元素（來自服務中的常見模式）
  def getMerchantsWithMembers(merchantIds: List[String]): Either[ServiceError, List[(Merchant, List[Member])]] = {
    merchantIds.traverse(getMerchantWithMembers)
  }
  
  // Option 的 flatMap 使用
  def findMerchantEmail(merchantId: String): Option[String] = {
    for {
      merchantResult <- MockRepositories.findMerchant(merchantId).toOption
      merchant <- merchantResult
      if merchant.isActive
    } yield merchant.email
  }
}

// 7. 實際服務層示範（模擬 core-merchants-v2 的服務層）
object MerchantService {
  
  // 模擬 TagLess Final 模式（服務中大量使用）
  trait MerchantServiceAlgebra[F[_]] {
    def getMerchant(id: String): F[Either[ServiceError, Merchant]]
    def getMerchantSummary(id: String): F[Either[ServiceError, String]]
    def validateAndCreateMerchant(name: String, email: String): F[Either[ServiceError, Merchant]]
  }
  
  // IO 實現
  implicit val merchantServiceIO: MerchantServiceAlgebra[IO] = new MerchantServiceAlgebra[IO] {
    
    def getMerchant(id: String): IO[Either[ServiceError, Merchant]] = {
      IO.pure(MockRepositories.findMerchant(id).flatMap(_.toRight(NotFoundError(s"Merchant $id not found"))))
    }
    
    def getMerchantSummary(id: String): IO[Either[ServiceError, String]] = {
      IO.pure(MonadExamples.getMerchantSummary(id))
    }
    
    def validateAndCreateMerchant(name: String, email: String): IO[Either[ServiceError, Merchant]] = {
      IO.pure(ApplicativeExamples.validateMerchantCreation(name, email))
    }
  }
}

// 8. 使用 Cats 語法的實用函數（來自服務）
object CatsSyntaxExamples {
  
  // 使用 cats.syntax.either._
  def safelyGetMerchant(id: String): Either[ServiceError, Merchant] = {
    MockRepositories.findMerchant(id)
      .leftMap(error => error: ServiceError)  // 轉換錯誤類型
      .flatMap(_.toRight(NotFoundError(s"Merchant $id not found")))
  }
  
  // 使用 cats.syntax.option._
  def getMerchantOrDefault(id: String): Merchant = {
    MockRepositories.findMerchant(id)
      .toOption
      .flatten
      .getOrElse(Merchant("default", "Default Merchant", "default@example.com", false))
  }
  
  // 使用 cats.syntax.traverse._
  def getAllMerchantsData(ids: List[String]): Either[ServiceError, List[String]] = {
    ids.traverse { id =>
      MockRepositories.findMerchant(id)
        .flatMap(_.toRight(NotFoundError(s"Merchant $id not found")))
        .map(_.name)
    }
  }
  
  // 條件操作
  def conditionalOperation(merchantId: String): Either[ServiceError, String] = {
    for {
      merchantOpt <- MockRepositories.findMerchant(merchantId)
      merchant <- merchantOpt.toRight(NotFoundError(s"Merchant $merchantId not found"))
      result <- Either.cond(
        merchant.isActive,
        s"Active merchant: ${merchant.name}",
        ValidationError("Merchant is not active")
      )
    } yield result
  }
}

// 9. 主程序示範
object CatsCoreDemo extends App {
  
  println("=== Cats 核心抽象示範 ===\n")
  
  // Functor 示範
  println("1. Functor 示範:")
  val merchant = Some(Merchant("1", "paidy", "contact@paidy.com", true))
  val merchantName = FunctorExamples.transformMerchantName(merchant)
  println(s"   轉換商戶名稱: $merchantName")
  
  val merchantResult = Right(Merchant("1", "paidy", "contact@paidy.com", true)): Either[ServiceError, Merchant]
  val merchantInfo = FunctorExamples.formatMerchantInfo(merchantResult)
  println(s"   格式化商戶信息: $merchantInfo\n")
  
  // Applicative 示範
  println("2. Applicative 示範:")
  val combined = ApplicativeExamples.combineMerchantData(
    Some(Merchant("1", "Paidy", "contact@paidy.com", true)),
    Some(5),
    Some(1250.0)
  )
  println(s"   組合數據: $combined")
  
  val validation = ApplicativeExamples.validateMerchantCreationWithAllErrors("", "invalid-email")
  println(s"   累積驗證錯誤: $validation\n")
  
  // Monad 示範
  println("3. Monad 示範:")
  val merchantWithMembers = MonadExamples.getMerchantWithMembers("1")
  println(s"   商戶和成員: $merchantWithMembers")
  
  val merchantSummary = MonadExamples.getMerchantSummary("1")
  println(s"   商戶摘要: $merchantSummary")
  
  val inactiveMerchantSummary = MonadExamples.getMerchantSummary("2")
  println(s"   非活躍商戶摘要: $inactiveMerchantSummary")
  
  val notFoundSummary = MonadExamples.getMerchantSummary("999")
  println(s"   不存在商戶摘要: $notFoundSummary\n")
  
  // Traverse 示範
  println("4. Traverse 示範:")
  val multipleMerchants = MonadExamples.getMerchantsWithMembers(List("1", "2"))
  println(s"   多個商戶數據: ${multipleMerchants.map(_.length)}")
  
  val allMerchantNames = CatsSyntaxExamples.getAllMerchantsData(List("1", "2"))
  println(s"   所有商戶名稱: $allMerchantNames")
  
  println("\n=== 練習完成 ===")
} 