package controllers

import play.api.mvc._
import model.service.toodledo.Toodledo


object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def board = Action {
    val tasks = Toodledo.get("")
    Ok(views.html.board(tasks))
  }

}
