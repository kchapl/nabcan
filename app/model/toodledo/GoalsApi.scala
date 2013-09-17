package model.toodledo

import net.liftweb.json.JsonAST.{JInt, JString, JField, JObject}
import net.liftweb.json.JsonParser.parse


object GoalsApi {

  def fetch(key: => String, httpClient: HttpClient = Registry.httpClient): List[Goal] = {
    val responseBody = httpClient.makeGetRequest(List("goals", "get.php"), Map("key" -> key))

    for {
      JObject(o) <- parse(responseBody)
      JField("id", JString(id)) <- o
      JField("name", JString(name)) <- o
      JField("level", JInt(level)) <- o
      JField("archived", JInt(archived)) <- o
      JField("contributes", JInt(contributes)) <- o
      JField("note", JString(note)) <- o
    } yield Goal(id.toLong, name, level.toInt, archived.toInt == 1, contributes.toInt, note)
  }
}

case class Goal(id: Long, name: String, level: Int, archived: Boolean, contributes: Int, note: String)
