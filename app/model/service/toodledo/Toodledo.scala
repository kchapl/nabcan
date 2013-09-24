package model.service.toodledo

import play.api.libs.ws.WS
import play.api.libs.json._
import model.{Task, Context}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import model.toodledo.{FileSysTokenCache, Authentication, App}
import java.util.Properties
import java.io.FileInputStream


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

  private def key: String = {
    Authentication.key(app, user, tokenCache)
  }

  def getContexts(key: => String = key): Future[Either[String, Seq[Context]]] = {
    val x = WS.url("http://api.toodledo.com/2/contexts/get.php?key=%s" format key).get()
    x map {
      response => {
        val z = response.json
        println(Json.prettyPrint(z))
        implicit val customReads = Json.reads[Seq[Context]]
        z.validate[Seq[Context]].fold(
          invalid = {
            e => println(e); Left(e.flatMap(_._2).head.message)
          },
          valid = {
            c => println(c); Right(c)
          }
        )
      }
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
