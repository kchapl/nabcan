package model.service

import scala.concurrent.Future
import model.{Context, Task}

object TaskRepository {

  def getContexts: Future[Either[Toodledo.Exception, Seq[Context]]] = Toodledo.getContexts

  def getBoard(contextId: Int): Future[Either[Toodledo.Exception, Seq[Task]]] = {
    for (tasks <- Toodledo.getTasks) yield
      tasks fold(
        e => Left(e),
        tasks => Right(tasks filter (_.contextId == contextId))
        )
  }
}
