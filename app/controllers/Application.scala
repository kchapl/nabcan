package controllers

import play.api.mvc._
import model.service.TaskRepository
import scala.concurrent.ExecutionContext.Implicits.global

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def contexts = Action.async {
    TaskRepository.getContexts map {
      _ fold(
        e => InternalServerError(views.html.exception(e)),
        contexts => Ok(views.html.contexts(contexts))
        )
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
