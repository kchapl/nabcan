package model.toodledo

import org.joda.time.DateTime
import net.liftweb.json.JsonAST.{JInt, JObject, JField, JString}
import net.liftweb.json.JsonParser.parse


object TasksApi {

  def fetch(key: => String, httpClient: HttpClient = Registry.httpClient)
           (modifiedBefore: Option[DateTime] = None,
            modifiedAfter: Option[DateTime] = None,
            completed: Option[Boolean] = None)
           (fetchFields: List[String] = Nil): List[Task] = {

    def addParameter(params: Map[String, String], name: String, value: Option[Any]) = {
      value.foldLeft(params)((ps, p) => ps + (name -> p.toString))
    }

    val params = addParameter(addParameter(Map("key" -> key), "modbefore", modifiedBefore), "modafter", modifiedAfter)
    val responseBody = httpClient.makeGetRequest(List("tasks", "get.php"), params)

    val x = for {
      JObject(o) <- parse(responseBody)
      JField("id", JString(id)) <- o
      JField("title", JString(title)) <- o
      JField("modified", JInt(modified)) <- o
//      JField("completed", JInt(completed)) <- o
//      JField("context", JString(context)) <- o
//    } yield Task(id.toLong, title,
//        new DateTime(modified.toLong * 1000),
//        new DateTime(completed.toLong * 1000),
//        context.toLong)
  } yield (id.toLong, title, modified)

    val y =  parse(responseBody)
//    y map (z => JObject(z))
    x
    Nil
  }

  def fetchDeleted(key: => String, httpClient: HttpClient = Registry.httpClient): List[Long] = {
    val baseParams = Map("key" -> key)
    val responseBody = httpClient.makeGetRequest(List("tasks", "deleted.php"), baseParams)

    for {
      JObject(o) <- parse(responseBody)
      JField("id", JString(id)) <- o
    } yield id.toLong
  }

}


case class Task(id: Long, title: String, modified: DateTime, completed: DateTime, contextId: Long)
