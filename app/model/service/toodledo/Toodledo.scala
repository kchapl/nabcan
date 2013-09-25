package model.service.toodledo

import play.api.libs.ws.WS
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

  private implicit val contextReads = ((__ \ 'id).read[String].map(_.toInt) and (__ \ 'name).read[String])(Context)

  def getContexts(key: => String = key): Future[Either[ToodledoError, Seq[Context]]] = {
    WS.url("http://api.toodledo.com/2/contexts/get.php?key=%s" format key) get() map {
      response =>
        val json = response.json
        println(Json.prettyPrint(json))
        json.validate[Seq[Context]] fold(
          // TODO: validate toodledo error and pass that through
          invalid = e => {
            Left(ToodledoError(1, "error"))
          },
          valid = Right(_)
          )
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


case class ToodledoError(id: Int, description: String)
