package controllers

import play.api.mvc._
import model.service.Toodledo
import scala.concurrent.ExecutionContext.Implicits.global


object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def contexts = Action.async {
    Toodledo.getContexts() map {
      _ match {
        case Left(exception) => InternalServerError(views.html.exception(exception))
        case Right(contexts) => Ok(views.html.contexts(contexts))
      }
    }
  }

  def board(id: Long) = Action.async {
    Toodledo.getTasksByContext(contextId = id) map {
      _ match {
        case Left(exception) => InternalServerError(views.html.exception(exception))
        case Right(tasks) => Ok(views.html.tasks(tasks))
      }
    }
  }

}
