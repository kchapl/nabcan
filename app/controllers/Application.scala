package controllers

import play.api.mvc._
import model.service.{ToodledoService, TaskRepository}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try
import scala.concurrent.Future
import model.Context

object Application extends Controller {

  // TODO catch exception coming from toodledo requests

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def contexts = Action.async {
    val contexts2 = Try(TaskRepository.getContexts)
    contexts2.
    val contexts1 = TaskRepository.getContexts
    contexts1 map {
      c => Ok(views.html.contexts(c))
    }
  }

  def board(id: Int) = Action.async {
    TaskRepository.getBoard(id) map {
      _ fold(
        e => InternalServerError(views.html.exception(e)),
        tasks => Ok(views.html.board(tasks))
        )
    }
  }
}
