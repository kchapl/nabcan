package model.toodledo

import scala.concurrent.Await
import dispatch._
import concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


trait HttpClient {

  val apiHost = "api.toodledo.com"
  val apiVersion = "2"

  def makeGetRequest(path: List[String],
                     query: Map[String, String] = Map(),
                     secure: Boolean = false): String
}


class DispatchHttpClient extends HttpClient {

  def makeGetRequest(path: List[String],
                     query: Map[String, String] = Map(),
                     secure: Boolean = false): String = {

    val request = (apiVersion :: path).foldLeft((host(apiHost), secure) match {
      case (host, true) => host.secure
      case (host, false) => host
    })((acc, item) => acc / item) <<? query

    Await.result(for (responseBody <- Http(request OK as.String)) yield responseBody, 30 seconds)
  }

}
