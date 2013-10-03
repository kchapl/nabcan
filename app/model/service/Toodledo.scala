package model.service


import play.api.libs.json._
import model.{Task, Context}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import model.toodledo.{FileSysTokenCache, Authentication, App}
import java.util.Properties
import java.io.FileInputStream
import play.api.libs.functional.syntax._
import play.api.libs.ws.{Response, WS}


object Toodledo {

  private val props = new Properties
  props.load(new FileInputStream(sys.props.get("user.home").get + "/Desktop/tdconf"))
  private val appId = props.getProperty("appId")
  private val appToken = props.getProperty("appToken")
  private val userEmail = props.getProperty("userEmail")
  private val userPassword = props.getProperty("userPassword")

  private val app = App(appId, appToken)
  private val user = Authentication.lookUpUser(app, userEmail, userPassword)
  private val tokenCache = new FileSysTokenCache(app, user)

  private val apiUrl = "http://api.toodledo.com/2"

  // TODO: sort this out
  private def key: String = {
    Authentication.key(app, user, tokenCache)
  }

  case class Exception(id: Int, description: String)

  private implicit val exceptionReads = (
    (__ \ 'errorCode).read[String].map(_.toInt) and (__ \ 'errorDesc).read[String]
    )(Exception)

  private implicit val contextReads = (
    (__ \ 'id).read[String].map(_.toInt) and (__ \ 'name).read[String]
    )(Context)

  private implicit val taskReads = (
    (__ \ 'id).read[String].map(_.toInt) and (__ \ 'title).read[String] and (__ \ 'context).read[String].map(_.toInt)
    )(Task)

  // TODO refactor with key
  private def get(path: String, queryString: Option[String] = None, key: => String = key) = {
    WS.url(s"$apiUrl/$path/get.php?$queryString&key=$key") get()
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

  def getContexts: Future[Either[Exception, Seq[Context]]] = {
    get("contexts") map parse(_.validate[Seq[Context]])
  }


  def getTasks(key: => String = key): Future[Either[Exception, Seq[Task]]] = {
    get("tasks", Some("fields=context")) map parse {
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
