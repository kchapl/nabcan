package model

import scala.concurrent.{Await, Future}
import java.util.Properties
import java.io.FileInputStream
import model.toodledo.Digest._
import scala.concurrent.duration._
import ToodledoService.App
import scala.concurrent.ExecutionContext.Implicits.global

// TODO: move to model package
object TaskRepository {

  // TODO: store these better
  private val props = new Properties
  props.load(new FileInputStream(sys.props.get("user.home").get + "/Desktop/tdconf"))
  private val appId = props.getProperty("appId")
  private val appToken = props.getProperty("appToken")
  private val userEmail = props.getProperty("userEmail")
  private val userPassword = props.getProperty("userPassword")

  private val app = App(appId, appToken)

  private lazy val userId = {
    Await.result(ToodledoService.getUserId(userEmail, userPassword, app), atMost = 2.seconds)
  }

  private lazy val key: String = {
    Await.result(for {
      sessionToken <- ToodledoService.getToken(userId, app)
      key = md5(md5(userPassword) + app.token + sessionToken)
    } yield key, atMost = 2.seconds)
  }

  def getContexts: Future[Seq[Context]] = ToodledoService.getContexts(key)

  def getBoard(contextId: Int): Future[Map[String, Seq[Task]]] = {

    def group(tasks: Seq[Task]): Map[String, Seq[Task]] = {
      tasks groupBy {
        case task if task.completed != 0 => "Done"
        case task if task.status == 0 => "No Status"
        case task if task.status == 1 => "Next Action"
        case task if task.status == 2 => "Active"
        case task if task.status == 3 => "Planning"
        case task if task.status == 4 => "Delegated"
        case task if task.status == 5 => "Waiting"
        case task if task.status == 6 => "Hold"
        case task if task.status == 7 => "Postponed"
        case task if task.status == 8 => "Someday"
        case task if task.status == 9 => "Cancelled"
        case task if task.status == 10 => "Reference"
      }
    }

    for (tasks <- ToodledoService.getTasks(key)) yield
      group(tasks filter (_.contextId == contextId))
  }

}
