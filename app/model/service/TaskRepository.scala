package model.service

import scala.concurrent.Future
import model.Task

object TaskRepository {

  // TODO: have exception independent of toodledo but with same fields
  def getTasksByContext(contextId: Int): Future[Either[Toodledo.Exception, Seq[Task]]] = {
    Toodledo.getTasks() map {
      _ fold(
        e => Left(e),
        tasks => Right(tasks filter (_.contextId == contextId))
        )
    }
  }
}
