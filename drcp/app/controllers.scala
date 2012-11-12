/**************************************************************************************************
 * Copyright (c) 2010, 2011 Fabian Steeg. All rights reserved. This program and the accompanying 
 * materials are made available under the terms of the Eclipse Public License v1.0 which accompanies 
 * this distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 * <p/>
 * Contributors: Fabian Steeg - initial API and implementation
 *************************************************************************************************/
package controllers

import com.quui.sinist.XmlDb
import de.uni_koeln.ub.drc.data._
import de.uni_koeln.ub.drc.util.MetsTransformer
import play._
import play.mvc._
import play.i18n.Lang
import play.i18n.Messages._
import play.data.validation._
import scala.xml.Elem
import scala.xml.Node
import scala.xml.NodeSeq
import scala.xml.XML
import scala.xml.Unparsed
import java.io.File
import java.io.FileWriter
import scala.collection.mutable.ListBuffer

object Application extends Controller with Secure {
  
  import views.Application._

  val server = "hydra1.spinfo.uni-koeln.de"
  val port = 8080
  val db = XmlDb(server, port, "guest", "guest")
  val col = "drc"
//  val server = "bob.spinfo.uni-koeln.de"
//  val port = 8080
//  val db = XmlDb(server, port, "drc", "crd")
//  val col = "drc"
  
  val Prefix = "PPN345572629_"
  val Plain = "drc-plain/"
  
  val CachedMeta: Map[String, MetsTransformer] = 
    for((k,v)<-Index.Volumes; if(k!="0014_RC")) yield k -> meta(k) // no metadata for volume 0014_RC
    
  private def meta(id:String) = new MetsTransformer(Prefix + id + ".xml", db)
    
  private val fields = List(
      ("/page/text", "$m", "views.index.search.text"),
      ("/page/tag/attribute::text", "<exist:match>text</exist:match>", "views.index.search.tags"),
      ("/page/comment", "$m", "views.index.search.comments")
  )

  def loadUsers =
    (for (u <- db.getXml(col + "/users").get) yield User.fromXml(u))
      .sortBy(_.reputation).reverse

  def index = {
    val top = loadUsers.take(5)
    html.index(top, fields.map(_._3))
  }
  
  def edit = html.edit(User.withId(col, db, currentUser))
  
  private def currentUser = session.get("username")

  def contact = html.contact()
  def faq = html.faq()
  def info = html.info()
  def press = html.press()
  def salid = html.salid()

  def users = {
    val all = loadUsers
    val (left, right) = all.splitAt(all.size / 2)
    html.users(left, right)
  }

  private def imageLink(id: String) = "http://" + server + ":" + port + "/exist/rest/db/" + col + "/" +
    (if (id.matches(".*?PPN345572629_0004-000[1-6].*?")) id /* temp workaround for old data */ else
      (if (id.contains("-")) id.substring(0, id.indexOf('-')) else id) + "/" + id.replace(".xml", ".png"))
      
  private def textLink(link: String) = link.replace(".png", ".xml").replace("drc/", Plain)

  def user(id: String) = {
    val user:User = User.withId(col, db, id)
    val hasEdited = !user.latestPage.trim.isEmpty
    val page: Page = if(hasEdited) new Page(null, user.latestPage) else null
    val mets: MetsTransformer = if(page != null) CachedMeta(page.volume) else null
    html.user(user, imageLink(user.latestPage), page, mets)
  }
  
  def browse(id:String) = {
    val ids = (db.getIds(Plain + vol(id)).getOrElse(all)).sorted
    val text = loadText(ids, "<br/>")
    html.browse(text, Index.Volumes(Index.RF(id.toInt - 1)))
  }

  def signup = html.signup()

  def account() = {
    val name = params.get("name")
    val id = params.get("id")
    val pass = params.get("pass")
    val region = params.get("region")
    val message = get("views.signup.error")
    Validation.required("name", name).message(message)
    Validation.required("id", id).message(message)
    Validation.required("pass", pass).message(message)
    Validation.required("region", region).message(message)
    createAccount(name, id, pass, region)
  }
  
  def createAccount(@Required name: String, @Required id: String, @Required pass: String, @Required region: String) = {
    println("signup; name: %s, id: %s, pass: %s, region: %s".format(name, id, pass, region))
    val users = loadUsers
    val changeExistingAccount = params.get("change") != null
    if (Validation.hasErrors || ((users).exists(_.id == id) && !changeExistingAccount)) {
      signup
    } else {
      val u = User(id, name, region, pass, col, db)
      if(changeExistingAccount) 
        update(u, User.withId(col, db, id))
      db.putXml(u.toXml, col + "/users", id + ".xml")
      Action(user(id))
    }
  }
  
  def update(u:User,old:User) = {
    u.upvotes = old.upvotes
    u.upvoted = old.upvoted
    u.downvotes = old.downvotes
    u.downvoted = old.downvoted
    u.edits = old.edits
    u.latestPage = old.latestPage
    u.latestWord = old.latestWord
  }
  
  def login = html.login()
  
  def loginCheck() = {
    val id = params.get("id")
    val pass = params.get("pass")
    val message = get("views.signup.error")
    Validation.required("id", id).message(message)
    Validation.required("pass", pass).message(message)
    loginWith(id, pass)
  }
  
