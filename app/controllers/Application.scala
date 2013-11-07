package controllers

import play.api.mvc._
import model.service.{ToodledoService, TaskRepository}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure, Try}
import scala.concurrent.Future
import model.Context

object Application extends Controller {

  // TODO catch exception coming from toodledo requests

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def contexts = Action.async {
    val contexts2 = TaskRepository.getContexts
    contexts2 map {
      x =>
        val w = Try(x)
        w match {
          case Success(c) => {
            Ok(views.html.contexts(c))
          }
          case Failure(ToodledoService.Exception(id, description)) => {
            InternalServerError(views.html.exception(ToodledoService.Exception(id, description)))
          }
          case Failure(e) =>
            throw e
        }
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
