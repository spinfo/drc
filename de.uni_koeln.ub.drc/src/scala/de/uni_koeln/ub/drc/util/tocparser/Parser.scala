package de.uni_koeln.ub.drc.util.tocparser

import scala.util.parsing.combinator._

/**
 * The central combinator parsing class.
 * It parses a revised version of the raw OCR output.
 * Everything but the sections were removed from the text files
 * (page numbers, some text on the first page, page headings), as
 * well as the information in parentheses.
 * Some sed and grep regular expression magic was applied to have
 * every volume appear on a single data line, followed by a dot.
 * The partitioning in sections and subsections was done manually to
 * ease the parser writing. OCR errors were also corrected manually.
 * @author Sebastian Rose
 */
class Parser extends RegexParsers {
	
	def content = rep(section)
	
	def section = "Sec:" ~> ".*".r ~! rep(subsection) <~ "EndSec" ^^
												{ case x ~ y => Map() ++ List(x -> y) }
	
	def subsection = "SubSec:" ~> ".*".r ~! rep(data) <~ "EndSubSec" ^^
												{ case x ~ y => Map() ++ List(x -> y) }
	
	def data = volume ~! pageRange <~ "." ^^
								{ case x ~ y => Map() ++ List(x -> y) }
	
	def volume = romanNumber <~ ":"
	
	def pageRange = repsep(romanRange | arabicRange, ",")
	
	def romanRange = romanNumber ~! opt("-" ~> romanNumber) ^^
									{ case x ~ y => (x, y.getOrElse(x)) }
	
	def arabicRange = arabicNumber ~! opt("-" ~> arabicNumber) ^^
									{ case x ~ y => (x, y.getOrElse(x)) }
	
	def romanNumber = "[I|V|X|L|C|D|M]+".r
	
	def arabicNumber = "[0-9]+".r ^^ (_.toInt)
	
	// No longer being used because of ambiguity between
	// roman and arabic numerals
	/*
	private def toArabic(in: String) = {
		val map = Map("I" -> 1, "II" -> 2, "III" -> 3, "IV" -> 4, "V" -> 5,
					"VI" -> 6, "VII" -> 7, "VIII" -> 8, "IX" -> 9, "X" -> 10,
					"XI" -> 11, "XII" -> 12, "XIII" -> 13, "XIV" -> 14, "XV" -> 15,
					"XVI" -> 16, "XVII" -> 17, "XVIII" -> 18, "XIX" -> 19, "XX" -> 20,
					"XXI" -> 21, "XXII" -> 22, "XXIII" -> 23, "XXIV" -> 24, "XXV" -> 25,
					"XXVI" -> 26, "XXVII" -> 27, "XXVIII" -> 28, "XXIX" -> 29, "XXX" -> 30)
		map(in)
	}
	*/
}