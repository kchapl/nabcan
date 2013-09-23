package model.service.toodledo

import play.api.libs.ws.WS
import play.api.libs.json._
import model.{Task, Context}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


object Toodledo {

  def getContexts(key: => String): Future[Either[String, Seq[Context]]] = {
    val x = WS.url("http://api.toodledo.com/2/contexts/get.php?key=%s" format key).get()
    x map {
      response => {
        val z = response.json
        println(Json.prettyPrint(z))
        val a = (z \ "errorDesc").as[String]
        Left(a)
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
