/**
 * ************************************************************************************************
 * Copyright (c) 2010 Fabian Steeg. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors: Fabian Steeg - initial API and implementation
 * ***********************************************************************************************
 */
package de.uni_koeln.ub.drc.data

import com.quui.sinist.XmlDb
import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
/**
 * @see User
 * @author Fabian Steeg
 */
@RunWith(classOf[JUnitRunner])
class SpecUser extends Spec with ShouldMatchers {
  val cdb = XmlDb("bob.spinfo.uni-koeln.de", 8080)
  val user = User("fsteeg", "Fabian Steeg", "Cologne, Germany", "drc", "drc", cdb)
  val db = Index.LocalDb
  describe("A User") {
    it("has a unique user name") {
      expect("fsteeg") { user.id }
    }
    it("has a full user name") {
      expect("Fabian Steeg") { user.name }
    }
    it("has an associated region") {
      expect("Cologne, Germany") { user.region }
    }
    it("can be associated with a specific DB location") {
      expect(cdb) { user.db }
      expect(cdb) { User.fromXml(user.toXml).db }
      val customDb = XmlDb("hydra2.spinfo.uni-koeln.de", 8080)
      val customUser = User("fsteeg", "Fabian Steeg", "Cologne, Germany", "", db = customDb)
      expect(customDb) { customUser.db }
      expect(customDb) { User.fromXml(customUser.toXml).db }
    }
    it("has a reputation that is adjusted according to upvoting, downvoting, being upvoted and being downvoted") {
      expect(0) { user.reputation }
      expect(true) { val prev = user.reputation; user.hasUpvoted; user.reputation > prev }
      expect(true) { val prev = user.reputation; user.wasUpvoted; user.reputation > prev }
      expect(true) { val prev = user.reputation; user.hasDownvoted; user.reputation < prev }
      expect(true) { val prev = user.reputation; user.wasDownvoted; user.reputation < prev }
    }
    it("can store the latest position worked on") {
      user.latestPage = "someid"
      expect("someid") { User.fromXml(user.toXml).latestPage }
      user.latestWord = 5
      expect(5) { User.fromXml(user.toXml).latestWord }
    }
    it("can be persisted via XML") {
      expect(user) { User.fromXml(user.toXml) }
    }
    it("can be changed and persisted") {
      expect(true) {
        user.hasUpvoted; user.save(db); User.withId(db = db, id = user.id).reputation > 0
      }
    }
  }
  describe("The User companion") {
    it("allows import of users into the DB") {
      User.initialImport(db = db, folder = "users", target = "users-test")
    }
    it("allows loading of a user from XML") {
      expect(user) { User.fromXml(user.toXml) }
    }
    it("allows access to individual saved user instances via user name") {
      val udb = XmlDb("localhost", 7777)
      val fsteeg = User("fsteeg", "Fabian Steeg", "Cologne, Germany", "drc", "drc", cdb)
      val claesn = User("claesn", "Claes Neuefeind", "Cologne, Germany", "drc", "drc", cdb)
      val matana = User("matana", "Mihail Atanassov", "Cologne, Germany", "drc", "drc", cdb)
      val rols = User("rols", "Jürgen Rolshoven", "Cologne, Germany", "drc", "drc", cdb)
      val ocr = User("OCR", "OCR", "Russia", "drc", "drc", cdb)
      expect(fsteeg) { User.withId(db = udb, id = "fsteeg") }
      expect(claesn) { User.withId(db = udb, id = "claesn") }
      expect(matana) { User.withId(db = udb, id = "matana") }
      expect(rols) { User.withId(db = udb, id = "rols") }
      expect(ocr) { User.withId(db = udb, id = "OCR") }
    }
  }
}