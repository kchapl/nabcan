package model.toodledo

import net.liftweb.json.JsonAST.{JObject, JField, JString}
import net.liftweb.json.JsonParser.parse


class ContextsApi(key: => String, httpClient: HttpClient = Registry.httpClient) {

  def fetch: List[Context] = {
    val responseBody = httpClient.makeGetRequest(List("contexts", "get.php"), Map("key" -> key))

    for {
      JObject(o) <- parse(responseBody)
      JField("id", JString(id)) <- o
      JField("name", JString(name)) <- o
    } yield Context(id.toLong, name)
  }
}

case class Context(id: Long, name: String)
