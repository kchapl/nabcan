package model.service

import play.api.libs.json._
import scala.concurrent.{Await, Future}
import java.util.Properties
import java.io.FileInputStream
import play.api.libs.functional.syntax._
import play.api.libs.ws.WS
import model.toodledo.Digest._
import concurrent.duration._
import play.api.libs.json.JsArray
import scala.Some
import model.Context
import model.Task
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure, Try}

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
    val result = Await.result(getUserId, atMost = 30.seconds)
    result.right.get
  }

  private def cachedKey: Option[String] = ???

  private def key: Future[Either[Exception, String]] = {
    cachedKey match {
      case Some(key) => Future(Right(key))
      case None =>
        getToken map (_ fold(
          e => Left(e),
          sessionToken => Right(md5(md5(userPassword) + app.token + sessionToken))
          ))
    }
  }

  private implicit val exceptionReads = (
    (__ \ 'errorCode).read[Int] and (__ \ 'errorDesc).read[String]
    )(Exception)

  private implicit val contextReads = (
    (__ \ 'id).read[String].map(_.toInt) and (__ \ 'name).read[String]
    )(Context)

  private implicit val taskReads = (
    (__ \ 'id).read[String].map(_.toInt) and
      (__ \ 'title).read[String] and
      (__ \ 'status).read[String].map(_.toInt) and
      (__ \ 'completed).read[Long] and
      (__ \ 'context).read[String].map(_.toInt)
    )(Task)

  private def get[T](path: String, queryString: String, secure: Boolean = true)
                    (parse: JsValue => JsResult[T]): Future[Either[Exception, T]] = {
    val protocol = if (secure) "https" else "http"
    val url = s"$protocol://$apiUrl/$path.php?$queryString"
    // TODO :log
    println(s"GET $url")
    for (response <- WS.url(url) get()) yield {
      val json = response.json
      //TODO: log
      println(Json.prettyPrint(json))
      json.validate[Exception] fold(
        invalid => parse(json) fold(
          invalid => throw new RuntimeException(invalid.toString()),
          valid => Right(valid)),
        valid => Left(valid)
        )
    }
  }

  private def getWithKey[T](path: String, addQueryString: Option[String] = None, secure: Boolean = false)
                           (parse: JsValue => JsResult[T]): Future[Either[Exception, T]] = {
    // TODO: chain together results of either on gettoken and this
    val aKey = key
    aKey match {
      case Success(k) =>
        val queryString = addQueryString.foldLeft(s"key=$aKey")((keyParam, extraParams) => s"$keyParam&$extraParams")
        get(path, queryString, secure)(parse)
      case Failure(e) =>
        val queryString = addQueryString.foldLeft(s"key=$aKey")((keyParam, extraParams) => s"$keyParam&$extraParams")
        get(path, queryString, secure)(parse)
    }
  }

  def getUserId: Future[Either[Exception, String]] = {
    val sig = md5(userEmail + app.token)
    val queryString = s"appid=${app.id}&sig=$sig&email=$userEmail&pass=$userPassword"
    get("account/lookup", queryString) {
      json => (json \ "userid").validate[String]
    }
  }

  def getToken: Future[Either[Exception, String]] = {
    val sig = md5(userId + app.token)
    val queryString = s"userid=$userId&appid=${app.id}&sig=$sig"
    get("account/token", queryString) {
      json => (json \ "token").validate[String]
    }
  }

  def getContexts: Future[Either[Exception, Seq[Context]]] = {
    getWithKey("contexts/get") {
      _.validate[Seq[Context]]
    }
  }

  def getTasks: Future[Either[Exception, Seq[Task]]] = {
    getWithKey("tasks/get", Some("fields=context,status")) {
      json => JsArray(json.asInstanceOf[JsArray].value.tail).validate[Seq[Task]]
    }
  }
}
