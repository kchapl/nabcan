package model.service.toodledo

import play.api.libs.ws.WS
import play.api.libs.json._
import model.{Task, Context}


object Toodledo {

  def getContexts(key: => String): Seq[Context] = {
    val x = WS.url("http://api.toodledo.com/2/contexts/get.php?key=%s" format key).get()
    x map {
      response => {
        val z = response.json
        println(Json.prettyPrint(z))
      }
    }
    Nil
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
