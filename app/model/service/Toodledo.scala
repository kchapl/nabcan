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
import play.api.libs.json.JsArray
import play.api.libs.ws.Response
import scala.Some
import model.Context
import model.Task

object Toodledo {

  case class Exception(id: Int, description: String)

  case class App(id: String, token: String)

  // TODO: store these better
  private val props = new Properties
  props.load(new FileInputStream(sys.props.get("user.home").get + "/Desktop/tdconf"))
  private val appId = props.getProperty("appId")
  private val appToken = props.getProperty("appToken")
  private val userEmail = props.getProperty("userEmail")
  private val userPassword = props.getProperty("userPassword")

  private val app = App(appId, appToken)

  private val apiUrl = "api.toodledo.com/2"

  private lazy val userId = {
    // TODO: need to wait?
    val result = Await.result(lookUpUserId(userEmail, userPassword), atMost = 30.seconds)
    result.right.get
  }

  private lazy val key = {
    val sessionToken = Await.result(genToken, atMost = 30.seconds).right.get
    md5(md5(userPassword) + app.token + sessionToken)
  }

  private implicit val exceptionReads = (
    (__ \ 'errorCode).read[Int] and (__ \ 'errorDesc).read[String]
    )(Exception)

  private implicit val contextReads = (
    (__ \ 'id).read[String].map(_.toInt) and (__ \ 'name).read[String]
    )(Context)

  private implicit val taskReads = (
    (__ \ 'id).read[String].map(_.toInt) and (__ \ 'title).read[String] and (__ \ 'context).read[String].map(_.toInt)
    )(Task)

  // TODO: refactor these
  private def doGet(url: String) = {
    // TODO :log
    println(s"GET $url")
    WS.url(url) get()
  }

  private def lookUp(path: String, queryString: String) = doGet(s"https://$apiUrl/$path.php?$queryString")

  private def get(path: String, queryString: String, secure: Boolean = true) = {
    val protocol = "https"
    val url = s"$protocol://$apiUrl/$path.php?$queryString"
    doGet(url)
  }

  private def getWithKey(path: String, queryString: Option[String] = None, secure: Boolean = false) = {
    val aKey = key
    //val url = queryString.foldLeft(s"http://$apiUrl/$path.php?key=$key") {
    val url = queryString.foldLeft(s"http://$apiUrl/$path.php?key=$aKey") {
      (prefix, suffix) => s"$prefix&$suffix"
    }
    doGet(url)
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

  def lookUpUserId(email: String, password: String): Future[Either[Exception, String]] = {
    val sig = md5(email + app.token)
    val queryString = s"appid=${app.id}&sig=$sig&email=$email&pass=$password"
    lookUp("account/lookup", queryString) map parse {
      json => (json \ "userid").validate[String]
    }
  }

  def genToken: Future[Either[Exception, String]] = {
    val sig = md5(userId + app.token)
    val queryString = s"userid=$userId&appid=${app.id}&sig=$sig"
    get("account/token", queryString) map parse {
      json => (json \ "token").validate[String]
    }
  }

  def getContexts: Future[Either[Exception, Seq[Context]]] = {
    getWithKey("contexts/get") map parse(_.validate[Seq[Context]])
  }

  def getTasks(key: => String = key): Future[Either[Exception, Seq[Task]]] = {
    getWithKey("tasks/get", Some("fields=context")) map parse {
      json => JsArray(json.asInstanceOf[JsArray].value.tail).validate[Seq[Task]]
    }
  }
}
