package model.service

import scala.concurrent.Future
import model.{Context, Task}

object TaskRepository {

  def getContexts: Future[Either[Toodledo.Exception, Seq[Context]]] = Toodledo.getContexts

  def getBoard(contextId: Int): Future[Either[Toodledo.Exception, Seq[(String, Seq[Task])]]] = {

    def groupTasks(tasks: Seq[Task]): Map[String, Seq[Task]] = {
      tasks.groupBy(task => if (task.completed == 0) "Done" else "Todo")
    }

    for (tasks <- Toodledo.getTasks) yield
      tasks fold(
        e => Left(e),
        tasks => Right {
          tasks filter (_.contextId == contextId)
        }
        )
  }
}
