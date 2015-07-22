package ru.maizy.dev.heartbeat

/**
 * Copyright (c) Nikita Kovaliov, maizy.ru, 2015
 * See LICENSE.txt for details.
 */
trait EnumerationMap extends Enumeration {
  self =>

  lazy val valuesMap = self.values.map{ v => (v.toString, v) }.toMap
}
