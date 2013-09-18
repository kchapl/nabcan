package model.toodledo


object Client extends scala.App {

  val appId = args(0)
  val appToken = args(1)
  val userEmail = args(2)
  val userPassword = args(3)

  val app = App(appId, appToken)
  val user = Authentication.lookUpUser(app, userEmail, userPassword)
  val tokenCache = new FileSysTokenCache(app, user)

  def key = Authentication.key(app, user, tokenCache)

  println(key)

  val contextsApi = new ContextsApi(key)
  val contexts = contextsApi.fetch
  contexts foreach println

  val folders = FoldersApi.fetch(key)
  folders foreach println

  val taskFetchFields = List("folder", "star", "priority")
  //val tasks = TasksApi.fetch(key)(modifiedAfter = Some(new DateTime().minusMonths(12)))(taskFetchFields)
  val tasks = TasksApi.fetch(key)()(taskFetchFields)
  println(tasks)

  val deleted = TasksApi.fetchDeleted(key)
  deleted foreach println

  // TODO: log http requests - this can possibly be done by turning on logging of http client library

  // TODO: filter fetch tasks

  // TODO: project fetch tasks

  // TODO: add context

  // TODO: remove context

  // TODO: add folder

  // TODO: remove folder

  // TODO: add task

  // TODO: remove task

  // TODO: toodledo api facade

}
