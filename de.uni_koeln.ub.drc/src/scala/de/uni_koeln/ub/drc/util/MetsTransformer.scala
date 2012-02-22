/**
 * ************************************************************************************************
 * Copyright (c) 2010 Fabian Steeg. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors: Fabian Steeg - initial API and implementation
 * ***********************************************************************************************
 */
package de.uni_koeln.ub.drc.util

import scala.xml._
import java.io._
import scala.xml._
import scala.collection.immutable._
import java.io.FileReader
import java.lang.StringBuilder
import scala.util.matching.Regex
import Configuration._
import de.uni_koeln.ub.drc.data.Index

/**
 * Early experimentation: convert METS metadata (XML) for scans at http://www.digizeitschriften.de/main/dms/toc/?IDDOC=5570
 * (see http://www.digizeitschriften.de/main/dms/toc/?IDDOC=5570 for format info in German) to the ContentDM input format (TSV).
 * @see MetsTransformerSpec
 * @author Fabian Steeg (fsteeg)
 */
object Count extends Enumeration {
  type Count = Value
  val Label = Value
  val File = Value
}

case class Chapter(volume: Int, number: Int, title: String) extends Ordered[Chapter] {
  def compare(that: Chapter) =
    if (this.volume == that.volume) this.number compare that.number else this.volume compare that.volume
  override def toString = "Chapitel %s: %s".format(if (number == Integer.MAX_VALUE) "X" else number, title)
}

class MetsTransformer(xml: Elem, name: String = "") {

  def this(name: String, db: com.quui.sinist.XmlDb) = this(db.getXml(Index.DefaultCollection + "/" + "PPN345572629", name).get(0), name)

  private val mods = xml \ "dmdSec" \ "mdWrap" \ "xmlData" \ "mods"
  private val loc = (mods \ "location" \ "url").text.trim
  private val num = (mods \ "part" \ "detail" \ "number").text.trim
  private val date = (mods \ "originInfo" \ "dateIssued").text.trim
  private val place = (mods \ "originInfo" \ "place" \ "placeTerm").text.trim
  private val pub = (mods \ "originInfo" \ "publisher").text.trim

  /* Maps used when generating the output in the transform method: */
  private val log: (String, Map[String, (String, String)]) = buildLogMap()
  private val fullTitle = log._1
  private var fileMap: Map[String, String] = buildFileMap // file -> physID, e.g. 205 -> phys206
  private var logMap: Map[String, (String, String)] = log._2 // logID -> chapter, e.g. log16 -> (Chapter 8, Canzun)
  private var physMap: Map[String, String] = buildPhysMap // physID -> label, e.g. phys562 -> 549
  private var linkMap: Map[String, List[String]] = buildLinkMap // physID -> logID, e.g. phys562 -> List(log67, log68)

  def label(file: Int): String = { // result is String, can be "XVI"
    val rf = physMap(fileMap(file.toString))
    name match {
      case s: String if s.contains("_0008.") => "%s (RF)".format(rf) // only correct in RF
      case s: String if s.contains("_0011.") => "%s (RF)".format(rf) // ""
      case s: String if s.contains("_0036.") => "%s (RF)".format(rf) // ""
      case _ => rf // same page numbers in RF and book edition
    }
  }

  /**
   * @return
   *   Returns the chapters the given page is part of, ordered from the most recent chapter
   *   (i.e. the most specific) to the oldest (i.e. the most general).
   */
  def chapters(page: Int, mode: Count.Value = Count.File): List[Chapter] = {
    lazy val labelMap = physMap.map(_.swap)
    val key = page.toString
    mode match {
      case Count.Label if labelMap.contains(key) => chapters(labelMap(key))
      case Count.File if fileMap.contains(key) => chapters(fileMap(key))
      case _ => List(unknownChapter)
    }
  }

  private def chapters(id: String) =
    for (logId <- linkMap(id)) yield if (logMap.contains(logId) && valid(logMap(logId)))
      // TODO get volume from initial metadata location, or init with volume number
      Chapter(4, logMap(logId)._1.split(" ")(1).toInt, logMap(logId)._2)
    else unknownChapter

  private def unknownChapter = Chapter(4, Integer.MAX_VALUE, "Unknown")

  private def valid(chapter: (String, String)) =
    List(
      "Chapter",
      "Appendix",
      "Epilogue",
      "Remarks",
      "TableOfContents",
      "TitlePage",
      "Dedication",
      "Introduction",
      "Preface",
      "List",
      "Figure").exists(chapter._1.contains(_))

