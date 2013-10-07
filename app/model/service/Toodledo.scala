package model.service

import play.api.libs.json._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.Properties
import java.io.FileInputStream
import play.api.libs.functional.syntax._
import play.api.libs.ws.WS
import model.toodledo.Digest._
import concurrent.duration._
import net.liftweb.json.JsonParser._
import play.api.libs.json.JsArray
import play.api.libs.ws.Response
import net.liftweb.json.JsonAST.JObject
import scala.Some
import net.liftweb.json.JsonAST.JField
import model.Context
import net.liftweb.json.JsonAST.JString
import model.Task
import scalax.file.Path
import org.joda.time.DateTime
import model.toodledo.{HttpClient, Registry, TokenAndExpiryTime}
import model.service.Toodledo.User


object Toodledo {

  case class Exception(id: Int, description: String)

  case class App(id: String, token: String)

  case class User(id: String, password: String)

  // TODO: store these better
  private val props = new Properties
  props.load(new FileInputStream(sys.props.get("user.home").get + "/Desktop/tdconf"))
  private val appId = props.getProperty("appId")
  private val appToken = props.getProperty("appToken")
  private val userEmail = props.getProperty("userEmail")
  private val userPassword = props.getProperty("userPassword")

  private val app = App(appId, appToken)

  private val apiUrl = "api.toodledo.com/2"

  private lazy val user: User = {
    val result = Await.result(lookupUser(userEmail, userPassword), atMost = 2.seconds)
    result.right.get
  }

  private lazy val key = {
    val tokenResult = Await.result(genToken, atMost = 2.seconds)
    val token = tokenResult.right.get
    md5(md5(user.password) + app.token + token)
  }

  private implicit val exceptionReads = (
    (__ \ 'errorCode).read[String].map(_.toInt) and (__ \ 'errorDesc).read[String]
    )(Exception)

  private implicit val userReads = (
    (__ \ 'id).read[String] and (__ \ 'password).read[String]
    )(User)

  private implicit val contextReads = (
    (__ \ 'id).read[String].map(_.toInt) and (__ \ 'name).read[String]
    )(Context)

  private implicit val taskReads = (
    (__ \ 'id).read[String].map(_.toInt) and (__ \ 'title).read[String] and (__ \ 'context).read[String].map(_.toInt)
    )(Task)

  private def lookUp(path: String, queryString: String) = {
    WS.url(s"http://$apiUrl/$path.php?$queryString") get()
  }

  private def get(path: String, queryString: Option[String] = None, secure: Boolean = false) = {
    val aKey = key
    //val url = queryString.foldLeft(s"http://$apiUrl/$path.php?key=$key") {
    val url = queryString.foldLeft(s"http://$apiUrl/$path.php?key=$aKey") {
      (prefix, suffix) => s"$prefix&$suffix"
    }
    WS.url(url) get()
  }

  private def parse[T](parseContent: JsValue => JsResult[T])(response: Response) = {
    val json = response.json
    //TODO: log
    println(Json.prettyPrint(json))
    json.validate[Exception] fold(
      invalid => parseContent(json) fold(
        invalid => throw new RuntimeException(invalid.toString()),
        valid => Right(valid)),
      valid => Left(valid)
      )
  }

  def lookupUser(email: String, password: String): Future[Either[Exception, User]] = {
    val sig = md5(email + app.token)
    val queryString = s"appid=${app.id}&sig=$sig&email=$email&pass=$password"
    lookUp("account", queryString) map parse(_.validate[User])
  }

  def genToken: Future[Either[Exception, String]] = {
    val sig = md5(user.id + app.token)
    val queryString = Some(s"userid=${user.id}&appid=${app.id}&sig=$sig")
    get("account/token", queryString, secure = true) map parse {
      json => json.validate[String]
    }
  }

  def getContexts: Future[Either[Exception, Seq[Context]]] = {
    get("contexts/get") map parse(_.validate[Seq[Context]])
  }

  def getTasks(key: => String = key): Future[Either[Exception, Seq[Task]]] = {
    get("tasks/get", Some("fields=context")) map parse {
      json => JsArray(json.asInstanceOf[JsArray].value.tail).validate[Seq[Task]]
    }
  }

  def getTasksByContext(key: => String = key, contextId: Int): Future[Either[Exception, Seq[Task]]] = {
    getTasks() map {
      _ fold(
        e => Left(e),
        tasks => Right(tasks filter (_.contextId == contextId)))
    }
  }
}


trait TokenCache {

  val app: Toodledo.App
  val user: Toodledo.User
  val httpClient: HttpClient

  def currentTokenAndExpiryTime: Option[TokenAndExpiryTime]

  def generateToken: String = {
    val sig = md5(user.id + app.token)
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


case class FileSysTokenCache(app: Toodledo.App, user: User, httpClient: HttpClient = Registry.httpClient) extends TokenCache {
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
