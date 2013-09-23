package controllers

import play.api.mvc._
import model.service.toodledo.Toodledo


object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def contexts = Action.async {
    for {
      c <- Toodledo.getContexts("key")
    } yield Ok(views.html.contexts(c))
  }

  def board = TODO

}
