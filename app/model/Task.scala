package model


case class Task(id: Int, title: String, contextId: Int)

case class Context(id: Int, name: String)
