package model.toodledo

import scalax.file.Path
import org.joda.time.DateTime
import scala.Some
import net.liftweb.json.JsonAST.{JString, JField, JObject}
import net.liftweb.json.JsonParser.parse


class TokenAndExpiryTime(val token: String, expires: DateTime) {
  def isActive = expires.isAfterNow
}

trait TokenCache {

  val app: App
  val user: User
  val httpClient: HttpClient

  def currentTokenAndExpiryTime: Option[TokenAndExpiryTime]

  def generateToken: String = {
    val sig = Digest.md5(user.id + app.token)
    val responseBody = httpClient.makeGetRequest(List("account", "token.php"),
      Map("userid" -> user.id, "appid" -> app.id, "sig" -> sig),
      secure = true)

    val JObject(List(JField("token", JString(token)))) = parse(responseBody)

    cache(token)
    token
  }

  def cache(token: String)

  def flush()

  def getToken: String = {
    currentTokenAndExpiryTime match {
      case Some(current) if current.isActive => current.token
      case _ => generateToken
    }
  }
}

case class FileSysTokenCache(app: App, user: User, httpClient: HttpClient = Registry.httpClient) extends TokenCache {
  val tokenStore = Path.fromString(sys.props.get("java.io.tmpdir").get) / "td.token.txt"

  def currentTokenAndExpiryTime: Option[TokenAndExpiryTime] = {
    if (!tokenStore.exists) None
    else {
      val serialized = tokenStore.string
      if (serialized.isEmpty) None
      else {
        val parts = serialized.split(":")
        Some(new TokenAndExpiryTime(parts(0), new DateTime(parts(1).toLong)))
      }
    }
  }

  def cache(token: String) {
    tokenStore.write("%s:%s".format(token, new DateTime().plusHours(3).getMillis))
  }

  def flush() {
    tokenStore.deleteIfExists()
  }
}
