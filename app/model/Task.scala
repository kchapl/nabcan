package model


case class Task(id: Int, title: String, completed: Long, contextId: Int)

case class Context(id: Int, name: String)
