package model.service

import scala.concurrent.Future
import model.{Context, Task}
import scala.concurrent.ExecutionContext.Implicits.global

object TaskRepository {

  def getContexts: Future[Either[Toodledo.Exception, Seq[Context]]] = Toodledo.getContexts

  def getBoard(contextId: Int): Future[Either[Toodledo.Exception, Map[String, Seq[Task]]]] = {

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

    for (tasks <- Toodledo.getTasks) yield
      tasks fold(
        e => Left(e),
        tasks => Right {
          group(tasks filter (_.contextId == contextId))
        }
        )
  }
}
