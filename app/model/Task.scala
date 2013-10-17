package model

// TODO status to be String and completed to be DateTime and context to be a Context instance
case class Task(id: Int, title: String, status: Int, completed: Long, contextId: Int)


case class Context(id: Int, name: String)
