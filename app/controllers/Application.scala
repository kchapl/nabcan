package controllers

import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure, Try}
import model.{ToodledoService, TaskRepository}

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def contexts = Action.async {
    TaskRepository.getContexts map {
      contexts =>
        Try(contexts) match {
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
      board =>
        Try(board) match {
          case Success(b) =>
            Ok(views.html.board(b))
          case Failure(ToodledoService.Exception(exceptionId, description)) => {
            InternalServerError(views.html.exception(ToodledoService.Exception(exceptionId, description)))
          }
          case Failure(e) =>
            throw e
        }
    }
  }

}