  private[util] def transform(): String = {
    val builder = new StringBuilder()
    val lines: List[String] = List()

    builder.append(tabbed(List("CDM_LVL", "CDM_LVL_NAME", "Titel", "Quelle", "Publikation", "Ausgabe", "Autor", "Jahr", "Verlag", "Ort", "Typ", "Dateiformat", "Bemerkungen", "Dateiname")))
    builder.append(tabbed(List("", fullTitle, fullTitle, loc, "Romanische Forschungen", num, "C. Decurtins", date, pub, place, "Image", "image/tif")))
    builder.append(tabbed(List("0", fullTitle, "Titelblatt", "", "", "", "", "", "", "", "", "", "o", "00000005.tif")))

    xml \ "fileSec" \ "fileGrp" \ "file" foreach { (file) =>
      val fileId = file \ "@ID"
      // The following line is not working with the current Scala 2.8 nightlies, work-around via regex below
      // val fileUrlOrig = (file\"FLocat"\"@{http://www.w3.org/1999/xlink}href").toString
      val fileUrl = attribute("href", (file \ "FLocat"))
      if (fileUrl endsWith ".tif") {
        /** For each valid file, we get the details... */
        xml \ "structMap" \ "div" \ "div" foreach { (div) =>
          div \ "fptr" foreach { (fptr) =>
            if (fptr \ "@FILEID" equals fileId) {
              /** Get the physical ID for the file... */
              val physId = (div \ "@ID").text
              builder.append(processStructLinks(physId, fileUrl))
            }
          }
        }
      }
    }

    def processStructLinks(physId: String, fileUrl: String) = {
      if (linkMap.contains(physId)) {
        for (logId <- linkMap(physId)) {
          if (logMap.contains(logId)) {
            val vals = logMap(logId)
            /** Print the line in the tabstop-sep. file for one scanned file: */
            val fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1)
            tabbed(List("1", vals._1 + ": " + vals._2, physMap(physId), "", "", "", "", "", "", "", "", "", "o", fileName))
          }
        }
      }
    }
    builder.toString
  }

  private def attribute(s: String, n: NodeSeq): String = {
    val regex = new Regex("""<(?:METS|mets):.* xlink:""" + s + """="(.*?)".*>""")
    val regex(fileUrl) = n.toString.split("\n")(0)
    fileUrl
  }

  private def tabbed(items: List[String]): String = {
    val builder = new StringBuilder()
    for (item <- items) {
      if (item.trim.size > 0) builder.append("\"" + item + "\"")
      builder.append("\t")
    }
    builder.append("\n").toString
  }

  private def buildLogMap(): (String, Map[String, (String, String)]) = {
    /* Map logical IDs to chapter numbers and labels: */
    var logMap: Map[String, (String, String)] = Map()
    var fullTitle = ""
    var chapterCount = 0

    //for special cases in vol IV + VIII (0030 + 0024) start here:
    xml \ "structMap" \ "div" \ "div" foreach { (div) =>
      /* Process on this level: */
      process(div)
      /* And one level below: */
      div \ "div" foreach { process(_) }
      /* And one level below: */
      div \ "div" \ "div" foreach { process(_) }
    }

    def process(div: Node) = {
      val parentLabel = (div \ "@LABEL").toString.trim
      var lastLabel = ""

      def addToLogMap(subdiv: Node, label: String) = {
        val typeLabel: String = (subdiv \ "@TYPE").toString()
        chapterCount += 1
        /* if (label != lastLabel) chapterCount += 1
         * lastLabel = label
         * originally in default case
         */
        val id = " " + chapterCount
        logMap += (subdiv \ "@ID").text -> ((typeLabel + id, label))
      }
      div \ "div" foreach { (subdiv) =>
        val label: String = (subdiv \ "@LABEL").toString.trim

        //special case #1: elements "log4" and "log5" outside RC in vol IV (0030):
        if (loc.contains("_0030") &&
          ((subdiv \ "@ID").toString() == "log4" || (subdiv \ "@ID").toString() == "log5")) {
          addToLogMap(subdiv, label)
        } //special case #2: element "log5" outside RC in vol. VIII (0024):
        else if (loc.contains("_0024") &&
          (subdiv \ "@ID").toString() == "log5") {
          addToLogMap(subdiv, label)
        }
        //default: correct metadata
        if (parentLabel.contains("Chrestomathie")) {
          fullTitle = parentLabel
          addToLogMap(subdiv, label)
        }
      }
    }

    (fullTitle, logMap)

  }

  private def buildPhysMap() = {
    /* Map physical IDs to page numbers: */
    var physMap: Map[String, String] = Map()
    xml \ "structMap" \ "div" \ "div" foreach { (div) =>
      val id = (div \ "@ID").text.trim
      var page = (div \ "@ORDERLABEL").text.trim
      if (page.length == 0) page = (div \ "@ORDER").text.trim
      if (id.length() > 0 && page.length() > 0) physMap += id -> page
    }
    physMap
  }

  private def buildFileMap() = {
    /* Map file IDs to physical IDs: */
    var fileMap: Map[String, String] = Map()
    xml \ "structMap" \ "div" \ "div" foreach { (div) =>
      val id = (div \ "@ID").text.trim
      var page = (div \ "@ORDER").text.trim
      if (id.length() > 0 && page.length() > 0) {
        fileMap += page -> id
      }
    }
    fileMap
  }

  private def buildLinkMap = {
    /* Map physical IDs to logical IDs: */
    var linkMap: Map[String, List[String]] = Map()
    xml \ "structLink" \ "smLink" foreach { (link) =>
      /* physId -> List(logId2, logId1) */
      val physId = attribute("to", link)
      val logId = attribute("from", link)
      val logIds = if (linkMap.contains(physId)) linkMap(physId)
      else List()
      if (logMap.contains(logId)) { // only if in logMap
        linkMap += physId -> (logId :: logIds) // prepend latest hit
      }
    }
    linkMap
  }

}