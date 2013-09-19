package model.service.toodledo

import play.api.libs.ws.WS


object Toodledo {

  def get(key: => String): Seq[model.Task] = {
    val x = WS.url("").get()
    val y = x map { response =>
    response.json
    }
    Nil
  }

}
