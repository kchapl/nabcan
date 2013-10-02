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

  private val apiPath = "http://api.toodledo.com/2"

  private def parse(response: Response): Either[Toodledo.Exception, Seq[Context]] = {
    val json = response.json
    //TODO: log
    println(Json.prettyPrint(json))
    json.validate[Seq[Context]] fold(
      invalid => json.validate[Exception] fold(
        invalid => throw new RuntimeException(invalid.toString()),
        valid => Left(valid)
        ),
      valid => Right(valid)
      )
  }

  private def parse2(response: Response): Either[Toodledo.Exception, Seq[Task]] = {
    val json = response.json
    //TODO: log
    println(Json.prettyPrint(json))
    json.validate[Exception] fold(
      invalid => JsArray(json.asInstanceOf[JsArray].value.tail).validate[Seq[Task]] fold(
        invalid => throw new RuntimeException(invalid.toString()),
        valid => Right(valid)
        ),
      valid => Left(valid)
      )
  }

  private def parse3(response: Response, contextId: Int): Either[Toodledo.Exception, Seq[Task]] = {
    val json = response.json
    //TODO: log
    println(Json.prettyPrint(json))
    json.validate[Exception] fold(
      invalid => JsArray(json.asInstanceOf[JsArray].value.tail).validate[Seq[Task]] fold(
        invalid => throw new RuntimeException(invalid.toString()),
        valid => Right(valid filter (_.contextId == contextId))
        ),
      valid => Left(valid)
      )
  }

  // TODO refactor with key
  def getContexts(key: => String = key): Future[Either[Exception, Seq[Context]]] = {
    WS.url(s"$apiPath/contexts/get.php?key=$key") get() map parse
  }

  def getTasks(key: => String = key): Future[Either[Exception, Seq[Task]]] = {
    WS.url(s"$apiPath/tasks/get.php?fields=context&key=$key") get() map parse2
  }

  def getTasksByContext(key: => String = key, contextId: Int): Future[Either[Exception, Seq[Task]]] = {
    val x = getTasks()
    for {
      y <- x
      z = y.fold(
       Left(l) => Left(l),
       Right(r) => Right(r)
      )
      y.
    }
    WS.url(s"$apiPath/tasks/get.php?fields=context&key=$key") get() map {
      response => {
        parse3(response, contextId)
      }
    }
  }
}