  def loginWith(@Required id: String, @Required pass: String) = {
    println("login; id: %s, pass: %s".format(id, pass))
    val users = loadUsers
    if (Validation.hasErrors || User.withId(col, db, id) == null || User.withId(col, db, id).pass != pass) {
      Action(login) // TODO: detailed error message
    } else {
      session.put("username", id)
      Action(user(id))
    }
  }
  
  def logout = {
    session.put("username", null)
    Redirect(Http.Request.current().headers.get("referer").value)
  }

  def changeLanguage(lang: String) = {
    response.setHeader("P3P", "CP=\"CAO PSA OUR\"")
    Lang.change(lang)
    val refererURL = Http.Request.current().headers.get("referer").value
    Redirect(refererURL)
  }

  def find() = {
    val term = params.get("term")
    val volume = params.get("volume")
    val field = params.get("field")
    search(term, volume, field)
  }
  
  def text = html.text()
  
  def load() = {
    val v = params.get("volume")
    val ids = (db.getIds(Plain + vol(v)).getOrElse(all)).sorted
    val text = loadText(ids,"\n")
    val file = write(text, vol(v))
    println("Generated: " + file.getAbsolutePath())
    response.contentType = "application/download"
    response.setHeader("Content-Disposition", "attachment; filename=" + file.getName())
    response.direct = file
  }
  
  private def vol(v:String) = {
    val volume = v
    val volumes = Index.RF
    if (volume.toInt - 1 >= 0) Prefix + volumes(volume.toInt - 1) else "drc-all"
  }
  
  private def loadText(ids: List[String], delim: String) = {
    val builder = new StringBuilder
    for(id <- ids) {
      val e: Elem = db.getXml(Plain + id.split("-")(0), id).get(0)
      if(!e.text.trim.isEmpty()) builder.append(delim*2).append(id).append(delim*2).append(e.text)
    }
    builder.toString.trim.replaceFirst(delim*2, "")
  }
  
  private def all:List[String] = {
    val buf = new ListBuffer[String]
    for(vol<-Index.RF) buf ++= db.getIds(Plain+Prefix + vol).get
    buf.toList
  }
  
  private def write(text:String, vol:String): File = {
    val tmp = File.createTempFile(vol+"_",".utf8.txt"); tmp.deleteOnExit()
    val fw = new FileWriter(tmp); fw.write(text); fw.close; tmp
  }
  
  def search(@Required term: String, @Required volume: String, @Required field: String) = {
    val volumes = Index.RF
    val vol = if (volume.toInt - 1 >= 0) Prefix + volumes(volume.toInt - 1) else ""
    val query = createQuery(fields(field.toInt), term)
    val q = db.query(Plain + vol, configure(query))
    val rows = (q \ "tr")
    val hits: Seq[Hit] = (for (row <- rows) yield parse(row)).sorted
    val label = if (volume.toInt - 1 >= 0) Index.Volumes(volumes(volume.toInt - 1)) else ""
    html.search(term, label, hits, volume, fields(field.toInt)._3)
  }

  def withLink(elems: String) = {
    val LinkParser = """(?s).*?href="([^"]+)".*?""".r
    val LinkParser(id) = elems
    XML.loadString(<i>{ Unparsed(elems.replace(id,textLink(imageLink(id)))) }</i>.toString) \"td"
  }

  def createQuery(selector: (String, String, String), term: String) = {
    """
      import module namespace kwic="http://exist-db.org/xquery/kwic";
      declare option exist:serialize "omit-xml-declaration=no encoding=utf-8";
      for $m in %s[ft:query(., '%s')]
      order by ft:score($m) descending
      return kwic:summarize(%s, <config width="40" table="yes" link="{$m/ancestor::page/attribute::id}"/>)
      """.format(selector._1, term.toLowerCase, selector._2)
  }

  private def configure(query: String): scala.xml.Elem = {
    val cdata = "<![CDATA[%s]]>".format(query)
    <query xmlns="http://exist.sourceforge.net/NS/exist" start="1" max="9999">
      <text>{ Unparsed(cdata) }</text>
      <properties><property name="indent" value="yes"/></properties>
    </query>
  }

  case class Hit(
    term: String = "",
    before: String = "",
    after: String = "",
    xml: String = "",
    volume: String = "",
    mappedVolume: String = "",
    page: String = "",
    textLink: String = "",
    imageLink: String = "") extends Ordered[Hit] {
      def compare(that:Hit) = {
        if(this.volume == that.volume) // Sort first by volume 
          try {
            this.page.split(" ")(0).toInt compare that.page.split(" ")(0).toInt // Sort second by page
          } catch {
            case _:NumberFormatException => this.page compare that.page // e.g. "n/a"
          }
        else
          Index.RF.indexOf(this.volume) compare (Index.RF.indexOf(that.volume))
      }
  }

  def parse(elem: Node): Hit = {
    val ds: Seq[Node] = elem \ "td"
    val link = (ds(1) \ "a" \ "@href").text
    val Page.VolumePageExtractor(volume, page) = link
    val mappedVolume = Index.Volumes(volume)
    val mappedPage = if (CachedMeta.contains(volume)) CachedMeta(volume).label(page.toInt) else "n/a"
    Hit(
      term = ds(1).text.trim,
      before = ds(0).text.trim,
      after = ds(2).text.trim,
      xml = link,
      volume = volume,
      mappedVolume = mappedVolume,
      page = mappedPage,
      textLink = textLink(imageLink(link)),
      imageLink = imageLink(link))
  }

}
