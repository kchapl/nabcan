package model.service

import play.api.libs.json._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.Properties
import java.io.FileInputStream
import play.api.libs.functional.syntax._
import play.api.libs.ws.WS
import model.toodledo.Digest._
import model.toodledo.{User, App, FileSysTokenCache}
import play.api.libs.json.JsArray
import play.api.libs.ws.Response
import scala.Some
import model.Context
import model.Task
import concurrent.duration._


object Toodledo {

  // TODO: store these better
  private val props = new Properties
  props.load(new FileInputStream(sys.props.get("user.home").get + "/Desktop/tdconf"))
  private val appId = props.getProperty("appId")
  private val appToken = props.getProperty("appToken")
  private val userEmail = props.getProperty("userEmail")
  private val userPassword = props.getProperty("userPassword")

  private val app = App(appId, appToken)

  private lazy val user: User = {
    val result = Await.result(lookupUser(userEmail, userPassword), atMost = 2.seconds)
    result.right.get
  }

  // TODO: sort this out
  private lazy val tokenCache = new FileSysTokenCache(app, user)

  private val apiUrl = "api.toodledo.com/2"

  private def key = md5(md5(user.password) + app.token + tokenCache.getToken)

  case class Exception(id: Int, description: String)

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

  private def get(path: String, queryString: Option[String] = None, secure: Boolean = false) = {
    val url = queryString.foldLeft(s"http://$apiUrl/$path.php?key=$key") {
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
    val queryString = Some(s"appid=${app.id}&sig=$sig&email=$email&pass=$password")
    get("account/lookup", queryString, secure = true) map parse(_.validate[User])
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
