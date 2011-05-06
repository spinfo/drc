package de.uni_koeln.ub.drc.util.tocparser

import scala.io.Source
import scala.xml.XML
import scala.xml.PrettyPrinter
import java.io.FileReader
import java.io.FileWriter

/**
 * Main extends Parser because Parser provides the parseAll method.
 * @author Sebastian Rose
 */
object Main extends Parser {
	
	private val in = "res/toc_combined"
	private val out = "res/toc_out.xml"
	private val infile = new FileReader(in)
	private val outfile = new FileWriter(out)
	private val pretty = new PrettyPrinter(80, 2)
	
	def main(args: Array[String]) {

		// Parse the input file
		val toc = parseAll(content, infile).get

		// Create a section object for every section
		// in the parsed toc
		val sections = for {
			content <- toc
			sec <- content.keySet
		} yield new Section {
			val name = sec
			val subsections = for {
					subsec <- content(sec)
				} yield subsec
		}
		
		// Aggregate all sections to a list of XML nodes
		val nodes = for {
			single <- sections
		} yield single.toXML
		
		// Make a XML tree containing all nodes surrounded
		// by a <content> element
		val tree = <content></content>.copy(child = nodes)
		
		// Write the big XML tree to a file with a XML declaration
		XML.write(outfile, tree, "UTF-8", true, null)
		
		// This is writing better looking XML but doesn't add
		// a XML declaration on top, so it's commented out
		
		//outfile.write(pretty.format(tree))
		
		outfile.close()

	}

}