package controllers

import play.api.mvc._
import model.service.toodledo.Toodledo


object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def contexts = Action.async {
    for {
      c <- Toodledo.getContexts()
    } yield c match {
      case Left(x) => InternalServerError(views.html.error(x))
      case Right(x) => Ok(views.html.contexts(x))
    }
  }

  def board = TODO

}
