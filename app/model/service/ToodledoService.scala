package model.service

import play.api.libs.json._
import scala.concurrent.Future
import play.api.libs.functional.syntax._
import play.api.libs.ws.WS
import model.toodledo.Digest._
import play.api.libs.json.JsArray
import scala.Some
import model.Context
import model.Task

object ToodledoService {

  case class Exception(id: Int, description: String) extends scala.Exception

  case class App(id: String, token: String)

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
                    (parse: JsValue => JsResult[T]): Future[T] = {
    val protocol = if (secure) "https" else "http"
    val url = s"$protocol://api.toodledo.com/2/$path.php?$queryString"
    // TODO :log
    println(s"GET $url")
    for (response <- WS.url(url) get()) yield {
      val json = response.json
      //TODO: log
      println(Json.prettyPrint(json))
      json.validate[Exception] fold(
        invalid => parse(json) fold(
          invalid => throw new RuntimeException(invalid.toString()),
          valid => valid),
        valid => throw valid
        )
    }
  }

  private def getWithKey[T](key: String)
                           (path: String, addQueryString: Option[String] = None, secure: Boolean = false)
                           (parse: JsValue => JsResult[T]): Future[T] = {
    val queryString = addQueryString.foldLeft(s"key=$key")((keyParam, extraParams) => s"$keyParam&$extraParams")
    get(path, queryString, secure)(parse)
  }

  def getUserId(email: String, password: String, app: App): Future[String] = {
    val sig = md5(email + app.token)
    val queryString = s"appid=${app.id}&sig=$sig&email=$email&pass=$password"
    get("account/lookup", queryString) {
      json => (json \ "userid").validate[String]
    }
  }

  def getToken(userId: String, app: App): Future[String] = {
    val sig = md5(userId + app.token)
    val queryString = s"userid=$userId&appid=${app.id}&sig=$sig"
    get("account/token", queryString) {
      json => (json \ "token").validate[String]
    }
  }

  def getContexts(key: String): Future[Seq[Context]] = {
    getWithKey(key)("contexts/get") {
      _.validate[Seq[Context]]
    }
  }

  def getTasks(key: String): Future[Seq[Task]] = {
    getWithKey(key)("tasks/get", Some("fields=context,status")) {
      json => JsArray(json.asInstanceOf[JsArray].value.tail).validate[Seq[Task]]
    }
  }

}
