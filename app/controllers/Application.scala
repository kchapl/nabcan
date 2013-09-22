package controllers

import play.api.mvc._
import model.service.toodledo.Toodledo


object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def contexts = Action.async {
     Toodledo.getContexts("key")
  }
    Ok(views.html.board(Nil))
  }

}
