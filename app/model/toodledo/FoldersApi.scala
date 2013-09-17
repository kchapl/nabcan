package model.toodledo

import net.liftweb.json.JsonAST.{JString, JField, JObject}
import net.liftweb.json.JsonParser.parse


object FoldersApi {

  def fetch(key: => String, httpClient: HttpClient = Registry.httpClient): List[Folder] = {
    val responseBody = httpClient.makeGetRequest(List("folders", "get.php"), Map("key" -> key))

    for {
      JObject(o) <- parse(responseBody)
      JField("id", JString(id)) <- o
      JField("name", JString(name)) <- o
      JField("private", JString(prv)) <- o
      JField("archived", JString(archived)) <- o
      JField("ord", JString(ord)) <- o
    } yield Folder(id.toLong, name, prv.toInt == 1, archived.toInt == 1, ord.toInt)
  }
}

case class Folder(id: Long, name: String, prv: Boolean, archived: Boolean, ord: Int)
