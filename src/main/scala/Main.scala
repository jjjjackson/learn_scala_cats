import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.ember.server._
import org.http4s.server.Router
import org.http4s.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import com.comcast.ip4s._
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import cats.effect.kernel.Sync

object Main extends IOApp.Simple {
  implicit val loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]

  case class User(name: String, age: Int)
  case class UserResponse(status: String, received: User)

  implicit val userDecoder = jsonOf[IO, User]

  val userRoutes = HttpRoutes.of[IO] {
    case GET -> Root / "hello" =>
      Ok("Hello, World from Cats + Http4s!")

    case req @ POST -> Root / "users" =>
      for { // Syntax sugar for flatMap
        user <- req.as[User]
        res <- Ok(UserResponse("ok", user).asJson)
      } yield res
  }

  val httpApp = Router(
    "/" -> userRoutes
  ).orNotFound

  val server = EmberServerBuilder
    .default[IO]
    .withHost(ipv4"0.0.0.0")
    .withPort(port"8080")
    .withHttpApp(httpApp)
    .build
    .useForever

  def run: IO[Unit] = server
}
