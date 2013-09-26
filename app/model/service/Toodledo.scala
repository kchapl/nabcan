package model.service

import play.api.libs.ws.{Response, WS}
import play.api.libs.json._
import model.{Task, Context}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import model.toodledo.{FileSysTokenCache, Authentication, App}
import java.util.Properties
import java.io.FileInputStream
import play.api.libs.functional.syntax._


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

  private implicit val exceptionReads = ((__ \ 'errorCode).read[String].map(_.toInt) and (__ \ 'errorDesc).read[String])(Exception)
  private implicit val contextReads = ((__ \ 'id).read[String].map(_.toInt) and (__ \ 'name).read[String])(Context)

  // TODO refactor parse
  // TODO refactor with key
  def getContexts(key: => String = key): Future[Either[Exception, Seq[Context]]] = {
    WS.url("http://api.toodledo.com/2/contexts/get.php?key=%s" format key) get() map {
      response =>
        val json = response.json
        println(Json.prettyPrint(json))
        json.validate[Seq[Context]] fold {
          invalid = e =>
          //            val jsResult: JsResult[Exception] = json.validate[Exception]
          //            val x = jsResult.fold{
          //               invalid = {case (k,v) => Exception(1,"err")},
          //              valid = {e => Left(e)}
          //            }
          //            x
            Left(Exception(1, "")),
          valid = Right(_)
        }
    }
  }

  def getContexts2(key: => String = key): Future[Either[Exception, Seq[Context]]] = {
    WS.url("http://api.toodledo.com/2/contexts/get.php?key=%s" format key) get() map {
      response => parseAs[Seq[Context]](response.json)
    }
  }

  def parseAs[T](json: JsValue): Either[Exception, T] = {
    println(Json.prettyPrint(json))
    json.validate[Seq[T]] fold{
      invalid = e => {
        val x: JsResult[Exception] = json.validate[Exception]
        x.fold {
          invalid = {e => Left(Exception(1,"err"))},
          valid = Left(_)
        }
      },
      valid = Right(_)
    }
  }

  def getTasksByContext(key: => String, context: String): Seq[Task] = {
    val x = WS.url("").get()
    x map {
      response => {
        val z = response.json
        println(Json.prettyPrint(z))
      }
    }
    Nil
  }

}
